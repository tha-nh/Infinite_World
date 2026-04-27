package com.infinite.common.constant;

public class PatternConstant {
    public interface DateTimeFormat {
        String DATE_FULL = "dd/MM/yyyy";
        String DATE_FULL_OG = "yyyy-MM-dd";
        String DATE_YEAR_MONTH = "MM/yyyy";
        String DATE_YEAR = "yyyy";
        String DATE_FULL_STR = "dd 'tháng' MM 'năm' yyyy";

        String DATE_TIME_FULL = "dd/MM/yyyy HH:mm:ss";
        String DATE_TIME_HM = "dd/MM/yyyy HH:mm";

        String TIME_HM = "HH:mm";
        String TIME_HMM = "H:mm";
        String TIME_FULL = "HH:mm:ss";
    }

    public interface Patter {
        String DATE_FORMATTER_FULL = "\\d{1,2}/\\d{1,2}/\\d{4}";
        String DATE_FORMATTER_YEAR_MONTH = "\\d{1,2}/\\d{4}";
        String DATE_FORMATTER_YEAR = "\\d{4}";
    }
}
