package com.liberarium.api.search.query;

import java.util.ArrayList;
import java.util.List;

public class SearchQueryParser {

  public record Parsed(
    String normalized,
    SearchOperator op,
    List<String> positives,
    List<String> negatives
  ) {}

  public static Parsed parse(String raw) {
    if (raw == null || raw.isBlank()) return new Parsed("", SearchOperator.SINGLE, List.of(), List.of());
    String q = raw.trim();

    // OR: a|b 연산
    if (q.contains("|")) {
      String[] parts = q.split("\\|", -1);
      List<String> pos = normalize(parts);
      if (pos.size() > 2) pos = pos.subList(0, 2);
      return new Parsed(q, SearchOperator.OR_OPERATION, pos, List.of());
    }

    // NOT: a-b 제외 연산
    if (q.contains("-")) {
      String[] parts = q.split("-", -1);
      List<String> terms = normalize(parts);
      String positive = terms.size() > 0 ? terms.get(0) : "";
      String negative = terms.size() > 1 ? terms.get(1) : "";
      return new Parsed(q, SearchOperator.NOT_OPERATION,
        positive.isBlank() ? List.of() : List.of(positive),
        negative.isBlank() ? List.of() : List.of(negative));
    }

    // SINGLE
    return new Parsed(q, SearchOperator.SINGLE, List.of(q), List.of());
  }

  private static List<String> normalize(String[] arr) {
    List<String> out = new ArrayList<>();
    for (String s : arr) {
      String t = s == null ? "" : s.trim();
      if (!t.isBlank()) out.add(t);
    }
    return out;
  }
}
