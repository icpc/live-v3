# Stability guarantees and versioning

The library is not stable yet. You can expect both source and binary
changes in future releases. Versioning doesn't follow semver semantics for now. 
It's just the same as an overlay version from the same commit. 

# Using parser as a standalone library

First, you need to add a repository to your build script. We are now using JitPack as a repo.
[![](https://jitpack.io/v/icpc/live-v3.svg)](https://jitpack.io/#icpc/live-v3)

To add the jitpack repo, add following to your `build.gradle.kts` inside repositories block

```kotlin
maven("https://jitpack.io") {
    // We recommend limiting jitpack to our lib. But you can remove this line if you don't care. 
    group = "com.github.icpc.live-v3"
}
```

Plugins for specific CDSs are loaded with ServiceLoader. If you are using shadow gradle plugin, 
it would break it by default. To preverve required files don't forget to add 
```kotlin
    shadowJar {
        mergeServiceFiles()
    }
```

in your shadowJar task definition. 

# Adding a full library

Add the following dependency to make everything accessible: 

```kotlin
dependencies {
    implementation("com.github.icpc.live-v3:org.icpclive.cds.full:3.3.2")
}
```

# More fine-grained dependency

You can add the following packages with the same version instead:
* `org.icpclive:org.icpclive.cds.core` - only core part of a library, no specific CDS-s parsers
* `org.icpclive:org.icpclive.cds.%testsystem-type%` - parser for a single contest system
  * For example `org.icpclive:org.icpclive.cds.codeforces` for parsing codeforces contests. 
* `org.icpclive.org.icpclive.cds.clics-api` - a separate package with a data model of [ccs spec](https://ccs-specs.icpc.io/). 
   Can be used to parse some clics api or create clics api server.

# Documentation

Some documentation is provided on https://icpc.io/live-v3/cds/
