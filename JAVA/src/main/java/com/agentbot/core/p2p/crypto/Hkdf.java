package com.agentbot.core.p2p.crypto;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.util.Arrays;

public class Hkdf {
  private Hkdf() {}

  public static byte[] extract(byte[] salt, byte[] ikm) throws Exception {
    Mac mac = Mac.getInstance("HmacSHA256");
    byte[] key = salt == null ? new byte[32] : salt;
    mac.init(new SecretKeySpec(key, "HmacSHA256"));
    return mac.doFinal(ikm);
  }

  public static byte[] expand(byte[] prk, byte[] info, int length) throws Exception {
    Mac mac = Mac.getInstance("HmacSHA256");
    mac.init(new SecretKeySpec(prk, "HmacSHA256"));
    byte[] result = new byte[length];
    byte[] t = new byte[0];
    int pos = 0;
    byte counter = 1;
    while (pos < length) {
      mac.reset();
      mac.update(t);
      if (info != null) {
        mac.update(info);
      }
      mac.update(counter);
      t = mac.doFinal();
      int copy = Math.min(t.length, length - pos);
      System.arraycopy(t, 0, result, pos, copy);
      pos += copy;
      counter++;
    }
    return result;
  }

  public static byte[] deriveKey(byte[] ikm, byte[] salt, byte[] info, int length) throws Exception {
    byte[] prk = extract(salt, ikm);
    return expand(prk, info, length);
  }

  public static byte[] sha256Truncate(byte[] data, int length) {
    if (data.length == length) return data;
    return Arrays.copyOf(data, length);
  }
}
