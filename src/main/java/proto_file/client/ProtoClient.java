package proto_file.client;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

public class ProtoClient {
    private static ProtoClient ourInstance = new ProtoClient();

    private final List<ChannelHandler> channelHandlers = new ArrayList<>();

    public static ProtoClient getInstance() {
        return ourInstance;
    }

    private ProtoClient() {
    }

    private Channel currentChannel;

    public Channel getCurrentChannel() {
        return currentChannel;
    }

    public void addHandler(ClientHandler clientHandler) {
        channelHandlers.add(clientHandler);
    }

    public void start(CountDownLatch countDownLatch) {
        EventLoopGroup group = new NioEventLoopGroup();
        try {
            Bootstrap clientBootstrap = new Bootstrap();
            clientBootstrap.group(group)
                    .channel(NioSocketChannel.class)
                    .remoteAddress(new InetSocketAddress("localhost", 8189))
                    .handler(new ChannelInitializer<SocketChannel>() {
                        protected void initChannel(SocketChannel socketChannel) throws Exception {
                            channelHandlers.forEach(socketChannel.pipeline()::addLast);
                            currentChannel = socketChannel;
                        }
                    });
            ChannelFuture channelFuture = clientBootstrap.connect().sync();
            countDownLatch.countDown();
            channelFuture.channel().closeFuture().sync();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                group.shutdownGracefully().sync();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public void stop() {
        currentChannel.close();
    }
}
