#!/usr/bin/env python3
"""Report the latest stable release for every version in a Gradle version catalog.

Reads gradle/libs.versions.toml, maps each [versions] entry to the artifacts that
reference it, and queries Maven Central and the Gradle Plugin Portal for releases.

Also scans the project's settings files for plugins whose version is hardcoded there.
A settings `plugins {}` block cannot use the catalog at all — it is evaluated before
`dependencyResolutionManagement` creates the `libs` accessor — so those versions are
necessarily written inline and are invisible to a catalog-only check. They drift
silently across upgrades unless something looks for them.

Deliberately read-only: it prints a table and never edits the catalog. Applying the
bump by hand keeps the file's comment alignment intact and forces a human decision on
the entries the script flags as ambiguous.

Usage:
    python3 latest_versions.py [path/to/libs.versions.toml] [--json]
"""

import json
import os
import re
import sys
import urllib.request
import xml.etree.ElementTree as ET
from concurrent.futures import ThreadPoolExecutor

try:
    import tomllib
except ModuleNotFoundError:  # Python < 3.11
    sys.exit("needs Python 3.11+ (for tomllib), or pip install tomli and adjust the import")

REPOS = [
    "https://repo1.maven.org/maven2",
    "https://plugins.gradle.org/m2",
]

# A release we can recommend without a human squinting at it: digits and dots only.
# This screens out two different things at once, which is why it beats a blocklist of
# "alpha|beta|rc" spellings:
#   - prereleases, however they are spelled (-RC2, -Beta1, -M3, -eap, -alpha01, .dev)
#   - variant builds that sort as "newer" but are not upgrades, e.g. kotlinx-datetime's
#     0.8.0-0.6.x-compat, which is 0.8.0 recompiled for 0.6.x consumers
PLAIN_STABLE = re.compile(r"^\d+(\.\d+)*$")


def sort_key(version):
    return [int(p) if p.isdigit() else 0 for p in re.split(r"[.\-]", version)]


def fetch_versions(group, artifact):
    """All published versions of an artifact, from the first repo that has it."""
    path = f"{group.replace('.', '/')}/{artifact}/maven-metadata.xml"
    for repo in REPOS:
        try:
            with urllib.request.urlopen(f"{repo}/{path}", timeout=30) as response:
                data = response.read()
        except Exception:
            continue
        return [e.text for e in ET.fromstring(data).iter("version") if e.text]
    return None


def catalog_artifacts(catalog):
    """Map each version ref -> the artifacts that use it.

    A plugin id resolves through its marker artifact (`<id>:<id>.gradle.plugin`), which
    is how Gradle itself finds plugins on the portal.
    """
    refs = {}
    for entry in catalog.get("libraries", {}).values():
        if isinstance(entry, dict) and isinstance(entry.get("version"), dict):
            ref = entry["version"].get("ref")
            if ref and entry.get("group") and entry.get("name"):
                refs.setdefault(ref, set()).add((entry["group"], entry["name"]))
    for entry in catalog.get("plugins", {}).values():
        if isinstance(entry, dict) and isinstance(entry.get("version"), dict):
            ref, plugin_id = entry["version"].get("ref"), entry.get("id")
            if ref and plugin_id:
                refs.setdefault(ref, set()).add((plugin_id, f"{plugin_id}.gradle.plugin"))
    return refs


# A plugin declared in a settings file, e.g. `id("com.gradle.develocity") version "4.1.1"`.
# Accepts the Groovy spelling (`id "x" version "y"`) too, and tolerates a trailing
# `apply false`. Anything matching this in a settings file is a plugin declaration.
SETTINGS_PLUGIN = re.compile(
    r"""id\s*\(?\s*["']([A-Za-z0-9_.\-]+)["']\s*\)?\s*version\s*["']([A-Za-z0-9_.\-]+)["']"""
)

# Directories that hold build output or vendored code rather than build logic.
SKIP_DIRS = {"build", "node_modules", ".gradle", ".git", ".idea", "dist", "out"}


def settings_files(root):
    """Every settings file in the project, root build first, then included builds."""
    found = []
    for dirpath, dirnames, filenames in os.walk(root):
        dirnames[:] = sorted(d for d in dirnames if d not in SKIP_DIRS and not d.startswith("."))
        for name in ("settings.gradle.kts", "settings.gradle"):
            if name in filenames:
                found.append(os.path.join(dirpath, name))
    return sorted(found, key=lambda p: (p.count(os.sep), p))


def settings_plugins(root):
    """Map each settings file -> [(plugin id, hardcoded version)] declared in it."""
    declared = {}
    for path in settings_files(root):
        hits = []
        with open(path, encoding="utf-8") as f:
            for line in f:
                line = line.split("//")[0]
                for plugin_id, version in SETTINGS_PLUGIN.findall(line):
                    hits.append((plugin_id, version))
        if hits:
            declared[os.path.relpath(path, root)] = hits
    return declared


