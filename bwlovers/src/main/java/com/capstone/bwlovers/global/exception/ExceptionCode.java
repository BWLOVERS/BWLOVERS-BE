package com.capstone.bwlovers.global.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ExceptionCode {

    // 전체
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, ClientExceptionCode.INTERNAL_SERVER_ERROR, "예상치 못한 서버에러가 발생했습니다."),
    ILLEGAL_ARGUMENT(HttpStatus.BAD_REQUEST, ClientExceptionCode.ILLEGAL_ARGUMENT, "필수 파라미터 누락"),

    // Auth / OAuth
    LOGIN_ERROR(HttpStatus.UNAUTHORIZED, ClientExceptionCode.LOGIN_ERROR, "소셜 로그인 인증에 실패했습니다."),
    AUTH_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, ClientExceptionCode.AUTH_SERVER_ERROR, "인증 서버와 통신 중 오류가 발생했습니다."),
    AUTH_TOKEN_EMPTY(HttpStatus.UNAUTHORIZED, ClientExceptionCode.AUTH_TOKEN_EMPTY, "인증 토큰이 존재하지 않습니다. 다시 로그인해주세요."),
    ACCESS_TOKEN_EMPTY(HttpStatus.UNAUTHORIZED, ClientExceptionCode.ACCESS_TOKEN_EMPTY, "엑세스 토큰이 존재하지 않습니다."),
    REFRESH_TOKEN_EMPTY(HttpStatus.UNAUTHORIZED, ClientExceptionCode.REFRESH_TOKEN_EMPTY, "리프레시 토큰이 존재하지 않습니다."),
    AUTH_TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, ClientExceptionCode.AUTH_TOKEN_EXPIRED, "만료된 토큰입니다."),
    AUTH_TOKEN_INVALID(HttpStatus.UNAUTHORIZED, ClientExceptionCode.AUTH_TOKEN_INVALID, "올바르지 않은 토큰입니다."),
    AUTH_TOKEN_MISMATCH(HttpStatus.UNAUTHORIZED, ClientExceptionCode.AUTH_TOKEN_MISMATCH, "토큰 소유자가 일치하지 않습니다."),
    ALREADY_LOGGED_TOKEN(HttpStatus.UNAUTHORIZED, ClientExceptionCode.ALREADY_LOGGED_TOKEN, "이미 로그아웃된 토큰입니다."),
    FORBIDDEN(HttpStatus.FORBIDDEN, ClientExceptionCode.FORBIDDEN, "해당 자원에 대한 접근 권한이 없습니다."),

    // User
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, ClientExceptionCode.USER_NOT_FOUND, "존재하지 않는 회원입니다."),
    HEALTH_STATUS_NOT_FOUND(HttpStatus.NOT_FOUND, ClientExceptionCode.HEALTH_STATUS_NOT_FOUND, "산모 건강 정보가 아직 등록되지 않았습니다."),
    PREGNANCY_INFO_NOT_FOUND(HttpStatus.NOT_FOUND, ClientExceptionCode.PREGNANCY_INFO_NOT_FOUND, "산모 기본 정도가 아직 등록되지 않았습니다."),
    JOB_NOT_FOUND(HttpStatus.NOT_FOUND, ClientExceptionCode.JOB_NOT_FOUND, "존재하지 않는 직업입니다."),

    // AI
    AI_INVALID_REQUEST(HttpStatus.BAD_REQUEST, ClientExceptionCode.AI_INVALID_REQUEST, "AI 요청 데이터가 올바르지 않습니다."),
    AI_PROCESSING_FAILED(HttpStatus.CONFLICT, ClientExceptionCode.AI_PROCESSING_FAILED, "AI가 현재 조건으로 분석을 수행할 수 없습니다."),
    AI_SERVER_5XX(HttpStatus.INTERNAL_SERVER_ERROR, ClientExceptionCode.AI_SERVER_ERROR, "AI 서버 내부 오류가 발생했습니다."),
    AI_RESULT_NOT_FOUND(HttpStatus.NOT_FOUND, ClientExceptionCode.AI_RESULT_NOT_FOUND, "AI 분석 결과를 찾을 수 없습니다. (만료되었거나 존재하지 않음)"),
    AI_SAVE_EMPTY_SELECTION(HttpStatus.BAD_REQUEST, ClientExceptionCode.AI_SAVE_EMPTY_SELECTION, "저장할 특약이 선택되지 않았습니다."),
    AI_ALREADY_SAVED(HttpStatus.CONFLICT, ClientExceptionCode.AI_ALREADY_SAVED, "이미 저장된 AI 추천 결과입니다."),
    JSON_SERIALIZATION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, ClientExceptionCode.JSON_SERIALIZATION_FAILED, "데이터 직렬화 처리 중 오류가 발생했습니다.");

    private final HttpStatus httpStatus;
    private final ClientExceptionCode clientExceptionCode;
    private final String message;

    ExceptionCode(HttpStatus httpStatus, ClientExceptionCode clientExceptionCode, String message) {
        this.httpStatus = httpStatus;
        this.clientExceptionCode = clientExceptionCode;
        this.message = message;
    }
}
