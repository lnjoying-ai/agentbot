package com.agentbot.gateway;

import com.agentbot.core.agent.AgentConfig;
import com.agentbot.core.agent.AgentInstance;
import com.agentbot.core.agent.AgentRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


/**
 * REST API controller for Agent management.
 */
@RestController
@RequestMapping("/api/agents")
@CrossOrigin(origins = "*")
public class AgentManagementController {
  private static final Logger log = LoggerFactory.getLogger(AgentManagementController.class);
  
  private final AgentRegistry registry;
  
  public AgentManagementController(AgentRegistry registry) {
    this.registry = registry;
  }
  
  /**
   * List all agents.
   * GET /api/agents
   */
  @GetMapping
  public ResponseEntity<List<Map<String, Object>>> listAgents() {
    try {
      List<Map<String, Object>> agents = registry.getAllAgents().values().stream()
          .map(this::toAgentSummary)
          .collect(Collectors.toList());
      
      return ResponseEntity.ok(agents);
    } catch (Exception e) {
      log.error("Failed to list agents", e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
    }
  }
  
  /**
   * Get agent by ID.
   * GET /api/agents/{agentId}
   */
  @GetMapping("/{agentId}")
  public ResponseEntity<Map<String, Object>> getAgent(@PathVariable("agentId") String agentId) {
    try {
      AgentInstance agent = registry.getAgent(agentId);
      if (agent == null) {
        return ResponseEntity.notFound().build();
      }
      
      Map<String, Object> response = toAgentDetail(agent);
      return ResponseEntity.ok(response);
    } catch (Exception e) {
      log.error("Failed to get agent: {}", agentId, e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
    }
  }
  
  /**
   * Create new agent.
   * POST /api/agents
   */
  @PostMapping
  public ResponseEntity<Map<String, Object>> createAgent(@RequestBody AgentConfig config) {
    try {
      // Validate required fields
      if (config.getId() == null || config.getId().isBlank()) {
        return ResponseEntity.badRequest().body(Map.of("error", "Agent ID is required"));
      }
      
      if (config.getName() == null || config.getName().isBlank()) {
        return ResponseEntity.badRequest().body(Map.of("error", "Agent name is required"));
      }
      
      AgentInstance agent = registry.createAgent(config);
      
      Map<String, Object> response = toAgentDetail(agent);
      return ResponseEntity.status(HttpStatus.CREATED).body(response);
    } catch (IllegalArgumentException e) {
      log.warn("Failed to create agent: {}", e.getMessage());
      return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
    } catch (Exception e) {
      log.error("Failed to create agent", e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body(Map.of("error", "Internal server error"));
    }
  }
  
  /**
   * Update agent configuration.
   * PUT /api/agents/{agentId}
   */
  @PutMapping("/{agentId}")
  public ResponseEntity<Map<String, Object>> updateAgent(
      @PathVariable("agentId") String agentId,
      @RequestBody AgentConfig config
  ) {
    try {
      // Ensure ID matches
      config.setId(agentId);
      
      registry.updateAgent(agentId, config);
      
      AgentInstance agent = registry.getAgent(agentId);
      Map<String, Object> response = toAgentDetail(agent);
      return ResponseEntity.ok(response);
    } catch (IllegalArgumentException e) {
      log.warn("Failed to update agent: {}", e.getMessage());
      return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
    } catch (Exception e) {
      log.error("Failed to update agent: {}", agentId, e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body(Map.of("error", "Internal server error"));
    }
  }
  
  /**
   * Delete agent.
   * DELETE /api/agents/{agentId}
   */
  @DeleteMapping("/{agentId}")
  public ResponseEntity<Map<String, Object>> deleteAgent(@PathVariable("agentId") String agentId) {
    try {
      registry.deleteAgent(agentId);
      return ResponseEntity.ok(Map.of("success", true, "message", "Agent deleted"));
    } catch (IllegalArgumentException e) {
      log.warn("Failed to delete agent: {}", e.getMessage());
      return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
    } catch (Exception e) {
      log.error("Failed to delete agent: {}", agentId, e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body(Map.of("error", "Internal server error"));
    }
  }
  
  /**
   * Get agent configuration.
   * GET /api/agents/{agentId}/config
   */
  @GetMapping("/{agentId}/config")
  public ResponseEntity<String> getAgentConfig(@PathVariable("agentId") String agentId) {
    try {
      Path configPath = registry.getAgentConfigPath(agentId);
      if (!Files.exists(configPath)) {
        return ResponseEntity.notFound().build();
      }
      
      String json = Files.readString(configPath);
      return ResponseEntity.ok()
          .contentType(MediaType.APPLICATION_JSON)
          .body(json);
    } catch (Exception e) {
      log.error("Failed to get agent config: {}", agentId, e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
    }
  }

  /**
   * Get agent skills (entries + loaded skills).
   */
  @GetMapping("/{agentId}/skills")
  public ResponseEntity<Map<String, Object>> getAgentSkills(@PathVariable("agentId") String agentId) {
    try {
      AgentInstance agent = registry.getAgent(agentId);
      if (agent == null) return ResponseEntity.notFound().build();
      AgentConfig config = agent.getConfig();
      AgentConfig.SkillsConfig skillsCfg = config.getCapabilities().getSkills();
      Map<String, AgentConfig.SkillEntryConfig> entries =
          skillsCfg.getEntries() == null ? Map.of() : skillsCfg.getEntries();
      List<Map<String, Object>> available = new java.util.ArrayList<>();
      if (agent.getLoadedSkills() != null) {
        for (com.agentbot.core.skills.Skill s : agent.getLoadedSkills()) {
          Map<String, Object> item = new HashMap<>();
          item.put("name", s.getName());
          item.put("description", s.getDescription());
          item.put("metadata", s.getMetadata());
          available.add(item);
        }
      }
      Map<String, Object> body = new HashMap<>();
      body.put("entries", entries);
      body.put("inherited", skillsCfg.isInherited());
      body.put("customPath", skillsCfg.getCustomPath());
      body.put("available", available);
      return ResponseEntity.ok(body);
    } catch (Exception e) {
      log.error("Failed to get agent skills: {}", agentId, e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
    }
  }

  /**
   * Update agent skills configuration (entries/customPath/inherited).
   */
  @PutMapping("/{agentId}/skills")
  public ResponseEntity<Map<String, Object>> updateAgentSkills(
      @PathVariable("agentId") String agentId,
      @RequestBody Map<String, Object> payload
  ) {
    try {
      AgentInstance agent = registry.getAgent(agentId);
      if (agent == null) return ResponseEntity.notFound().build();
      AgentConfig config = agent.getConfig();
      AgentConfig.SkillsConfig skillsCfg = config.getCapabilities().getSkills();
      if (payload.containsKey("inherited")) {
        Object inh = payload.get("inherited");
        if (inh instanceof Boolean b) {
          skillsCfg.setInherited(b);
        }
      }

      if (payload.containsKey("customPath")) {
        Object cp = payload.get("customPath");
        skillsCfg.setCustomPath(cp == null ? null : cp.toString());
      }
      if (payload.containsKey("entries")) {

        Object entriesObj = payload.get("entries");
        if (entriesObj instanceof Map<?, ?> map) {
          Map<String, AgentConfig.SkillEntryConfig> next = new HashMap<>();
          for (Map.Entry<?, ?> e : map.entrySet()) {
            String key = String.valueOf(e.getKey());
            AgentConfig.SkillEntryConfig entry = new AgentConfig.SkillEntryConfig();
            if (e.getValue() instanceof Map<?, ?> valMap) {
              if (valMap.containsKey("enabled")) {
                Object ev = valMap.get("enabled");
                if (ev instanceof Boolean) entry.setEnabled((Boolean) ev);
              }
              if (valMap.containsKey("apiKey")) {
                Object ak = valMap.get("apiKey");
                if (ak != null) entry.setApiKey(ak.toString());
              }
              if (valMap.containsKey("env")) {
                Object envObj = valMap.get("env");
                if (envObj instanceof Map<?, ?> envMap) {
                  Map<String, String> env = new HashMap<>();
                  for (Map.Entry<?, ?> envEntry : envMap.entrySet()) {
                    env.put(String.valueOf(envEntry.getKey()), String.valueOf(envEntry.getValue()));
                  }
                  entry.setEnv(env);
                }
              }
            }
            next.put(key, entry);
          }
          skillsCfg.setEntries(next);
        }
      }
      config.getCapabilities().setSkills(skillsCfg);
      registry.updateAgent(agentId, config);
      return ResponseEntity.ok(Map.of("success", true));
    } catch (IllegalArgumentException ex) {
      return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
    } catch (Exception e) {
      log.error("Failed to update agent skills: {}", agentId, e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", "Internal server error"));
    }
  }

  
  /**
   * Update agent configuration.
   * PUT /api/agents/{agentId}/config
   */
  @PutMapping("/{agentId}/config")
  public ResponseEntity<Map<String, Object>> updateAgentConfig(
      @PathVariable("agentId") String agentId,
      @RequestBody AgentConfig config
  ) {
    return updateAgent(agentId, config);
  }
  
  /**
   * Get agent statistics.
   * GET /api/agents/{agentId}/stats
   */

  @GetMapping("/{agentId}/stats")
  public ResponseEntity<Map<String, Object>> getAgentStats(@PathVariable("agentId") String agentId) {
    try {
      AgentInstance agent = registry.getAgent(agentId);
      if (agent == null) {
        return ResponseEntity.notFound().build();
      }
      
      Map<String, Object> stats = new HashMap<>();
      stats.put("agentId", agentId);
      stats.put("enabled", agent.getConfig().isEnabled());
      stats.put("healthy", agent.isHealthy());
      
      // Memory stats
      if (agent.getMemory() != null) {
        stats.put("memory", agent.getMemory().getStats());
      }
      
      // Tool count
      if (agent.getTools() != null) {
        stats.put("toolCount", agent.getTools().getToolCount());
      }
      
      // Skill count
      if (agent.getSkills() != null) {
        stats.put("skillCount", agent.getSkills().loadSkills().size());
      }
      
      return ResponseEntity.ok(stats);
    } catch (Exception e) {
      log.error("Failed to get agent stats: {}", agentId, e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
    }
  }
  
  /**
   * Convert agent to summary format.
   */
  private Map<String, Object> toAgentSummary(AgentInstance agent) {
    AgentConfig config = agent.getConfig();
    Map<String, Object> summary = new HashMap<>();
    
    summary.put("id", config.getId());
    summary.put("name", config.getName());
    summary.put("displayName", config.getDisplayName());
    summary.put("description", config.getDescription());
    summary.put("avatar", config.getAvatar());
    summary.put("enabled", config.isEnabled());
    summary.put("healthy", agent.isHealthy());
    summary.put("sessionStatus", agent.isWorking() ? "working" : "idle");
    summary.put("activeSessions", agent.getActiveSessions());
    summary.put("createdAt", config.getCreatedAt());
    summary.put("updatedAt", config.getUpdatedAt());
    
    return summary;

  }
  
  /**
   * Convert agent to detailed format.
   */
  private Map<String, Object> toAgentDetail(AgentInstance agent) {
    Map<String, Object> detail = toAgentSummary(agent);
    
    AgentConfig config = agent.getConfig();
    detail.put("routing", config.getRouting());
    detail.put("capabilities", config.getCapabilities());
    
    return detail;
  }

}