def resolve(ref, current, artifacts):
    """Latest version published for *every* artifact sharing this ref.

    Artifacts on one ref are released together (kotlin-reflect with kotlin-gradle-plugin,
    symbol-processing-api with its Gradle plugin), so intersecting is what keeps the
    recommendation resolvable: a bump that exists for only half the ref's artifacts
    breaks the build at resolution time.

    The exception is an artifact that stopped being published — upstream discontinued it,
    or the catalog entry outlived its usefulness and nothing depends on it any more. That
    artifact would otherwise pin the whole ref to its final release, so it gets excluded
    from the intersection and named in the note instead.
    """
    result = {"ref": ref, "current": current, "latest": None, "note": "", "artifacts": []}
    if not artifacts:
        result["note"] = "no artifact in catalog — check the URL in the comment by hand"
        return result

    published = {}
    for group, artifact in sorted(artifacts):
        ga = f"{group}:{artifact}"
        result["artifacts"].append(ga)
        versions = fetch_versions(group, artifact)
        if versions is None:
            result["note"] = f"not found in any repo: {ga}"
            return result
        published[ga] = versions

    def newest_common(sources):
        common = set.intersection(*(set(v) for v in sources.values()))
        stable = [v for v in common if PLAIN_STABLE.match(v)]
        if stable:
            return max(stable, key=sort_key), None
        # Everything published is a prerelease or a variant build. Don't guess — show the
        # newest few and let a human read the project's release page.
        return None, sorted(common, key=sort_key)[-3:]

    latest, candidates = newest_common(published)

    # An artifact with nothing at or above the current version is no longer keeping up.
    # Recommending its last release would be a downgrade, so drop it and say so.
    if latest and current and PLAIN_STABLE.match(current) and sort_key(latest) < sort_key(current):
        laggards = {
            ga: max((v for v in versions if PLAIN_STABLE.match(v)), key=sort_key, default="none")
            for ga, versions in published.items()
            if not any(PLAIN_STABLE.match(v) and sort_key(v) >= sort_key(current) for v in versions)
        }
        keeping_up = {ga: v for ga, v in published.items() if ga not in laggards}
        detail = ", ".join(f"{ga} stops at {v}" for ga, v in sorted(laggards.items()))
        if keeping_up:
            latest, candidates = newest_common(keeping_up)
            result["note"] = f"{detail} — discontinued upstream, or a dead catalog entry; verify before bumping"
        else:
            result["note"] = f"{detail} — nothing on this ref still publishes; the dependency may be abandoned"
            latest = current

    result["latest"] = latest or current
    if candidates:
        result["note"] = "no plain-stable release; candidates: " + ", ".join(candidates)
    return result


def main():
    args = [a for a in sys.argv[1:] if not a.startswith("-")]
    as_json = "--json" in sys.argv
    path = args[0] if args else "gradle/libs.versions.toml"

    with open(path, "rb") as f:
        catalog = tomllib.load(f)

    versions = catalog.get("versions", {})
    refs = catalog_artifacts(catalog)

    # The project root is the catalog's grandparent (gradle/libs.versions.toml).
    root = os.path.dirname(os.path.dirname(os.path.abspath(path))) or "."
    declared = settings_plugins(root)

    # One job per thing to resolve, catalog refs and settings plugins alike, so the
    # whole report costs one round of parallel requests rather than two.
    jobs = [(item[0], item[1], refs.get(item[0], set()), None) for item in versions.items()]
    jobs += [
        (plugin_id, version, {(plugin_id, f"{plugin_id}.gradle.plugin")}, settings_file)
        for settings_file, hits in declared.items()
        for plugin_id, version in hits
    ]

    with ThreadPoolExecutor(max_workers=8) as pool:
        resolved = list(pool.map(lambda j: (resolve(j[0], j[1], j[2]), j[3]), jobs))

    for r, settings_file in resolved:
        r["settings_file"] = settings_file
    results = [r for r, f in resolved if f is None]
    from_settings = [r for r, f in resolved if f is not None]

    if as_json:
        print(json.dumps(results + from_settings, indent=2))
        return 0

    def table(rows):
        # Width per section: plugin ids are far longer than catalog refs, and a shared
        # width would stretch the catalog table to match them.
        width = max(len(r["ref"]) for r in rows)
        upgrades = 0
        for r in rows:
            changed = r["latest"] and r["latest"] != r["current"]
            upgrades += bool(changed)
            marker = "  <== UPGRADE" if changed else ""
            note = f"  [{r['note']}]" if r["note"] else ""
            print(f"{r['ref']:<{width}}  {r['current']:<16} -> {r['latest'] or '?':<16}{marker}{note}")
        return upgrades

    upgrades = table(results) if results else 0

    # Settings plugins are reported per file, because each settings file is a separate
    # build with its own declarations — and the innermost one is a bootstrap that cannot
    # be moved into the catalog no matter what.
    for settings_file in declared:
        rows = [r for r in from_settings if r["settings_file"] == settings_file]
        if rows:
            print(f"\n{settings_file}:")
            upgrades += table(rows)

    flagged = [r for r in results + from_settings if r["note"]]
    print(f"\n{upgrades} upgrade(s) available, {len(flagged)} entr(ies) need a human look.")
    if flagged:
        print("Check the project link in the catalog comment for each flagged entry.")
    if from_settings:
        print("Versions under a settings file are hardcoded there, not in the catalog: "
              "edit the settings file itself.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
