package com.agentbot.core.agent;

import com.agentbot.core.bus.events.InboundMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GeoRoutingStrategy implements AgentRoutingStrategy {
  private static final Logger log = LoggerFactory.getLogger(GeoRoutingStrategy.class);

  private final String localRegionId;

  public GeoRoutingStrategy(String localRegionId) {
    this.localRegionId = localRegionId == null ? "" : localRegionId;
  }

  @Override
  public String route(InboundMessage message, AgentRegistry registry) {
    if (message == null || message.getMetadata() == null) {
      return null;
    }
    Object region = message.getMetadata().get("regionId");
    if (region == null) {
      return null;
    }
    String regionId = String.valueOf(region);
    if (regionId.isBlank() || regionId.equalsIgnoreCase(localRegionId)) {
      return null;
    }
    String target = registry.hasAgent("default") ? "default" : null;
    if (target != null) {
      log.debug("Geo route to default agent: localRegion={}, msgRegion={}", localRegionId, regionId);
    }
    return target;

  }
}
