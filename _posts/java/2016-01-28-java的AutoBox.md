---
layout: post
category : java
tags : [java]
---
{% highlight java %}
 	Integer a = new Integer(1);
        Integer b = new Integer(1);
        System.out.println(a == b); // false
        System.out.println(a == 1); // true
        System.out.println(a .equals(b));   // true


        Integer c = 1;
        Integer d = 1;
        System.out.println(c == d); // true

        Integer e = Integer.valueOf(2883);
        Integer f = Integer.valueOf(2883);

        System.out.println(e == f); // false

        Integer g = 23321;
        Integer h = 23321;
        System.out.println(g == h); // false

{% endhighlight %}

今天组里的同学问起Integer能不能直接用==比较，直觉是不行的。但是发现java在autobox的时候，貌似用的是有cache？通过autobox出来的对象是同一个，估计是放在常量池的？Integer.valueOf是把128以内的放在了cache里面，这个倒是比较明确的。

输出字节码，能发现`Integer g = 23321;` 在生成字节码的时候是：

```
       126: sipush        23321
       129: invokestatic  #8                  // Method java/lang/Integer.valueOf:(I)Ljava/lang/Integer;
       132: astore        7
       134: sipush        23321
       137: invokestatic  #8                  // Method java/lang/Integer.valueOf:(I)Ljava/lang/Integer;

```
所以实际上是调用的Integer.valueOf()，因此大于127的自然都是新对象而不是cache了。
