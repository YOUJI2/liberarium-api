package com.liberarium.api.search.config;

import com.liberarium.api.domain.entity.Book;
import com.liberarium.api.domain.repository.BookRepository;
import com.liberarium.api.search.doc.BookSearchDocument;
import com.liberarium.api.search.support.EsAwaiterUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.core.annotation.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.client.elc.NativeQueryBuilder;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;

import java.time.Instant;
import java.util.List;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class BookSearchSeeder {

  private static final int BATCH_SIZE = 200;

  private final EsAwaiterUtil esAwaiterUtil;
  private final ElasticsearchOperations operations;
  private final BookRepository bookRepository;

  @Bean
  @DependsOn(BooksIndexInitializer.BEAN_NAME)
  @Order(1)
  public ApplicationRunner seedBooksToEs() {
    return args -> {

      IndexCoordinates IDX = IndexCoordinates.of("books");

      // 1) ES 연결/ops 준비 대기
      if (!esAwaiterUtil.waitForIndexOps(IDX)) {
        log.warn("ES 연결/OPS 준비 실패 - 시드 작업 스킵");
        return;
      }

      // 2) 인덱스 존재 안하면 스킵 (생성기는 따로 동작)
      boolean indexExists = esAwaiterUtil.indexExists("books");
      if (!indexExists) {
        log.warn("인덱스 'books' 이(가) 없음 - 인덱스 생성 실패 가능. 시드 작업 생략.");
        return;
      }

      // 3) 이미 문서 있으면 스킵
      try {
        NativeQuery countQuery = new NativeQueryBuilder()
          .withQuery(q -> q.matchAll(m -> m))
          .build();

        long current = operations.count(countQuery, BookSearchDocument.class, IDX);
        if (current > 0) {
          log.info("ES 인덱스 '{}' 에 이미 {}건 존재. 시드 작업 생략.", IDX.getIndexNames()[0], current);
          return;
        }
      } catch (Exception e) {
        log.warn("문서 카운트 실패: {} - 시드 작업 생략", e.getMessage());
        return;
      }

      // 4) 배치 페이징으로 색인
      long totalSaved = 0L;
      int pageNo = 0;

      while (true) {
        Page<Book> page = bookRepository.findAll(PageRequest.of(pageNo, BATCH_SIZE));
        if (!page.hasContent()) break;

        List<BookSearchDocument> docs = page.getContent().stream()
          .map(b -> new BookSearchDocument(
            b.getIsbn(),          // id
            b.getIsbn(),          // isbn
            b.getTitle(),         // title
            b.getSubtitle(),      // subtitle
            b.getAuthor(),        // author
            b.getImageUrl(),      // image
            b.getPublishedDate(), // LocalDate published
            Instant.now()         // createdAt: 최신 등록 정렬용
          ))
          .toList();

        try {
          operations.save(docs, IDX);
          totalSaved += docs.size();
          log.info("ES 시드 저장 완료 - page={}, size={}, 누적={}", pageNo, docs.size(), totalSaved);
        } catch (Exception e) {
          log.warn("ES 시드 저장 실패 - page={}, err={}", pageNo, e.getMessage());
        }

        if (!page.hasNext()) break;
        pageNo++;
      }

      try {
        operations.indexOps(IDX).refresh();
      } catch (Exception e) {
        log.warn("ES refresh 실패: {}", e.getMessage());
      }

      log.info("ES 색인 완료: {}건 -> 인덱스 'books'", totalSaved);
    };
  }
}
