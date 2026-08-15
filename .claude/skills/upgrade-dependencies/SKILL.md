---
name: upgrade-dependencies
description: Upgrade a Gradle project's dependencies — the Gradle wrapper itself and the versions in gradle/libs.versions.toml — verifying each bump against upstream release metadata and a real build. Use this whenever the user asks to update, upgrade, or bump dependencies, check whether anything is out of date, move to a newer Gradle, or get the build "current", even if they name only one dependency or phrase it vaguely. Also use it when a build breaks after a version change and you need to tell an upgrade regression apart from a failure that was already there.
---

# Upgrading dependencies

Two jobs that look similar but need different care: moving the Gradle wrapper, and moving
the library and plugin versions in the version catalog. Do the wrapper first — the build
tool constrains which plugin versions are even valid, so catalog bumps validated against
the old Gradle may need re-testing anyway.

The work is mostly mechanical. What makes it go wrong is *misattribution*: blaming an
upgrade for a failure that was already there, or trusting a green build that never
actually re-ran the check. Step 1 is what makes the first one impossible.

## 1. Start from a green build

Before touching a single version, run the build exactly the way you intend to verify it
afterwards:

```bash
./gradlew build
```

This is not a formality. It converts every later failure into a certainty — if the build
was green before and is red now, the upgrade did it, and there's no forensic work to do.
Skip it and every red build afterwards costs a round of stashing and re-running to find
out whether you caused it.

**If the baseline is red, stop and ask the user how to proceed.** Don't start upgrading.
Say what's failing and what you think it is, then let them choose — fix it first as its
own change, or knowingly proceed and exclude that check. Both are reasonable; neither is
yours to pick. Upgrading on top of a broken build means debugging two problems that are
now tangled together, and the fix ends up buried in a diff that was supposed to be a
routine version bump.

## 2. The Gradle wrapper

**Read the current upgrade docs before running anything** —
https://docs.gradle.org/current/userguide/gradle_wrapper.html#sec:upgrading_wrapper. The
procedure has details that are easy to get subtly wrong from memory, and it changes
between versions.

Confirm the target version and get its official checksums from
`https://services.gradle.org/versions/current` (returns JSON with `version`, `checksum`
for the distribution, and `wrapperChecksum` for the jar).

Always pass the checksum. This project pins `distributionSha256Sum`, and the wrapper task
refuses to run against a pinned properties file without it:

```bash
./gradlew :wrapper --gradle-version <X.Y.Z> --gradle-distribution-sha256-sum <checksum>
```

Pass the `checksum` value fetched above, never one computed from the file you
just downloaded.

**Run the command twice.** The first run rewrites `gradle-wrapper.properties` only; the
wrapper jar and the `gradlew`/`gradlew.bat` scripts are still the old ones until a second
run executes them under the new version.

Then verify both, since a supply-chain pin you didn't check is decoration:

```bash
sha256sum gradle/wrapper/gradle-wrapper.jar   # must equal wrapperChecksum
```

and confirm `distributionSha256Sum` in the properties file matches `checksum`.

**Read the release notes for the target version** (`https://docs.gradle.org/<X.Y.Z>/release-notes.html`)
and apply what applies. Deprecations here are cheap to fix now and expensive at the next
major. Renamed properties in `gradle.properties` are the usual hit — for example
`org.gradle.unsafe.isolated-projects` became `org.gradle.isolated-projects` in 9.7.

## 3. The version catalog

```bash
python3 <skill-dir>/scripts/latest_versions.py gradle/libs.versions.toml
```

The script maps every `[versions]` entry to the artifacts that reference it (plugin ids go
through their `<id>.gradle.plugin` marker), queries Maven Central and the Gradle Plugin
Portal, and prints current → latest stable. It never edits the file. Reading its notes
matters more than reading its table:

- **It only recommends versions of the form `1.2.3`.** That single rule screens out
  prereleases in every spelling (`-RC2`, `-Beta1`, `-M3`, `-eap`, `-alpha01`) *and* variant
  builds that sort as newer but aren't upgrades. A real example: `kotlinx-datetime` publishes
  `0.8.0-0.6.x-compat`, which is 0.8.0 recompiled for 0.6.x consumers, and a naive
  "highest version" query recommends it over plain `0.8.0`.
- **It intersects across every artifact on a ref**, because a version that exists for
  `kotlin-gradle-plugin` but not `kotlin-reflect` breaks at resolution time.
- **A "stops at X" note means one artifact stopped being published.** Either upstream
  discontinued it or the catalog entry is dead. Check whether anything actually uses it
  (`grep` for the alias in `*.gradle.kts`) before doing anything — an unused entry should
  be deleted, not bumped.
- **A "no plain-stable release" note means the dependency has only ever shipped
  prereleases.** Follow the project link in the catalog comment and decide with the user.

Apply the bumps by editing the TOML by hand rather than with a script. The comments are
column-aligned and each carries the project's release page, which is the authority when
the metadata is ambiguous; a regex rewrite ruins the alignment and skips the judgment. If
an entry is currently on a prerelease, check whether a stable release has since appeared —
that's a pin worth removing.

Version numbers alone don't tell you whether an upgrade is safe. For a major bump, or a
library at 0.x where minor bumps break APIs, check the release notes for breaking changes
before assuming a green build means compatible.

## 4. Verify

Run the same build as the baseline, now with deprecation warnings visible:

```bash
./gradlew build --warning-mode all
```

Because step 1 was green, anything red here is yours — go straight to fixing it instead of
investigating whose fault it is.

The one thing worth ruling out first is stale build state rather than a real regression. A
version bump can leave cached state that fails to load instead of invalidating cleanly,
which produces alarming errors unrelated to your change. Before debugging anything that
looks structural rather than like a compile error:

```bash
rm -rf .gradle/configuration-cache
```

## 5. When an upgrade genuinely breaks something

Prefer moving forward over reverting. A plugin that's incompatible with the new Gradle is
usually incompatible with an *old version* of that plugin — check for a newer release
first.

If nothing upstream fixes it, don't quietly drop that dependency from the upgrade. Report
what blocks it and let the user choose between pinning, waiting, or working around.

## 6. Report

Give the user a table of what moved, then — just as importantly — what didn't:

- **Upgraded**: name, from, to.
- **Already latest stable**: list them, so "you skipped things" is visibly not the case.
- **Deliberately not upgraded**: the version and the reason (prerelease-only, variant
  build, blocked by an incompatibility, needs a major-version migration).
- **Anything you noticed but didn't act on**: dead catalog entries, pre-existing failures.

State what you verified and how — a full build, cold configuration cache, tests included —
so the user knows the scope of the green checkmark. Don't commit unless asked; the user
may want to split the wrapper move and the catalog bumps into separate commits.
