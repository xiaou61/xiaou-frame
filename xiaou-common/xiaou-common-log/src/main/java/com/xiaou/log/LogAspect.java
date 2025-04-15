package com.xiaou.log;

import com.google.gson.Gson;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

/**
 * 日志切面
 */
@Aspect
@Slf4j
@Component
public class LogAspect {


    public LogAspect() {
        log.info("✅ LogAspect 已加载");
    }


    @Pointcut("execution(* com.xiaou.*.controller.*Controller.*(..)) || execution(* com.xiaou.*.service.*Service.*(..))")
    private void pointCut() {
    }

    @Around("pointCut()")
    public Object around(ProceedingJoinPoint proceedingJoinPoint) throws Throwable {
        Object[] args = proceedingJoinPoint.getArgs();
        String req = new Gson().toJson(args);
        MethodSignature methodSignature = (MethodSignature) proceedingJoinPoint.getSignature();
        String methodName = methodSignature.getDeclaringType().getName() + "." + methodSignature.getName();
        log.info("请求方法：{}，请求参数：{}", methodName, req);
        Long startTime = System.currentTimeMillis();
        Object respobj = proceedingJoinPoint.proceed();
        String resp = new Gson().toJson(respobj);
        Long endTime = System.currentTimeMillis();
        log.info("响应参数：{}，耗时：{}ms", resp, endTime - startTime);
        return respobj;
    }
}
