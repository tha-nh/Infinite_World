package com.infinite.common.exception;

public class NotPermissionException extends RuntimeException {

    private final String code;

    public NotPermissionException(String code, Object... args) {
        super(code);
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}