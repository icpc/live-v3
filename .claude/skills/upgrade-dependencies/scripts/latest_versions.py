#!/usr/bin/env python3
"""Report the latest stable release for every version in a Gradle version catalog.

Reads gradle/libs.versions.toml, maps each [versions] entry to the artifacts that
reference it, and queries Maven Central and the Gradle Plugin Portal for releases.

Deliberately read-only: it prints a table and never edits the catalog. Applying the
bump by hand keeps the file's comment alignment intact and forces a human decision on
the entries the script flags as ambiguous.

Usage:
    python3 latest_versions.py [path/to/libs.versions.toml] [--json]
"""

import json
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

    with ThreadPoolExecutor(max_workers=8) as pool:
        results = list(pool.map(
            lambda item: resolve(item[0], item[1], refs.get(item[0], set())),
            versions.items(),
        ))

    if as_json:
        print(json.dumps(results, indent=2))
        return 0

    width = max((len(r["ref"]) for r in results), default=10)
    upgrades = 0
    for r in results:
        changed = r["latest"] and r["latest"] != r["current"]
        upgrades += bool(changed)
        marker = "  <== UPGRADE" if changed else ""
        note = f"  [{r['note']}]" if r["note"] else ""
        print(f"{r['ref']:<{width}}  {r['current']:<16} -> {r['latest'] or '?':<16}{marker}{note}")

    flagged = [r for r in results if r["note"]]
    print(f"\n{upgrades} upgrade(s) available, {len(flagged)} entr(ies) need a human look.")
    if flagged:
        print("Check the project link in the catalog comment for each flagged entry.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
