package com.infinite.common.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;

public class CommonUtils {
    public static Boolean isStrongPassword(String password) {
        if (password == null) {
            return false;
        }
        return (password.matches("^((?=(.*[a-z]){1,})(?=(.*[A-Z]){1,})(?=(.*[\\d]){1,})(?=(.*[\\W]){1,})(?!.*\\s)).{8,}$"));
    }

    public static String datePrintAndView(Date date, String day, String month, String year) {
        if (date != null) {
            Calendar calendar = new GregorianCalendar();
            calendar.setTime(date);
            if (calendar.get(Calendar.DAY_OF_MONTH) < 10) {
                day = "0" + calendar.get(Calendar.DAY_OF_MONTH);
            } else {
                day = String.valueOf(calendar.get(Calendar.DAY_OF_MONTH));
            }
            if ((calendar.get(Calendar.MONTH) + 1) < 10) {
                month = "0" + ((calendar.get(Calendar.MONTH) + 1));
            } else {
                month = String.valueOf((calendar.get(Calendar.MONTH) + 1));
            }
            year = String.valueOf(calendar.get(Calendar.YEAR));
            return "...................................., ngày " + day + " tháng " + month + " năm " + year;
        } else {
            return "...................................., ngày    tháng     năm     ";
        }
    }

    /**
     * Lay ten file
     *
     * @param input
     * @return
     */
    public static String getSafeFileName(String input) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            if (c != '/' && c != '\\' && c != 0) {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    private static String uuid = UUID.randomUUID().toString();
    private static final List<String> lstExceptLogFields = new ArrayList<>(Arrays.asList("rawPassword"));
    private static String uuidQR = "TOIYEUVNTOIYEUVN";
    public static String saltCheckSum = "TOIYEUVN";
    private static final String[] partern = {"à", "á", "ã", "ả", "ạ", "ằ", "ắ", "ẵ", "ẳ", "ặ", "ầ",
            "ấ", "ẫ", "ẩ", "ậ", "è", "é", "ẽ", "ẻ", "ẹ", "ề", "ế", "ễ", "ể", "ệ", "ì", "í", "ĩ", "ỉ",
            "ị", "ò", "ó", "õ", "ỏ", "ọ", "ồ", "ố", "ỗ", "ổ", "ộ", "ờ", "ớ", "ỡ", "ở", "ợ", "ù", "ú",
            "ũ", "ủ", "ụ", "ừ", "ứ", "ữ", "ử", "ự", "À", "Á", "Ã", "Ả", "Ạ", "Ằ", "Ắ", "Ẵ", "Ẳ", "Ặ",
            "Ầ", "Ấ", "Ẫ", "Ẩ", "Ậ", "È", "É", "Ẽ", "Ẻ", "Ẹ", "Ề", "Ế", "Ễ", "Ể", "Ệ", "Ì", "Í", "Ĩ",
            "Ỉ", "Ị", "Ò", "Ó", "Õ", "Ỏ", "Ọ", "Ồ", "Ố", "Ỗ", "Ổ",
            "Ộ", "Ờ", "Ớ", "Ỡ", "Ở", "Ợ", "Ù", "Ú", "Ũ", "Ủ", "Ụ", "Ừ", "Ứ", "Ữ", "Ử", "Ự"};
    private static final String[] destinct = {"à", "á", "ã", "ả", "ạ", "ằ", "ắ", "ẵ", "ẳ", "ặ", "ầ",
            "ấ", "ẵ", "ẩ", "ậ", "è", "é", "ẽ", "ẻ", "ẹ", "ề", "ế", "ễ", "ể", "ệ", "ì", "í", "ĩ", "ỉ",
            "ị", "ò", "ó", "õ", "ỏ", "ọ", "ồ", "ố", "ỗ", "ổ", "ộ", "ờ", "ớ", "ỡ", "ở", "ợ", "ù", "ú", "ũ",
            "ủ", "ụ", "ừ", "ứ", "ữ", "ử", "ự", "À", "Á", "Ã", "Ả", "Ạ", "Ằ", "Ắ", "Ẵ", "Ẳ", "Ặ", "Ầ", "Ấ",
            "Ẫ", "Ẩ", "Ậ", "È", "É", "Ẽ", "Ẻ", "Ẹ", "Ề", "Ế", "Ễ", "Ể", "Ệ", "Ì", "Í", "Ĩ", "Ỉ", "Ị", "Ò", "Ó", "Õ", "Ỏ", "Ọ", "Ồ",
            "Ố", "Ỗ", "Ổ", "Ộ", "Ờ", "Ớ", "Ỡ", "Ở", "Ợ", "Ù", "Ú", "Ũ", "Ủ", "Ụ", "Ừ", "Ứ", "Ữ", "Ử", "Ự"};
    private static final char[] SPECIAL_CHARACTERS = {' ', '!', '"', '#', '$', '%',
            '*', '+', ',', ':', '<', '=', '>', '?', '@', '[', '\\', ']', '^',
            '`', '|', '~', 'À', 'Á', 'Â', 'Ã', 'È', 'É', 'Ê', 'Ì', 'Í', 'Ò',
            'Ó', 'Ô', 'Õ', 'Ù', 'Ú', 'Ý', 'à', 'á', 'â', 'ã', 'è', 'é', 'ê',
            'ì', 'í', 'ò', 'ó', 'ô', 'õ', 'ù', 'ú', 'ý', 'Ă', 'ă', 'Đ', 'đ',
            'Ĩ', 'ĩ', 'Ũ', 'ũ', 'Ơ', 'ơ', 'Ư', 'ư', 'Ạ', 'ạ', 'Ả', 'ả', 'Ấ',
            'ấ', 'Ầ', 'ầ', 'Ẩ', 'ẩ', 'Ẫ', 'ẫ', 'Ậ', 'ậ', 'Ắ', 'ắ', 'Ằ', 'ằ',
            'Ẳ', 'ẳ', 'Ẵ', 'ẵ', 'Ặ', 'ặ', 'Ẹ', 'ẹ', 'Ẻ', 'ẻ', 'Ẽ', 'ẽ', 'Ế',
            'ế', 'Ề', 'ề', 'Ể', 'ể', 'Ễ', 'ễ', 'Ệ', 'ệ', 'Ỉ', 'ỉ', 'Ị', 'ị',
            'Ọ', 'ọ', 'Ỏ', 'ỏ', 'Ố', 'ố', 'Ồ', 'ồ', 'Ổ', 'ổ', 'Ỗ', 'ỗ', 'Ộ',
            'ộ', 'Ớ', 'ớ', 'Ờ', 'ờ', 'Ở', 'ở', 'Ỡ', 'ỡ', 'Ợ', 'ợ', 'Ụ', 'ụ',
            'Ủ', 'ủ', 'Ứ', 'ứ', 'Ừ', 'ừ', 'Ử', 'ử', 'Ữ', 'ữ', 'Ự', 'ự',};
    private static final char[] REPLACEMENTS = {'-', '\0', '\0', '\0', '\0', '\0',
            '\0', '_', '\0', '_', '\0', '\0', '\0', '\0', '\0', '\0', '_',
            '\0', '\0', '\0', '\0', '\0', 'A', 'A', 'A', 'A', 'E', 'E', 'E',
            'I', 'I', 'O', 'O', 'O', 'O', 'U', 'U', 'Y', 'a', 'a', 'a', 'a',
            'e', 'e', 'e', 'i', 'i', 'o', 'o', 'o', 'o', 'u', 'u', 'y', 'A',
            'a', 'D', 'd', 'I', 'i', 'U', 'u', 'O', 'o', 'U', 'u', 'A', 'a',
            'A', 'a', 'A', 'a', 'A', 'a', 'A', 'a', 'A', 'a', 'A', 'a', 'A',
            'a', 'A', 'a', 'A', 'a', 'A', 'a', 'A', 'a', 'E', 'e', 'E', 'e',
            'E', 'e', 'E', 'e', 'E', 'e', 'E', 'e', 'E', 'e', 'E', 'e', 'I',
            'i', 'I', 'i', 'O', 'o', 'O', 'o', 'O', 'o', 'O', 'o', 'O', 'o',
            'O', 'o', 'O', 'o', 'O', 'o', 'O', 'o', 'O', 'o', 'O', 'o', 'O',
            'o', 'U', 'u', 'U', 'u', 'U', 'u', 'U', 'u', 'U', 'u', 'U', 'u',
            'U', 'u',};

    public static String parseByte2HexStr(byte buf[]) {
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < buf.length; i++) {
            String hex = Integer.toHexString(buf[i] & 0xFF);
            if (hex.length() == 1) {
                hex = '0' + hex;
            }
            sb.append(hex.toUpperCase());
        }
        return sb.toString();
    }

