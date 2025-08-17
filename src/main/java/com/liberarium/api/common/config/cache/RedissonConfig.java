package com.liberarium.api.common.config.cache;

import lombok.RequiredArgsConstructor;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class RedissonConfig {

  @Value("${spring.data.redis.host}")
  String host;

  @Value("${spring.data.redis.port}")
  int port;

  @Bean
  public RedissonClient redissonClient() {
    Config config = new Config();
    config.useSingleServer()
      .setAddress("redis://" + host + ":" + port)
      .setConnectionMinimumIdleSize(8)
      .setConnectionPoolSize(16);

    return Redisson.create(config);
  }
}
