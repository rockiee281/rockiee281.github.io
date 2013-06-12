---
layout: post
category : android
tagline: "android app升级"
tags : [android, trouble shoot]
---
上周遇到了一个非常蛋疼的bug，我们的应用通过下载更新升级的时候，显示升级成功，但是重启手机之后检查应用的版本发现还是原来的版本。分析了各种各样的问题，比如因为我们的应用是系统app，安装在/system/app/目录下，担心是不是因为这个导致了更新失败。在检查了/data/app/目录之后，发现没次更新之后这里确实有新的apk包安装，但是重启之后文件就消失了。

又经过了一段时间蛋疼的排查，终于有个兄弟提醒我们，是不是只改了version name没有改version source？检查了一下，果然，我们的version code一直是1，每次升级都是改的version name…… 崩溃啊。因为之前的更新都是跟着OTA升级，相当于覆盖更新，所以这个问题一直没有暴露。这次我们自主更新，就发现了这个问题。初学者果然容易倒在这种小坑上。
