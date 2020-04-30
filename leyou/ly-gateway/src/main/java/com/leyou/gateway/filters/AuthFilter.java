package com.leyou.gateway.filters;

import com.leyou.auth.entity.UserInfo;
import com.leyou.auth.utils.JwtUtils;
import com.leyou.common.utils.CookieUtils;
import com.leyou.gateway.config.AllowFilterProperties;
import com.leyou.gateway.config.JwtProperties;
import com.netflix.zuul.ZuulFilter;
import com.netflix.zuul.context.RequestContext;
import com.netflix.zuul.exception.ZuulException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.netflix.zuul.filters.support.FilterConstants;
import org.springframework.stereotype.Component;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import java.util.List;

@Component
@EnableConfigurationProperties({JwtProperties.class, AllowFilterProperties.class})
public class AuthFilter extends ZuulFilter {

    @Autowired
    private JwtProperties prop;

    @Autowired
    private AllowFilterProperties filterProp;

    @Override
    public String filterType() {
        return FilterConstants.PRE_TYPE;    //过滤器类型：前置
    }

    @Override
    public int filterOrder() {
        return FilterConstants.PRE_DECORATION_FILTER_ORDER - 1; //配置过滤器顺序
    }

    //是否过滤
    @Override
    public boolean shouldFilter() {
        //获取请求上下文
        RequestContext ctx = RequestContext.getCurrentContext();
        //获取request
        HttpServletRequest request = ctx.getRequest();
        //获取请求url
        String path = request.getRequestURI();
        //将请求路径和白名单进行判断
        return !isAllowPath(path);
    }

    private boolean isAllowPath(String path) {
        //定义一个标记
        boolean flag = false;
        //获取路径白名单
        List<String> allowPaths = this.filterProp.getAllowPaths();
        //遍历允许访问的路径
        for (String allowPath : allowPaths) {
            //判断请求路径是否符合白名单
            if (path.startsWith(allowPath)) {
                flag = true;
                break;
            }
        }
        return flag;
    }


    @Override
    public Object run() throws ZuulException {
        //获取请求上下文
        RequestContext ctx = RequestContext.getCurrentContext();
        //获取request
        HttpServletRequest request = ctx.getRequest();
        //获取cookie
        Cookie[] cookies = request.getCookies();
        //获取token
        String token = CookieUtils.getCookieValue(request, prop.getCookieName());
        try {
            // 解析token
            UserInfo user = JwtUtils.getInfoFromToken(token, prop.getPublicKey());
            // TODO 校验权限
        }catch (Exception e){
            // 解析token失败，未登录，进行拦截操作
            ctx.setSendZuulResponse(false);
            // 返回状态码 未授权
            ctx.setResponseStatusCode(403);
        }
        return null;
    }
}
