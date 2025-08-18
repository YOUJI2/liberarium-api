package com.liberarium.api.search.dto;

import java.util.List;

public record SearchResponseDto(
  String searchQuery,
  PageInfoDto pageInfo,
  List<BookItemDto> books,
  SearchMetadata metadata
) {
  public static record SearchMetadata(
    long executionTime,   // ms
    String strategy       // OR_OPERATION / NOT_OPERATION / SINGLE 타입
  ) {}
}
