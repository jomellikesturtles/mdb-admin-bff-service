package com.mdb.adminbff.exception;

import lombok.ToString;
import org.springframework.http.HttpStatus;

@ToString
public enum ApiErrorCode {

    UNHANDLED_ERROR("UNHANDLED_ERROR", "Unhandled Error.", HttpStatus.INTERNAL_SERVER_ERROR),
    UNRECOGNIZED_REGION("ERR_UNRECOGNIZED_REGION", "Unrecognized region.", HttpStatus.BAD_REQUEST),
    ACTUATOR_DATE_EXCEPTION("ACTUATOR_DATE_EXCEPTION", "Actuator System Date Exception.", HttpStatus.BAD_REQUEST),
    UNRECOGNIZED_USER_MEDIA_DATA_TYPE("", "", HttpStatus.BAD_REQUEST),
    NOT_FOUND("NOT_FOUND", "Not found", HttpStatus.BAD_REQUEST),

    COMING_SOON("COMING_SOON", "Feature coming soon", HttpStatus.NO_CONTENT),
    FEATURE_DISABLED("FEATURE_DISABLED", "Feature disabled", HttpStatus.NO_CONTENT),

    BAD_REQUEST_GENERIC("BAD_REQUEST_GENERIC", "Invalid request", HttpStatus.BAD_REQUEST),
    TOKEN_INVALID("ERR_TOKEN_INVALID", "Token is invalid", HttpStatus.FORBIDDEN),
    DECRYPTION_FAILURE("ERR_DECRYPTION_FAILURE", "Decryption Failure", HttpStatus.BAD_REQUEST),
    ENCRYPTION_FAILURE("ERR_ENCRYPTION_FAILURE", "Encryption Failure", HttpStatus.BAD_REQUEST),

    // Upstream / gRPC Mappings
    UPSTREAM_SERVICE_UNAVAILABLE("ERR_UPSTREAM_UNAVAILABLE", "Upstream service is currently unavailable", HttpStatus.SERVICE_UNAVAILABLE),
    UPSTREAM_SERVICE_TIMEOUT("ERR_UPSTREAM_TIMEOUT", "Upstream service request timed out", HttpStatus.GATEWAY_TIMEOUT),
    RESOURCE_NOT_FOUND("ERR_RESOURCE_NOT_FOUND", "The requested resource was not found", HttpStatus.NOT_FOUND),
    PERMISSION_DENIED("ERR_PERMISSION_DENIED", "Permission denied for this operation", HttpStatus.FORBIDDEN),
    INTERNAL_UPSTREAM_ERROR("ERR_INTERNAL_UPSTREAM", "An internal error occurred in the upstream service", HttpStatus.BAD_GATEWAY),
    CONFLICT("ERR_CONFLICT", "Resource already exists", HttpStatus.CONFLICT);

    private final String code;
    private final String message;
    private final HttpStatus httpStatus;

    ApiErrorCode(String code, String message, HttpStatus httpStatus) {
        this.code = code;
        this.message = message;
        this.httpStatus = httpStatus;
    }

    public String getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }

    public HttpStatus getHttpStatus() {
        return httpStatus;
    }
}
