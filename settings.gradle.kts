// CI（GitHub Actions）直连 google/mavenCentral，本地开发走国内镜像加速。
// 阿里云镜像间歇性 502 曾导致 CI 构建失败（Gradle 对 5xx 不会 fallthrough 到下一个仓库）。
// 注意：pluginManagement 块会被提前求值，不能引用块外声明的变量，故两个块内各自读取环境变量。

pluginManagement {
    repositories {
        if (System.getenv("CI") == "true") {
            google()
            mavenCentral()
            gradlePluginPortal()
        } else {
            maven { url = uri("https://maven.aliyun.com/repository/gradle-plugin") }
            maven { url = uri("https://maven.aliyun.com/repository/public") }
            maven { url = uri("https://maven.aliyun.com/repository/google") }
            maven { url = uri("https://maven.aliyun.com/repository/jcenter") }
            maven { url = uri("https://mirrors.huaweicloud.com/repository/maven/jetbrains/") }
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
}

dependencyResolutionManagement {
    repositories {
        if (System.getenv("CI") == "true") {
            google()
            mavenCentral()
        } else {
            maven { url = uri("https://maven.aliyun.com/repository/public") }
            maven { url = uri("https://maven.aliyun.com/repository/google") }
            maven { url = uri("https://mirrors.huaweicloud.com/repository/maven/jetbrains/") }
            google()
            mavenCentral()
        }
    }
}

rootProject.name = "hundunfrp"
include(":composeApp")
