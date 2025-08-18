package com.liberarium.api.search.service;

import com.liberarium.api.search.dto.PopularKeywordDto;
import com.liberarium.api.search.query.SearchQueryParser;
import com.liberarium.api.search.query.SearchQueryParser.Parsed;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RScoredSortedSet;
import org.redisson.api.RedissonClient;
import org.redisson.client.protocol.ScoredEntry;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class PopularKeywordService {

  private static final String ZSET_KEY = "popular:keywords";
  private final RedissonClient redissonClient;

  public void recordSearch(String rawQuery) {
    if (rawQuery == null || rawQuery.isBlank()) return;

    try {
      Parsed parsed = SearchQueryParser.parse(rawQuery);
      RScoredSortedSet<String> zset = redissonClient.getScoredSortedSet(ZSET_KEY);

      for (String term : parsed.positives()) { // 검색어 포함 키워드만 저장
        String k = normalize(term); // 검증
        if (!k.isEmpty()) {
          zset.addScore(k, 1.0);  // 점수(카운트) +1 (원자적으로 동작함)
        }
      }
    } catch (Exception e) {
      log.warn("인기 검색어 저장에 실패하였습니다. : {}", rawQuery, e);
    }
  }

  public List<PopularKeywordDto> popularTopN(int n) {
    int size = Math.max(1, n);
    RScoredSortedSet<String> zset = redissonClient.getScoredSortedSet(ZSET_KEY);
    List<ScoredEntry<String>> entries = (List<ScoredEntry<String>>) zset.entryRangeReversed(0, size - 1);

    List<PopularKeywordDto> result = new ArrayList<>(entries.size());
    for (ScoredEntry<String> e : entries) {
      long count = (long) Math.floor(e.getScore());
      result.add(new PopularKeywordDto(e.getValue(), count));
    }

    return result;
  }

  private String normalize(String s) {
    String k = s.strip().toLowerCase();
    return k.length() >= 2 ? k : ""; // 최소 2글자부터 기록
  }
}
