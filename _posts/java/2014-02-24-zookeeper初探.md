---
layout: post
category : java
tags : [分布式]
---
再挖个坑，准备仔细看一下zookeeper。zookeeper作为很多分布式服务中用于管理的关键一环，意义其实很重要，这里先仔细学习下吧。


##什么是zookeeper
zookeeper贵为[apache的顶级项目](http://zookeeper.apache.org/),在它的介绍中说`ZooKeeper is a centralized service for maintaining configuration information, naming, providing distributed synchronization, and providing group services`，看见zk可以用来做配置管理、名称服务管理、分布式同步和提供集群服务。


##zookeeper的安装配置
有个小插曲，在编译zk的C代码的时候，autoreconf一直过不去，提示` error: possibly undefined macro: AM_PATH_CPPUNIT `,后来在zk的[邮件组](http://zookeeper-user.578899.n2.nabble.com/AM-PATH-CPPUNIT-not-found-in-library-when-doing-autoreconf-and-or-configure-td3628553.html)里查到了，原来是少了cppunit-devel包，安装上之后就能编译了。用java来写code这么费劲，还是先尝试一下python来操作zk吧，正好在网上找到了一个[很不错的demo](http://www.zlovezl.cn/articles/40/)，他这里使用的是zkpython这个python的zookeeper包，使用的是zk的C版客户端。


##zookeeper的使用
还是在上面提到的那个样例中，作者介绍了一种很不错的zk使用办法。
