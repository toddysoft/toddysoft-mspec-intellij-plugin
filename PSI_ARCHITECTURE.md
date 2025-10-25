# MSpec Plugin PSI Architecture

## Current Implementation: Flat PSI Tree + Text-Based Validation

### Why Flat PSI?
The plugin currently uses a **flat PSI tree** where all tokens are direct children of `MSpecFile`. While this might seem simplistic, it's actually the right choice for this plugin because:

1. **ANTLR Incompatibility**: ANTLR generates parsers that use their own token stream, which doesn't integrate well with IntelliJ's `PsiBuilder` architecture
2. **Complexity vs Benefit**: A hierarchical PSI tree requires significant infrastructure but provides limited benefit for MSpec's validation needs
3. **Working Solution**: Text-based validation in the annotator is working perfectly for all current requirements

### What Works Today

✅ **Syntax Highlighting** - Fully functional

✅ **Semantic Validation** - Correctly identifies:
- Invalid type references (e.g., "Huiiii" when undefined)
- Missing size parameters (e.g., "uint" without a number)
- Field names vs type references (e.g., "messageType" not flagged as error)
- Case-sensitive type validation (uppercase = names, lowercase = primitive types)

✅ **Code Completion** - Suggests field types and data types

✅ **Cross-File Type Recognition** - Recognizes types from other .mspec files in same directory
- Local types: normal highlighting
- External types: italic highlighting
- Undefined types: error highlighting

✅ **Navigate to Definition** - Jump to type definitions with Cmd+B / Ctrl+B
- Works within same file and across sibling files
- Uses `GotoDeclarationHandler` for ANTLR compatibility

✅ **File Type Recognition** - `.mspec` files properly detected

### Architecture

```
MSpec Plugin Architecture:
┌─────────────────────────────────────────┐
│ MSpecFileType                           │
│ - Registers .mspec extension           │
└─────────────────────────────────────────┘
            ↓
┌─────────────────────────────────────────┐
│ MSpecLexerAdapter                       │
│ - Uses ANTLR-generated lexer           │
│ - Tokenizes the file                    │
└─────────────────────────────────────────┘
            ↓
┌─────────────────────────────────────────┐
│ MSpecParser (Flat)                      │
│ - Consumes all tokens                   │
│ - Creates flat PSI tree                 │
└─────────────────────────────────────────┘
            ↓
┌─────────────────────────────────────────┐
│ MSpecAnnotator                          │
│ - Text-based context analysis           │
│ - Validates type references             │
│ - Checks size parameters                │
│ - Distinguishes field names from types  │
│ - Cross-file type recognition           │
│ - Visual distinction (italic) for       │
│   external types                        │
└─────────────────────────────────────────┘
            ↓
┌─────────────────────────────────────────┐
│ MSpecGotoDeclarationHandler             │
│ - Navigate to definition (Cmd+B)        │
│ - Text-based type search                │
│ - Works across files in same directory  │
└─────────────────────────────────────────┘
            ↓
┌─────────────────────────────────────────┐
│ MSpecCompletionContributor              │
│ - Code completion suggestions           │
│ - Field types and data types            │
│ - Custom type recognition               │
└─────────────────────────────────────────┘
```

### Text-Based Validation Strategy

The `MSpecAnnotator` uses regex patterns to understand context:

```java
// Detect field names (comes after type)
if (beforeContext.matches(".*\\[\\s*\\w+\\s+\\w+\\s+\\d+\\s+$")) {
    // This is a field name, skip validation
    return;
}

// Detect type references (comes after field keyword)
Matcher typeRefPattern = Pattern.compile("\\[\\s*(\\w+)\\s+$")
    .matcher(beforeContext);
if (typeRefPattern.find()) {
    // Validate that this type exists
    validateTypeExists(text, file, holder);
}
```

This approach:
- **Works reliably** for MSpec's straightforward syntax
- **Performs well** (no tree traversal needed)
- **Is maintainable** (simple regex patterns)

## Implemented Features (Option 3: Smart Navigation)

The plugin successfully implements **navigate to definition** using text-based search with `GotoDeclarationHandler`:

### How It Works

