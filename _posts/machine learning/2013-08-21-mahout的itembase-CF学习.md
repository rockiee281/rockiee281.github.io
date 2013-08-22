---
layout : post
category : machine learning
tags : [机器学习, mahout]
---
最近在了解推荐系统方面的知识，下了mahout的代码并用我们的数据集跑了一遍。使用itembase CF算法的话，GenericRecommenderIRStatsEvaluator计算出来的数据如下：
    IRStatisticsImpl[precision:0.088206627680312,recall:0.088206627680312,fallOut:0.004144515328725854,nDCG:0.08950552860760644,reach:1.0]
    IRStatisticsImpl[precision:0.11704422869471408,recall:0.11704422869471408,fallOut:0.008063523025619029,nDCG:0.11989507918850728,reach:1.0]
    IRStatisticsImpl[precision:0.13502109704641352,recall:0.13502109704641352,fallOut:0.011903379398443853,nDCG:0.1310291428593,reach:1.0]

at值分别为3、6、9，也就是每次提取出比较的TopN的值，precision和recall惨不忍睹啊。但是在通过AverageAbsoluteDifferenceRecommenderEvaluator计算出来的RMSE值0.6464869346946893，感觉并不是太差，不太理解啊。后面两项值fallOut和nDCG没太明白是什么含义，特意去网上搜了一下，最后在stackoverflow上找到一个[答案](http://stackoverflow.com/questions/16478192/how-to-interpret-irstatisticsimpl-data-in-mahout):
    by definition fallOut is "The proportion of non-relevant documents that are retrieved, out of all non-relevant documents available:" en.wikipedia.org/wiki/Information_retrieval#Fall-out As far as I know, it should be lowest as possible, but it also trivial to get 0% so you might evaluate your domain. – gpicchiarelli 
    about nDCG, it is a normalized version of DCG which means "Discounted Cumulative Gain". To be precise, take a look here en.wikipedia.org/wiki/Discounted_cumulative_gain – gpicchiarelli 
上面回答中提到的wiki页面上也有很不错的内容
