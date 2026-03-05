package com.agentbot.core.p2p;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.LengthFieldPrepender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class P2pServer {
  private static final Logger log = LoggerFactory.getLogger(P2pServer.class);
  private static final int MAX_FRAME_SIZE = 16 * 1024 * 1024;
  private static final int BACKLOG = 4096;

  private final int port;
  private final P2pServerHandler handler;
  private volatile boolean running;
  private EventLoopGroup bossGroup;
  private EventLoopGroup workerGroup;
  private Channel serverChannel;

  public interface P2pServerHandler {
    void handle(Channel channel);
  }

  public P2pServer(int port, P2pServerHandler handler) {
    this.port = port;
    this.handler = handler;
  }

  public void start() {
    if (running) return;
    running = true;
    int workers = Math.max(4, Runtime.getRuntime().availableProcessors() * 2);
    bossGroup = new NioEventLoopGroup(1);
    workerGroup = new NioEventLoopGroup(workers);

    ServerBootstrap bootstrap = new ServerBootstrap()
        .group(bossGroup, workerGroup)
        .channel(NioServerSocketChannel.class)
        .option(ChannelOption.SO_BACKLOG, BACKLOG)
        .childOption(ChannelOption.TCP_NODELAY, true)
        .childOption(ChannelOption.SO_KEEPALIVE, true)
        .childHandler(new ChannelInitializer<SocketChannel>() {
          @Override
          protected void initChannel(SocketChannel ch) {
            ChannelPipeline pipeline = ch.pipeline();
            pipeline.addLast(new LengthFieldBasedFrameDecoder(MAX_FRAME_SIZE, 0, 4, 0, 4));
            pipeline.addLast(new LengthFieldPrepender(4));
            pipeline.addLast(new P2pConnection.NettyPacketHandler());
            handler.handle(ch);
          }
        });

    serverChannel = bootstrap.bind(port).syncUninterruptibly().channel();
    log.info("P2P server started: port={}, backlog={}, workers={}", port, BACKLOG, workers);
  }

  public void stop() {
    running = false;
    try {
      if (serverChannel != null) {
        serverChannel.close().syncUninterruptibly();
      }
    } catch (Exception ignored) {
      // ignore
    }
    if (bossGroup != null) {
      bossGroup.shutdownGracefully();
    }
    if (workerGroup != null) {
      workerGroup.shutdownGracefully();
    }
    log.info("P2P server stopped");
  }
}
