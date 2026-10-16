package com.codesentinel.parser.detector;

import com.codesentinel.common.uir.FileClassification;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DetectionResult {
    private String detectedLanguage;
    private String detectionMethod;
    private Double confidence;
    private FileClassification classification;
    private boolean isBinary;
}