package com.viettel.civil.common.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;

@Getter
public enum StatusCode {
	SUCCESS(1000, "SUCCESS", HttpStatus.OK),
	DUPLICATE(1001, "DUPLICATE", HttpStatus.OK),
	INVALID_KEY(1002, "INVALID KEY", HttpStatus.BAD_REQUEST),
	BAD_REQUEST(1003, "BAD REQUEST", HttpStatus.BAD_REQUEST),
	DATA_NOT_EXISTED(1005, "DATA NOT EXISTED", HttpStatus.NOT_FOUND),
	DATA_EXISTED(1006, "DATA EXISTED", HttpStatus.CONFLICT),
	PARAM_NULL(1007, "PARAM NULL", HttpStatus.BAD_REQUEST),
	NOT_PERMIT(1008, "NOT_PERMIT", HttpStatus.BAD_REQUEST),
	BAD_REQUEST_BE_TRANSLATED(4001, "BAD_REQUEST_BE_TRANSLATED", HttpStatus.BAD_REQUEST),
	BAD_REQUEST_BE_TRANSLATED_OPEN_PU_INFO(4002, "BAD_REQUEST_BE_TRANSLATED_OPEN_PU_INFO", HttpStatus.BAD_REQUEST),
	BAD_REQUEST_BE_TRANSLATED_OPEN_PU_WARNING(4003, "BAD_REQUEST_BE_TRANSLATED_OPEN_PU_WARNING", HttpStatus.BAD_REQUEST),
	BAD_REQUEST_NEED_BE_TRANSLATED(4004, "BAD_REQUEST_NEED_BE_TRANSLATED", HttpStatus.BAD_REQUEST),
	UNCATEGORIZED_EXCEPTION(9999, "UNCATEGORIZED ERROR", HttpStatus.INTERNAL_SERVER_ERROR),
	INVALID_STATE(2001, "INVALID STATE", HttpStatus.BAD_REQUEST),
    API_KEY_NOT_FOUND(2002, "Api Key not found", HttpStatus.NOT_FOUND),
    STOP_ROUTE_FAILED(2003, "Failed to stop route",  HttpStatus.SERVICE_UNAVAILABLE),
    ROUTE_ERROR(2004, "ROUTE ERROR", HttpStatus.INTERNAL_SERVER_ERROR),
    ERROR_ADD_FILE(2005, "ERROR ADD FILE", HttpStatus.BAD_REQUEST),
    FILE_NOT_EXISTED(2006,"File không tồn tại",HttpStatus.NOT_FOUND),
    FILE_NOT_READ(2007,"Không thể đọc file",HttpStatus.BAD_REQUEST),
    FILE_NOT_DOWNLOAD(2008,"Không thể tải file",HttpStatus.BAD_REQUEST),
    FILE_NOT_DELETED(2009,"Không thể xóa file",HttpStatus.BAD_REQUEST),
    TABLE_NOT_EXIST(2010,"Bảng không tồn tại",HttpStatus.BAD_REQUEST),
    DATA_SYNC_FAIL(2011,"Yêu cầu đồng bộ dữ liệu thất bại",HttpStatus.BAD_REQUEST);


	private final int code;
	private final HttpStatusCode httpStatusCode;
	private String message;

	StatusCode(int code, String message, HttpStatusCode httpStatusCode) {
		this.code = code;
		this.message = message;
		this.httpStatusCode = httpStatusCode;
	}

	// Method to set dynamic message
	public StatusCode withMessage(String message) {
		this.message = message;
		return this;
	}
}
