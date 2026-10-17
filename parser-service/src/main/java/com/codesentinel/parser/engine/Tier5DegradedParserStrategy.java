package com.codesentinel.parser.engine;

import com.codesentinel.common.uir.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;

@Component
@Slf4j
public class Tier5DegradedParserStrategy implements ParserStrategy {

    @Override
    public boolean supports(String detectedLanguage, Path filePath) {
        return true; // Catch-all fallback
    }

    @Override
    public ExtractionTier getTier() {
        return ExtractionTier.TIER_5_DEGRADED;
    }

    @Override
    public ParsedFileResult parse(Long projectId, Path filePath, Path projectDir, String detectedLanguage) throws Exception {
        String relativePath = projectDir.relativize(filePath).toString().replace('\\', '/');
        String fileNodeId = "proj-" + projectId + ":file-" + relativePath;

        long lineCount = 0;
        try {
            lineCount = Files.lines(filePath).count();
        } catch (Exception ignored) {}

        UnifiedNode fileNode = UnifiedNode.builder()
                .id(fileNodeId)
                .projectId(String.valueOf(projectId))
                .fileId(relativePath)
                .category(NodeCategory.FILE)
                .language(detectedLanguage)
                .name(filePath.getFileName().toString())
                .qualifiedName(relativePath)
                .extractionTier(ExtractionTier.TIER_5_DEGRADED)
                .confidence(0.50)
                .verificationStatus(VerificationStatus.EXTRACTED_UNVERIFIED)
                .properties(Collections.singletonMap("lineCount", lineCount))
                .build();

        return ParsedFileResult.builder()
                .nodes(Collections.singletonList(fileNode))
                .relationships(Collections.emptyList())
                .extractionTier(ExtractionTier.TIER_5_DEGRADED)
                .processingStatus(FileProcessingStatus.UNRECOGNIZED_SYNTAX)
                .diagnostics("Syntax not recognized by earlier tiers; file preserved as FILE node")
                .build();
    }
}