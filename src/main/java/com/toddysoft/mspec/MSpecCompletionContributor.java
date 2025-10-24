package com.toddysoft.mspec;

import com.intellij.codeInsight.completion.*;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.patterns.PlatformPatterns;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.util.ProcessingContext;
import com.toddysoft.mspec.psi.MSpecFile;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Provides code completion for MSpec files.
 * <p>
 * Supports:
 * - Definition type completion after [ at top level (type, enum, dataIo, discriminatedType)
 * - Field type completion after [ inside type definitions (simple, array, const, etc.)
 * - Data type completion (primitives + custom types)
 */
public class MSpecCompletionContributor extends CompletionContributor {

    // Definition types that can appear at the top level after [
    private static final String[] DEFINITION_TYPES = {
        "type",
        "discriminatedType",
        "dataIo",
        "enum"
    };

    // Field types that can appear after an opening bracket inside a type definition
    private static final String[] FIELD_TYPES = {
        "abstract",
        "array",
        "assert",
        "checksum",
        "const",
        "discriminator",
        "enum",
        "implicit",
        "manualArray",
        "manual",
        "optional",
        "padding",
        "peek",
        "reserved",
        "simple",
        "state",
        "typeSwitch",
        "unknown",
        "validation",
        "virtual"
    };

    // Primitive data types
    private static final String[] PRIMITIVE_TYPES = {
        "bit",
        "byte",
        "int",
        "uint",
        "vint",
        "vuint",
        "float",
        "ufloat",
        "string",
        "vstring",
        "time",
        "date",
        "dateTime"
    };

    // Array loop types
    private static final String[] ARRAY_LOOP_TYPES = {
        "count",
        "length",
        "terminated"
    };

    // Pattern to find type definitions: [type TypeName, [enum TypeName, [dataIo TypeName, [discriminatedType TypeName
    private static final Pattern TYPE_DEFINITION_PATTERN =
        Pattern.compile("\\[\\s*(?:type|enum|dataIo|discriminatedType)\\s+([A-Z][a-zA-Z0-9_]*)");

    public MSpecCompletionContributor() {
        // Provide completion for all MSpec files
        extend(CompletionType.BASIC,
                PlatformPatterns.psiElement().inFile(PlatformPatterns.psiFile(MSpecFile.class)),
                new CompletionProvider<CompletionParameters>() {
                    @Override
                    protected void addCompletions(@NotNull CompletionParameters parameters,
                                                   @NotNull ProcessingContext context,
                                                   @NotNull CompletionResultSet result) {
                        PsiElement position = parameters.getPosition();
                        PsiFile file = position.getContainingFile();

                        // Determine what context we're in
                        CompletionContext completionContext = analyzeContext(position);

                        switch (completionContext) {
                            case DEFINITION_TYPE:
                                // After opening bracket [ at top level - suggest definition types
                                addDefinitionTypeCompletions(result);
                                break;

                            case FIELD_TYPE:
                                // After opening bracket [ inside type definition - suggest field types
                                addFieldTypeCompletions(result);
                                break;

                            case DATA_TYPE:
                                // Typing a data type - suggest primitives and custom types
                                addDataTypeCompletions(result, file);
                                break;

                            case ARRAY_LOOP_TYPE:
                                // In array field loop type position - suggest count, length, terminated
                                addArrayLoopTypeCompletions(result);
                                break;

                            case UNKNOWN:
                                // Provide both as fallback
                                addDefinitionTypeCompletions(result);
                                addFieldTypeCompletions(result);
                                addDataTypeCompletions(result, file);
                                break;
                        }
                    }
                });
    }

    /**
     * Analyzes the context to determine what kind of completion to provide
     */
    private CompletionContext analyzeContext(PsiElement position) {
        String textBeforeCursor = getTextBeforeCursor(position, 1000);

        // Check if we're in the array loop type position
        // Pattern: [array typeReference fieldName <cursor>
        // or: [manualArray typeReference fieldName <cursor>
        // We need to match: [array/manualArray followed by a type, then a field name
        Pattern arrayLoopTypePattern = Pattern.compile("\\[\\s*(?:array|manualArray)\\s+\\S+\\s+\\S+\\s+\\S*$");
        if (arrayLoopTypePattern.matcher(textBeforeCursor).find()) {
            return CompletionContext.ARRAY_LOOP_TYPE;
        }

        // Check if we're right after an opening bracket
        // Pattern: [ followed by optional whitespace
        if (textBeforeCursor.matches(".*\\[\\s*$")) {
            // Determine if we're at the top level or inside a type definition
            if (isInsideTypeDefinition(textBeforeCursor)) {
                return CompletionContext.FIELD_TYPE;
            } else {
                return CompletionContext.DEFINITION_TYPE;
            }
        }

        // Check if we're in a field definition after a field type keyword
        // Pattern: [simple uint, [array byte, etc.
        // Look for: [ fieldType whitespace ...
        Pattern fieldPattern = Pattern.compile("\\[\\s*(" + String.join("|", FIELD_TYPES) + ")\\s+\\S*$");
        if (fieldPattern.matcher(textBeforeCursor).find()) {
            return CompletionContext.DATA_TYPE;
        }

        // Check if we're after a type definition keyword
        // Pattern: [type SomeName, [enum uint 8 SomeName
        Pattern typeDefPattern = Pattern.compile("\\[\\s*(?:type|enum|dataIo|discriminatedType)\\s+.*$");
        if (typeDefPattern.matcher(textBeforeCursor).find()) {
            // Could be completing the type name or nothing - don't suggest data types here
            return CompletionContext.UNKNOWN;
        }

        return CompletionContext.UNKNOWN;
    }

    /**
     * Checks if we're inside an unclosed type definition
     */
    private boolean isInsideTypeDefinition(String textBeforeCursor) {
        // Count opening and closing brackets to determine nesting level
        int openCount = 0;
        int lastDefinitionIndex = -1;

        // Find the last definition type keyword
        Pattern defPattern = Pattern.compile("\\[\\s*(?:type|enum|dataIo|discriminatedType)\\s+");
        Matcher matcher = defPattern.matcher(textBeforeCursor);
        while (matcher.find()) {
            lastDefinitionIndex = matcher.start();
        }

        // If we found a definition keyword, count brackets from that point
        if (lastDefinitionIndex >= 0) {
            String textAfterDef = textBeforeCursor.substring(lastDefinitionIndex);
            for (char c : textAfterDef.toCharArray()) {
                if (c == '[') openCount++;
                else if (c == ']') openCount--;
            }
            // If there are unclosed brackets after the definition, we're inside it
            return openCount > 0;
        }

        return false;
    }

    /**
     * Gets text before the cursor position for context analysis
     */
    private String getTextBeforeCursor(PsiElement position, int maxLength) {
        try {
            int offset = position.getTextRange().getStartOffset();
            PsiFile file = position.getContainingFile();
            int start = Math.max(0, offset - maxLength);
            return file.getText().substring(start, offset);
        } catch (Exception e) {
            return "";
        }
    }

    /**
     * Adds definition type completions (type, enum, dataIo, discriminatedType)
     */
    private void addDefinitionTypeCompletions(CompletionResultSet result) {
        for (String defType : DEFINITION_TYPES) {
            result.addElement(LookupElementBuilder.create(defType)
                    .withTypeText("definition type")
                    .withBoldness(true));
        }
    }

    /**
     * Adds field type completions (simple, array, const, etc.)
     */
    private void addFieldTypeCompletions(CompletionResultSet result) {
        for (String fieldType : FIELD_TYPES) {
            result.addElement(LookupElementBuilder.create(fieldType)
                    .withTypeText("field type")
                    .withBoldness(true));
        }
    }

    /**
     * Adds data type completions (primitives + custom types found in file)
     */
    private void addDataTypeCompletions(CompletionResultSet result, PsiFile file) {
        // Add primitive types
        for (String primitiveType : PRIMITIVE_TYPES) {
            result.addElement(LookupElementBuilder.create(primitiveType)
                    .withTypeText("primitive type")
                    .withBoldness(true));
        }

        // Add custom types found in the file
        Set<String> customTypes = findCustomTypes(file);
        for (String customType : customTypes) {
            result.addElement(LookupElementBuilder.create(customType)
                    .withTypeText("custom type")
                    .withIcon(MSpecFileType.INSTANCE.getIcon()));
        }
    }

    /**
     * Adds array loop type completions (count, length, terminated)
     */
    private void addArrayLoopTypeCompletions(CompletionResultSet result) {
        for (String loopType : ARRAY_LOOP_TYPES) {
            result.addElement(LookupElementBuilder.create(loopType)
                    .withTypeText("array loop type")
                    .withBoldness(true));
        }
    }

    /**
     * Scans the file for custom type definitions
     * Finds: [type Name, [enum Name, [dataIo Name, [discriminatedType Name
     */
    private Set<String> findCustomTypes(PsiFile file) {
        Set<String> types = new HashSet<>();
        String fileText = file.getText();

        Matcher matcher = TYPE_DEFINITION_PATTERN.matcher(fileText);
        while (matcher.find()) {
            String typeName = matcher.group(1);
            types.add(typeName);
        }

        return types;
    }

    /**
     * Context enum to determine what kind of completion to provide
     */
    private enum CompletionContext {
        DEFINITION_TYPE,  // After [ at top level - suggest definition types (type, enum, etc.)
        FIELD_TYPE,       // After [ inside type definition - suggest field types (simple, array, etc.)
        DATA_TYPE,        // After field type keyword - suggest data types
        ARRAY_LOOP_TYPE,  // In array field loop type position - suggest count, length, terminated
        UNKNOWN           // Unknown context
    }
}
