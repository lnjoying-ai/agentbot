package com.agentbot.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import com.agentbot.core.util.ConfigPathResolver;

import java.nio.file.Path;

@Configuration
public class WebConfiguration implements WebMvcConfigurer {
    private static final Logger log = LoggerFactory.getLogger(WebConfiguration.class);

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Map /workspace/** to the local workspace directory
        Path path = ConfigPathResolver.resolveUserDataDir().resolve("workspace").toAbsolutePath().normalize();
        String location = "file:" + path.toString().replace("\\", "/") + "/";

        
        log.info("Mapping /workspace/** to {}", location);
        
        registry.addResourceHandler("/workspace/**")
                .addResourceLocations(location);
    }
}
