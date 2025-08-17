package com.liberarium.api.support;

@FunctionalInterface
public interface AopForTransaction {
  Object proceed(org.aspectj.lang.ProceedingJoinPoint pjp) throws Throwable;
}
