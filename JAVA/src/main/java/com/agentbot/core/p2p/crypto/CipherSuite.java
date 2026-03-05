package com.agentbot.core.p2p.crypto;

public enum CipherSuite {
  AES_GCM_256("AES/GCM/NoPadding"),
  CHACHA20_POLY1305("ChaCha20-Poly1305");

  private final String jceName;

  CipherSuite(String jceName) {
    this.jceName = jceName;
  }

  public String getJceName() {
    return jceName;
  }

  public static CipherSuite from(String name) {
    if (name == null) return AES_GCM_256;
    for (CipherSuite suite : values()) {
      if (suite.name().equalsIgnoreCase(name) || suite.jceName.equalsIgnoreCase(name)) {
        return suite;
      }
    }
    return AES_GCM_256;
  }
}
