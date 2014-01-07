---
layout: page
title: 醉卧沙场君莫笑
tagline: 古来征战几人回
---
{% include JB/setup %}
<meta property="wb:webmaster" content="72932cce5c47b863" />

## 文章列表

<ul class="posts">
  {% for post in site.posts %}
    <li><span>{{ post.date | date_to_string }}</span> &raquo; <a href="{{ BASE_PATH }}{{ post.url }}">{{ post.title }}</a></li>
  {% endfor %}
</ul>
