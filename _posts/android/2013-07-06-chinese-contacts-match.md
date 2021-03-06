---
layout: post
category : android
tags : [android,contacts match]
---
这两天在琢磨一个中文通讯录里面用户名称匹配的问题，大概的需求是这样的，比如我的通讯录里面有一个名字叫“张三丰” 的联系人，我希望能够更方便的查找到联系人。手机上输入中文肯定是不方便的嘛，肯定是输入字母来快速定位更便捷，基本的想法是通过将中文转化为拼音然后来匹配，基本的case如下:    
* 首先，输入完整的拼音肯定是要能够匹配的，比如输入 <code>zhang san feng</code>
* 其次，输入首字母肯定也能够匹配，比如“zsf”
* 然后输入拼音的一部分也得能够匹配出来 - -||， 比如“zhangsf”
* 总而言之，就是如果输入的字符串是拼音全拼的顺序字串，就应该能够匹配出来，并且需要将对应位置的名字高亮
* 但是，如果输入“zhgf”或者“zhsn”这种，虽然也是全拼名称的顺序字串，但是因为zh后面紧跟的不是“s”这个声母，那表面用户试图输入的应该是“张国锋”或者“张三娘”之类的名字，这个时候不应该匹配到“张三丰”    

大概的需求就是这样，一开始从网上找了下资料，然后发现了[LCS算法](http://en.wikipedia.org/wiki/Longest_common_subsequence_problem)，不过发现和我想要的并不是十分一致，不过还是给了点启发。然后就自己动手写了下面这个小东西，粗略的估算下，算法的时间复杂度应该是o(n^2)，肯定是应该有更优秀的解决办法的。
代码地址：https://github.com/rockiee281/JavaUtils/blob/master/src/com/liyun/contacts/MyContactsMatch.java
