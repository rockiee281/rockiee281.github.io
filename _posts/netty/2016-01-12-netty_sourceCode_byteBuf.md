---
layout: post
category : opensource project
tags : [netty,source code,]
---
开篇第一节关注的是netty对ByteBuf的封装。对于封装的意义，我理解分几个方面：

1. 通过引入Pool来管理内存分配，做到对象池服用
2. 简化API操作
3. 加入了UnSafe、DirectBuffer这些类型的buffer对象作为可选项。
![netty-buffer](/img/in-post/post-netty-buffer.jpg)