package com.agentbot.core.skills;

import java.util.Map;

public class Skill {
    private final String name;
    private final String description;
    private final String content;
    private final Map<String, Object> metadata;

    public Skill(String name, String description, String content, Map<String, Object> metadata) {
        this.name = name;
        this.description = description;
        this.content = content;
        this.metadata = metadata;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getContent() {
        return content;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    @Override
    public String toString() {
        return String.format("### Skill: %s\nDescription: %s\n\n%s\n", name, description, content);
    }
}
