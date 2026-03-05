package com.agentbot.core.agent;

import com.agentbot.core.skills.SkillLoader;

import com.agentbot.core.tools.ToolRegistry;
import com.agentbot.core.model.LLMProvider;
import com.agentbot.core.model.ToolCallParser;
import com.agentbot.core.events.SystemEventBus;
import org.junit.jupiter.api.BeforeEach;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.Map;


import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * AgentRegistry 单元测试.
 */
class AgentRegistryTest {
  
  @TempDir
  Path tempDir;
  
  private AgentFactory mockFactory;
  private AgentRegistry registry;
  
  @BeforeEach
  void setUp() {
    // Create mock dependencies
    LLMProvider mockProvider = mock(LLMProvider.class);
    ToolRegistry mockTools = mock(ToolRegistry.class);
    SkillLoader mockSkills = mock(SkillLoader.class);
    ToolCallParser mockParser = mock(ToolCallParser.class);
    PendingActionStore mockStore = mock(PendingActionStore.class);
    SystemEventBus mockEventBus = mock(SystemEventBus.class);
    
    mockFactory = new AgentFactory(
        tempDir,
        mockProvider,
        mockTools,
        mockSkills,
        mockParser,
        mockStore,
        mockEventBus
    );

    
    registry = new AgentRegistry(tempDir, mockFactory);
  }
  
  @Test
  void testCreateDefaultAgent() {
    // When
    registry.initialize();
    
    // Then
    AgentInstance defaultAgent = registry.getAgent("default");
    assertNotNull(defaultAgent);
    assertEquals("default", defaultAgent.getConfig().getId());
    assertEquals("Default Agent", defaultAgent.getConfig().getName());
  }
  
  @Test
  void testGetAllAgents() {
    // Given
    registry.initialize();
    
    // When
    Map<String, AgentInstance> agents = registry.getAllAgents();
    
    // Then
    assertFalse(agents.isEmpty());
    assertTrue(agents.containsKey("default"));
  }
  
  @Test
  void testGetNonExistentAgent() {
    // When
    AgentInstance agent = registry.getAgent("non-existent");
    
    // Then
    assertNull(agent);
  }
  
  @Test
  void testRegistryInitialization() {
    // When
    registry.initialize();
    
    // Then
    Map<String, AgentInstance> agents = registry.getAllAgents();
    assertNotNull(agents);
    assertFalse(agents.isEmpty());
  }
}
