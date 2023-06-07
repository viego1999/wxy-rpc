# WXY-RPC

## 介绍

一款基于 Netty + Zookeeper + SpringBoot 实现的自定义 RPC 框架。

同时引入其他通信协议，有 Http、Socket 等，注册中心引入了 Zookeeper、Nacos、Eureka等。

基于 JMH 压测在 10000 并发量下的吞吐量在 29300 上下。

----------------

### 项目实现内容

- [x] 实现基于 Netty/Socket/Http 三种方式进行网路通信
- [x] 自定义消息协议，编解码器
- [x] 五种序列化算法（JDK、JSON、HESSIAN、KRYO、PROTOSTUFF）
- [x] 三种负载均衡算法（RoundRobin、Random、ConsistentHash）
- [x] 两种动态代理（JDK、CGLIB）
- [x] 基于 Zookeeper 的服务注册与发现，增加服务本地缓存与监听
- [x] 集成 Spring，自定义注解提供 RPC 组件扫描、服务注册、服务消费
- [x] 集成 SpringBoot，完成自动配置
- [x] 增加 Netty 心跳机制，复用 Channel 连接
- [x] 实现自定义 SPI 机制
- [x] 10000个线程同时发起RPC调用的吞吐量在 29300 上下

----

### RPC概述

RPC 又称远程过程调用（Remote Procedure Call），用于解决分布式系统中服务之间的调用问题。通俗地讲，就是开发者能够像调用本地方法一样调用远程的服务。一个最基本的RPC框架的基本架构如下图所示：

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

既然 RPC 是远程调用，必然离不开网络通信协议。客户端在向服务端发起调用之前，需要考虑采用何种方式将调用信息进行编码，并传输到服务端。因为 RPC 框架对性能有非常高的要求，所以通信协议应该越简单越好，这样可以减少编解码的性能损耗。RPC 框架可以基于不同的协议实现，大部分主流 RPC 框架会选择 TCP、HTTP 协议，出名的 gRPC 框架使用的则是 HTTP2。TCP、HTTP、HTTP2 都是稳定可靠的，但其实使用 UDP 协议也是可以的，具体看业务使用的场景。成熟的 RPC 框架能够支持多种协议，例如阿里开源的 Dubbo 框架被很多互联网公司广泛使用，其中可插拔的协议支持是 Dubbo 的一大特色，这样不仅可以给开发者提供多种不同的选择，而且为接入异构系统提供了便利。

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

#### 概述

客户端和服务端在通信过程中需要传输哪些数据呢？这些数据又该如何编解码呢？如果采用 TCP 协议，你需要将调用的接口、方法、请求参数、调用属性等信息序列化成二进制字节流传递给服务提供方，服务端接收到数据后，再把二进制字节流反序列化得到调用信息，然后利用反射的原理调用对应方法，最后将返回结果、返回码、异常信息等返回给客户端。所谓序列化和反序列化就是将对象转换成二进制流以及将二进制流再转换成对象的过程。因为网络通信依赖于字节流，而且这些请求信息都是不确定的，所以一般会选用通用且高效的序列化算法。比较常用的序列化算法有 FastJson、Kryo、Hessian、Protobuf 等，这些第三方序列化算法都比 Java 原生的序列化操作都更加高效。Dubbo 支持多种序列化算法，并定义了 Serialization 接口规范，所有序列化算法扩展都必须实现该接口，其中默认使用的是 Hessian 序列化算法。

序列化对于远程调用的响应速度、吞吐量、网络带宽消耗等同样也起着至关重要的作用，是我们提升分布式系统性能的最关键因素之一。

判断一个编码框架的优劣主要从以下几个方面：

```undefined
1. 是否支持跨语言，支持语种是否丰富
2. 编码后的码流
3. 编解码的性能
4. 类库是否小巧，API使用是否方便
5. 使用者开发的工作量和难度。
```

#### 实现

本项目实现了五种序列化算法，分别是：

