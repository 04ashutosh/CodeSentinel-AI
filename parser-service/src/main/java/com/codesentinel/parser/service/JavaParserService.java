package com.codesentinel.parser.service;

import com.codesentinel.parser.entity.ParsedClass;
import com.codesentinel.parser.enums.ClassType;
import com.codesentinel.parser.repository.ParsedClassRepository;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.EnumDeclaration;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class JavaParserService {

    private final ParsedClassRepository parsedClassRepository;

    @Transactional
    public List<ParsedClass> parseProject(Long projectId, String storagePath) {
        List<ParsedClass> results = new ArrayList<>();

        // Delete any previous parsing results for this project (re-parse support)
        parsedClassRepository.deleteByProjectId(projectId);

        try {
            // Walk through ALL .java files in the project directory
            Path projectDir = Paths.get(storagePath);
            List<Path> javaFiles = new ArrayList<>();

            Files.walkFileTree(projectDir, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                    if (file.toString().endsWith(".java")) {
                        javaFiles.add(file);
                    }
                    return FileVisitResult.CONTINUE;
                }
            });

            log.info("Found {} .java files in project {}", javaFiles.size(), projectId);

            // Parse each .java file
            for (Path javaFile : javaFiles) {
                try {
                    List<ParsedClass> parsed = parseFile(projectId, javaFile, projectDir);
                    results.addAll(parsed);
                } catch (Exception e) {
                    log.warn("Failed to parse file: {}. Error: {}", javaFile, e.getMessage());
                }
            }

            // Save all parsed classes to database
            parsedClassRepository.saveAll(results);
            log.info("Saved {} parsed classes for project {}", results.size(), projectId);

        } catch (IOException e) {
            log.error("Failed to walk project directory: {}", storagePath, e);
        }

        return results;
    }

    private List<ParsedClass> parseFile(Long projectId, Path filePath, Path projectDir) throws IOException {
        List<ParsedClass> results = new ArrayList<>();
        CompilationUnit cu = StaticJavaParser.parse(filePath);

        String packageName = cu.getPackageDeclaration()
                .map(pd -> pd.getNameAsString())
                .orElse("");

        String relativePath = projectDir.relativize(filePath).toString();

        // Process classes and interfaces
        cu.findAll(ClassOrInterfaceDeclaration.class).forEach(classDecl -> {
            List<String> annotations = classDecl.getAnnotations().stream()
                    .map(a -> a.getNameAsString())
                    .collect(Collectors.toList());

            ClassType classType = determineClassType(classDecl, annotations);

            List<String> interfaces = classDecl.getImplementedTypes().stream()
                    .map(t -> t.getNameAsString())
                    .collect(Collectors.toList());

            String extendsClass = classDecl.getExtendedTypes().stream()
                    .findFirst()
                    .map(t -> t.getNameAsString())
                    .orElse(null);

            ParsedClass parsed = ParsedClass.builder()
                    .projectId(projectId)
                    .className(classDecl.getNameAsString())
                    .packageName(packageName)
                    .classType(classType)
                    .filePath(relativePath)
                    .annotations(String.join(",", annotations))
                    .methodCount(classDecl.getMethods().size())
                    .fieldCount(classDecl.getFields().size())
                    .implementsInterfaces(String.join(",", interfaces))
                    .extendsClass(extendsClass)
                    .build();

            results.add(parsed);
        });

        // Process enums
        cu.findAll(EnumDeclaration.class).forEach(enumDecl -> {
            List<String> annotations = enumDecl.getAnnotations().stream()
                    .map(a -> a.getNameAsString())
                    .collect(Collectors.toList());

            ParsedClass parsed = ParsedClass.builder()
                    .projectId(projectId)
                    .className(enumDecl.getNameAsString())
                    .packageName(packageName)
                    .classType(ClassType.ENUM)
                    .filePath(relativePath)
                    .annotations(String.join(",", annotations))
                    .methodCount(enumDecl.getMethods().size())
                    .fieldCount(enumDecl.getEntries().size())
                    .build();

            results.add(parsed);
        });

        return results;
    }

    private ClassType determineClassType(ClassOrInterfaceDeclaration classDecl, List<String> annotations) {
        if (classDecl.isInterface()) return ClassType.INTERFACE;
        if (annotations.contains("RestController")) return ClassType.REST_CONTROLLER;
        if (annotations.contains("Controller")) return ClassType.CONTROLLER;
        if (annotations.contains("Service")) return ClassType.SERVICE;
        if (annotations.contains("Repository")) return ClassType.REPOSITORY;
        if (annotations.contains("Entity")) return ClassType.ENTITY;
        if (annotations.contains("Configuration")) return ClassType.CONFIGURATION;
        if (annotations.contains("Component")) return ClassType.COMPONENT;
        return ClassType.OTHER;
    }
}