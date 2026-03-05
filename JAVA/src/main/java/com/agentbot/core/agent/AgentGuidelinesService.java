package com.agentbot.core.agent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * Service for loading agent guidelines and documentation into system prompts.
 * Inspired by nanobot's AGENTS.md pattern.
 */
public class AgentGuidelinesService {
    private static final Logger log = LoggerFactory.getLogger(AgentGuidelinesService.class);
    
    private final Path workspacePath;
    
    public AgentGuidelinesService(Path workspacePath) {
        this.workspacePath = workspacePath;
    }
    
    public AgentGuidelinesService(String workspacePath) {
        this(Paths.get(workspacePath));
    }
    
    /**
     * Build a comprehensive system prompt by loading workspace documentation files.
     * 
     * Loading priority:
     * 1. SOUL.md - Personality, values, and communication style
     * 2. AGENTS.md - Core behavior guidelines and safety rules
     * 3. TOOLS.md - Tool reference documentation
     * 4. USER.md - User-specific preferences (optional)
     * 
     * @return Combined system prompt with agent guidelines, tool documentation, etc.
     */
    public String buildSystemPrompt() {
        List<String> sections = new ArrayList<>();
        
        // 1. Load SOUL.md (personality and values) - HIGHEST PRIORITY
        String soul = loadDocument("SOUL.md");
        if (soul != null && !soul.isBlank()) {
            sections.add(soul);
        }
        
        // 2. Load AGENTS.md (core behavior guidelines)
        String agentsGuidelines = loadDocument("AGENTS.md");
        if (agentsGuidelines != null && !agentsGuidelines.isBlank()) {
            sections.add(agentsGuidelines);
        }
        
        // 3. Load TOOLS.md (tool reference documentation)
        String toolsDocs = loadDocument("TOOLS.md");
        if (toolsDocs != null && !toolsDocs.isBlank()) {
            sections.add("# Tool Reference\n\n" + toolsDocs);
        }
        
        // 4. Load USER.md if exists (user-specific preferences)
        String userPrefs = loadDocument("USER.md");
        if (userPrefs != null && !userPrefs.isBlank()) {
            sections.add("# User Preferences\n\n" + userPrefs);
        }
        
        if (sections.isEmpty()) {
            log.warn("No agent guideline documents found in workspace: {}", workspacePath);
            return getDefaultSystemPrompt();
        }
        
        return String.join("\n\n---\n\n", sections);
    }
    
    /**
     * Load a single document from the workspace.
     * 
     * @param filename Name of the file (e.g., "AGENTS.md")
     * @return File contents or null if not found
     */
    private String loadDocument(String filename) {
        Path filePath = workspacePath.resolve(filename);
        
        if (!Files.exists(filePath)) {
            log.debug("Document not found: {}", filePath);
            return null;
        }
        
        try {
            String content = Files.readString(filePath);
            log.info("Loaded agent guideline document: {} ({} chars)", filename, content.length());
            return content;
        } catch (IOException e) {
            log.error("Failed to load document: {}", filePath, e);
            return null;
        }
    }
    
    /**
     * Fallback system prompt when no guideline documents are available.
     */
    private String getDefaultSystemPrompt() {
        return """
            You are an intelligent AI assistant integrated into the agentbot system.
            
            Your purpose is to help users accomplish tasks efficiently and safely.
            
            Guidelines:
            - Always explain what you're doing before executing high-risk operations
            - Ask clarifying questions when requests are ambiguous
            - Use available tools effectively to accomplish tasks
            - Respect the approval mechanism for dangerous operations
            
            When a tool requires user approval:
            1. System will request confirmation from the user
            2. User will see a confirmation card with command details
            3. After approval, continue your workflow naturally
            4. Do not re-request approval for the same operation
            """;
    }
    
    /**
     * Check if the workspace has been initialized with guideline documents.
     */
    public boolean hasGuidelines() {
        return Files.exists(workspacePath.resolve("AGENTS.md"));
    }
    
    /**
     * Initialize the workspace with default guideline templates.
     * Call this during onboarding or first run.
     */
    public void initializeWorkspace() {
        try {
            Files.createDirectories(workspacePath);
            
            // Create subdirectories
            Files.createDirectories(workspacePath.resolve("agents"));
            Files.createDirectories(workspacePath.resolve("sessions"));
            Files.createDirectories(workspacePath.resolve("skills"));
            
            log.info("Workspace initialized at: {}", workspacePath);
        } catch (IOException e) {
            log.error("Failed to initialize workspace", e);
        }
    }
    
    public Path getWorkspacePath() {
        return workspacePath;
    }
}
