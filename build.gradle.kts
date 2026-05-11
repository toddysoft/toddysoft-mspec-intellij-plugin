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
    options.compilerArgs.add("-Xlint:deprecation")
    options.isDeprecation = true
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

    // Post-process generated files to neutralize the deprecated getTokenNames() overrides flagged
    // by the IntelliJ Plugin Verifier. Two cases:
    //   - In Lexer subclasses: Lexer.getTokenNames() is concrete in the ANTLR runtime, so we can
    //     drop the override entirely and inherit the default. Verifier finding goes away.
    //   - In Parser subclasses: Recognizer.getTokenNames() is abstract, so the override is
    //     mandatory; we can only add @SuppressWarnings to silence javac. The plugin verifier will
    //     still flag these two cases — that is inherent to using the ANTLR 4 runtime.
    doLast {
        val overrideBlock = Regex(
            """\t@Override\s+@Deprecated\s+(?:@SuppressWarnings\([^)]*\)\s+)?public String\[\] getTokenNames\(\)\s*\{[^}]*\}\s*""",
            RegexOption.MULTILINE
        )
        val parserAnnotated = Regex(
            """(\t)@Override\s+@Deprecated\s+public String\[\] getTokenNames\(\)""",
            RegexOption.MULTILINE
        )
        val generatedDir = layout.buildDirectory.dir("generated-src/antlr/main").get().asFile
        generatedDir.walkTopDown()
            .filter { it.extension == "java" }
            .forEach { file ->
                val content = file.readText()
                val isLexer = content.contains("extends Lexer")
                val updatedContent = if (isLexer) {
                    content.replace(overrideBlock, "")
                } else {
                    content.replace(
                        parserAnnotated,
                        "$1@Override\n$1@Deprecated\n$1@SuppressWarnings(\"deprecation\")\n$1public String[] getTokenNames()"
                    )
                }
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
