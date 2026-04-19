package com.infinite.notification.service.impl;

import com.infinite.common.constant.EmailTemplate;
import com.infinite.common.constant.OtpConstant;
import com.infinite.common.util.MessageUtils;
import com.infinite.notification.dto.request.EmailRequest;
import com.infinite.notification.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import jakarta.mail.internet.MimeMessage;

import java.text.MessageFormat;
import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;
    private final MessageUtils messageUtils;

    @Value("${spring.mail.username}")
    private String fromEmail;
    
    @Value("${app.base-url:http://localhost:8080}")
    private String appBaseUrl;

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

        String htmlContent = buildSimpleEmailHtml(subject, content, otp);

        EmailRequest request = EmailRequest.builder()
                .to(email)
                .subject(subject)
                .content(htmlContent)
                .isHtml(true)
                .build();
        sendEmail(request);
    }

    @Override
    public void sendVerificationEmail(String email, String verificationUrl) {
        try {
            String subject = MessageUtils.getMessage(EmailTemplate.VERIFICATION_SUBJECT);
            String approveButton = MessageUtils.getMessage(EmailTemplate.VERIFICATION_BUTTON_APPROVE);
            String rejectButton = MessageUtils.getMessage(EmailTemplate.VERIFICATION_BUTTON_REJECT);

            String htmlContent = buildVerificationEmailHtml(verificationUrl, approveButton, rejectButton);

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
                "    <style>" +
                "        body { font-family: Arial, sans-serif; background: #f5f5f5; margin: 0; padding: 20px; }" +
                "        .container { max-width: 600px; margin: 0 auto; background: white; border-radius: 10px; padding: 30px; box-shadow: 0 2px 10px rgba(0,0,0,0.1); }" +
                "        .header { text-align: center; margin-bottom: 30px; }" +
                "        .title { color: #333; font-size: 24px; margin-bottom: 10px; }" +
                "        .content { color: #555; line-height: 1.6; margin-bottom: 30px; }" +
                "        .button-container { text-align: center; margin: 30px 0; }" +
                "        .btn { display: inline-block; padding: 12px 25px; margin: 0 10px; text-decoration: none; border-radius: 5px; font-weight: bold; }" +
                "        .btn-approve { background: #28a745; color: white; }" +
                "        .btn-reject { background: #dc3545; color: white; }" +
                "        .footer { text-align: center; color: #666; font-size: 14px; margin-top: 30px; }" +
                "    </style>" +
                "</head>" +
                "<body>" +
                "    <div class=\"container\">" +
                "        <div class=\"header\">" +
                "            <h1 class=\"title\">📧 " + greeting + "</h1>" +
                "        </div>" +
                "        <div class=\"content\">" +
                "            <p>" + description + "</p>" +
                "        </div>" +
                "        <div class=\"button-container\">" +
                "            <a href=\"" + verificationUrl + "&action=approve\" class=\"btn btn-approve\">✅ " + approveText + "</a>" +
                "            <a href=\"" + verificationUrl + "&action=reject\" class=\"btn btn-reject\">❌ " + rejectText + "</a>" +
                "        </div>" +
                "        <div class=\"footer\">" +
                "            <p>" + footer + "</p>" +
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

    @Override
    public void sendPasswordResetVerificationEmail(String email, String verificationUrl, String defaultPassword) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(email);
            helper.setSubject(MessageUtils.getMessage(EmailTemplate.PASSWORD_RESET_VERIFICATION_SUBJECT));

            String htmlContent = String.format("""
                            <!DOCTYPE html>
                            <html>
                            <head>
                                <meta charset="UTF-8">
                                <style>
                                    body { font-family: Arial, sans-serif; background: #f5f5f5; margin: 0; padding: 20px; }
                                    .container { max-width: 600px; margin: 0 auto; background: white; border-radius: 10px; padding: 30px; box-shadow: 0 2px 10px rgba(0,0,0,0.1); }
                                    .header { text-align: center; margin-bottom: 30px; }
                                    .title { color: #333; font-size: 24px; margin-bottom: 10px; }
                                    .content { color: #555; line-height: 1.6; margin-bottom: 30px; }
                                    .button-container { text-align: center; margin: 30px 0; }
                                    .btn { display: inline-block; padding: 12px 25px; margin: 0 10px; text-decoration: none; border-radius: 5px; font-weight: bold; }
                                    .btn-approve { background: #28a745; color: white; }
                                    .btn-reject { background: #dc3545; color: white; }
                                    .footer { text-align: center; color: #666; font-size: 14px; margin-top: 30px; }
                                </style>
                            </head>
                            <body>
                                <div class="container">
                                    <div class="header">
                                        <h1 class="title">🔐 %s</h1>
                                    </div>
                                    <div class="content">
                                        <p>%s</p>
                                        <p>%s</p>
                                    </div>
                                    <div class="button-container">
                                        <a href="%s&action=approve" class="btn btn-approve">✅ %s</a>
                                        <a href="%s&action=reject" class="btn btn-reject">❌ %s</a>
                                    </div>
                                    <div class="footer">
                                        <p>%s</p>
                                    </div>
                                </div>
                            </body>
                            </html>
                            """,
                    MessageUtils.getMessage(EmailTemplate.PASSWORD_RESET_VERIFICATION_SUBJECT),
                    MessageUtils.getMessage(EmailTemplate.PASSWORD_RESET_VERIFICATION_CONTENT),
                    MessageUtils.getMessage(EmailTemplate.PASSWORD_RESET_VERIFICATION_ACTION),
                    verificationUrl, MessageUtils.getMessage(EmailTemplate.EMAIL_VERIFICATION_APPROVE),
                    verificationUrl, MessageUtils.getMessage(EmailTemplate.EMAIL_VERIFICATION_REJECT),
                    MessageUtils.getMessage(EmailTemplate.EMAIL_VERIFICATION_FOOTER)
            );

            helper.setText(htmlContent, true);
            mailSender.send(message);

        } catch (Exception e) {
            throw new RuntimeException("Failed to send password reset verification email", e);
        }
    }

    private String buildSimpleEmailHtml(String title, String content, String otp) {
        return "<!DOCTYPE html>" +
                "<html>" +
                "<head>" +
                "    <meta charset=\"UTF-8\">" +
                "    <style>" +
                "        body { font-family: Arial, sans-serif; background: #f5f5f5; margin: 0; padding: 20px; }" +
                "        .container { max-width: 600px; margin: 0 auto; background: white; border-radius: 10px; padding: 30px; box-shadow: 0 2px 10px rgba(0,0,0,0.1); }" +
                "        .header { text-align: center; margin-bottom: 30px; }" +
                "        .title { color: #333; font-size: 24px; margin-bottom: 10px; }" +
                "        .content { color: #555; line-height: 1.6; margin-bottom: 30px; }" +
                "        .otp-box { background: #e3f2fd; border: 2px solid #2196f3; border-radius: 10px; padding: 20px; margin: 20px 0; text-align: center; }" +
                "        .otp { font-size: 32px; font-weight: bold; color: #1976d2; font-family: monospace; letter-spacing: 3px; }" +
                "        .footer { text-align: center; color: #666; font-size: 14px; margin-top: 30px; }" +
                "    </style>" +
                "</head>" +
                "<body>" +
                "    <div class=\"container\">" +
                "        <div class=\"header\">" +
                "            <h1 class=\"title\">🔐 " + title + "</h1>" +
                "        </div>" +
                "        <div class=\"content\">" +
                "            <p>" + content + "</p>" +
                "        </div>" +
                "        <div class=\"otp-box\">" +
                "            <div class=\"otp\">" + otp + "</div>" +
                "        </div>" +
                "        <div class=\"footer\">" +
                "            <p>© 2026 Infinite World. All rights reserved.</p>" +
                "        </div>" +
                "    </div>" +
                "</body>" +
                "</html>";
    }

    @Override
    @Async
    public void sendUserLockedEmail(String email, String username, LocalDateTime lockTime, String performedBy) {
        try {
            String subject = messageUtils.getMessage("email.user.locked.subject");
            String lockType = lockTime != null 
                ? messageUtils.getMessage("email.user.locked.temporary") + " " + lockTime 
                : messageUtils.getMessage("email.user.locked.permanent");
            String content = buildUserStatusChangeHtml(
                messageUtils.getMessage("email.user.locked.title"),
                MessageFormat.format(messageUtils.getMessage("email.user.locked.message"), username, lockType, performedBy),
                messageUtils.getMessage("email.user.locked.note"),
                "#dc3545"
            );
            
            EmailRequest request = EmailRequest.builder()
                .to(email)
                .subject(subject)
                .content(content)
                .isHtml(true)
                .build();
                
            sendHtmlEmail(request);
            log.info("User locked email sent to: {}", email);
        } catch (Exception e) {
            log.error("Failed to send user locked email to: {}", email, e);
        }
    }

    @Override
    @Async
    public void sendUserUnlockedEmail(String email, String username, String performedBy) {
        try {
            String subject = messageUtils.getMessage("email.user.unlocked.subject");
            String content = buildUserStatusChangeHtml(
                messageUtils.getMessage("email.user.unlocked.title"),
                MessageFormat.format(messageUtils.getMessage("email.user.unlocked.message"), username, performedBy),
                messageUtils.getMessage("email.user.unlocked.note"),
                "#28a745"
            );
            
            EmailRequest request = EmailRequest.builder()
                .to(email)
                .subject(subject)
                .content(content)
                .isHtml(true)
                .build();
                
            sendHtmlEmail(request);
            log.info("User unlocked email sent to: {}", email);
        } catch (Exception e) {
            log.error("Failed to send user unlocked email to: {}", email, e);
        }
    }

    @Override
    @Async
    public void sendUserAutoUnlockedEmail(String email, String username) {
        try {
            String subject = messageUtils.getMessage("email.user.auto.unlocked.subject");
            String content = buildUserStatusChangeHtml(
                messageUtils.getMessage("email.user.auto.unlocked.title"),
                MessageFormat.format(messageUtils.getMessage("email.user.auto.unlocked.message"), username),
                messageUtils.getMessage("email.user.auto.unlocked.note"),
                "#17a2b8"
            );
            
            EmailRequest request = EmailRequest.builder()
                .to(email)
                .subject(subject)
                .content(content)
                .isHtml(true)
                .build();
                
            sendHtmlEmail(request);
            log.info("User auto-unlocked email sent to: {}", email);
        } catch (Exception e) {
            log.error("Failed to send user auto-unlocked email to: {}", email, e);
        }
    }

    @Override
    @Async
    public void sendUserUpdatedEmail(String email, String username, String performedBy) {
        try {
            String subject = messageUtils.getMessage("email.user.updated.subject");
            String content = buildUserStatusChangeHtml(
                messageUtils.getMessage("email.user.updated.title"),
                MessageFormat.format(messageUtils.getMessage("email.user.updated.message"), username, performedBy),
                messageUtils.getMessage("email.user.updated.note"),
                "#ffc107"
            );
            
            EmailRequest request = EmailRequest.builder()
                .to(email)
                .subject(subject)
                .content(content)
                .isHtml(true)
                .build();
                
            sendHtmlEmail(request);
            log.info("User updated email sent to: {}", email);
        } catch (Exception e) {
            log.error("Failed to send user updated email to: {}", email, e);
        }
    }

    @Override
    @Async
    public void sendAccountVerificationEmail(String email, String username, String verificationToken, String userId) {
        try {
            String currentLang = LocaleContextHolder.getLocale().getLanguage();
            String subject = messageUtils.getMessage("email.account.verification.subject");
            String approveUrl = appBaseUrl + "/v1/api/auth/verify-registration?token=" + verificationToken + "&action=approve&lang=" + currentLang;
            String rejectUrl = appBaseUrl + "/v1/api/auth/verify-registration?token=" + verificationToken + "&action=reject&lang=" + currentLang;
            
            String content = buildAccountVerificationHtml(username, approveUrl, rejectUrl);
            
            EmailRequest request = EmailRequest.builder()
                .to(email)
                .subject(subject)
                .content(content)
                .isHtml(true)
                .build();
                
            sendHtmlEmail(request);
            log.info("Account verification email sent to: {} with locale: {}", email, currentLang);
        } catch (Exception e) {
            log.error("Failed to send account verification email to: {}", email, e);
        }
    }

    private String buildUserStatusChangeHtml(String title, String message, String note, String color) {
        String footer = messageUtils.getMessage("email.footer.auto");
        return String.format("""
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <title>%s</title>
                <style>
                    body { font-family: Arial, sans-serif; margin: 0; padding: 20px; background-color: #f5f5f5; }
                    .container { max-width: 600px; margin: 0 auto; background-color: white; padding: 30px; border-radius: 10px; box-shadow: 0 2px 10px rgba(0,0,0,0.1); }
                    .header { text-align: center; margin-bottom: 30px; }
                    .title { color: %s; font-size: 24px; font-weight: bold; margin-bottom: 10px; }
                    .message { font-size: 16px; line-height: 1.6; margin-bottom: 20px; color: #333; }
                    .note { font-size: 14px; color: #666; font-style: italic; }
                    .footer { margin-top: 30px; text-align: center; font-size: 12px; color: #999; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <div class="title">%s</div>
                    </div>
                    <div class="message">%s</div>
                    <div class="note">%s</div>
                    <div class="footer">
                        <p>%s</p>
                    </div>
                </div>
            </body>
            </html>
            """, title, color, title, message, note, footer);
    }

    private String buildAccountVerificationHtml(String username, String approveUrl, String rejectUrl) {
        String title = messageUtils.getMessage("email.account.verification.title");
        String greeting = MessageFormat.format(messageUtils.getMessage("email.account.verification.greeting"), username);
        String message = messageUtils.getMessage("email.account.verification.message");
        String approveBtn = messageUtils.getMessage("email.account.verification.approve");
        String rejectBtn = messageUtils.getMessage("email.account.verification.reject");
        String noteTitle = messageUtils.getMessage("email.account.verification.note.title");
        String noteApprove = messageUtils.getMessage("email.account.verification.note.approve");
        String noteReject = messageUtils.getMessage("email.account.verification.note.reject");
        String footer = messageUtils.getMessage("email.footer.auto");
        String footerIgnore = messageUtils.getMessage("email.account.verification.footer.ignore");
        
        return String.format("""
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <title>%s</title>
                <style>
                    body { font-family: Arial, sans-serif; margin: 0; padding: 20px; background-color: #f5f5f5; }
                    .container { max-width: 600px; margin: 0 auto; background-color: white; padding: 30px; border-radius: 10px; box-shadow: 0 2px 10px rgba(0,0,0,0.1); }
                    .header { text-align: center; margin-bottom: 30px; }
                    .title { color: #007bff; font-size: 24px; font-weight: bold; margin-bottom: 10px; }
                    .message { font-size: 16px; line-height: 1.6; margin-bottom: 30px; color: #333; }
                    .buttons { text-align: center; margin: 30px 0; }
                    .btn { display: inline-block; padding: 12px 30px; margin: 0 10px; text-decoration: none; border-radius: 5px; font-weight: bold; font-size: 16px; }
                    .btn-approve { background-color: #28a745; color: white; }
                    .btn-reject { background-color: #dc3545; color: white; }
                    .btn:hover { opacity: 0.8; }
                    .footer { margin-top: 30px; text-align: center; font-size: 12px; color: #999; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <div class="title">%s</div>
                    </div>
                    <div class="message">
                        %s <strong>%s</strong>,<br><br>
                        %s
                    </div>
                    <div class="buttons">
                        <a href="%s" class="btn btn-approve">%s</a>
                        <a href="%s" class="btn btn-reject">%s</a>
                    </div>
                    <div class="message">
                        <strong>%s</strong> %s<br>
                        %s
                    </div>
                    <div class="footer">
                        <p>%s</p>
                        <p>%s</p>
                    </div>
                </div>
            </body>
            </html>
            """, title, title, greeting, username, message, approveUrl, approveBtn, rejectUrl, rejectBtn, 
            noteTitle, noteApprove, noteReject, footer, footerIgnore);
    }
}