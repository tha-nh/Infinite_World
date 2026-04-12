package com.infinite.common.dto.response;

import com.infinite.common.constant.StatusCode;
import com.infinite.common.util.I18n;
import org.springframework.stereotype.Component;

@Component
public class Response {

    private static I18n i18n;

    public Response(I18n i18n) {Response.i18n = i18n;}
    public static int code(StatusCode status) {return status.getCode();}
    public static String message(StatusCode status) {return i18n.msg(status.getMessage());}
    public static String message(String key) {return i18n.msg(key);}
}