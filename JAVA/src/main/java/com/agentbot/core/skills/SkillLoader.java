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
    private final List<Path> skillsPaths;
    private final ObjectMapper yamlMapper = new YAMLMapper();
    private static final Pattern FRONT_MATTER_PATTERN = Pattern.compile("^---\\s*\\n(.*?)\\n---\\s*\\n(.*)$", Pattern.DOTALL);

    public SkillLoader(Path skillsPath) {
        this.skillsPaths = List.of(skillsPath);
    }

    public SkillLoader(List<Path> skillsPaths) {
        this.skillsPaths = skillsPaths == null || skillsPaths.isEmpty()
                ? List.of()
                : List.copyOf(skillsPaths);
    }

    public List<Skill> loadSkills() {
        Map<String, Skill> merged = new LinkedHashMap<>();
        for (Path skillsPath : skillsPaths) {
            loadSkillsFromPath(skillsPath, merged);
        }
        return new ArrayList<>(merged.values());
    }

    private void loadSkillsFromPath(Path skillsPath, Map<String, Skill> merged) {
        log.debug("Loading skills from path: {}", skillsPath);
        if (skillsPath == null || !Files.exists(skillsPath)) {
            log.debug("Skills path does not exist: {}", skillsPath);
            return;
        }
        try (Stream<Path> walk = Files.walk(skillsPath)) {
            List<Path> skillFiles = walk
                    .filter(p -> p.getFileName().toString().equalsIgnoreCase("SKILL.md"))
                    .collect(Collectors.toList());
            log.debug("Found {} skill files under {}", skillFiles.size(), skillsPath);

            for (Path file : skillFiles) {
                log.debug("Parsing skill file: {}", file);
                try {
                    Skill skill = parseSkillFile(file);
                    if (skill != null && !merged.containsKey(skill.getName())) {
                        merged.put(skill.getName(), skill);
                    }
                } catch (Exception e) {
                    log.error("Failed to parse skill file: {}", file, e);
                }
            }
        } catch (IOException e) {
            log.error("Failed to walk skills directory: {}", skillsPath, e);
        }
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
