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
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.10.0"
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "FinPet"
include(":app")
include(":core")
include(":core:designsystem")
include(":core:products")
include(":feature:product-market")
include(":feature:game-session")
include(":feature:game-state")
include(":feature:economy")
include(":feature:week")
include(":feature:room")
include(":feature:pet")

include(":feature:mini-games:common")
include(":feature:mini-games:fishing")
include(":feature:mini-games:flight")
