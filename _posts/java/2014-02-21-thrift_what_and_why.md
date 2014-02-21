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
```
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
