package com.liberarium.api.support.dto;

import com.liberarium.api.domain.dto.BookDetailDto;
import com.liberarium.api.domain.entity.Book;
import java.time.LocalDate;

public final class BookTestData {

  private BookTestData() {
  }

  public static final String ISBN = "9788964149270";

  public static Book getEntity() {
    Book b = Book.builder()
      .isbn(ISBN)
      .title("무역학개론 (유창권 외)")
      .subtitle("무역학개론유창권외")
      .author("유창권")
      .imageUrl("https://image.aladin.co.kr/product/27944/37/cover/8964149270_1.jpg")
      .publishedDate(LocalDate.of(2021, 10, 2))
      .publisher("두남")
      .build();

    return b;
  }

  public static BookDetailDto getDto() {
    return new BookDetailDto(
      ISBN,
      "무역학개론 (유창권 외)",
      "무역학개론유창권외",
      "유창권",
      "경제/경영/자기계발",
      "https://image.aladin.co.kr/product/27944/37/cover/8964149270_1.jpg",
      LocalDate.of(2021, 10, 2),
      "두남",
      false,
      true,
      25000,
      25000,
      "유창권에 대한 간단 소개입니다.",
      "무역의 발생 원인과 무역이익을 규명하는 국제무역 이론에서부터 무역계약의 체결과 무역운송 절차 등을 설명하고 무역 활동의 위험관리를 위한 해상보험 및 무역보험 등의 내용을 기술하였다."
    );
  }
}
