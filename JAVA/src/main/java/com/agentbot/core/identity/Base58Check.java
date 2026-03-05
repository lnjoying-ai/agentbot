package com.agentbot.core.identity;

import java.security.MessageDigest;
import java.util.Arrays;

public final class Base58Check {
  private static final char[] ALPHABET = "123456789ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz".toCharArray();

  private Base58Check() {
  }

  public static String encode(byte version, byte[] payload) {
    byte[] data = new byte[1 + payload.length];
    data[0] = version;
    System.arraycopy(payload, 0, data, 1, payload.length);
    byte[] checksum = checksum(data);
    byte[] full = new byte[data.length + 4];
    System.arraycopy(data, 0, full, 0, data.length);
    System.arraycopy(checksum, 0, full, data.length, 4);
    return encodeBase58(full);
  }

  private static byte[] checksum(byte[] data) {
    try {
      MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
      byte[] first = sha256.digest(data);
      byte[] second = sha256.digest(first);
      return Arrays.copyOf(second, 4);
    } catch (Exception ex) {
      throw new IllegalStateException("base58 checksum failed", ex);
    }
  }

  private static String encodeBase58(byte[] input) {
    if (input.length == 0) return "";
    int zeros = 0;
    while (zeros < input.length && input[zeros] == 0) {
      zeros++;
    }
    byte[] copy = Arrays.copyOf(input, input.length);
    StringBuilder sb = new StringBuilder();
    int startAt = zeros;
    while (startAt < copy.length) {
      int mod = divmod58(copy, startAt);
      sb.append(ALPHABET[mod]);
      while (startAt < copy.length && copy[startAt] == 0) {
        startAt++;
      }
    }
    for (int i = 0; i < zeros; i++) {
      sb.append(ALPHABET[0]);
    }
    return sb.reverse().toString();
  }

  private static int divmod58(byte[] number, int startAt) {
    int remainder = 0;
    for (int i = startAt; i < number.length; i++) {
      int digit = number[i] & 0xFF;
      int temp = remainder * 256 + digit;
      number[i] = (byte) (temp / 58);
      remainder = temp % 58;
    }
    return remainder;
  }
}
