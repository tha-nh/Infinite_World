package com.infinite.common.exception;

public class ExtendedRuntimeException extends RuntimeException {

    private final String code;

    public ExtendedRuntimeException(String code, Object... args) {
        super(code);
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}