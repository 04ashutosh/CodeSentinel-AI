package com.codesentinel.parser;

import com.codesentinel.common.uir.*;
import com.codesentinel.parser.detector.DetectionResult;
import com.codesentinel.parser.detector.LanguageDetectionService;
import com.codesentinel.parser.engine.*;
import com.codesentinel.parser.repository.FileInventoryRepository;
import com.codesentinel.parser.repository.ParsedClassRepository;
import com.codesentinel.parser.service.FileInventoryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
public class UniversalParserEngineTest {

    private LanguageDetectionService detectionService;
    private FileInventoryService inventoryService;
    private UniversalParserEngine parserEngine;

    @Mock
    private FileInventoryRepository inventoryRepository;

    @Mock
    private ParsedClassRepository parsedClassRepository;

    private Path fixtureDir;

    @BeforeEach
    void setUp() {
        Path path = Paths.get("src/test/resources/fixtures/mixed-repo");
        if (!Files.exists(path)) {
            path = Paths.get("parser-service/src/test/resources/fixtures/mixed-repo");
        }
        fixtureDir = path.toAbsolutePath();

        detectionService = new LanguageDetectionService();
        inventoryService = new FileInventoryService(detectionService, inventoryRepository);

        Tier1JavaParserStrategy javaParser = new Tier1JavaParserStrategy();
        Tier3GenericStructuralParser genericParser = new Tier3GenericStructuralParser();
        Tier4LlmParserStrategy llmParser = new Tier4LlmParserStrategy();
        Tier5DegradedParserStrategy degradedParser = new Tier5DegradedParserStrategy();

        parserEngine = new UniversalParserEngine(
                inventoryService,
                inventoryRepository,
                parsedClassRepository,
                javaParser,
                genericParser,
                llmParser,
                degradedParser,
                null
        );

        lenient().when(inventoryRepository.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));
        lenient().when(parsedClassRepository.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    @DisplayName("1. Multi-signal language detection correctly classifies all paradigms")
    void testLanguageDetectionAcrossParadigms() {
        // Java
        DetectionResult javaResult = detectionService.detect(fixtureDir.resolve("src/main/java/PaymentService.java"));
        assertThat(javaResult.getDetectedLanguage()).isEqualTo("java");
        assertThat(javaResult.getClassification()).isEqualTo(FileClassification.SOURCE);

        // Python
        DetectionResult pyResult = detectionService.detect(fixtureDir.resolve("scripts/data_processor.py"));
        assertThat(pyResult.getDetectedLanguage()).isEqualTo("python");

        // TypeScript
        DetectionResult tsResult = detectionService.detect(fixtureDir.resolve("frontend/calculator.ts"));
        assertThat(tsResult.getDetectedLanguage()).isEqualTo("typescript");

        // COBOL (Legacy)
        DetectionResult cobolResult = detectionService.detect(fixtureDir.resolve("legacy/payroll.cbl"));
        assertThat(cobolResult.getDetectedLanguage()).isEqualTo("cobol");

        // Fortran (Legacy)
        DetectionResult fortranResult = detectionService.detect(fixtureDir.resolve("scientific/matrix_solver.f90"));
        assertThat(fortranResult.getDetectedLanguage()).isEqualTo("fortran");

        // Clojure (Functional)
        DetectionResult cljResult = detectionService.detect(fixtureDir.resolve("logic/rules.clj"));
        assertThat(cljResult.getDetectedLanguage()).isEqualTo("clojure");

        // Extensionless Shebang
        DetectionResult shebangResult = detectionService.detect(fixtureDir.resolve("bin/deploy_task"));
        assertThat(shebangResult.getDetectedLanguage()).isEqualTo("python");
        assertThat(shebangResult.getDetectionMethod()).isEqualTo("SHEBANG");

        // Binary File
        DetectionResult binResult = detectionService.detect(fixtureDir.resolve("assets/sample.bin"));
        assertThat(binResult.isBinary()).isTrue();
        assertThat(binResult.getClassification()).isEqualTo(FileClassification.BINARY);
    }

    @Test
    @DisplayName("2. Crawl and index produces explicit outcomes for all repository files")
    void testCrawlAndIndex() {
        List<FileInventoryRecord> records = inventoryService.crawlAndIndex(100L, fixtureDir);

        assertThat(records).isNotEmpty();
        // Check every file has a deterministic hash, classification, and status
        for (FileInventoryRecord rec : records) {
            assertThat(rec.getFileId()).isNotBlank();
            assertThat(rec.getContentHash()).isNotBlank();
            assertThat(rec.getClassification()).isNotNull();
            assertThat(rec.getProcessingStatus()).isNotNull();
        }

        // Verify binary file is marked as BINARY_EXCLUDED
        assertThat(records).anyMatch(r -> r.getFileId().contains("sample.bin")
                && r.getProcessingStatus() == FileProcessingStatus.BINARY_EXCLUDED);
    }

    @Test
    @DisplayName("3. End-to-end UniversalParserEngine parses mixed repository into Unified Graph")
    void testParseProjectMixedRepo() {
        UnifiedGraphPayload payload = parserEngine.parseProject(100L, fixtureDir.toString());

        assertThat(payload).isNotNull();
        assertThat(payload.getNodes()).isNotEmpty();
        assertThat(payload.getRelationships()).isNotEmpty();

        // Must contain root PROJECT node
        assertThat(payload.getNodes()).anyMatch(n -> n.getCategory() == NodeCategory.PROJECT);

        // Tier 1 Java validation: PaymentService class and its method
        assertThat(payload.getNodes()).anyMatch(n -> "PaymentService".equals(n.getName())
                && n.getExtractionTier() == ExtractionTier.TIER_1_DEDICATED
                && n.getVerificationStatus() == VerificationStatus.VERIFIED_FACT);

        // Tier 3 Python extraction: DataPipeline class and execute_batch method
        assertThat(payload.getNodes()).anyMatch(n -> "DataPipeline".equals(n.getName())
                && n.getExtractionTier() == ExtractionTier.TIER_3_GENERIC);

        // Tier 3 COBOL extraction: IDENTIFICATION DIVISION
        assertThat(payload.getNodes()).anyMatch(n -> n.getName().contains("DIVISION")
                && "cobol".equals(n.getLanguage()));

        // Tier 3 Fortran extraction: MATRIX_SOLVER / INVERT_MATRIX
        assertThat(payload.getNodes()).anyMatch(n -> "INVERT_MATRIX".equals(n.getName())
                && "fortran".equals(n.getLanguage()));

        // Tier 5 Graceful Degradation: Malformed or unfamiliar files are represented as FILE nodes without breaking parsing
        assertThat(payload.getNodes()).anyMatch(n -> n.getFileId().contains("corrupted.c")
                && n.getCategory() == NodeCategory.FILE);

        // Verify relationships exist (e.g. CONTAINS, DEFINES)
        assertThat(payload.getRelationships()).anyMatch(r -> r.getType() == RelationshipType.CONTAINS);
    }
}
