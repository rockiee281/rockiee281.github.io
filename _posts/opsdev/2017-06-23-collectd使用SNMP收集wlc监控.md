---
layout: post
category : opsdev
tags : [802.11, collectd, cisco, snmp, python, wlc]
---
## 前述
最近被wifi网络下VOIP的语音通话质量折磨，无法自拔。voip的语音数据经常毫无征兆的发生丢包、延迟抖动，但是又是偶发的，无从查起。
所以就想还是先搞起监控来呗，cisco的wlc是支持snmp协议的，那就按照设想的路数搞起来呗？
collectd通过snmp协议收集指标，感觉都是很成熟的东西了，应该不难吧……
使用网上查到的OID，直接就在collect的配置文件上加上了，看看效果先。
配置上之后，果然就愉快的出来监控了，对应的指标是`ifHCInOctets`,指代交换机上面的流量，也可以写成`Values ifInOctets ifOutOctets`。

```xml
    <Data "AcInterfaceTrafficBytesInOut">
        Type "if_octets"
        Table true
        Instance ".1.3.6.1.2.1.2.2.1.2"
        Values ".1.3.6.1.2.1.31.1.1.1.6" ".1.3.6.1.2.1.31.1.1.1.10"
    </Data>
    <Host "main_uplink_ac">
        Address "xx.xx.x.xxx"
        Version 2
        Community "public"
        Collect AcInterfaceTrafficBytesInOut
        Interval 30
    </Host>
```

当然，在折腾了一圈之后，发现有人用zabbix解决了这个问题，具体参见：https://github.com/B4ckF0rw4rd/Zabbix-Templates

## 基本概念
先说下几个概念：

+ SNMP：简单网络管理协议，可以通过这个协议去网络设备获取一些信息，也可以设置参数。这里我们通过SNMP协议抓取监控指标
+ MIB：[Management information base](https://en.wikipedia.org/wiki/Management_information_base) 供SNMP协议管理实体使用的数据库文件，简单来理解也可以把它当作一种scheme定义。
+ ASN: [Abstract Syntax Notation One](https://en.wikipedia.org/wiki/Abstract_Syntax_Notation_One) ASN是一种接口描述语言，
+ SMIv2：[Structure of Management Information Version 2](https://tools.ietf.org/html/rfc1155) 是MIB使用的ASN
+ OID：[Object identifier](https://en.wikipedia.org/wiki/Object_identifier) 可以理解为每个指标的名称，应该是全局唯一的比如`1.3.6.1.4.1.14179.1.1.1.12`就是一个OID，对应的名称是`agentInventoryManufacturerName`。目测的话，OID是遵循前缀层级的。

一般而言，针对多值的指标，使用table指明指标index对应的值，然后在做snmpwalk的时候通过OID+OID_INDEX获取对应的指标。但是对于cisco的wlc就遇到一个恶心的问题，
它的AP指标格式是OID+OID_INDEX+0/1，OID_INDEX是AP的mac地址，可以通过table转成AP的name，但是因为最后区分2.4G/5G网络，多出来了一级0或者1，就导致collectd
无法正常的解析AP的指标。没有仔细研究collectd的源码，不过在遇到value数量和table数量不匹配的时候，collectd就自动放弃解析了。网络上搜了半天也没看到现成的解决办法，
心一横，那就自己来呗？原理既然清楚了，那就自己来好了。用<s>最好的编程语言</s>python去收集指标，然后collectd支持python的plugin，问题分分钟解决。

## 环境准备

### 安装MIB

那么现在要做的事情就很简单了，首先参考[这个帖子](http://awesomeadmin.blogspot.jp/2009/11/monitoring-cisco-wireless-controller.html) 找到监控cisco 2500 wlc需要的MIB，http://www.oidview.com/mibs/14179/AIRESPACE-SWITCHING-MIB.html、http://www.oidview.com/mibs/14179/AIRESPACE-WIRELESS-MIB.html.当然，还有关联的一坨MIB文件，
下载下来的MIB文件放到原来的MIB文件夹中，比如`/usr/share/snmp/mibs/`,然后创建一个`/etc/snmp/snmp.conf`文件，把这些MIB文件导入进来就好了,样例如下。

```bash
mibs +AIRESPACE-SWITCHING-MIB
mibs +AIRESPACE-WIRELESS-MIB
mibs +AIRESPACE-REF-MIB
mibs +Q-BRIDGE-MIB
mibs +P-BRIDGE-MIB
mibs +RMON2-MIB
mibs +TOKEN-RING-RMON-MIB
```

这样一来，运行`snmpwalk`之类的命令测试的时候，看到的就不是一对数字啦，而是会比较好识别的OID指标的英文名称。比如`snmpwalk  -v 2c -c public  -OX  xxx.xxx.xx.xx  1.3.6.1.2.1.31.1.1.1.10`,会返回如下内容：

```bash
IF-MIB::ifHCOutOctets[1] = Counter64: 2788540709866
IF-MIB::ifHCOutOctets[2] = Counter64: 0
IF-MIB::ifHCOutOctets[3] = Counter64: 0
IF-MIB::ifHCOutOctets[4] = Counter64: 0
IF-MIB::ifHCOutOctets[5] = Counter64: 0
```
### 安装软件环境
本人使用的python版本是2.7，OS版本centos7，其他平台未经测试。

+ 通过pip安装python的easysnmp和collect库
+ 安装collectd，这里最好是从源码编译。二进制版本的话，logfile不支持debug级别，不方便查问题。自己从github下载源码，然后编译时候通过`--enable-debug`打开debug日志


## 脚本

### python部分
python脚本命名为get_wlc_collectd.py,可以放到任意喜欢的目录下去,代码参见: https://github.com/rockiee281/wlc_snmp_collectd_parser

### collectd配置

```bash
FQDNLookup   false
Interval 30

Timeout  15
ReadThreads  30
WriteThreads 10

LoadPlugin logfile
<Plugin logfile>
    LogLevel "debug"
    File "/var/log/collectd.log"
    Timestamp true
    PrintSeverity false
</Plugin>

LoadPlugin python
<Plugin python>
        ModulePath "/home/q/tools/bin/"
        Import "get_wlc_collectd"

        <Module get_wlc_collectd>
                host xx.xx.xx.xx
		community public
		version 2
        </Module>

</Plugin>

#LoadPlugin csv
#<Plugin "csv">
#  DataDir "/var/lib/collectd/csv"
#  StoreRates true
#</Plugin>

LoadPlugin write_graphite
<Plugin write_graphite>
  <Node "watcher_net">
    Host "xx.xx.0.xx"
    Port "2003"
    Protocol "tcp"
    Prefix "h."
    EscapeCharacter "."
  </Node>
</Plugin>

```

我们目前是把指标输出到graphite，为了测试方便可以先以CSV格式输出到本地。这里需要注意一点的是，为了方便后面在grafana里面针对各个维度聚合，指标名称里面是包含了`.`的，
所以用`EscapeCharacter "."` 保留dot，否则默认的话会被write_graphite插件转成下划线。
