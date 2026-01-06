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
        mavenCentral()
        google()
        maven { url = uri("https://maven.google.com/") }
        maven { url = uri("https://jitpack.io") }
    }
}

rootProject.name = "AfterLog"
include(":app")
