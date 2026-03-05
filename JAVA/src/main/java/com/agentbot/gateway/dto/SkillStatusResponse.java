package com.agentbot.gateway.dto;

import com.agentbot.core.agent.AgentConfig;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class SkillStatusResponse {
    private List<SkillStatusItem> available = new ArrayList<>();
    private Map<String, AgentConfig.SkillEntryConfig> entries;
    private Boolean inherited;
    private String customPath;
    private List<String> missingBins = new ArrayList<>();
    private List<String> missingEnv = new ArrayList<>();
    private List<String> missingConfig = new ArrayList<>();

    public SkillStatusResponse() {}

    public SkillStatusResponse(List<SkillStatusItem> available,
                               Map<String, AgentConfig.SkillEntryConfig> entries,
                               Boolean inherited,
                               String customPath) {
        this.available = available;
        this.entries = entries;
        this.inherited = inherited;
        this.customPath = customPath;
    }

    public List<SkillStatusItem> getAvailable() {
        return available;
    }

    public void setAvailable(List<SkillStatusItem> available) {
        this.available = available;
    }

    public Map<String, AgentConfig.SkillEntryConfig> getEntries() {
        return entries;
    }

    public void setEntries(Map<String, AgentConfig.SkillEntryConfig> entries) {
        this.entries = entries;
    }

    public Boolean getInherited() {
        return inherited;
    }

    public void setInherited(Boolean inherited) {
        this.inherited = inherited;
    }

    public String getCustomPath() {
        return customPath;
    }

    public void setCustomPath(String customPath) {
        this.customPath = customPath;
    }

    public List<String> getMissingBins() {
        return missingBins;
    }

    public void setMissingBins(List<String> missingBins) {
        this.missingBins = missingBins;
    }

    public List<String> getMissingEnv() {
        return missingEnv;
    }

    public void setMissingEnv(List<String> missingEnv) {
        this.missingEnv = missingEnv;
    }

    public List<String> getMissingConfig() {
        return missingConfig;
    }

    public void setMissingConfig(List<String> missingConfig) {
        this.missingConfig = missingConfig;
    }
}
