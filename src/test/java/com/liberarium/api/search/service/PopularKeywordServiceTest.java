package com.liberarium.api.search.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.liberarium.api.search.dto.PopularKeywordDto;
import java.util.List;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.DisabledIfEnvironmentVariable;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@Testcontainers
@DisabledIfEnvironmentVariable(named = "CI", matches = "true")
class PopularKeywordServiceTest {

  private static final DockerImageName REDIS_IMAGE = DockerImageName.parse("redis:7-alpine");
  private static final int REDIS_PORT = 6379;
  private static final String ZSET_KEY = "popular:keywords";

  @Container
  static final GenericContainer<?> REDIS =
    new GenericContainer<>(REDIS_IMAGE)
      .withExposedPorts(REDIS_PORT)
      .waitingFor(Wait.forListeningPort());

  private static RedissonClient redisson;
  private static PopularKeywordService service;

  @BeforeAll
  static void setUpClient() {
    String addr = "redis://" + REDIS.getHost() + ":" + REDIS.getMappedPort(REDIS_PORT);
    Config cfg = new Config();
    cfg.useSingleServer().setAddress(addr);
    redisson = Redisson.create(cfg);

    service = new PopularKeywordService(redisson);
  }

  @AfterEach
  void cleanZset() {
    redisson.getKeys().delete(ZSET_KEY); // 매번 비우기
  }

  @AfterAll
  static void tearDown() {
    if (redisson != null) redisson.shutdown();
  }

  @Test
  void 현재_인기검색어_데이터_비어있을시_빈리스트_반환() {
    List<PopularKeywordDto> top = service.popularTopN(10);
    assertThat(top).isEmpty();
  }

  @Test
  @DisplayName("recordSearch: 단일/복합/NOT 쿼리 반영 및 상위 N 정렬 검증")
  void 키워드_반영_후_상위_N개_검증() {
    // given
    service.recordSearch("spring");
    service.recordSearch("spring");
    service.recordSearch("nosql|java");
    service.recordSearch("java-Youji");
    service.recordSearch("  a  ");

    // when
    List<PopularKeywordDto> top = service.popularTopN(10);

    // then
    assertThat(top).hasSize(3);

    // 점수 검증
    long spring = top.stream().filter(k -> k.keyword().equals("spring")).findFirst().get().count();
    long mongodb = top.stream().filter(k -> k.keyword().equals("nosql")).findFirst().get().count();
    long java = top.stream().filter(k -> k.keyword().equals("java")).findFirst().get().count();

    assertThat(spring).isEqualTo(2L);
    assertThat(mongodb).isEqualTo(1L);
    assertThat(java).isEqualTo(2L);

    // 내림차순 정렬 (동점은 순서 보장 X)
    assertThat(top.subList(0, 2))
      .extracting(PopularKeywordDto::keyword)
      .containsAnyOf("spring", "java");
  }

  @Test
  void 인기검색어_반환값_만큼만_출력() {
    service.recordSearch("spring");
    service.recordSearch("nosql|java");

    // 현재 인기검색어 3개 저장되어 있음
    assertThat(service.popularTopN(2)).hasSize(2); // limit == 2 -> 2개 출력
    assertThat(service.popularTopN(5)).hasSize(3); // 3개 출력
  }

  @Test
  void 대소문자_공백_길이_규칙반영() { // 2글자 미만은 제외한다.
    service.recordSearch("  Redis  ");  // redis 저장
    service.recordSearch("re");         // re 저장
    service.recordSearch("r");          // 무시

    List<PopularKeywordDto> top = service.popularTopN(10);
    assertThat(top).extracting(PopularKeywordDto::keyword)
      .contains("redis", "re")
      .doesNotContain("r");
  }
}
