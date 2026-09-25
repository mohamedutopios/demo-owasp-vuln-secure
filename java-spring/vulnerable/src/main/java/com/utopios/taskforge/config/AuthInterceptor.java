package com.utopios.taskforge.config;

import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Component
public class AuthInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest req, HttpServletResponse resp, Object handler) {
        try {
            Object user = req.getSession().getAttribute("user");
            if (user == null) {
                resp.sendRedirect("/login");
                return false;
            }
            return true;
        } catch (Exception e) {
            return true;
        }
    }
}
