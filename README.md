# MSpec Language Support Plugin for IntelliJ IDEA

An IntelliJ IDEA plugin that provides language support for Apache PLC4X MSpec (Message Specification) files.

## Features

- **Syntax Highlighting**: Color-coded syntax for MSpec language elements including:
  - Keywords (type, simple, enum, etc.)
  - Identifiers
  - Comments (line and block)
  - String literals
  - Numeric literals
  - Operators and brackets

- **Code Completion**: Intelligent completion for:
  - Definition types (type, enum, discriminatedType, dataIo)
  - Field types (simple, array, const, etc.)
  - Data types (primitive types and custom types defined in your files)

- **Semantic Validation**: Real-time error detection for:
  - Invalid keywords and field types
  - Missing size parameters for types like int, uint, float, string
  - Undefined type references
  - Proper validation following ANTLR grammar rules

- **Cross-File Type Recognition**: Automatically recognizes types defined in other .mspec files in the same directory

- **Navigate to Definition**: Jump to type definitions with Cmd+B (macOS) or Ctrl+B (Windows/Linux)
  - Works across files in the same directory
  - External types are visually distinguished with italic styling

- **File Type Recognition**: Automatic recognition of `.mspec` files

- **ANTLR-based Parser**: Uses the official Apache PLC4X MSpec ANTLR4 grammar for accurate parsing

## Building the Plugin

### Prerequisites

- Java 17 or higher (note: the build requires Java 17 specifically due to Gradle compatibility)
- Gradle 8.5+ (included via wrapper)

### Build Instructions

1. Clone the repository
2. Run the build with Java 17:

```bash
export JAVA_HOME=/path/to/java-17
./gradlew build
```

The plugin will be built and packaged in `build/distributions/`.

### Running in Development

To run IntelliJ IDEA with the plugin for testing:

```bash
export JAVA_HOME=/path/to/java-17
./gradlew runIde
```

## Installation

1. Build the plugin as described above
2. In IntelliJ IDEA, go to `File > Settings > Plugins`
3. Click the gear icon and select `Install Plugin from Disk...`
4. Select the ZIP file from `build/distributions/`
5. Restart IntelliJ IDEA

## Grammar Files

The plugin uses the official Apache PLC4X ANTLR4 grammars:
- **MSpec.g4**: Main MSpec language grammar
- **Expression.g4**: Expression grammar for MSpec expressions

These grammars are located in `src/main/antlr/` and are automatically processed during the build.

## Project Structure

```
toddysoft-mspec-intellij-plugin/
├── src/
│   ├── main/
│   │   ├── antlr/              # ANTLR grammar files
│   │   │   ├── MSpec.g4
│   │   │   └── Expression.g4
│   │   ├── java/               # Java source files
│   │   │   └── com/toddysoft/mspec/
│   │   │       ├── MSpecLanguage.java
│   │   │       ├── MSpecFileType.java
│   │   │       ├── MSpecLexerAdapter.java
│   │   │       ├── MSpecParserDefinition.java
│   │   │       ├── MSpecSyntaxHighlighter.java
│   │   │       ├── MSpecAnnotator.java          # Semantic validation
│   │   │       ├── MSpecCompletionContributor.java
│   │   │       ├── MSpecGotoDeclarationHandler.java  # Navigate to definition
│   │   │       └── ...
│   │   └── resources/
│   │       ├── META-INF/
│   │       │   └── plugin.xml  # Plugin descriptor
│   │       └── icons/
│   │           └── mspec.svg   # File type icon
├── examples/                   # Example MSpec files
│   ├── eip.mspec
│   ├── ads.mspec
│   └── profinet/
├── build.gradle.kts            # Gradle build configuration
├── README.md
├── QUICKSTART.md
├── COMPLETION.md
└── PSI_ARCHITECTURE.md
```

## Development Notes

- The plugin uses ANTLR 4.13.1 for parsing
- IntelliJ Platform version: 2023.2.5
- Minimum IDE build: 232
- Maximum IDE build: 242.*

## License

This plugin is developed for use with Apache PLC4X MSpec files. The MSpec grammar files are licensed under the Apache License 2.0.

## Contributing

Contributions are welcome! Please ensure that:
1. Code follows the existing style
2. The build passes: `./gradlew build`
3. The plugin runs correctly: `./gradlew runIde`

## Acknowledgments

- Apache PLC4X project for the MSpec grammar definition
- JetBrains for the IntelliJ Platform SDK
