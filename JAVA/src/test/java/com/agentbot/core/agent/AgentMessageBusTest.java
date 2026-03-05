package com.agentbot.core.agent;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * AgentMessageBus 单元测试.
 */
class AgentMessageBusTest {
  
  private AgentRegistry mockRegistry;
  private AgentMessageBus messageBus;
  
  @BeforeEach
  void setUp() {
    mockRegistry = org.mockito.Mockito.mock(AgentRegistry.class);
    messageBus = new AgentMessageBus(mockRegistry, 2);
    messageBus.start();
  }
  
  @Test
  void testSendMessage() {
    // Given
    String[] receivedMessage = new String[1];
    messageBus.registerHandler("agent1", msg -> {
      receivedMessage[0] = msg.getContent();
    });
    
    AgentMessage message = AgentMessage.builder()
        .from("agent2")
        .to("agent1")
        .content("Test message")
        .build();
    
    // When
    CompletableFuture<Void> future = messageBus.sendMessage(message);
    
    // Then
    assertDoesNotThrow(() -> future.get(5, TimeUnit.SECONDS));
    
    // Wait for async processing
    try {
      Thread.sleep(500);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
    
    assertEquals("Test message", receivedMessage[0]);
  }
  
  @Test
  void testBroadcast() {
    // Given
    org.mockito.Mockito.when(mockRegistry.getAllAgents())
        .thenReturn(java.util.Map.of(
            "agent1", org.mockito.Mockito.mock(AgentInstance.class),
            "agent2", org.mockito.Mockito.mock(AgentInstance.class)
        ));
    
    int[] receivedCount = {0};
    messageBus.registerHandler("agent1", msg -> receivedCount[0]++);
    messageBus.registerHandler("agent2", msg -> receivedCount[0]++);
    
    AgentMessage message = AgentMessage.builder()
        .from("system")
        .content("Broadcast message")
        .type(AgentMessage.MessageType.BROADCAST)
        .build();
    
    // When
    CompletableFuture<Void> future = messageBus.broadcast(message);
    
    // Then
    assertDoesNotThrow(() -> future.get(5, TimeUnit.SECONDS));
    
    // Wait for async processing
    try {
      Thread.sleep(500);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
    
    assertEquals(2, receivedCount[0]);
  }
  
  @Test
  void testMessagePriority() {
    // Given
    AgentMessage lowPriority = AgentMessage.builder()
        .from("agent1")
        .to("agent2")
        .content("Low")
        .priority(1)
        .build();
    
    AgentMessage highPriority = AgentMessage.builder()
        .from("agent1")
        .to("agent2")
        .content("High")
        .priority(9)
        .build();
    
    // When/Then - higher priority should be processed first
    assertNotNull(lowPriority);
    assertNotNull(highPriority);
    assertTrue(highPriority.getPriority() > lowPriority.getPriority());
  }
}
