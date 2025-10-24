# Quick Start Guide

## Building the Plugin

1. Ensure you have Java 17 installed:
```bash
java -version  # Should show Java 17
```

2. Build the plugin:
```bash
export JAVA_HOME=/path/to/java-17
./gradlew build
```

The plugin ZIP will be created at: `build/distributions/toddysoft-mspec-intellij-plugin-1.0-SNAPSHOT.zip`

## Installing the Plugin

1. Open IntelliJ IDEA
2. Go to **File → Settings → Plugins** (or **IntelliJ IDEA → Preferences → Plugins** on macOS)
3. Click the ⚙️ (gear) icon
4. Select **Install Plugin from Disk...**
5. Navigate to `build/distributions/` and select the ZIP file
6. Click **OK** and restart IntelliJ IDEA

## Testing the Plugin

1. After restarting IntelliJ IDEA, create a new file with the `.mspec` extension
2. Open the sample file from `examples/sample.mspec` to see syntax highlighting in action
3. The plugin will recognize keywords like:
   - `type`, `enum`, `constants`
   - `simple`, `array`, `const`
   - `typeSwitch`, `discriminator`
   - Data types: `uint`, `int`, `byte`, `string`, etc.

## Development Mode

To run IntelliJ IDEA with your plugin for testing without installing:

```bash
export JAVA_HOME=/path/to/java-17
./gradlew runIde
```

This will launch a new IntelliJ IDEA instance with your plugin loaded.

## Troubleshooting

### Java Version Issues

If you encounter errors related to Java versions:
- Ensure you're using Java 17 for the build
- Set JAVA_HOME explicitly before running Gradle commands

### Clean Build

If you need to rebuild from scratch:
```bash
./gradlew clean build
```

### Gradle Daemon Issues

If Gradle daemon causes problems:
```bash
./gradlew --stop
./gradlew build
```

## Next Steps

- Explore the MSpec grammar in `src/main/antlr/`
- Customize syntax highlighting in `MSpecSyntaxHighlighter.java`
- Add more language features (code completion, refactoring, etc.)
