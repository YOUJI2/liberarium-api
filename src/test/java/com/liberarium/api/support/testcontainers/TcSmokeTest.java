package com.liberarium.api.support.testcontainers;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
class TcSmokeTest {

  @Container
  static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0")
    .withDatabaseName("bookdb")
    .withUsername("test")
    .withPassword("test");

  @DynamicPropertySource
  static void props(DynamicPropertyRegistry r) {
    r.add("spring.datasource.url", mysql::getJdbcUrl);
    r.add("spring.datasource.username", mysql::getUsername);
    r.add("spring.datasource.password", mysql::getPassword);
    r.add("spring.datasource.driver-class-name", mysql::getDriverClassName);
    r.add("spring.jpa.hibernate.ddl-auto", () -> "none"); // 필요에 따라 validate/update
  }

  @Container
  static final GenericContainer<?> REDIS =
    new GenericContainer<>("redis:7-alpine").withExposedPorts(6379);

  @Test
  void REDIS_컨테이너_테스트() {
    assertThat(REDIS.getHost()).isNotBlank();
    assertThat(REDIS.getMappedPort(6379)).isPositive();
  }
}
