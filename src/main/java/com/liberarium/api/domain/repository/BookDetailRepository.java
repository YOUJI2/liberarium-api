package com.liberarium.api.domain.repository;

import com.liberarium.api.domain.entity.BookDetail;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BookDetailRepository extends JpaRepository<BookDetail, String> {

}
