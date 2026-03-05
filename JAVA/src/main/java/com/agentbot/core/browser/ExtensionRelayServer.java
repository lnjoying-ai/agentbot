package com.agentbot.core.browser;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.HttpUtil;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.QueryStringDecoder;
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;
import io.netty.handler.codec.http.websocketx.CloseWebSocketFrame;
import io.netty.handler.codec.http.websocketx.PingWebSocketFrame;
import io.netty.handler.codec.http.websocketx.PongWebSocketFrame;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketServerHandshaker;
import io.netty.handler.codec.http.websocketx.WebSocketServerHandshakerFactory;
import io.netty.util.AttributeKey;
import io.netty.util.CharsetUtil;
import io.netty.util.ReferenceCountUtil;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class ExtensionRelayServer {
  private static final AttributeKey<String> ATTR_ROLE = AttributeKey.valueOf("relayRole");
  private static final AttributeKey<String> ATTR_TARGET_ID = AttributeKey.valueOf("relayTargetId");
  private static final AttributeKey<Channel> ATTR_PEER = AttributeKey.valueOf("relayPeer");
  private static final AttributeKey<WebSocketServerHandshaker> ATTR_HANDSHAKER = AttributeKey.valueOf("relayHandshaker");

  private final ObjectMapper mapper = new ObjectMapper();
  private final int port;
  private final Map<String, TargetInfo> targets = new ConcurrentHashMap<>();
  private final AtomicInteger idCounter = new AtomicInteger(1);

  private EventLoopGroup bossGroup;
  private EventLoopGroup workerGroup;
  private Channel serverChannel;

  public ExtensionRelayServer(int port) {
    this.port = port;
  }

  public synchronized void start() {
    if (serverChannel != null && serverChannel.isActive()) return;
    bossGroup = new NioEventLoopGroup(1);
    workerGroup = new NioEventLoopGroup();
    try {
      ServerBootstrap bootstrap = new ServerBootstrap();
      bootstrap.group(bossGroup, workerGroup)
          .channel(NioServerSocketChannel.class)
          .childHandler(new RelayInitializer());
      ChannelFuture future = bootstrap.bind(new InetSocketAddress("127.0.0.1", port)).syncUninterruptibly();
      serverChannel = future.channel();
    } catch (Exception e) {
      stop();
      throw new RuntimeException("Failed to start extension relay on port " + port, e);
    }
  }

  public synchronized void stop() {
    if (serverChannel != null) {
      serverChannel.close().syncUninterruptibly();
      serverChannel = null;
    }
    if (bossGroup != null) {
      bossGroup.shutdownGracefully();
      bossGroup = null;
    }
    if (workerGroup != null) {
      workerGroup.shutdownGracefully();
      workerGroup = null;
    }
    targets.clear();
  }

  public boolean isRunning() {
    return serverChannel != null && serverChannel.isActive();
  }

  private class RelayInitializer extends ChannelInitializer<SocketChannel> {
    @Override
    protected void initChannel(SocketChannel ch) {
      ChannelPipeline pipeline = ch.pipeline();
      pipeline.addLast(new HttpServerCodec());
      pipeline.addLast(new HttpObjectAggregator(2 * 1024 * 1024));
      pipeline.addLast(new RelayHandler());
    }
  }

  private class RelayHandler extends SimpleChannelInboundHandler<Object> {
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Object msg) throws Exception {
      if (msg instanceof FullHttpRequest request) {
        handleHttpRequest(ctx, request);
        return;
      }
      if (msg instanceof WebSocketFrame frame) {
        handleWebSocketFrame(ctx, frame);
      }
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
      String role = ctx.channel().attr(ATTR_ROLE).get();
      String targetId = ctx.channel().attr(ATTR_TARGET_ID).get();
      if (targetId != null) {
        TargetInfo info = targets.get(targetId);
        if (info != null) {
          if ("extension".equals(role)) {
            targets.remove(targetId);
            if (info.cdpChannel != null) info.cdpChannel.close();
          } else if ("cdp".equals(role)) {
            info.cdpChannel = null;
          }
        }
      }
      Channel peer = ctx.channel().attr(ATTR_PEER).get();
      if (peer != null && peer.isActive()) {
        peer.attr(ATTR_PEER).set(null);
      }
    }

    private void handleHttpRequest(ChannelHandlerContext ctx, FullHttpRequest request) throws Exception {
      try {
        if (!request.decoderResult().isSuccess()) {
          sendResponse(ctx, request, HttpResponseStatus.BAD_REQUEST, "Bad request");
          return;
        }
        if (isWebSocketUpgrade(request)) {
          handleWebSocketHandshake(ctx, request);
          return;
        }
        if (!"GET".equalsIgnoreCase(request.method().name())) {
          sendResponse(ctx, request, HttpResponseStatus.METHOD_NOT_ALLOWED, "Method not allowed");
          return;
        }
        String path = new QueryStringDecoder(request.uri()).path();
        if ("/json/version".equals(path) || "/json/version/".equals(path)) {
          sendJson(ctx, request, buildVersionPayload());
          return;
        }
        if ("/json".equals(path) || "/json/".equals(path) || "/json/list".equals(path) || "/json/list/".equals(path)) {
          sendJson(ctx, request, buildTargetList());
          return;
        }
        sendResponse(ctx, request, HttpResponseStatus.NOT_FOUND, "Not found");
      } finally {
        ReferenceCountUtil.release(request);
      }
    }

    private void handleWebSocketHandshake(ChannelHandlerContext ctx, FullHttpRequest request) {
      String path = new QueryStringDecoder(request.uri()).path();
      String role;
      String targetId;
      if ("/extension".equals(path) || "/extension/".equals(path)) {
        role = "extension";
        targetId = nextTargetId();
      } else if (path.startsWith("/devtools/page/")) {
        role = "cdp";
        targetId = path.substring("/devtools/page/".length());
        if (!targets.containsKey(targetId)) {
          sendResponse(ctx, request, HttpResponseStatus.NOT_FOUND, "Target not found");
          return;
        }
      } else if ("/devtools/browser".equals(path) || "/devtools/browser/".equals(path)) {
        role = "cdp";
        targetId = firstTargetId();
        if (targetId == null) {
          sendResponse(ctx, request, HttpResponseStatus.NOT_FOUND, "No target attached");
          return;
        }
      } else {
        sendResponse(ctx, request, HttpResponseStatus.NOT_FOUND, "Not found");
        return;
      }
      String wsLocation = getWebSocketLocation(request, path);
      WebSocketServerHandshakerFactory factory = new WebSocketServerHandshakerFactory(wsLocation, null, true);
      WebSocketServerHandshaker handshaker = factory.newHandshaker(request);
      if (handshaker == null) {
        WebSocketServerHandshakerFactory.sendUnsupportedVersionResponse(ctx.channel());
        return;
      }
      ctx.channel().attr(ATTR_ROLE).set(role);
      ctx.channel().attr(ATTR_TARGET_ID).set(targetId);
      ctx.channel().attr(ATTR_HANDSHAKER).set(handshaker);
      handshaker.handshake(ctx.channel(), request);
      if ("extension".equals(role)) {
        registerExtension(ctx.channel(), targetId);
      } else {
        registerCdp(ctx.channel(), targetId);
      }
    }

    private void handleWebSocketFrame(ChannelHandlerContext ctx, WebSocketFrame frame) throws Exception {
      if (frame instanceof CloseWebSocketFrame closeFrame) {
        WebSocketServerHandshaker handshaker = ctx.channel().attr(ATTR_HANDSHAKER).get();
        if (handshaker != null) {
          handshaker.close(ctx.channel(), closeFrame.retain());
        } else {
          ctx.channel().close();
        }
        return;
      }
      if (frame instanceof PingWebSocketFrame) {
        ctx.channel().writeAndFlush(new PongWebSocketFrame(frame.content().retain()));
        return;
      }
      if (frame instanceof PongWebSocketFrame) return;

      Channel peer = ctx.channel().attr(ATTR_PEER).get();
      if (peer == null || !peer.isActive()) return;

      if (frame instanceof TextWebSocketFrame textFrame) {
        if ("extension".equals(ctx.channel().attr(ATTR_ROLE).get())) {
          maybeUpdateTargetMeta(ctx.channel(), textFrame.text());
        }
        peer.writeAndFlush(new TextWebSocketFrame(textFrame.text()));
        return;
      }
      if (frame instanceof BinaryWebSocketFrame) {
        peer.writeAndFlush(frame.retain());
      }
    }
  }

  private void registerExtension(Channel channel, String targetId) {
    TargetInfo info = new TargetInfo(targetId, "Extension Tab", "about:blank", channel);
    targets.put(targetId, info);
    channel.attr(ATTR_PEER).set(null);
  }

  private void registerCdp(Channel channel, String targetId) {
    TargetInfo info = targets.get(targetId);
    if (info == null || info.extensionChannel == null || !info.extensionChannel.isActive()) {
      channel.close();
      return;
    }
    if (info.cdpChannel != null && info.cdpChannel.isActive()) {
      info.cdpChannel.close();
    }
    info.cdpChannel = channel;
    channel.attr(ATTR_PEER).set(info.extensionChannel);
    info.extensionChannel.attr(ATTR_PEER).set(channel);
  }

  private void maybeUpdateTargetMeta(Channel channel, String payload) {
    String targetId = channel.attr(ATTR_TARGET_ID).get();
    if (targetId == null || payload == null || payload.isBlank()) return;
    try {
      Map<?, ?> json = mapper.readValue(payload, Map.class);
      Object type = json.get("type");
      if (type == null || !"hello".equalsIgnoreCase(String.valueOf(type))) return;
      String title = json.get("title") == null ? "" : String.valueOf(json.get("title"));
      String url = json.get("url") == null ? "" : String.valueOf(json.get("url"));
      TargetInfo info = targets.get(targetId);
      if (info != null) {
        if (!title.isBlank()) info.title = title;
        if (!url.isBlank()) info.url = url;
      }
    } catch (Exception ignored) {
    }
  }

  private Map<String, Object> buildVersionPayload() {
    Map<String, Object> payload = new HashMap<>();
    payload.put("Browser", "Agentbot/extension-relay");
    payload.put("Protocol-Version", "1.3");
    String targetId = firstTargetId();
    if (targetId != null) {
      payload.put("webSocketDebuggerUrl", "ws://127.0.0.1:" + port + "/devtools/page/" + targetId);
    }
    return payload;
  }

  private List<Map<String, Object>> buildTargetList() {
    List<Map<String, Object>> list = new ArrayList<>();
    for (TargetInfo info : targets.values()) {
      Map<String, Object> item = new HashMap<>();
      item.put("id", info.id);
      item.put("type", "page");
      item.put("title", info.title);
      item.put("url", info.url);
      item.put("webSocketDebuggerUrl", "ws://127.0.0.1:" + port + "/devtools/page/" + info.id);
      list.add(item);
    }
    return list;
  }

  private String firstTargetId() {
    return targets.keySet().stream().findFirst().orElse(null);
  }

  private String nextTargetId() {
    return "t" + idCounter.getAndIncrement();
  }

  private void sendJson(ChannelHandlerContext ctx, FullHttpRequest request, Object body) throws Exception {
    byte[] data = mapper.writeValueAsBytes(body);
    FullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK, Unpooled.wrappedBuffer(data));
    response.headers().set(HttpHeaderNames.CONTENT_TYPE, "application/json; charset=utf-8");
    response.headers().setInt(HttpHeaderNames.CONTENT_LENGTH, data.length);
    writeAndFlush(ctx, request, response);
  }

  private void sendResponse(ChannelHandlerContext ctx, FullHttpRequest request, HttpResponseStatus status, String message) {
    byte[] data = message == null ? new byte[0] : message.getBytes(CharsetUtil.UTF_8);
    FullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, status, Unpooled.wrappedBuffer(data));
    response.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/plain; charset=utf-8");
    response.headers().setInt(HttpHeaderNames.CONTENT_LENGTH, data.length);
    writeAndFlush(ctx, request, response);
  }

  private void writeAndFlush(ChannelHandlerContext ctx, FullHttpRequest request, FullHttpResponse response) {
    boolean keepAlive = HttpUtil.isKeepAlive(request);
    if (keepAlive) {
      response.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.KEEP_ALIVE);
    }
    ChannelFuture future = ctx.writeAndFlush(response);
    if (!keepAlive) {
      future.addListener(f -> ctx.close());
    }
  }

  private boolean isWebSocketUpgrade(FullHttpRequest request) {
    String upgrade = request.headers().get(HttpHeaderNames.UPGRADE);
    return upgrade != null && "websocket".equalsIgnoreCase(upgrade);
  }

  private String getWebSocketLocation(FullHttpRequest request, String path) {
    String host = request.headers().get(HttpHeaderNames.HOST);
    if (host == null || host.isBlank()) {
      host = "127.0.0.1:" + port;
    }
    return "ws://" + host + path;
  }

  private static class TargetInfo {
    private final String id;
    private volatile String title;
    private volatile String url;
    private final Channel extensionChannel;
    private volatile Channel cdpChannel;

    private TargetInfo(String id, String title, String url, Channel extensionChannel) {
      this.id = id;
      this.title = title;
      this.url = url;
      this.extensionChannel = extensionChannel;
    }
  }
}
