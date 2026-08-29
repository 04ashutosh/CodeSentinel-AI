package com.codesentinel.ingestion.entity;

import com.codesentinel.ingestion.enums.IngestionStatus;
import com.codesentinel.ingestion.enums.SourceType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "projects")
public class Project {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(name = "user_email", nullable = false)
    private String userEmail;

    @Enumerated(EnumType.STRING)
    @Column(name = "source_type", nullable = false)
    private SourceType sourceType;

    @Column(name = "source_path")
    private String sourcePath;

    @Column(name = "storage_path")
    private String storagePath;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private IngestionStatus status;

    @Column(name = "error_message")
    private String errorMessage;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}