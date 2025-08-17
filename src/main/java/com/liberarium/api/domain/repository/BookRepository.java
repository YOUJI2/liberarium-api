package com.liberarium.api.domain.repository;

import com.liberarium.api.domain.entity.Book;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BookRepository extends JpaRepository<Book, String> {

  @Query("""
    select distinct b
    from Book b
    left join fetch b.detail d
    left join fetch b.catalogs cts
    left join fetch cts.catalog c
    where b.isbn = :isbn
  """)
  Optional<Book> findBookDetailInfoByIsbn(@Param("isbn") String isbn);
}
