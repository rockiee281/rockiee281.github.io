---
layout: post
category : java
tags : [java, grpc]
---

最近线上遇到了一个Direct Memory溢出的问题，这里记录一下排查的过程，希望能给其他遇到类似问题的同学提供解决问题的思路。

首先是线上收到告警，出现了大量服务端错误，而且势头猛烈，第一反应首先肯定是去sls上根据错误码线搜索日志。出现问题的应用本身提供的是gateway网关接入服务，承担鉴权、路由、限流、请求分发的职责，服务端错误集中在两类错误上：gateway服务自身错误和访问下游服务失败。在sls上对日志根据服务端IP聚合，发现集中在少量2-3台服务器上，初步分析问题主要集中在个别服务不可用。但是从监控上看，错误量虽然不少，但是同时绝大部分请求还是能正常响应的，这个时候内心还是稍微轻松了一些，至少没有血崩……这里需要给sls提点建议，想kibana是可以根据索引把java的异常栈合并到一个row去展示的，但是sls貌似不支持，所以我们只能登到机器上去看具体错误信息了。这一看心头一紧，`io.netty.util.internal.OutOfDirectMemoryError: failed to allocate 16777216 byte(s) of direct memory (used: 1056964910, max: 1073741824)`，发生了内存溢出，而且是我最头疼的堆外direct memory内存溢出。我们的应用接受业务端的请求，然后经过处理后转发到后端多组提供原子服务的server上去，这里为了提供网络IO性能，使用的组建如netty、async-http、grpc、lettuce底层都是基于netty实现的异步网络IO，换言之会有多个场景都有可能导致内存泄漏。jvm配置了堆外内存的大小为1G，超过1G的时候netty在尝试分配内存失败就会抛出这个异常。

目前大部分的jvm分析工具都不太好去分析堆外内存的使用情况，而且是素有内存泄漏恶名的netty4。 从网上扒到一个帖子，提供了一个分析思路。在linux下通过`pmap -x <pid>`分析jvm进程在内存中的使用情况，找出direct memory buffer使用的内存块，然后通过gdb把整个地址区间的二进制数据dump到磁盘，转换为hex数据去直接看内容。因为大部分的String对象都是英文字符，所以有办法能从这里直接看出问题所在。不幸的是这种方式没有太多收获。那么问题又回到分析堆外内存是如果打满了。

