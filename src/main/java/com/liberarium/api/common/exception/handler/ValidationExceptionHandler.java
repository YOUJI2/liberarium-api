package com.liberarium.api.common.exception.handler;

import com.liberarium.api.common.model.ErrorCode;
import com.liberarium.api.common.model.ResponseObject;
import com.liberarium.api.common.util.ResponseUtils;
import jakarta.validation.ConstraintViolationException;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.core.annotation.Order;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;

@Slf4j
@RestControllerAdvice
@Order(0)
public class ValidationExceptionHandler {

  @ExceptionHandler(HandlerMethodValidationException.class)
  public ResponseEntity<ResponseObject<Void>> handleHandlerMethodValidation(HandlerMethodValidationException ex) {
    List<String> messages = ex.getAllValidationResults().stream()
      .flatMap(r -> r.getResolvableErrors().stream())
      .map(MessageSourceResolvable::getDefaultMessage)
      .toList();

    return ResponseUtils.validationErrors(ErrorCode.INVALID_REQUEST, messages);
  }

  @ExceptionHandler(ConstraintViolationException.class)
  public ResponseEntity<ResponseObject<Void>> handleConstraintViolation(ConstraintViolationException ex) {
    List<String> messages = ex.getConstraintViolations().stream()
      .map(v -> v.getPropertyPath() + " " + v.getMessage())
      .toList();

    return ResponseUtils.validationErrors(ErrorCode.INVALID_REQUEST, messages);
  }
}
