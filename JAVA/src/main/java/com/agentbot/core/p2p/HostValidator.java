package com.agentbot.core.p2p;

import java.net.InetAddress;

public final class HostValidator {
  private HostValidator() {}

  public static boolean isValidHost(String host) {
    return isValidIpLiteral(host) || isValidHostname(host);
  }

  public static boolean isPublicHost(String host) {
    if (isValidIpLiteral(host)) {
      return isPublicIp(host);
    }
    if (isValidHostname(host)) {
      return isPublicHostname(host);
    }
    return false;
  }

  public static boolean isValidIpLiteral(String host) {
    if (host == null || host.isBlank()) return false;
    if (host.contains(":")) {
      if (!host.matches("^[0-9a-fA-F:.]+$")) return false;
      try {
        InetAddress address = InetAddress.getByName(host);
        return address.getHostAddress().contains(":");
      } catch (Exception ex) {
        return false;
      }
    }
    if (!host.matches("^((25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)\\.){3}(25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)$")) {
      return false;
    }
    try {
      InetAddress address = InetAddress.getByName(host);
      return address.getHostAddress().equals(host);
    } catch (Exception ex) {
      return false;
    }
  }

  public static boolean isValidHostname(String host) {
    if (host == null || host.isBlank()) return false;
    if (host.length() > 253) return false;
    String h = host.endsWith(".") ? host.substring(0, host.length() - 1) : host;
    if (h.isBlank()) return false;
    if (!h.matches("^[A-Za-z0-9.-]+$") || h.contains("..")) return false;
    String[] labels = h.split("\\.");
    for (String label : labels) {
      if (label.isEmpty() || label.length() > 63) return false;
      if (!label.matches("^[A-Za-z0-9](?:[A-Za-z0-9-]*[A-Za-z0-9])?$") && !label.matches("^[A-Za-z0-9]$")) {
        return false;
      }
    }
    return true;
  }

  public static boolean isPublicIp(String host) {
    try {
      InetAddress address = InetAddress.getByName(host);
      return isPublicAddress(address);
    } catch (Exception ex) {
      return false;
    }
  }

  public static boolean isPublicHostname(String host) {
    try {
      InetAddress[] addresses = InetAddress.getAllByName(host);
      if (addresses == null || addresses.length == 0) return false;
      for (InetAddress address : addresses) {
        if (isPublicAddress(address)) return true;
      }
      return false;
    } catch (Exception ex) {
      return false;
    }
  }

  private static boolean isPublicAddress(InetAddress address) {
    if (address.isAnyLocalAddress() || address.isLoopbackAddress() || address.isLinkLocalAddress()
        || address.isMulticastAddress()) {
      return false;
    }
    byte[] bytes = address.getAddress();
    if (bytes.length == 4) {
      int b0 = bytes[0] & 0xFF;
      int b1 = bytes[1] & 0xFF;
      if (b0 == 0 || b0 == 10 || b0 == 127) return false;
      if (b0 == 169 && b1 == 254) return false;
      if (b0 == 172 && b1 >= 16 && b1 <= 31) return false;
      if (b0 == 192 && b1 == 168) return false;
      if (b0 == 100 && b1 >= 64 && b1 <= 127) return false;
      if (b0 >= 224) return false;
      return true;
    }
    if (bytes.length == 16) {
      int b0 = bytes[0] & 0xFF;
      int b1 = bytes[1] & 0xFF;
      if ((b0 & 0xFE) == 0xFC) return false; // fc00::/7
      if (b0 == 0xFE && (b1 & 0xC0) == 0x80) return false; // fe80::/10
      if (b0 == 0xFF) return false; // multicast
      return true;
    }
    return false;
  }
}
