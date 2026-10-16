package com.codesentinel.parser.entity;

import com.codesentinel.common.uir.ExtractionTier;
import com.codesentinel.common.uir.FileClassification;
import com.codesentinel.common.uir.FileProcessingStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "file_inventory", indexes = {
        @Index(name = "idx_inv_project_id", columnList = "projectId"),
        @Index(name = "idx_inv_file_id", columnList = "fileId")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FileInventoryEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long projectId;

    @Column(nullable = false, length = 1000)
    private String fileId;

    private String extension;

    private String detectedLanguage;

    private String detectionMethod;

    private Double detectionConfidence;

    @Enumerated(EnumType.STRING)
    private FileClassification classification;

    private String contentHash;

    @Enumerated(EnumType.STRING)
    private ExtractionTier analysisStrategy;

    @Enumerated(EnumType.STRING)
    private FileProcessingStatus processingStatus;

    @Column(columnDefinition = "TEXT")
    private String diagnostics;

    private int extractedNodeCount;

    private int extractedRelationshipCount;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}