pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
        maven {
            url = uri("https://jitpack.io")
        }
        jcenter()
        maven {
            url = uri("https://maven.fabric.io/public")
        }
    }
    dependencyResolutionManagement {
        repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
        repositories {
            google()
            mavenCentral()
            maven {
                url = uri("https://jitpack.io")
            }
            jcenter()
            maven {
                url = uri("https://maven.fabric.io/public")
            }
        }
    }

    rootProject.name = "FlashCard Application"
    include(":app")
}
