package com.infinite.common.exception;

public class BadRequestException extends ExtendedRuntimeException {
    public BadRequestException(String code, Object... args) {
        super(code, args);
    }
}