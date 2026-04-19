package com.infinite.notification.service.impl;

import com.infinite.common.constant.EmailTemplate;
import com.infinite.common.constant.OtpConstant;
import com.infinite.common.util.MessageUtils;
import com.infinite.notification.dto.request.EmailRequest;
import com.infinite.notification.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import jakarta.mail.internet.MimeMessage;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailServiceImpl implements EmailService {
    
    private final JavaMailSender mailSender;
    
    @Value("${spring.mail.username}")
    private String fromEmail;
    
    @Async
    @Override
    public void sendEmail(EmailRequest request) {
        try {
            if (request.isHtml()) {
                sendHtmlEmail(request);
            } else {
                sendSimpleEmail(request);
            }
            log.info("Email sent successfully to: {}", request.getTo());
        } catch (Exception e) {
            log.error("Failed to send email to: {}", request.getTo(), e);
            throw new RuntimeException("Failed to send email", e);
        }
    }
    
    @Async
    @Override
    public void sendOtpEmail(String email, String otp, String type) {
        String subjectKey = getOtpSubjectKey(type);
        String purposeKey = getOtpPurposeKey(type);
        long expirationMinutes = getOtpExpirationMinutes(type);
        
        String subject = MessageUtils.getMessage(subjectKey);
        String purpose = MessageUtils.getMessage(purposeKey);
        String content = MessageUtils.getMessage(EmailTemplate.OTP_CONTENT, 
                purpose, otp, String.valueOf(expirationMinutes));
        
        EmailRequest request = EmailRequest.builder()
                .to(email)
                .subject(subject)
                .content(content)
                .isHtml(false)
                .build();
        sendEmail(request);
    }
    
    @Override
    public void sendVerificationEmail(String email, String verificationUrl) {
        try {
            String currentLang = org.springframework.context.i18n.LocaleContextHolder.getLocale().getLanguage();
            String verificationUrlWithLang = verificationUrl + "&lang=" + currentLang;
            
            String subject = MessageUtils.getMessage(EmailTemplate.VERIFICATION_SUBJECT);
            String approveButton = MessageUtils.getMessage(EmailTemplate.VERIFICATION_BUTTON_APPROVE);
            String rejectButton = MessageUtils.getMessage(EmailTemplate.VERIFICATION_BUTTON_REJECT);
            
            String htmlContent = buildVerificationEmailHtml(verificationUrlWithLang, approveButton, rejectButton);
            
            EmailRequest request = EmailRequest.builder()
                    .to(email)
                    .subject(subject)
                    .content(htmlContent)
                    .isHtml(true)
                    .build();
            
            if (request.isHtml()) {
                sendHtmlEmail(request);
            } else {
                sendSimpleEmail(request);
            }
            log.info("Verification email sent successfully to: {}", email);
        } catch (Exception e) {
            log.error("Failed to send verification email to: {}", email, e);
            throw new RuntimeException("Failed to send verification email", e);
        }
    }
    
    @Override
    public void sendPasswordResetEmail(String email, String otp) {
        sendOtpEmail(email, otp, "forgot_password");
    }
    
    private String buildVerificationEmailHtml(String verificationUrl, String approveText, String rejectText) {
        String greeting = MessageUtils.getMessage(EmailTemplate.VERIFICATION_GREETING);
        String description = MessageUtils.getMessage(EmailTemplate.VERIFICATION_DESCRIPTION);
        String footer = MessageUtils.getMessage(EmailTemplate.VERIFICATION_FOOTER);
        
        return "<!DOCTYPE html>" +
                "<html>" +
                "<head>" +
                "    <meta charset=\"UTF-8\">" +
                "    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">" +
                "    <style>" +
                "        * { margin: 0; padding: 0; box-sizing: border-box; }" +
                "        body { font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); padding: 40px 20px; }" +
                "        .email-container { max-width: 600px; margin: 0 auto; background: white; border-radius: 20px; overflow: hidden; box-shadow: 0 20px 60px rgba(0,0,0,0.3); }" +
                "        .header { background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); padding: 40px 30px; text-align: center; }" +
                "        .header h1 { color: white; font-size: 28px; margin-bottom: 10px; font-weight: 600; }" +
                "        .header p { color: rgba(255,255,255,0.9); font-size: 14px; }" +
                "        .content { padding: 40px 30px; }" +
                "        .content p { color: #555; font-size: 16px; line-height: 1.8; margin-bottom: 30px; }" +
                "        .icon-box { text-align: center; margin-bottom: 30px; }" +
                "        .icon-box svg { width: 80px; height: 80px; }" +
                "        .button-container { text-align: center; margin: 40px 0; }" +
                "        .button { display: inline-block; padding: 16px 40px; margin: 10px 8px; text-decoration: none; border-radius: 50px; font-weight: 600; font-size: 16px; transition: all 0.3s ease; box-shadow: 0 4px 15px rgba(0,0,0,0.2); }" +
                "        .button-approve { background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); color: white !important; }" +
                "        .button-reject { background: linear-gradient(135deg, #f093fb 0%, #f5576c 100%); color: white !important; }" +
                "        .footer { background: #f8f9fa; padding: 30px; text-align: center; border-top: 1px solid #e9ecef; }" +
                "        .footer p { color: #6c757d; font-size: 14px; line-height: 1.6; }" +
                "        .divider { height: 1px; background: linear-gradient(to right, transparent, #ddd, transparent); margin: 30px 0; }" +
                "        @media only screen and (max-width: 600px) {" +
                "            .header h1 { font-size: 24px; }" +
                "            .content { padding: 30px 20px; }" +
                "            .button { display: block; margin: 10px 0; }" +
                "        }" +
                "    </style>" +
                "</head>" +
                "<body>" +
                "    <div class=\"email-container\">" +
                "        <div class=\"header\">" +
                "            <h1>✉️ " + greeting + "</h1>" +
                "            <p>Infinite World Platform</p>" +
                "        </div>" +
                "        <div class=\"content\">" +
                "            <div class=\"icon-box\">" +
                "                <svg viewBox=\"0 0 24 24\" fill=\"none\" xmlns=\"http://www.w3.org/2000/svg\">" +
                "                    <circle cx=\"12\" cy=\"12\" r=\"10\" stroke=\"url(#gradient)\" stroke-width=\"2\"/>" +
                "                    <path d=\"M8 12L11 15L16 9\" stroke=\"url(#gradient)\" stroke-width=\"2\" stroke-linecap=\"round\" stroke-linejoin=\"round\"/>" +
                "                    <defs>" +
                "                        <linearGradient id=\"gradient\" x1=\"0%\" y1=\"0%\" x2=\"100%\" y2=\"100%\">" +
                "                            <stop offset=\"0%\" style=\"stop-color:#667eea;stop-opacity:1\" />" +
                "                            <stop offset=\"100%\" style=\"stop-color:#764ba2;stop-opacity:1\" />" +
                "                        </linearGradient>" +
                "                    </defs>" +
                "                </svg>" +
                "            </div>" +
                "            <p>" + description + "</p>" +
                "            <div class=\"divider\"></div>" +
                "            <div class=\"button-container\">" +
                "                <a href=\"" + verificationUrl + "&action=approve\" class=\"button button-approve\">✓ " + approveText + "</a>" +
                "                <a href=\"" + verificationUrl + "&action=reject\" class=\"button button-reject\">✗ " + rejectText + "</a>" +
                "            </div>" +
                "        </div>" +
                "        <div class=\"footer\">" +
                "            <p>" + footer + "</p>" +
                "            <p style=\"margin-top: 15px; font-size: 12px; color: #adb5bd;\">© 2026 Infinite World. All rights reserved.</p>" +
                "        </div>" +
                "    </div>" +
                "</body>" +
                "</html>";
    }
    
    private void sendSimpleEmail(EmailRequest request) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setTo(request.getTo());
        message.setSubject(request.getSubject());
        message.setText(request.getContent());
        mailSender.send(message);
    }
    
    private void sendHtmlEmail(EmailRequest request) throws Exception {
        MimeMessage mimeMessage = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");
        
        helper.setFrom(fromEmail);
        helper.setTo(request.getTo());
        helper.setSubject(request.getSubject());
        helper.setText(request.getContent(), true);
        
        mailSender.send(mimeMessage);
    }
    
    private String getOtpSubjectKey(String type) {
        return switch (type) {
            case "forgot_password" -> EmailTemplate.OTP_SUBJECT_FORGOT_PASSWORD;
            case "registration" -> EmailTemplate.OTP_SUBJECT_REGISTRATION;
            case "login" -> EmailTemplate.OTP_SUBJECT_LOGIN;
            default -> EmailTemplate.OTP_SUBJECT_VERIFICATION;
        };
    }
    
    private String getOtpPurposeKey(String type) {
        return switch (type) {
            case "forgot_password" -> EmailTemplate.OTP_PURPOSE_FORGOT_PASSWORD;
            case "registration" -> EmailTemplate.OTP_PURPOSE_REGISTRATION;
            case "login" -> EmailTemplate.OTP_PURPOSE_LOGIN;
            default -> EmailTemplate.OTP_PURPOSE_VERIFICATION;
        };
    }
    
    private long getOtpExpirationMinutes(String type) {
        return switch (type) {
            case "forgot_password" -> OtpConstant.DEFAULT_OTP_EXPIRATION_MINUTES;
            case "registration" -> OtpConstant.REGISTRATION_OTP_EXPIRATION_MINUTES;
            case "login" -> OtpConstant.LOGIN_OTP_EXPIRATION_MINUTES;
            default -> OtpConstant.DEFAULT_OTP_EXPIRATION_MINUTES;
        };
    }
}
