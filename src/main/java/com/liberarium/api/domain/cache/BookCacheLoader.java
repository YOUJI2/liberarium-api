package com.liberarium.api.domain.cache;

import com.liberarium.api.common.aop.DistributedLock;
import com.liberarium.api.common.cache.CacheType;
import com.liberarium.api.common.exception.defined.BusinessException;
import com.liberarium.api.common.model.ErrorCode;
import com.liberarium.api.common.util.RedissonCacheUtil;
import com.liberarium.api.domain.converter.BookConverter;
import com.liberarium.api.domain.dto.BookDetailDto;
import com.liberarium.api.domain.entity.Book;
import com.liberarium.api.domain.repository.BookRepository;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBucket;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class BookCacheLoader {

  private final BookRepository bookRepository;
  private final RedissonCacheUtil redissonCacheUtil;

  @DistributedLock(key = "#id", waitTime = 200L, releaseTime = 2000L, fullbackCacheType = CacheType.BOOK_DETAIL)
  public BookDetailDto setBookCacheWithLock(String id) {
    // 1. 한 번더 cache hit 확인하기
    RBucket<BookDetailDto> cache = redissonCacheUtil.getBucket(CacheType.BOOK_DETAIL, id);
    BookDetailDto c = cache.get();
    if (c != null) {
      return c;
    }

    // 2. 도서 상세 조회
    Book book = bookRepository.findBookDetailInfoByIsbn(id)
      .orElseThrow(() -> {
        putBlockBook(id);  // 잘못된 id로 DB 재조회 방지
        return new BusinessException(ErrorCode.BOOK_NOT_FOUND);
      });

    // 3. 캐시 저장 (TTL 적용)
    BookDetailDto dto = BookConverter.toDetailDto(book);
    redissonCacheUtil.setWithTTL(CacheType.BOOK_DETAIL, id, dto);
    return dto;
  }

  public Optional<BookDetailDto> getCachingData(String id) {
    RBucket<BookDetailDto> bookCache = redissonCacheUtil.getBucket(CacheType.BOOK_DETAIL, id);
    BookDetailDto cached = bookCache.get();

    if (cached != null) {
      return Optional.of(cached);
    }
    return Optional.empty();
  }

  public void checkBlockBook(String id) {
    RBucket<Boolean> blockCache = redissonCacheUtil.getBucket(CacheType.BOOK_BLOCK, id);

    if (Boolean.TRUE.equals(blockCache.get())) {
      throw new BusinessException(ErrorCode.BOOK_NOT_FOUND);
    }
  }

  private void putBlockBook(String id) {
    redissonCacheUtil.setWithTTL(CacheType.BOOK_BLOCK, id, true);
  }
}
