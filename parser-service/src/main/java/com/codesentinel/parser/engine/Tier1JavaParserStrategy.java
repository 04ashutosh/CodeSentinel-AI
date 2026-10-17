package com.codesentinel.parser.engine;

import com.codesentinel.common.uir.*;
import com.codesentinel.parser.entity.ParsedClass;
import com.codesentinel.parser.enums.ClassType;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.EnumDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

@Component
@Slf4j
public class Tier1JavaParserStrategy implements ParserStrategy {

    @Getter
    private final List<ParsedClass> legacyParsedClasses = new ArrayList<>();

    @Override
    public boolean supports(String detectedLanguage, Path filePath) {
        return "java".equalsIgnoreCase(detectedLanguage) || filePath.toString().endsWith(".java");
    }

    @Override
    public ExtractionTier getTier() {
        return ExtractionTier.TIER_1_DEDICATED;
    }

    @Override
    public ParsedFileResult parse(Long projectId, Path filePath, Path projectDir, String detectedLanguage) throws Exception {
        List<UnifiedNode> nodes = new ArrayList<>();
        List<UnifiedRelationship> relationships = new ArrayList<>();
        String relativePath = projectDir.relativize(filePath).toString().replace('\\', '/');
        String fileNodeId = "proj-" + projectId + ":file-" + relativePath;

        CompilationUnit cu = StaticJavaParser.parse(filePath);

        // Package Node
        String packageName = cu.getPackageDeclaration()
                .map(pd -> pd.getNameAsString())
                .orElse("default");
        String packageNodeId = "proj-" + projectId + ":pkg-" + packageName;

        nodes.add(UnifiedNode.builder()
                .id(packageNodeId)
                .projectId(String.valueOf(projectId))
                .fileId(relativePath)
                .category(NodeCategory.PACKAGE)
                .language("java")
                .name(packageName)
                .qualifiedName(packageName)
                .extractionTier(ExtractionTier.TIER_1_DEDICATED)
                .confidence(1.0)
                .verificationStatus(VerificationStatus.VERIFIED_FACT)
                .build());

        // Process Classes & Interfaces
        for (ClassOrInterfaceDeclaration decl : cu.findAll(ClassOrInterfaceDeclaration.class)) {
            String className = decl.getNameAsString();
            String classQualifiedName = packageName.equals("default") ? className : packageName + "." + className;
            String classNodeId = "proj-" + projectId + ":class-" + classQualifiedName;
            boolean isInterface = decl.isInterface();

            List<String> annotations = decl.getAnnotations().stream()
                    .map(a -> a.getNameAsString())
                    .collect(Collectors.toList());

            SourceRange classRange = decl.getRange().map(r -> SourceRange.builder()
                    .startLine(r.begin.line)
                    .endLine(r.end.line)
                    .startColumn(r.begin.column)
                    .endColumn(r.end.column)
                    .build()).orElse(null);

            Map<String, Object> props = new HashMap<>();
            props.put("isInterface", isInterface);
            props.put("annotations", annotations);
            props.put("methodCount", decl.getMethods().size());
            props.put("fieldCount", decl.getFields().size());

            nodes.add(UnifiedNode.builder()
                    .id(classNodeId)
                    .projectId(String.valueOf(projectId))
                    .fileId(relativePath)
                    .category(isInterface ? NodeCategory.INTERFACE : NodeCategory.CLASS)
                    .language("java")
                    .name(className)
                    .qualifiedName(classQualifiedName)
                    .sourceRange(classRange)
                    .extractionTier(ExtractionTier.TIER_1_DEDICATED)
                    .confidence(1.0)
                    .verificationStatus(VerificationStatus.VERIFIED_FACT)
                    .properties(props)
                    .build());

            // Package CONTAINS Class
            relationships.add(UnifiedRelationship.builder()
                    .id("rel:" + packageNodeId + "->CONTAINS->" + classNodeId)
                    .projectId(String.valueOf(projectId))
                    .sourceId(packageNodeId)
                    .targetId(classNodeId)
                    .type(RelationshipType.CONTAINS)
                    .confidence(1.0)
                    .verificationStatus(VerificationStatus.VERIFIED_FACT)
                    .extractionTier(ExtractionTier.TIER_1_DEDICATED)
                    .build());

            // Methods inside Class
            for (MethodDeclaration method : decl.getMethods()) {
                String methodName = method.getNameAsString();
                String methodNodeId = classNodeId + ":m-" + methodName + ":" + method.getParameters().size();

                SourceRange methodRange = method.getRange().map(r -> SourceRange.builder()
                        .startLine(r.begin.line)
                        .endLine(r.end.line)
                        .startColumn(r.begin.column)
                        .endColumn(r.end.column)
                        .build()).orElse(null);

                nodes.add(UnifiedNode.builder()
                        .id(methodNodeId)
                        .projectId(String.valueOf(projectId))
                        .fileId(relativePath)
                        .category(NodeCategory.METHOD)
                        .language("java")
                        .name(methodName)
                        .qualifiedName(classQualifiedName + "." + methodName)
                        .sourceRange(methodRange)
                        .extractionTier(ExtractionTier.TIER_1_DEDICATED)
                        .confidence(1.0)
                        .verificationStatus(VerificationStatus.VERIFIED_FACT)
                        .properties(Map.of("returnType", method.getTypeAsString()))
                        .build());

                relationships.add(UnifiedRelationship.builder()
                        .id("rel:" + classNodeId + "->DEFINES->" + methodNodeId)
                        .projectId(String.valueOf(projectId))
                        .sourceId(classNodeId)
                        .targetId(methodNodeId)
                        .type(RelationshipType.DEFINES)
                        .confidence(1.0)
                        .verificationStatus(VerificationStatus.VERIFIED_FACT)
                        .extractionTier(ExtractionTier.TIER_1_DEDICATED)
                        .build());
            }

            // Extends & Implements Relationships
            decl.getExtendedTypes().forEach(ext -> {
                String parentName = ext.getNameAsString();
                String parentId = "proj-" + projectId + ":class-" + parentName;
                relationships.add(UnifiedRelationship.builder()
                        .id("rel:" + classNodeId + "->EXTENDS->" + parentId)
                        .projectId(String.valueOf(projectId))
                        .sourceId(classNodeId)
                        .targetId(parentId)
                        .type(RelationshipType.EXTENDS)
                        .confidence(1.0)
                        .verificationStatus(VerificationStatus.VERIFIED_FACT)
                        .extractionTier(ExtractionTier.TIER_1_DEDICATED)
                        .build());
            });

            decl.getImplementedTypes().forEach(impl -> {
                String interfaceName = impl.getNameAsString();
                String interfaceId = "proj-" + projectId + ":class-" + interfaceName;
                relationships.add(UnifiedRelationship.builder()
                        .id("rel:" + classNodeId + "->IMPLEMENTS->" + interfaceId)
                        .projectId(String.valueOf(projectId))
                        .sourceId(classNodeId)
                        .targetId(interfaceId)
                        .type(RelationshipType.IMPLEMENTS)
                        .confidence(1.0)
                        .verificationStatus(VerificationStatus.VERIFIED_FACT)
                        .extractionTier(ExtractionTier.TIER_1_DEDICATED)
                        .build());
            });

            // Backward Compatibility: Populate legacy ParsedClass
            List<String> interfaces = decl.getImplementedTypes().stream()
                    .map(t -> t.getNameAsString()).collect(Collectors.toList());
            String extendsClass = decl.getExtendedTypes().stream().findFirst().map(t -> t.getNameAsString()).orElse(null);

            legacyParsedClasses.add(ParsedClass.builder()
                    .projectId(projectId)
                    .className(className)
                    .packageName(packageName)
                    .classType(determineClassType(decl, annotations))
                    .filePath(relativePath)
                    .annotations(String.join(",", annotations))
                    .methodCount(decl.getMethods().size())
                    .fieldCount(decl.getFields().size())
                    .implementsInterfaces(String.join(",", interfaces))
                    .extendsClass(extendsClass)
                    .build());
        }

        return ParsedFileResult.builder()
                .nodes(nodes)
                .relationships(relationships)
                .extractionTier(ExtractionTier.TIER_1_DEDICATED)
                .processingStatus(FileProcessingStatus.PARSED_SUCCESS)
                .diagnostics("Successfully parsed with JavaParser")
                .build();
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