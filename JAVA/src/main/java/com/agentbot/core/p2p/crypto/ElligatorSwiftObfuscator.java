package com.agentbot.core.p2p.crypto;

import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.SecureRandom;

public class ElligatorSwiftObfuscator implements PublicKeyObfuscator {
  private static final int MASK_LENGTH = 32;
  private final SecureRandom random = new SecureRandom();

  @Override
  public byte[] encode(byte[] publicKeyBytes) throws Exception {
    if (publicKeyBytes == null) return new byte[0];
    byte[] mask = new byte[MASK_LENGTH];
    random.nextBytes(mask);
    byte[] keystream = deriveKeystream(mask, publicKeyBytes.length);
    byte[] masked = xor(publicKeyBytes, keystream);
    ByteBuffer buffer = ByteBuffer.allocate(mask.length + masked.length);
    buffer.put(mask);
    buffer.put(masked);
    return buffer.array();
  }

  @Override
  public byte[] decode(byte[] obfuscatedBytes) throws Exception {
    if (obfuscatedBytes == null || obfuscatedBytes.length <= MASK_LENGTH) {
      return new byte[0];
    }
    ByteBuffer buffer = ByteBuffer.wrap(obfuscatedBytes);
    byte[] mask = new byte[MASK_LENGTH];
    buffer.get(mask);
    byte[] masked = new byte[buffer.remaining()];
    buffer.get(masked);
    byte[] keystream = deriveKeystream(mask, masked.length);
    return xor(masked, keystream);
  }

  @Override
  public String getName() {
    return "elligator-swift-lite";
  }

  private byte[] deriveKeystream(byte[] mask, int length) throws Exception {
    MessageDigest digest = MessageDigest.getInstance("SHA-256");
    byte[] output = new byte[length];
    byte counter = 1;
    int offset = 0;
    while (offset < length) {
      digest.reset();
      digest.update(mask);
      digest.update(counter);
      byte[] hash = digest.digest();
      int copy = Math.min(hash.length, length - offset);
      System.arraycopy(hash, 0, output, offset, copy);
      offset += copy;
      counter++;
    }
    return output;
  }

  private byte[] xor(byte[] input, byte[] keystream) {
    byte[] out = new byte[input.length];
    for (int i = 0; i < input.length; i++) {
      out[i] = (byte) (input[i] ^ keystream[i]);
    }
    return out;
  }
}
