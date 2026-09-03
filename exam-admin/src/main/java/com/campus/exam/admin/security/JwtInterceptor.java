package com.campus.exam.admin.security;

import com.campus.exam.common.context.LoginUser;
import com.campus.exam.common.context.UserContext;
import com.campus.exam.common.exception.BizException;
import com.campus.exam.common.result.ResultCode;
import com.campus.exam.common.utils.JwtUtils;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 登录态 JWT 拦截器：解析长 Token，写入 UserContext。
 * 考试接口的“一次性考试 Token”双重校验在 ExamService 内完成。
 */
@Component
@RequiredArgsConstructor
public class JwtInterceptor implements HandlerInterceptor {

    @Value("${exam.jwt.secret:" + JwtUtils.DEFAULT_SECRET + "}")
    private String jwtSecret;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // 预检请求直接放行
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }
        String authorization = request.getHeader("Authorization");
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            throw new BizException(ResultCode.UNAUTHORIZED);
        }
        try {
            Claims claims = JwtUtils.parse(authorization.substring(7), jwtSecret);
            LoginUser user = LoginUser.builder()
                    .userId(JwtUtils.getUserId(claims))
                    .username(claims.get("username", String.class))
                    .role(claims.get("role", String.class))
                    .build();
            UserContext.set(user);
            return true;
        } catch (Exception e) {
            throw new BizException(ResultCode.UNAUTHORIZED);
        }
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler, Exception ex) {
        UserContext.clear();
    }
}
