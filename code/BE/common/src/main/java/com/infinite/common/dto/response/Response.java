package com.infinite.common.dto.response;

import com.infinite.common.constant.StatusCode;
import com.infinite.common.util.I18n;

public class Response {
    public static int code(StatusCode status) {return status.getCode();}
    public static String message(StatusCode status) {return I18n.msg(status.getMessage());}
    public static String message(String key) {return I18n.msg(key);}
}