**JDK、JSON、HESSIAN、KRYO 、PROTOSTUFF**，其中JSON使用的是Gson实现，此外还可以使用FastJson、Jackson等实现JSON序列化。

五种序列化算法的比较如下：

| 序列化算法          | **优点**         | **缺点**   |
|----------------|----------------|----------|
| **Kryo**       | 速度快，序列化后体积小    | 跨语言支持较复杂 |
| **Hessian**    | 默认支持跨语言        | 较慢       |
| **Protostuff** | 速度快，基于protobuf | 需静态编译    |
| **Json**       | 使用方便           | 性能一般     |
| **Jdk**        | 使用方便，可序列化所有类   | 速度慢，占空间  |

性能对比图，单位为 nanos：

<img src="images\序列化性能对比.png" alt="序列化性能对比" style="zoom:100%;" />

<img src="images\序列化性能比较.png" alt="序列化性能比较图" style="zoom:100%;" />

测试环境：

### 负载均衡算法

本项目实现了 Random、RoundRobin、ConsistentHash 三种负载均衡算法

在分布式系统中，服务提供者和服务消费者都会有多台节点，如何保证服务提供者所有节点的负载均衡呢？客户端在发起调用之前，需要感知有多少服务端节点可用，然后从中选取一个进行调用。客户端需要拿到服务端节点的状态信息，并根据不同的策略实现负载均衡算法。负载均衡策略是影响 RPC 框架吞吐量很重要的一个因素，下面我们介绍几种最常用的负载均衡策略。

- Round-Robin 轮询。Round-Robin 是最简单有效的负载均衡策略，并没有考虑服务端节点的实际负载水平，而是依次轮询服务端节点。
- Weighted Round-Robin 权重轮询。对不同负载水平的服务端节点增加权重系数，这样可以通过权重系数降低性能较差或者配置较低的节点流量。权重系数可以根据服务端负载水平实时进行调整，使集群达到相对均衡的状态。
- Least Connections 最少连接数。客户端根据服务端节点当前的连接数进行负载均衡，客户端会选择连接数最少的一台服务器进行调用。Least Connections 策略只是服务端其中一种维度，我们可以演化出最少请求数、CPU 利用率最低等其他维度的负载均衡方案。
- Consistent Hash 一致性 Hash。目前主流推荐的负载均衡策略，Consistent Hash 是一种特殊的 Hash 算法，在服务端节点扩容或者下线时，尽可能保证客户端请求还是固定分配到同一台服务器节点。Consistent Hash 算法是采用哈希环来实现的，通过 Hash 函数将对象和服务器节点放置在哈希环上，一般来说服务器可以选择 IP + Port 进行 Hash，然后为对象选择对应的服务器节点，在哈希环中顺时针查找距离对象 Hash 值最近的服务器节点。

此外，负载均衡算法可以是多种多样的，客户端可以记录例如健康状态、连接数、内存、CPU、Load 等更加丰富的信息，根据综合因素进行更好地决策。

### 动态代理

RPC 框架怎么做到像调用本地接口一样调用远端服务呢？这必须依赖动态代理来实现。需要创建一个代理对象，在代理对象中完成数据报文编码，然后发起调用发送数据给服务提供方，以此屏蔽 RPC 框架的调用细节。因为代理类是在运行时生成的，所以代理类的生成速度、生成的字节码大小都会影响 RPC 框架整体的性能和资源消耗，所以需要慎重选择动态代理的实现方案。动态代理比较主流的实现方案有以下几种：JDK 动态代理、Cglib、Javassist、ASM、Byte Buddy，我们简单做一个对比和介绍。

