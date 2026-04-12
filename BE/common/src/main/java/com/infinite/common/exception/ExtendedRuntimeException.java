package com.viettel.civil.common.exception;

import com.viettel.civil.common.utils.MessageUtils;

public class ExtendedRuntimeException extends RuntimeException {
	private String code;
	private String message;

	public ExtendedRuntimeException() {
	}

	public ExtendedRuntimeException(String code, Object... var2) {
		this.code = code;
		this.message = MessageUtils.getMessage(code, var2);
	}

	@Override
	public String getMessage() {
		return message;
	}

	public String getCode() {
		return code;
	}
}
