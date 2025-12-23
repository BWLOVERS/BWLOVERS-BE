package com.capstone.bwlovers.global.exception;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatusCode;

@RequiredArgsConstructor
public class CustomException extends RuntimeException {
    private final ExceptionCode exceptionCode;

    @Override
    public String getMessage() {
        return exceptionCode.getMessage();
    }

    public HttpStatusCode getHttpStatusCode() {
        return exceptionCode.getHttpStatus();
    }

    public String getExceptionCodeName(){
        return exceptionCode.getClientExceptionCode().name();
    }
}
