package com.liberarium.api.search.service;

import com.liberarium.api.common.util.RedissonCacheUtil;
import com.liberarium.api.search.doc.BookSearchDocument;
import com.liberarium.api.search.dto.SearchResponseDto;
import co.elastic.clients.elasticsearch.ElasticsearchClient;

import java.io.IOException;
import java.time.Duration;
import org.junit.jupiter.api.*;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.elasticsearch.ElasticsearchContainer;
import org.testcontainers.images.builder.ImageFromDockerfile;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.testcontainers.utility.DockerImageName;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
class BookSearchServiceTest {

  @MockBean
  private RedissonClient redissonClient;

  @MockBean
  private RedissonCacheUtil redissonCacheUtil;

  @Autowired ElasticsearchClient client;
  @Autowired ElasticsearchOperations operations;
  @Autowired BookSearchService service;

  static final String INDEX = "books";
  static final IndexCoordinates IDX = IndexCoordinates.of(INDEX);

  static final DockerImageName BASE =
    DockerImageName.parse("docker.elastic.co/elasticsearch/elasticsearch:8.14.0");

  // 1. 테스트 시작 전에 nori 포함 이미지를 빌드하도록 한다.
  static final ImageFromDockerfile ES_IMAGE = new ImageFromDockerfile("es-nori-test:8.14.0", false)
    .withDockerfileFromBuilder(b -> b
      .from(BASE.asCanonicalNameString())
      .run("elasticsearch-plugin install --batch analysis-nori")
      .build());

  // 2. 이 커스텀 이미지를 ES 이미지 파싱
  static final DockerImageName ES_IMAGE_NAME =
    DockerImageName.parse("es-nori-test:8.14.0")
      .asCompatibleSubstituteFor("docker.elastic.co/elasticsearch/elasticsearch");

  // 3. ElasticsearchContainer로 실행
  @Container
  static final ElasticsearchContainer ES;
  static {
    try {
      ES_IMAGE.get();  // 블로킹 빌드 (asCompatibleSubstituteFor 쓰려면 이름이 먼저 존재해야 함)

      ES = new ElasticsearchContainer(ES_IMAGE_NAME)
        .withEnv("xpack.security.enabled", "false")
        .withEnv("discovery.type", "single-node")
        .withEnv("ES_JAVA_OPTS", "-Xms512m -Xmx512m")
        .withStartupTimeout(Duration.ofMinutes(3));
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  @DynamicPropertySource
  static void elasticProps(DynamicPropertyRegistry r) {
    r.add("spring.elasticsearch.uris", () -> "http://" + ES.getHttpHostAddress());
  }

  @BeforeEach
  void setUp() throws IOException {
    // 인덱스 초기화
    try {
      client.indices().delete(b -> b.index(INDEX));
    } catch (Exception ignored) {}

    // 인덱스 생성 (테스트 전용 매핑)
    try (InputStreamReader rd = new InputStreamReader(
      new ClassPathResource("elasticsearch/books-index-test.json").getInputStream(),
      StandardCharsets.UTF_8)) {
      client.indices().create(b -> b.index(INDEX).withJson(rd));
    }

    // 문서 색인: 최근 등록순 -> 최든 출판순
    var now = Instant.now();
    List<BookSearchDocument> docs = List.of(
      new BookSearchDocument("1","978-1","MongoDB in Action, 2nd Edition","sub","Kyle Banker",
        "img1", LocalDate.parse("2016-03-01"), now.minusSeconds(10)),
      new BookSearchDocument("2","978-2","JavaScript Patterns","sub","Stoyan Stefanov",
        "img2", LocalDate.parse("2010-09-01"), now.minusSeconds(5)),
      new BookSearchDocument("3","978-3","현대 무역 실무","sub","나무요",
        "img3", LocalDate.parse("2021-01-01"), now.minusSeconds(1))
    );
    operations.save(docs, IDX);
    operations.indexOps(IDX).refresh();
  }

  @Test
  void 단일_검색_테스트() { // 제목,작가만 대상으로 최근 등록순 -> 최근 출판순
    SearchResponseDto res = service.searchSingle("mongodb", 1, 10);
    assertThat(res.books()).hasSize(1);
    assertThat(res.books().get(0).title()).containsIgnoringCase("mongodb");
  }

  @Test
  void 복합_검색_OR_연산_테스트() { // 최소 하나라도 매칭
    SearchResponseDto res = service.searchComposite("mongodb|나무요", 1, 10);
    assertThat(res.books()).extracting(b -> b.id()).containsExactlyInAnyOrder("1", "3");
  }

  @Test
  void 복합_검색_NOT_연산_테스트() { // 하나 매칭 + 제외
    SearchResponseDto res = service.searchComposite("mongodb-나무요", 1, 10);
    assertThat(res.books()).extracting(b -> b.id()).containsExactly("1");
  }
}
