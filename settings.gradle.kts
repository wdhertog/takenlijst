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
        // Alleen voor Tesseract4Android; verder komt alles van Google/Maven Central.
        maven("https://jitpack.io") {
            content { includeGroup("cz.adaptech.tesseract4android") }
        }
    }
}

rootProject.name = "takenlijst"
include(":app")
