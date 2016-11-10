---
layout: post
category : opensource project
tags : [spark SQL, Lzo, Json]
---
为了减少对存储的消耗，dfs中文件采用了LZO的方式去存储，这样就遇到了一个比较棘手的问题。SparkSQL官方文档里面给的示例都是直接读取一个未压缩文件，对于一个Lzo压缩过之后的文件怎么处理呢，找了半天没有找到一个比较直接的方案。看了下spark的scala api，最终找到了一个方法。

先用LzoTextInputFormat加载RDD，然后转化成RDD[String]，最后让sparkSqlContext直接去读这个RDD。

BTW:这里需要注意另外一个问题，使用Lzo做存储格式的时候，需要建立index，否则是不能做并行处理的……之前采用gz的格式存储数据，就吃了这个亏。

```scala
import org.apache.hadoop.io._
import com.hadoop.mapreduce._

val data = sc.newAPIHadoopFile[LongWritable, Text, LzoTextInputFormat]("/path/of/file/*/*")
val sqlData = spark.read.json(data.values.map(_.toString))
sqlData.printSchema()
sqlData.createOrReplaceTempView("vehicle_info")
val ret = spark.sql("select * from vehicle_info where carNo like '云%'")
ret.count()
ret.show(10,false)
ret.coalesce(1).write.mode("overwrite").format("com.databricks.spark.csv").option("header", "true").save("/user/vin")

```
