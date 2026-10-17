package com.codesentinel.parser.controller;

import com.codesentinel.common.dto.ApiResponse;
import com.codesentinel.common.uir.FileInventoryRecord;
import com.codesentinel.common.uir.UnifiedGraphPayload;
import com.codesentinel.parser.dto.ClassMetadataResponse;
import com.codesentinel.parser.engine.UniversalParserEngine;
import com.codesentinel.parser.entity.FileInventoryEntity;
import com.codesentinel.parser.entity.ParsedClass;
import com.codesentinel.parser.enums.ClassType;
import com.codesentinel.parser.repository.FileInventoryRepository;
import com.codesentinel.parser.repository.ParsedClassRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/parser")
@RequiredArgsConstructor
public class ParserController {

    private final UniversalParserEngine universalParserEngine;
    private final ParsedClassRepository parsedClassRepository;
    private final FileInventoryRepository inventoryRepository;

    @PostMapping("/parse/{projectId}")
    public ResponseEntity<ApiResponse<UnifiedGraphPayload>> parseProject(
            @PathVariable Long projectId,
            @RequestParam String storagePath) {
        UnifiedGraphPayload payload = universalParserEngine.parseProject(projectId, storagePath);
        return ResponseEntity.ok(ApiResponse.success("Project parsed successfully", payload));
    }

    @GetMapping("/projects/{projectId}/graph")
    public ResponseEntity<ApiResponse<UnifiedGraphPayload>> getProjectGraph(
            @PathVariable Long projectId) {
        UnifiedGraphPayload payload = universalParserEngine.getProjectGraph(projectId);
        return ResponseEntity.ok(ApiResponse.success("Project graph retrieved", payload));
    }

    @GetMapping("/projects/{projectId}/inventory")
    public ResponseEntity<ApiResponse<List<FileInventoryEntity>>> getProjectInventory(
            @PathVariable Long projectId) {
        List<FileInventoryEntity> inventory = inventoryRepository.findByProjectId(projectId);
        return ResponseEntity.ok(ApiResponse.success("Project inventory retrieved", inventory));
    }

    // ==========================================
    // BACKWARD-COMPATIBLE ENDPOINTS
    // ==========================================
    @GetMapping("/projects/{projectId}/classes")
    public ResponseEntity<ApiResponse<List<ClassMetadataResponse>>> getProjectClasses(
            @PathVariable Long projectId) {
        List<ParsedClass> classes = parsedClassRepository.findByProjectId(projectId);
        List<ClassMetadataResponse> response = classes.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.success("Classes retrieved", response));
    }

    @GetMapping("/projects/{projectId}/classes/{type}")
    public ResponseEntity<ApiResponse<List<ClassMetadataResponse>>> getProjectClassesByType(
            @PathVariable Long projectId,
            @PathVariable ClassType type) {
        List<ParsedClass> classes = parsedClassRepository.findByProjectIdAndClassType(projectId, type);
        List<ClassMetadataResponse> response = classes.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.success("Classes retrieved", response));
    }

    private ClassMetadataResponse toResponse(ParsedClass parsed) {
        return ClassMetadataResponse.builder()
                .id(parsed.getId())
                .className(parsed.getClassName())
                .packageName(parsed.getPackageName())
                .classType(parsed.getClassType())
                .filePath(parsed.getFilePath())
                .annotations(parsed.getAnnotations() != null ?
                        Arrays.asList(parsed.getAnnotations().split(",")) : List.of())
                .methodCount(parsed.getMethodCount())
                .fieldCount(parsed.getFieldCount())
                .implementsInterfaces(parsed.getImplementsInterfaces() != null ?
                        Arrays.asList(parsed.getImplementsInterfaces().split(",")) : List.of())
                .extendsClass(parsed.getExtendsClass())
                .build();
    }
}