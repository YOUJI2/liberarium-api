package com.liberarium.api.domain.entity;

import com.liberarium.api.common.model.BaseEntity;
import jakarta.persistence.*;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Builder
@Entity
@Getter
@Table(name = "book_catalog")
@NoArgsConstructor
@AllArgsConstructor
public class BookCatalog extends BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "isbn", nullable = false,
    foreignKey = @ForeignKey(name = "fk_book_catalog__book"))
  private Book book;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "catalog_id", nullable = false,
    foreignKey = @ForeignKey(name = "fk_book_catalog__catalog"))
  private Catalog catalog;

}
