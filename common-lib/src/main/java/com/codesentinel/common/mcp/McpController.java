package com.codesentinel.common.mcp;

import com.codesentinel.common.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/mcp")
@RequiredArgsConstructor
public class McpController {

    private final McpToolRegistry toolRegistry;

    @GetMapping("/tools")
    public ResponseEntity<ApiResponse<List<McpToolDefinition>>> getTools() {
        List<McpToolDefinition> tools = toolRegistry.getTools();
        return ResponseEntity.ok(
                ApiResponse.success("Tools discovered", tools)
        );
    }
}