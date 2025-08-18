package com.liberarium.api.search.support;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.HealthStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Slf4j
@Component
@RequiredArgsConstructor
public class EsAwaiterUtil {

  private final ElasticsearchClient es;
  private final ElasticsearchOperations operations;

  @Value("${app.es.await.total-seconds:120}")
  private int totalSeconds;

  @Value("${app.es.await.poll-millis:1000}")
  private long pollMillis;

  // Cluster Health 가 Yellow/Green 될 때까지 대기
  public boolean waitForClusterYellow() {
    return waitForClusterYellow(Duration.ofSeconds(totalSeconds), Duration.ofMillis(pollMillis));
  }

  private boolean waitForClusterYellow(Duration total, Duration poll) {
    long deadline = System.nanoTime() + total.toNanos();
    int attempt = 0;

    while (System.nanoTime() < deadline) {
      attempt++;
      try {
        var health = es.cluster().health(h -> h
          .waitForStatus(HealthStatus.Yellow)
          .timeout(t -> t.time((poll.toSeconds() + 1) + "s"))
        );
        var st = health.status();
        log.info("ES cluster health = {} (attempt {})", st, attempt);
        if (st == HealthStatus.Yellow || st == HealthStatus.Green) {
          return true;
        }
      } catch (Exception ignore) {}
      sleep(poll);
    }
    return false;
  }

  // IndexOperations 를 사용할 수 있을 때까지 대기 (연결 및 client 준비 확인)
  public boolean waitForIndexOps(IndexCoordinates idx) {
    return waitForIndexOps(idx, Duration.ofSeconds(totalSeconds), Duration.ofMillis(pollMillis));
  }

  public boolean waitForIndexOps(IndexCoordinates idx, Duration total, Duration poll) {
    long deadline = System.nanoTime() + total.toNanos();
    int attempt = 0;

    while (System.nanoTime() < deadline) {
      attempt++;
      try {
        // exists 호출 자체가 연결/클라이언트 준비 여부를 확인하는 데 유용
        boolean exists = operations.indexOps(idx).exists();
        log.info("ES 오퍼레이션 준비 완료 '{}' (attempt {}) exists={}", idx.getIndexNames()[0], attempt, exists);
        return true;
      } catch (Exception ignore) {}
      sleep(poll);
    }
    return false;
  }

  // 인덱스 존재 확인
  public boolean indexExists(String indexName) {
    try {
      return es.indices().exists(b -> b.index(indexName)).value();
    } catch (Exception e) {
      return false;
    }
  }

  private void sleep(Duration d) {
    try {
      Thread.sleep(d.toMillis());
    } catch (InterruptedException ie) {
      Thread.currentThread().interrupt();
    }
  }
}
