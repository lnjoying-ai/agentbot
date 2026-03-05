package com.agentbot.core.p2p.crypto;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;

import java.util.concurrent.atomic.AtomicLong;

public class SecureChannel {
  private static final int NONCE_LENGTH = 12;
  private static final int GCM_TAG_BITS = 128;
  private static final long REKEY_INTERVAL = 1L << 20;

  private final CipherSuite cipherSuite;
  private byte[] sendKey;
  private byte[] recvKey;
  private final AtomicLong sendCounter = new AtomicLong(0);
  private final AtomicLong recvCounter = new AtomicLong(0);

  public SecureChannel(CipherSuite cipherSuite, byte[] sendKey, byte[] recvKey) {
    this.cipherSuite = cipherSuite;
    this.sendKey = sendKey;
    this.recvKey = recvKey;
  }

  public byte[] encrypt(byte[] plaintext, byte[] aad) throws Exception {
    byte[] nonce = nextNonce(sendCounter, true);
    Cipher cipher = Cipher.getInstance(cipherSuite.getJceName());
    if (cipherSuite == CipherSuite.AES_GCM_256) {
      cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(sendKey, "AES"), new GCMParameterSpec(GCM_TAG_BITS, nonce));
    } else {
      cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(sendKey, "ChaCha20"), new javax.crypto.spec.IvParameterSpec(nonce));
    }
    if (aad != null && aad.length > 0) {
      cipher.updateAAD(aad);
    }
    byte[] ciphertext = cipher.doFinal(plaintext);
    ByteBuffer buffer = ByteBuffer.allocate(nonce.length + ciphertext.length);
    buffer.put(nonce);
    buffer.put(ciphertext);
    return buffer.array();
  }

  public byte[] decrypt(byte[] ciphertextWithNonce, byte[] aad) throws Exception {
    ByteBuffer buffer = ByteBuffer.wrap(ciphertextWithNonce);
    byte[] nonce = new byte[NONCE_LENGTH];
    buffer.get(nonce);
    byte[] ciphertext = new byte[buffer.remaining()];
    buffer.get(ciphertext);
    Cipher cipher = Cipher.getInstance(cipherSuite.getJceName());
    if (cipherSuite == CipherSuite.AES_GCM_256) {
      cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(recvKey, "AES"), new GCMParameterSpec(GCM_TAG_BITS, nonce));
    } else {
      cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(recvKey, "ChaCha20"), new javax.crypto.spec.IvParameterSpec(nonce));
    }
    if (aad != null && aad.length > 0) {
      cipher.updateAAD(aad);
    }
    recvCounter.incrementAndGet();
    return cipher.doFinal(ciphertext);
  }

  public boolean shouldRekeySend() {
    return sendCounter.get() >= REKEY_INTERVAL;
  }

  public boolean shouldRekeyRecv() {
    return recvCounter.get() >= REKEY_INTERVAL;
  }

  public void resetCounters() {
    sendCounter.set(0);
    recvCounter.set(0);
  }

  public void updateKeys(byte[] newSendKey, byte[] newRecvKey) {
    this.sendKey = newSendKey;
    this.recvKey = newRecvKey;
    resetCounters();
  }

  private byte[] nextNonce(AtomicLong counter, boolean outbound) {
    long value = counter.incrementAndGet();
    byte[] nonce = new byte[NONCE_LENGTH];
    ByteBuffer buffer = ByteBuffer.wrap(nonce);
    buffer.putLong(value);
    buffer.putInt(outbound ? 1 : 0);
    return nonce;
  }
}

