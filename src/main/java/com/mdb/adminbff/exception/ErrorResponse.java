package com.mdb.adminbff.exception;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(value = JsonInclude.Include.NON_NULL)
public class ErrorResponse implements Serializable {

    private String code;
    private String message;
    private String id;
    private String traceId;

    public ErrorResponse code(String code) {
        this.code = code;
        return this;
    }

    public ErrorResponse message(String message) {
        this.message = message;
        return this;
    }

    public ErrorResponse id(String id) {
        this.id = id;
        return this;
    }

    public ErrorResponse traceId(String traceId) {
        this.traceId = traceId;
        return this;
    }
}
