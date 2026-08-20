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
        mavenCentral()
    }
}

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

rootProject.name = "pclauncher"

// SRS §9 -- feature-first modules. Only :app depends on features.
include(":app")
include(":core:design")
include(":core:data")
include(":core:apps")
include(":feature:desktop")
include(":feature:shell")
include(":feature:search")
include(":feature:windows")
include(":feature:settings")
include(":platform:privileged")
