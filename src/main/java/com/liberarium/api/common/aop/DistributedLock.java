package com.liberarium.api.common.aop;

import com.liberarium.api.common.cache.CacheType;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.concurrent.TimeUnit;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface DistributedLock {
  String key();
  TimeUnit timeUnit() default TimeUnit.MILLISECONDS;   // 시간 단위
  long waitTime() default 100L;                   // 락 대기시간
  long releaseTime() default 3000L;               // 락 잡은 후 해제시간
  CacheType fullbackCacheType();                        // 복구 Redis 버킷
}
