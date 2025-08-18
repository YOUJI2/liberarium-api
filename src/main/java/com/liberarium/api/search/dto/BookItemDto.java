package com.liberarium.api.search.dto;

import java.time.LocalDate;

public record BookItemDto(
  String id,
  String title,
  String subtitle,
  String image,
  String author,
  String isbn,
  LocalDate published
) {}
