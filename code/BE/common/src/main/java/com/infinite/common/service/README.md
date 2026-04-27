# OTP Service Documentation

## Overview
OTP Service cung cấp chức năng tạo, lưu trữ và xác thực OTP (One-Time Password) sử dụng Redis làm storage backend.

## Features
- Tạo OTP 6 chữ số an toàn với SecureRandom
- Lưu trữ OTP trong Redis với thời gian hết hạn tùy chỉnh
- Xác thực OTP
- Hỗ trợ nhiều loại OTP khác nhau (forgot password, registration, login, phone/email verification)
- Exception handling chuyên dụng cho OTP

## Usage

### 1. Inject OtpService
```java
@Service
@RequiredArgsConstructor
public class YourService {
    private final OtpService otpService;
}
```

### 2. Generate và Store OTP
```java
// Tạo OTP
String otp = otpService.generateOtp();

// Lưu OTP với thời gian hết hạn tùy chỉnh
String otpKey = otpService.generateForgotPasswordOtpKey(email);
otpService.storeOtp(otpKey, otp, 5, TimeUnit.MINUTES);

// Hoặc sử dụng thời gian hết hạn mặc định (5 phút)
otpService.storeOtpWithDefaultExpiration(otpKey, otp);
```

### 3. Verify OTP
```java
String otpKey = otpService.generateForgotPasswordOtpKey(email);
boolean isValid = otpService.verifyOtp(otpKey, userInputOtp);

if (!isValid) {
    // Handle invalid OTP
    String storedOtp = otpService.getOtp(otpKey);
    if (storedOtp == null) {
        throw new OtpException.OtpExpiredException("OTP đã hết hạn");
    } else {
        throw new OtpException.OtpInvalidException("OTP không đúng");
    }
}

// Xóa OTP sau khi verify thành công
otpService.deleteOtp(otpKey);
```

### 4. Các loại OTP Key
```java
// Forgot Password
String key = otpService.generateForgotPasswordOtpKey(email);

// Registration
String key = otpService.generateRegistrationOtpKey(email);

// Login
String key = otpService.generateLoginOtpKey(identifier);

// Phone Verification
String key = otpService.generatePhoneVerificationOtpKey(phoneNumber);

// Email Verification
String key = otpService.generateEmailVerificationOtpKey(email);
```

## Configuration
OTP Service được tự động cấu hình thông qua `OtpConfig`. Chỉ cần đảm bảo có `RedisTemplate<String, String>` bean trong context.

## Constants
Sử dụng `OtpConstant` class để truy cập các constants:
- `OTP_LENGTH`: Độ dài OTP (6)
- `DEFAULT_OTP_EXPIRATION_MINUTES`: Thời gian hết hạn mặc định (5 phút)
- Các prefix cho các loại OTP khác nhau

## Exception Handling
- `OtpException.OtpExpiredException`: OTP đã hết hạn
- `OtpException.OtpInvalidException`: OTP không đúng
- `OtpException.OtpGenerationException`: Lỗi khi tạo OTP