package com.liberarium.api.domain.service;

import com.liberarium.api.domain.dto.BookDetailDto;

public interface BookService {
  BookDetailDto findBookDetailInfo(String isbn);
}
