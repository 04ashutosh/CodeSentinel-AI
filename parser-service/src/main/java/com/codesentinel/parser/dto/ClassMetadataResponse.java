package com.codesentinel.parser.dto;

import com.codesentinel.parser.enums.ClassType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClassMetadataResponse {
    private Long id;
    private String className;
    private String packageName;
    private ClassType classType;
    private String filePath;
    private List<String> annotations;
    private Integer methodCount;
    private Integer fieldCount;
    private List<String> implementsInterfaces;
    private String extendsClass;
}