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
```yaml
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
```
编译、安装好glide，配置好mirror，然后就可以执行`glide install` 来安装依赖了。
