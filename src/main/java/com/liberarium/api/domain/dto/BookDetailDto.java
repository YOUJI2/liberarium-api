package com.liberarium.api.domain.dto;

import java.time.LocalDate;

public record BookDetailDto (
  String isbn,
  String title,
  String subtitle,
  String author,
  String catalogName,
  String imageUrl,
  LocalDate publishedDate,
  String publisher,
  boolean soldOut,
  boolean freeShipping,
  int listPrice,
  int salePrice,
  String authorBio,
  String bookDescription
  ) {}

