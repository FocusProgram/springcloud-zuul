package com.api.zuul.filter;


import com.netflix.zuul.ZuulFilter;
import com.netflix.zuul.context.RequestContext;
import org.springframework.stereotype.Component;
import java.nio.charset.Charset;
import java.util.Base64;


/**
 * Description: 过滤器
 *
 * @author Mr.Kong
 * @date 2020-05-12 22:30
 */
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
