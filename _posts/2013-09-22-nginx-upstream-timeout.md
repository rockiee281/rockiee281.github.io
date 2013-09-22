---
layout: post
category : nginx
tags : [nginx,php-fpm]
---
今天检查我们nginx的错误日志，发现大量的 upstream timed out (110: Connection timed out) while reading response header from upstream  ,果断google一下，发现网上不少建议是在使用php-fpm的时候，调大`fastcgi_read_timeout`这个参数，实际上应该是延迟超时时间吧。



