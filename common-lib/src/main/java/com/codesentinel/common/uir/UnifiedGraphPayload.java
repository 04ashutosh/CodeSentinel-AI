package com.codesentinel.common.uir;

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
public class UnifiedGraphPayload {
    @Builder.Default
    private String schemaVersion = "1.0";
    private String projectId;

    @Builder.Default
    private List<FileInventoryRecord> inventory = new ArrayList<>();

    @Builder.Default
    private List<UnifiedNode> nodes = new ArrayList<>();

    @Builder.Default
    private List<UnifiedRelationship> relationships = new ArrayList<>();
}