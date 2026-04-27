package com.infinite.common.util;

import com.infinite.common.dto.response.HtmlResultPageData;
import org.springframework.stereotype.Component;

/**
 * Renderer để tạo HTML cho các trang kết quả chung
 * Dùng cho các trang redirect từ email như verify registration, password reset, etc.
 */
@Component
public class HtmlResultPageRenderer {
    
    /**
     * Render HTML page từ dữ liệu đầu vào
     */
    public String render(HtmlResultPageData data) {
        String accentColor = determineAccentColor(data);
        String icon = determineIcon(data);
        String redirectScript = buildRedirectScript(data);
        
        return String.format("""
            <!DOCTYPE html>
            <html lang="%s">
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>%s</title>
                <style>
                    * {
                        margin: 0;
                        padding: 0;
                        box-sizing: border-box;
                    }
                    
                    body {
                        font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Oxygen, Ubuntu, Cantarell, sans-serif;
                        background: linear-gradient(135deg, #667eea 0%%, #764ba2 100%%);
                        min-height: 100vh;
                        display: flex;
                        align-items: center;
                        justify-content: center;
                        padding: 20px;
                    }
                    
                    .container {
                        background: white;
                        border-radius: 16px;
                        box-shadow: 0 20px 40px rgba(0, 0, 0, 0.1);
                        padding: 40px;
                        max-width: 500px;
                        width: 100%%;
                        text-align: center;
                        animation: slideUp 0.6s ease-out;
                    }
                    
                    @keyframes slideUp {
                        from {
                            opacity: 0;
                            transform: translateY(30px);
                        }
                        to {
                            opacity: 1;
                            transform: translateY(0);
                        }
                    }
                    
                    .icon {
                        font-size: 64px;
                        margin-bottom: 20px;
                        color: %s;
                    }
                    
                    .title {
                        font-size: 28px;
                        font-weight: 700;
                        color: #2d3748;
                        margin-bottom: 12px;
                        line-height: 1.2;
                    }
                    
                    .subtitle {
                        font-size: 18px;
                        color: #4a5568;
                        margin-bottom: 20px;
                        font-weight: 500;
                    }
                    
                    .message {
                        font-size: 16px;
                        color: #4a5568;
                        line-height: 1.6;
                        margin-bottom: 24px;
                    }
                    
                    .extra-info {
                        background: #f7fafc;
                        border: 1px solid #e2e8f0;
                        border-radius: 8px;
                        padding: 16px;
                        margin: 20px 0;
                    }
                    
                    .extra-label {
                        font-size: 14px;
                        color: #718096;
                        margin-bottom: 8px;
                        font-weight: 600;
                    }
                    
                    .extra-value {
                        font-size: 16px;
                        color: #2d3748;
                        font-family: 'Courier New', monospace;
                        background: white;
                        padding: 8px 12px;
                        border-radius: 4px;
                        border: 1px solid #cbd5e0;
                        word-break: break-all;
                    }
                    
                    .instruction {
                        font-size: 14px;
                        color: #718096;
                        margin-top: 24px;
                        padding-top: 24px;
                        border-top: 1px solid #e2e8f0;
                        line-height: 1.5;
                    }
                    
                    .footer {
                        margin-top: 32px;
                        padding-top: 20px;
                        border-top: 1px solid #e2e8f0;
                        font-size: 12px;
                        color: #a0aec0;
                    }
                    
                    .redirect-info {
                        background: #ebf8ff;
                        border: 1px solid #bee3f8;
                        border-radius: 6px;
                        padding: 12px;
                        margin-top: 20px;
                        font-size: 14px;
                        color: #2b6cb0;
                    }
                    
                    @media (max-width: 480px) {
                        .container {
                            padding: 24px;
                            margin: 10px;
                        }
                        
                        .title {
                            font-size: 24px;
                        }
                        
                        .icon {
                            font-size: 48px;
                        }
                    }
                </style>
                %s
            </head>
            <body>
                <div class="container">
                    <div class="icon">%s</div>
                    
                    <h1 class="title">%s</h1>
                    
                    %s
                    
                    <div class="message">%s</div>
                    
                    %s
                    
                    %s
                    
                    %s
                    
                    <div class="footer">
                        © 2026 Infinite World. All rights reserved.
                    </div>
                </div>
            </body>
            </html>
            """,
            data.getLang(),
            data.getTitle(),
            accentColor,
            redirectScript,
            icon,
            data.getTitle(),
            buildSubtitle(data.getSubtitle()),
            data.getMessage(),
            buildExtraInfo(data.getExtraLabel(), data.getExtraValue()),
            buildInstruction(data.getInstruction()),
            buildRedirectInfo(data.getRedirectUrl(), data.getRedirectDelay())
        );
    }
    
    private String determineAccentColor(HtmlResultPageData data) {
        if (data.getAccentColor() != null) {
            return data.getAccentColor();
        }
        
        return data.isSuccess() ? "#48bb78" : "#f56565"; // green for success, red for error
    }
    
    private String determineIcon(HtmlResultPageData data) {
        if (data.getIcon() != null) {
            return data.getIcon();
        }
        
        return data.isSuccess() ? "✅" : "❌";
    }
    
    private String buildSubtitle(String subtitle) {
        if (subtitle == null || subtitle.trim().isEmpty()) {
            return "";
        }
        return String.format("<div class=\"subtitle\">%s</div>", subtitle);
    }
    
    private String buildExtraInfo(String label, String value) {
        if (label == null || value == null || label.trim().isEmpty() || value.trim().isEmpty()) {
            return "";
        }
        
        return String.format("""
            <div class="extra-info">
                <div class="extra-label">%s</div>
                <div class="extra-value">%s</div>
            </div>
            """, label, value);
    }
    
    private String buildInstruction(String instruction) {
        if (instruction == null || instruction.trim().isEmpty()) {
            return "";
        }
        return String.format("<div class=\"instruction\">%s</div>", instruction);
    }
    
    private String buildRedirectScript(HtmlResultPageData data) {
        if (data.getRedirectUrl() == null || data.getRedirectDelay() == null) {
            return "";
        }
        
        return String.format("""
            <script>
                setTimeout(function() {
                    window.location.href = '%s';
                }, %d);
            </script>
            """, data.getRedirectUrl(), data.getRedirectDelay() * 1000);
    }
    
    private String buildRedirectInfo(String redirectUrl, Integer redirectDelay) {
        if (redirectUrl == null || redirectDelay == null) {
            return "";
        }
        
        return String.format("""
            <div class="redirect-info">
                You will be redirected automatically in %d seconds...
            </div>
            """, redirectDelay);
    }
}