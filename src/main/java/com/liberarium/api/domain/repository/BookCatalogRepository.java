package com.liberarium.api.domain.repository;

import com.liberarium.api.domain.entity.BookCatalog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BookCatalogRepository extends JpaRepository<BookCatalog, Long> {

}
