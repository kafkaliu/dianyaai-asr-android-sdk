pluginManagement {
    repositories {
        maven { url = uri("https://maven.aliyun.com/repository/google") }
        maven { url = uri("https://maven.aliyun.com/repository/central") }
        maven { url = uri("https://maven.aliyun.com/repository/gradle-plugin") }
        maven { url = uri("https://maven.aliyun.com/repository/public") }

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
        maven { url = uri("https://maven.aliyun.com/repository/google") }
        maven { url = uri("https://maven.aliyun.com/repository/central") }
        maven { url = uri("https://maven.aliyun.com/repository/public") }

        google()
        mavenCentral()
        maven {
            url = uri("https://kafkaliu.github.io/dianyaai-asr-android-sdk/")
        }
    }
}

rootProject.name = "ASRDemo2"
include(":app")

// includeBuild("../../../dianyaai-asr-android-sdk-source") {
//     dependencySubstitution {
//         substitute(module("com.dianyaai.asr:dianyaai-asr-android-sdk")).using(project(":sdk"))
//     }
// }