- JDK 动态代理。在运行时可以动态创建代理类，但是 JDK 动态代理的功能比较局限，代理对象必须实现一个接口，否则抛出异常。因为代理类会继承 Proxy 类，然而 Java 是不支持多重继承的，只能通过接口实现多态。JDK 动态代理所生成的代理类是接口的实现类，不能代理接口中不存在的方法。JDK 动态代理是通过反射调用的形式代理类中的方法，比直接调用肯定是性能要慢的。
- Cglib 动态代理。Cglib 是基于 ASM 字节码生成框架实现的，通过字节码技术生成的代理类，所以代理类的类型是不受限制的。而且 Cglib 生成的代理类是继承于被代理类，所以可以提供更加灵活的功能。在代理方法方面，Cglib 是有优势的，它采用了 FastClass 机制，为代理类和被代理类各自创建一个 Class，这个 Class 会为代理类和被代理类的方法分配 index 索引，FastClass 就可以通过 index 直接定位要调用的方法，并直接调用，这是一种空间换时间的优化思路。
- Javassist 和 ASM。二者都是 Java 字节码操作框架，使用起来难度较大，需要开发者对 Class 文件结构以及 JVM 都有所了解，但是它们都比反射的性能要高。Byte Buddy 也是一个字节码生成和操作的类库，Byte Buddy 功能强大，相比于 Javassist 和 ASM，Byte Buddy 提供了更加便捷的 API，用于创建和修改 Java 类，无须理解字节码的格式，而且 Byte Buddy 更加轻量，性能更好。

本项目实现了 【JDK动态代理】 和 【CGLIB 动态代理】。

### 服务注册与发现

在分布式系统中，不同服务之间应该如何通信呢？传统的方式可以通过 HTTP 请求调用、保存服务端的服务列表等，这样做需要开发者主动感知到服务端暴露的信息，系统之间耦合严重。为了更好地将客户端和服务端解耦，以及实现服务优雅上线和下线，于是注册中心就出现了。

在 RPC 框架中，主要是使用注册中心来实现服务注册和发现的功能。服务端节点上线后自行向注册中心注册服务列表，节点下线时需要从注册中心将节点元数据信息移除。客户端向服务端发起调用时，自己负责从注册中心获取服务端的服务列表，然后在通过负载均衡算法选择其中一个服务节点进行调用。以上是最简单直接的服务端和客户端的发布和订阅模式，不需要再借助任何中间服务器，性能损耗也是最小的。

现在思考一个问题，服务在下线时需要从注册中心移除元数据，那么注册中心怎么才能感知到服务下线呢？我们最先想到的方法就是节点主动通知的实现方式，当节点需要下线时，向注册中心发送下线请求，让注册中心移除自己的元数据信息。但是如果节点异常退出，例如断网、进程崩溃等，那么注册中心将会一直残留异常节点的元数据，从而可能造成服务调用出现问题。

为了避免上述问题，实现服务优雅下线比较好的方式是采用主动通知 + 心跳检测的方案。除了主动通知注册中心下线外，还需要增加节点与注册中心的心跳检测功能，这个过程也叫作探活。心跳检测可以由节点或者注册中心负责，例如注册中心可以向服务节点每 60s 发送一次心跳包，如果 3 次心跳包都没有收到请求结果，可以任务该服务节点已经下线。

由此可见，采用注册中心的好处是可以解耦客户端和服务端之间错综复杂的关系，并且能够实现对服务的动态管理。服务配置可以支持动态修改，然后将更新后的配置推送到客户端和服务端，无须重启任何服务。

本项目目前实现了以 Zookeeper 为注册中心，后续考虑引入 Nacos、Redis 等实现服务注册于发现功能。

### RPC调用方式

#### 概述

成熟的 RPC 框架一般会提供四种调用方式，分别为同步 Sync、异步 Future、回调 Callback和单向 Oneway。RPC 框架的性能和吞吐量与合理使用调用方式是息息相关的，下面我们逐一介绍下四种调用方式的实现原理。

- Sync 同步调用。客户端线程发起 RPC 调用后，当前线程会一直阻塞，直至服务端返回结果或者处理超时异常。Sync 同步调用一般是 RPC 框架默认的调用方式，为了保证系统可用性，客户端设置合理的超时时间是非常重要的。虽说 Sync 是同步调用，但是客户端线程和服务端线程并不是同一个线程，实际在 RPC 框架内部还是异步处理的。Sync 同步调用的过程如下图所示。

