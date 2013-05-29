---
layout: post
category : android
tagline: "android MQTT 学习"
tags : [android, 笔记,MQTT]
---
MQTT是一种M2M协议，用于节点之间通信，网上不少人推荐它，认为其性能和开销方面要强于XMPP，准备搞个demo试一下。

看了它的官方网站(http://mqtt.org/) ,不爽的是主流的服务器端实现是IBM的闭源服务器，对于喜欢开源有洁癖的我来说，很是不爽，而且一想到要和IBM的一堆服务绑到一起，一阵恶心又要涌上心头。不过MQTT也有开源的实现[Mosquitto][1](http://mosquitto.org/),据说性能上可能暂时不及IBM。

准备先从了解了解MQTT的协议，看看它到底在哪些方面要优于XMPP。如果真的可取，可以考虑以后用到我们的实时消息推送里头去。

[这里][2]有个基于IBM技术的MQTT android端的介绍,虽然年代稍远，不过写的还是很详尽的，最重要的是它是中文。

[1]: http://mosquitto.org/
[2]: https://www.ibm.com/developerworks/cn/websphere/library/techarticles/1109_wangb_mqandroid/1109_wangb_mqandroid.html
