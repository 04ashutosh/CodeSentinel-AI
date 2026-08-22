package com.codesentinel.common.mcp;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class McpToolDefinition {
    private String name;
    private String description;
    private String serviceName;
    private String methodName;
}