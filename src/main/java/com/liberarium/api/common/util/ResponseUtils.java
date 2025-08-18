package com.liberarium.api.common.util;

import static com.liberarium.api.common.model.ErrorCode.FAIL_RESPONSE_UTIL_ERROR;

import com.liberarium.api.common.exception.defined.BusinessException;
import com.liberarium.api.common.model.ErrorCode;
import com.liberarium.api.common.model.ResponseError;
import com.liberarium.api.common.model.ResponseObject;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

@Slf4j
public class ResponseUtils {

  private ResponseUtils() {
    throw new BusinessException(FAIL_RESPONSE_UTIL_ERROR);
  }

  public static HttpHeaders getDefaultHttpHeaders() {
    HttpHeaders httpHeaders = new HttpHeaders();
    MediaType mediaType = new MediaType(MediaType.APPLICATION_JSON, StandardCharsets.UTF_8);
    httpHeaders.setContentType(mediaType);
    return httpHeaders;
  }

  public static <T> ResponseEntity<ResponseObject<T>> createResponseEntityByException(
    BusinessException ex) {
    return new ResponseEntity<>(ex.getBody(), getDefaultHttpHeaders(), ex.getHttpStatus());
  }

  public static <T> ResponseEntity<ResponseObject<T>> createResponseEntityByGlobalException(
    HttpStatus status) {
    return new ResponseEntity<>(null, getDefaultHttpHeaders(), status);
  }

  public static <T> ResponseEntity<ResponseObject<T>> createResponseEntity(T data,
    HttpStatus status) {
    return new ResponseEntity<>(ResponseObject.ofData(data), status);
  }

  /**
   * ErrorCode 기반 단일 에러 응답 (ErrorCode.detailMessage 사용)
   */
  public static ResponseEntity<ResponseObject<Void>> error(ErrorCode code) {
    HttpStatus status = code.getStatus();
    ResponseError err = ResponseError.of(Integer.toString(code.getStatus().value()), code.getCode(),
      code.getDetailMessage());
    return new ResponseEntity<>(ResponseObject.ofError(err), getDefaultHttpHeaders(), status);
  }

  /**
   * ErrorCode 기반 단일 에러 응답 (message 오버라이드)
   */
  public static ResponseEntity<ResponseObject<Void>> error(ErrorCode code, String messageOverride) {
    HttpStatus status = code.getStatus();
    String message = (messageOverride == null || messageOverride.isBlank())
      ? code.getDetailMessage() : messageOverride;
    ResponseError err = ResponseError.of(Integer.toString(code.getStatus().value()), code.getCode(),
      message);
    return new ResponseEntity<>(ResponseObject.ofError(err), getDefaultHttpHeaders(), status);
  }

  /**
   * 400 Bad Request (ErrorCode는 status가 400 계열이어야 자연스러움)
   */
  public static ResponseEntity<ResponseObject<Void>> badRequest(ErrorCode code) {
    // status가 400이 아닌 ErrorCode여도, 해당 status로 내려갑니다.
    return error(code);
  }

  /**
   * 400 Bad Request (메시지 오버라이드)
   */
  public static ResponseEntity<ResponseObject<Void>> badRequest(ErrorCode code,
    String messageOverride) {
    return error(code, messageOverride);
  }

  /**
   * 임의 상태코드 + 에러 리스트
   */
  public static ResponseEntity<ResponseObject<Void>> error(HttpStatus status,
    List<ResponseError> errors) {
    return new ResponseEntity<>(ResponseObject.ofErrors(errors), getDefaultHttpHeaders(), status);
  }

  /**
   * 검증 실패 전용: 같은 ErrorCode로 메시지 리스트를 매핑하여 응답
   */
  public static ResponseEntity<ResponseObject<Void>> validationErrors(ErrorCode code,
    Collection<String> messages) {
    HttpStatus status = code.getStatus();
    List<ResponseError> errors = messages.stream()
      .map(msg -> ResponseError.of(Integer.toString(code.getStatus().value()), code.getCode(), msg))
      .collect(Collectors.toList());
    return new ResponseEntity<>(ResponseObject.ofErrors(errors), getDefaultHttpHeaders(), status);
  }
}
