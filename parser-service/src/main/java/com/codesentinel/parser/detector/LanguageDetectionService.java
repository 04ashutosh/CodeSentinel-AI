package com.codesentinel.parser.detector;

import com.codesentinel.common.uir.FileClassification;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Pattern;

@Service
@Slf4j
public class LanguageDetectionService {

    private static final int BUFFER_PROBE_SIZE = 8192;

    // Extension -> Language mappings (including legacy, functional, systems, and modern)
    private static final Map<String, String> EXTENSION_MAP = new HashMap<>();

    static {
        // Modern & Common
        EXTENSION_MAP.put("java", "java");
        EXTENSION_MAP.put("py", "python");
        EXTENSION_MAP.put("js", "javascript");
        EXTENSION_MAP.put("jsx", "javascript");
        EXTENSION_MAP.put("ts", "typescript");
        EXTENSION_MAP.put("tsx", "typescript");
        EXTENSION_MAP.put("c", "c");
        EXTENSION_MAP.put("h", "c");
        EXTENSION_MAP.put("cpp", "cpp");
        EXTENSION_MAP.put("hpp", "cpp");
        EXTENSION_MAP.put("cc", "cpp");
        EXTENSION_MAP.put("cs", "csharp");
        EXTENSION_MAP.put("go", "go");
        EXTENSION_MAP.put("rs", "rust");
        EXTENSION_MAP.put("rb", "ruby");
        EXTENSION_MAP.put("php", "php");
        EXTENSION_MAP.put("swift", "swift");
        EXTENSION_MAP.put("kt", "kotlin");
        EXTENSION_MAP.put("scala", "scala");
        EXTENSION_MAP.put("sh", "shell");
        EXTENSION_MAP.put("bash", "shell");
        EXTENSION_MAP.put("zsh", "shell");
        EXTENSION_MAP.put("sql", "sql");

        // Legacy Languages
        EXTENSION_MAP.put("cob", "cobol");
        EXTENSION_MAP.put("cbl", "cobol");
        EXTENSION_MAP.put("cpy", "cobol");
        EXTENSION_MAP.put("f", "fortran");
        EXTENSION_MAP.put("for", "fortran");
        EXTENSION_MAP.put("f90", "fortran");
        EXTENSION_MAP.put("f95", "fortran");
        EXTENSION_MAP.put("pas", "pascal");
        EXTENSION_MAP.put("asm", "assembly");
        EXTENSION_MAP.put("s", "assembly");

        // Functional & Logic Languages
        EXTENSION_MAP.put("hs", "haskell");
        EXTENSION_MAP.put("lhs", "haskell");
        EXTENSION_MAP.put("clj", "clojure");
        EXTENSION_MAP.put("cljs", "clojure");
        EXTENSION_MAP.put("erl", "erlang");
        EXTENSION_MAP.put("hrl", "erlang");
        EXTENSION_MAP.put("ex", "elixir");
        EXTENSION_MAP.put("exs", "elixir");
        EXTENSION_MAP.put("ml", "ocaml");
        EXTENSION_MAP.put("mli", "ocaml");
        EXTENSION_MAP.put("pl", "prolog");
        EXTENSION_MAP.put("pro", "prolog");
        EXTENSION_MAP.put("lisp", "lisp");
        EXTENSION_MAP.put("lsp", "lisp");
        EXTENSION_MAP.put("scm", "scheme");
    }

    private static final Set<String> CONFIG_EXTENSIONS = Set.of(
            "json", "yaml", "yml", "xml", "toml", "ini", "properties", "env"
    );

    private static final Set<String> DOC_EXTENSIONS = Set.of(
            "md", "txt", "rst", "adoc", "pdf", "docx"
    );

