package com.liberarium.api.common.exception.defined;

import com.liberarium.api.common.model.ErrorCode;
import com.liberarium.api.common.model.ResponseError;
import com.liberarium.api.common.model.ResponseObject;
import org.springframework.http.HttpStatus;

public class BusinessException extends RuntimeException{

  private final ErrorCode errorCode;

  public BusinessException(ErrorCode errorCode) {
    super(errorCode.toString());
    this.errorCode = errorCode;
  }

  // 예외 발생시 error 내용을 ResponseError에 추가
  public <T> ResponseObject<T> getBody() {
    //에러시 에러코드로 공유(자세한 에러는 Client에 내려주지 않는다)
    ResponseError responseError = ResponseError
        .of(Integer.toString(errorCode.getStatus().value()), errorCode.getCode(),errorCode.getTitle());

    return ResponseObject.ofError(responseError);
  }

  //로그 수준 : 4XX = true
  public boolean isClientError() {
    return this.errorCode.getStatus().is4xxClientError();
  }

  public HttpStatus getHttpStatus() {
    return this.errorCode.getStatus();
  }

  public ErrorCode getErrorCode() { return this.errorCode; }

  @Override
  public String toString() {
    return "BusinessException{" +
        "error =" + errorCode + "}";
  }
}
