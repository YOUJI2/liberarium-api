package com.liberarium.api.common.util;

import com.liberarium.api.common.cache.CacheType;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RedissonCacheUtil {

  private final RedissonClient redissonClient;

  public <T> RBucket<T> getBucket(CacheType cacheType, String cacheKey) {
    String redisKey = cacheType.getName() + "::" + cacheKey;
    return redissonClient.getBucket(redisKey);
  }

  public <T> void setWithTTL(CacheType cacheType, String cacheKey, T value) {
    RBucket<T> bucket = getBucket(cacheType, cacheKey);
    bucket.set(value, cacheType.getTtlMs(), TimeUnit.MILLISECONDS);
  }

  public <T> T get(CacheType cacheType, String cacheKey) {
    return (T) getBucket(cacheType, cacheKey).get();
  }
}
