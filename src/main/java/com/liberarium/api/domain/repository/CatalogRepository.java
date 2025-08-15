package com.liberarium.api.domain.repository;

import com.liberarium.api.domain.entity.Catalog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CatalogRepository extends JpaRepository<Catalog, Integer> {

}
