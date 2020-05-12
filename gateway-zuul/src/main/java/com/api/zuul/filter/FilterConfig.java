package com.api.zuul.filter;

import com.marcosbarbero.cloud.autoconfigure.zuul.ratelimit.config.properties.RateLimitProperties;
import com.netflix.zuul.ZuulFilter;
import com.netflix.zuul.context.RequestContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.netflix.zuul.filters.Route;
import org.springframework.cloud.netflix.zuul.filters.RouteLocator;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UrlPathHelper;
import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * Description:
 *
 * @author Mr.Kong
 * @date 2020-05-12 22:30
 */
@Component
public abstract class FilterConfig extends ZuulFilter {

    private static final Logger LOGGER = LoggerFactory.getLogger(FilterConfig.class) ;

    private  RateLimitProperties properties;

    private  RouteLocator routeLocator;

    private  UrlPathHelper urlPathHelper;

    @Override
    public String filterType() {
        return "pre";
    }

    @Override
    public int filterOrder() {
        return 0;
    }

    @Override
    public Object run() {
        RequestContext requestContext = RequestContext.getCurrentContext() ;
        try {
            doBizProcess(requestContext);
        } catch (Exception e){
            LOGGER.info("异常：{}",e.getMessage());
        }
        return null;
    }

    public void doBizProcess (RequestContext requestContext) throws Exception {
        HttpServletRequest request = requestContext.getRequest() ;
        String reqUri = request.getRequestURI() ;
        if (!reqUri.contains("getAuthorInfo")){
            requestContext.setSendZuulResponse(false);
            requestContext.setResponseStatusCode(401);
            requestContext.getResponse().getWriter().print("Path Is Error...");
        }
    }

    /**
     * 判断是否需要过滤
     * @return
     */
    @Override
    public boolean shouldFilter() {
        return properties.isEnabled() && !policy(route()).isEmpty();
    }

    Route route() {
        String requestURI = urlPathHelper.getPathWithinApplication(RequestContext.getCurrentContext().getRequest());
        return routeLocator.getMatchingRoute(requestURI);
    }

    protected List<RateLimitProperties.Policy> policy(final Route route) {
        if (route != null) {
            // 根据路由id来进行匹配
            return properties.getPolicies(route.getId());
        }
        return properties.getDefaultPolicyList();
    }
}
