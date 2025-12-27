package com.capstone.bwlovers.global.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ExceptionCode {

    // 전체
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, ClientExceptionCode.INTERNAL_SERVER_ERROR, "예상치 못한 서버에러가 발생했습니다."),
    ILLEGAL_ARGUMENT(HttpStatus.BAD_REQUEST, ClientExceptionCode.ILLEGAL_ARGUMENT, "필수 파라미터 누락"),

    //  Auth / OAuth
    LOGIN_ERROR(HttpStatus.UNAUTHORIZED, ClientExceptionCode.LOGIN_ERROR, "소셜 로그인 인증에 실패했습니다."),
    AUTH_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, ClientExceptionCode.AUTH_SERVER_ERROR, "인증 서버와 통신 중 오류가 발생했습니다."),
    AUTH_TOKEN_EMPTY(HttpStatus.UNAUTHORIZED, ClientExceptionCode.AUTH_TOKEN_EMPTY, "인증 토큰이 존재하지 않습니다. 다시 로그인해주세요."),
    ACCESS_TOKEN_EMPTY(HttpStatus.UNAUTHORIZED, ClientExceptionCode.ACCESS_TOKEN_EMPTY, "엑세스 토큰이 존재하지 않습니다."),
    REFRESH_TOKEN_EMPTY(HttpStatus.UNAUTHORIZED, ClientExceptionCode.REFRESH_TOKEN_EMPTY, "리프레시 토큰이 존재하지 않습니다."),
    AUTH_TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, ClientExceptionCode.AUTH_TOKEN_EXPIRED, "만료된 토큰입니다."),
    AUTH_TOKEN_INVALID(HttpStatus.UNAUTHORIZED, ClientExceptionCode.AUTH_TOKEN_INVALID, "올바르지 않은 토큰입니다."),
    AUTH_TOKEN_MISMATCH(HttpStatus.UNAUTHORIZED, ClientExceptionCode.AUTH_TOKEN_MISMATCH, "토큰 소유자가 일치하지 않습니다."),
    ALREADY_LOGGED_TOKEN(HttpStatus.UNAUTHORIZED, ClientExceptionCode.ALREADY_LOGGED_TOKEN, "이미 로그아웃된 토큰입니다."),
    FORBIDDEN(HttpStatus.FORBIDDEN, ClientExceptionCode.FORBIDDEN, "해당 자원에 대한 접근 권한이 없습니다.");


    private final HttpStatus httpStatus;
    private final ClientExceptionCode clientExceptionCode;
    private final String message;

    ExceptionCode(HttpStatus httpStatus, ClientExceptionCode clientExceptionCode, String message) {
        this.httpStatus = httpStatus;
        this.clientExceptionCode = clientExceptionCode;
        this.message = message;
    }
}
