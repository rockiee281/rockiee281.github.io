---
layout: post
category : opensource project
tags : [netty,source code,]
---
## 前序
开篇第一节关注的是netty对ByteBuf的封装。对于封装的意义，我理解分几个方面：

1. 通过引入Pool来管理内存分配，做到对象池服用
2. 简化API操作
3. 加入了UnSafe、DirectBuffer这些类型的buffer对象作为可选项。
![netty-buffer](/img/in-post/post-netty-buffer.jpg)
(buffer包涉及的类太多，请使用查看原图)

大体分为这么几块：

1. ByteBuf的派生类
2. ByteBufAllocator的派生类
3. 辅助的功能类

## ByteBuf派生类
ByteBuf直接派生出来的类有这么几个:`WrappedByteBuf`,`AbstractByteBuf`,`SwappedByteBuf`,`EmptyByteBuf`

### WrappedByteBuf
WrapperedByteBuf完全代理ByteBuf的方法，它有三个继承类，分别是：`AdvancedLeakAwareByteBuf` `SimpleLeakAwareByteBuf` `UnreleasableByteBuf`.

`AdvancedLeakAwareByteBuf`和`SimpleLeakAwareByteBuf`被ByteBufAllocator调用，用于封装LeakAwareBuf。他们在内部持有了一个`ResourceLeak`用来记录操作
追踪是否有对象泄漏。

`UnreleasableByteBuf`则会忽略掉ByteBuf的release和retain操作，适用于定义一些常量，比如：
```java
    private static final ByteBuf CRLF_BUF = unreleasableBuffer(directBuffer(CRLF.length).writeBytes(CRLF))
```

## ByteBufAllocator派生类

## 辅助功能类
