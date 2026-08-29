package com.codesentinel.ingestion.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class IngestionRequest {

    @NotBlank(message = "Project name is required")
    private String name;

    @NotBlank(message = "GitHub URL is required")
    private String githubUrl;
}