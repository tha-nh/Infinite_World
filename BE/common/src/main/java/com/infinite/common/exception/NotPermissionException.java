package com.viettel.civil.common.exception;

import com.viettel.civil.common.utils.MessageUtils;
import lombok.Getter;

public class NotPermissionException extends RuntimeException {
	@Getter
	private String code;
	private String message;

	public NotPermissionException() {
	}

	public NotPermissionException(String code, Object... var2) {
		this.code = code;
		this.message = MessageUtils.getMessage(code, var2);
	}

	@Override
	public String getMessage() {
		return message;
	}

}
