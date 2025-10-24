package com.toddysoft.mspec;

import com.intellij.psi.tree.IElementType;

/**
 * Element types corresponding to ANTLR grammar rules.
 * These allow IntelliJ to create proper PSI nodes for each grammar construct.
 */
public interface MSpecTypes {
    // File structure
    IElementType FILE_RULE = new MSpecElementType("FILE_RULE");
    IElementType CONSTANTS_DEFINITION = new MSpecElementType("CONSTANTS_DEFINITION");
    IElementType GLOBALS_DEFINITION = new MSpecElementType("GLOBALS_DEFINITION");
    IElementType CONTEXT_DEFINITION = new MSpecElementType("CONTEXT_DEFINITION");
    IElementType COMPLEX_TYPE_DEFINITION = new MSpecElementType("COMPLEX_TYPE_DEFINITION");

    // Complex types
    IElementType COMPLEX_TYPE = new MSpecElementType("COMPLEX_TYPE");
    IElementType TYPE_DEFINITION = new MSpecElementType("TYPE_DEFINITION");
    IElementType DISCRIMINATED_TYPE_DEFINITION = new MSpecElementType("DISCRIMINATED_TYPE_DEFINITION");
    IElementType ENUM_DEFINITION = new MSpecElementType("ENUM_DEFINITION");
    IElementType DATAIO_DEFINITION = new MSpecElementType("DATAIO_DEFINITION");

    // Field definitions
    IElementType FIELD_DEFINITION = new MSpecElementType("FIELD_DEFINITION");
    IElementType FIELD = new MSpecElementType("FIELD");
    IElementType SIMPLE_FIELD = new MSpecElementType("SIMPLE_FIELD");
    IElementType ARRAY_FIELD = new MSpecElementType("ARRAY_FIELD");
    IElementType CONST_FIELD = new MSpecElementType("CONST_FIELD");
    IElementType ABSTRACT_FIELD = new MSpecElementType("ABSTRACT_FIELD");
    IElementType ASSERT_FIELD = new MSpecElementType("ASSERT_FIELD");
    IElementType CHECKSUM_FIELD = new MSpecElementType("CHECKSUM_FIELD");
    IElementType DISCRIMINATOR_FIELD = new MSpecElementType("DISCRIMINATOR_FIELD");
    IElementType ENUM_FIELD = new MSpecElementType("ENUM_FIELD");
    IElementType IMPLICIT_FIELD = new MSpecElementType("IMPLICIT_FIELD");
    IElementType MANUAL_FIELD = new MSpecElementType("MANUAL_FIELD");
    IElementType MANUAL_ARRAY_FIELD = new MSpecElementType("MANUAL_ARRAY_FIELD");
    IElementType OPTIONAL_FIELD = new MSpecElementType("OPTIONAL_FIELD");
    IElementType PADDING_FIELD = new MSpecElementType("PADDING_FIELD");
    IElementType PEEK_FIELD = new MSpecElementType("PEEK_FIELD");
    IElementType RESERVED_FIELD = new MSpecElementType("RESERVED_FIELD");
    IElementType STATE_FIELD = new MSpecElementType("STATE_FIELD");
    IElementType TYPESWITCH_FIELD = new MSpecElementType("TYPESWITCH_FIELD");
    IElementType UNKNOWN_FIELD = new MSpecElementType("UNKNOWN_FIELD");
    IElementType VALIDATION_FIELD = new MSpecElementType("VALIDATION_FIELD");
    IElementType VIRTUAL_FIELD = new MSpecElementType("VIRTUAL_FIELD");

    // Type references
    IElementType TYPE_REFERENCE = new MSpecElementType("TYPE_REFERENCE");
    IElementType DATA_TYPE = new MSpecElementType("DATA_TYPE");
    IElementType COMPLEX_TYPE_REFERENCE = new MSpecElementType("COMPLEX_TYPE_REFERENCE");
    IElementType SIMPLE_TYPE_REFERENCE = new MSpecElementType("SIMPLE_TYPE_REFERENCE");

    // Other constructs
    IElementType ID_EXPRESSION = new MSpecElementType("ID_EXPRESSION");
    IElementType EXPRESSION = new MSpecElementType("EXPRESSION");
    IElementType ATTRIBUTE = new MSpecElementType("ATTRIBUTE");
    IElementType ATTRIBUTE_LIST = new MSpecElementType("ATTRIBUTE_LIST");
    IElementType CASE_STATEMENT = new MSpecElementType("CASE_STATEMENT");
}
