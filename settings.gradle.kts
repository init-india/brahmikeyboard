pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "BrahmiKeyboard"

// Include Android app
include(":apps:android:app")

// Include package modules
include(":packages:core-engine")
include(":packages:shared-data")
