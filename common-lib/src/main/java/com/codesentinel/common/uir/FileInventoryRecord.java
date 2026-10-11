package com.codesentinel.common.uir;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FileInventoryRecord {
    private String projectId;
    private String fileId;
    private String extension;
    private String detectedLanguage;
    private String detectionMethod;
    private Double detectionConfidence;
    private FileClassification classification;
    private String contentHash;
    private ExtractionTier analysisStrategy;
    private FileProcessingStatus processingStatus;
    private String diagnostics;
    private int extractedNodeCount;
    private int extractedRelationshipCount;
}