package com.agentbot.core.p2p;

import com.agentbot.config.AgentbotProperties;
import com.agentbot.core.identity.NodeIdentity;
import com.agentbot.core.p2p.crypto.CipherSuite;
import com.agentbot.core.protocol.CborMessageCodec;
import com.agentbot.core.protocol.JsonMessageCodec;
import com.agentbot.core.protocol.MessageCodecRegistry;
import com.agentbot.core.protocol.ProtobufMessageCodec;
import com.agentbot.core.protocol.ProtocolConstants;


import java.util.List;
import java.util.stream.Collectors;

public final class P2pSettingsFactory {
  private P2pSettingsFactory() {}

  public static P2pSettings fromProperties(AgentbotProperties.P2p props, NodeIdentity identity) {

    List<String> contentTypes = props.getSupportedContentTypes().isEmpty()
        ? List.of(ProtocolConstants.CONTENT_JSON, ProtocolConstants.CONTENT_CBOR, ProtocolConstants.CONTENT_PROTO)
        : props.getSupportedContentTypes();

    List<CipherSuite> suites = props.getSupportedCipherSuites().isEmpty()
        ? List.of(CipherSuite.AES_GCM_256, CipherSuite.CHACHA20_POLY1305)
        : props.getSupportedCipherSuites().stream().map(CipherSuite::from).collect(Collectors.toList());

    String preferredContentType = props.getPreferredContentType();
    if (preferredContentType == null || preferredContentType.isBlank()) {
      preferredContentType = ProtocolConstants.CONTENT_JSON;
    }

    CipherSuite preferredCipher = CipherSuite.from(props.getPreferredCipherSuite());

    String nodeId = identity == null ? "" : identity.getNodeIdBech32();
    String publicKeyHex = identity == null ? null : identity.getPublicKeyHex();
    String privateKeyHex = identity == null ? null : identity.getPrivateKeyHex();

    java.util.Map<String, Object> features = new java.util.HashMap<>();
    features.put("idempotentWindow", props.getIdempotentWindow());
    features.put("flowWindow", props.getFlowWindow());
    features.put("maxInFlight", props.getMaxInFlight());
    features.put("chatAckEnabled", props.isChatAckEnabled());
    features.put("chatNackEnabled", props.isChatNackEnabled());
    features.put("chatTtlDefault", props.getChatTtlDefault());
    features.put("chatFanout", props.getChatFanout());
    features.put("chatAckTimeoutMs", props.getChatAckTimeoutMs());
    features.put("chatRetryMax", props.getChatRetryMax());
    features.put("chatDedupWindowMs", props.getChatDedupWindowMs());
    features.put("chatRateLimitQps", props.getChatRateLimitQps());
    features.put("chatMaxPayloadBytes", props.getChatMaxPayloadBytes());



    return new P2pSettings(
        nodeId,
        props.getRegionId(),
        props.getProtocolVersion(),
        props.getMaxPayload(),
        contentTypes,
        props.getSupportedCompression(),
        suites,
        props.isRequireEncryption(),
        preferredCipher,
        preferredContentType,
        props.isAuthRequired(),
        publicKeyHex,
        privateKeyHex,
        props.isObfuscationEnabled(),
        props.getObfuscationAlgo(),
        props.getFlowWindow(),
        props.getMaxInFlight(),
        props.getIdempotentWindow(),
        props.isChatAckEnabled(),
        props.isChatNackEnabled(),
        props.getChatTtlDefault(),
        props.getChatFanout(),
        props.getChatAckTimeoutMs(),
        props.getChatRetryMax(),
        props.getChatDedupWindowMs(),
        props.getChatRateLimitQps(),
        props.getChatMaxPayloadBytes(),
        features
    );



  }

  public static MessageCodecRegistry defaultCodecRegistry() {
    MessageCodecRegistry registry = new MessageCodecRegistry(new JsonMessageCodec());
    registry.register(new CborMessageCodec());
    registry.register(new ProtobufMessageCodec());
    return registry;
  }
}
