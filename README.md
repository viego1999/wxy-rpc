# WXY-RPC

## 介绍

一款基于 Netty + Zookeeper + SpringBoot 实现的自定义 RPC 框架。

后续考虑引入其他通信协议，例如（Http、Socket等），注册中心引入（Nacos等）。

等完善后在补充项目描述内容，未完待续。。。。

----------------

一个最基本的RPC框架如下图所示：

<img src="images\简单RPC架构图.png" alt="简单RPC架构图" style="zoom: 45%;" />

RPC框架一般必须包含三个组件，分别是**客户端、服务端**以及**注册中心**，一次完整的 RPC 调用流程一般为：

1. 服务端启动服务后，将他提供的服务列表发布到注册中心（服务注册）；
2. 客户端会向注册中心订阅相关的服务地址（服务订阅）；
3. 客户端通常会利用本地代理模块 Proxy 向服务端发起远程过程调用，Proxy 负责将调用的方法、参数等数据转化为网络字节流；
4. 客户端从服务列表中根据负载均衡策略选择一个服务地址，并将数据通过网络发送给服务端；
5. 服务端得到数据后，调用对应的服务，然后将结果通过网络返回给客户端。

虽然 RPC 调用流程很容易理解，但是实现一个完整的 RPC 框架设计到很多内容，例如服务注册与发现、通信协议与序列化、负载均衡、动态代理等，下面我们一一进行初步地讲解。

## 项目结构介绍

<img src="images\项目架构图.png" alt="项目架构图" style="zoom:67%;" />

`consumer`模块：服务的消费者，依赖于 `rpc-client-spring-boot-starter` 模块；

`provider-api`模块：服务提供者暴露的API；

`provider`模块：服务的提供者，依赖于 `rpc-server-spring-boot-starter` 模块：

`rpc-client-spring-boot`模块：rpc 客户端模块，封装客户端发起的请求过程，提供服务发现、动态代理，网络通信等功能；

`rpc-client-spring-boot-stater`模块：是`rpc-client-spring-boot`的stater模块，负责引入相应依赖进行自动配置；

`rpc-framework-core`模块：是rpc核心依赖，提供负载均衡、服务注册发现、消息协议、消息编码解码、序列化算法；

`rpc-server-spring-boot`模块：rpc 服务端模块，负责启动服务，接受和处理RPC请求，提供服务发布、反射调用等功能；

`rpc-server-spring-boot-stater`模块：是`rpc-server-spring-boot`的stater模块，负责引入相应依赖进行自动配置；

## 运行项目

1、首先需要安装并启动 zookeeper；

2、修改 Consumer 和 Provider 模块下的 application.yml 的注册中心地址属性，即 rpc.client.registry-addr=你的zk连接地址，服务端则配置 rpc.server.registry-addr属性；

3、先启动 Provider 模块，正常启动 SpringBoot 项目即可，本项目使用基于 SpringBoot 的自动配置，运行后会自动向 SpringIOC 容器中创建需要的 Bean 对象。

4、然后启动 Consumer 模块，通过 Controller 去访问服务进行 rpc 调用了。

## 项目实现的主要内容

### 自定义消息协议，编解码

#### 自定义消息协议

自定义协议的要数：

* 魔数，用来在第一时间判定是否是无效数据包
* 版本号，可以支持协议的升级
* 序列化算法，消息正文到底采用哪种序列化反序列化方式，可以由此扩展，例如：json、protobuf、hessian、jdk、kryo
* 指令类型，是登录、注册、单聊、群聊... 跟业务相关
* 请求序号，为了双工通信，提供异步能力，通过这个请求ID将响应关联起来，也可以通过请求ID做链路追踪。
* 正文长度，标注传输数据内容的长度，用于判断是否是一个完整的数据包
* 消息正文，主要传递的消息内容

