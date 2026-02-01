package com.mdb.adminbff.exception;

public class ApiException extends RuntimeException {
    private ApiError apiError;

    public ApiException(ApiError apiError) {
        super(apiError.getMessage());
        this.apiError = apiError;
    }

    public ApiError getApiError() {
        return this.apiError;
    }
}
