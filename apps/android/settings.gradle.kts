pluginManagement {
    repositories {
        google()
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

rootProject.name = "Brahmikeyboard-Android"
include(":app")
include(":packages:core-engine")
include(":packages:shared-data")

project(":packages:core-engine").projectDir = file("../../packages/core-engine")
project(":packages:shared-data").projectDir = file("../../packages/shared-data")
