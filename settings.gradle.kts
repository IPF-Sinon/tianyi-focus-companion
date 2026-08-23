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
include(":plugins:example-hello")
include(":plugins:onboarding")
include(":plugins:focus-engine")
include(":plugins:affection-system")
include(":plugins:character-tianyi")
include(":plugins:voice-conversation")
include(":plugins:patrol-vlm")
include(":plugins:enforce-block")
include(":plugins:enforce-lock")
include(":plugins:honor-system")
include(":plugins:statistics")
include(":plugins:soundscape")
include(":plugins:peace-zone")
include(":plugins:notification")
include(":plugins:account")
include(":plugins:plugin-market")
