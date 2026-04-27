# 👤 User Service

## 📋 Tổng quan

User Service quản lý toàn bộ lifecycle của người dùng trong hệ thống Infinite World, bao gồm authentication, authorization, profile management, và avatar handling.

---

## 🎯 Chức năng chính

### 1. **Authentication & Authorization**
- User registration với email verification
- Login/Logout với JWT tokens
- OAuth2 integration (Google, Facebook)
- Role-based access control (RBAC)
- Password reset và change password
- Account locking/unlocking mechanism

### 2. **User Management**
- CRUD operations cho users
- Profile management (update info, avatar)
- User search và filtering
- Pagination support
- Soft delete với audit trail

### 3. **Avatar Management** 
- Upload avatar qua gRPC (file-service)
- Automatic old avatar cleanup (async)
- Avatar URL caching
- Support multiple image formats

### 4. **Security Features**
- JWT-based authentication
- Password encryption (BCrypt)
- Account lockout after failed attempts
- Auto-unlock scheduler
- CORS configuration
- XSS protection

### 5. **Notification Integration**
- Kafka event publishing
- Email notifications (verification, password reset, OTP)
- WebSocket notifications

---

## 🏗️ Kiến trúc

### Module Structure
```
user-service/
├── src/main/java/com/infinite/user/
│   ├── client/                      # External service clients
│   │   ├── FileClient.java          # File service interface
│   │   └── impl/
│   │       ├── GrpcFileClientImpl.java  # gRPC implementation
│   │       └── FileClientImpl.java      # REST implementation (deprecated)
│   ├── config/                      # Configuration classes
│   │   ├── SecurityConfig.java      # Spring Security
│   │   ├── FileClientConfig.java    # File client bean
│   │   ├── AsyncConfig.java         # Async processing
│   │   ├── KafkaProducerConfig.java # Kafka setup
│   │   └── filter/                  # Security filters
│   ├── controller/rest/             # REST API endpoints
│   │   ├── UserController.java      # User CRUD
│   │   └── AuthController.java      # Authentication
│   ├── dto/                         # Data Transfer Objects
│   │   ├── request/                 # Request DTOs
│   │   └── response/                # Response DTOs
│   ├── model/                       # JPA Entities
│   │   ├── User.java                # User entity
│   │   └── Role.java                # Role entity
│   ├── repository/                  # Data access layer
│   │   ├── UserRepository.java
│   │   └── RoleRepository.java
│   ├── service/                     # Business logic
│   │   ├── UserService.java
│   │   ├── RoleService.java
│   │   └── impl/
│   ├── scheduler/                   # Scheduled tasks
│   │   └── UserUnlockScheduler.java # Auto-unlock accounts
│   └── util/                        # Utilities
│       ├── JwtUtil.java             # JWT operations
│       └── ValidationUtil.java      # Validation helpers
└── src/main/resources/
    ├── application.properties       # Configuration
    └── db/migration/                # Flyway migrations
```

---

## 🔧 Configuration

### Application Properties
```properties
# Server
server.port=8081
spring.application.name=user-service

# Database
spring.datasource.url=jdbc:postgresql://localhost:5432/user_db
spring.datasource.username=${DB_USERNAME:postgres}
spring.datasource.password=${DB_PASSWORD:postgres}

# JWT
jwt.secret=${JWT_SECRET:your-secret-key}
jwt.expiration=86400000  # 24 hours

# gRPC Client (file-service)
grpc.client.file-service.address=${GRPC_FILE_SERVICE_ADDRESS:static://localhost:9093}
grpc.client.file-service.negotiation-type=plaintext

# Kafka
spring.kafka.bootstrap-servers=${KAFKA_BOOTSTRAP_SERVERS:localhost:9092}
spring.kafka.producer.key-serializer=org.apache.kafka.common.serialization.StringSerializer
spring.kafka.producer.value-serializer=org.springframework.kafka.support.serializer.JsonSerializer

# Redis
spring.data.redis.host=${REDIS_HOST:localhost}
spring.data.redis.port=${REDIS_PORT:6379}

# Async
spring.task.execution.pool.core-size=5
spring.task.execution.pool.max-size=10
```

---

## 🚀 gRPC Integration

### File Service Communication

User service gọi file-service qua **gRPC** thay vì REST để tăng performance và type safety.

#### Architecture
```
UserServiceImpl
    ↓ (uses)
FileClient (interface)
    ↓ (implemented by)
GrpcFileClientImpl (adapter)
    ↓ (uses)
FileGrpcClient (grpc-common)
    ↓ (gRPC call)
FileServiceGrpcImpl (file-service)
```

