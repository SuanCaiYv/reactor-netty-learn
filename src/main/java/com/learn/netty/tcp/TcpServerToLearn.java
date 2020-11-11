package com.learn.netty.tcp;

import io.netty.channel.ChannelOption;
import reactor.core.publisher.Mono;
import reactor.netty.DisposableServer;
import reactor.netty.resources.LoopResources;
import reactor.netty.tcp.TcpServer;

/**
 * @author Mr.M
 */
public class TcpServerToLearn {

    public static void main(String[] args) {
        DisposableServer disposableServer = TcpServer.create()
                .host("127.0.0.1")
                .port(8190)
                .handle((nettyInbound, nettyOutbound) -> {
                    nettyInbound
                            // 接收数据
                            .receive()
                            .then();
                    return nettyOutbound
                            // 写入数据
                            .sendString(Mono.just("我是穷狗"));
                })
                // 一些回调方法
                .doOnBind(c -> {
                    System.out.println("绑定完成");
                })
                .doOnBound(c -> {
                    System.out.println("服务器启动");
                })
                .doOnConnection(c -> {
                    System.out.println("已连接");
                })
                .bindNow();
        disposableServer.onDispose()
                .block();
    }

    public static void mainConfiguration(String[] args) {
        // 创建一个EventLoopGroup，其中Boos线程为2个，工作线程为16个，且随着JVM的关闭而关闭
        LoopResources loopResources = LoopResources.create("我是LoopResources名", 2, 16, true);
        DisposableServer disposableServer = TcpServer.create()
                .host("127.0.0.1")
                .port(8190)
                // 一般来说，TcpServer有一些默认的配置，但是你也可以通过下面的方法，来更改配置，使之更符合你。
                .option(ChannelOption.AUTO_CLOSE, Boolean.TRUE)
                // 还有很多配置，请自行选择
                // 具体可见：https://netty.io/4.1/api/io/netty/channel/ChannelOption.html
                // 或：https://docs.oracle.com/javase/8/docs/technotes/guides/net/socketOpt.html
                // 开启两点之间流量检查日志
                .wiretap(true)
                // 设置线程模型
                .runOn(loopResources)
                // TcpServer还支持SSL和TLS设置，具体见官方文档
                // 同时开可以和性能指标分析模块对接，具体见文档
                .bindNow();
        disposableServer.onDispose()
                .block();
    }
}
