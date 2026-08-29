package com.codesentinel.ingestion.service;

import com.codesentinel.common.exception.ResourceNotFoundException;
import com.codesentinel.ingestion.dto.ProjectResponse;
import com.codesentinel.ingestion.entity.Project;
import com.codesentinel.ingestion.enums.IngestionStatus;
import com.codesentinel.ingestion.enums.SourceType;
import com.codesentinel.ingestion.event.IngestionCompletedEvent;
import com.codesentinel.ingestion.repository.ProjectRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.*;
import java.util.List;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Service
@RequiredArgsConstructor
@Slf4j
public class IngestionService {

    private final ProjectRepository projectRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${ingestion.storage-path}")
    private String storagePath;

    public ProjectResponse uploadZip(MultipartFile file, String projectName, String userEmail) {
        // 1. Create project record with PENDING status
        Project project = Project.builder()
                .name(projectName)
                .userEmail(userEmail)
                .sourceType(SourceType.ZIP_UPLOAD)
                .status(IngestionStatus.PROCESSING)
                .build();
        project = projectRepository.save(project);

        try {
            // 2. Create a unique directory for this project
            Path projectDir = Paths.get(storagePath, project.getId().toString());
            Files.createDirectories(projectDir);

            // 3. Extract the ZIP file
            extractZip(file, projectDir);

            // 4. Update project with storage path and COMPLETED status
            project.setStoragePath(projectDir.toString());
            project.setStatus(IngestionStatus.COMPLETED);
            projectRepository.save(project);

            // 5. Publish Kafka event so Parser Service picks it up
            IngestionCompletedEvent event = IngestionCompletedEvent.builder()
                    .projectId(project.getId())
                    .projectName(project.getName())
                    .storagePath(projectDir.toString())
                    .userEmail(userEmail)
                    .source("ingestion-service")
                    .build();
            event.initDefaults();

            kafkaTemplate.send("ingestion.completed", event);
            log.info("Published ingestion.completed event for project: {}", project.getId());

        } catch (Exception e) {
            // If anything goes wrong, mark as FAILED
            project.setStatus(IngestionStatus.FAILED);
            project.setErrorMessage(e.getMessage());
            projectRepository.save(project);
            log.error("Failed to process upload for project: {}", project.getId(), e);
        }

        return toResponse(project);
    }

    public ProjectResponse getProject(Long projectId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found with id: " + projectId));
        return toResponse(project);
    }

    public List<ProjectResponse> getUserProjects(String userEmail) {
        return projectRepository.findByUserEmailOrderByCreatedAtDesc(userEmail)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    private void extractZip(MultipartFile file, Path targetDir) throws IOException {
        try (ZipInputStream zis = new ZipInputStream(file.getInputStream())) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                Path entryPath = targetDir.resolve(entry.getName()).normalize();

                // Security check: prevent Zip Slip attack
                if (!entryPath.startsWith(targetDir)) {
                    throw new IOException("Bad zip entry: " + entry.getName());
                }

                if (entry.isDirectory()) {
                    Files.createDirectories(entryPath);
                } else {
                    Files.createDirectories(entryPath.getParent());
                    Files.copy(zis, entryPath, StandardCopyOption.REPLACE_EXISTING);
                }
                zis.closeEntry();
            }
        }
    }

    private ProjectResponse toResponse(Project project) {
        return ProjectResponse.builder()
                .id(project.getId())
                .name(project.getName())
                .sourceType(project.getSourceType())
                .status(project.getStatus())
                .errorMessage(project.getErrorMessage())
                .createdAt(project.getCreatedAt())
                .build();
    }
}