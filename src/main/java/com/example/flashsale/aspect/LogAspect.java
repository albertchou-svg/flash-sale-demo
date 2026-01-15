package com.example.flashsale.aspect;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

@Aspect // 宣告這是一個切面
@Component // 交給 Spring 管理
@Slf4j // Lombok 提供 log 物件
public class LogAspect {

    /**
     * @Around: 環繞通知，包圍目標方法。
     * execution(* com.example.flashsale.controller..*(..)):
     * 攔截 controller 包底下所有的 Class 的所有方法
     */
    @Around("execution(* com.example.flashsale.controller..*(..))")
    public Object logExecutionTime(ProceedingJoinPoint joinPoint) throws Throwable {
        long start = System.currentTimeMillis();

        // 1. 執行原本的程式碼 (Controller 的方法)
        Object proceed = joinPoint.proceed();

        // 2. 計算耗時
        long executionTime = System.currentTimeMillis() - start;

        // 3. 印出 Log
        log.info("👉 [API 監控] 方法: {} | 耗時: {} ms", joinPoint.getSignature(), executionTime);

        return proceed;
    }
}