    public static byte[] parseHexStr2Byte(String hexStr) {
        if (hexStr.length() < 1)
            return null;
        byte[] result = new byte[hexStr.length() / 2];
        for (int i = 0; i < hexStr.length() / 2; i++) {
            int high = Integer.parseInt(hexStr.substring(i * 2, i * 2 + 1),
                    16);
            int low = Integer.parseInt(
                    hexStr.substring(i * 2 + 1, i * 2 + 2), 16);
            result[i] = (byte) (high * 16 + low);
        }
        return result;
    }

    public static String encryptHexQrCode(String value) {
        if (value == null || "".equals(value)) {
            return "";
        }
        try {
            byte[] keyValue = uuidQR.substring(0, 16).getBytes("UTF-8");
            Cipher chiper = Cipher.getInstance("AES");
            Key key = new SecretKeySpec(keyValue, "AES");
            //System.out.println(keyValue.toString());
            chiper.init(Cipher.ENCRYPT_MODE, key);
            byte[] encVal = chiper.doFinal(value.getBytes());
            String encryptedValue = parseByte2HexStr(encVal);
//            String encryptedValue = new BASE64Encoder().encode(encVal);
            return encryptedValue;
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return "";

    }

    public static Long decryptHexQrCode(String encryptedValue) {
        try {
            byte[] keyValue = uuidQR.substring(0, 16).getBytes(StandardCharsets.UTF_8);
            Cipher chipper = Cipher.getInstance("AES");
            Key key = new SecretKeySpec(keyValue, "AES");

            chipper.init(Cipher.DECRYPT_MODE, key);
            if (encryptedValue == null || encryptedValue.isEmpty()) {
                return null;
            }

            byte[] decValue = chipper.doFinal(Objects.requireNonNull(parseHexStr2Byte(encryptedValue)));
            String decryptedValue = new String(decValue);

            return Long.parseLong(decryptedValue);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return null;

    }

    public static String getChangesAsString(String jsonBefore, Object objectAfter) throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        if ("".equals(jsonBefore)) {
            return "{}, " + gson().toJson(objectAfter);
        }
        JsonNode beforeNode = objectMapper.readTree(jsonBefore);
        String json = gson().toJson(objectAfter);
        JsonNode afterNode = objectMapper.readTree(json);

        Map<String, Object> changesBefore = new HashMap<>();
        Map<String, Object> changesAfter = new HashMap<>();

        // Duyệt qua tất cả các trường
        Iterator<String> fieldNames = beforeNode.fieldNames();
        while (fieldNames.hasNext()) {
            String fieldName = fieldNames.next();
            JsonNode beforeValue = beforeNode.get(fieldName);
            if (afterNode.has(fieldName)) {
                JsonNode afterValue = afterNode.get(fieldName);

                // So sánh và lưu giá trị cũ và mới nếu có sự khác biệt
                if (!equals(beforeValue, afterValue)) {
                    changesBefore.put(fieldName, beforeValue);
                    changesAfter.put(fieldName, afterValue);
                }
            }

        }

        // Chuyển đổi cả hai Map thành chuỗi JSON
        return objectMapper.writeValueAsString(changesBefore) + ", " + objectMapper.writeValueAsString(changesAfter);
    }

    private static boolean equals(JsonNode a, JsonNode b) {
        String aStr = a == null ? "" : a.toString();
        String bStr = b == null ? "" : b.toString();
        if ("null".equals(aStr)) {
            aStr = "";
        }
        if ("null".equals(bStr)) {
            bStr = "";
        }
        return aStr.equals(bStr);
    }

    public static String NVL(String str) {
        return str == null ? "" : str;
    }

    public static String NVLToString(Object o) {
        return o == null ? "" : o.toString();
    }

    public static Long NVL(Long str) {
        return str == null ? 0 : str;
    }

    public static int NVLVersionOffice(Integer versionOffice) {
        return versionOffice == null ? 2 : versionOffice;
    }

    public static long toEpochMilli(LocalDateTime localDateTime, ZoneId zoneId) {
        return localDateTime.atZone(zoneId).toInstant().toEpochMilli();
    }

    public static Gson gson() {
        return new GsonBuilder()
                .serializeNulls()
                .registerTypeAdapter(LocalDate.class, new LocalDateAdapter())
                .registerTypeAdapter(LocalDateTime.class, new LocalDateTimeAdapter())
                .registerTypeAdapter(Instant.class, new InstantAdapter())
                .registerTypeAdapter(String.class, new NullStringAdapter())
                .create();
    }

    public static Long getRandomLongPlusCurrentTime() {
        Random random = new Random();
        long randomLong = random.nextInt();
        long currentTimeMillis = System.currentTimeMillis();
        return randomLong + currentTimeMillis;
    }

    public static String trimString(String str) {
        if (str == null) {
            return null;
        }
        return str.trim();
    }

    public static String filter(String str) {
        if (str != null && !str.trim().isEmpty()) {
            return str.trim().replace("/", "//").replace("_", "/_").replace("%", "/%");
        }
        return null;
    }

    public static String toLowerCase(String str) {
        if (str == null) {
            return null;
        }
        return str.trim().toLowerCase();
    }

    public static String toUpperCase(String str) {
        if (str == null) {
            return null;
        }
        return str.trim().toUpperCase();
    }

    public static String stringFromInputStream(InputStream inputStream) throws IOException {
        ByteArrayOutputStream result = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int length;
        while ((length = inputStream.read(buffer)) != -1) {
            result.write(buffer, 0, length);
        }
        return result.toString(StandardCharsets.UTF_8);
    }

    public static String setGender(Long gender) {
        if (gender == null) return null;
        return switch (gender.intValue()) {
            case 1 -> "Nam";
            case 2 -> "Nữ";
            default -> null;
        };
    }

    public static boolean isNullOrEmpty(Object value) {
        if (value == null) return true;
        if (value instanceof Collection<?> collection) {
            return collection.isEmpty();
        }
        return value.toString().isEmpty();
    }

    public static boolean isContainNullOrEmpty(Collection<?> collection) {
        if (collection == null) return true;
        if (collection.isEmpty()) return true;
        for (Object object : collection) {
            if (object == null) return true;
        }
        return false;
    }

    public static String escapeWithStr(String s, String escapeStr) {
        if (isNullOrEmpty(s) || isNullOrEmpty(escapeStr)) return null;
        s =
                s
                        .trim()
                        .toUpperCase()
                        .replace("\\", escapeStr + "\\")
                        .replace("!", escapeStr + "!")
                        .replace("%", escapeStr + "%")
                        .replace("_", escapeStr + "_");
        return s;
    }

    public static String safeToStringIncludeNull(Object object) {
        if (object == null) return null;
        else return object.toString();
    }

    public static String stringJoinerNotNull(String delimiter, String... vars) {
        if (vars == null || vars.length == 0) return "";
        StringJoiner stringJoiner = new StringJoiner(delimiter);
        for (String var : vars) {
            if (!isNullOrEmpty(var)) {
                stringJoiner.add(var);
            }
        }
        return stringJoiner.toString();
    }

    public static List<String> checkIfNullOfObjectThenAddList(Object object, List<String> inKeyCheckNull) {
        List<String> outKeyNull = new ArrayList<>();
        Method[] methods = object.getClass().getMethods();
        Map<String, Method> methodMapName = new HashMap<>();
        for (Method method : methods) {
            String name = method.getName().toUpperCase();
            if (!methodMapName.containsKey(name)) {
                methodMapName.put(name, method);
            }
        }
        for (String key : inKeyCheckNull) {
            String keyTemp = ("get" + key).toUpperCase();
            Method method = methodMapName.get(keyTemp);
            if (method == null) continue;
            try {
                Object invoke = method.invoke(object);
                if (invoke == null) {
                    outKeyNull.add(key);
                }
            } catch (IllegalAccessException | InvocationTargetException e) {
                throw new RuntimeException(e);
            }
        }
        return outKeyNull;
    }
}
