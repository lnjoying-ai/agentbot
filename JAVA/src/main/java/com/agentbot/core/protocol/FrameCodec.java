package com.agentbot.core.protocol;

import java.nio.ByteBuffer;

public class FrameCodec {
  private static final int MAX_FRAME_SIZE = 16 * 1024 * 1024;

  private FrameCodec() {}


  public static byte[] encode(byte[] headerBytes, byte[] payloadBytes) {
    int headerLength = headerBytes == null ? 0 : headerBytes.length;
    int payloadLength = payloadBytes == null ? 0 : payloadBytes.length;
    if (headerLength < 0 || payloadLength < 0) {
      throw new IllegalArgumentException("Negative frame length");
    }
    long totalLength = 8L + headerLength + payloadLength;
    if (totalLength > Integer.MAX_VALUE) {
      throw new IllegalArgumentException("Frame too large: " + totalLength);
    }
    if (totalLength > MAX_FRAME_SIZE) {
      throw new IllegalArgumentException("Frame exceeds max size: " + totalLength);
    }

    ByteBuffer buffer = ByteBuffer.allocate((int) totalLength);
    buffer.putInt(headerLength);
    buffer.putInt(payloadLength);
    if (headerLength > 0) {
      buffer.put(headerBytes);
    }
    if (payloadLength > 0) {
      buffer.put(payloadBytes);
    }
    return buffer.array();
  }

  public static Frame decode(byte[] frameBytes) {
    if (frameBytes == null) {
      throw new FrameDecodeException(FrameDecodeException.Reason.NULL_FRAME, "Frame bytes is null");
    }
    if (frameBytes.length < 8) {
      throw new FrameDecodeException(FrameDecodeException.Reason.SHORT_FRAME, "Frame too short: " + frameBytes.length);
    }
    ByteBuffer buffer = ByteBuffer.wrap(frameBytes);
    int headerLength = buffer.getInt();
    int payloadLength = buffer.getInt();
    if (headerLength < 0 || payloadLength < 0) {
      throw new FrameDecodeException(FrameDecodeException.Reason.NEGATIVE_LENGTH, "Negative header/payload length");
    }
    long totalLength = 8L + headerLength + payloadLength;
    if (totalLength > Integer.MAX_VALUE) {
      throw new FrameDecodeException(FrameDecodeException.Reason.LENGTH_OVERFLOW, "Frame length overflow: " + totalLength);
    }
    if (totalLength > MAX_FRAME_SIZE) {
      throw new FrameDecodeException(FrameDecodeException.Reason.LENGTH_EXCEEDED, "Frame length exceeds max: " + totalLength);
    }

    if (totalLength != frameBytes.length) {
      throw new FrameDecodeException(FrameDecodeException.Reason.LENGTH_MISMATCH, "Frame length mismatch: expected=" + totalLength + ", actual=" + frameBytes.length);
    }
    if (buffer.remaining() < headerLength + payloadLength) {
      throw new FrameDecodeException(FrameDecodeException.Reason.BUFFER_UNDERFLOW, "Frame buffer underflow");
    }
    byte[] headerBytes = new byte[headerLength];
    byte[] payloadBytes = new byte[payloadLength];
    if (headerLength > 0) {
      buffer.get(headerBytes);
    }
    if (payloadLength > 0) {
      buffer.get(payloadBytes);
    }
    return new Frame(headerBytes, payloadBytes);
  }


  public record Frame(byte[] headerBytes, byte[] payloadBytes) {}
}
