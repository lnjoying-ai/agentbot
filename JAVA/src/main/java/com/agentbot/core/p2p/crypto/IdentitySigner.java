package com.agentbot.core.p2p.crypto;

import org.bouncycastle.jce.ECNamedCurveTable;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.jce.spec.ECNamedCurveParameterSpec;
import org.bouncycastle.jce.spec.ECNamedCurveSpec;
import org.bouncycastle.jce.spec.ECPublicKeySpec;

import java.math.BigInteger;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Security;
import java.security.Signature;
import java.security.spec.ECPrivateKeySpec;
import java.util.Base64;
import java.util.HexFormat;


public final class IdentitySigner {
  private IdentitySigner() {}

  static {
    if (Security.getProvider("BC") == null) {
      Security.addProvider(new BouncyCastleProvider());
    }
  }

  public static String sign(byte[] data, String privateKeyHex) throws Exception {
    if (privateKeyHex == null || privateKeyHex.isBlank()) {
      throw new IllegalArgumentException("private key missing");
    }
    ECNamedCurveParameterSpec params = ECNamedCurveTable.getParameterSpec("secp256k1");
    ECNamedCurveSpec spec = new ECNamedCurveSpec("secp256k1", params.getCurve(), params.getG(), params.getN());
    BigInteger priv = new BigInteger(privateKeyHex, 16);
    KeyFactory keyFactory = KeyFactory.getInstance("EC", "BC");
    PrivateKey privateKey = keyFactory.generatePrivate(new ECPrivateKeySpec(priv, spec));
    Signature signer = Signature.getInstance("SHA256withECDSA", "BC");
    signer.initSign(privateKey);
    signer.update(data);
    return Base64.getEncoder().encodeToString(signer.sign());
  }

  public static boolean verify(byte[] data, String publicKeyHex, String signatureBase64) throws Exception {
    if (publicKeyHex == null || publicKeyHex.isBlank() || signatureBase64 == null || signatureBase64.isBlank()) {
      return false;
    }
    ECNamedCurveParameterSpec params = ECNamedCurveTable.getParameterSpec("secp256k1");
    byte[] pubBytes = HexFormat.of().parseHex(publicKeyHex);
    var point = params.getCurve().decodePoint(pubBytes);
    KeyFactory keyFactory = KeyFactory.getInstance("EC", "BC");
    PublicKey publicKey = keyFactory.generatePublic(new ECPublicKeySpec(point, params));

    Signature verifier = Signature.getInstance("SHA256withECDSA", "BC");
    verifier.initVerify(publicKey);
    verifier.update(data);
    byte[] sig = Base64.getDecoder().decode(signatureBase64);
    return verifier.verify(sig);
  }
}
