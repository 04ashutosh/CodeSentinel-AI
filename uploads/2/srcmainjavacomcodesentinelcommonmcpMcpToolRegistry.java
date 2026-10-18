package com.codesentinel.common.mcp;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Slf4j
@Component
public class McpToolRegistry {

    private final ApplicationContext applicationContext;
    private final List<McpToolDefinition> tools = new ArrayList<>();

    @Value("${spring.application.name:unknown-service}")
    private String serviceName;

    public McpToolRegistry(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    @PostConstruct
    public void discoverTools() {
        String[] beanNames = applicationContext.getBeanDefinitionNames();

        for (String beanName : beanNames) {
            Object bean = applicationContext.getBean(beanName);
            for (Method method : bean.getClass().getDeclaredMethods()) {
                McpTool annotation = method.getAnnotation(McpTool.class);
                if (annotation != null) {
                    McpToolDefinition tool = McpToolDefinition.builder()
                            .name(annotation.name())
                            .description(annotation.description())
                            .serviceName(serviceName)
                            .methodName(method.getName())
                            .build();
                    tools.add(tool);
                    log.info("Registered MCP tool: {} -> {}.{}()",
                            annotation.name(), serviceName, method.getName());
                }
            }
        }
    }

    public List<McpToolDefinition> getTools() {
        return Collections.unmodifiableList(tools);
    }
}