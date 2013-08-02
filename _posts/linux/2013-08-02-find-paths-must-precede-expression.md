---
layout: post
category : shell 
tags : [linux,shell]
---
今天执行了一个find命令`find . -name *php`，意外的抛出了一个错误`find: paths must precede expression: `。思来想去也没明白为啥，网上查了下，需要给name后面的表达式加上引号，试了一下果然就搞定了。不过知其然也得知其所以然，继续搜了一下，果然在linuxorg上找到了解释：
<pre>
to explain the wild card issue, the solution is \*

The reason why, and why its a spontaneous error. lets just say I run:
---------------------
]# find / -name fcgi*
---------------------

That works fine in _most_ directories... however, if I'm in the FCGI directory:
---------------------
]# ls
fcgi.c fcgi.h fcgi.o

]# find / -name fcgi*
find: paths mus precede expression
--------------------

its because the shell in this instance replaces
find / -name fcgi*
with:
find / -name fcgi.c fcgi.h fcgi.o

but that does not happen if there are no files matching the pattern in the current directory
</pre>
看来，引起问题的根源在于执行find命令的目录下，有多个可以匹配到的文件，shell在解释命令的时候，自动的做了替换。试了里面的建议，用`\`做转义之后命令就能执行了。而且如果对应的模式在当前目录仅有一个匹配文件的时候，也不会报错，但是就相当于只查找名称跟这个文件名一致的其他文件了，这就背离了我们查询的本意，也算是个小小的陷阱。

![示例](/images/20130802114149.jpg)

