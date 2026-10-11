package com.codesentinel.common.uir;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UnifiedNode {
    private String id;
    private String projectId;
    private String fileId;
    private NodeCategory category;
    private String language;
    private String name;
    private String qualifiedName;
    private SourceRange sourceRange;
    private ExtractionTier extractionTier;
    private Double confidence;
    private VerificationStatus verificationStatus;
    private String customCategory;

    @Builder.Default
    private Map<String, Object> properties = new HashMap<>();
}