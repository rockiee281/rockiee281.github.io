---
layout: post
category : java
tags : [java, thrift]
---
这两天有点时间，琢磨下一直一知半解的thrift。thrift的[官网](http://thrift.apache.org/)中对它的介绍是：

```
The Apache Thrift software framework, for scalable cross-language services development, combines a software stack with a code generation engine to build services that work efficiently and seamlessly between C++, Java, Python, PHP, Ruby, Erlang, Perl, Haskell, C#, Cocoa, JavaScript, Node.js, Smalltalk, OCaml and Delphi and other languages
```

首先就是安装，按照官网的指导，下载了thrift的安装包，编译安装。thrift有官方的一个[白皮书](http://thrift.apache.org/static/files/thrift-20070401.pdf),看这格式很像论文的样子……安装就遇到问题了：
1. `libs/thrifttest_constants.o no such file or directory` 首先是用官网的tar包来make，遇到这个错误可耻的失败了。去了github clone一个回来，就ok了。
2. `'SSLProtocol' is not a class or namespace` ,google 半天也不知道所以然。git checkout到0.9.1版，搞定

安装完成之后，按照官方的提供的例子创建了一个thrift配置文件：

```
      struct UserProfile {
        1: i32 uid,
        2: string name,
        3: string blurb
      }
      service UserStorage {
        void store(1: UserProfile user),
        UserProfile retrieve(1: i32 uid)
      }
```

其参数是个结构体，然后service中定义了2个接口，一个用来储存数据，一个用来根据UID查询。调用`thrift --gen py my.thrift`，就可以生成python的代码，用于编写python的客户端和服务器端。生成文件的结构如下：

```
gen-py/
├── __init__.py
└── my
    ├── constants.py
    ├── __init__.py
    ├── ttypes.py
    ├── UserStorage.py
    └── UserStorage-remote
```
其中ttypes.py中包含了自动生成的类型代码，UserStorage中封装了相关的class和基本架构。下面附上python服务器端的代码：

```python
#!/usr/bin/env python
import sys
sys.path.append('./gen-py')
from my import UserStorage
from my.ttypes import *
from thrift.transport import TSocket
from thrift.transport import TTransport
from thrift.protocol import TBinaryProtocol
from thrift.server import TServer
import socket


class myhandler:
    def retrieve(self, uid):
        print uid
        user = UserProfile()
        user.uid = uid
        user.name = 'lx281'
        user.blurb = 'xxxx'
        return user

handler = myhandler()
processor = UserStorage.Processor(handler)
transport = TSocket.TServerSocket('localhost', 9090)
tfactory = TTransport.TBufferedTransportFactory()
pfactory = TBinaryProtocol.TBinaryProtocolFactory()

server = TServer.TSimpleServer(processor, transport, tfactory, pfactory)

# You could do one of these for a multithreaded server
#server = TServer.TThreadedServer(processor, transport, tfactory, pfactory)
#server = TServer.TThreadPoolServer(processor, transport, tfactory, pfactory)

print 'starting python server ....'
server.serve()
print 'done!'
```

把它放到脚本中启动，即可作为服务端提供服务啦。UserStorage-remote可以作为一个简单的客户端脚本，作为测试用。比如：

```
[rockiee@testserver thrift]$ ./UserStorage-remote retrieve 123
UserProfile(uid=123, blurb='xxxx', name='lx281')
```

周末正好去听了百度技术沙龙的一个分享，是关于百度自己搭建的一个sofa系统，跟thrift还真是挺类似的。他们也是做了一个异构系统的服务发布平台，通过类似thrift一样的系统，去远程调用服务。不过还是不够我心目中的完美，我理想中的服务化平台，应该有这样的功能：

1. 客户端不用关心提供服务方的具体信息，比如服务器ip、端口这些，只需要向配置管理中心提出申请，由管理中心自动指定。这样客户端也就不用自己去指定需要多少个服务端来提供服务，都可以由配置管理中心来动态的配置，这样可以根据负载灵活的增删提供服务的节点。
2. Qos监控。配置管理中心的存在，一方面是配置管理、服务分配；另一方面就可以对服务质量进行监控，对于那些响应时间超长或者占用资源过多的服务端、客户端发出告警。
3. 服务注册自动化。服务端启动之后自动的向配置管理中心注册自己的信息，服务端要实现基本的监控接口并提供给配置管理中心，由配置管理中心负责定时监控。
4. 服务发布平台。把注册上来的服务发布出去，并实现不同级别的权限管理。
5. pipeline优化。如果是顺序调用多个服务，争取能够做到优化，尽量调用部署在同一个物理节点的N个服务？或者说，如果是同一个节点提供的服务，就避免通过网络传递数据了，通过IPC之类的方式 ：）
把这些都实现了，应该就会是一个比较理想的服务发布平台了吧，希望以后有机会做一个，嘿嘿。

上面都是what和how，之后琢磨的就是why了。在百度的案例中，他提到的场景是：想做一个搜索服务，分词是python的，index是C++的，要想把这两者糅合起来实现服务。通过类似thrift这样的平台，就能够轻易的利用既有的code和功能，组合实现想要的新功能了，这个我想就是thrift最大的价值吧。特别是大的组织中，各种类型的code肯定都有，每种语言也都有自己最擅长的地方，发扬各自的长处物尽其用，这就是thrift出场的时候了。
