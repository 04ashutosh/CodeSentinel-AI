package com.codesentinel.parser.engine;

import com.codesentinel.common.uir.ExtractionTier;

import java.nio.file.Path;

public interface ParserStrategy {
    boolean supports(String detectedLanguage, Path filePath);
    ExtractionTier getTier();
    ParsedFileResult parse(Long projectId, Path filePath, Path projectDir, String detectedLanguage) throws Exception;
}