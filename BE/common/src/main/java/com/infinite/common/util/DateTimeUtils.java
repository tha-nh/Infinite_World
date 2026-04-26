package com.infinite.common.util;

import com.infinite.common.constant.PatternConstant;
import org.apache.kafka.shaded.com.google.protobuf.Timestamp;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DateTimeUtils {
    private static final Logger log = LoggerFactory.getLogger(DateTimeUtils.class);

    public static final String DATE_TIME_PATTERN = "dd/MM/yyyy HH:mm";
    public static final String INSTANT_PATTERN = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'";
    public static final String DD = "dd";
    public static final String MM = "MM";
    public static final String YYYY = "yyyy";

    public static Instant toInstant(String timeStr) {
        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern(DATE_TIME_PATTERN, Locale.getDefault());
        LocalDateTime localDateTime = LocalDateTime.parse(timeStr, dateTimeFormatter);
        ZonedDateTime zonedDateTime = localDateTime.atZone(ZoneId.systemDefault());
        return zonedDateTime.toInstant();
    }

    public static String toString(Instant instant) {
        if (instant == null) {
            return "";
        }
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(DATE_TIME_PATTERN)
                .withZone(ZoneId.systemDefault());
        return formatter.format(instant);
    }

    /**
     * Format LocalDateTime to dd/MM/yyyy HH:mm string
     * @param localDateTime the LocalDateTime to format
     * @return formatted string in dd/MM/yyyy HH:mm format, empty string if input is null
     */
    public static String toString(LocalDateTime localDateTime) {
        if (localDateTime == null) {
            return "";
        }
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(DATE_TIME_PATTERN);
        return localDateTime.format(formatter);
    }

    public static String getCurrentDateTimeString() {
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss dd-MM-yyyy");
        return now.format(formatter);
    }

    public static String getCurrentDateTimeString(String patten) {
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(patten);
        return now.format(formatter);
    }

    public static Timestamp toTimestamp(Instant instant) {
        if (instant == null) {
            return null;
        }
        return Timestamp.newBuilder()
                .setSeconds(instant.getEpochSecond())
                .setNanos(instant.getNano())
                .build();
    }

    public static Timestamp toTimestamp(String instant) {
        if (instant == null) {
            return null;
        }
        try {
            DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern(INSTANT_PATTERN, Locale.getDefault());
            LocalDateTime localDateTime = LocalDateTime.parse(instant, dateTimeFormatter);
            ZonedDateTime zonedDateTime = localDateTime.atZone(ZoneId.systemDefault());
            return Timestamp.newBuilder()
                    .setSeconds(zonedDateTime.toInstant().getEpochSecond())
                    .setNanos(zonedDateTime.toInstant().getNano())
                    .build();
        } catch (Exception e) {
            return null;
        }
    }

    public static Instant combineDateTime(String time, String date) {
        try {
            // Define the formatters for time and date
            DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern(PatternConstant.DateTimeFormat.TIME_HMM);
            DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern(PatternConstant.DateTimeFormat.DATE_FULL);

            // Parse the time and date strings
            LocalTime localTime = LocalTime.parse(time, timeFormatter);
            LocalDate localDate = LocalDate.parse(date, dateFormatter);

            // Combine the date and time into a LocalDateTime
            LocalDateTime localDateTime = LocalDateTime.of(localDate, localTime);

            // Convert the LocalDateTime to an Instant
            return localDateTime.atZone(ZoneId.systemDefault()).toInstant();
        } catch (DateTimeParseException e) {
            // Handle parsing exception
            e.printStackTrace();
            return null;
        }
    }

    /**
     * combine LocalDateTime with formatter
     *
     * @param time
     * @param formatTime
     * @param date
     * @param formatDate
     * @return
     */
    public static LocalDateTime combineDateTime(String time, String formatTime, String date, String formatDate) {
        try {
            // Define the formatters for time and date
            DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern(formatTime);
            DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern(formatDate);

            // Parse the time and date strings
            LocalTime localTime = LocalTime.parse(time, timeFormatter);
            LocalDate localDate = LocalDate.parse(date, dateFormatter);

            // Combine the date and time into a LocalDateTime
            LocalDateTime localDateTime = localDate.atTime(localTime);

            // Convert the LocalDateTime to an Instant
            return localDateTime;
        } catch (DateTimeParseException e) {
            // Handle parsing exception
            e.printStackTrace();
            return null;
        }
    }

    /**
     * combine LocalDateTime with formatter default dd/MM/yyyy HH:mm:ss
     *
     * @param time
     * @param date
     * @return
     */
    public static LocalDateTime combineToLocalDateTime(String time, String date) {
        try {
            // Define the formatters for time and date
            DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern(PatternConstant.DateTimeFormat.TIME_FULL);
            DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern(PatternConstant.DateTimeFormat.DATE_FULL);

            // Parse the time and date strings
            LocalTime localTime = LocalTime.parse(time, timeFormatter);
            LocalDate localDate = LocalDate.parse(date, dateFormatter);

            // Combine the date and time into a LocalDateTime
            LocalDateTime localDateTime = localDate.atTime(localTime);

            // Convert the LocalDateTime to an Instant
            return localDateTime;
        } catch (DateTimeParseException e) {
            // Handle parsing exception
            e.printStackTrace();
            return null;
        }
    }

    /**
     * convert LocalDateTime to String
     *
     * @param date
     * @param pattern
     * @return
     */
    public static String localDateTimeToString(LocalDateTime date, String pattern) {
        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern(pattern);
            return date.format(formatter);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Định dạng không hợp lệ: " + pattern, e);
        }
    }

    /**
     * convert String to LocalDateTime
     *
     * @param dateStr
     * @param pattern
     * @return
     */
    public static LocalDateTime stringToLocalDateTime(String dateStr, String pattern) {
        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern(pattern);
        try {
            return LocalDateTime.parse(dateStr, dateTimeFormatter);
        } catch (DateTimeParseException e) {
            log.error("Invalid datetime format", e.getMessage());
            return null;
        }
    }

    /**
     * convert LocalDate to String
     *
     * @param date
     * @param pattern
     * @return
     */
    public static String localDateToString(LocalDate date, String pattern) {
        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern(pattern);
            return date.format(formatter);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Định dạng không hợp lệ: " + pattern, e);
        }
    }

    /**
     * convert String to LocalDate
     *
     * @param dateStr
     * @param pattern
     * @return
     */
    public static LocalDate stringToLocalDate(String dateStr, String pattern) {
        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern(pattern);
        try {
            return LocalDate.parse(dateStr, dateTimeFormatter);
        } catch (DateTimeParseException e) {
            log.error("Invalid date format", e.getMessage());
            return null;
        }
    }

    /**
     * convert String to LocalDate
     *
     * @param dateStr
     * @return
     */
    public static LocalDate stringToLocalDate(String dateStr) {
        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern(PatternConstant.DateTimeFormat.DATE_FULL);
        try {
            return LocalDate.parse(dateStr, dateTimeFormatter);
        } catch (DateTimeParseException e) {
            log.error("Invalid date format", e.getMessage());
            return null;
        }
    }

    /**
     * Ngày tháng ghi bằng chữ
     *
     * @param date
     * @return
     */
    public static String dateString(String date) {
        String outDate = "";
        if (date.length() == 10) {
            String ngay = date.substring(0, 2);
            String thang = date.substring(3, 5);
            String nam = date.substring(6, 10);
            outDate = "Ngày " + ngay + ", tháng " + thang + ", năm " + nam;
        }
        if (date.length() == 7) {
            String thang = date.substring(0, 2);
            String nam = date.substring(3, 7);
            outDate = "Tháng " + thang + ", năm " + nam;
        }
        if (date.length() == 4) {
            outDate = "Năm " + date;
        }
        return outDate;
    }

    /**
     * Xử lí ngày tháng khuyết thiếu để chuyển về ngày tháng thật
     *
     * @param date
     * @return
     */
    public static String handleDate(String date) {
        String result;
        DateTimeFormatter formatterFull = DateTimeFormatter.ofPattern(PatternConstant.DateTimeFormat.DATE_FULL);
        try {
            if (date.matches(PatternConstant.Patter.DATE_FORMATTER_FULL)) {
                // Full date format dd/MM/yyyy
                LocalDate localDate = LocalDate.parse(date, formatterFull);
                result = localDate.format(formatterFull);
            } else if (date.matches(PatternConstant.Patter.DATE_FORMATTER_YEAR_MONTH)) {
                // Year and month format MM/yyyy
                LocalDate localDate = LocalDate.parse("01/" + date, formatterFull);
                result = localDate.format(formatterFull);
            } else if (date.matches(PatternConstant.Patter.DATE_FORMATTER_YEAR)) {
                // Year format yyyy
                LocalDate localDate = LocalDate.parse("01/01/" + date, formatterFull);
                result = localDate.format(formatterFull);
            } else {
                // Invalid format
                result = null; // Default fallback
            }
        } catch (DateTimeException e) {
            // Handle invalid date cases
            result = null; // Default fallback
        }

        return result;
    }

    /**
     * Kiểm tra chuỗi có phải ngày tháng hợp lệ hay không và đúng format hay không
     *
     * @param dateStr
     * @param pattern
     * @return
     */
    public static boolean isValidateDate(String dateStr, String pattern) {
        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern(pattern);
        try {
            LocalDate date = LocalDate.parse(dateStr, dateTimeFormatter);
            return date.format(dateTimeFormatter).equals(dateStr);
        } catch (DateTimeParseException e) {
            return false;
        }
    }

    /**
     * Kiểm tra chuỗi có phải ngày tháng yyyy hoặc MM/yyyy hoặc dd/MM/yyyy
     *
     * @param dateStr
     * @return
     */
    public static boolean isValidateDate(String dateStr) {
        String YYYY_PATTERN = "^\\d{4}$";
        String MM_YYYY_PATTERN = "^(0[1-9]|1[0-2])\\/\\d{4}$";
        String DD_MM_YYYY_PATTERN = "^(0[1-9]|[12][0-9]|3[01])/(0[1-9]|1[0-2])/\\d{4}$";
        Pattern yyyyPattern = Pattern.compile(YYYY_PATTERN);
        Pattern mmYyyyPattern = Pattern.compile(MM_YYYY_PATTERN);
        Pattern ddMmYyyyPattern = Pattern.compile(DD_MM_YYYY_PATTERN);
        Matcher matcher;
        matcher = yyyyPattern.matcher(dateStr);
        if (matcher.matches()) {
            return true;
        }
        matcher = mmYyyyPattern.matcher(dateStr);
        if (matcher.matches()) {
            return true;
        }
        matcher = ddMmYyyyPattern.matcher(dateStr);
        return matcher.matches();
    }

    public static String convertDateToStringRegis(Date date, String pattern) {
        Calendar calendar = new GregorianCalendar();
        try {
            calendar.setTime(date);
            if (calendar.get(Calendar.MONTH) < 2) {
                return convertDateToString(date, pattern);
            } else {
                if ("MM".equals(pattern) || "M".equals(pattern)) {
                    pattern = "MM";
                }
                return convertDateToString(date, pattern);
            }
        } catch (Exception e) {
        }
        return "";
    }

    public static String convertDateToString(Date date, String pattern) {
        SimpleDateFormat dateFormat = new SimpleDateFormat(pattern);
        if (date == null) {
            return "";
        }
        try {
            return dateFormat.format(date);
        } catch (Exception e) {
            log.info("Can not parse " + date);
        }
        return "";
    }

    public static Date stringToDateByPattern(String rawValue, String pattern) {
        if (CommonUtils.isNullOrEmpty(rawValue) || CommonUtils.isNullOrEmpty(pattern)) return null;
        SimpleDateFormat sdf = new SimpleDateFormat(pattern);
        try {
            return sdf.parse(rawValue);
        } catch (ParseException e) {
            log.info(e.getMessage(), e);
        }
        return null;
    }

    public static LocalDate dateToLocalDate(Date date) {
        if (date == null) return null;
        Instant instant = Instant.ofEpochMilli(date.getTime());
        return LocalDate.ofInstant(instant, ZoneId.systemDefault());
    }

    public static Date localDateToDate(LocalDate localDate) {
        if (localDate == null) return null;
        return Date.from(localDate.atStartOfDay(ZoneId.systemDefault()).toInstant());
    }

    public static String convertDateToString(LocalDateTime date, String pattern) {
        DateTimeFormatter formatters = DateTimeFormatter.ofPattern(pattern);
        if (date == null) {
            return "";
        }
        try {
            return date.format(formatters);
        } catch (Exception e) {
            log.info("Can not parse " + date);
        }
        return "";
    }

    public static String convertDateToString(LocalDate date, String pattern) {
        DateTimeFormatter formatters = DateTimeFormatter.ofPattern(pattern);
        if (date == null) {
            return "";
        }
        try {
            return date.format(formatters);
        } catch (Exception e) {
            log.info("Can not parse " + date);
        }
        return "";
    }

    public static Instant localDateToInstant(LocalDate localDate) {
        if (localDate != null) {
            ZoneId zoneId = ZoneId.systemDefault(); // Lấy múi giờ hệ thống
            return localDate.atStartOfDay(zoneId).toInstant();
        }
        return null;
    }

    public static LocalDate instantToLocalDate(Instant instant) {
        if (instant != null) {
            return instant.atZone(ZoneId.systemDefault()).toLocalDate();
        }
        return null;
    }
}
