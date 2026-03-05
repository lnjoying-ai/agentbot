package com.agentbot.core.p2p.crypto;

public class NoopObfuscator implements PublicKeyObfuscator {
  @Override
  public byte[] encode(byte[] publicKeyBytes) {
    return publicKeyBytes;
  }

  @Override
  public byte[] decode(byte[] obfuscatedBytes) {
    return obfuscatedBytes;
  }

  @Override
  public String getName() {
    return "none";
  }
}
