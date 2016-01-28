---
layout: post
category : java
tags : [java, multithread, yield]
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
        System.out.println(c == d); // true

{% endhighlight %}
今天组里的同学问起Integer能不能直接用==比较，直觉是不行的。但是发现java在autobox的时候，貌似用的是有cache？通过autobox出来的对象是同一个，估计是放在常量池的？Integer.valueOf是把128以内的放在了cache里面，这个倒是比较明确的。
