---
layout: post
category : opensource project
tags : [netty,source code]
---

# netty的流控

netty作为一个异步的网络处理框架，如果作为请求的代理服务使用或者说作为service mesh的网关，需要能够对自身的可用性做保障。这里就类似大家说的Spark、HBase等框架中使用的反压机制，当下游的接受速度跟不上的时候，避免大量的写操作堆积临时对象在内存中，导致heap 或者 direct memory 溢出。有人在github上给netty提过[issue](https://github.com/netty/netty/issues/3511)，网上也有人写了文章来[分析](http://www.sohu.com/a/142663931_684743)，大概意思是netty的写缓冲区是个无界队列，如果自己不做控制的话，一旦下游的读取速度跟不上，就会因为这个队列把内存打爆。

netty提供了`WRITE_BUFFER_HIGH_WATER_MARK`和`WRITE_BUFFER_LOW_WATER_MARK`来设置水位，同时通过`Channel.isWritable`和`channelWritabilityChanged`来感知netty下游水位的变化。网上有别人提供的一段[代码示例](https://lishoubo.github.io/2017/08/31/%E6%B5%81%E5%A4%84%E7%90%86%E4%B8%AD%E5%8F%8D%E5%8E%8B%E9%97%AE%E9%A2%98/),代码如下：

```java
//配置水位线
serverBootstrap.group(selectorBossGroup, selectorWorkerGroup)
    .channel(NioServerSocketChannel.class)
    .option(ChannelOption.SO_BACKLOG, 128)
    .childOption(ChannelOption.WRITE_BUFFER_HIGH_WATER_MARK, 1024 * 64)
    .childOption(ChannelOption.WRITE_BUFFER_LOW_WATER_MARK, 1024 * 32)

//监听水位线变化，关闭自动读
public class ConnectManageHandler extends ChannelDuplexHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConnectManageHandler.class);

    @Override
    public void channelWritabilityChanged(ChannelHandlerContext ctx) throws Exception {
        if (ctx.channel().isWritable()) {
            LOGGER.warn("[remoting] channel isWritable is true. remote:{}", ctx.channel().remoteAddress());
            ctx.channel().config().setAutoRead(true);
        }
        super.channelWritabilityChanged(ctx);
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
        if (!ctx.channel().isWritable()) {
            LOGGER.warn("[remoting] channel isWritable is false. remote:{}", ctx.channel().remoteAddress());
            ctx.channel().config().setAutoRead(false);
        }
        super.channelReadComplete(ctx);
    }
}

```

这个方法很好，但是实际业务中会更复杂一些。服务端通过netty接收客户端请求，拿到的请求数据会通过grpc流式的转发给下游的服务，而一旦下游服务出问题，通过grpc的`asyncBidiStreamingCall`还是会不断的写入数据，最后导致了网关服务爆掉。所以这里如果只是设置netty的水位，只能是在向请求方写Response跟不上的时候有用，对与代理网关这种场景，需要在通过 Gprc / HttpClient 等方式访问下游服务变慢时通知到netty server，关闭自动read。