<img src="images\Sync同步调用.png" alt="Sync同步调用" style="zoom:67%;" />

- Future 异步调用。客户端发起调用后不会再阻塞等待，而是拿到 RPC 框架返回的 Future 对象，调用结果会被服务端缓存，客户端自行决定后续何时获取返回结果。当客户端主动获取结果时，该过程是阻塞等待的。Future 异步调用过程如下图所示。

<img src="images\Future异步调用.png" alt="Future异步调用" style="zoom:67%;" />

- Callback 回调调用。如下图所示，客户端发起调用时，将 Callback 对象传递给 RPC 框架，无须同步等待返回结果，直接返回。当获取到服务端响应结果或者超时异常后，再执行用户注册的 Callback 回调。所以 Callback 接口一般包含 onResponse 和 onException 两个方法，分别对应成功返回和异常返回两种情况。

<img src="images\Callback回调调用.png" alt="Callback回调调用" style="zoom:67%;" />

- Oneway 单向调用。客户端发起请求之后直接返回，忽略返回结果。Oneway 方式是最简单的，具体调用过程如下图所示。

<img src="images\Onway单向调用.png" alt="Onway单向调用" style="zoom:67%;" />

四种调用方式都各有优缺点，很难说异步方式一定会比同步方式效果好，在不用的业务场景可以按需选取更合适的调用方式。

#### 实现

本项目实现的是第一种 Sync 同步调用。具体的实现逻辑在类 `com.wxy.rpc.client.transport.netty.NettyRpcClient` 中，使用 `io.netty.util.concurrent.Promise` 去接受响应结果，将暂未处理的`RpcResponse`根据`sequenceId`信息存入`ConcurrentHashMap` 中，`RpcResponseHadler` 根据 `sequenceId` 取出 `Promise` 对象存储的未处理的响应消息，处理后通过设置 `promise`的状态来`notify`等待结果的线程并返回，核心代码如下：

```java
public class NettyRpcClient implements RpcClient {
    
	// ....
    
    @Override
    public RpcMessage sendRpcRequest(RequestMetadata requestMetadata) {
        // 构建接收返回结果的 promise
        Promise<RpcMessage> promise;
        // 获取 Channel 对象
        Channel channel = getChannel(new InetSocketAddress(requestMetadata.getServerAddr(), requestMetadata.getPort()));
        if (channel.isActive()) {
            // 创建 promise 来接受结果         指定执行完成通知的线程
            promise = new DefaultPromise<>(channel.eventLoop());
            // 获取请求的序列号 ID
            int sequenceId = requestMetadata.getRpcMessage().getHeader().getSequenceId();
            // 存入还未处理的请求
            RpcResponseHandler.UNPROCESSED_RPC_RESPONSES.put(sequenceId, promise);
            // 发送数据并监听发送状态
            channel.writeAndFlush(requestMetadata.getRpcMessage()).addListener((ChannelFutureListener) future -> {
                if (future.isSuccess()) {
                    log.info("The client send the message successfully, msg: [{}].", requestMetadata);
                } else {
                    future.channel().close();
                    promise.setFailure(future.cause());
                    log.error("The client send the message failed.", future.cause());
                }
            });
            // 等待结果返回（让出cpu资源，同步阻塞调用线程main，其他线程去执行获取操作（eventLoop））
            promise.await();
            if (promise.isSuccess()) {
                // 返回响应结果
                return promise.getNow();
            } else {
                throw new RpcException(promise.cause());
            }
        } else {
            throw new IllegalStateException("The channel is inactivate.");
        }
    }
    
    // ....
    
}
```

