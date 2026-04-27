package com.infinite.common.exception;

public class ResourceExistedException extends ExtendedRuntimeException {
	public ResourceExistedException(String code, Object... var2) {
		super(code, var2);
	}
}
