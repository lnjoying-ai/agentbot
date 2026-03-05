package com.agentbot.core.p2p.crypto;

public interface PublicKeyObfuscator {
  byte[] encode(byte[] publicKeyBytes) throws Exception;

  byte[] decode(byte[] obfuscatedBytes) throws Exception;

  String getName();
}
