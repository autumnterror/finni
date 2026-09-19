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

rootProject.name = "FinPet"
include(":app")
include(":core")
include(":core:designsystem")
include(":core:products")
include(":feature:product-market")
include(":feature:shop")
include(":feature:game-session")
include(":feature:game-state")
include(":feature:economy")
include(":feature:week")
include(":feature:planning")
include(":feature:learning")
include(":feature:savings")
include(":feature:room")
include(":feature:pet")

include(":feature:mini-games:common")
include(":feature:mini-games:fishing")
include(":feature:mini-games:flight")
