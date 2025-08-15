package com.liberarium.api.common.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import java.util.List;

@JsonInclude(Include.NON_DEFAULT)
public record ResponseObject<T>(
    @JsonInclude(Include.NON_NULL)
    T data,

    @JsonInclude(Include.NON_NULL)
    List<ResponseError> errors
) {
  public static <T> ResponseObject<T> ofData(T data) {
    return new ResponseObject<>(data, null);
  }

  public static <T> ResponseObject<T> ofError(ResponseError error) {
    return new ResponseObject<>(null, List.of(error));
  }

  public static <T> ResponseObject<T> ofErrors(List<ResponseError> errors) {
    return new ResponseObject<>(null, errors);
  }
}
