package com.mdb.adminbff.exception;

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

	@ExceptionHandler({ApiException.class})
	protected ResponseEntity<Object> handleApiException(ApiException ex) {
		return buildErrorResponseEntity(ex);
	}

	private ResponseEntity<Object> buildErrorResponseEntity(ApiException ex) {
		ErrorResponse errorResponse = new ErrorResponse();
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
	protected ResponseEntity<Object> handleHttpMessageNotReadable(HttpMessageNotReadableException ex, HttpHeaders headers, HttpStatusCode status, WebRequest requet) {
		ErrorResponse errorResponse = new ErrorResponse();
		HttpStatus httpStatus = HttpStatus.BAD_REQUEST;
		errorResponse.setCode(ApiErrorCode.BAD_REQUEST_GENERIC.getCode());
		errorResponse.setMessage(ApiErrorCode.BAD_REQUEST_GENERIC.getMessage());

		return new ResponseEntity<>(errorResponse, httpStatus);
	}

}
