package com.liberarium.api.domain.converter;

import com.liberarium.api.domain.dto.BookDetailDto;
import com.liberarium.api.domain.entity.Book;
import com.liberarium.api.domain.entity.BookCatalog;
import com.liberarium.api.domain.entity.BookDetail;
import com.liberarium.api.domain.entity.Catalog;
import java.util.stream.Collectors;
import lombok.NoArgsConstructor;

@NoArgsConstructor
public class BookConverter {

  public static BookDetailDto toDetailDto(Book b) {
    // 대표 카탈로그 1개 선택 (active=1, sortOrder → id)
    String catalogNames = b.getCatalogs().stream()
      .map(BookCatalog::getCatalog)
      .map(Catalog::getName)
      .sorted()
      .collect(Collectors.joining(", "));

    BookDetail d = b.getDetail(); // 1:1 상세 (nullable)
    return new BookDetailDto(
      b.getIsbn(),
      b.getTitle(),
      b.getSubtitle(),
      b.getAuthor(),
      catalogNames,
      b.getImageUrl(),
      b.getPublishedDate(),
      b.getPublisher(),
      d != null && d.isSoldOut(),
      d != null && d.isFreeShipping(),
      d != null ? d.getListPrice() : 0,
      d != null ? d.getSalePrice() : 0,
      d != null ? d.getAuthorBio() : "",
      d != null ? d.getBookDescription() : ""
    );
  }
}
