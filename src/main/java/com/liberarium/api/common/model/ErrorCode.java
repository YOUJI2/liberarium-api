package com.liberarium.api.common.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

  //400번 에러
  INVALID_INPUT_CATEGORY("40001", "잘못된 요청 : 파라미터 에러", HttpStatus.BAD_REQUEST, "존재하지 않은 카탈로그 입니다."),
  INVALID_CATEGORY_TYPE("40002", "잘못된 요청 : 타입 에러", HttpStatus.BAD_REQUEST, "잘못된 타입입니다."),

  //404번 에러
  BOOK_NOT_FOUND("40401", "비니지스 에러 : 조회 데이터 오류", HttpStatus.NOT_FOUND, "해당 도서에 대한 정보가 존재하지 않습니다."),
  CATEGORY_PRODUCT_NOT_FOUND("40402", "비지니스 에러 : 조회 데이터 오류", HttpStatus.NOT_FOUND, "해당 카탈로그에 대한 상품이 존재하지 않습니다."),

  //409번 에러
  LOCK_TRY_FAILED("40901", "비지니스 에러 : 리소스 상태 에러", HttpStatus.CONFLICT, "리소스 동시 접근으로 인한 충돌이 발생되었습니다."),

  //500번대 에러
  FAIL_RESPONSE_UTIL_ERROR("50001", "서버 에러", HttpStatus.INTERNAL_SERVER_ERROR, "ResponseUtils 인스턴스화는 금지입니다."),
  CACHE_NOT_FOUND_ERROR("50002", "서버 에러", HttpStatus.INTERNAL_SERVER_ERROR, "서버설정시 캐시가 누락되었습니다."),
  INVALID_CACHE_MANAGER_TYPE("50003", "서버 에러", HttpStatus.INTERNAL_SERVER_ERROR, "지원하지않는 캐시 타입입니다.");

  private final String code;
  private final String title;
  private final HttpStatus status;
  private final String detailMessage;

  @Override
  public String toString() {
    return "ErrorCode{" +
        "code='" + code + '\'' +
        ", title='" + title + '\'' +
        ", status=" + status +
        ", detailMessage='" + detailMessage + '\'' +
        '}';
  }
}
