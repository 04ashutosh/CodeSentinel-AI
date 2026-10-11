package com.codesentinel.common.uir;

public enum FileProcessingStatus {
    PARSED_SUCCESS,
    PARSED_PARTIAL,
    PARSED_LLM,
    UNRECOGNIZED_SYNTAX,
    BINARY_EXCLUDED,
    FAILED
}