```java
public class RpcResponseHandler extends SimpleChannelInboundHandler<RpcMessage> {

    /**
     * 存放未处理的响应请求
     */
    public static final Map<Integer, Promise<RpcMessage>> UNPROCESSED_RPC_RESPONSES = new ConcurrentHashMap<>();

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, RpcMessage msg) throws Exception {
        try {
            MessageType type = MessageType.parseByType(msg.getHeader().getMessageType());
            // 如果是 RpcRequest 请求
            if (type == MessageType.RESPONSE) {
                int sequenceId = msg.getHeader().getSequenceId();
                // 拿到还未执行完成的 promise 对象
                Promise<RpcMessage> promise = UNPROCESSED_RPC_RESPONSES.remove(sequenceId);
                if (promise != null) {
                    Exception exception = ((RpcResponse) msg.getBody()).getExceptionValue();
                    if (exception == null) {
                        promise.setSuccess(msg);
                    } else {
                        promise.setFailure(exception);
                    }
                }
            } else if (type == MessageType.HEARTBEAT_RESPONSE) { // 如果是心跳检查请求
                log.info("Heartbeat info {}.", msg.getBody());
            }
        } finally {
            // 释放内存，防止内存泄漏
            ReferenceCountUtil.release(msg);
        }
    }
    
    // ......
}
```

### 集成 Spring 自定义注解提供服务注册与消费

- @RpcComponentScan - 扫描被 @RpcService 标注的组件并将对应的 BeanDefiniton 对象注册到Spring。

```java
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
@Import(RpcBeanDefinitionRegistrar.class)
public @interface RpcComponentScan {
    // ......
}
```

```java
public class RpcBeanDefinitionRegistrar implements ImportBeanDefinitionRegistrar, ResourceLoaderAware {

    // ......

    /**
     * 此方法会在 spring 自定义扫描执行之后执行，这个时候 beanDefinitionMap 已经有扫描到的 beanDefinition 对象了
     *
     * @param annotationMetadata annotation metadata of the importing class
     * @param registry           current bean definition registry
     */
    @Override
    public void registerBeanDefinitions(AnnotationMetadata annotationMetadata, BeanDefinitionRegistry registry) {
        // 获取 RpcComponentScan 注解的属性和值
        AnnotationAttributes annotationAttributes = AnnotationAttributes
                .fromMap(annotationMetadata.getAnnotationAttributes(RpcComponentScan.class.getName()));
        String[] basePackages = {};
        if (annotationAttributes != null) {
            // 此处去获取RpcComponentScan 注解的 basePackages 值
            basePackages = annotationAttributes.getStringArray("basePackages");
        }
        // 如果没有指定名称的话
        if (basePackages.length == 0) {
            basePackages = new String[]{((StandardAnnotationMetadata) annotationMetadata).getIntrospectedClass().getPackage().getName()};
        }
        // 创建一个浏览 RpcService 注解的 Scanner
        // 备注：此处可以继续扩展，例如扫描 spring bean 或者其他类型的 Scanner
        RpcClassPathBeanDefinitionScanner rpcServiceScanner = new RpcClassPathBeanDefinitionScanner(registry, RpcService.class);

        if (this.resourceLoader != null) {
            rpcServiceScanner.setResourceLoader(this.resourceLoader);
        }

        // 扫描包下的所有 Rpc bean 并返回注册成功的数量（scan方法会调用register方法去注册扫描到的类并生成 BeanDefinition 注册到 spring 容器）
        int count = rpcServiceScanner.scan(basePackages);
        log.info("The number of BeanDefinition scanned and registered by RpcServiceScanner is {}.", count);
    }
}
```

- @RpcService - 该注解用来标注需要暴露的服务实现类，被标注的类将会被注入到 Spring 容器中，同时将对应服务信息注册到远程注册中心；

```java
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
public @interface RpcService {
    // ......
}
```

