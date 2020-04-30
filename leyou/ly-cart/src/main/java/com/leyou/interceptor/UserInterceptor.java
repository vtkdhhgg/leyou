package com.leyou.interceptor;


import com.leyou.auth.entity.UserInfo;
import com.leyou.auth.utils.JwtUtils;
import com.leyou.common.utils.CookieUtils;
import com.leyou.config.JwtProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Slf4j
public class UserInterceptor implements HandlerInterceptor {

    private JwtProperties prop;

    // threadLocal是一个map结构，key是thread，value是存储的值
    private static final ThreadLocal<UserInfo> t1 = new ThreadLocal<>();

    public UserInterceptor(JwtProperties prop) {
        this.prop = prop;
    }

    /**
     * 在前置拦截器中，从cookie中获取用户信息，并存储
     * @param request
     * @param response
     * @param handler
     * @return
     */
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler){

        try {
            //获取cookie中的token
            String token = CookieUtils.getCookieValue(request, prop.getCookieName());
            //解析token
            UserInfo user = JwtUtils.getInfoFromToken(token, prop.getPublicKey());

            //保存user
            t1.set(user);

            //放行
            return true;
        } catch (Exception e) {
            log.error("[购物车异常] 用户身份解析失败！", e);
            return false;
        }
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        //用完之后要删除，否则数据会越来越多
        t1.remove();
    }

    public static UserInfo getUser(){
         return t1.get();
    }
}
