package com.agentbot.core.p2p.crypto;

import org.bouncycastle.jce.provider.BouncyCastleProvider;

import javax.crypto.KeyAgreement;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Security;
import java.security.spec.ECGenParameterSpec;
import java.security.spec.X509EncodedKeySpec;

public class KeyExchange {
  static {
    if (Security.getProvider("BC") == null) {
      Security.addProvider(new BouncyCastleProvider());
    }
  }

  private final KeyPair keyPair;

  public KeyExchange() throws Exception {
    KeyPairGenerator generator = KeyPairGenerator.getInstance("EC", "BC");
    generator.initialize(new ECGenParameterSpec("secp256k1"));
    this.keyPair = generator.generateKeyPair();
  }

  public byte[] getPublicKeyEncoded() {
    return keyPair.getPublic().getEncoded();
  }

  public byte[] computeSharedSecret(byte[] peerPublicKey) throws Exception {
    KeyFactory keyFactory = KeyFactory.getInstance("EC", "BC");
    PublicKey remote = keyFactory.generatePublic(new X509EncodedKeySpec(peerPublicKey));
    KeyAgreement agreement = KeyAgreement.getInstance("ECDH", "BC");
    agreement.init(keyPair.getPrivate());
    agreement.doPhase(remote, true);
    return agreement.generateSecret();
  }

  public PrivateKey getPrivateKey() {
    return keyPair.getPrivate();
  }
}
