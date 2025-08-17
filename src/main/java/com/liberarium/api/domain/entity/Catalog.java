package com.liberarium.api.domain.entity;

import com.liberarium.api.common.model.BaseEntity;
import jakarta.persistence.*;

import java.util.HashSet;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Table(name = "catalog")
@NoArgsConstructor
@AllArgsConstructor
public class Catalog extends BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Integer id;

  @Column(length = 100, nullable = false, unique = true)
  private String code;

  @Column(length = 100, nullable = false)
  private String name;

  @Column(name = "sort_order")
  private int sortOrder;

  @Column(nullable = false)
  private boolean active;

  @OneToMany(mappedBy = "catalog", fetch = FetchType.LAZY)
  private Set<BookCatalog> books = new HashSet<>();
}
