package com.mdb.adminbff.exception;

import org.springframework.http.HttpStatus;

public enum BFFErrorCode {

    TOKEN_EXPIRED("", "", HttpStatus.FORBIDDEN);

    private String code;
    private String message;
    private HttpStatus httpStatus;
    BFFErrorCode(String code, String message, HttpStatus httpStatus) {
        this.code = code;
        this.message = message;
        this.httpStatus = httpStatus;
    }

    public String getMessage() {
        return  this.message;
    }
}
