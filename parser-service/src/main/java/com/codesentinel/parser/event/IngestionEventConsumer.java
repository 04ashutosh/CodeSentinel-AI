package com.codesentinel.parser.event;

import com.codesentinel.parser.service.JavaParserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class IngestionEventConsumer {

    private final JavaParserService javaParserService;

    @KafkaListener(topics = "ingestion.completed", groupId = "parser-service-group")
    public void onIngestionCompleted(Map<String, Object> event) {
        try {
            Long projectId = Long.valueOf(event.get("projectId").toString());
            String storagePath = event.get("storagePath").toString();
            String projectName = event.get("projectName").toString();

            log.info("Received ingestion.completed event for project: {} (id: {})", projectName, projectId);

            javaParserService.parseProject(projectId, storagePath);

            log.info("Finished parsing project: {} (id: {})", projectName, projectId);
        } catch (Exception e) {
            log.error("Failed to process ingestion event: {}", e.getMessage(), e);
        }
    }
}