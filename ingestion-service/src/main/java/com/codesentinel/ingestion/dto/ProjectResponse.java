package com.codesentinel.ingestion.dto;

import com.codesentinel.ingestion.enums.IngestionStatus;
import com.codesentinel.ingestion.enums.SourceType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProjectResponse {
    private Long id;
    private String name;
    private SourceType sourceType;
    private IngestionStatus status;
    private String errorMessage;
    private LocalDateTime createdAt;
}