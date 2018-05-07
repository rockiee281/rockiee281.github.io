---
layout: post
category : golang
tags : [golang,glide,seaweedfs]
---
# 0x00
项目中使用到了[seaweedfs](https://github.com/chrislusf/seaweedfs)作为对象存储，据称是参照了facebook的[Haystrack](https://www.usenix.org/legacy/event/osdi10/tech/full_papers/Beaver.pdf)这篇论文。从实际使用中来看，确实稳定性还不错，IO吞吐也挺好。但是还是存在几个问题：

+ 日志输出的比较少，遇到问题的时候往往无从下手
+ 偶尔遇到读写失败，同样看不到啥日志
+ 曾经莫名丢过一次几百个文件，也找不到啥日志追踪前后做过什么操作

所以这回打算硬着头皮，把go和seaweedfs一起研究一下。

# 0x01
老规矩，下go的包、源码，下seaweedfs的源码。google了一下，目前好用的golang ide也就是vim或者netbrain出品的，所以还是用intellj+go plugin的组合。首先遇到的一个拦路虎就是GOPATH和GOROOT这俩兄弟，按照之前java的套路，GOROOT比较好理解，设置为go的根目录就OK了，很快也就解决了。但是GOPATH一开始理解错误了，其实如果从全局而言，相当于把我依赖的所有lib都放到这里来。golang这里感觉更暴力了，直接拉扯源码下来，不过我喜欢 :)   从我的理解，其实就是把自己的代码和依赖的lib一视同仁，都放到一个大的workspace下面去，听起来很有Google的风格。Golang自带`go get`命令去获取依赖，seaweedfs项目使用[glide](https://github.com/bumptech/glide)去做依赖管理，配置文件是yaml格式的，还是比较友好的。过程中为了解决google坑爹的依赖路径问题，使用glide的mirror来解决类似grpc等修改了托管的地址却没有改包名这种坑爹的行为，参考：[https://github.com/Chyroc/chyroc.github.io/issues/26]、[https://my.oschina.net/u/553243/blog/1475626]。 glide会默认把mirror文件放到`${home}/.glide`目录下，找了半天才发现……我的mirror文件如下，可以参考：
{% highlight yaml %}        
repos:
- original: https://golang.org/x/crypto
  repo: https://github.com/golang/crypto
  vcs: git
- original: https://golang.org/x/image
  repo: https://github.com/golang/image
  vcs: git
- original: https://golang.org/x/mobile
  repo: https://github.com/golang/mobile
  vcs: git
- original: https://golang.org/x/net
  repo: https://github.com/golang/net
  vcs: git
- original: https://golang.org/x/sys
  repo: https://github.com/golang/sys
  vcs: git
- original: https://golang.org/x/text
  repo: https://github.com/golang/text
  vcs: git
- original: https://golang.org/x/tools
  repo: https://github.com/golang/tools
  vcs: git
- original: https://google.golang.org/grpc
  repo: https://github.com/grpc/grpc-go
  vcs: git
- original: https://google.golang.org/genproto
  repo: https://github.com/google/go-genproto
  vcs: git
{% endhighlight %}        
编译、安装好glide，配置好mirror，然后就可以执行`glide install` 来安装依赖了。

这里必须继续吐槽一句，win下面用这些工具总有奇怪得问题……还好win10开启了ubuntu，进入ubuntu下面分分钟搞定了环境，`go build` 也成功了，总算可以开始研究代码了

# 0x02
不过在看代码之前，因为作者说是基于Facebook的论文开发的，那就先看看论文呗。Haystack是作为NFS的替代品而出现的，NFS的问题是存储的文件数量一旦太多，对metadata的lookup就会成为一个很大的瓶颈。在Haystack的论文中提到：`We carefully reduce this per photo metadata so that Haystack storage machines can perform all metadata lookups in main memory. This choice conserves disk operations for reading actual data and thus increases overall throughput` 首先是减少了metadata的量，因为变少了所以可以把metadata都放入内存，就能把磁盘的IO性能完全释放出来给文件读写使用。 使用的场景：`written once, read often, never modified, and rarely deleted.` 传统基于POSIX文件系统的弊端在于他们需要保存文件的权限信息，而这对于图像等文件存储服务而言意义不大，只是额外消耗。一次文件读取需要有三次操作：

+ 把文件名转换为inode
+ 从磁盘上读取inode
+ 根据inode去读取实际文件

所以Facebook基于以下几个目标重新设计了一个对象存储：高吞吐低延迟、支持故障转移、存储高效、结构简单。起初Facebook的方案是NAS，上面再架Photo Server。但是在文件数量多的时候还是会有问题，他们甚至尝试过直接cache文件的file handle，然后直接通过file handle走系统调用直接访问文件，但是提升效果并不明显。
Facebook设计的haystack分成了三个部分的服务：directory、cache和store。


Directory服务可以被视为主服务，提供了三个方面的功能：

+ 逻辑卷到物理卷的映射
+ 文件读写的负载均衡
+ 决定文件的读取是走cdn还是cache服务
+ 可以把卷标记为只读，可能是主动操作，也可能是磁盘空间已满等
考虑到directory要存储大量信息，这里他们采用了PHP+memcache的方案，缓存中的entry当节点下线的时候被摘除，上线的时候重新注册上来。


Cache服务比较简单好懂了，这里有两个缓存的原则：

+ 只有用户请求来的需要被缓存，CDN穿透过来的不用。因为一般CDN拦不住的，基本上本地也不会有cache
+ 只有从一个可写节点读到的文件才会被缓存。理由是一般photo都是在刚刚被上传之后的一段时间内访问频繁；另外，一台节点同一时间读、写只做一件事情的时候效率最高，把读请求cache住，可以让写操作有更高的吞吐。
