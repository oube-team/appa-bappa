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
        // 같은 그룹 ID의 라이브러리가 다른 저장소에 배포되어도 내려받지 않도록, ai.pleos.playground 그룹은 이 저장소에서만 내려받는다.
        exclusiveContent {
            forRepository {
                maven("https://nexus-playground.pleos.ai/repository/maven-releases/")
            }
            filter {
                includeGroupAndSubgroups("ai.pleos.playground")
            }
        }
    }
}

rootProject.name = "appa-bappa"
include(":app")
