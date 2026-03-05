package com.agentbot.core.p2p.crypto;

import org.bouncycastle.jce.ECNamedCurveTable;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.jce.spec.ECNamedCurveParameterSpec;
import org.bouncycastle.jce.spec.ECNamedCurveSpec;
import org.bouncycastle.jce.spec.ECPublicKeySpec;

import javax.crypto.KeyAgreement;
import java.math.BigInteger;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Security;
import java.security.spec.ECPrivateKeySpec;
import java.util.HexFormat;

public final class EcdhKeyAgreement {
  private EcdhKeyAgreement() {}

  static {
    if (Security.getProvider("BC") == null) {
      Security.addProvider(new BouncyCastleProvider());
    }
  }

  public static byte[] computeSharedSecret(String privateKeyHex, String publicKeyHex) throws Exception {
    if (privateKeyHex == null || privateKeyHex.isBlank() || publicKeyHex == null || publicKeyHex.isBlank()) {
      throw new IllegalArgumentException("missing ecdh key material");
    }
    ECNamedCurveParameterSpec params = ECNamedCurveTable.getParameterSpec("secp256k1");
    ECNamedCurveSpec spec = new ECNamedCurveSpec("secp256k1", params.getCurve(), params.getG(), params.getN());

    BigInteger priv = new BigInteger(privateKeyHex, 16);
    KeyFactory keyFactory = KeyFactory.getInstance("EC", "BC");
    PrivateKey privateKey = keyFactory.generatePrivate(new ECPrivateKeySpec(priv, spec));

    byte[] pubBytes = HexFormat.of().parseHex(publicKeyHex);
    var point = params.getCurve().decodePoint(pubBytes);
    PublicKey publicKey = keyFactory.generatePublic(new ECPublicKeySpec(point, params));

    KeyAgreement agreement = KeyAgreement.getInstance("ECDH", "BC");
    agreement.init(privateKey);
    agreement.doPhase(publicKey, true);
    return agreement.generateSecret();
  }
}
