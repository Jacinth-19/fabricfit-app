pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        // Google's Maven Central mirror, listed *before* mavenCentral(). CI has been getting
        // HTTP 429 from repo.maven.apache.org on individual artifacts (repeatedly on
        // org.robolectric:nativeruntime-dist-compat, which is what broke the unit-test run);
        // pulling from a mirror spreads that load. Gradle tries repositories in order and
        // uses the first that serves the module, so a mirror hit never touches Central.
        maven("https://maven-central.storage-download.googleapis.com/maven2") {
            name = "MavenCentralMirror"
        }
        // Retry, because the remaining requests still go to Central. Gradle's built-in
        // network retry only covers connection failures, not HTTP status codes, and a 429 on a
        // shared CI IP is exactly the case it misses.
        mavenCentral()
    }
}

rootProject.name = "FabricFit"
include(":app")
