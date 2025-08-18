package com.liberarium.api.search.config;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import com.liberarium.api.search.support.EsAwaiterUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;

@Slf4j
@Component
@RequiredArgsConstructor
public class BooksIndexInitializer {

  public static final String BEAN_NAME = "booksIndexCreateRunner";

  private final ElasticsearchClient es;
  private final EsAwaiterUtil esAwaiterUtil;

  @Value("${app.es.books-index:books}")
  private String indexName;

  @Value("classpath:elasticsearch/books-index.json")
  private Resource indexJson;

  @Bean(name = BEAN_NAME)
  @Order(0)
  public ApplicationRunner createBooksIndexIfAbsent() {
    return args -> {
      // 1. ES 준비될 때까지 충분히 대기 (Cluster Yellow)
      if (!esAwaiterUtil.waitForClusterYellow()) {
        log.warn("ES 연결 불가 - 인덱스({}) 생성 X", indexName);
        return;
      }

      // 2. 이미 존재하면 스킵
      try {
        boolean exists = es.indices().exists(b -> b.index(indexName)).value();
        if (exists) {
          log.info("ES 인덱스 '{}' 이미 존재. 생성 스킵", indexName);
          return;
        }
      } catch (Exception e) {
        log.warn("인덱스 존재 확인 실패: {} - 생성 시도 중단", e.getMessage());
        return;
      }

      // 3. JSON 매핑으로 인덱스 생성
      try (InputStream is = indexJson.getInputStream()) {
        String json = new String(is.readAllBytes(), StandardCharsets.UTF_8);
        es.indices().create(b -> b.index(indexName).withJson(new StringReader(json)));
        log.info("ES 인덱스 '{}' 생성 완료(JSON 매핑 적용)", indexName);
      } catch (Exception e) {
        log.warn("인덱스 '{}' 생성 실패: {}", indexName, e.getMessage());
      }
    };
  }
}