#### Benefits
- ✅ **Type Safety**: Protobuf contract
- ✅ **Performance**: Binary protocol, faster than JSON/REST
- ✅ **Auto-config**: No manual endpoint configuration
- ✅ **Abstraction**: Business logic không biết về gRPC
- ✅ **Async Support**: Built-in async operations

#### Usage Example
```java
@Service
public class UserServiceImpl {
    
    private final FileClient fileClient;  // Abstraction
    
    public ApiResponse<UserResponse> updateAvatar(String userId, MultipartFile avatar) {
        // Upload new avatar via gRPC
        ApiResponse<FileUploadResponse> uploadResult = 
            fileClient.uploadFile(avatar, "avatars", userId);
        
        if (uploadResult.getCode() == StatusCode.SUCCESS.getCode()) {
            String newAvatarUrl = uploadResult.getResult().getFileUrl();
            
            // Update user
            user.setImageUrl(newAvatarUrl);
            userRepository.save(user);
            
            // Delete old avatar async
            if (oldAvatarUrl != null) {
                fileClient.deleteFileAsync(oldFileName, "avatars");
            }
        }
        
        return buildResponse(user);
    }
}
```

---

## 📝 API Endpoints & Testing

### Base URL
```
http://localhost:8081
```

---

### 🔐 Authentication Endpoints

#### 1. Register New User
```bash
curl -X POST http://localhost:8081/v1/api/auth/register \
  -H "Accept-Language: vi" \
  -F 'request={"username":"user123","password":"Password123!","name":"Nguyen Van A","email":"user@example.com","phone":"+84901234567"}' \
  -F 'avatar=@/path/to/avatar.jpg'
```

**Without Avatar:**
```bash
curl -X POST http://localhost:8081/v1/api/auth/register \
  -H "Accept-Language: vi" \
  -F 'request={"username":"user123","password":"Password123!","name":"Nguyen Van A","email":"user@example.com","phone":"+84901234567"}'
```

**Response:**
```json
{
  "code": 1000,
  "message": "Đăng ký thành công",
  "result": {
    "id": "uuid-here",
    "username": "user123",
    "email": "user@example.com",
    "name": "Nguyen Van A",
    "phone": "+84901234567",
    "status": "ACTIVE",
    "emailVerified": false,
    "createdAt": "2024-04-26T10:30:00"
  }
}
```

#### 2. Login
```bash
curl -X POST http://localhost:8081/v1/api/auth/login \
  -H "Content-Type: application/json" \
  -H "Accept-Language: en" \
  -d '{
    "username": "user123",
    "password": "Password123!"
  }'
```

**Response:**
```json
{
  "code": 1000,
  "message": "Login successful",
  "result": {
    "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "tokenType": "Bearer",
    "expiresIn": 86400,
    "user": {
      "id": "uuid-here",
      "email": "user@example.com",
      "fullName": "Nguyen Van A",
      "roles": ["USER"]
    }
  }
}
```

#### 3. Refresh Token
```bash
curl -X POST http://localhost:8081/api/v1/auth/refresh-token \
  -H "Content-Type: application/json" \
  -d '{
    "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
  }'
```

#### 4. Logout
```bash
curl -X POST http://localhost:8081/api/v1/auth/logout \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
```

#### 5. Forgot Password
```bash
curl -X POST http://localhost:8081/api/v1/auth/forgot-password \
  -H "Content-Type: application/json" \
  -d '{
    "email": "user@example.com"
  }'
```

#### 6. Reset Password
```bash
curl -X POST http://localhost:8081/api/v1/auth/reset-password \
  -H "Content-Type: application/json" \
  -d '{
    "token": "reset-token-here",
    "newPassword": "NewPassword123!"
  }'
```

#### 7. Verify Email
```bash
curl -X POST http://localhost:8081/api/v1/auth/verify-email \
  -H "Content-Type: application/json" \
  -d '{
    "token": "verification-token-here"
  }'
```

---

### 👤 User Management Endpoints

#### 8. Get All Users (Paginated)
```bash
curl -X GET "http://localhost:8081/api/v1/users?page=0&size=10&sort=createdAt,desc" \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..." \
  -H "Accept-Language: vi"
```

