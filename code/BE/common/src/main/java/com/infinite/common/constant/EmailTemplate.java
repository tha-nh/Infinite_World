package com.infinite.common.constant;

public final class EmailTemplate {
    
    private EmailTemplate() {
    }
    
    public static final String OTP_SUBJECT_FORGOT_PASSWORD = "email.otp.subject.forgot_password";
    public static final String OTP_SUBJECT_REGISTRATION = "email.otp.subject.registration";
    public static final String OTP_SUBJECT_LOGIN = "email.otp.subject.login";
    public static final String OTP_SUBJECT_VERIFICATION = "email.otp.subject.verification";
    public static final String VERIFICATION_SUBJECT = "email.verification.subject";
    
    public static final String OTP_CONTENT = "email.otp.content";
    public static final String OTP_PURPOSE_FORGOT_PASSWORD = "email.otp.purpose.forgot_password";
    public static final String OTP_PURPOSE_REGISTRATION = "email.otp.purpose.registration";
    public static final String OTP_PURPOSE_LOGIN = "email.otp.purpose.login";
    public static final String OTP_PURPOSE_VERIFICATION = "email.otp.purpose.verification";
    
    public static final String VERIFICATION_GREETING = "email.verification.greeting";
    public static final String VERIFICATION_DESCRIPTION = "email.verification.description";
    public static final String VERIFICATION_BUTTON_APPROVE = "email.verification.button.approve";
    public static final String VERIFICATION_BUTTON_REJECT = "email.verification.button.reject";
    public static final String VERIFICATION_FOOTER = "email.verification.footer";
    public static final String VERIFICATION_SUCCESS = "email.verification.success";
    public static final String VERIFICATION_REJECT_MSG = "email.verification.reject.message";
    
    public static final String VERIFICATION_RESULT_SUCCESS_TITLE = "email.verification.result.success.title";
    public static final String VERIFICATION_RESULT_SUCCESS_SUBTITLE = "email.verification.result.success.subtitle";
    public static final String VERIFICATION_RESULT_ERROR_TITLE = "email.verification.result.error.title";
    public static final String VERIFICATION_RESULT_ERROR_SUBTITLE = "email.verification.result.error.subtitle";
    public static final String VERIFICATION_COPYRIGHT = "email.verification.copyright";
    
    public static final String VERIFICATION_CONTENT = "email.verification.content";
    
    // Password Reset Verification Email Templates
    public static final String PASSWORD_RESET_VERIFICATION_SUBJECT = "password.reset.verification.subject";
    public static final String PASSWORD_RESET_VERIFICATION_CONTENT = "password.reset.verification.content";
    public static final String PASSWORD_RESET_DEFAULT_PASSWORD = "password.reset.default.password";
    public static final String PASSWORD_RESET_WARNING_TITLE = "password.reset.warning.title";
    public static final String PASSWORD_RESET_WARNING_CONTENT = "password.reset.warning.content";
    public static final String PASSWORD_RESET_VERIFICATION_ACTION = "password.reset.verification.action";
    
    // Reuse existing verification templates
    public static final String EMAIL_VERIFICATION_APPROVE = "email.verification.button.approve";
    public static final String EMAIL_VERIFICATION_REJECT = "email.verification.button.reject";
    public static final String EMAIL_VERIFICATION_FOOTER = "email.verification.footer";
}