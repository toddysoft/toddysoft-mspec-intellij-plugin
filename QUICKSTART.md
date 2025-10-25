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
2. Open example files from `examples/` directory (e.g., `eip.mspec`, `ads.mspec`) to see the plugin in action
3. Features you can test:
   - **Syntax Highlighting**: Keywords like `type`, `enum`, `simple`, `array` are color-coded
   - **Code Completion**: Press Ctrl+Space after `[` to see suggestions
   - **Navigate to Definition**: Cmd+B (macOS) or Ctrl+B (Windows/Linux) on a type reference
   - **Error Detection**: Invalid keywords and undefined types are highlighted in red
   - **Cross-File Types**: Types from other .mspec files in same directory are recognized (shown in italic)

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

### Learn More About the Plugin

- **COMPLETION.md** - Detailed guide on code completion, error highlighting, and navigate to definition
- **PSI_ARCHITECTURE.md** - Technical details on the plugin architecture and design decisions
- **README.md** - Full feature list and project overview

### Explore the Code

- `src/main/antlr/` - ANTLR grammar files (MSpec.g4, Expression.g4)
- `src/main/java/com/toddysoft/mspec/` - Plugin source code
  - `MSpecAnnotator.java` - Semantic validation and error detection
  - `MSpecGotoDeclarationHandler.java` - Navigate to definition feature
  - `MSpecCompletionContributor.java` - Code completion provider
  - `MSpecSyntaxHighlighter.java` - Syntax highlighting

### Example Files

Open files in `examples/` to see real-world MSpec usage:
- `examples/eip.mspec` - Ethernet/IP protocol definitions
- `examples/ads.mspec` - ADS protocol definitions
- `examples/profinet/pnio.mspec` - PROFINET I/O definitions
