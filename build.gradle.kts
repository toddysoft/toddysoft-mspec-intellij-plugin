plugins {
    java
    id("org.jetbrains.intellij") version "1.17.2"
    antlr
}

group = "com.toddysoft"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    antlr("org.antlr:antlr4:4.13.1")
    implementation("org.antlr:antlr4-runtime:4.13.1")
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

intellij {
    version.set("2023.2.5")
    type.set("IC")
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
}

tasks.patchPluginXml {
    sinceBuild.set("232")
    untilBuild.set("252.*")
}

tasks.named("compileJava") {
    dependsOn("generateGrammarSource")
}

tasks.withType<AntlrTask>().configureEach {
    arguments = arguments + listOf(
        "-visitor",
        "-package", "com.toddysoft.mspec.parser"
    )
}

sourceSets {
    main {
        java {
            srcDir("src/main/java")
            srcDir(layout.buildDirectory.dir("generated-src/antlr/main"))
        }
    }
}
