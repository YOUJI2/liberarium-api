package com.liberarium.api.domain.controller;

import com.liberarium.api.common.model.ResponseObject;
import com.liberarium.api.common.util.ResponseUtils;
import com.liberarium.api.domain.dto.BookDetailDto;
import com.liberarium.api.domain.service.BookService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@Tag(name = "도서 상세 API", description = "도서 정보를 조회합니다.")
@RequestMapping(value = "/api/books", produces = MediaType.APPLICATION_JSON_VALUE)
public class BookController {

  private final BookService bookService;

  /**
   *  구현 과제) 도서 상세 조회 API
   */
  @Operation(summary = "도서 상세 조회", description = "도서의 고유 ID(ISBN)을 기준으로 상세정보를 조회합니다.")
  @GetMapping("/{id}")
  public ResponseEntity<ResponseObject<BookDetailDto>> getBookDetail(@PathVariable("id") String id) {
    BookDetailDto bookDetailDto = bookService.findBookDetailInfo(id);
    return ResponseUtils.createResponseEntity(bookDetailDto, HttpStatus.OK);
  }
}
