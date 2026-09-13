package com.codesentinel.ingestion.dto;

import lombok.Data;

@Data
public class GitIngestionRequest {
    private String repoUrl;
    private String branch;
    private String projectName;
}