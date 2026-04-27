# I18n Service

I18n Service là một microservice độc lập để quản lý đa ngôn ngữ (internationalization) trong hệ thống.

## Tính năng

- **Quản lý message đa ngôn ngữ**: Lưu trữ và quản lý các message theo từng ngôn ngữ
- **Cache với Redis**: Sử dụng Redis để cache các message, tăng performance
- **Dynamic loading**: Load message từ properties file vào database
- **RESTful API**: Cung cấp API để CRUD message và quản lý cache
- **Hierarchical keys**: Hỗ trợ key có cấu trúc phân cấp (tối đa 10 level)
- **Soft delete**: Không xóa thật mà chỉ đánh dấu is_deleted

## Cấu hình

### Database
Service sử dụng PostgreSQL và tự động tạo bảng `i18n_{language}` cho mỗi ngôn ngữ.

### Redis
Sử dụng Redis để cache message với TTL mặc định 24 giờ.

### Properties
```properties
# Ngôn ngữ hỗ trợ
i18n.languages=en,vi

# Cache configuration
i18n.cache-prefix=i18n:
i18n.cache-expire-hours=24

# Properties file path
i18n.properties-file=/i18n/messages
```

## API Endpoints

### 1. Tạo/Cập nhật Message
```http
POST /v1/api/i18n/message?language=en
Content-Type: application/json

{
  "key": "user.profile.name",
  "message": "Full Name"
}
```

### 2. Xóa Message
```http
DELETE /v1/api/i18n/message?language=en&key=user.profile.name
```

### 3. Load từ Properties File
```http
POST /v1/api/i18n/load-from-properties?language=en
```

### 4. Refresh Cache
```http
POST /v1/api/i18n/refresh-cache
```

## Response Format

```json
{
  "code": 1000,
  "message": "SUCCESS",
  "result": "data"
}
```

### Status Codes
- `1000`: SUCCESS
- `1001`: BAD_REQUEST  
- `1003`: INVALID_KEY
- `1006`: DATA_NOT_EXISTED
- `9999`: INTERNAL_ERROR

## Cách sử dụng từ các service khác

Các service khác có thể gọi i18n service thông qua HTTP API hoặc sử dụng Redis cache trực tiếp:

```java
// Đọc từ Redis cache
String message = redisTemplate.opsForValue().get("i18n:en:user.profile.name");
```

## Deployment

Service chạy trên port `8082` và có thể deploy độc lập với Docker:

```bash
# Build
mvn clean package

# Run
java -jar target/i18n-service-1.0.0.jar
```

## Migration từ Common Module

Service này đã được tách ra từ common module để:
- Tăng tính độc lập và khả năng scale
- Giảm coupling giữa các service
- Dễ dàng maintain và phát triển tính năng mới
- Có thể sử dụng technology stack riêng nếu cần