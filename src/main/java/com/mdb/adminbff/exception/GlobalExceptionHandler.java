package com.mdb.adminbff.exception;

import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.Objects;

@ControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    private static final String TRACE_ID_KEY = "traceId";

    @ExceptionHandler({ApiException.class})
    protected ResponseEntity<Object> handleApiException(ApiException ex) {
        return buildErrorResponseEntity(ex);
    }

    @ExceptionHandler(StatusRuntimeException.class)
    protected ResponseEntity<Object> handleStatusRuntimeException(StatusRuntimeException ex) {
        logger.error("gRPC error caught in GlobalExceptionHandler: {}", ex.getStatus());
        
        ApiErrorCode errorCode = mapGrpcStatusToErrorCode(ex.getStatus());
        ErrorResponse errorResponse = new ErrorResponse()
                .code(errorCode.getCode())
                .message(errorCode.getMessage())
                .traceId(MDC.get(TRACE_ID_KEY));

        return new ResponseEntity<>(errorResponse, errorCode.getHttpStatus());
    }

    @ExceptionHandler(Exception.class)
    protected ResponseEntity<Object> handleGeneralException(Exception ex) {
        logger.error("Unhandled exception caught: ", ex);
        
        ErrorResponse errorResponse = new ErrorResponse()
                .code(ApiErrorCode.UNHANDLED_ERROR.getCode())
                .message(ApiErrorCode.UNHANDLED_ERROR.getMessage())
                .traceId(MDC.get(TRACE_ID_KEY));

        return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    private ApiErrorCode mapGrpcStatusToErrorCode(Status status) {
        return switch (status.getCode()) {
            case NOT_FOUND -> ApiErrorCode.RESOURCE_NOT_FOUND;
            case UNAVAILABLE -> ApiErrorCode.UPSTREAM_SERVICE_UNAVAILABLE;
            case DEADLINE_EXCEEDED -> ApiErrorCode.UPSTREAM_SERVICE_TIMEOUT;
            case PERMISSION_DENIED, UNAUTHENTICATED -> ApiErrorCode.PERMISSION_DENIED;
            default -> ApiErrorCode.INTERNAL_UPSTREAM_ERROR;
        };
    }

    private ResponseEntity<Object> buildErrorResponseEntity(ApiException ex) {
        ErrorResponse errorResponse = new ErrorResponse();
        errorResponse.setTraceId(MDC.get(TRACE_ID_KEY));
        HttpStatus httpStatus = HttpStatus.INTERNAL_SERVER_ERROR;

        if (ex.getApiError() instanceof BaseApiError) {
            BaseApiError apiError = (BaseApiError) ex.getApiError();
            if (Objects.isNull(apiError.getErrorCode())) {
                errorResponse.setCode(apiError.getDynamicCode());
                errorResponse.setMessage(apiError.getDynamicMessage());
                errorResponse.setId(apiError.getId());
                httpStatus = apiError.getStatus();
            } else {
                errorResponse.setCode(apiError.getErrorCode().getCode());
                errorResponse.setMessage(apiError.getErrorCode().getMessage());
                errorResponse.setId(apiError.getId());
                httpStatus = apiError.getErrorCode().getHttpStatus();
            }
        } else {
            errorResponse.setCode(ApiErrorCode.BAD_REQUEST_GENERIC.getCode());
            errorResponse.setMessage(ApiErrorCode.BAD_REQUEST_GENERIC.getMessage());
        }
        return new ResponseEntity<>(errorResponse, httpStatus);
    }

    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(HttpMessageNotReadableException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        ErrorResponse errorResponse = new ErrorResponse();
        errorResponse.setTraceId(MDC.get(TRACE_ID_KEY));
        HttpStatus httpStatus = HttpStatus.BAD_REQUEST;
        errorResponse.setCode(ApiErrorCode.BAD_REQUEST_GENERIC.getCode());
        errorResponse.setMessage(ApiErrorCode.BAD_REQUEST_GENERIC.getMessage());

        return new ResponseEntity<>(errorResponse, httpStatus);
    }

}
