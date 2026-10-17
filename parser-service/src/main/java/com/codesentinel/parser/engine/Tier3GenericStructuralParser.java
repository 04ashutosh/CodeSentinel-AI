package com.codesentinel.parser.engine;

import com.codesentinel.common.uir.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
@Slf4j
public class Tier3GenericStructuralParser implements ParserStrategy {

    // Regex patterns for diverse paradigms
    private static final Pattern FUNCTION_PATTERN = Pattern.compile(
            "^\\s*(?:(?:public|private|protected|static|async|def|fn|func|function|sub|procedure)\\s+)+([a-zA-Z_][a-zA-Z0-9_]*)\\s*\\(",
            Pattern.CASE_INSENSITIVE
    );

    private static final Pattern TYPE_CLASS_PATTERN = Pattern.compile(
            "^\\s*(?:(?:public|private|protected|abstract|class|struct|interface|type|enum|record)\\s+)+([a-zA-Z_][a-zA-Z0-9_]*)",
            Pattern.CASE_INSENSITIVE
    );

    private static final Pattern IMPORT_PATTERN = Pattern.compile(
            "^\\s*(?:import|from|use|require|include|using)\\s+([\"'a-zA-Z0-9_./\\\\]+)",
            Pattern.CASE_INSENSITIVE
    );

    // Legacy Language patterns (COBOL / Fortran)
    private static final Pattern COBOL_DIVISION = Pattern.compile("^\\s*([A-Z0-9-]+)\\s+DIVISION\\.", Pattern.CASE_INSENSITIVE);
    private static final Pattern COBOL_SECTION = Pattern.compile("^\\s*([A-Z0-9-]+)\\s+SECTION\\.", Pattern.CASE_INSENSITIVE);
    private static final Pattern FORTRAN_SUBROUTINE = Pattern.compile("^\\s*(?:SUBROUTINE|PROGRAM)\\s+([a-zA-Z0-9_]+)", Pattern.CASE_INSENSITIVE);

    // Lisp / Clojure pattern
    private static final Pattern LISP_DEF = Pattern.compile("^\\s*\\((?:defun|defn|def)\\s+([a-zA-Z0-9_-]+)", Pattern.CASE_INSENSITIVE);

    @Override
    public boolean supports(String detectedLanguage, Path filePath) {
        // Universal fallback for any non-binary source file
        return true;
    }

    @Override
    public ExtractionTier getTier() {
        return ExtractionTier.TIER_3_GENERIC;
    }

