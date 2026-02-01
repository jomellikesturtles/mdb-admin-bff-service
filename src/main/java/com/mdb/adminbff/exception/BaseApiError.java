package com.mdb.adminbff.exception;

import org.springframework.http.HttpStatus;

public class BaseApiError extends ApiError {

    private ApiErrorCode errorCode;
    private String dynamicCode;
    private String dynamicMessage;
    private String id;

    public BaseApiError(ApiErrorCode errorCode) {
        super(errorCode.getHttpStatus(), errorCode.getMessage(), errorCode.getCode());
        this.errorCode = errorCode;
    }
    public BaseApiError(String dynamicCode, String dynamicMessage) {
        this(HttpStatus.INTERNAL_SERVER_ERROR, dynamicCode, dynamicMessage);
    }
    public BaseApiError(HttpStatus httpStatus, String dynamicCode, String dynamicMessage) {
        super(httpStatus, dynamicMessage, dynamicCode);
        this.dynamicCode = dynamicCode;
        this.dynamicMessage = dynamicMessage;
    }

    public BaseApiError(ApiErrorCode errorCode, String id) {
        super(errorCode.getHttpStatus(), errorCode.getCode(), errorCode.getMessage());
        this.errorCode = errorCode;
        this.dynamicCode = errorCode.getCode();
        this.dynamicMessage = errorCode.getMessage();
        this.id = id;
    }

    public ApiErrorCode getErrorCode() {
        return errorCode;
    }

    public void SetErrorCode(ApiErrorCode errorCode) {
        this.errorCode = errorCode;
    }

    public String getDynamicCode() {
        return dynamicCode;
    }
    public void setDynamicCode(String dynamicCode) {
        this.dynamicCode = dynamicCode;
    }
    public String getDynamicMessage() {
        return dynamicMessage;
    }
    public void setDynamicMessage(String dynamicMessage) {
        this.dynamicMessage = dynamicMessage;
    }
    public String getId() {
        return id;
    }
    public void setId(String id) {
        this.id = id;
    }
}
