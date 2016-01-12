---
layout: post
category : opensource project
tags : [netty,source code]
---
今天开挖新坑，记录netty的源码学习之路。

对netty感兴趣主要是在这么几个方向上：

1. 内存管理，主要是在DirectBuf的使用上
2. EventLoop如何实现的，另外看看它如何让CPU利用率更高效
3. FrameWork层如何设计，更好的扩展以及封装底层API

Netty 4.x的包结构大概是：

    io.netty
       |-bootstrap
       |-buffer
       |-channel
       |-handler
       |-util
后面会按照顺序去挨个分析每个包
