package com.liberarium.api.domain.service;

import com.liberarium.api.domain.cache.BookCacheLoader;
import com.liberarium.api.domain.dto.BookDetailDto;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class BookServiceImpl implements BookService {

  private final BookCacheLoader bookCacheLoader;

  @Override
  @Transactional(readOnly = true)
  public BookDetailDto findBookDetailInfo(String id) {

    // 존재하지 도서에 대해 요청이 많을때 위한 방어 로직(404 not found 방지)
    bookCacheLoader.checkBlockBook(id);

    Optional<BookDetailDto> cachingData = bookCacheLoader.getCachingData(id);
    return cachingData.orElseGet(() -> bookCacheLoader.setBookCacheWithLock(id));
  }
}