将有问题的实例offline后抓取了dump文件，通过jxray(http://www.jxray.com/ )能分析出off-heap数据确实已经达到了1G，netty在堆外分配了多个区域的16MB左右的数块，已经把内存吃光了。但是dump文件中应该是不包含Direct Memory中的数据内容，所以也无从得知究竟是谁持有这些数据，导致了无法释放出来。

有同学推测一些长生命周期的对象因为存活时间较长，而进入了老年代，但是因为我们的应用大量依赖的是堆外内存，老年代使用量很少，长时间没有full gc的话，导致堆外内存也无法释放。为了验证这种推测，通过jmap -dump:live 的方式强制jvm进行了一次full gc，对比了操作前后的dump，内存的占用并没有下降，failed…… 不过这里也推测因为netty已经申请了这些数据块进行了预分配，所以即使其中的数据已经被回收，是否内存中的数据块还是会被保留？这块还要待后面取分析netty的内存管理机制。

这个时候把问题的dump文件分享到组内钉钉群里面，激起各个老中医们对这个问题的兴趣。躜总分享了这篇文章（ https://www.jianshu.com/p/4e96beb37935 ），这里面提到的一个思路很棒，通过反射获取到netty内部的计数器，拿到netty自己计算的堆外内存使用量，这样我们就能通过日志准确监控起来。另一方面，作者通过测试并观察堆外内存增长的方式，逐步定位到是连接关闭异常造成的堆外内存泄漏，这也给了解决问题的灵感，事实证明最后出现问题还真就是连接未正常关闭造成的。加上监控指标之后，在离线环境反复使用各种情况去压测，观察堆外内存并没有发现线上出现的缓慢增长的现象，再一次陷入困境。

这个时候组里凯哥在观察堆内内存使用情况时，发现了端倪。服务端提供的是WebSocket服务，在处理客户端上行请求时，针对每个连接会有一个netty channel，而对应的服务端会有一个WsFrameHandler处理，当请求结束时channel 会关闭，FrameHandler也会被remove掉结束它的生命周期，然后被gc掉和大家say goodbye，但是分析dump中FrameHandler对象的数量远大于这台实例当时处理的并发数。于是我们推测因为某种异常原因，handler对象被其他引用一直持有未释放，结果导致了长期驻留内存。

这个时候轮到jdk自带的visual vm出场了。谁说只有付费的jprofiler、jxray之流才是最好的呢，visual vm根红苗正功能强大关键是还随jdk自带，简直不要更好用。使用visualvm打开了heap文件，根据类名可以查找到所有的FrameHandler实例，这里就能体现visualvm强大之处，可以方便的对任意类的实例做查询，而且在图形界面上可以轻松看到实例的内部属性值。这里正好遇到之前一个设计上的东西帮助了我，FrameHandler里为了记录客户请求到服务端响应的latency，保存了一个客户端最后请求包到达的时间戳；为了方便记录日志，在实例属性里面保存了context对象，其中包含了traceid。通过抽样了几个FrameHandler对象，发现大量的时间戳都是数天前的请求，通过traceid在sls上搜索，发现请求都是异常结束的，例如IDLE TIMEOUT或者后端服务其他异常导致的连接断开。visual vm还支持类似sql的oql语法，能通过类似`select {taskid:x.nlsCtx.taskId, ts:x.lastRequestTs, namespace:x.nlsCtx.namespace} from com.xxx.xxx.x.xxx.WsFrameHandler x where x.lastRequestTs < 1551715200000` 的语句把所有我们怀疑有问题的实例某些field输出，方便我们复杂出来去日志系统中做进一步的排查。

查到这里基本上心里就有底了，通过在offline环境模拟这些错误，我们很快在gateway上重现了线上内存溢出的场景。进一步的分析代码就发现了问题产生的原因：我们和后端通信的时候使用的是Grpc双向流模式，当后端服务检测到异常时，会调用stream.onError(Throwable throwable) 来通知调用方。之前开发时认为此处grpc会自己处理连接关闭，我们只是把异常做包装后抛给业务层处理，没想到grpc并没有把这个sream从底层移除，netty的streamMap还持有这些对象，然后通过层层引用导致了整个链条上的对象都存在引用而无法被jvm回收。修改的方法就是在调用端onError的处理时，调用一下StreamObserver.onCompleted() , 通知grpc请求结束连接需要关闭。修改之后在离线环境反复测试，内存不再有溢出的现象。

简单总结一下：

1. 上来被堆外内存溢出的表现带偏了，一直在考虑是否触发了netty异常或者其他堆外内存未释放的问题，而忽略了因为堆内对象持有引用也可能导致堆外无法释放
2. 出现问题第一时间保存现场很重要，最后问题排查非常依赖第一次抓下来的heap dump
3. visualvm是个好东西，提供的对象分析、属性分析和方便的oql查询功能组合起来非常好用，相对之下之前用jhat去分析简直就是小米加步枪了
4. jxray设计的不错，不纠结于细节，直接帮你给出可能存在问题的点，特别是对于direct memory 给出分析建议，相对于eclipse memory analyzer，对新人更友好，对老手也能节省很多成本，可惜是付费软件 :) 
5. jvm的参数设置还是可以继续优化的，因为主要是网络IO使用的堆外内存居多，young区的对象基本在一次young gc内都回收掉了，线上一周多都没有一次fullgc，所以可以考虑放大young区调小old区。另外，也在考虑把垃圾回收器从CMS更换为G1，毕竟jdk8 的G1已经很稳定可靠了。
6. 事件驱动、异步方式的设计架构给内存泄漏问题的排查带来新的挑战。以往大量采用同步方式设计的应用，发生内存泄漏的时候很多时候是线程池设计、使用不当造成的，使用jstack简单分析下线程栈就能快速定位，但是使用异步方式编写的代码，目前看起来只能通过分析dump中对象的引用关系和实例数量来做了，无疑增加了分析的难度
7. Java 官方提供了Native Memory Tracking(NMT)工具，可以输出native内存的使用情况，但是需要在jvm启动参数里面增加一个`-XX:NativeMemoryTracking=summary`或者 `-XX:NativeMemoryTracking=detail`，没有评估过对线上系统性能如何，推荐还是在预发用比较好。启动以后可以通过`jcmd <pid> VM.native_memory detail`命令看到native memory的详细使用情况了。
8. 增加netty自己的Direct Memory buffer区使用量监控，及时发现问题。参照网上的文章，通过反射拿到Netty自己内部的引用计数器，然后输出Metrics的方式来准确获取Direct Memory的消耗情况

```java
Field field = ReflectionUtils.findField(PlatformDependent.class, "DIRECT_MEMORY_COUNTER");
field.setAccessible(true);

try {
    directMemory = (AtomicLong)field.get(PlatformDependent.class);
}catch (Exception e) {
}
```

参考文章：

1. [使用google perf工具来排查堆外内存占用](https://qsli.github.io/2017/12/02/google-perf-tools/#%E4%BD%BF%E7%94%A8pmap%E6%9F%A5%E7%9C%8B%E8%BF%9B%E7%A8%8B%E7%9A%84%E5%86%85%E5%AD%98%E6%98%A0%E5%B0%84)
2. [oracle官方NMT介绍](https://docs.oracle.com/javase/8/docs/technotes/guides/vm/nmt-8.html)
3. [Java堆外内存排查小结](https://mp.weixin.qq.com/s?__biz=MzA4MTc4NTUxNQ==&mid=2650518452&idx=1&sn=c196bba265f888ed086b7059ca5d3fd2&chksm=8780b470b0f73d66c79b7df96435d48caa8c49a9a6b696e543c0df24e3356202ccde69f2f671&mpshare=1&scene=1&srcid=0831YG589PwShEgNLJ8CKQOp#rd)