```java
public class RpcServerBeanPostProcessor implements BeanPostProcessor, CommandLineRunner {

    // .......
    
    /**
     * 在 bean 实例化后，初始化后，检测标注有 @RpcService 注解的类，将对应的服务类进行注册，对外暴露服务，同时进行本地服务注册
     *
     * @param bean     bean
     * @param beanName beanName
     * @return 返回增强后的 bean
     * @throws BeansException Bean 异常
     */
    @SneakyThrows
    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        // 判断当前 bean 是否被 @RpcService 注解标注
        if (bean.getClass().isAnnotationPresent(RpcService.class)) {
            log.info("[{}] is annotated with [{}].", bean.getClass().getName(), RpcService.class.getCanonicalName());
            // 获取到该类的 @RpcService 注解
            RpcService rpcService = bean.getClass().getAnnotation(RpcService.class);
            String interfaceName;
            if ("".equals(rpcService.interfaceName())) {
                interfaceName = rpcService.interfaceClass().getName();
            } else {
                interfaceName = rpcService.interfaceName();
            }
            String version = rpcService.version();
            String serviceName = ServiceUtil.serviceKey(interfaceName, version);
            // 构建 ServiceInfo 对象
            ServiceInfo serviceInfo = ServiceInfo.builder()
                    .appName(properties.getAppName())
                    .serviceName(serviceName)
                    .version(version)
                    .address(InetAddress.getLocalHost().getHostAddress())
                    .port(properties.getPort())
                    .build();
            // 进行远程服务注册
            serviceRegistry.register(serviceInfo);
            // 进行本地服务缓存注册
            LocalServiceCache.addService(serviceName, bean);
        }
        return bean;
    }
}
```

- @RpcReference - 服务注入注解，被标注的属性将自动注入服务的实现类（基于动态代理实现）

```java
@Target({ElementType.FIELD, ElementType.METHOD, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
public @interface RpcReference {
    // ......
}
```

```java
public class RpcClientBeanPostProcessor implements BeanPostProcessor {

    // ......

    /**
     * 在 bean 实例化完后，扫描 bean 中需要进行 rpc 注入的属性，将对应的属性使用 代理对象 进行替换
     *
     * @param bean     bean 对象
     * @param beanName bean 名称
     * @return 后置增强后的 bean 对象
     * @throws BeansException bean 异常
     */
    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        // 获取该 bean 的类的所有属性（getFields - 获取所有的public属性，getDeclaredFields - 获取所有声明的属性，不区分访问修饰符）
        Field[] fields = bean.getClass().getDeclaredFields();
        // 遍历所有属性
        for (Field field : fields) {
            // 判断是否被 RpcReference 注解标注
            if (field.isAnnotationPresent(RpcReference.class)) {
                // 获得 RpcReference 注解
                RpcReference rpcReference = field.getAnnotation(RpcReference.class);
                // 默认类为属性当前类型
                // filed.class = java.lang.reflect.Field
                // filed.type = com.wxy.xxx.service.XxxService
                Class<?> clazz = field.getType();
                try {
                    // 如果指定了全限定类型接口名
                    if (!"".equals(rpcReference.interfaceName())) {
                        clazz = Class.forName(rpcReference.interfaceName());
                    }
                    // 如果指定了接口类型
                    if (rpcReference.interfaceClass() != void.class) {
                        clazz = rpcReference.interfaceClass();
                    }
                    // 获取指定类型的代理对象
                    Object proxy = proxyFactory.getProxy(clazz, rpcReference.version());
                    // 关闭安全检查
                    field.setAccessible(true);
                    // 设置域的值为代理对象
                    field.set(bean, proxy);
                } catch (ClassNotFoundException | IllegalAccessException e) {
                    throw new RpcException(String.format("Failed to obtain proxy object, the type of field %s is %s, " +
                            "and the specified loaded proxy type is %s.", field.getName(), field.getClass(), clazz), e);
                }
            }
        }
        return bean;
    }
}
```

### 集成 SpringBoot 完成自动配置

实现 `rpc-client` 和 `rpc-server` 的 `starter` 模块，编写对应的自动配置的配置类以及 `spring.factories` 文件，引入对应的`starter` 即可完成自动配置功能。

