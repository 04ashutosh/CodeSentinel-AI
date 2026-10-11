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
public class UnifiedRelationship {
    private String id;
    private String projectId;
    private String sourceId;
    private String targetId;
    private RelationshipType type;
    private String customType;
    private Double confidence;
    private VerificationStatus verificationStatus;
    private ExtractionTier extractionTier;

    @Builder.Default
    private Map<String, Object> properties = new HashMap<>();
}