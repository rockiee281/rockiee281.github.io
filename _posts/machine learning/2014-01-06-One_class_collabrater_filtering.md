---
layout : post
category : machine learning
tags : [翻译,机器学习,OCCF]
---
http://www.resyschina.com/2011/04/one-class-collaborative-filtering.html#0-tsina-1-92495-397232819ff9a47a7b7e80a40613cfe1


##摘要
有许多协同过滤问题，例如对新闻、书签的推荐，都会被很自然的视为单分类协同过滤(OCCF)。在这些问题中，训练集数据经常是由简单的二元数据{binary data}组成的，比如新闻是否被访问或者网页是否被用户收藏到书签。经常这类数据是极其稀疏的，即其中只有一小部分是正样本，因此在解释负样本的时候就会出现模糊的情况。负样本和未标注的正样本会混在一起，而我们没有通用的办法去区分它们。例如，我们没有办法真正的确定一个用户没有收藏一个页面究竟是没有兴趣，还是没有浏览过这个页面。之前的研究仅仅将这种单分类问题视为一种分类问题。在这篇论文中，我们会在协同过滤的范畴下讨论这个问题。我们使用两种框架来解决OCCF问题。一种是基于加权低秩逼急算法；另一种是基于负样本采样。实验的结果表明我们的结果要明显的优于基准值。

##介绍
从搜索结果到产品推荐，网络上的个性化的服务已经变得越来越平常。这样的系统包括亚马逊为用户推荐的商品，Netflex推荐的DVD，Google推荐的新闻等等。这些系统中使用的核心技术就是协同过滤，一种利用之前所有用户对物品的打分情况来预测某个用户对物品偏好的技术。在这些系统中得分是显式的由用户打出的，例如在Netflix中用户的打分是1-5。但是，在更多的情况下用户行为的表现并不明显，比如是否点击或是收藏页面。这些隐式的评分数据更常见并且更容易获取。

尽管显示打分的优点很明显，但是其也存在缺陷，特别是在样本数据稀疏的情况下，很难去鉴别出典型的负样本数据。所有的这些负样本数据和确实的正样本混合在一起无法区分开来。所以我们把这种在纯正样本数据集上做的协同过滤叫做单分类协同过滤(OCCF)。OCCF会在不同的场景下使用，下面会举出两个例子。    

+ 社会化书签。