**Response:**
```json
{
  "code": 1000,
  "message": "Lấy danh sách người dùng thành công",
  "result": {
    "content": [
      {
        "id": "uuid-1",
        "email": "user1@example.com",
        "fullName": "Nguyen Van A",
        "phone": "+84901234567",
        "imageUrl": "http://localhost:9000/avatars/avatar1.jpg",
        "status": "ACTIVE",
        "roles": ["USER"]
      }
    ],
    "totalElements": 50,
    "totalPages": 5,
    "currentPage": 0,
    "pageSize": 10
  }
}
```

#### 9. Get User by ID
```bash
curl -X GET http://localhost:8081/api/v1/users/uuid-here \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
```

#### 10. Create User (Admin Only)
```bash
curl -X POST http://localhost:8081/api/v1/users \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..." \
  -H "Content-Type: application/json" \
  -d '{
    "email": "newuser@example.com",
    "password": "Password123!",
    "fullName": "Tran Thi B",
    "phone": "+84907654321",
    "roles": ["USER"]
  }'
```

#### 11. Update User
```bash
curl -X PUT http://localhost:8081/api/v1/users/uuid-here \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..." \
  -H "Content-Type: application/json" \
  -d '{
    "fullName": "Nguyen Van A Updated",
    "phone": "+84909999999",
    "status": "ACTIVE"
  }'
```

#### 12. Delete User (Soft Delete)
```bash
curl -X DELETE http://localhost:8081/api/v1/users/uuid-here \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
```

#### 13. Search Users
```bash
curl -X GET "http://localhost:8081/api/v1/users/search?keyword=nguyen&status=ACTIVE&page=0&size=10" \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
```

---

### 👨‍💼 Profile Management Endpoints

#### 14. Get Current User Profile
```bash
curl -X GET http://localhost:8081/api/v1/users/profile \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..." \
  -H "Accept-Language: vi"
```

**Response:**
```json
{
  "code": 1000,
  "message": "Lấy thông tin profile thành công",
  "result": {
    "id": "uuid-here",
    "email": "user@example.com",
    "fullName": "Nguyen Van A",
    "phone": "+84901234567",
    "imageUrl": "http://localhost:9000/avatars/20240426_avatar.jpg",
    "status": "ACTIVE",
    "emailVerified": true,
    "roles": ["USER"],
    "createdAt": "2024-04-26T10:30:00",
    "updatedAt": "2024-04-26T15:45:00"
  }
}
```

#### 15. Update Profile
```bash
curl -X PUT http://localhost:8081/api/v1/users/profile \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..." \
  -H "Content-Type: application/json" \
  -d '{
    "fullName": "Nguyen Van A Updated",
    "phone": "+84909999999"
  }'
```

#### 16. Upload Avatar (via gRPC to file-service)
```bash
curl -X POST http://localhost:8081/api/v1/users/avatar \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..." \
  -F "file=@/path/to/avatar.jpg"
```

**Response:**
```json
{
  "code": 1000,
  "message": "Upload avatar thành công",
  "result": {
    "id": "uuid-here",
    "email": "user@example.com",
    "fullName": "Nguyen Van A",
    "imageUrl": "http://localhost:9000/avatars/20240426_123456_abc123.jpg",
    "status": "ACTIVE"
  }
}
```

**Note:** Avatar được upload qua gRPC tới file-service, old avatar tự động xóa async.

#### 17. Delete Avatar
```bash
curl -X DELETE http://localhost:8081/api/v1/users/avatar \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
```

#### 18. Change Password
```bash
curl -X PUT http://localhost:8081/api/v1/users/password \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..." \
  -H "Content-Type: application/json" \
  -d '{
    "oldPassword": "Password123!",
    "newPassword": "NewPassword456!"
  }'
```

---

### 🔒 Admin Endpoints

#### 19. Lock User Account
```bash
curl -X POST http://localhost:8081/api/v1/users/uuid-here/lock \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..." \
  -H "Content-Type: application/json" \
  -d '{
    "reason": "Suspicious activity",
    "duration": 3600
  }'
```

#### 20. Unlock User Account
```bash
curl -X POST http://localhost:8081/api/v1/users/uuid-here/unlock \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
```

#### 21. Assign Role to User
```bash
curl -X POST http://localhost:8081/api/v1/users/uuid-here/roles \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..." \
  -H "Content-Type: application/json" \
  -d '{
    "roleId": "role-uuid-here"
  }'
```

#### 22. Remove Role from User
```bash
curl -X DELETE http://localhost:8081/api/v1/users/uuid-here/roles/role-uuid-here \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
```

---

### 📊 Statistics Endpoints

#### 23. Get User Statistics
```bash
curl -X GET http://localhost:8081/api/v1/users/statistics \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
```

