---
layout: post
category : android
tagline: "android lesson notebook(1)"
tags : [android]
---
准备啃掉google 官方网站的training lesson.    
第一课是activity，内容很简单，主要包含三个部分。
+ starting an activity  
+ resuming and pausing activity    
+ Stoping and Restarting activity    
+ Recreating an Activity    

##Resuming and Pausing
其实在开发app的过程中，对这几个环节都已经比较熟悉了，但是通过学习官方的doc，还是对原来一知半解的东西有了额外的了解。比如在activity销毁的过程中，android只保证`onPause()`是一定可以被调用的，其他的几个方法比如`onDestory()`和`onStop()`并不保证一定会被调用。因此一些重要信息的保存最好放在`onPause`中，比如如一个mail应用需要保存草稿，又或者一个拍照应用释放camera供别的activity调用，还有停止动画效果来节省CPU。但是同时`onPause()`方法里不要做一些过于重的CPU操作，比如插入数据库等，因为这可能会影响到下一个Activity的流畅性。    
另一个方面，`onResume()`应该去负责重新初始化在`onPause()`中释放的资源，比如开始动画效果等。

##Stoping and Restarting
当前的activity完全不可见的时候，就是调用`onStop()`的时机，比如用户接到一个电话、打开了一个新的activity或者跳转到另外一个app中。上面提到的`onPause()`中中不适合干的脏活累活，现在就可以都交给`onStop()`啦，因为此时activity完全不可见，就不用担心流畅性问题了，比如保存draft数据到DB中。

至于`onRestart()`方法，文档中提到因为用户可能经过比较长一段时间才会返回这个activity，所以可以在`onRestart`方法中判断用户使用环境是否有变化，比如GPS是否依然开启等。

##Recreating an Activity
这里提到如何重新创建一个activity。用户在点击返回键或者在程序中调用`finish()`方法会触发销毁一个activity，同时如果一个activity如果在后台运行，系统也可能能为了回收资源而销毁activity。对于系统回收资源引起的销毁，一些信息会保存在一个Bundle对象中，可以通过这方对象来恢复Activity.在这里，文档中提到了一个很有价值的Tip，手机在旋转时如果出发了屏幕从横屏切换到纵屏的时候，会将原来的activity销毁去创建一个新的。所以这样来讲的话，禁止屏幕的自动旋转应该会节省一些开销吧。

