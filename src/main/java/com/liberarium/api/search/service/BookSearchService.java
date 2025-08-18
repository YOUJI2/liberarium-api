package com.liberarium.api.search.service;

import com.liberarium.api.search.doc.BookSearchDocument;
import com.liberarium.api.search.dto.BookItemDto;
import com.liberarium.api.search.dto.PageInfoDto;
import com.liberarium.api.search.dto.SearchResponseDto;
import com.liberarium.api.search.dto.SearchResponseDto.SearchMetadata;
import com.liberarium.api.search.query.SearchOperator;
import com.liberarium.api.search.query.SearchQueryParser;
import com.liberarium.api.search.query.SearchQueryParser.Parsed;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.client.elc.NativeQueryBuilder;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.List;

import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch._types.query_dsl.TextQueryType;

@Service
@RequiredArgsConstructor
public class BookSearchService {

  private final ElasticsearchOperations operations;
  private final PopularKeywordService popularKeywordService;

  private static final IndexCoordinates IDX = IndexCoordinates.of("books");

  private static final int MAX_SIZE = 50;

  // 단일 검색: keyword 1개
  public SearchResponseDto searchSingle(String keyword, int page, int size) {
    popularKeywordService.recordSearch(keyword); // 인기 검색어 집계

    Parsed parsed = SearchQueryParser.parse(keyword); // SINGLE 타입
    long start = System.currentTimeMillis();

    int safePage = Math.max(page, 1);
    int safeSize = Math.max(1, Math.min(size, MAX_SIZE)); // size 상한 50
    String term = parsed.positives().isEmpty() ? "" : parsed.positives().get(0);

    NativeQueryBuilder nb = new NativeQueryBuilder()
      .withQuery(q -> q.multiMatch(mm -> mm
        .query(term)
        .fields(
          "title^3", "title.ko^3", "title.en^2",
          "author^2", "author.ko^2", "author.en^1"
        )
        .type(TextQueryType.BestFields)
      ))
      .withPageable(PageRequest.of(safePage - 1, safeSize))
      .withTrackTotalHits(true);

    applySorts(nb); // 최근 등록순 정렬 적용

    SearchHits<BookSearchDocument> hits =
      operations.search(nb.build(), BookSearchDocument.class, IDX);

    return toResponse(keyword, SearchOperator.SINGLE.name(), start, hits, safePage, safeSize);
  }

  // 복합 검색: OR(|), NOT(-) 최대 2개 키워드
  public SearchResponseDto searchComposite(String q, int page, int size) {
    popularKeywordService.recordSearch(q); // 인기 검색어 집계

    Parsed parsed = SearchQueryParser.parse(q);
    long start = System.currentTimeMillis();

    int safePage = Math.max(page, 1);
    int safeSize = Math.max(1, Math.min(size, MAX_SIZE));

    NativeQueryBuilder nb = new NativeQueryBuilder()
      .withTrackTotalHits(true)
      .withPageable(PageRequest.of(safePage - 1, safeSize));

    applySorts(nb); // 최근 등록순 정렬 적용

    switch (parsed.op()) {
      case OR_OPERATION -> nb.withQuery(root -> root.bool(b -> {
        List<Query> shoulds = new ArrayList<>();
        for (String term : parsed.positives()) shoulds.add(multiMatch(term));
        return b.should(shoulds).minimumShouldMatch("1");
      }));
      case NOT_OPERATION -> nb.withQuery(root -> root.bool(b -> {
        if (!parsed.positives().isEmpty()) b.must(multiMatch(parsed.positives().get(0)));
        if (!parsed.negatives().isEmpty()) b.mustNot(multiMatch(parsed.negatives().get(0)));
        return b;
      }));
      case SINGLE -> {
        String term = parsed.positives().isEmpty() ? "" : parsed.positives().get(0);
        nb.withQuery(multiMatch(term));
      }
    }

    SearchHits<BookSearchDocument> hits =
      operations.search(nb.build(), BookSearchDocument.class, IDX);

    return toResponse(q, parsed.op().name(), start, hits, safePage, safeSize);
  }

  // 제목 + 작가만 검색한다.
  private Query multiMatch(String term) {
    return Query.of(q -> q.multiMatch(mm -> mm
      .query(term)
      .fields(
        "title^3", "title.ko^3", "title.en^2",
        "author^2", "author.ko^2", "author.en^1"
      )
      .type(TextQueryType.BestFields)
    ));
  }

  // 최근 등록순 정렬: createdAt DESC -> published DESC (둘 다 없으면 뒤로)
  private void applySorts(NativeQueryBuilder nb) {
    nb.withSort(s -> s.field(f -> f.field("createdAt").order(SortOrder.Desc).missing("_last")));
    nb.withSort(s -> s.field(f -> f.field("published").order(SortOrder.Desc).missing("_last")));
  }

  private SearchResponseDto toResponse(String searchQuery, String strategy, long startMs,
    SearchHits<BookSearchDocument> hits, int currentPage, int size) {
    long took = System.currentTimeMillis() - startMs;
    long total = hits.getTotalHits();
    int totalPages = (int) Math.ceil(total / (double) size);

    List<BookItemDto> items = hits.getSearchHits().stream()
      .map(sh -> {
        BookSearchDocument d = sh.getContent();
        return new BookItemDto(
          d.id(), d.title(), d.subtitle(), d.image(), d.author(), d.isbn(), d.published()
        );
      })
      .toList();

    PageInfoDto pageInfo = new PageInfoDto(currentPage, size, totalPages, total);
    SearchMetadata meta = new SearchResponseDto.SearchMetadata(took, strategy);

    return new SearchResponseDto(searchQuery, pageInfo, items, meta);
  }
}
