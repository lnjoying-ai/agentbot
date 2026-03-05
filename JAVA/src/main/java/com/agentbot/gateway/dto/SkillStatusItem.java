package com.agentbot.gateway.dto;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SkillStatusItem {
    private String name;
    private String description;
    private String source;
    private boolean installable;
    private boolean eligible;
    private boolean blocked;
    private boolean blockedByAllowlist;
    private String primaryEnv;
    private SkillRequirement requirements;
    private SkillMissing missing;
    private List<SkillConfigCheck> configChecks = new ArrayList<>();
    private List<SkillInstallOption> install = new ArrayList<>();
    private Map<String, Object> metadata = new HashMap<>();

    public SkillStatusItem() {}

    public SkillStatusItem(String name, String description, String source, boolean installable, Map<String, Object> metadata) {
        this.name = name;
        this.description = description;
        this.source = source;
        this.installable = installable;
        if (metadata != null) {
            this.metadata = metadata;
        }
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public boolean isInstallable() {
        return installable;
    }

    public void setInstallable(boolean installable) {
        this.installable = installable;
    }

    public boolean isEligible() {
        return eligible;
    }

    public void setEligible(boolean eligible) {
        this.eligible = eligible;
    }

    public boolean isBlocked() {
        return blocked;
    }

    public void setBlocked(boolean blocked) {
        this.blocked = blocked;
    }

    public boolean isBlockedByAllowlist() {
        return blockedByAllowlist;
    }

    public void setBlockedByAllowlist(boolean blockedByAllowlist) {
        this.blockedByAllowlist = blockedByAllowlist;
    }

    public String getPrimaryEnv() {
        return primaryEnv;
    }

    public void setPrimaryEnv(String primaryEnv) {
        this.primaryEnv = primaryEnv;
    }

    public SkillRequirement getRequirements() {
        return requirements;
    }

    public void setRequirements(SkillRequirement requirements) {
        this.requirements = requirements;
    }

    public SkillMissing getMissing() {
        return missing;
    }

    public void setMissing(SkillMissing missing) {
        this.missing = missing;
    }

    public List<SkillConfigCheck> getConfigChecks() {
        return configChecks;
    }

    public void setConfigChecks(List<SkillConfigCheck> configChecks) {
        this.configChecks = configChecks;
    }

    public List<SkillInstallOption> getInstall() {
        return install;
    }

    public void setInstall(List<SkillInstallOption> install) {
        this.install = install;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }

    public static class SkillRequirement {
        private List<String> bins = new ArrayList<>();
        private List<String> anyBins = new ArrayList<>();
        private List<String> env = new ArrayList<>();
        private List<String> config = new ArrayList<>();
        private List<String> os = new ArrayList<>();

        public List<String> getBins() {
            return bins;
        }

        public void setBins(List<String> bins) {
            this.bins = bins;
        }

        public List<String> getAnyBins() {
            return anyBins;
        }

        public void setAnyBins(List<String> anyBins) {
            this.anyBins = anyBins;
        }

        public List<String> getEnv() {
            return env;
        }

        public void setEnv(List<String> env) {
            this.env = env;
        }

        public List<String> getConfig() {
            return config;
        }

        public void setConfig(List<String> config) {
            this.config = config;
        }

        public List<String> getOs() {
            return os;
        }

        public void setOs(List<String> os) {
            this.os = os;
        }
    }

    public static class SkillMissing {
        private List<String> bins = new ArrayList<>();
        private List<String> anyBins = new ArrayList<>();
        private List<String> env = new ArrayList<>();
        private List<String> config = new ArrayList<>();
        private List<String> os = new ArrayList<>();

        public List<String> getBins() {
            return bins;
        }

        public void setBins(List<String> bins) {
            this.bins = bins;
        }

        public List<String> getAnyBins() {
            return anyBins;
        }

        public void setAnyBins(List<String> anyBins) {
            this.anyBins = anyBins;
        }

        public List<String> getEnv() {
            return env;
        }

        public void setEnv(List<String> env) {
            this.env = env;
        }

        public List<String> getConfig() {
            return config;
        }

        public void setConfig(List<String> config) {
            this.config = config;
        }

        public List<String> getOs() {
            return os;
        }

        public void setOs(List<String> os) {
            this.os = os;
        }
    }

    public static class SkillConfigCheck {
        private String path;
        private String value;
        private boolean satisfied;

        public SkillConfigCheck() {}

        public SkillConfigCheck(String path, String value, boolean satisfied) {
            this.path = path;
            this.value = value;
            this.satisfied = satisfied;
        }

        public String getPath() {
            return path;
        }

        public void setPath(String path) {
            this.path = path;
        }

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }

        public boolean isSatisfied() {
            return satisfied;
        }

        public void setSatisfied(boolean satisfied) {
            this.satisfied = satisfied;
        }
    }

    public static class SkillInstallOption {
        private String id;
        private String kind;
        private String label;
        private String packageName;
        private String formula;
        private String url;
        private String command;
        private String target;
        private List<String> bins = new ArrayList<>();

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getKind() {
            return kind;
        }

        public void setKind(String kind) {
            this.kind = kind;
        }

        public String getLabel() {
            return label;
        }

        public void setLabel(String label) {
            this.label = label;
        }

        public String getPackageName() {
            return packageName;
        }

        public void setPackageName(String packageName) {
            this.packageName = packageName;
        }

        public String getFormula() {
            return formula;
        }

        public void setFormula(String formula) {
            this.formula = formula;
        }

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }

        public String getCommand() {
            return command;
        }

        public void setCommand(String command) {
            this.command = command;
        }

        public String getTarget() {
            return target;
        }

        public void setTarget(String target) {
            this.target = target;
        }

        public List<String> getBins() {
            return bins;
        }

        public void setBins(List<String> bins) {
            this.bins = bins;
        }
    }
}

