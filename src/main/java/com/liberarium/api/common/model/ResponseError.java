package com.liberarium.api.common.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

@JsonInclude(Include.NON_DEFAULT)
public record ResponseError(
    String httpStatus,
    String errorCode,
    String message
) {
  public static ResponseError of(String httpStatus, String code, String message) {
    return new ResponseError(httpStatus, code, message);
  }
}
