---
layout: post
category : nginx
tags : [nginx,php-fpm]
---
今天检查我们nginx的错误日志，发现大量的 upstream timed out (110: Connection timed out) while reading response header from upstream ,果断google一下，发现网上不少建议是在使用php-fpm的时候，调大`fastcgi_read_timeout`这个参数，实际上应该是延迟超时时间吧。感觉这个不是最好的解决方案，后来看到另外一篇[文章](http://hi.baidu.com/jjxiaoyan/item/d7a6f43916fef6be134b14f0),里面介绍他的解决办法是增大fastcig_buffers这个参数，检查了一下我们的nginx配置文件，果然是默认参数没有优化过，按照文章中的建议，设置缓存区为8*128k，果然不再出现upstream time out的错误了，搞定。