**Response:**
```json
{
  "code": 1000,
  "message": "Success",
  "result": {
    "totalUsers": 1250,
    "activeUsers": 1100,
    "inactiveUsers": 100,
    "lockedUsers": 50,
    "newUsersToday": 15,
    "newUsersThisWeek": 87,
    "newUsersThisMonth": 342
  }
}
```

---

### 🧪 Testing Tips

#### Postman Collection Setup

1. **Create Environment Variables:**
   - `base_url`: `http://localhost:8081`
   - `access_token`: (will be set after login)

2. **Set Authorization:**
   - Type: Bearer Token
   - Token: `{{access_token}}`

3. **Auto-save Token Script** (in Login request Tests tab):
```javascript
var jsonData = pm.response.json();
if (jsonData.result && jsonData.result.accessToken) {
    pm.environment.set("access_token", jsonData.result.accessToken);
}
```

#### Test Flow

1. **Register** → Get user created
2. **Login** → Get access token (auto-saved)
3. **Get Profile** → Verify user info
4. **Upload Avatar** → Test gRPC integration
5. **Update Profile** → Test update
6. **Change Password** → Test security
7. **Logout** → Clean session

#### Common Headers

```
Content-Type: application/json
Accept-Language: vi (or en)
Authorization: Bearer {token}
```

#### Error Response Format

```json
{
  "code": 2001,
  "message": "User not found",
  "timestamp": "2024-04-26T10:30:00"
}
```

---

## 🔐 Security

### JWT Authentication
```java
// JWT Token Structure
{
  "sub": "user@example.com",
  "userId": "123",
  "roles": ["USER", "ADMIN"],
  "iat": 1234567890,
  "exp": 1234654290
}
```

### Role-Based Access Control
```java
@PreAuthorize("hasRole('ADMIN')")
public ApiResponse<List<UserResponse>> getAllUsers() {
    // Only admins can access
}

@PreAuthorize("hasAnyRole('USER', 'ADMIN')")
public ApiResponse<UserResponse> getProfile() {
    // Authenticated users can access
}
```

### Account Locking
- Lock after 5 failed login attempts
- Auto-unlock after 30 minutes (scheduler)
- Manual unlock by admin

---

## 📊 Database Schema

### User Table
```sql
CREATE TABLE users (
    id UUID PRIMARY KEY,
    email VARCHAR(255) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    full_name VARCHAR(255),
    phone VARCHAR(20),
    image_url VARCHAR(500),
    status VARCHAR(20),
    is_locked BOOLEAN DEFAULT FALSE,
    locked_until TIMESTAMP,
    failed_login_attempts INT DEFAULT 0,
    email_verified BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255)
);
```

### Role Table
```sql
CREATE TABLE roles (
    id UUID PRIMARY KEY,
    name VARCHAR(50) UNIQUE NOT NULL,
    description VARCHAR(255),
    created_at TIMESTAMP,
    updated_at TIMESTAMP
);

CREATE TABLE user_roles (
    user_id UUID REFERENCES users(id),
    role_id UUID REFERENCES roles(id),
    PRIMARY KEY (user_id, role_id)
);
```

---

## 🔄 Event Publishing

### Kafka Events
```java
// User created event
{
  "eventType": "USER_CREATED",
  "userId": "123",
  "email": "user@example.com",
  "timestamp": "2024-04-25T12:34:56"
}

// Email verification event
{
  "eventType": "EMAIL_VERIFICATION_REQUIRED",
  "userId": "123",
  "email": "user@example.com",
  "verificationToken": "abc123"
}

// Password reset event
{
  "eventType": "PASSWORD_RESET_REQUESTED",
  "userId": "123",
  "email": "user@example.com",
  "resetToken": "xyz789"
}
```

---

## 🧪 Testing

### Unit Tests
```bash
mvn test
```

### Integration Tests
```bash
mvn verify
```

### Test với gRPC
```java
@SpringBootTest
class UserServiceIntegrationTest {
    
    @MockBean
    private FileGrpcClient fileGrpcClient;
    
    @Test
    void testUploadAvatar() {
        // Mock gRPC response
        FileInfo mockFileInfo = FileInfo.newBuilder()
            .setFileUrl("http://example.com/avatar.jpg")
            .build();
            
        when(fileGrpcClient.uploadFile(any(), any(), any(), any(), any()))
            .thenReturn(mockFileInfo);
        
        // Test
        ApiResponse<UserResponse> response = userService.updateAvatar(userId, avatar);
        
        assertEquals(StatusCode.SUCCESS.getCode(), response.getCode());
    }
}
```

