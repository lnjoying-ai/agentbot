package com.agentbot.core.identity;

import java.util.ArrayList;
import java.util.List;

public final class Bech32 {
  private static final String CHARSET = "qpzry9x8gf2tvdw0s3jn54khce6mua7l";
  private static final int[] GENERATOR = new int[]{0x3b6a57b2, 0x26508e6d, 0x1ea119fa, 0x3d4233dd, 0x2a1462b3};

  private Bech32() {
  }

  public static String encodeWitnessAddress(String hrp, int witver, byte[] program) {
    if (witver < 0 || witver > 16) {
      throw new IllegalArgumentException("invalid witness version");
    }
    List<Integer> data = new ArrayList<>();
    data.add(witver);
    data.addAll(convertBits(program, 8, 5, true));
    return encode(hrp, data);
  }

  private static String encode(String hrp, List<Integer> data) {
    StringBuilder sb = new StringBuilder();
    sb.append(hrp.toLowerCase());
    sb.append('1');
    for (int value : data) {
      sb.append(CHARSET.charAt(value));
    }
    List<Integer> checksum = createChecksum(hrp, data);
    for (int value : checksum) {
      sb.append(CHARSET.charAt(value));
    }
    return sb.toString();
  }

  private static List<Integer> createChecksum(String hrp, List<Integer> values) {
    List<Integer> expanded = hrpExpand(hrp);
    List<Integer> combined = new ArrayList<>(expanded.size() + values.size() + 6);
    combined.addAll(expanded);
    combined.addAll(values);
    combined.addAll(List.of(0, 0, 0, 0, 0, 0));
    int polymod = polymod(combined) ^ 1;
    List<Integer> checksum = new ArrayList<>();
    for (int i = 0; i < 6; i++) {
      checksum.add((polymod >> (5 * (5 - i))) & 31);
    }
    return checksum;
  }

  private static int polymod(List<Integer> values) {
    int chk = 1;
    for (int v : values) {
      int top = chk >>> 25;
      chk = ((chk & 0x1ffffff) << 5) ^ v;
      for (int i = 0; i < 5; i++) {
        if (((top >>> i) & 1) == 1) {
          chk ^= GENERATOR[i];
        }
      }
    }
    return chk;
  }

  private static List<Integer> hrpExpand(String hrp) {
    List<Integer> result = new ArrayList<>(hrp.length() * 2 + 1);
    for (char c : hrp.toCharArray()) {
      result.add(c >> 5);
    }
    result.add(0);
    for (char c : hrp.toCharArray()) {
      result.add(c & 31);
    }
    return result;
  }

  private static List<Integer> convertBits(byte[] data, int fromBits, int toBits, boolean pad) {
    int acc = 0;
    int bits = 0;
    int maxv = (1 << toBits) - 1;
    List<Integer> ret = new ArrayList<>();
    for (byte value : data) {
      int b = value & 0xff;
      if (b >> fromBits != 0) {
        throw new IllegalArgumentException("invalid data range");
      }
      acc = (acc << fromBits) | b;
      bits += fromBits;
      while (bits >= toBits) {
        bits -= toBits;
        ret.add((acc >> bits) & maxv);
      }
    }
    if (pad) {
      if (bits > 0) {
        ret.add((acc << (toBits - bits)) & maxv);
      }
    } else if (bits >= fromBits || ((acc << (toBits - bits)) & maxv) != 0) {
      throw new IllegalArgumentException("invalid padding");
    }
    return ret;
  }
}