### 增加 Netty 心跳机制

解决了每次请求客户端都要重新与服务端建立 netty 连接，非常耗时，增加心跳检查机制，保持长连接，复用 channel 连接；

- 长连接：避免了每次调用新建TCP连接，提高了调用的响应速度；
- Channel 连接复用：避免重复连接服务端；
- 多路复用：单个TCP连接可交替传输多个请求和响应的消息，降低了连接的等待闲置时间，从而减少了同样并发数下的网络连接数，提高了系统吞吐量。

具体实现代码在

 `com.wxy.rpc.client.transport.netty.NettyRpcClient`，`com.wxy.rpc.client.transport.netty.ChannelProvider` 和   `com.wxy.rpc.server.transport.netty.NettyRpcRequestHandler`三个类中。

### 增加 Zookeeper 服务本地缓存并监听

解决了每次请求都需要访问 zk 来进行服务发现，可以添加本地服务缓存功能，然后监听 zk 服务节点的变化来动态更新本地服务列表。

服务本地缓存并监听的核心代码如下：

```java
public class ZookeeperServiceDiscovery implements ServiceDiscovery {
    
    // ....
    
    @Override
    public List<ServiceInfo> getServices(String serviceName) throws Exception {
        if (!serviceMap.containsKey(serviceName)) {
            // 构建本地服务缓存
            ServiceCache<ServiceInfo> serviceCache = serviceDiscovery.serviceCacheBuilder()
                    .name(serviceName)
                    .build();
            // 添加服务监听，当服务发生变化时主动更新本地缓存并通知
            serviceCache.addListener(new ServiceCacheListener() {
                @Override
                public void cacheChanged() {
                    log.info("The service [{}] cache has changed. The current number of service samples is {}."
                            , serviceName, serviceCache.getInstances().size());
                    // 更新本地缓存的服务列表
                    serviceMap.put(serviceName, serviceCache.getInstances().stream()
                            .map(ServiceInstance::getPayload)
                            .collect(Collectors.toList()));
                }

                @Override
                public void stateChanged(CuratorFramework client, ConnectionState newState) {
                    // 当连接状态发生改变时，只打印提示信息，保留本地缓存的服务列表
                    log.info("The client {} connection status has changed. The current status is: {}."
                            , client, newState);
                }
            });
            // 开启服务缓存监听
            serviceCache.start();
            // 将服务缓存对象存入本地
            serviceCacheMap.put(serviceName, serviceCache);
            // 将服务列表缓存到本地
            serviceMap.put(serviceName, serviceCacheMap.get(serviceName).getInstances()
                    .stream()
                    .map(ServiceInstance::getPayload)
                    .collect(Collectors.toList()));
        }
        return serviceMap.get(serviceName);
    }
    
    // ....
    
}
```



### 实现了 SPI 机制

已实现，参考Dubbo部分源码，实现了自定义的SPI机制，目前仅支持根据接口类型加载配置文件中的所有具体的扩展实现类，并且可以根据指定的key获取特定的实现类，具体实现类逻辑在 `com.wxy.rpc.core.extension.ExtensionLoader` 中。

服务存储目录在 `resource/META-INF/extensions`

<img src="images\spi服务目录.png" alt="image-20230222120620124" style="zoom: 67%;" />

文件内容格式如下：

```config
protostuff=com.wxy.rpc.core.serialization.protostuff.ProtostuffSerialization
kryo=com.wxy.rpc.core.serialization.kryo.KryoSerialization
json=com.wxy.rpc.core.serialization.json.JsonSerialization
jdk=com.wxy.rpc.core.serialization.jdk.JdkSerialization
hessian=com.wxy.rpc.core.serialization.hessian.HessianSerialization
```

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
- 测试：浏览器输入 http://localhost:8080/hello/zhangsan ，成功返回：`hello, zhangsan`，rpc 调用成功。
- 调用接口 100 次耗时 26ms，调用 10_0000 次耗时 25164 ms。

