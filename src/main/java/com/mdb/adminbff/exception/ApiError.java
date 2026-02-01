package com.mdb.adminbff.exception;

import org.springframework.http.HttpStatus;

import java.util.Arrays;
import java.util.List;

public class ApiError {

    private HttpStatus status;

    @Deprecated
    private String message;
    private List<String> errors;
    private String source;

    public ApiError() {
    }

    public ApiError(HttpStatus status, String message, List<String> errors) {
        this.status = status;
        this.message = message;
        this.errors = errors;
    }

    public ApiError(HttpStatus status, String message, String errors) {
        this.status = status;
        this.message = message;
        this.errors = Arrays.asList(errors);
    }

    public ApiError(HttpStatus status, String message, String errors, String source) {
        this.status = status;
        this.message = message;
        this.errors = Arrays.asList(errors);
        this.source = source;
    }

    public HttpStatus getStatus() {
        return this.status;
    }

    public void setStatus(HttpStatus httpStatus) {
        this.status = httpStatus;
    }

    @Deprecated
    public String getMessage() {
        return this.message;
    }

    @Deprecated
    public void setMessage(String message) {
        this.message = message;
    }
    public List<String> getErrors() {
        return this.errors;
    }
    public void setErrors(List<String> errors) {
        this.errors = errors;
    }
}
