<font size=4.5>

**zuul**

---

- **文章目录**

[toc]

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
    fetchRegistry: false 
    registerWithEureka: false
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
    fetchRegistry: true
    registerWithEureka: true 
    service-url:
  server:
    enable-self-preservation: false 
    eviction-interval-timer-in-ms: 60000 

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

<!--        eureka-->
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-netflix-eureka-client</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-netflix-eureka-server</artifactId>
</dependency>
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




