package com.liberarium.api.domain.repository;

import com.liberarium.api.domain.entity.Book;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BookRepository extends JpaRepository<Book, String> {


}
