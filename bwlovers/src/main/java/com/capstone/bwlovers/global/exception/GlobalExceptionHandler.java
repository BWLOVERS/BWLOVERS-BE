package com.capstone.bwlovers.global.exception;

import com.capstone.bwlovers.global.exception.dto.ExceptionResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.ZonedDateTime;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(CustomException.class)
    public ResponseEntity<ExceptionResponse> handleCustomException(CustomException e, HttpServletRequest request) {

        ExceptionCode ec = e.getExceptionCode();

        log.warn("[CustomException] code={}, msg={}, path={}",
                ec.getClientExceptionCode().name(),
                ec.getMessage(),
                request.getRequestURI()
        );

        ExceptionResponse response = new ExceptionResponse(
                ec.getHttpStatus().value(),
                ec.getClientExceptionCode().name(),
                ec.getMessage(),
                request.getRequestURI(),
                ZonedDateTime.now()
        );

        return ResponseEntity.status(ec.getHttpStatus()).body(response);
    }


    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ExceptionResponse> handleValidationException(MethodArgumentNotValidException e, HttpServletRequest request) {
        FieldError fieldError = e.getFieldError();
        String errorMessage = (fieldError != null) ? fieldError.getDefaultMessage() : "유효하지 않은 입력입니다.";

        ExceptionCode ec = ExceptionCode.ILLEGAL_ARGUMENT;

        log.warn("[ValidationException] field={}, msg={}",
                fieldError != null ? fieldError.getField() : "unknown",
                errorMessage
        );

        ExceptionResponse response = new ExceptionResponse(
                ec.getHttpStatus().value(),
                ec.getClientExceptionCode().name(),
                errorMessage,
                request.getRequestURI(),
                ZonedDateTime.now()
        );

        return ResponseEntity.status(ec.getHttpStatus()).body(response);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ExceptionResponse> handleIllegalArgumentException(IllegalArgumentException e, HttpServletRequest request) {
        ExceptionCode ec = ExceptionCode.ILLEGAL_ARGUMENT;

        log.warn("[IllegalArgumentException] msg={}", e.getMessage());

        ExceptionResponse response = new ExceptionResponse(
                ec.getHttpStatus().value(),
                ec.getClientExceptionCode().name(),
                ec.getMessage(),
                request.getRequestURI(),
                ZonedDateTime.now()
        );

        return ResponseEntity.status(ec.getHttpStatus()).body(response);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ExceptionResponse> handleException(Exception e, HttpServletRequest request) {
        ExceptionCode ec = ExceptionCode.INTERNAL_SERVER_ERROR;

        log.error("[UnhandledException] path={}", request.getRequestURI(), e);

        ExceptionResponse response = new ExceptionResponse(
                ec.getHttpStatus().value(),
                ec.getClientExceptionCode().name(),
                ec.getMessage(),
                request.getRequestURI(),
                ZonedDateTime.now()
        );

        return ResponseEntity.status(ec.getHttpStatus()).body(response);
    }
}
