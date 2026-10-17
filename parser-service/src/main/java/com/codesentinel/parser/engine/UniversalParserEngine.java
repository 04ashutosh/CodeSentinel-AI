package com.codesentinel.parser.engine;

import com.codesentinel.common.uir.*;
import com.codesentinel.parser.entity.FileInventoryEntity;
import com.codesentinel.parser.entity.ParsedClass;
import com.codesentinel.parser.repository.FileInventoryRepository;
import com.codesentinel.parser.repository.ParsedClassRepository;
import com.codesentinel.parser.service.FileInventoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class UniversalParserEngine {

    private final FileInventoryService inventoryService;
    private final FileInventoryRepository inventoryRepository;
    private final ParsedClassRepository parsedClassRepository;
    private final Tier1JavaParserStrategy javaParserStrategy;
    private final Tier3GenericStructuralParser genericStructuralParser;
    private final Tier4LlmParserStrategy llmParserStrategy;
    private final Tier5DegradedParserStrategy degradedParserStrategy;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    // Cache latest graph payload in memory for fast REST querying
    private final Map<Long, UnifiedGraphPayload> payloadCache = new HashMap<>();

    @Transactional
    public UnifiedGraphPayload parseProject(Long projectId, String storagePath) {
        log.info("Starting Universal Multi-Language Parsing for project {} at {}", projectId, storagePath);
        Path projectDir = Paths.get(storagePath);

        // 1. Crawl & Index inventory
        List<FileInventoryRecord> inventory = inventoryService.crawlAndIndex(projectId, projectDir);

        // Clear legacy tables for re-ingestion
        parsedClassRepository.deleteByProjectId(projectId);
        javaParserStrategy.getLegacyParsedClasses().clear();

        List<UnifiedNode> allNodes = new ArrayList<>();
        List<UnifiedRelationship> allRelationships = new ArrayList<>();

        // Add root PROJECT node
        String projectNodeId = "proj-" + projectId;
        allNodes.add(UnifiedNode.builder()
                .id(projectNodeId)
                .projectId(String.valueOf(projectId))
                .fileId("")
                .category(NodeCategory.PROJECT)
                .language("multi")
                .name("Project-" + projectId)
                .extractionTier(ExtractionTier.TIER_1_DEDICATED)
                .confidence(1.0)
                .verificationStatus(VerificationStatus.VERIFIED_FACT)
                .build());

        // 2. Parse eligible files
        for (FileInventoryRecord record : inventory) {
            if (record.getClassification() == FileClassification.BINARY) {
                continue;
            }

            Path filePath = projectDir.resolve(record.getFileId());
            String lang = record.getDetectedLanguage();
            String fileNodeId = "proj-" + projectId + ":file-" + record.getFileId();

            // Add FILE node
            allNodes.add(UnifiedNode.builder()
                    .id(fileNodeId)
                    .projectId(String.valueOf(projectId))
                    .fileId(record.getFileId())
                    .category(NodeCategory.FILE)
                    .language(lang)
                    .name(filePath.getFileName().toString())
                    .qualifiedName(record.getFileId())
                    .extractionTier(record.getAnalysisStrategy())
                    .confidence(record.getDetectionConfidence())
                    .verificationStatus(VerificationStatus.VERIFIED_FACT)
                    .build());

            // Link PROJECT -> CONTAINS -> FILE
            allRelationships.add(UnifiedRelationship.builder()
                    .id("rel:" + projectNodeId + "->CONTAINS->" + fileNodeId)
                    .projectId(String.valueOf(projectId))
                    .sourceId(projectNodeId)
                    .targetId(fileNodeId)
                    .type(RelationshipType.CONTAINS)
                    .confidence(1.0)
                    .verificationStatus(VerificationStatus.VERIFIED_FACT)
                    .extractionTier(ExtractionTier.TIER_1_DEDICATED)
                    .build());

            ParsedFileResult result;
            try {
                if (javaParserStrategy.supports(lang, filePath)) {
                    result = javaParserStrategy.parse(projectId, filePath, projectDir, lang);
                } else {
                    result = genericStructuralParser.parse(projectId, filePath, projectDir, lang);
                    if (result.getNodes().isEmpty() && "unknown_text".equalsIgnoreCase(lang)) {
                        result = llmParserStrategy.parse(projectId, filePath, projectDir, lang);
                    }
                    if (result.getNodes().isEmpty()) {
                        result = degradedParserStrategy.parse(projectId, filePath, projectDir, lang);
                    }
                }
            } catch (Exception e) {
                log.warn("Error parsing {}: {}", record.getFileId(), e.getMessage());
                try {
                    result = degradedParserStrategy.parse(projectId, filePath, projectDir, lang);
                    result.setDiagnostics("Error during parsing: " + e.getMessage());
                } catch (Exception ex) {
                    result = ParsedFileResult.builder()
                            .nodes(Collections.emptyList())
                            .relationships(Collections.emptyList())
                            .extractionTier(ExtractionTier.TIER_5_DEGRADED)
                            .processingStatus(FileProcessingStatus.FAILED)
                            .diagnostics(e.getMessage())
                            .build();
                }
            }

            allNodes.addAll(result.getNodes());
            allRelationships.addAll(result.getRelationships());

            // Update inventory record with counts
            record.setExtractedNodeCount(result.getNodes().size());
            record.setExtractedRelationshipCount(result.getRelationships().size());
            record.setAnalysisStrategy(result.getExtractionTier());
            record.setProcessingStatus(result.getProcessingStatus());
            record.setDiagnostics(result.getDiagnostics());
        }

        // Save legacy classes (backward compatibility)
        if (!javaParserStrategy.getLegacyParsedClasses().isEmpty()) {
            parsedClassRepository.saveAll(new ArrayList<>(javaParserStrategy.getLegacyParsedClasses()));
        }

        // 3. Assemble UnifiedGraphPayload
        UnifiedGraphPayload payload = UnifiedGraphPayload.builder()
                .schemaVersion("1.0")
                .projectId(String.valueOf(projectId))
                .inventory(inventory)
                .nodes(allNodes)
                .relationships(allRelationships)
                .build();

        payloadCache.put(projectId, payload);

        // 4. Publish Kafka Events
        // A) Unified graph payload event
        log.info("Publishing graph.update event for project {} (nodes: {}, edges: {})",
                projectId, allNodes.size(), allRelationships.size());
        kafkaTemplate.send("graph.update", Map.of(
                "projectId", projectId,
                "status", "SUCCESS",
                "totalNodes", allNodes.size(),
                "totalEdges", allRelationships.size()
        ));

        // B) Legacy parsing.completed event (backward compatibility)
        kafkaTemplate.send("parsing.completed", Map.of(
                "projectId", projectId,
                "status", "SUCCESS",
                "totalClasses", javaParserStrategy.getLegacyParsedClasses().size()
        ));

        return payload;
    }

    public UnifiedGraphPayload getProjectGraph(Long projectId) {
        return payloadCache.get(projectId);
    }
}