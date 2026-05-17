# I18n JSON Configuration

## Overview
Hệ thống i18n đã được chuyển đổi từ properties files sang JSON files để dễ dàng quản lý và mở rộng.

## File Structure
```
i18n/
├── vi.json          # Tiếng Việt
├── en.json          # English
└── README.md        # Tài liệu này
```

## JSON Format

### Flat Keys (Backward Compatible)
Các key cũ từ properties vẫn hoạt động:
```json
{
  "SUCCESS": "Thành công",
  "auth.login.success": "Đăng nhập thành công"
}
```

### Nested Structure (Recommended)
Cấu trúc lồng nhau giúp tổ chức tốt hơn:
```json
{
  "auth": {
    "login": {
      "success": "Đăng nhập thành công",
      "fail": "Sai tài khoản hoặc mật khẩu"
    }
  }
}
```

Cả hai cách đều được hỗ trợ. Nested structure sẽ được tự động flatten thành `auth.login.success`.

## Usage

### 1. Basic Usage
```java
// Lấy message theo locale hiện tại
String message = MessageUtils.getMessage("auth.login.success");
```

### 2. With Parameters
```java
// Sử dụng MessageFormat pattern {0}, {1}, etc.
String message = MessageUtils.getMessage(
    "email.otp.content", 
    "đăng nhập",      // {0}
    "123456",         // {1}
    "5"               // {2}
);
// Result: "Mã OTP của bạn để đăng nhập là: 123456\n\nMã có hiệu lực trong 5 phút..."
```

### 3. Specific Language
```java
// Lấy message theo ngôn ngữ cụ thể
String message = MessageUtils.getMessage("auth.login.success", "en");
```

## Adding New Languages

1. Tạo file mới: `{language_code}.json` (ví dụ: `ja.json` cho tiếng Nhật)
2. Copy cấu trúc từ `vi.json` hoặc `en.json`
3. Dịch các giá trị
4. Restart application để load file mới

## Migration from Properties

Các file properties cũ vẫn tồn tại để tham khảo:
- `messages_vi.properties`
- `messages_en.properties`

Tất cả các key đã được chuyển đổi sang JSON với cấu trúc tương ứng.

## Key Naming Convention

### Dot Notation
Sử dụng dấu chấm (.) để phân cấp:
```
module.feature.action
```

### Examples
- `auth.login.success` - Authentication > Login > Success message
- `email.otp.subject.login` - Email > OTP > Subject > Login
- `file.upload.success` - File > Upload > Success message

## MessageFormat Patterns

Sử dụng `{0}`, `{1}`, `{2}`, ... cho parameters:

```json
{
  "user.greeting": "Xin chào {0}, bạn có {1} tin nhắn mới"
}
```

```java
MessageUtils.getMessage("user.greeting", "John", "5");
// Result: "Xin chào John, bạn có 5 tin nhắn mới"
```

## Fallback Strategy

1. Tìm message theo locale hiện tại (từ `LocaleContextHolder`)
2. Nếu không tìm thấy, fallback về ngôn ngữ mặc định (`vi`)
3. Nếu vẫn không tìm thấy, trả về key

## Hot Reload (Development)

Để reload messages trong development:
```java
@Autowired
private JsonMessageSource jsonMessageSource;

// Reload all messages
jsonMessageSource.reload();
```

## Best Practices

1. **Sử dụng nested structure** cho các key mới để dễ quản lý
2. **Đặt tên key có ý nghĩa** theo convention `module.feature.action`
3. **Sử dụng MessageFormat** cho dynamic content thay vì string concatenation
4. **Kiểm tra cả hai ngôn ngữ** khi thêm key mới
5. **Validate JSON** trước khi commit để tránh lỗi syntax

## Troubleshooting

### Message không hiển thị
- Kiểm tra key có tồn tại trong JSON file
- Kiểm tra JSON syntax có hợp lệ
- Kiểm tra locale đang được sử dụng
- Restart application nếu vừa thêm file mới

### MessageFormat error
- Đảm bảo số lượng parameters truyền vào khớp với pattern
- Sử dụng single quote `'` để escape special characters trong MessageFormat
