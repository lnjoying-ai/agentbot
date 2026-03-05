package com.agentbot.core.identity;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.bouncycastle.asn1.x9.X9ECParameters;
import org.bouncycastle.crypto.digests.RIPEMD160Digest;
import org.bouncycastle.crypto.ec.CustomNamedCurves;
import org.bouncycastle.math.ec.ECPoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.HexFormat;

public class NodeIdentityService {
  private static final Logger log = LoggerFactory.getLogger(NodeIdentityService.class);
  private static final String CURVE_NAME = "secp256k1";
  private static final X9ECParameters SECP256K1 = CustomNamedCurves.getByName(CURVE_NAME);
  private static final BigInteger CURVE_N = SECP256K1.getN();
  private static final String BECH32_HRP = "bc";


  private final ObjectMapper objectMapper;
  private final Path nodeFile;
  private final SecureRandom secureRandom = new SecureRandom();

  public NodeIdentityService(ObjectMapper objectMapper, Path nodeFile) {
    this.objectMapper = objectMapper;
    this.nodeFile = nodeFile;
  }

  public NodeIdentity loadOrCreate() {
    NodeIdentity identity = readExisting();
    if (identity != null) {
      return identity;
    }
    NodeIdentity created = createNewIdentity();
    persist(created, true);
    return created;
  }

  private NodeIdentity readExisting() {
    if (!Files.exists(nodeFile)) {
      return null;
    }
    try {
      NodeIdentity identity = objectMapper.readValue(nodeFile.toFile(), NodeIdentity.class);
      return validateExisting(identity);
    } catch (Exception ex) {
      log.warn("node.yml is invalid: path={}", nodeFile, ex);
      throw new IllegalStateException("node.yml is invalid", ex);
    }
  }

  private NodeIdentity validateExisting(NodeIdentity identity) {
    if (identity == null || identity.getPrivateKeyHex() == null || identity.getPrivateKeyHex().isBlank()) {
      throw new IllegalStateException("node.yml missing privateKeyHex");

    }
    String normalized = normalizeHex(identity.getPrivateKeyHex());
    if (!isValidPrivateKey(normalized)) {
      throw new IllegalStateException("node.yml has invalid privateKeyHex");

    }
    NodeIdentity expected = rebuildFromPrivateKey(normalized, identity.getCreatedAt());

    String existingPubKey = identity.getPublicKeyHex() == null ? "" : normalizeHex(identity.getPublicKeyHex());
    if (!existingPubKey.equals(expected.getPublicKeyHex())) {
      throw new IllegalStateException("node.yml publicKeyHex mismatch");

    }
    if (!identity.isPubKeyCompressed()) {
      throw new IllegalStateException("node.yml pubKeyCompressed must be true");

    }
    if (identity.getCurve() == null || !identity.getCurve().equalsIgnoreCase(CURVE_NAME)) {
      throw new IllegalStateException("node.yml curve mismatch");

    }
    if (!safeEquals(identity.getNodeIdBech32(), expected.getNodeIdBech32())) {

      throw new IllegalStateException("node.yml nodeIdBech32 mismatch");

    }

    return expected;
  }

  private boolean safeEquals(String left, String right) {
    if (left == null && right == null) return true;
    if (left == null || right == null) return false;
    return left.equals(right);
  }


  private NodeIdentity createNewIdentity() {
    BigInteger privateKey = generatePrivateKey();
    String privateKeyHex = toFixedHex(privateKey);
    return rebuildFromPrivateKey(privateKeyHex, Instant.now().toString());
  }

  private NodeIdentity rebuildFromPrivateKey(String privateKeyHex, String createdAt) {
    BigInteger privKey = new BigInteger(privateKeyHex, 16);
    ECPoint publicPoint = SECP256K1.getG().multiply(privKey).normalize();
    byte[] publicKeyCompressed = publicPoint.getEncoded(true);
    byte[] publicKeyHash = hash160(publicKeyCompressed);

    String nodeIdBech32 = Bech32.encodeWitnessAddress(BECH32_HRP, 0, publicKeyHash);

    NodeIdentity identity = NodeIdentity.from(
        privateKeyHex,
        HexFormat.of().formatHex(publicKeyCompressed),
        true,
        CURVE_NAME,
        nodeIdBech32
    );

    identity.setCreatedAt(createdAt == null ? Instant.now().toString() : createdAt);
    return identity;
  }

  private void persist(NodeIdentity identity, boolean overwrite) {
    try {
      Files.createDirectories(nodeFile.getParent());
      if (!overwrite && Files.exists(nodeFile)) {
        objectMapper.writerWithDefaultPrettyPrinter().writeValue(nodeFile.toFile(), identity);
        return;
      }
      objectMapper.writerWithDefaultPrettyPrinter().writeValue(nodeFile.toFile(), identity);
    } catch (IOException ex) {
      log.warn("node.yml persist failed: path={}", nodeFile, ex);
    }
  }

  private BigInteger generatePrivateKey() {
    while (true) {
      byte[] bytes = new byte[32];
      secureRandom.nextBytes(bytes);
      BigInteger candidate = new BigInteger(1, bytes);
      if (candidate.signum() > 0 && candidate.compareTo(CURVE_N) < 0) {
        return candidate;
      }
    }
  }

  private boolean isValidPrivateKey(String hex) {
    if (hex == null || hex.length() != 64) {
      return false;
    }
    try {
      BigInteger key = new BigInteger(hex, 16);
      return key.signum() > 0 && key.compareTo(CURVE_N) < 0;
    } catch (Exception ex) {
      return false;
    }
  }

  private String normalizeHex(String input) {
    String trimmed = input.trim();
    if (trimmed.startsWith("0x") || trimmed.startsWith("0X")) {
      trimmed = trimmed.substring(2);
    }
    return trimmed.toLowerCase();
  }

  private String toFixedHex(BigInteger key) {
    byte[] bytes = key.toByteArray();
    byte[] padded = new byte[32];
    int offset = Math.max(0, bytes.length - 32);
    int length = Math.min(bytes.length, 32);
    System.arraycopy(bytes, offset, padded, 32 - length, length);
    return HexFormat.of().formatHex(padded);
  }

  public static String nodeIdFromPublicKeyHex(String publicKeyHex) {
    if (publicKeyHex == null || publicKeyHex.isBlank()) return null;
    byte[] pubBytes = HexFormat.of().parseHex(publicKeyHex.trim());
    byte[] publicKeyHash = hash160Internal(pubBytes);

    return Bech32.encodeWitnessAddress(BECH32_HRP, 0, publicKeyHash);
  }

  private byte[] hash160(byte[] input) {
    return hash160Internal(input);
  }

  private static byte[] hash160Internal(byte[] input) {

    try {
      MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
      byte[] sha = sha256.digest(input);
      RIPEMD160Digest digest = new RIPEMD160Digest();
      digest.update(sha, 0, sha.length);
      byte[] out = new byte[digest.getDigestSize()];
      digest.doFinal(out, 0);
      return out;
    } catch (Exception ex) {
      throw new IllegalStateException("hash160 failed", ex);
    }
  }


}

