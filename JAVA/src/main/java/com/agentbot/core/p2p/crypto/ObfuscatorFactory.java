package com.agentbot.core.p2p.crypto;

public final class ObfuscatorFactory {
  private ObfuscatorFactory() {}

  public static PublicKeyObfuscator resolve(String name, boolean enabled) {
    if (!enabled) {
      return new NoopObfuscator();
    }
    if (name == null || name.isBlank()) {
      return new NoopObfuscator();
    }
    String key = name.trim().toLowerCase();
    if ("none".equals(key)) {
      return new NoopObfuscator();
    }
    return new ElligatorSwiftObfuscator();
  }

}
