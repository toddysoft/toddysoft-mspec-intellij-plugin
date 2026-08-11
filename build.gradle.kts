import org.jetbrains.intellij.platform.gradle.IntelliJPlatformType
// Imported explicitly: inside this script "java" resolves to the Java plugin extension, so the
// fully qualified java.* package names would not resolve.
import java.security.cert.CertificateFactory
import java.util.Base64

plugins {
    java
    id("org.jetbrains.intellij.platform") version "2.18.1"
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

// --- YubiKey (PKCS#11) signing support ---------------------------------------------------------
// Mirrors the "release-signing" profile of the Maven-based ToddySoft builds: the same SunPKCS11
// config file, the same token, the same PIN property name. Activate with -PreleaseSigning.
//
// The private key never leaves the YubiKey, so the key is addressed through PKCS#11 rather than
// passed as PEM text. Without -PreleaseSigning the build falls back to a PEM key + certificate.
val releaseSigning = providers.gradleProperty("releaseSigning").isPresent

// Neither Gradle nor java.io.File expands a leading "~", and a path that silently resolves against
// the project directory is a confusing failure, so it is expanded here.
val userHome = providers.systemProperty("user.home")
fun Provider<String>.expandingUserHome(): Provider<String> = zip(userHome) { path, home ->
    if (path.startsWith("~/")) home + path.substring(1) else path
}

// Path to the SunPKCS11 config of the signing token. Defined per machine in
// ~/.gradle/gradle.properties, never here — this repository is public.
val pkcs11ConfigPath = providers.gradleProperty("yubikey.pkcs11.config")
    .orElse(providers.environmentVariable("YUBIKEY_PKCS11_CONFIG"))
    .expandingUserHome()

// The JCA provider name is "SunPKCS11-<name>", where <name> comes from that config file, so it is
// read from there rather than duplicated here.
val pkcs11ConfigFile = layout.file(pkcs11ConfigPath.map { file(it) })
val pkcs11ProviderName = providers.fileContents(pkcs11ConfigFile).asText.map { text ->
    val name = Regex("""(?m)^\s*name\s*=\s*(\S+)\s*$""").find(text)?.groupValues?.get(1)
        ?: error("No 'name' entry found in the SunPKCS11 config ${pkcs11ConfigPath.get()}")
    "SunPKCS11-$name"
}

// The PIN is never stored in this repository. It is read from the same property name the Maven
// builds use ("yubikey.pin", conventionally in ~/.gradle/gradle.properties) or from the
// YUBIKEY_PIN environment variable.
val yubikeyPin = providers.gradleProperty("yubikey.pin")
    .orElse(providers.environmentVariable("YUBIKEY_PIN"))

// Marketplace upload token, from https://plugins.jetbrains.com/author/me/tokens
val marketplaceToken = providers.gradleProperty("marketplace.token")
    .orElse(providers.environmentVariable("PUBLISH_TOKEN"))

val pkcs11Dir = layout.buildDirectory.dir("pkcs11")
val pkcs11PlaceholderKeyStore = pkcs11Dir.map { it.file("placeholder.keystore") }

// SunPKCS11 has to be registered in the forked JVM that runs the signer, by appending it to the
// JDK's provider list. The index matters: the JDK reads security.provider.N consecutively and stops
// at the first gap, so the entry has to directly follow the last one of the JDK's own list.
val preparePkcs11Config = tasks.register("preparePkcs11Config") {
    description = "Registers the token's SunPKCS11 provider for the JVM that runs the signer."
    val configPath = pkcs11ConfigPath
    val outputDir = pkcs11Dir
    val javaHome = providers.systemProperty("java.home")
    onlyIf { releaseSigning }
    inputs.property("pkcs11ConfigPath", configPath).optional(true)
    inputs.property("javaHome", javaHome)
    outputs.dir(outputDir)
    doLast {
        require(configPath.isPresent) {
            "Property 'yubikey.pkcs11.config' (or YUBIKEY_PKCS11_CONFIG) must point at the token's " +
                "SunPKCS11 config. Set it in ~/.gradle/gradle.properties — see RELEASING.md"
        }
        val moduleConfig = file(configPath.get())
        require(moduleConfig.isFile) {
            "SunPKCS11 config not found at $moduleConfig — see RELEASING.md"
        }
        val masterSecurity = file("${javaHome.get()}/conf/security/java.security")
        require(masterSecurity.isFile) { "Cannot read the JDK security config at $masterSecurity" }
        val lastIndex = Regex("""(?m)^security\.provider\.(\d+)\s*=""")
            .findAll(masterSecurity.readText())
            .maxOfOrNull { it.groupValues[1].toInt() }
            ?: error("No security.provider entries found in $masterSecurity")

        val dir = outputDir.get().asFile.apply { mkdirs() }
        dir.resolve("java.security.overrides").writeText(
            "security.provider.${lastIndex + 1}=SunPKCS11 ${moduleConfig.absolutePath}\n"
        )
        // Read but ignored by SunPKCS11; it only exists so the signer has a stream to open.
        dir.resolve("placeholder.keystore").writeText("")
    }
}

// The signature is verified against the token's certificate chain. The chain lives next to the
// SunPKCS11 config as a PKCS#7 file, but the signer reads certificates as *text*, so a DER-encoded
// .p7b has to be re-encoded as PEM first. CertificateFactory reads both encodings.
val signingCertificateChain = providers.gradleProperty("yubikey.cert.chain")
    .orElse(providers.environmentVariable("YUBIKEY_CERT_CHAIN"))
    .expandingUserHome()

// Deliberately not under build/pkcs11: that whole directory is the output of preparePkcs11Config,
// and overlapping task outputs make Gradle infer a dependency between the two tasks.
val pemCertificateChain = layout.buildDirectory.dir("signing").map { it.file("certificate-chain.pem") }

val exportSigningCertificateChain = tasks.register("exportSigningCertificateChain") {
    description = "Re-encodes the signing certificate chain as PEM for signature verification."
    val source = signingCertificateChain
    val target = pemCertificateChain
    onlyIf { releaseSigning }
    inputs.property("source", source).optional(true)
    outputs.file(target)
    doLast {
        require(source.isPresent) {
            "Property 'yubikey.cert.chain' (or YUBIKEY_CERT_CHAIN) must point at the signing " +
                "certificate chain. Set it in ~/.gradle/gradle.properties — see RELEASING.md"
        }
        val sourceFile = file(source.get())
        require(sourceFile.isFile) {
            "Signing certificate chain not found at $sourceFile — see RELEASING.md"
        }
        val certificates = sourceFile.inputStream().buffered().use { stream ->
            CertificateFactory.getInstance("X.509").generateCertificates(stream)
        }
        require(certificates.isNotEmpty()) { "No certificates found in $sourceFile" }
        val encoder = Base64.getMimeEncoder(64, "\n".toByteArray())
        target.get().asFile.writeText(
            certificates.joinToString("") {
                "-----BEGIN CERTIFICATE-----\n${encoder.encodeToString(it.encoded)}\n-----END CERTIFICATE-----\n"
            }
        )
    }
}

tasks.named("publishPlugin") {
    val token = marketplaceToken
    val pluginVersion = providers.gradleProperty("pluginVersion")
    doFirst {
        require(token.isPresent) {
            "Property 'marketplace.token' (or PUBLISH_TOKEN) must be set to publish. " +
                "Create one at https://plugins.jetbrains.com/author/me/tokens"
        }
        // Publishing is not reversible: a version cannot be replaced, only superseded.
        logger.lifecycle("Publishing version ${pluginVersion.get()} to the JetBrains Marketplace.")
    }
}

tasks.named("verifyPluginSignature") {
    if (releaseSigning) {
        // The plugin does not wire these itself: the task reads signPlugin's archive and the PEM
        // chain, but declares no dependency on either.
        dependsOn(exportSigningCertificateChain, "signPlugin")
    }
}

tasks.named<JavaExec>("signPlugin") {
    if (releaseSigning) {
        dependsOn(preparePkcs11Config, exportSigningCertificateChain)
        val overrides = pkcs11Dir.map { it.file("java.security.overrides") }
        // A single "=" merges these entries into the JDK's master security file; "==" would replace it.
        jvmArgumentProviders.add(CommandLineArgumentProvider {
            listOf("-Djava.security.properties=${overrides.get().asFile.absolutePath}")
        })
        doFirst {
            // A wrong PIN burns one of the YubiKey's few attempts, so fail before the token is touched.
            require(yubikeyPin.isPresent) {
                "Property 'yubikey.pin' (or YUBIKEY_PIN) must be set when -PreleaseSigning is used"
            }
        }
    }
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
            // "recommended()" only covers the IDEs JetBrains publishes as recommended, which lags
            // behind the current releases, so the newest platforms in our compatibility range are
            // listed explicitly.
            recommended()
            // Note: since 2025.3 (253) IntelliJ IDEA Community is no longer published separately,
            // so these use the unified "IntellijIdea" distribution rather than "IntellijIdeaCommunity".
            create(IntelliJPlatformType.IntellijIdea, "2025.3.6.1")
            create(IntelliJPlatformType.IntellijIdea, "2026.1.4")
            create(IntelliJPlatformType.IntellijIdea, "2026.2.1")
        }
    }

    // Credentials are read from the environment only; nothing secret belongs in this file or in
    // gradle.properties. See "Publishing" in README.md for the release procedure.
    //
    // Two signing paths are supported, selected by -PreleaseSigning:
    //   - YubiKey via PKCS#11 (release builds), see the block above.
    //   - PEM key + certificate, for an exportable certificate or for CI.
    signing {
        if (releaseSigning) {
            // SunPKCS11 ignores the keystore stream in its default keyStoreCompatibilityMode, but
            // the signer always opens the file, so it is given an empty placeholder to read.
            keyStore = pkcs11PlaceholderKeyStore
            keyStoreType = "PKCS11"
            keyStoreProviderName = pkcs11ProviderName
            keyStorePassword = yubikeyPin
            // Only needed if the token exposes more than one key; PIV slot 9a is the default.
            keyStoreKeyAlias = providers.gradleProperty("yubikey.key.alias")
                .orElse(providers.environmentVariable("YUBIKEY_KEY_ALIAS"))
            // Used by verifyPluginSignature only: signPlugin takes the chain off the token, since
            // the signer prefers the keystore over these properties.
            certificateChainFile = pemCertificateChain
        } else {
            certificateChain = providers.environmentVariable("CERTIFICATE_CHAIN")
            privateKey = providers.environmentVariable("PRIVATE_KEY")
            password = providers.environmentVariable("PRIVATE_KEY_PASSWORD")
        }
    }

    publishing {
        // Same convention as the signing PIN: a property in ~/.gradle/gradle.properties, or an
        // environment variable for CI. Never in this repository.
        token = marketplaceToken
        // A version like "1.4.0-beta.1" goes to the matching Marketplace channel; a plain "1.4.0"
        // goes to the default (stable) channel that every user receives.
        channels = providers.gradleProperty("pluginVersion").map {
            listOf(it.substringAfter('-', "").substringBefore('.').ifEmpty { "default" })
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
