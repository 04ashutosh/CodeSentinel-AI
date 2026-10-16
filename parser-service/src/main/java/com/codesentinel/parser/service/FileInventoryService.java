package com.codesentinel.parser.service;

import com.codesentinel.common.uir.ExtractionTier;
import com.codesentinel.common.uir.FileClassification;
import com.codesentinel.common.uir.FileInventoryRecord;
import com.codesentinel.common.uir.FileProcessingStatus;
import com.codesentinel.parser.detector.DetectionResult;
import com.codesentinel.parser.detector.LanguageDetectionService;
import com.codesentinel.parser.entity.FileInventoryEntity;
import com.codesentinel.parser.repository.FileInventoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class FileInventoryService {

    private final LanguageDetectionService detectionService;
    private final FileInventoryRepository inventoryRepository;

    private static final Set<String> EXCLUDED_DIRECTORIES = Set.of(
            ".git", "node_modules", "target", "build", "bin", ".idea", ".vscode", "dist", ".angular"
    );

    @Transactional
    public List<FileInventoryRecord> crawlAndIndex(Long projectId, Path projectDir) {
        log.info("Starting file inventory crawl for project {} in {}", projectId, projectDir);
        inventoryRepository.deleteByProjectId(projectId);

        List<FileInventoryRecord> records = new ArrayList<>();
        List<FileInventoryEntity> entities = new ArrayList<>();

        try {
            Files.walkFileTree(projectDir, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                    if (EXCLUDED_DIRECTORIES.contains(dir.getFileName().toString())) {
                        return FileVisitResult.SKIP_SUBTREE;
                    }
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                    try {
                        String relativePath = projectDir.relativize(file).toString().replace('\\', '/');
                        String fileName = file.getFileName().toString();
                        String extension = fileName.contains(".") ? fileName.substring(fileName.lastIndexOf('.') + 1) : "";
                        String hash = computeSha256(file);

                        DetectionResult detection = detectionService.detect(file);

                        ExtractionTier defaultTier = detection.isBinary()
                                ? ExtractionTier.EXCLUDED_BINARY
                                : (detection.getDetectedLanguage().equals("java")
                                   ? ExtractionTier.TIER_1_DEDICATED
                                   : ExtractionTier.TIER_3_GENERIC);

                        FileProcessingStatus initialStatus = detection.isBinary()
                                ? FileProcessingStatus.BINARY_EXCLUDED
                                : FileProcessingStatus.PARSED_SUCCESS;

                        FileInventoryRecord record = FileInventoryRecord.builder()
                                .projectId(String.valueOf(projectId))
                                .fileId(relativePath)
                                .extension(extension)
                                .detectedLanguage(detection.getDetectedLanguage())
                                .detectionMethod(detection.getDetectionMethod())
                                .detectionConfidence(detection.getConfidence())
                                .classification(detection.getClassification())
                                .contentHash(hash)
                                .analysisStrategy(defaultTier)
                                .processingStatus(initialStatus)
                                .diagnostics("")
                                .extractedNodeCount(0)
                                .extractedRelationshipCount(0)
                                .build();

                        records.add(record);

                        FileInventoryEntity entity = FileInventoryEntity.builder()
                                .projectId(projectId)
                                .fileId(relativePath)
                                .extension(extension)
                                .detectedLanguage(detection.getDetectedLanguage())
                                .detectionMethod(detection.getDetectionMethod())
                                .detectionConfidence(detection.getConfidence())
                                .classification(detection.getClassification())
                                .contentHash(hash)
                                .analysisStrategy(defaultTier)
                                .processingStatus(initialStatus)
                                .diagnostics("")
                                .extractedNodeCount(0)
                                .extractedRelationshipCount(0)
                                .build();

                        entities.add(entity);
                    } catch (Exception e) {
                        log.warn("Failed to index file {}: {}", file, e.getMessage());
                    }
                    return FileVisitResult.CONTINUE;
                }
            });

            inventoryRepository.saveAll(entities);
            log.info("Completed inventory crawl for project {}: {} files indexed", projectId, records.size());

        } catch (Exception e) {
            log.error("Error walking directory for project {}: {}", projectId, e.getMessage(), e);
        }

        return records;
    }

    private String computeSha256(Path path) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            try (InputStream is = Files.newInputStream(path);
                 DigestInputStream dis = new DigestInputStream(is, md)) {
                byte[] buffer = new byte[8192];
                while (dis.read(buffer) != -1) {}
            }
            byte[] digest = md.digest();
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            return "UNKNOWN_HASH";
        }
    }
}