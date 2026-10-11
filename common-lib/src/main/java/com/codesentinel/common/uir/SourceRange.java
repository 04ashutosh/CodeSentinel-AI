package com.codesentinel.common.uir;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SourceRange {
    private Integer startLine;
    private Integer endLine;
    private Integer startColumn;
    private Integer endColumn;
}