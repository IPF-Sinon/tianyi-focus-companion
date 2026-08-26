pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    @Suppress("UnstableApiUsage")
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "YijianZhongqin"

include(":host")
include(":plugin-sdk")

// 官方插件
include(":plugins:onboarding")
include(":plugins:statistics")
include(":plugins:home-html")
