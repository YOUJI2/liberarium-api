package com.liberarium.api.domain.service;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.liberarium.api.common.cache.CacheType;
import com.liberarium.api.common.util.RedissonCacheUtil;
import com.liberarium.api.domain.dto.BookDetailDto;
import com.liberarium.api.domain.repository.BookRepository;
import com.liberarium.api.support.dto.BookTestData;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.redisson.api.RBucket;
import org.junit.jupiter.api.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Tag("integration")
@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
class BookServiceConcurrencyTest {

  @Autowired
  private RedissonCacheUtil redissonCacheUtil;

  @SpyBean
  private BookRepository bookRepository;

  @SpyBean
  private BookService bookService;

  private final String BOOK_TEST_ID = BookTestData.ISBN;

  @Container
  static final GenericContainer<?> redisContainer =
    new GenericContainer<>("redis:7-alpine").withExposedPorts(6379);

  @DynamicPropertySource
  static void redisProperties(DynamicPropertyRegistry registry) {
    registry.add("spring.data.redis.host", redisContainer::getHost);
    registry.add("spring.data.redis.port", () -> redisContainer.getMappedPort(6379));
  }

  @BeforeEach
  void setUp() {
    RBucket<BookDetailDto> rBucket = redissonCacheUtil.getBucket(CacheType.BOOK_DETAIL, BOOK_TEST_ID);
    if(rBucket.get() != null) {
      rBucket.delete();
    }
  }

  @Test
  void 캐시는_1회만_미스되고_나머지는_히트된다() throws Exception {
    int n = 10;
    ExecutorService executor = Executors.newFixedThreadPool(n);
    CountDownLatch ready = new CountDownLatch(n);
    CountDownLatch start = new CountDownLatch(1);

    List<Future<BookDetailDto>> futures = new ArrayList<>();

    for (int i = 0; i < n; i++) {
      futures.add(executor.submit(() -> {
        try {
          ready.countDown();                                        // 준비 완료
          if (!start.await(5, TimeUnit.SECONDS)) {          // 타임아웃
            throw new IllegalStateException("start 신호 대기 타임아웃");
          }
          return bookService.findBookDetailInfo(BOOK_TEST_ID);
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
          throw e;
        }
      }));
    }

    // 모든 작업이 대기 위치까지 도달했는지 확인
    if (!ready.await(5, TimeUnit.SECONDS)) {
      throw new IllegalStateException("일부 작업이 준비되지 않았습니다");
    }

    start.countDown();

    for (Future<BookDetailDto> f : futures) {
      assertNotNull(f.get(10, TimeUnit.SECONDS));
    }

    RBucket<BookDetailDto> rBucket =
        redissonCacheUtil.getBucket(CacheType.BOOK_DETAIL, BOOK_TEST_ID);
    Assertions.assertTrue(rBucket.get() != null);

    verify(bookRepository, times(1)).findBookDetailInfoByIsbn(BOOK_TEST_ID);
    executor.shutdownNow();
  }
}
