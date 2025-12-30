plugins {
    id("java")
    id("org.jetbrains.kotlin.jvm") version "2.1.20"
    id("org.jetbrains.intellij.platform") version "2.10.2"
}

group = "com.api.doc.generate"
version = "1.1-SNAPSHOT"

repositories {
    mavenCentral()
    intellijPlatform {
        defaultRepositories()
    }
}

// Read more: https://plugins.jetbrains.com/docs/intellij/tools-intellij-platform-gradle-plugin.html
dependencies {
    intellijPlatform {
        // 方式1: 使用本地已安装的 IDEA（避免下载）
        // 请将路径修改为你的 IDEA 安装目录
        local("C:/Program Files/JetBrains/IntelliJ IDEA 2024.3.1")
        
        // 方式2: 从网络下载指定版本（注释掉上面的 local，取消下面的注释）
        // intellijIdea("2024.3")
        
        testFramework(org.jetbrains.intellij.platform.gradle.TestFrameworkType.Platform)


        // Add plugin dependencies for compilation here, example:
        bundledPlugin("com.intellij.java")
    }
}

intellijPlatform {
    pluginConfiguration {
        ideaVersion {
            // 支持 IDEA 2024.1 及以上版本
            // 241 = IDEA 2024.1, 242 = IDEA 2024.2, 243 = IDEA 2024.3
            // 251 = IDEA 2025.1, 252 = IDEA 2025.2
            sinceBuild = "241"
            // 不设置 untilBuild 表示支持所有未来版本
            // 如需限制最高版本，可设置如: untilBuild = "243.*"
        }

        changeNotes = """
            Initial version - 支持 IDEA 2024.1+
        """.trimIndent()
    }
}

tasks {
    // Set the JVM compatibility versions
    // IDEA 2024.x 使用 Java 17，IDEA 2025.x 使用 Java 21
    // 为了兼容 2024，使用 Java 17
    withType<JavaCompile> {
        sourceCompatibility = "17"
        targetCompatibility = "17"
        options.encoding = "UTF-8"
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}
