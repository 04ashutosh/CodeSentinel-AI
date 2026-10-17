package com.codesentinel.parser.engine;

import com.codesentinel.common.uir.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

@Component
@Slf4j
public class Tier4LlmParserStrategy implements ParserStrategy {

    @Override
    public boolean supports(String detectedLanguage, Path filePath) {
        return "unknown_text".equalsIgnoreCase(detectedLanguage);
    }

    @Override
    public ExtractionTier getTier() {
        return ExtractionTier.TIER_4_LLM;
    }

    @Override
    public ParsedFileResult parse(Long projectId, Path filePath, Path projectDir, String detectedLanguage) throws Exception {
        // Fallback placeholder that gracefully returns LLM-tagged result
        String relativePath = projectDir.relativize(filePath).toString().replace('\\', '/');
        log.info("Tier 4 LLM analysis invoked for {}", relativePath);

        return ParsedFileResult.builder()
                .nodes(Collections.emptyList())
                .relationships(Collections.emptyList())
                .extractionTier(ExtractionTier.TIER_4_LLM)
                .processingStatus(FileProcessingStatus.PARSED_LLM)
                .diagnostics("File indexed for LLM semantic enrichment")
                .build();
    }
}