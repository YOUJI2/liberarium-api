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
@Table(name = "book_detail")
@NoArgsConstructor
@AllArgsConstructor
public class BookDetail extends BaseEntity {

  // book PK와 동일하게 설정, @MapsId 로 FK=PK 매핑
  @Id
  @Column(length = 20, nullable = false)
  private String isbn;

  @MapsId
  @OneToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "isbn",
    foreignKey = @ForeignKey(name = "fk_book_detail_book"))
  private Book book;

  @Column(name = "sold_out", nullable = false)
  private boolean soldOut;  // 도서 재고 품절 여부만 확인 (재고 수량은 따로 관리 필요)

  @Column(name = "free_shipping", nullable = false)
  private boolean freeShipping;

  @Column(name = "list_price")
  private int listPrice;

  @Column(name = "sale_price")
  private int salePrice;

  @Lob
  @Column(name = "author_bio")
  private String authorBio;

  @Lob
  @Column(name = "book_description")
  private String bookDescription;

}