## 压力测试

**[JMH](https://zhuanlan.zhihu.com/p/434083702)**

`JMH`即`Java Microbenchmark Harness`，是`Java`用来做基准测试的一个工具，该工具由`OpenJDK`提供并维护，测试结果可信度高。

相对于 Jmeter、ab ，它通过编写代码的方式进行压测，在特定场景下会更能评估某项性能。

本次通过使用 JMH 来压测 RPC 的性能（官方也是使用JMH压测）

启动 10000 个线程同时访问 sayHello 接口，总共进行 3 轮测试，测试结果如下：

```
Benchmark                                          Mode     Cnt      Score       Error  Units
BenchmarkTest.testSayHello                        thrpt       3  29288.573 ± 20780.318  ops/s
BenchmarkTest.testSayHello                         avgt       3      0.532 ±     6.159   s/op
BenchmarkTest.testSayHello                       sample  395972      0.382 ±     0.002   s/op
BenchmarkTest.testSayHello:testSayHello·p0.00    sample              0.003               s/op
BenchmarkTest.testSayHello:testSayHello·p0.50    sample              0.318               s/op
BenchmarkTest.testSayHello:testSayHello·p0.90    sample              0.387               s/op
BenchmarkTest.testSayHello:testSayHello·p0.95    sample              0.840               s/op
BenchmarkTest.testSayHello:testSayHello·p0.99    sample              2.282               s/op
BenchmarkTest.testSayHello:testSayHello·p0.999   sample              2.470               s/op
BenchmarkTest.testSayHello:testSayHello·p0.9999  sample              2.496               s/op
BenchmarkTest.testSayHello:testSayHello·p1.00    sample              2.508               s/op
BenchmarkTest.testSayHello                           ss       3      0.118 ±     0.051   s/op
```

测试曲线图：

<img src="images\rpc10000并发测试结果.png">

同时，在同样的条件下，启动 5000（1w个电脑会卡死） 个线程同时对 **Dubbo2.7.14** 发起 RPC 调用，得到的结果如下：

```
Benchmark                                       Mode     Cnt      Score      Error  Units
StressTest.testSayHello                        thrpt       3  41549.866 ± 9703.455  ops/s
StressTest.testSayHello                         avgt       3      0.119 ±    0.034   s/op
StressTest.testSayHello                       sample  611821      0.123 ±    0.001   s/op
StressTest.testSayHello:testSayHello·p0.00    sample              0.042              s/op
StressTest.testSayHello:testSayHello·p0.50    sample              0.119              s/op
StressTest.testSayHello:testSayHello·p0.90    sample              0.129              s/op
StressTest.testSayHello:testSayHello·p0.95    sample              0.139              s/op
StressTest.testSayHello:testSayHello·p0.99    sample              0.195              s/op
StressTest.testSayHello:testSayHello·p0.999   sample              0.446              s/op
StressTest.testSayHello:testSayHello·p0.9999  sample              0.455              s/op
StressTest.testSayHello:testSayHello·p1.00    sample              0.456              s/op
StressTest.testSayHello                           ss       3      0.058 ±    0.135   s/op
```

<img src="images\dubbo5000并发测试结果.png">

**结果**：

|            | RPC     | RPC   | Dubbo2.7.14 |
| ---------- | ------- | ----- | ----------- |
| 并发数     | 10000   | 5000  | 5000        |
| TPS        | 29288   | 31675 | 41549       |
| RTT        | 95% 8ms | xxx   | 95% 50ms    |
| AVGTime/OP | 0.532   | 0.532 | 0.119       |
| OOM        | 无      | 无    | 无          |

对比了 jmeter、Apache-Benmark（ab）、jmh 这三个压测工具，个人比较推荐使用jmh，原因有：

- jmh压测简单，只需要引入依赖，声明注解
- 准确性高，目前大多数性能压测都是使用jmh
- 缺点就是代码入侵