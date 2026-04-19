package com.infinite.common.constant;

public final class OtpConstant {
    
    private OtpConstant() {
    }
    
    public static final String FORGOT_PASSWORD_OTP_PREFIX = "forgot_password_otp:";
    public static final String REGISTRATION_OTP_PREFIX = "registration_otp:";
    public static final String LOGIN_OTP_PREFIX = "login_otp:";
    public static final String PHONE_VERIFICATION_OTP_PREFIX = "phone_verification_otp:";
    public static final String EMAIL_VERIFICATION_OTP_PREFIX = "email_verification_otp:";
    
    public static final int OTP_LENGTH = 6;
    public static final int OTP_MIN_VALUE = 100000;
    public static final int OTP_MAX_VALUE = 999999;
    
    public static final long DEFAULT_OTP_EXPIRATION_MINUTES = 10;
    public static final long REGISTRATION_OTP_EXPIRATION_MINUTES = 1;
    public static final long LOGIN_OTP_EXPIRATION_MINUTES = 5;
    public static final long PHONE_VERIFICATION_OTP_EXPIRATION_MINUTES = 5;
    public static final long EMAIL_VERIFICATION_OTP_EXPIRATION_MINUTES = 15;
}