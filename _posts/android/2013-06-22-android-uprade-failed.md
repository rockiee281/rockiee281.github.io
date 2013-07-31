---
layout: post
category : android
tags : [android]
---
这两天鼓捣我们APP的在线升级，遇到一个让人抓狂的问题，折磨了两天终于搞定了。首先，我们的app是作为system app，随ROM一起发布，安装在/system/app目录下，这个是导致bug出现的根本原因。 
   
APP在启动的时候，会去服务器检查版本，发现有新版本的客户端的时候会提示用户更新，用户确认后启动下载并在下载完成后自动安装。故障的现象：
+ 安装没有问题，但是在安装之后如果选择“打开”则会报错，错误描述为某个activity不能cast成自己………… 十分诡异，理解不能 ╮(╯▽╰)╭ 
+ 打开之后发现app的界面还是旧版本界面，但是通过app的关于查看版本号发现已经是新版本的了
+ 因为我们这次对某些activity修改了名称，在打开这些activity的时候，系统会报错，提示该activity未注册
+ 多次退出app重新打开，现象依旧
+ /data/system/目录下的packages.xml文件中app的指向已经更新，对应的包名已经指向了/data/app/目录下新的apk文件
+ 重启手机之后，再打开app一切正常，不会报错，界面也都是新版的了    

就被这个bug给我折磨疯了…… 尝试了检查各种日志，然后还学习了strace这个神器，不过能力有限，很多日志看不明白。最后看了网上别人的在线app升级代码，发现问题可能是因为我们在安装新版本的代码时没有关闭目前正在运行的app引起的。所以就尝试把目前的升级代码：
<pre>
String filePath = (String) msg.obj;
Intent intent = new Intent(Intent.ACTION_VIEW);
intent.setDataAndType(Uri.parse("file://" + filePath), "application/vnd.android.package-archive");
mContext.startActivity(intent);
</pre>
进行了修改，增加了`System.exit(0)`，即在启动安装新版本的包之后，退出当前的app。进行了这次修改之后，发现解决了问题。    

对问题稍微做一下总结：
+ 之前的代码，重启手机恢复正常，同时查看app的版本信息已经是新的了，说明android在启动app的时候会读取一些缓存之类的东西，但是只是部分读取……
+ 只有system目录下的app会有这个问题，因为安装在/data/app目录下的app在更新时是直接替换的，就没有旧版本apk文件的存在了
+ 更新应用的时候退出一次就行，哪怕因为有注册service而被系统重新启动也不会有影响，具体的原因不明    
感觉android里头的坑还是好多啊……所以对这种严重依赖于系统的api的东西一直以来都比较恐惧，因为行为无法预知。找了本framework的书，要研究下app如何加载了。

BTW：之前升级也遇到过一个更白痴的问题，更新安装成功，然后重启之后版本就退回了原来的旧版本，百思不得其解。最后发现是……………………我们升级的时候只改了version name,没有更改version code。

2013-7-31 update:
本来以为事情到这就告一段落了，结果在后续的测试中发现还是有问题。虽然升级之后UI更新了，新功能也有了，但是其实还是会报错的。如果新旧版本中有删掉的activity或者改名了的，还是会报错。就是说，系统还是会去找原来的apk包。    
后来在开发同学们仔细检查了应用的代码之后发现，android的manifest文件中有一行配置`android:persistent="true"`,去掉这一行之后再升级就不会有问题了。后来发现有一段对这个配置的解释:
<pre>
Whether or not the application should remain running at all times . The default value is "false". Applications should not normally set this flag; persistence mode is intended only for certain system applications(phone,system).
</pre>    
这样一来，之前kill不掉应用就可以解释了。

