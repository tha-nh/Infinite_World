package com.infinite.common.exception;

public class BadRequestException extends ExtendedRuntimeException {
	public BadRequestException(String code, Object... var2) {
		super(code, var2);
	}
}
