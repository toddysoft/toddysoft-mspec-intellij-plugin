plugins {
    java
    id("org.jetbrains.intellij.platform") version "2.14.0"
    antlr
}

group = providers.gradleProperty("pluginGroup").get()
version = providers.gradleProperty("pluginVersion").get()

repositories {
    mavenCentral()
    intellijPlatform {
        defaultRepositories()
    }
}

dependencies {
    antlr("org.antlr:antlr4:4.13.2")
    implementation("org.antlr:antlr4-runtime:4.13.2")

    intellijPlatform {
        intellijIdeaCommunity(providers.gradleProperty("platformVersion"))
    }
}

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

intellijPlatform {
    pluginConfiguration {
        version = providers.gradleProperty("pluginVersion")
        ideaVersion {
            sinceBuild = providers.gradleProperty("pluginSinceBuild")
            untilBuild = providers.gradleProperty("pluginUntilBuild")
        }
    }

    pluginVerification {
        ides {
            recommended()
        }
    }
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
}

tasks.named("compileJava") {
    dependsOn("generateGrammarSource")
}

tasks.named("build") {
    dependsOn("buildPlugin")
}

tasks.withType<AntlrTask>().configureEach {
    arguments = arguments + listOf(
        "-visitor",
        "-package", "com.toddysoft.mspec.parser"
    )

    // Post-process generated files to suppress deprecation warnings
    doLast {
        val generatedDir = layout.buildDirectory.dir("generated-src/antlr/main").get().asFile
        generatedDir.walkTopDown()
            .filter { it.extension == "java" }
            .forEach { file ->
                val content = file.readText()

                // Add @SuppressWarnings to getTokenNames() overrides
                val updatedContent = content.replace(
                    Regex("""(\t)@Override\s+@Deprecated\s+public String\[\] getTokenNames\(\)""", RegexOption.MULTILINE),
                    "$1@Override\n$1@Deprecated\n$1@SuppressWarnings(\"deprecation\")\n$1public String[] getTokenNames()"
                )

                if (content != updatedContent) {
                    file.writeText(updatedContent)
                }
            }
    }
}

sourceSets {
    main {
        java {
            srcDir("src/main/java")
            srcDir(layout.buildDirectory.dir("generated-src/antlr/main"))
        }
    }
}
