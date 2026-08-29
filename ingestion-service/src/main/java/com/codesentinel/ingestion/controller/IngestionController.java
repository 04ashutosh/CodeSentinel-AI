package com.codesentinel.ingestion.controller;

import com.codesentinel.common.dto.ApiResponse;
import com.codesentinel.ingestion.dto.ProjectResponse;
import com.codesentinel.ingestion.service.IngestionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/v1/ingestion")
@RequiredArgsConstructor
public class IngestionController {

    private final IngestionService ingestionService;

    @PostMapping("/upload")
    public ResponseEntity<ApiResponse<ProjectResponse>> uploadZip(
            @RequestParam("file") MultipartFile file,
            @RequestParam("name") String projectName,
            @RequestHeader("X-User-Email") String userEmail) {

        ProjectResponse response = ingestionService.uploadZip(file, projectName, userEmail);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Project uploaded successfully", response));
    }

    @GetMapping("/projects/{id}")
    public ResponseEntity<ApiResponse<ProjectResponse>> getProject(@PathVariable Long id) {
        ProjectResponse response = ingestionService.getProject(id);
        return ResponseEntity.ok(ApiResponse.success("Project retrieved", response));
    }

    @GetMapping("/projects")
    public ResponseEntity<ApiResponse<List<ProjectResponse>>> getUserProjects(
            @RequestHeader("X-User-Email") String userEmail) {
        List<ProjectResponse> projects = ingestionService.getUserProjects(userEmail);
        return ResponseEntity.ok(ApiResponse.success("Projects retrieved", projects));
    }
}