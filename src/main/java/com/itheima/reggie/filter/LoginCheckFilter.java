package com.itheima.reggie.filter;

import com.alibaba.fastjson.JSONObject;
import com.itheima.reggie.common.BaseContext;
import com.itheima.reggie.common.R;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.AntPathMatcher;

import javax.servlet.*;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * 与Login相关的自定义过滤器
 * fileName是过滤器的名称，为了便于理解通常与class名称相同；
 * urlPatterns则指定过滤器将拦截哪些请求的 URL 模式，如果URL中有匹配的部分，则会被过滤器处理。
 */
@Slf4j
@WebFilter(filterName = "LoginCheckFilter", urlPatterns = "/*")
public class LoginCheckFilter implements Filter {

    // AntPathMatcher是Spring Framework 提供的一个用于路径匹配的工具类。它支持使用 Ant 风格的路径模式进行字符串匹配，类似于 Ant 中用于匹配文件路径的模式匹配规则。
    public static final AntPathMatcher PATH_MATCHER = new AntPathMatcher();

    String[] URLs = new String[]{
//            放行静态资源
            "/backend/**",
            "/front/**",
//            放行login与logout api，因为能够执行login和logout就说明登陆是成功的，不必过滤
            "/employee/login",
            "/employee/logout",
            // 测试用
            "/common/**",
            "/user/login",
            "/user/sendMsg"
    };

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        // 1. 获取本次请求的url
        HttpServletRequest request = (HttpServletRequest) servletRequest;
        HttpServletResponse response = (HttpServletResponse) servletResponse;

        String url = request.getRequestURI().toString();

        log.info("拦截到请求{}", url);

        // 2. 判断本次请求是否需要处理
        boolean IsPass = checkURL(url);
        // 如果不需要处理，则直接放行
        if(IsPass) {
            log.info("{}不需要处理，放行", url);
            filterChain.doFilter(request, response);
            return;
        }

        // 3. 判断是否已经登陆
        // 如果已经登陆，则直接放行
        if(request.getSession().getAttribute("employee") != null){
            log.info("员工已登陆，放行");
            // 如果已经登陆，则将id加入到ThreadLocal中
            BaseContext.setId((Long)request.getSession().getAttribute("employee"));
            filterChain.doFilter(request, response);
            return;
        }

        // TODO: 这里逻辑其实有问题，只要员工或者用户登陆，则所有员工和用户都可以访问到所有页面，或许可以考虑用拦截器，对员工和用户单独拦截？
        if(request.getSession().getAttribute("user") != null){
            log.info("用户已登陆，放行");
            BaseContext.setId((Long)request.getSession().getAttribute("user"));
            filterChain.doFilter(request, response);
            return;
        }

        // 如果未登陆，则处理
        log.info("需要过滤请求:{}", url);
        response.getWriter().write(JSONObject.toJSONString(R.error("NOTLOGIN")));
    }

    public boolean checkURL(String url) {
        for(String pattern : URLs) {
            boolean match = PATH_MATCHER.match(pattern, url);
            if(match) {
                return true;
            }
        }
        return false;
    }
}
