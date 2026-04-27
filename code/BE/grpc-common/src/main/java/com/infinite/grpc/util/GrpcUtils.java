package com.infinite.grpc.util;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class GrpcUtils {
    
    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
    
    public static String formatDateTime(LocalDateTime dateTime) {
        return dateTime != null ? dateTime.format(DATETIME_FORMATTER) : "";
    }
    
    public static LocalDateTime parseDateTime(String dateTimeStr) {
        try {
            return dateTimeStr != null && !dateTimeStr.isEmpty() 
                ? LocalDateTime.parse(dateTimeStr, DATETIME_FORMATTER) 
                : null;
        } catch (Exception e) {
            return null;
        }
    }
    
    public static String getCurrentDateTime() {
        return formatDateTime(LocalDateTime.now());
    }
}