---

## 📈 Monitoring

### Health Check
```bash
curl http://localhost:8081/actuator/health
```

### Metrics
```bash
curl http://localhost:8081/actuator/metrics
```

### gRPC Channel Health
```bash
curl http://localhost:8081/actuator/health/grpcChannel
```

---

## 🚀 Deployment

### Docker
```dockerfile
FROM openjdk:21-jdk-slim
COPY target/user-service-1.0.0-SNAPSHOT.jar app.jar
ENTRYPOINT ["java", "-jar", "/app.jar"]
```

### Required Environment Variables

See `.env.properties` file in project root for all configuration keys.

**Critical Variables:**
- `DB_USERNAME`, `DB_PASSWORD` - Database credentials
- `SECRET_KEY` - JWT signing key
- `GRPC_FILE_SERVICE_PORT` - gRPC port for file-service
- `KAFKA_BOOTSTRAP_SERVERS` - Kafka connection
- `REDIS_HOST`, `REDIS_PORT` - Redis connection

---

## 🔧 Troubleshooting

### Issue: gRPC connection failed
```
io.grpc.StatusRuntimeException: UNAVAILABLE
```
**Solution**: Check file-service is running và port 18083 accessible
```
io.grpc.StatusRuntimeException: UNAVAILABLE
```
**Solution**: Check file-service is running và port 18083 accessible

### Issue: JWT token invalid
```
401 Unauthorized
```
**Solution**: Check JWT secret matches và token chưa expired

### Issue: Avatar upload failed
```
GrpcClientException: code=2002
```
**Solution**: Check file-service logs và MinIO connection

---

## 📚 Dependencies

- Spring Boot 3.x (4.0.5)
- Spring Security
- Spring Data JPA
- PostgreSQL
- Redis
- Kafka
- gRPC (grpc-common with grpc-spring-boot-starter 3.1.0.RELEASE)
- JWT (jjwt)
- Lombok

**Version Compatibility Note:**
- Project uses Spring Boot `4.0.5` (Spring Boot 3.x series)
- grpc-spring-boot-starter `3.1.0.RELEASE` is compatible
- If upgrading Spring Boot, verify grpc-spring-boot-starter compatibility

---

## 🎯 Future Enhancements

- [ ] OAuth2 với nhiều providers (GitHub, Twitter)
- [ ] Two-factor authentication (2FA)
- [ ] User activity logging
- [ ] Advanced search với Elasticsearch
- [ ] Rate limiting
- [ ] API versioning
- [ ] GraphQL support

---

## 🚀 Quick Start Testing Guide

### 1. Start Required Services
```bash
# PostgreSQL (port 5432)
# Redis (port 6379)
# Kafka (port 9092)
# MinIO (port 9000)
# file-service with gRPC (port 18083)
# user-service (port 8081)
```

### 2. Quick Test Flow
```bash
# Step 1: Register (with avatar)
curl -X POST http://localhost:8081/v1/api/auth/register \
  -H "Accept-Language: vi" \
  -F 'request={"username":"testuser","password":"Test123!","name":"Test User","email":"test@example.com"}' \
  -F 'avatar=@avatar.jpg'

# Step 2: Register (without avatar)
curl -X POST http://localhost:8081/v1/api/auth/register \
  -H "Accept-Language: vi" \
  -F 'request={"username":"testuser2","password":"Test123!","name":"Test User 2","email":"test2@example.com"}'

# Step 3: Login (save the token)
curl -X POST http://localhost:8081/v1/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"testuser","password":"Test123!"}'

# Step 4: Get Profile (use token from step 3)
curl -X GET http://localhost:8081/api/v1/users/profile \
  -H "Authorization: Bearer YOUR_TOKEN_HERE"

# Step 5: Upload Avatar (test gRPC integration)
curl -X POST http://localhost:8081/api/v1/users/avatar \
  -H "Authorization: Bearer YOUR_TOKEN_HERE" \
  -F "file=@avatar.jpg"
```

### 3. Import to Postman
1. Create new collection "User Service"
2. Add environment variable `base_url` = `http://localhost:8081`
3. Copy curl commands above
4. Import as cURL
5. Set up auto-save token script in Login request

### 4. Verify gRPC Integration
- Upload avatar → Check file-service logs for gRPC call on port 18083
- Old avatar should be deleted async
- New avatar URL should be in user profile

---

**Version**: 1.0.0  
**Status**: Production Ready  
**Last Updated**: 2024-04-26
