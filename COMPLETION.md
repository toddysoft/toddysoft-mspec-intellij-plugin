# Code Completion Feature

The MSpec plugin now includes intelligent code completion to help you write MSpec files faster.

## Installation

After building the plugin:
```bash
export JAVA_HOME=/path/to/java-17
./gradlew build
```

The plugin ZIP is in `build/distributions/toddysoft-mspec-intellij-plugin-1.0-SNAPSHOT.zip`

Install it via **File → Settings → Plugins → ⚙️ → Install Plugin from Disk...**

## How It Works

### 1. Definition Type Completion

When you type an opening bracket `[` at the **top level**, press **Ctrl+Space** to see available definition types:

```mspec
[          <-- Ctrl+Space here shows: type, discriminatedType, dataIo, enum
```

**Available definition types:**
- `type` - Define a new complex type
- `discriminatedType` - Define a discriminated type with subtypes
- `dataIo` - Define a data I/O type
- `enum` - Define an enumeration type

### 2. Field Type Completion

When you type an opening bracket `[` **inside a type definition**, press **Ctrl+Space** to see available field types:

```mspec
[type MyMessage
    [          <-- Ctrl+Space here shows: simple, array, const, etc.
]
```

**Available field types:**
- `abstract` - Abstract field declaration
- `array` - Array field
- `assert` - Assertion field
- `checksum` - Checksum field
- `const` - Constant field
- `discriminator` - Discriminator for type switching
- `enum` - Enum field
- `implicit` - Implicit field
- `manualArray` - Manually parsed array
- `manual` - Manually parsed field
- `optional` - Optional field
- `padding` - Padding field
- `peek` - Peek field
- `reserved` - Reserved field
- `simple` - Simple field (most common)
- `state` - State field
- `typeSwitch` - Type switch for polymorphism
- `unknown` - Unknown data
- `validation` - Validation field
- `virtual` - Virtual field

### 3. Data Type Completion

When typing a field definition, after the field type keyword, press **Ctrl+Space** to see available data types:

```mspec
[type MyMessage
    [simple        <-- Ctrl+Space here shows: uint, int, byte, string, Message, Item, etc.
]
```

**Primitive types:**
- `bit` - Bit field
- `byte` - Byte (8 bits)
- `int` - Signed integer
- `uint` - Unsigned integer
- `vint` - Variable-length signed integer
- `vuint` - Variable-length unsigned integer
- `float` - Floating point
- `ufloat` - Unsigned float
- `string` - String
- `vstring` - Variable-length string
- `time` - Time value
- `date` - Date value
- `dateTime` - Date/time value

**Custom types:**
All types defined in the current file (with `[type Name`, `[enum Name`, etc.) will also appear in the completion list.

### 4. Example Usage

```mspec
// At top level, after [ - completion shows definition types
[        <-- Ctrl+Space: shows type, discriminatedType, dataIo, enum

// Define some custom types
[type Item
    [simple uint 8 itemType]
    [simple uint 16 itemLength]
]

[type Message
    [simple uint 16 length]
    [        <-- Ctrl+Space: shows simple, array, const, etc. (field types)
    [simple    <-- Ctrl+Space: shows uint, int, byte, Item, Message, etc. (data types)
    [array Item items count 'length']
]
```

## Tips

- Use **Ctrl+Space** (or **Cmd+Space** on macOS) to trigger completion
- Start typing to filter the list: `[s` → shows `simple`, `state`, etc.
- Completion works anywhere in the file - it scans all type definitions
- Custom types are automatically indexed as you type

## Error Highlighting

The plugin now includes semantic validation that highlights errors in real-time:

### Invalid Keywords
```mspec
[type Message
    [simpl uint 8 test]      // Error: "simpl" is not a valid field type. Did you mean "simple"?
    [aray byte data count 5] // Error: "aray" should be "array"
]
```

### Missing Size Parameters
```mspec
[type Message
    [simple int test]        // Error: Type 'int' requires a size parameter (e.g., 'int 8')
    [simple string name]     // Error: Type 'string' requires a size parameter (e.g., 'string 64')
]
```

### Undefined Type References
```mspec
[type Message
    [simple uint 8 length]
    [array NonExistentType items count 'length']  // Error: Undefined type 'NonExistentType'
]
```

The error highlighting:
- Appears as red squiggly underlines in the editor
- Provides helpful suggestions for common typos
- Validates type references against defined types in the file
- Shows errors immediately as you type

## Limitations

- Currently only scans the current file for custom types
- Cross-file type references not yet supported
- No completion for expression syntax (in tick marks `'...'`)
