package com.agentbot.core.skills;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class SkillLoader {
    private static final Logger log = LoggerFactory.getLogger(SkillLoader.class);
    private final Path skillsPath;
    private final ObjectMapper yamlMapper = new YAMLMapper();
    private static final Pattern FRONT_MATTER_PATTERN = Pattern.compile("^---\\s*\\n(.*?)\\n---\\s*\\n(.*)$", Pattern.DOTALL);

    public SkillLoader(Path skillsPath) {
        this.skillsPath = skillsPath;
    }

    public List<Skill> loadSkills() {
        if (!Files.exists(skillsPath)) {
            log.warn("Skills path does not exist: {}", skillsPath);
            return Collections.emptyList();
        }

        List<Skill> skills = new ArrayList<>();
        try (Stream<Path> walk = Files.walk(skillsPath)) {
            List<Path> skillFiles = walk
                    .filter(p -> p.getFileName().toString().equalsIgnoreCase("SKILL.md"))
                    .collect(Collectors.toList());

            for (Path file : skillFiles) {
                try {
                    Skill skill = parseSkillFile(file);
                    if (skill != null) {
                        skills.add(skill);
                    }
                } catch (Exception e) {
                    log.error("Failed to parse skill file: {}", file, e);
                }
            }
        } catch (IOException e) {
            log.error("Failed to walk skills directory", e);
        }
        return skills;
    }

    private Skill parseSkillFile(Path path) throws IOException {
        String content = Files.readString(path);
        Matcher matcher = FRONT_MATTER_PATTERN.matcher(content);

        if (matcher.find()) {
            String yamlPart = matcher.group(1);
            String bodyPart = matcher.group(2);

            Map<String, Object> metadata = yamlMapper.readValue(yamlPart, Map.class);
            String name = (String) metadata.getOrDefault("name", path.getParent().getFileName().toString());
            String description = (String) metadata.getOrDefault("description", "");

            return new Skill(name, description, bodyPart, metadata);
        } else {
            log.warn("No front matter found in skill file: {}", path);
            return null;
        }
    }
}