The `MSpecGotoDeclarationHandler`:
1. Intercepts navigation requests (Cmd+B / Ctrl+B)
2. Checks if cursor is on an identifier in type reference position
3. Uses regex patterns to find type definitions in current and sibling files
4. Returns the definition location for navigation

**Advantages:**
- Works perfectly with ANTLR's flat PSI structure
- No need for complex PSI tree restructuring
- Fast and reliable
- Handles cross-file references

**Implementation:**
```java
// Pattern matching for type definitions
Pattern.compile("\\[\\s*(?:type|dataIo|discriminatedType)\\s+([A-Za-z][A-Za-z0-9_-]*)");
Pattern.compile("\\[\\s*enum\\s+(?:...)?([A-Za-z][A-Za-z0-9_-]*)");

// Search current file and sibling .mspec files
PsiElement definition = findTypeDefinition(file, typeName);
if (definition == null) {
    // Search sibling files in same directory
    for (PsiFile siblingFile : getSiblingMspecFiles()) {
        definition = findTypeDefinition(siblingFile, typeName);
    }
}
```

## Future Improvements

### If More Advanced Features Are Needed

If you need features like **find usages** or **rename refactoring**, here are the options:

#### Option 1: Use Grammar-Kit
[Grammar-Kit](https://github.com/JetBrains/Grammar-Kit) is JetBrains' official parser generator for IntelliJ plugins.

**Pros:**
- Native IntelliJ integration
- Generates proper PSI classes
- Built-in support for references, completion, etc.

**Cons:**
- Requires rewriting grammar from ANTLR to Grammar-Kit format
- Learning curve for Grammar-Kit syntax

#### Option 2: Manual PsiBuilder Parser
Write a hand-coded parser that uses `PsiBuilder` directly.

**Pros:**
- Full control over PSI structure
- Can reuse ANTLR grammar as reference

**Cons:**
- Labor-intensive to write and maintain
- Must manually handle all grammar rules

### Recommended Path Forward

The current implementation with **flat PSI + text-based features** is working excellently. All essential IDE features are implemented:
- ✅ Syntax highlighting
- ✅ Code completion
- ✅ Semantic validation
- ✅ Navigate to definition
- ✅ Cross-file type recognition

Only migrate to Grammar-Kit if you need advanced refactoring features like rename or find all usages.

## Technical Details

### Current Files
- `MSpecParser.java` - Simple flat parser
- `MSpecAnnotator.java` - Text-based validator with cross-file type recognition
- `MSpecGotoDeclarationHandler.java` - Navigate to definition implementation
- `MSpecCompletionContributor.java` - Code completion provider
- `MSpecFileType.java` - File type registration
- `MSpecLexerAdapter.java` - ANTLR lexer wrapper
- `MSpecSyntaxHighlighter.java` - Syntax highlighting
- `MSpec.g4` - ANTLR grammar (used for lexer generation)
- `Expression.g4` - Expression grammar (used for lexer generation)

### Architecture Decisions

**Why GotoDeclarationHandler instead of PsiReference?**
- `GotoDeclarationHandler` works directly with the navigation action
- Doesn't require specific PSI element types
- Compatible with ANTLR's flat PSI tree
- Simpler and more maintainable than PsiReferenceContributor

**Why text-based validation?**
- ANTLR's token stream doesn't integrate well with IntelliJ's PsiBuilder
- Regex patterns are sufficient for MSpec's straightforward syntax
- Performs well without tree traversal
- Easy to maintain and extend

## Summary

**Current state**: ✅ Fully functional plugin with flat PSI and text-based features

**Implemented features:**
- ✅ Syntax highlighting
- ✅ Code completion (field types, data types, custom types)
- ✅ Semantic validation (invalid keywords, size parameters, undefined types)
- ✅ Navigate to definition (Cmd+B / Ctrl+B)
- ✅ Cross-file type recognition
- ✅ Visual distinction for external types (italic)

**Architecture approach**: Flat PSI + text-based validation + GotoDeclarationHandler
**Recommendation**: Keep current architecture - it provides all essential IDE features

The plugin successfully provides all core features needed for productive MSpec file editing without requiring complex PSI tree restructuring.
