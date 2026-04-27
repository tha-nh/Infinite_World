package com.infinite.common.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO chứa dữ liệu để render trang HTML kết quả chung
 * Dùng cho các trang redirect từ email như verify registration, password reset, etc.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HtmlResultPageData {
    
    /**
     * Trạng thái thành công hay thất bại
     */
    private boolean success;
    
    /**
     * Tiêu đề chính của trang
     */
    private String title;
    
    /**
     * Tiêu đề phụ (optional)
     */
    private String subtitle;
    
    /**
     * Nội dung thông báo chính
     */
    private String message;
    
    /**
     * Hướng dẫn cho người dùng (optional)
     */
    private String instruction;
    
    /**
     * Ngôn ngữ hiển thị
     */
    @Builder.Default
    private String lang = "en";
    
    /**
     * Màu accent cho trang (success: green, error: red, warning: orange)
     */
    private String accentColor;
    
    /**
     * Label cho thông tin bổ sung (optional)
     */
    private String extraLabel;
    
    /**
     * Giá trị thông tin bổ sung (optional) - ví dụ: default password
     */
    private String extraValue;
    
    /**
     * Loại trang để áp dụng style phù hợp
     */
    @Builder.Default
    private String variant = "default";
    
    /**
     * Icon hiển thị (optional)
     */
    private String icon;
    
    /**
     * URL để redirect sau một khoảng thời gian (optional)
     */
    private String redirectUrl;
    
    /**
     * Thời gian redirect tính bằng giây (optional)
     */
    private Integer redirectDelay;
}