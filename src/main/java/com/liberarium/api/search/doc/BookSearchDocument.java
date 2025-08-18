package com.liberarium.api.search.doc;

import java.time.Instant;
import java.time.LocalDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;
import org.springframework.data.elasticsearch.annotations.DateFormat;

@Document(indexName = "books")
public record BookSearchDocument(
  @Id String id,

  @Field(type = FieldType.Keyword)
  String isbn,

  @Field(type = FieldType.Text)
  String title,

  @Field(type = FieldType.Text)
  String subtitle,

  @Field(type = FieldType.Text)
  String author,

  @Field(type = FieldType.Keyword)
  String image,

  @Field(type = FieldType.Date, format = DateFormat.date, pattern = "yyyy-MM-dd")
  LocalDate published,

  @Field(type = FieldType.Date)
  Instant createdAt
) {}