    @Override
    public ParsedFileResult parse(Long projectId, Path filePath, Path projectDir, String detectedLanguage) throws Exception {
        List<UnifiedNode> nodes = new ArrayList<>();
        List<UnifiedRelationship> relationships = new ArrayList<>();
        String relativePath = projectDir.relativize(filePath).toString().replace('\\', '/');
        String fileNodeId = "proj-" + projectId + ":file-" + relativePath;

        List<String> lines = Files.readAllLines(filePath);
        String currentContainerId = fileNodeId;

        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            int lineNum = i + 1;

            // 1. Check Import/Include
            Matcher importMatcher = IMPORT_PATTERN.matcher(line);
            if (importMatcher.find()) {
                String importTarget = importMatcher.group(1).replace("\"", "").replace("'", "");
                String importNodeId = fileNodeId + ":import-" + lineNum;

                nodes.add(UnifiedNode.builder()
                        .id(importNodeId)
                        .projectId(String.valueOf(projectId))
                        .fileId(relativePath)
                        .category(NodeCategory.IMPORT)
                        .language(detectedLanguage)
                        .name(importTarget)
                        .sourceRange(SourceRange.builder().startLine(lineNum).endLine(lineNum).build())
                        .extractionTier(ExtractionTier.TIER_3_GENERIC)
                        .confidence(0.85)
                        .verificationStatus(VerificationStatus.EXTRACTED_UNVERIFIED)
                        .build());

                relationships.add(UnifiedRelationship.builder()
                        .id("rel:" + fileNodeId + "->IMPORTS->" + importNodeId)
                        .projectId(String.valueOf(projectId))
                        .sourceId(fileNodeId)
                        .targetId(importNodeId)
                        .type(RelationshipType.IMPORTS)
                        .confidence(0.85)
                        .verificationStatus(VerificationStatus.EXTRACTED_UNVERIFIED)
                        .extractionTier(ExtractionTier.TIER_3_GENERIC)
                        .build());
                continue;
            }

            // 2. Check Type / Class
            Matcher typeMatcher = TYPE_CLASS_PATTERN.matcher(line);
            if (typeMatcher.find()) {
                String typeName = typeMatcher.group(1);
                String typeNodeId = "proj-" + projectId + ":type-" + relativePath + ":" + typeName;
                currentContainerId = typeNodeId;

                nodes.add(UnifiedNode.builder()
                        .id(typeNodeId)
                        .projectId(String.valueOf(projectId))
                        .fileId(relativePath)
                        .category(NodeCategory.CLASS)
                        .language(detectedLanguage)
                        .name(typeName)
                        .qualifiedName(relativePath + ":" + typeName)
                        .sourceRange(SourceRange.builder().startLine(lineNum).endLine(lineNum).build())
                        .extractionTier(ExtractionTier.TIER_3_GENERIC)
                        .confidence(0.80)
                        .verificationStatus(VerificationStatus.EXTRACTED_UNVERIFIED)
                        .build());

                relationships.add(UnifiedRelationship.builder()
                        .id("rel:" + fileNodeId + "->CONTAINS->" + typeNodeId)
                        .projectId(String.valueOf(projectId))
                        .sourceId(fileNodeId)
                        .targetId(typeNodeId)
                        .type(RelationshipType.CONTAINS)
                        .confidence(0.80)
                        .verificationStatus(VerificationStatus.EXTRACTED_UNVERIFIED)
                        .extractionTier(ExtractionTier.TIER_3_GENERIC)
                        .build());
                continue;
            }

            // 3. Check Function / Method
            Matcher funcMatcher = FUNCTION_PATTERN.matcher(line);
            if (funcMatcher.find()) {
                String funcName = funcMatcher.group(1);
                String funcNodeId = currentContainerId + ":fn-" + funcName + ":" + lineNum;

                nodes.add(UnifiedNode.builder()
                        .id(funcNodeId)
                        .projectId(String.valueOf(projectId))
                        .fileId(relativePath)
                        .category(NodeCategory.FUNCTION)
                        .language(detectedLanguage)
                        .name(funcName)
                        .sourceRange(SourceRange.builder().startLine(lineNum).endLine(lineNum).build())
                        .extractionTier(ExtractionTier.TIER_3_GENERIC)
                        .confidence(0.75)
                        .verificationStatus(VerificationStatus.EXTRACTED_UNVERIFIED)
                        .build());

                relationships.add(UnifiedRelationship.builder()
                        .id("rel:" + currentContainerId + "->DEFINES->" + funcNodeId)
                        .projectId(String.valueOf(projectId))
                        .sourceId(currentContainerId)
                        .targetId(funcNodeId)
                        .type(RelationshipType.DEFINES)
                        .confidence(0.75)
                        .verificationStatus(VerificationStatus.EXTRACTED_UNVERIFIED)
                        .extractionTier(ExtractionTier.TIER_3_GENERIC)
                        .build());
                continue;
            }

            // 4. Check COBOL / Fortran / Lisp constructs
            Matcher cobolDiv = COBOL_DIVISION.matcher(line);
            if (cobolDiv.find()) {
                String divName = cobolDiv.group(1);
                String divId = fileNodeId + ":div-" + divName;
                nodes.add(UnifiedNode.builder()
                        .id(divId)
                        .projectId(String.valueOf(projectId))
                        .fileId(relativePath)
                        .category(NodeCategory.MODULE)
                        .language("cobol")
                        .name(divName + " DIVISION")
                        .extractionTier(ExtractionTier.TIER_3_GENERIC)
                        .confidence(0.90)
                        .verificationStatus(VerificationStatus.EXTRACTED_UNVERIFIED)
                        .build());
                continue;
            }

            Matcher fortranSub = FORTRAN_SUBROUTINE.matcher(line);
            if (fortranSub.find()) {
                String subName = fortranSub.group(1);
                String subId = fileNodeId + ":sub-" + subName;
                nodes.add(UnifiedNode.builder()
                        .id(subId)
                        .projectId(String.valueOf(projectId))
                        .fileId(relativePath)
                        .category(NodeCategory.PROCEDURE)
                        .language("fortran")
                        .name(subName)
                        .extractionTier(ExtractionTier.TIER_3_GENERIC)
                        .confidence(0.85)
                        .verificationStatus(VerificationStatus.EXTRACTED_UNVERIFIED)
                        .build());
                continue;
            }

            Matcher lispDef = LISP_DEF.matcher(line);
            if (lispDef.find()) {
                String defName = lispDef.group(1);
                String lispId = fileNodeId + ":lisp-" + defName;
                nodes.add(UnifiedNode.builder()
                        .id(lispId)
                        .projectId(String.valueOf(projectId))
                        .fileId(relativePath)
                        .category(NodeCategory.FUNCTION)
                        .language(detectedLanguage)
                        .name(defName)
                        .extractionTier(ExtractionTier.TIER_3_GENERIC)
                        .confidence(0.80)
                        .verificationStatus(VerificationStatus.EXTRACTED_UNVERIFIED)
                        .build());
            }
        }

        FileProcessingStatus status = nodes.isEmpty()
                ? FileProcessingStatus.UNRECOGNIZED_SYNTAX
                : FileProcessingStatus.PARSED_PARTIAL;

        String diag = nodes.isEmpty()
                ? "No familiar structural patterns matched; ready for Tier 4 / Tier 5 fallback"
                : "Extracted " + nodes.size() + " structural elements using generic parser";

        return ParsedFileResult.builder()
                .nodes(nodes)
                .relationships(relationships)
                .extractionTier(ExtractionTier.TIER_3_GENERIC)
                .processingStatus(status)
                .diagnostics(diag)
                .build();
    }
}