---
layout: post
category : java
tags : [java, GC]
---
Oracle在收购Bea之后拿到之前他家灰常牛逼的JVM技术JRockit，其中就包含Flight Recorder。之前一直没有机会使用，这次线上spark集群使用的jdk就是java8的，终于有机会一试身手了。

## 问题
首先说下起因，有同学在集群上跑一个spark job，剩下最后一个task迟迟不肯结束。在spark ui上抓thread dump，发现线程卡在fastJson的toString。好嘛，为了输出日志把对象都序列化了，浪费资源,但是按理说也不至于卡到一直跑不完啊。

## 分析
首先出动jstat，看一下GC的情况。一看吓了一大跳，一共46G的堆空间，基本上每秒能new出1G多空间，没几下OLD区就满了，就要卡着99%开始GC。辛辛苦苦GC了10几秒把内存释放到了30%多，没几下又满了，又开始10几秒的GC…… 就这德性任务能跑完就见鬼了。这时候就得看看程序到底生成了什么对象，这么占空间，试用了下面几个工具

### jvmtop
google上扒出来的一个工具，别人自己写的一个util，可以方便的看出内存增长、cpu占用、gc情况。很好用，地址是:[https://github.com/patric-r/jvmtop]。
```
 JvmTop 0.8.0 alpha   amd64  8 cpus, Linux 2.6.32-27, load avg 0.12
 https://github.com/patric-r/jvmtop

  PID MAIN-CLASS      HPCUR HPMAX NHCUR NHMAX    CPU     GC    VM USERNAME   #T DL
 3370 rapperSimpleApp  165m  455m  109m  176m  0.12%  0.00% S6U37 web        21
11272 ver.resin.Resin [ERROR: Could not attach to VM]
27338 WatchdogManager   11m   28m   23m  130m  0.00%  0.00% S6U37 web        31
19187 m.jvmtop.JvmTop   20m 3544m   13m  130m  0.93%  0.47% S6U37 web        20
16733 artup.Bootstrap  159m  455m  166m  304m  0.12%  0.00% S6U37 web        46

```
但是它解决不了我们的问题，我们需要的是去了解堆内部对象的创建及使用，pass！

### jmap & jhat
回到传统的解决办法，使用jmap抓去heap，然后用jhat去分析dump文件。一般平时几个G的内存，都可以jmap抓完dump拿回来用jprofiler或者IBM贡献的Memory Analyzer去分析，但是这40多个G的内存，只有Memory Analyzer能分析了，jprofiler受限于分析模式，对于大的dump无能为了。但是40多G的话我自己电脑的磁盘空间都不够啊！！！！所以老老实实的用jhat在服务器上直接分析吧。这时候还发现另外一个问题，虽然用jstat去看内存占用了几十G，抓下来的dump文件却只有6.3G，也就是说大量的都是new出来的对象，抓下来的dump中都已经被gc掉了，更无迹可寻，这下头疼了

h3. jfr
这时候就想起了java flight recorder(jfr)。之前也用过他去分析过jvm进程，但是根据官方的文档，命令行工具的选项一共就那么几个，没看到怎么去获取内存的分配情况。官方文档中示例的命令如下`sudo -u hadoop jcmd 75280 JFR.start duration=360s filename=/tmp/rockiee.jfr` 。 这个命令会启动jfr，在360s内做抽样然后把jfr文件保存在tmp目录下。但是这样抓下来只能看到很少的信息，jdk工具包里面的java Mission Control里面倒是可以启动对内存分配的分析，但是官方文档里面死活没找到命令行怎么办啊…… 还好，在全球最大的同性交友网站堆栈溢出上找到了一个很棒的帖子:[http://stackoverflow.com/questions/19056826/java-mission-control-heap-profile] 按照这里面的介绍，从Mission Control里面export出来配置文件，然后用` sudo -u hadoop jcmd 75280 JFR.start duration=360s settings=/tmp/my.jfc  filename=/tmp/rockiee.jfr` 这个命令重新抓取jfr文件。这回在Java Mission Control里面打开jfr文件，就能看到漂亮的各种视图了。

![thread](/img/in-post/thread.jpg)
![gc](/img/in-post/gc.jpg)
![heapOfThread](/img/in-post/heapOfThread.jpg)

有这三张图，基本就不用说啥了吧，罪魁祸首一目了然！jfr确实是神器，感觉有了他，可以不要用考虑jprofiler了，哈哈。官方逼死同人啊……

