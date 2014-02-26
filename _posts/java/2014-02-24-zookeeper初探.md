---
layout: post
category : java
tags : [分布式]
---
再挖个坑，准备仔细看一下zookeeper。zookeeper作为很多分布式服务中用于管理的关键一环，意义其实很重要，这里先仔细学习下吧。


##什么是zookeeper
zookeeper贵为[apache的顶级项目](http://zookeeper.apache.org/),在它的介绍中说
```
ZooKeeper is a centralized service for maintaining configuration information, naming, providing distributed synchronization, and providing group services
```
看见zk可以用来做配置管理、名称服务管理、分布式同步和提供集群服务。


##zookeeper的安装配置
有个小插曲，在编译zk的C代码的时候，autoreconf一直过不去，提示` error: possibly undefined macro: AM_PATH_CPPUNIT `,后来在zk的[邮件组](http://zookeeper-user.578899.n2.nabble.com/AM-PATH-CPPUNIT-not-found-in-library-when-doing-autoreconf-and-or-configure-td3628553.html)里查到了，原来是少了cppunit-devel包，安装上之后就能编译了。用java来写code这么费劲，还是先尝试一下python来操作zk吧，正好在网上找到了一个[很不错的demo](http://www.zlovezl.cn/articles/40/)，他这里使用的是zkpython这个python的zookeeper包，使用的是zk的C版客户端。

尝试在单机部署多个zk的节点，那就需要为每个节点做一个配置文件，里面指定不同datadir和client port，例如配置文件zoo.1.cfg：
```
dataDir=/home/rockiee281/zk/server1
# the port at which the clients will connect
clientPort=2181
server.1=localhost:2887:2889
server.2=localhost:3888:3889
server.3=localhost:4888:4889
```
上面是其中一个server的配置，clientPort开在了2181，这个是客户端用来连接zk server的；server配置中，第一个端口用于和leader通讯，第二个端口用于leader的选举。每个dataDir中要创建对应的myid文件，文件内容就是server的id。然后通过命令`bin/zkServer.sh  start conf/zoo.1.cfg`来分别启动3个server，就大功告成啦。

##zookeeper的使用
还是在上面提到的那个样例中，作者介绍了一种很不错的zk使用办法。在创建zk的node时，加上`zookeeper.EPHEMERAL`和`zookeeper.SEQUENCE`。EPHEMERAL的节点是临时节点，当创建节点的客户端断掉链接之后，zk将删除这个节点；而加上SEQUENCE之后，zk会自动为创建的节点加上sequence number，方便为节点进行排序。这样，就可以使用一种巧妙的办法来做master-slaver状态维护。多个客户端在zk上创建节点，然后把sequence最小的那个节点作为master，同时所有的master都设置watcher监控；一旦master节点down掉，那么剩下节点中sequence最小的那个将继承为master，其他的slave节点也都会被通知到。这样就可以得到一个动态稳定的master-slaver结构。
