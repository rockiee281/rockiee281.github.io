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

一段最简单的item base的代码如下：

	public void process(String inputFile, String outputData, String recommendItemNum) {

		BufferedWriter bw = null;
		try {
			DataModel dataModel = new FileDataModel(new File(inputFile));
			ItemSimilarity similarity = new LogLikelihoodSimilarity(dataModel);
			ItemBasedRecommender recommender = new GenericBooleanPrefItemBasedRecommender(dataModel, similarity);
			LongPrimitiveIterator it = dataModel.getItemIDs();

			bw = new BufferedWriter(new FileWriter(new File(outputData)));
			while (it.hasNext()) {
				long baseItemId = it.next();
				List<RecommendedItem> recommendResult = recommender.mostSimilarItems(baseItemId,
						Integer.valueOf(recommendItemNum));
				StringBuilder sb = new StringBuilder();
				for (RecommendedItem recommendedItem : recommendResult) {
					sb.append("," + recommendedItem.getItemID());
				}
				bw.append(baseItemId + sb.append("\n").toString());
			}
			bw.flush();
		} catch (Exception e) {
			logger.error("处理文件:【" + inputFile + "】失败", e);
		} finally {
			if (bw != null) {
				try {
					bw.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}

	}

##DataModel
首先关注的是FileDataModel类，作为一个datamodel，负责从文本文件中抽取出数据，并做预处理。

在FileDataModel的构造方法中，它首先会检查参数是否合法，通过封装了BufferedReader得到一个FileLineIterator对象用于读取文本内容，这里发现代码中大量使用了GWT的框架代码。比如使用Preconditions.checkArgument()来校验参数，用Splitter.on()来分割字符串，用Closeables.closeQuietly（）来关闭reader等，看来自己也得学习少造轮子了。FileDataModel中通过reload()方法来加载model，这里通过一个ReentrantLock来保证线程安全。在处理数据文件的时候，也会根据有没有preference数据、是否完全重新加载来做区分操作。在Data File中，如果格式为"userID,itemID,"就会删除对应的数据，如果带有Preference就删除Preference，相当于用户未对该Item打分；如果没有Preference则删除对应的Item，相当于用户未对Item有过行为。在保存数据文件的时候，它使用的是FastByIDMap，根据描述，它在有的场合下比JDK中自带的HashMap性能要好。嗯，也是根据高纳德的神书中的算法写出来的。最后得出来的rawdata就是一个fastByIDMap数据集和一个对应timestamp数据集，然后由他们生成一个GenericDataModel（有preference）或者GenericBooleanPrefDataModel（没有Preference）。    

这里可以看出在mahout的设计者眼中，timestamp是推荐模型的一个很重要的维度，可惜暂时我们现在还没用上这个维度的数据。目前可以想到的一个点，就是在天的维度上做去马太效应。因为我们每天都会有推荐游戏，这些游戏可能只是因为同时出现在推荐位而被用户下载，而不是真正的因为共同的兴趣，最好是可以把这部分数据过滤掉。

##ItemSimilarity
ItemSimilarity用来计算物品之间、用户之间的相似度。    

首先让我们来看看它如何计算User之间的相似度的，这个是由`public double userSimilarity(long userID1, long userID2)`方法来实现的，而其核心的计算依赖于LogLikelihood.logLikelihoodRatio方法，这里有个blog介绍其[具体的算法](http://tdunning.blogspot.com/2008/03/surprise-and-coincidence.html)。里面提到的一个计算公式是根据 Shannon entropy 来计算相似度，即香农熵，貌似是信息论里面一个很熟悉的公式啊。

##CandidateItemsStrategy和MostSimilarItemsCandidateItemsStrategy
在GenericItemBasedRecommender类的构造方法中可以看出来，完整的构造器需要4个参数，除了datamodel和similarity，就是标题中提到的这两位。

如果不指定参数，默认的CandiadteItemStrategy和MostSimilarItemsCandidateItemsStrategy都是PreferredItemsNeighborhoodCandidateItemsStrategy。它的策略很简单，就是找出对该物品有过行为的用户其他有行为过的物品，也就是和该物品“相关”的物品。剩下的工作就是调用TopItems.getTopItems对这个物品的集合进行打分，获取topN的数据了。这里它会调用Estimator对每个Item打分，然后用一个PriorityQueue保存数据，遍历所有的item集合，只保存评分最高的N个数据。那这样最重要的打分，就是由MostSimilarEstimator来完成。进去看一下MostSimilarEstimator的estimate方法，发现它里面还是在调用Similarity的itemSimilarities方法。OK，现在事情明了了，最终ItemBase的物品推荐，最核心的物品相关度的计算，就是依赖于ItemSimilarity的计算，而其计算的算法就是上文提到的Ted Dunning的算法。

能够看出来，这里的算法还是有很多可以定制的空间的。结合我们的应用场景做一些个定制，应该是可以提升推荐的效果。