    public DetectionResult detect(Path filePath) {
        String fileName = filePath.getFileName().toString();
        String extension = getExtension(fileName).toLowerCase();

        // Signal 0: Binary content probe
        if (isBinaryFile(filePath)) {
            return DetectionResult.builder()
                    .detectedLanguage("binary")
                    .detectionMethod("BINARY_PROBE")
                    .confidence(1.0)
                    .classification(FileClassification.BINARY)
                    .isBinary(true)
                    .build();
        }

        // Signal 1: Shebang check (for extensionless or shell/python files)
        String shebangLang = probeShebang(filePath);
        if (shebangLang != null) {
            return DetectionResult.builder()
                    .detectedLanguage(shebangLang)
                    .detectionMethod("SHEBANG")
                    .confidence(0.95)
                    .classification(FileClassification.SOURCE)
                    .isBinary(false)
                    .build();
        }

        // Signal 2: Config / Documentation classification
        if (CONFIG_EXTENSIONS.contains(extension)) {
            return DetectionResult.builder()
                    .detectedLanguage(extension)
                    .detectionMethod("EXTENSION_CONFIG")
                    .confidence(0.95)
                    .classification(FileClassification.CONFIG)
                    .isBinary(false)
                    .build();
        }

        if (DOC_EXTENSIONS.contains(extension)) {
            return DetectionResult.builder()
                    .detectedLanguage(extension)
                    .detectionMethod("EXTENSION_DOC")
                    .confidence(0.95)
                    .classification(FileClassification.DOCUMENTATION)
                    .isBinary(false)
                    .build();
        }

        // Signal 3: Extension mapping
        if (EXTENSION_MAP.containsKey(extension)) {
            return DetectionResult.builder()
                    .detectedLanguage(EXTENSION_MAP.get(extension))
                    .detectionMethod("EXTENSION")
                    .confidence(0.90)
                    .classification(FileClassification.SOURCE)
                    .isBinary(false)
                    .build();
        }

        // Signal 4: Lexical content heuristics for unknown/extensionless files
        String lexicalLang = probeLexicalCharacteristics(filePath);
        if (lexicalLang != null) {
            return DetectionResult.builder()
                    .detectedLanguage(lexicalLang)
                    .detectionMethod("LEXICAL_HEURISTIC")
                    .confidence(0.70)
                    .classification(FileClassification.SOURCE)
                    .isBinary(false)
                    .build();
        }

        // Signal 5: Universal Fallback for unseen text files (Never discard an unknown source file!)
        return DetectionResult.builder()
                .detectedLanguage("unknown_text")
                .detectionMethod("GENERIC_FALLBACK")
                .confidence(0.50)
                .classification(FileClassification.SOURCE)
                .isBinary(false)
                .build();
    }

    private boolean isBinaryFile(Path path) {
        try (InputStream in = new BufferedInputStream(Files.newInputStream(path))) {
            byte[] buffer = new byte[BUFFER_PROBE_SIZE];
            int bytesRead = in.read(buffer);
            if (bytesRead <= 0) return false;

            int controlChars = 0;
            for (int i = 0; i < bytesRead; i++) {
                byte b = buffer[i];
                if (b == 0) { // NUL byte is definitive binary marker
                    return true;
                }
                if ((b < 32 && b != '\t' && b != '\n' && b != '\r') || b == 127) {
                    controlChars++;
                }
            }
            return ((double) controlChars / bytesRead) > 0.30;
        } catch (Exception e) {
            return false;
        }
    }

    private String probeShebang(Path path) {
        try (Scanner scanner = new Scanner(path)) {
            if (scanner.hasNextLine()) {
                String firstLine = scanner.nextLine().trim();
                if (firstLine.startsWith("#!")) {
                    if (firstLine.contains("python")) return "python";
                    if (firstLine.contains("node") || firstLine.contains("javascript")) return "javascript";
                    if (firstLine.contains("bash") || firstLine.contains("sh")) return "shell";
                    if (firstLine.contains("perl")) return "perl";
                    if (firstLine.contains("ruby")) return "ruby";
                }
                if (firstLine.startsWith("<?php")) return "php";
                if (firstLine.toUpperCase().contains("IDENTIFICATION DIVISION")) return "cobol";
                if (firstLine.toUpperCase().startsWith("PROGRAM ")) return "fortran";
            }
        } catch (Exception ignored) {}
        return null;
    }

    private String probeLexicalCharacteristics(Path path) {
        try (Scanner scanner = new Scanner(path)) {
            int linesRead = 0;
            StringBuilder sample = new StringBuilder();
            while (scanner.hasNextLine() && linesRead < 50) {
                sample.append(scanner.nextLine()).append("\n");
                linesRead++;
            }
            String content = sample.toString().toUpperCase();

            // COBOL signature
            if (content.contains("IDENTIFICATION DIVISION") || content.contains("PROCEDURE DIVISION") || content.contains("WORKING-STORAGE SECTION")) {
                return "cobol";
            }
            // Fortran signature
            if (content.contains("SUBROUTINE ") || content.contains("END SUBROUTINE") || content.contains("IMPLICIT NONE")) {
                return "fortran";
            }
            // Lisp / Scheme signature
            if (content.contains("(DEFUN ") || content.contains("(DEFINE ")) {
                return "lisp";
            }
            // SQL signature
            if (content.contains("CREATE TABLE") || content.contains("SELECT ") && content.contains(" FROM ")) {
                return "sql";
            }
        } catch (Exception ignored) {}
        return null;
    }

    private String getExtension(String fileName) {
        int dotIndex = fileName.lastIndexOf('.');
        return (dotIndex == -1 || dotIndex == fileName.length() - 1) ? "" : fileName.substring(dotIndex + 1);
    }
}