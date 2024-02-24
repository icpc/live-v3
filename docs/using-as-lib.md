# Using parser as a standalone library

First, you need to add a repository.

We are now using GitHub packages, but maybe would move to something more convenient later.

To add the GitHub packages repo, add following to your `build.gradle.kts`

```kotlin
repositories {
    repositories {
        maven("https://maven.pkg.github.com/icpc/live-v3/") {
            group = "org.icpclive"
            credentials {
                username = "your_github_username"
                password = "your_github_access_token"
            }
        }
    }
    mavenCentral()
}
```

Note, that github requires authorization for repository access.

# Adding a full library

Add the following dependency to make everything accessible: 

```kotlin
dependencies {
    implementation("org.icpclive:org.icpclive.cds.full:0.1")
}
```

# More fine-grained dependency

You can add the following packages with the same version instead:
* `org.icpclive:org.icpclive.cds.core` - only core part of a library, no specific CDS-s parsers
* `org.icpclive:org.icpclive.cds.%testsystem-type%` - parser for a single library
  * For example `org.icpclive:org.icpclive.cds.codeforces` for parsing codeforces contests. 
* `org.icpclive.org.icpclive.cds.clics-api` - a separate package with a data model of [ccs spec](https://ccs-specs.icpc.io/). 
   Can be used to parse some clics api or create clics api server.

# Stability guarantees

The library is not stable yet. You can expect both source and binary
changes in future releases.