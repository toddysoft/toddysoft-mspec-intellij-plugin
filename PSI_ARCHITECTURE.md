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

✅ **Code Completion** - Suggests field types and data types
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

## Future Improvements

### If Hierarchical PSI Is Needed

For advanced features like go-to-definition, find usages, or rename refactoring, you would need a proper hierarchical PSI tree. Here are the options:

#### Option 1: Use Grammar-Kit (Recommended)
[Grammar-Kit](https://github.com/JetBrains/Grammar-Kit) is JetBrains' official parser generator for IntelliJ plugins.

**Pros:**
- Native IntelliJ integration
- Generates proper PSI classes
- Built-in support for references, completion, etc.

**Cons:**
- Requires rewriting grammar from ANTLR to Grammar-Kit format
- Learning curve for Grammar-Kit syntax

**How to migrate:**
1. Convert `MSpec.g4` to Grammar-Kit `.bnf` format
2. Configure Grammar-Kit plugin in `build.gradle.kts`
3. Generate PSI classes
4. Implement reference resolution and navigation

#### Option 2: Manual PsiBuilder Parser
Write a hand-coded parser that uses `PsiBuilder` directly.

**Pros:**
- Full control over PSI structure
- Can reuse ANTLR grammar as reference

**Cons:**
- Labor-intensive to write and maintain
- Must manually handle all grammar rules

#### Option 3: Keep Flat PSI, Add Smart Navigation
Implement navigation features using text-based search rather than PSI references.

**Pros:**
- Doesn't require PSI restructuring
- Faster to implement

**Cons:**
- Less robust than true PSI-based navigation
- May not handle all edge cases

### Recommended Path Forward

For now, **keep the current flat PSI + text-based validation**. It's working well.

If you later need advanced features:
1. Start with **Option 3** (smart navigation) for quick wins
2. If that's insufficient, migrate to **Grammar-Kit** for full PSI support

## Technical Details

### Current Files
- `MSpecParser.java` - Simple flat parser
- `MSpecAnnotator.java` - Text-based validator
- `MSpecFileType.java` - File type registration
- `MSpecLexerAdapter.java` - ANTLR lexer wrapper
- `MSpec.g4` - ANTLR grammar (used only for lexer)

### Unused Files (Can Be Deleted)
- `MSpecTypes.java` - Element type definitions (not used in flat PSI)
- `MSpecElementType.java` - Custom element type class (not needed)
- `psi/*.java` - Specific PSI element classes (not used)

These were created during an attempt to integrate ANTLR parsing with IntelliJ PSI, which didn't work due to token stream incompatibility.

## Summary

**Current state**: ✅ Working plugin with flat PSI and text-based validation
**Hierarchical PSI attempt**: ❌ Failed due to ANTLR/PsiBuilder incompatibility
**Recommendation**: Keep current architecture; migrate to Grammar-Kit only if advanced features are needed

The plugin successfully provides syntax highlighting, code completion, and semantic validation - all the core features needed for productive MSpec file editing.
