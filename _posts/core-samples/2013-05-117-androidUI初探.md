---
layout: post
category : lessons
tagline: "android 学习录"
tags : [android, UI]
---
今天开始做android的UI了，要自己独立做一个新的页面，嗯，新的挑战…… 
第一个拦路虎，就是发现我们一个180x180的图片，在1280x720的宽屏手机上显示的特别大。但是如果在<code>ImageView</code>里头写死90dip或者180px之后，图片在手机上显示的大小就比较合理了，真是百思不得其解。怀疑和具体设备上的像素密度有关，后来就找到了[这篇文章](http://618119.com/archives/2011/01/12/205.html)。 用里面提供的代码，测出来我们设备的dpi是320，也就是对应的xhdpi。之前我的图片都是只放到了drawable目录下，copy了一份到drawable-xhdpi之后，在手机上显示就正常了！

