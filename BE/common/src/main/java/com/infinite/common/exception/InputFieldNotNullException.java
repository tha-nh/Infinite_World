package com.viettel.civil.common.exception;

public class InputFieldNotNullException extends ExtendedRuntimeException {
	public static final String ERROR_CODE = "error.input_fields_cannot_be_null";

	public InputFieldNotNullException(Object... var2) {
		super(ERROR_CODE, var2);
	}
}
