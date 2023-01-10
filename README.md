## WXY-RPC
### 介绍

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

### 项目结构介绍

<img src="images\项目架构图.png" alt="项目架构图" style="zoom:67%;" />

consumer模块：服务的消费者，依赖于 rpc-client-spring-boot-starter 模块；

provider-api模块：服务提供者暴露的API；

provider模块：服务的提供者，依赖于 rpc-server-spring-boot-starter 模块：

rpc-client-spring-boot模块：rpc 客户端模块，封装客户端发起的请求过程，提供服务发现、动态代理，网络通信等功能；

rpc-client-spring-boot-stater模块：是rpc-client-spring-boot的stater模块，负责引入相应依赖进行自动配置；

rpc-framework-core模块：是rpc核心依赖，提供负载均衡、服务注册发现、消息协议、消息编码解码、序列化算法；

rpc-server-spring-boot模块：rpc 服务端模块，负责启动服务，接受和处理RPC请求，提供服务发布、反射调用等功能；

rpc-server-spring-boot-stater模块：是rpc-server-spring-boot的stater模块，负责引入相应依赖进行自动配置；

### 运行项目

1、首先需要安装并启动 zookeeper；

2、修改 Consumer 和 Provider 模块下的 application.yml 的注册中心地址属性，即 rpc.client.registry-addr=你的zk连接地址，服务端则配置 rpc.server.registry-addr属性；

3、先启动 Provider 模块，正常启动 SpringBoot 项目即可，本项目使用基于 SpringBoot 的自动配置，运行后会自动向 SpringIOC 容器中创建需要的 Bean 对象。

4、然后启动 Consumer 模块，通过 Controller 去访问服务进行 rpc 调用了。

#### 项目实现的主要组件

#### 自定义消息协议，编解码

自定义协议的要数：

* 魔数，用来在第一时间判定是否是无效数据包
* 版本号，可以支持协议的升级
* 序列化算法，消息正文到底采用哪种序列化反序列化方式，可以由此扩展，例如：json、protobuf、hessian、jdk
* 指令类型，是登录、注册、单聊、群聊... 跟业务相关
* 请求序号，为了双工通信，提供异步能力
* 正文长度
* 消息正文

魔数的作用：**快速** 识别[字节流](https://so.csdn.net/so/search?q=字节流&spm=1001.2101.3001.7020)是否是程序能够处理的，能处理才进行后面的 **耗时** 业务操作，如果不能处理，尽快执行失败，断开连接等操作。