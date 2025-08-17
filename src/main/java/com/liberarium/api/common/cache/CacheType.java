package com.liberarium.api.common.cache;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum CacheType {

  BOOK_BLOCK("book_block", 60_000L),
  BOOK_DETAIL("book_detail", 300_000L);

  private final String name;
  private final long ttlMs;      // 지정된 만료 시간
}
