package com.liberarium.api.common.aop;

import com.liberarium.api.common.exception.defined.BusinessException;
import com.liberarium.api.common.model.ErrorCode;
import com.liberarium.api.common.util.RedissonCacheUtil;
import java.lang.reflect.Method;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.redisson.api.RBucket;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;

@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class DistributedLockAop {
  private static final String REDISSON_LOCK_PREFIX = "lock:";
  private static final int RETRY_CNT = 3;

  private final RedissonCacheUtil redissonCacheUtil;
  private final AopForTransaction aopForTransaction;
  private final RedissonClient redisson;

  @Around("@annotation(com.liberarium.api.common.aop.DistributedLock)")
  public Object lock(final ProceedingJoinPoint joinPoint) throws Throwable {
    MethodSignature signature = (MethodSignature) joinPoint.getSignature();
    Method method = signature.getMethod();
    DistributedLock distributedLock = method.getAnnotation(DistributedLock.class);

    // 1) SpEL 평가된 "원본 키"와 "락 키"를 분리해서 관리
    String evaluatedKey = String.valueOf(ELParser.getMethodValue(
      signature.getParameterNames(),
      joinPoint.getArgs(),
      distributedLock.key()
    ));

    String key = REDISSON_LOCK_PREFIX + evaluatedKey;
    RLock rLock = redisson.getLock(key);

    try {
      boolean available = rLock.tryLock(distributedLock.waitTime(), distributedLock.releaseTime(), distributedLock.timeUnit());
      if (!available) {
        for (int i = 0; i < RETRY_CNT; i++) {
          Thread.sleep(100);
          RBucket<Object> cached = redissonCacheUtil.getBucket(distributedLock.fullbackCacheType(), evaluatedKey);
          if(cached != null && cached.get() != null) {
            return cached.get();
          }
        }
        throw new BusinessException(ErrorCode.LOCK_TRY_FAILED);
      } else {
        return aopForTransaction.proceed(joinPoint);
      }
    } catch (InterruptedException e) {
      throw new InterruptedException();
    } finally {
      try {
        rLock.unlock(); // 락 해제
      } catch (IllegalMonitorStateException e) {
        log.info("Redisson Lock이 이미 해제되었습니다. [ methodName : {}, key {} ]", method.getName(), key);
      }
    }
  }
}