> 魔数的作用：**快速** 识别[字节流](https://so.csdn.net/so/search?q=字节流&spm=1001.2101.3001.7020)是否是程序能够处理的，能处理才进行后面的 **耗时** 业务操作，如果不能处理，尽快执行失败，断开连接等操作。

本项目设计的消息协议如下：

```
---------------------------------------------------------------------
| 魔数 (4byte) | 版本号 (1byte)  | 序列化算法 (1byte) | 消息类型 (1byte) |
-------------------------------------------------------------------
|    状态类型 (1byte)  |    消息序列号 (4byte)   |    消息长度 (4byte)   |
---------------------------------------------------------------------
|                        消息内容 (不固定)                             |
---------------------------------------------------------------------
```

#### 编解码

##### 编解码实现

编解码主要实现类为：`com.wxy.rpc.core.codec.SharableRpcMessageCodec.java`，该类继承于 netty 中的 `io.netty.handler.codec.MessageToMessageCodec`，这个类是一个用于动态编/解码消息的编解码器，这可以看作是`MessageToMessageDecoder` 和 `MessageToMessageEncoder` 的组合。这个类中有两个方法，`encode()` 就是将输入的 `RpcMessage` 编码成 `ByteBuf` ，`decode()` 就是将 `ByteBuf` 解码成 `RpcMessage`，编码为出站操作，解码为入站操作。

##### 解决粘包半包

1、现象分析

粘包

* 现象，发送 abc def，接收 abcdef
* 原因
  * 应用层：接收方 ByteBuf 设置太大（Netty 默认 1024）
  * 滑动窗口：假设发送方 256 bytes 表示一个完整报文，但由于接收方处理不及时且窗口大小足够大，这 256 bytes 字节就会缓冲在接收方的滑动窗口中，当滑动窗口中缓冲了多个报文就会粘包
  * Nagle 算法：会造成粘包

半包

* 现象，发送 abcdef，接收 abc def
* 原因
  * 应用层：接收方 ByteBuf 小于实际发送数据量
  * 滑动窗口：假设接收方的窗口只剩了 128 bytes，发送方的报文大小是 256 bytes，这时放不下了，只能先发送前 128 bytes，等待 ack 后才能发送剩余部分，这就造成了半包
  * MSS 限制：当发送的数据超过 MSS 限制后，会将数据切分发送，就会造成半包



本质是因为 TCP 是流式协议，消息无边界

2、解决方案

- 短连接：发一次数据包建立一次连接，这样连接建立到连接断开之间就是一次消息边界，缺点是效率低；
- 固定长度：每一条消息采用固定长度，缺点是浪费空间；
- 分隔符：每一条消息采用分隔符，例如 \n ，缺点是需要转义；
- 消息长度+消息内容：每一条消息分为 header 和 body，header 中包含 body 的长度（推荐）；

本项目采取的是 消息长度 + 消息内容 来解决的半包问题，主要实现类为 `com.wxy.rpc.core.codec.RpcFrameDecoder` ，这个类继承了 netty 中的 `io.netty.handler.codec.LengthFieldBasedFrameDecoder` 类，这个类是一种解码器，根据消息中长度字段的值动态拆分接收到的ByteBufs。

在发送消息前，先约定用定长字节表示接下来数据的长度：

```java
// 最大长度，长度偏移，长度占用字节，长度调整，剥离字节数
public class LengthFieldBasedFrameDecoder {
    /**
     * Creates a new instance.
     *
     * @param maxFrameLength 		帧的最大长度（单位为字节，下同）
     * @param lengthFieldOffset 	长度字段的偏移长度（这里的长度字段就是 消息长度字段）
     * @param lengthFieldLength 	长度字段的长度
     * @param lengthAdjustment  	要添加到长度字段值的补偿值
     * @param initialBytesToStrip 	从解码帧中取出的第一个字节数
     */
    public LengthFieldBasedFrameDecoder(int maxFrameLength, int lengthFieldOffset, int lengthFieldLength, 
                                        int lengthAdjustment, int initialBytesToStrip) {
        this(maxFrameLength, lengthFieldOffset, lengthFieldLength, lengthAdjustment, 
             initialBytesToStrip, true);
    }
}
```

所以，按照我们定义的消息协议，只需要创建一个 `new LengthFiledBasedFrameDecoder(1024, 12, 4, 0)` 的帧解码器就可以解决粘包半包问题了。

### 序列化算法

本项目实现了五种序列化算法，分别是：

JDK、JSON、HESSIAN、KRYO 、PROTOSTUFF

五种序列化算法的比较如下：

后续补充......

### 负载均衡算法

本项目实现了 Random、RoundRobin、ConsistentHash 三种负载均衡算法

具体描述后续补充......

### 动态代理

已实现，后续补充描述.......

### 服务注册与发现

已实现，后续补充描述......

### 集成Spring通过注解进行服务注册与消费

已实现，后续补充描述......

### 集成 SpringBoot 完成自动配置

已实现，后续补充描述......

### 增加 Netty 心跳机制

已实现，后续补充描述......

## 环境搭建

- 操作系统：Windows + Linux
- 集成开发工具：IntelliJ IDEA
- 项目技术栈：SpringBoot 2.5.2 + JDK 1.8 + Netty 4.1.65.Final
- 项目依赖管理工具：Maven 4.0.0
- 注册中心：Zookeeeper 3.7.1

## 项目测试

- 启动 Zookeeper 服务器：进入到zk的bin目录，输入命令 `./zkServer.sh`
- 启动 provider 模块 ProviderApplication
- 启动 consumer 模块 ConsumerApplication
- 测试：浏览器输入 http://localhost:8080/hello/zhangsan，成功返回：`hello, zhangsan`, rpc 调用成功

## 流程

......