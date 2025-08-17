package com.liberarium.api.domain.entity;

import com.liberarium.api.common.model.BaseEntity;
import jakarta.persistence.*;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Builder
@Entity
@Getter
@Table(name = "book")
@NoArgsConstructor
@AllArgsConstructor
public class Book extends BaseEntity {

  @Id
  @Column(length = 20, nullable = false)
  private String isbn;

  @Column(length = 300, nullable = false)
  private String title;

  @Column(length = 300)
  private String subtitle;

  @Column(length = 200, nullable = false)
  private String author;

  @Column(name = "image_url", length = 500)
  private String imageUrl;

  @Column(name = "published_date")
  private LocalDate publishedDate;

  @Column(length = 200)
  private String publisher;

  @OneToOne(mappedBy = "book", fetch = FetchType.LAZY, cascade = CascadeType.REMOVE, orphanRemoval = true)
  private BookDetail detail;

  @Builder.Default
  @OneToMany(mappedBy = "book", fetch = FetchType.LAZY, cascade = CascadeType.REMOVE, orphanRemoval = true)
  private Set<BookCatalog> catalogs = new HashSet<>();
}
