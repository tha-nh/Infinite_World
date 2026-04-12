package com.infinite.common.exception;

public class NotFoundException extends ExtendedRuntimeException {
	public NotFoundException(String code, Object... var2) {
		super(code, var2);
	}
}
