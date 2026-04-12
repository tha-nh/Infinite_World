package com.viettel.civil.common.exception;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;

@Data
@EqualsAndHashCode(callSuper = true)
public class ValidateException extends RuntimeException {
	@Serial
	private static final long serialVersionUID = -7161560477076500424L;

	private String mess;
	private int errorCode = StatusCode.BAD_REQUEST_BE_TRANSLATED.getCode();
	private Object data;

	public ValidateException(String mess) {
		super(mess);
		this.mess = mess;
	}

	public ValidateException(String mess, int errorCode) {
		super(mess);
		this.mess = mess;
		this.errorCode = errorCode;
	}

	public ValidateException(String mess, int errorCode, Object data) {
		super(mess);
		this.mess = mess;
		this.errorCode = errorCode;
		this.data = data;
	}
}