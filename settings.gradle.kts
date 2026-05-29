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
        maven("https://repo.itextsupport.com/android")
    }
    resolutionStrategy {
        eachPlugin {
            if (requested.id.id == "io.objectbox") {
                useModule("io.objectbox:objectbox-gradle-plugin:${requested.version}")
            }
        }
    }
}
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven("https://jitpack.io")
        mavenLocal()
    }
}

rootProject.name = "EterUee"
include(":app")
include(":highlight")
include(":ai")
include(":search")
include(":tts")
include(":common")
include(":shared")
include(":desktop")
include(":document")
include(":web")
include(":roleplay")
include(":material3")

val termuxAppDir = file("../termux-app").takeIf { it.isDirectory } ?: file("termux-app")
include(":terminal-emulator")
project(":terminal-emulator").projectDir = termuxAppDir.resolve("terminal-emulator")
include(":terminal-view")
project(":terminal-view").projectDir = termuxAppDir.resolve("terminal-view")
