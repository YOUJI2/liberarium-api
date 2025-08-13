package com.liberarium.api.logging;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Optional;
import java.util.UUID;

@Component
public class CorrelationFilter extends OncePerRequestFilter {

  private static final Logger log = LoggerFactory.getLogger(CorrelationFilter.class);

  @Override
  protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
    throws ServletException, IOException {

    String rid = Optional.ofNullable(request.getHeader("X-Request-Id"))
      .filter(s -> !s.isBlank())
      .orElse(UUID.randomUUID().toString());

    MDC.put("requestId", rid);
    MDC.put("clientIp", request.getRemoteAddr()); // 프록시 환경이면 resolveClientIp(request)로 교체 권장
    response.setHeader("X-Request-Id", rid);        // 응답에도 리턴

    long start = System.currentTimeMillis();
    try {
      chain.doFilter(request, response);
    } finally {
      long took = System.currentTimeMillis() - start;
      int status = response.getStatus();
      String method = request.getMethod();
      String uri = request.getRequestURI();

      // 요청 요약 로그: 메서드, URI, 상태코드, 소요시간(ms) 표시
      String msg = String.format("HTTP %s %s -> %d (%d ms)", method, uri, status, took);

      if (status >= 500)      log.error(msg);
      else if (status >= 400) log.warn(msg);
      else                    log.info(msg);

      MDC.clear();
    }
  }
}
