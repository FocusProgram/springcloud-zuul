<font size=4.5>

**zuul**

---

- **文章目录**

* [1\. 什么是zuul?](#1-%E4%BB%80%E4%B9%88%E6%98%AFzuul)
* [2\. zuul作用](#2-zuul%E4%BD%9C%E7%94%A8)
* [3\. 架构设计原理](#3-%E6%9E%B6%E6%9E%84%E8%AE%BE%E8%AE%A1%E5%8E%9F%E7%90%86)
* [4\. Filters 过滤器](#4-filters-%E8%BF%87%E6%BB%A4%E5%99%A8)
* [5\. springcloud集成zuul](#5-springcloud%E9%9B%86%E6%88%90zuul)

# 1. 什么是zuul?

> [zuul](https://github.com/Netflix/zuul) 是从设备和网站到 Netflix 流媒体应用程序后端的所有请求的前门。 作为一个边缘服务应用程序，构建 Zuul 是为了支持动态路由、监视、弹性和安全性。 它还可以根据需要将请求路由到多个 Amazon 自动缩放组。Zuul是Spring Cloud全家桶中的微服务API网关。
所有从设备或网站来的请求都会经过Zuul到达后端的Netflix应用程序。作为一个边界性质的应用程序，Zuul提供了动态路由、监控、弹性负载和安全功能。

# 2. zuul作用

>
> - 认证和安全 识别每个需要认证的资源，拒绝不符合要求的请求。
> - 性能监测 在服务边界追踪并统计数据，提供精确的生产视图。
> - 动态路由 根据需要将请求动态路由到后端集群。
> - 压力测试 逐渐增加对集群的流量以了解其性能。
> - 负载卸载 预先为每种类型的请求分配容量，当请求超过容量时自动丢弃。
> - 静态资源处理 直接在边界返回某些响应。

# 3. 架构设计原理

> Zuul 2.0是一个 Netty 服务器，它运行前置过滤器(入站过滤器) ，然后使用 Netty 客户机代理请求，然后在运行后置过滤器(出站过滤器)后返回响应。

![](http://image.focusprogram.top/20200512220426.png)

# 4. Filters 过滤器

> 过滤器是 Zuul 业务逻辑的核心部分。 它们有能力执行非常大范围的操作，并且可以在请求-响应生命周期的不同部分运行
>
> - Inbound Filters, 入站过滤器在路由到原点之前执行，可以用于身份验证、路由和修饰请求
> - Endpoint Filters, 端点过滤器可用于返回静态响应，否则内置的 ProxyEndpoint 过滤器将把请求路由到原始端
> - Outbound Filters, 出站过滤器在从源获得响应之后执行，可用于度量、修饰对用户的响应或添加自定义头
> 
> 还有两种类型的过滤器: 同步和异步。 因为我们是在事件循环中运行，所以永远不要在过滤器中阻塞是至关重要的。 如果要阻塞，请在单独线程池中的异步过滤器中进行阻塞，否则可以使用同步过滤器。
>
> zuul的核心是一系列的filters, 其作用可以类比Servlet框架的Filter，或者AOP。
>
> zuul把Request route到 用户处理逻辑 的过程中，这些filter参与一些过滤处理，比如Authentication，Load Shedding等。 

![](http://image.focusprogram.top/20200513114110.png)

**Zuul提供了一个框架，可以对过滤器进行动态的加载，编译，运行。**

> Zuul的过滤器之间没有直接的相互通信，他们之间通过一个RequestContext的静态类来进行数据传递的。RequestContext类中有ThreadLocal变量来记录每个Request所需要传递的数据。
>
> Zuul的过滤器是由Groovy写成，这些过滤器文件被放在Zuul Server上的特定目录下面，Zuul会定期轮询这些目录，修改过的过滤器会动态的加载到Zuul Server中以便过滤请求使用。
> 
> 下面有几种标准的过滤器类型：
>
> - Zuul大部分功能都是通过过滤器来实现的。Zuul中定义了四种标准过滤器类型，这些过滤器类型对应于请求的典型生命周期。
>
> - (1) PRE：这种过滤器在请求被路由之前调用。我们可利用这种过滤器实现身份验证、在集群中选择请求的微服务、记录调试信息等。
>
> - (2) ROUTING：这种过滤器将请求路由到微服务。这种过滤器用于构建发送给微服务的请求，并使用Apache HttpClient或Netfilx Ribbon请求微服务。
>
> - (3) POST：这种过滤器在路由到微服务以后执行。这种过滤器可用来为响应添加标准的HTTP Header、收集统计信息和指标、将响应从微服务发送给客户端等。
>
> - (4) ERROR：在其他阶段发生错误时执行该过滤器。

**内置的特殊过滤器**

> zuul还提供了一类特殊的过滤器，分别为：StaticResponseFilter和SurgicalDebugFilter
>
> StaticResponseFilter：StaticResponseFilter允许从Zuul本身生成响应，而不是将请求转发到源。
>
> SurgicalDebugFilter：SurgicalDebugFilter允许将特定请求路由到分隔的调试集群或主机。

**自定义的过滤器**

> 除了默认的过滤器类型，Zuul还允许我们创建自定义的过滤器类型。
>
> 例如，我们可以定制一种STATIC类型的过滤器，直接在Zuul中生成响应，而不将请求转发到后端的微服务。

**过滤器的生命周期**

![](http://image.focusprogram.top/20200513114152.png)

# 5. springcloud集成zuul

> springcloud-eureka模块

引入maven依赖

```
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-netflix-eureka-client</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-netflix-eureka-server</artifactId>
</dependency>
```

添加配置文件application.yml

```
eureka:
  client:
    fetchRegistry: false #自己提供服务，所有设置为false
    registerWithEureka: false #不需要去检索服务，所有设置为false
server:
  port: 9000
spring:
  application:
    name: discovery-eureka
```

添加启动类EurekaApplication

```
@SpringBootApplication
@EnableEurekaServer
public class EurekaApplication {

    public static void main(String[] args) {
        SpringApplication.run(EurekaApplication.class, args);
    }

}
```

> springcloud-zuul模块

引入maven依赖

```
 <!-- zuul -->
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-zuul</artifactId>
    <version>${spring-cloud-zuul.version}</version>
</dependency>

<!-- eureka -->
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-netflix-eureka-client</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-netflix-eureka-server</artifactId>
</dependency>
```

添加配置文件application.yml

```
spring:
  application:
    name: gateway-zuul

eureka:
  client:
    fetchRegistry: true #注册到别的服务，所有设置为true
    registerWithEureka: true #需要去检索服务，所有设置为true
    service-url:
      defaultZone: http://localhost:9000/eureka #服务注册中心的配置内容，指定服务注册中心的位置
  #  instance:
  #    hostname: eureka-provider
  #    prefer-ip-address: true #以ip地址注册到服务中心（单节点部署为分布式Eureka集群，禁止设置为true）
  server:
    enable-self-preservation: false #是否开启自我保护模式
    eviction-interval-timer-in-ms: 60000 #服务注册表清理间隔（单位：毫秒，默认60*1000）

server:
  port: 8080

zuul:
  # 前缀，可以用来做版本控制
  prefix: /v1
  # 禁用默认路由，执行配置的路由
  ignored-services: "*"
  routes:
    # 指定路由名称
    order:
      # 指定路由访问路径
      path: /order/**
      # 指定向下游服务转发请求头中携带的信息，例如head:tx-token
      sensitiveHeaders:
      # 自定义路由指向服务id
      serviceId: order-service
    # 路由排除访问路径
    ignored-patterns:
      - /order/getFilter/*
  # 添加代理头
  add-proxy-headers: true
```

添加服务降级类FallBackConfig

```
@Component
public class FallBackConfig implements FallbackProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(FallBackConfig.class) ;

    @Override
    public ClientHttpResponse fallbackResponse(String route, Throwable cause) {
        // 捕获超时异常，返回自定义信息
        if (cause instanceof HystrixTimeoutException) {
            return response(HttpStatus.GATEWAY_TIMEOUT);
        } else {
            return fallbackResponse();
        }
    }

    private ClientHttpResponse response(final HttpStatus status) {
        return new ClientHttpResponse() {
            @Override
            public HttpStatus getStatusCode() {
                return status;
            }
            @Override
            public int getRawStatusCode() {
                return status.value();
            }
            @Override
            public String getStatusText() {
                return status.getReasonPhrase();
            }
            @Override
            public void close() {
                LOGGER.info("close");
            }
            @Override
            public InputStream getBody() {
                String message =
                        "{\n" +
                                "\"code\": 200,\n" +
                                "\"message\": \"服务暂时不可用\"\n" +
                                "}";
                return new ByteArrayInputStream(message.getBytes());
            }
            @Override
            public HttpHeaders getHeaders() {
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);
                return headers;
            }
        };
    }

    @Override
    public String getRoute() {
        return "*";
    }

    public ClientHttpResponse fallbackResponse() {
        return response(HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
```

过滤器拦截类FilterConfig

```
@Component
public abstract class FilterConfig extends ZuulFilter {

    /**
     * // 在进行Zuul过滤的时候可以设置其过滤执行的位置，那么此时有如下几种类型：
     * // 1、pre：在请求发出之前执行过滤，如果要进行访问，肯定在请求前设置头信息
     * // 2、route：在进行路由请求的时候被调用；
     * // 3、post：在路由之后发送请求信息的时候被调用；
     * // 4、error：出现错误之后进行调用
     * @return
     */
    @Override
    public String filterType() {
        return "pre";
    }

    /**
     *  设置优先级，数字越大优先级越低
     * @return
     */
    @Override
    public int filterOrder() {
        return 0;
    }

    /**
     * 判断是否需要过滤
     * @return
     */
    @Override
    public boolean shouldFilter() {
        return true;
    }

    /**
     * 执行具体的过滤操作
     * @return
     */
    @Override
    public Object run() {
        // 获取当前请求的上下文
        RequestContext currentContext = RequestContext.getCurrentContext() ;
        // 认证的原始信息
        String auth = "zulladmin";
        // 进行一个加密的处理
        byte[] encodedAuth = Base64.getEncoder()
                .encode(auth.getBytes(Charset.forName("US-ASCII")));
        // 在进行授权的头信息内容配置的时候加密的信息一定要与“Basic”之间有一个空格
        String authHeader = "Basic " + new String(encodedAuth);
        currentContext.addZuulRequestHeader("Authorization", authHeader);
        return null;
    }
}
```

添加启动类ZuulApplication

```
@SpringBootApplication
@EnableZuulProxy
@EnableDiscoveryClient
@EnableEurekaClient
public class ZuulApplication {

    public static void main(String[] args) {
        SpringApplication.run(ZuulApplication.class, args);
    }

}
```

> springcloud-order模块

添加maven依赖

```
<!-- web -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>
</dependency>

<!-- openfegin -->
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-openfeign</artifactId>
</dependency>

<!-- eureka -->
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-netflix-eureka-client</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-netflix-eureka-server</artifactId>
</dependency>
```

添加配置文件application.yml

```
spring:
  application:
    name: order-service
  redis:
    host: 114.55.34.44
    password: root

eureka:
  client:
    fetchRegistry: true #注册到别的服务，所有设置为true
    registerWithEureka: true #需要去检索服务，所有设置为true
    service-url:
      defaultZone: http://localhost:9000/eureka #服务注册中心的配置内容，指定服务注册中心的位置
  #  instance:
  #    hostname: eureka-provider
  #    prefer-ip-address: true #以ip地址注册到服务中心（单节点部署为分布式Eureka集群，禁止设置为true）
  server:
    enable-self-preservation: false #是否开启自我保护模式
    eviction-interval-timer-in-ms: 60000 #服务注册表清理间隔（单位：毫秒，默认60*1000）

server:
  port: 8081

```

添加OrderController

```
@RestController
@RequestMapping("order")
public class OrderController {

    @GetMapping("/getOrder")
    public String getOrder(){
        return "order";
    }

    @GetMapping("getFilter")
    public String getFilter(){
        return "filter";
    }

}
```

添加启动类OrderServiceApplication

```
@SpringBootApplication
@EnableDiscoveryClient
@EnableEurekaClient
public class OrderServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(OrderServiceApplication.class, args);
    }

}
```

> 测试

```
curl http://localhost:8080/v1/order/order/getOrder
```

![](http://image.focusprogram.top/20200512234529.png)

终止order-service服务，再次访问，显示如下，说明服务降级成功

![](http://image.focusprogram.top/20200512234926.png)




