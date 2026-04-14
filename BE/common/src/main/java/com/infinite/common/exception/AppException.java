package com.infinite.common.exception;

import com.infinite.common.constant.StatusCode;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AppException extends RuntimeException {
    private Object result;
	public AppException(StatusCode statusCode) {
		super(statusCode.getMessage());
		this.statusCode = statusCode;
	}

	public AppException(StatusCode statusCode, String mess) {
		super(mess);
		this.statusCode = statusCode;
	}

    public AppException(int code, String mess) {
        super(mess);
        this.statusCode = statusCode;
    }

    public AppException(StatusCode statusCode, Object result) {
        super(statusCode.getMessage());
        this.statusCode = statusCode;
        this.result = result;
    }

	private StatusCode statusCode;
}