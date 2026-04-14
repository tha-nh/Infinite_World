package com.infinite.common.constant;

import lombok.Getter;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;

@Getter
public enum StatusCode {

    // ================= SUCCESS =================
    SUCCESS(1000, "SUCCESS", HttpStatus.OK),

    // ================= COMMON ERROR =================
    BAD_REQUEST(1001, "BAD_REQUEST", HttpStatus.BAD_REQUEST),
    PARAM_NULL(1002, "PARAM_NULL", HttpStatus.BAD_REQUEST),
    INVALID_KEY(1003, "INVALID_KEY", HttpStatus.BAD_REQUEST),
    BAD_REQUEST_BE_TRANSLATED(4001, "BAD_REQUEST_BE_TRANSLATED", HttpStatus.BAD_REQUEST),
    UNAUTHORIZED(1004, "UNAUTHORIZED", HttpStatus.UNAUTHORIZED),
    NOT_PERMIT(1005, "NOT_PERMIT", HttpStatus.FORBIDDEN),
    DATA_NOT_EXISTED(1006, "DATA_NOT_EXISTED", HttpStatus.NOT_FOUND),
    DATA_EXISTED(1007, "DATA_EXISTED", HttpStatus.CONFLICT),
    DUPLICATE(1008, "DUPLICATE", HttpStatus.CONFLICT),
    LOCKED(1009, "LOCKED", HttpStatus.LOCKED),
    // ================= FILE =================
    FILE_NOT_EXISTED(2001, "FILE_NOT_EXISTED", HttpStatus.NOT_FOUND),
    FILE_NOT_READ(2002, "FILE_NOT_READ", HttpStatus.BAD_REQUEST),
    FILE_NOT_DOWNLOAD(2003, "FILE_NOT_DOWNLOAD", HttpStatus.BAD_REQUEST),
    FILE_NOT_DELETED(2004, "FILE_NOT_DELETED", HttpStatus.BAD_REQUEST),

    // ================= SYSTEM =================
    INTERNAL_ERROR(9999, "INTERNAL_ERROR", HttpStatus.INTERNAL_SERVER_ERROR);

    private final int code;
    private final HttpStatusCode httpStatusCode;
    private final String message;

    StatusCode(int code, String message, HttpStatusCode httpStatusCode) {
        this.code = code;
        this.message = message;
        this.httpStatusCode = httpStatusCode;
    }

}