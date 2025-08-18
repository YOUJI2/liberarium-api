package com.liberarium.api.search.dto;

public record PageInfoDto(
  int currentPage,
  int pageSize,
  int totalPages,
  long totalElements
) {}
