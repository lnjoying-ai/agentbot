package com.agentbot.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;
import java.nio.file.Paths;

@Configuration
public class WebConfiguration implements WebMvcConfigurer {
    private static final Logger log = LoggerFactory.getLogger(WebConfiguration.class);
    private final AgentbotProperties properties;

    public WebConfiguration(AgentbotProperties properties) {
        this.properties = properties;
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Map /workspace/** to the local workspace directory
        String workspaceDir = properties.getWorkspaceDir();
        Path path = Paths.get(workspaceDir).toAbsolutePath();
        String location = "file:" + path.toString().replace("\\", "/") + "/";
        
        log.info("Mapping /workspace/** to {}", location);
        
        registry.addResourceHandler("/workspace/**")
                .addResourceLocations(location);
    }
}
