package com.codesentinel.parser.engine;

import com.codesentinel.common.uir.ExtractionTier;
import com.codesentinel.common.uir.FileProcessingStatus;
import com.codesentinel.common.uir.UnifiedNode;
import com.codesentinel.common.uir.UnifiedRelationship;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ParsedFileResult {
    @Builder.Default
    private List<UnifiedNode> nodes = new ArrayList<>();

    @Builder.Default
    private List<UnifiedRelationship> relationships = new ArrayList<>();

    private ExtractionTier extractionTier;
    private FileProcessingStatus processingStatus;
    private String diagnostics;
}