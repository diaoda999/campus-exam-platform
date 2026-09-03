package com.campus.exam.admin.aop;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

/**
 * 接口耗时与异常日志切面，慢接口阈值 500ms
 */
@Slf4j
@Aspect
@Component
public class WebLogAspect {

    private static final long SLOW_THRESHOLD_MS = 500L;

    @Around("execution(public * com.campus.exam.admin.controller..*.*(..))")
    public Object around(ProceedingJoinPoint pjp) throws Throwable {
        long start = System.currentTimeMillis();
        String method = pjp.getSignature().toShortString();
        try {
            Object result = pjp.proceed();
            long cost = System.currentTimeMillis() - start;
            if (cost > SLOW_THRESHOLD_MS) {
                log.warn("[SLOW] {} 耗时 {}ms", method, cost);
            } else {
                log.info("[API] {} 耗时 {}ms", method, cost);
            }
            return result;
        } catch (Throwable t) {
            log.error("[ERR] {} 异常: {}", method, t.getMessage());
            throw t;
        }
    }
}
