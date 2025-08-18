package com.liberarium.api.search.controller;

import com.liberarium.api.common.model.ResponseObject;
import com.liberarium.api.common.util.ResponseUtils;
import com.liberarium.api.search.dto.PopularKeywordDto;
import com.liberarium.api.search.dto.SearchResponseDto;
import com.liberarium.api.search.service.BookSearchService;
import com.liberarium.api.search.service.PopularKeywordService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

@Validated
@RestController
@RequiredArgsConstructor
@Tag(name = "도서 검색 API", description = "도서를 단일, 복합 검색합니다.")
@RequestMapping(value = "/api", produces = MediaType.APPLICATION_JSON_VALUE)
public class BookSearchController {

  private final BookSearchService bookSearchService;
  private final PopularKeywordService popularKeywordService;

  /**
   *  구현 과제) 도서 단일 검색
   *    GET /api/books?keyword={keyword}&page={page}&size={size}
   */
  @Operation(summary = "도서 단일 키워드 검색", description = "단일 키워드로 도서 검색을 지원합니다.")
  @GetMapping("/books")
  public ResponseEntity<ResponseObject<SearchResponseDto>> singleSearchBook(
    @RequestParam @NotBlank String keyword,
    @RequestParam(defaultValue = "1") @Min(1) int page,
    @RequestParam(defaultValue = "20") @Min(1) @Max(50) int size
  ) {
    return ResponseUtils.createResponseEntity(bookSearchService.searchSingle(keyword, page, size), HttpStatus.OK);
  }

  /**
   *  구현 과제) 도서 복합 검색
   *    GET /api/search/books?q={query}&page={page}&size={size}
   */
  @Operation(summary = "도서 복합 키워드 검색", description = "검색 쿼리로 도서 복합 검색을 지원합니다.")
  @GetMapping("/search/books")
  public ResponseEntity<ResponseObject<SearchResponseDto>> compositeSearchBook(
    @RequestParam(name = "q") @NotBlank String query,
    @RequestParam(defaultValue = "1") @Min(1) int page,
    @RequestParam(defaultValue = "20") @Min(1) @Max(50) int size
  ) {
    return ResponseUtils.createResponseEntity(bookSearchService.searchComposite(query, page, size), HttpStatus.OK);
  }

  @Operation(summary = "인기 검색어 Top N", description = "누적 기준 인기 검색어를 반환합니다.")
  @GetMapping("/search/popular")
  public ResponseEntity<ResponseObject<List<PopularKeywordDto>>> getPopularKeywords(
    @RequestParam(defaultValue = "10") @Min(1) @Max(50) int size) {

    List<PopularKeywordDto> top = popularKeywordService.popularTopN(size);
    return ResponseUtils.createResponseEntity(top, HttpStatus.OK);
  }
}
