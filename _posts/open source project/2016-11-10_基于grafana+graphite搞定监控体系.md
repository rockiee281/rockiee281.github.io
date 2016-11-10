---
layout: post
category : opensource project
tags : [grafana, graphite]
---
grafana是监控的展示前端，graphite用于日志的搜集、存储，这两者配合起来可以很好的共同工作。graphite这边主要是用了三个组件：whisper(日志存储)、carbon(日志采集)和graphite-api（提供restFul的请求监控数据接口）。
