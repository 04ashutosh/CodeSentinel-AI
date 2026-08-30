package com.codesentinel.parser.controller;

import com.codesentinel.common.dto.ApiResponse;
import com.codesentinel.parser.dto.ClassMetadataResponse;
import com.codesentinel.parser.entity.ParsedClass;
import com.codesentinel.parser.enums.ClassType;
import com.codesentinel.parser.repository.ParsedClassRepository;
import com.codesentinel.parser.service.JavaParserService;
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

    private final JavaParserService javaParserService;
    private final ParsedClassRepository parsedClassRepository;

    @PostMapping("/parse/{projectId}")
    public ResponseEntity<ApiResponse<List<ClassMetadataResponse>>> parseProject(
            @PathVariable Long projectId,
            @RequestParam String storagePath) {
        List<ParsedClass> results = javaParserService.parseProject(projectId, storagePath);
        List<ClassMetadataResponse> response = results.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.success("Project parsed successfully", response));
    }

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