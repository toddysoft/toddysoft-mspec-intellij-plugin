# Releasing

How to cut and publish a new version of the plugin to the
[JetBrains Marketplace](https://plugins.jetbrains.com/plugin/28936-toddysoft-mspec-language-support).

Releases are signed with the ToddySoft YubiKey and uploaded through the Marketplace API. The plugin
is already listed (id `28936`), so every release is an *update* — only a plugin's first-ever
submission needs a manual web upload.

> **Publishing cannot be undone.** A published version can never be replaced or withdrawn, only
> superseded by a higher version number. If a release turns out to be broken, the fix is 1.x.y+1.

## One-time setup

### 1. Signing token and PKCS#11 module

The signing certificate lives on the ToddySoft YubiKey and its private key cannot be exported, so
signing goes through PKCS#11. This is the same token and the same config file used by the
`release-signing` profile of the Maven-based ToddySoft builds.

```bash
brew install opensc          # provides /opt/homebrew/lib/opensc-pkcs11.so and pkcs11-tool
```

Two files are needed. Keep them wherever you keep your signing material — the build locates them
through the properties in the next section, so no path is recorded in this repository.

**A SunPKCS11 config**, the same one the Maven builds use:

```
name = <token-name>
library = /opt/homebrew/lib/opensc-pkcs11.so
slotListIndex = 0
```

The `name` matters: the build reads it from this file and derives the JCA provider name
(`SunPKCS11-<token-name>`) from it, so the two can never drift apart.

**The certificate chain** exported from the token, in PKCS#7 form. Either DER or PEM encoding
works — the build re-encodes it as needed.

### 2. Credentials

Both secrets go in `~/.gradle/gradle.properties` — your home directory, never this repository.
This file is the Gradle counterpart of the `~/.m2/settings.xml` profile the Maven builds use; Gradle
cannot read `settings.xml`, so the PIN is defined in both places.

```properties
yubikey.pin=<PIV PIN>
marketplace.token=<token from https://plugins.jetbrains.com/author/me/tokens>
yubikey.pkcs11.config=<path to the SunPKCS11 config>
yubikey.cert.chain=<path to the certificate chain>
```

The two paths live here rather than in the build script because this repository is public. A leading
`~/` is expanded by the build (Gradle itself does not expand it, and an unexpanded `~` would resolve
against the project directory).

Create it with restrictive permissions:

```bash
umask 077
$EDITOR ~/.gradle/gradle.properties
```

Format notes, because a malformed PIN costs you an attempt on the token: no quotes (they become part
of the value), no trailing whitespace (it is preserved), `\` is an escape character, and `#` starts a
comment.

Both settings also accept environment variables — `YUBIKEY_PIN` and `PUBLISH_TOKEN` — for CI.

### 3. Settings reference

Every setting is a Gradle property with an environment-variable equivalent for CI. None of them
have defaults baked into `build.gradle.kts`, deliberately: no machine-local path or secret belongs
in a public repository.

| Property                | Environment variable    | Required                                                                |
|-------------------------|-------------------------|-------------------------------------------------------------------------|
| `yubikey.pin`           | `YUBIKEY_PIN`           | to sign                                                                 |
| `yubikey.pkcs11.config` | `YUBIKEY_PKCS11_CONFIG` | to sign                                                                 |
| `yubikey.cert.chain`    | `YUBIKEY_CERT_CHAIN`    | to verify the signature                                                 |
| `marketplace.token`     | `PUBLISH_TOKEN`         | to publish                                                              |
| `yubikey.key.alias`     | `YUBIKEY_KEY_ALIAS`     | no — the token holds a single key, so the signer picks it automatically |

## Release procedure

### 1. Bump the version

`gradle.properties`:

```properties
pluginVersion = 1.5.0
```

Use [SemVer](https://semver.org/). A plain version publishes to the **stable** channel that every
user receives. A pre-release suffix routes to a matching channel instead — `1.5.0-beta.1` → `beta`,
`1.5.0-rc.1` → `rc`, `2.0.0-eap.1` → `eap` — which users only get after subscribing to that channel
in their IDE. Use one when you want a release staged rather than pushed to everybody.

### 2. Check IDE compatibility

Find the current IntelliJ release:

```bash
# -L matters: the repository URL redirects.
curl -sL "https://cache-redirector.jetbrains.com/intellij-repository/releases/com/jetbrains/intellij/idea/ideaIU/maven-metadata.xml" \
  | grep -o '<version>[^<]*</version>' | tail -5
```

If a newer platform has shipped since the last release, update **both**:

- `pluginUntilBuild` in `gradle.properties` — e.g. 2026.2 → `262.*`
- the explicit IDE list in the `pluginVerification` block of `build.gradle.kts`

That second step is easy to forget and matters: `recommended()` only covers the IDEs JetBrains
publishes as *recommended*, which lags well behind current releases. Without an explicit entry a
widened `pluginUntilBuild` ships **unverified**.

Note that IntelliJ IDEA Community is no longer published separately as of 2025.3 (253) —
`IntellijIdeaCommunity` will not resolve for those versions, so newer entries use the unified
`IntellijIdea` type.

### 3. Verify against every supported IDE

```bash
./gradlew verifyPlugin
```

Every IDE in the range must report **Compatible**. Two deprecated-API usages are expected and
unavoidable: the ANTLR-generated parsers override `Recognizer.getTokenNames()`, which is abstract in
the ANTLR runtime and therefore cannot be dropped.

This downloads each IDE it has not seen before — several GB apiece, so the first run after a
platform bump is slow.

### 4. Sign

Plug in the YubiKey, then:

```bash
./gradlew verifyPluginSignature -PreleaseSigning
```

This signs the plugin and verifies the resulting signature against the certificate chain, producing
`build/distributions/toddysoft-mspec-intellij-plugin-<version>-signed.zip`.

Without `-PreleaseSigning` the build uses a PEM key and certificate
(`CERTIFICATE_CHAIN`, `PRIVATE_KEY`, `PRIVATE_KEY_PASSWORD`) instead — that path exists for CI with
an exportable certificate and is not used for releases.

### 5. Publish

```bash
./gradlew publishPlugin -PreleaseSigning
```

The build prints the version it is about to publish, and refuses to start if the token is missing.

### 6. Confirm it went live

The upload succeeding does **not** mean the version is public: JetBrains runs its own verification
and re-signs the plugin with its key before releasing it. This usually takes minutes.

```bash
curl -s "https://plugins.jetbrains.com/api/plugins/28936/updates?size=3" \
  | python3 -c "import json,sys; [print(u['version'], '| until', u['until']) for u in json.load(sys.stdin)[:3]]"
```

Pending versions appear in your author dashboard before they show up here.

### 7. Commit and tag

```bash
git commit -am "chore: Release 1.5.0"
git tag v1.5.0
git push --follow-tags
```

## Troubleshooting

| Symptom                                                       | Cause and fix                                                                                                                                            |
|---------------------------------------------------------------|----------------------------------------------------------------------------------------------------------------------------------------------------------|
| `Property 'yubikey.pin' (or YUBIKEY_PIN) must be set`         | The PIN is not in `~/.gradle/gradle.properties`. Deliberate guard — it fires *before* the token is touched, so an empty PIN never reaches it.            |
| `Property 'marketplace.token' (or PUBLISH_TOKEN) must be set` | No Marketplace token. Create one at [My Tokens](https://plugins.jetbrains.com/author/me/tokens).                                                         |
| `no such provider: SunPKCS11-<token-name>`                    | The YubiKey is not plugged in, or `slotListIndex` in the SunPKCS11 config is wrong. SunPKCS11 silently fails to register when it cannot reach the token. |
| `CKR_PIN_INCORRECT`                                           | Wrong PIN — **stop**. Each failure consumes one of a handful of attempts, and exhausting them blocks the PIV applet until it is unblocked with the PUK.  |
| `Property 'yubikey.pkcs11.config' … must point at`            | The property is unset; see one-time setup.                                                                                                               |
| `SunPKCS11 config not found at <project dir>/~/…`             | The `~` was not expanded because the path is not written as `~/…` (only a *leading* `~/` is expanded). Use an absolute path.                             |
| `Signing certificate chain not found at …`                    | The file named by `yubikey.cert.chain` does not exist.                                                                                                   |
| Verifier reports an incompatibility                           | Do not widen `pluginUntilBuild` past it. Fix the API usage, or lower the bound to the last compatible platform.                                          |

To inspect the token — neither command needs the PIN, so neither can lock it:

```bash
pkcs11-tool --module /opt/homebrew/lib/opensc-pkcs11.so --list-token-slots
pkcs11-tool --module /opt/homebrew/lib/opensc-pkcs11.so --list-objects --type cert
```

To check the PIN itself (this *does* consume an attempt if wrong, but a success resets the counter):

```bash
pkcs11-tool --module /opt/homebrew/lib/opensc-pkcs11.so --login --list-objects --type privkey
```

## Certificate expiry

The current signing certificate — an EC P-384 key in PIV slot `9a`, issued by SSL.com — is valid
until **2027-07-19**. To re-check that date:

```bash
pkcs11-tool --module /opt/homebrew/lib/opensc-pkcs11.so --read-object --type cert --id 01 \
  | openssl x509 -inform DER -noout -subject -issuer -dates
```

The signature carries **no trusted timestamp**, so it becomes unverifiable once the certificate
expires rather than remaining valid for signatures made while it was current. Distribution itself is
unaffected — the Marketplace re-signs with its own key — but plan a re-signed release before that
date.

## How the signing wiring works

Useful if this ever breaks. The JetBrains signer accepts a keystore plus a JCA provider name, which
is what makes PKCS#11 possible, but three details are non-obvious:

- **The keystore file is a placeholder.** `SignerInfoLoader.loadSignerInfoFromKeystore` always opens
  the keystore path as a stream, while a PKCS#11 keystore expects `load(null, pin)`. This works only
  because SunPKCS11's `keyStoreCompatibilityMode` defaults to `true`, which tolerates and ignores a
  non-null stream. The build therefore writes an empty `build/pkcs11/placeholder.keystore` purely so
  there is something to open.
- **The provider index must be contiguous.** SunPKCS11 is registered by appending
  `security.provider.N=SunPKCS11 <config>` to the JDK's list via `-Djava.security.properties`. The
  JDK reads those indices consecutively and **stops at the first gap**, so the build computes
  `N` as one past the highest index in the JDK's own `java.security` rather than hardcoding a value.
- **EC keys are supported.** `PublicKeyUtils.getSuggestedSignatureAlgorithm` dispatches on `is
  ECKey` and maps to `ECDSA_WITH_SHA384`, which matches the token's P-384 key. (This is worth
  knowing because Ignition's `.modl` verifier is RSA-only and rejects the same key.)
