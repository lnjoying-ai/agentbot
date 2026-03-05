package com.agentbot.core.p2p;

import com.agentbot.core.p2p.crypto.CipherSuite;

import java.util.List;

public class P2pSettings {
  private final String nodeId;
  private final String regionId;
  private final int protocolVersion;
  private final int maxPayload;
  private final List<String> supportedContentTypes;
  private final List<String> supportedCompression;
  private final List<CipherSuite> supportedCipherSuites;
  private final boolean requireEncryption;
  private final CipherSuite preferredCipherSuite;
  private final String preferredContentType;
  private final boolean authRequired;
  private final String identityPublicKeyHex;
  private final String identityPrivateKeyHex;
  private final boolean obfuscationEnabled;
  private final String obfuscationAlgo;
  private final int flowWindow;
  private final int maxInFlight;
  private final int idempotentWindow;
  private final boolean chatAckEnabled;
  private final boolean chatNackEnabled;
  private final int chatTtlDefault;
  private final int chatFanout;
  private final long chatAckTimeoutMs;
  private final int chatRetryMax;
  private final int chatDedupWindowMs;
  private final int chatRateLimitQps;
  private final int chatMaxPayloadBytes;
  private final java.util.Map<String, Object> features;


  public P2pSettings(String nodeId,
                     String regionId,
                     int protocolVersion,
                     int maxPayload,
                     List<String> supportedContentTypes,
                     List<String> supportedCompression,
                     List<CipherSuite> supportedCipherSuites,
                     boolean requireEncryption,
                     CipherSuite preferredCipherSuite,
                     String preferredContentType,
                     boolean authRequired,
                     String identityPublicKeyHex,
                     String identityPrivateKeyHex,
                     boolean obfuscationEnabled,
                     String obfuscationAlgo,
                     int flowWindow,
                     int maxInFlight,
                     int idempotentWindow,
                     boolean chatAckEnabled,
                     boolean chatNackEnabled,
                     int chatTtlDefault,
                     int chatFanout,
                     long chatAckTimeoutMs,
                     int chatRetryMax,
                     int chatDedupWindowMs,
                     int chatRateLimitQps,
                     int chatMaxPayloadBytes,
                     java.util.Map<String, Object> features) {

    this.nodeId = nodeId;
    this.regionId = regionId;
    this.protocolVersion = protocolVersion;
    this.maxPayload = maxPayload;
    this.supportedContentTypes = supportedContentTypes;
    this.supportedCompression = supportedCompression;
    this.supportedCipherSuites = supportedCipherSuites;
    this.requireEncryption = requireEncryption;
    this.preferredCipherSuite = preferredCipherSuite;
    this.preferredContentType = preferredContentType;
    this.authRequired = authRequired;
    this.identityPublicKeyHex = identityPublicKeyHex;
    this.identityPrivateKeyHex = identityPrivateKeyHex;
    this.obfuscationEnabled = obfuscationEnabled;
    this.obfuscationAlgo = obfuscationAlgo;
    this.flowWindow = flowWindow;
    this.maxInFlight = maxInFlight;
    this.idempotentWindow = idempotentWindow;
    this.chatAckEnabled = chatAckEnabled;
    this.chatNackEnabled = chatNackEnabled;
    this.chatTtlDefault = chatTtlDefault;
    this.chatFanout = chatFanout;
    this.chatAckTimeoutMs = chatAckTimeoutMs;
    this.chatRetryMax = chatRetryMax;
    this.chatDedupWindowMs = chatDedupWindowMs;
    this.chatRateLimitQps = chatRateLimitQps;
    this.chatMaxPayloadBytes = chatMaxPayloadBytes;
    this.features = features == null ? java.util.Map.of() : java.util.Map.copyOf(features);

  }



  public String getNodeId() {
    return nodeId;
  }

  public String getRegionId() {
    return regionId;
  }

  public int getProtocolVersion() {
    return protocolVersion;
  }

  public int getMaxPayload() {
    return maxPayload;
  }

  public List<String> getSupportedContentTypes() {
    return supportedContentTypes;
  }

  public List<String> getSupportedCompression() {
    return supportedCompression;
  }

  public List<CipherSuite> getSupportedCipherSuites() {
    return supportedCipherSuites;
  }

  public boolean isRequireEncryption() {
    return requireEncryption;
  }

  public CipherSuite getPreferredCipherSuite() {
    return preferredCipherSuite;
  }

  public String getPreferredContentType() {
    return preferredContentType;
  }

  public boolean isAuthRequired() {
    return authRequired;
  }

  public String getIdentityPublicKeyHex() {
    return identityPublicKeyHex;
  }

  public String getIdentityPrivateKeyHex() {
    return identityPrivateKeyHex;
  }

  public boolean isObfuscationEnabled() {
    return obfuscationEnabled;
  }

  public String getObfuscationAlgo() {
    return obfuscationAlgo;
  }

  public int getFlowWindow() {
    return flowWindow;
  }

  public int getMaxInFlight() {
    return maxInFlight;
  }

  public int getIdempotentWindow() {
    return idempotentWindow;
  }

  public boolean isChatAckEnabled() {
    return chatAckEnabled;
  }

  public boolean isChatNackEnabled() {
    return chatNackEnabled;
  }

  public int getChatTtlDefault() {
    return chatTtlDefault;
  }

  public int getChatFanout() {
    return chatFanout;
  }

  public long getChatAckTimeoutMs() {
    return chatAckTimeoutMs;
  }

  public int getChatRetryMax() {
    return chatRetryMax;
  }

  public int getChatDedupWindowMs() {
    return chatDedupWindowMs;
  }

  public int getChatRateLimitQps() {
    return chatRateLimitQps;
  }

  public int getChatMaxPayloadBytes() {
    return chatMaxPayloadBytes;
  }

  public java.util.Map<String, Object> getFeatures() {
    return features;
  }
}



