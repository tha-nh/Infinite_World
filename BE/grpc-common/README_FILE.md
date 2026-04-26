# 🚀 gRPC Implementation cho File Service

> **⚠️ ENCODING NOTE**: File này cần được save as UTF-8. Nếu thấy ký tự lỗi (mojibake), vui lòng re-save với UTF-8 encoding trong editor của bạn.

## 📋 Tổng quan

Tài liệu này mô tả việc triển khai gRPC support cho File Service với **common infrastructure** có thể tái sử dụng cho nhiều services.

---

## 🎯 Mục tiêu

- **Common Infrastructure**: Tạo reusable components trong `grpc-common` cho tất cả services
- **Tách biệt**: gRPC module chỉ làm trung gian, business logic vẫn ở service
- **Centralized Error Handling**: Exception translation tự động, không cần manual try/catch
- **i18n Support**: Multi-language support qua gRPC metadata
- **Context Propagation**: Request-id, user-id flow through service calls
- **Không ảnh hưởng**: REST API hoạt động độc lập

---

## 🏗️ Kiến trúc

### 1. Module Structure

```
grpc-common/                                    # Common gRPC infrastructure
├── src/main/proto/
│   └── file_service.proto                      # gRPC contract
├── src/main/java/com/infinite/grpc/
│   ├── constant/
│   │   ├── GrpcMetadataKeys.java              # Common metadata keys
│   │   └── GrpcServiceNames.java              # Service naming constants
│   ├── interceptor/
│   │   ├── GrpcExceptionTranslator.java       # Centralized exception handling
│   │   ├── GrpcLocaleInterceptor.java         # i18n support
│   │   ├── GrpcContextInboundInterceptor.java # Context extraction
│   │   └── GrpcContextOutboundInterceptor.java # Context propagation
│   ├── service/file/
│   │   ├── FileServiceGrpc.java               # Interface contract
│   │   └── impl/
│   │       └── FileServiceGrpcImpl.java       # gRPC implementation (no try/catch!)
│   └── util/
│       ├── GrpcUtils.java                     # Utilities
│       └── GrpcMultipartFile.java             # MultipartFile adapter
└── .editorconfig                               # Encoding configuration

file-service/                                   # Business service
├── src/main/java/com/infinite/file/
│   ├── service/impl/
│   │   └── FileServiceImpl.java               # Business logic
│   └── controller/
│       └── FileController.java                # REST API
└── pom.xml                                     # grpc-common dependency
```

### 2. Common Infrastructure Components

#### GrpcMetadataKeys
```java
// Centralized metadata keys - prevents typos
public static final Metadata.Key<String> ACCEPT_LANGUAGE = ...;
public static final Metadata.Key<String> REQUEST_ID = ...;
public static final Metadata.Key<String> USER_ID = ...;
```

#### GrpcExceptionTranslator
```java
// Automatic exception mapping - no manual try/catch needed
AppException → gRPC Status (NOT_FOUND, INVALID_ARGUMENT, etc.)
IllegalArgumentException → INVALID_ARGUMENT
Exception → INTERNAL
```

#### GrpcLocaleInterceptor
```java
// Automatic i18n support
// Reads accept-language from metadata
// Sets LocaleContextHolder for message() calls
```

#### GrpcContextInboundInterceptor
```java
// Extracts request-id, user-id from metadata
// Sets in MDC for logging
```

---

## 🔧 Configuration

### Environment Variables (.env.properties)
```properties
GRPC_USER_SERVICE_PORT=18081
GRPC_NOTIFICATION_SERVICE_PORT=18082
GRPC_FILE_SERVICE_PORT=18083
```

### Application Properties (file-service)
```properties
# gRPC Server
grpc.server.port=${GRPC_FILE_SERVICE_PORT:18083}
grpc.server.address=0.0.0.0
grpc.server.reflection-service-enabled=true

# Service naming
spring.application.name=file-service

# gRPC Clients (for calling other services)
grpc.client.user-service.address=static://localhost:${GRPC_USER_SERVICE_PORT:18081}
grpc.client.user-service.negotiation-type=plaintext
```

---

## 📝 gRPC Service Definition

### file_service.proto
```protobuf
syntax = "proto3";

option java_multiple_files = true;
option java_package = "com.infinite.file.grpc";

service FileServiceRpc {
  rpc UploadFile(UploadFileRequest) returns (UploadFileResponse);
  rpc GetFileUrl(GetFileUrlRequest) returns (GetFileUrlResponse);
  rpc DeleteFile(DeleteFileRequest) returns (DeleteFileResponse);
  rpc GetFileInfo(GetFileInfoRequest) returns (GetFileInfoResponse);
  rpc ListFiles(ListFilesRequest) returns (ListFilesResponse);
}

message UploadFileRequest {
  bytes file_data = 1;
  string original_filename = 2;
  string category = 3;
  string user_id = 4;
  string content_type = 5;
}

message FileInfo {
  string file_name = 1;
  string original_file_name = 2;
  string file_url = 3;
  string file_type = 4;
  int64 file_size = 5;
  string category = 6;
  string user_id = 7;
  string created_at = 8;
  string updated_at = 9;
}

message UploadFileResponse {
  int32 code = 1;
  string message = 2;
  FileInfo file_info = 3;
}

// ... other messages
```

---

## 🧪 Testing với Postman

### 1. Setup Postman

1. **Import Proto File**:
   - New → gRPC Request
   - Import `grpc-common/src/main/proto/file_service.proto`

2. **Configure Server**:
   - Server URL: `localhost:18083`
   - Use server reflection: Enabled

### 2. Test Upload File

**Method**: `FileServiceRpc/UploadFile`

**Metadata** (optional):
```
accept-language: vi
x-request-id: test-123
x-user-id: user-456
```

**Request Body**:
```json
{
  "file_data": "<base64_encoded_file>",
  "original_filename": "test.jpg",
  "category": "images",
  "user_id": "user123",
  "content_type": "image/jpeg"
}
```

**Expected Response**:
```json
{
  "code": 200,
  "message": "File uploaded successfully",
  "file_info": {
    "file_name": "20240425_123456_abc12345.jpg",
    "original_file_name": "test.jpg",
    "file_url": "http://localhost:9000/...",
    "file_type": "image/jpeg",
    "file_size": 12345,
    "category": "images",
    "user_id": "user123",
    "created_at": "2024-04-25T12:34:56",
    "updated_at": "2024-04-25T12:34:56"
  }
}
```

### 3. Test Get File URL

**Method**: `FileServiceRpc/GetFileUrl`

**Request**:
```json
{
  "file_path": "images/20240425_123456_abc12345.jpg"
}
```

**Response**:
```json
{
  "code": 200,
  "message": "File URL retrieved successfully",
  "file_url": "http://localhost:9000/..."
}
```

### 4. Test với i18n

**Metadata**:
```
accept-language: vi
```

**Response** (Vietnamese):
```json
{
  "code": 200,
  "message": "Tải file lên thành công",
  ...
}
```

**Metadata**:
```
accept-language: en
```

**Response** (English):
```json
{
  "code": 200,
  "message": "File uploaded successfully",
  ...
}
```

---

## 🔍 Logging với Context

Với `GrpcContextInboundInterceptor`, logs tự động có request-id và user-id:

```
2024-04-25 12:34:56 [requestId=test-123, userId=user-456] INFO  - Processing file upload
2024-04-25 12:34:57 [requestId=test-123, userId=user-456] INFO  - File uploaded successfully
```

---

## ✅ Benefits của Common Infrastructure

### 1. No Manual Error Handling
```java
// Before (manual try/catch everywhere)
try {
    ApiResponse result = service.uploadFile(...);
    responseObserver.onNext(buildResponse(result));
    responseObserver.onCompleted();
} catch (AppException e) {
    // manual error handling
} catch (Exception e) {
    // manual error handling
}

// After (automatic via GrpcExceptionTranslator)
ApiResponse result = service.uploadFile(...);
responseObserver.onNext(buildResponse(result));
responseObserver.onCompleted();
// Exceptions automatically mapped to gRPC status!
```

### 2. Automatic i18n
```java
// Just use message() - locale automatically set from metadata
.setMessage(message("file.upload.success"))
// Returns "File uploaded successfully" or "Tải file lên thành công"
```

### 3. Automatic Context Propagation
```java
// request-id, user-id automatically in MDC
log.info("Processing file upload"); 
// Logs: [requestId=xxx, userId=yyy] Processing file upload
```

### 4. Reusable for All Services
- user-service: Add grpc-common dependency → instant gRPC support
- notification-service: Same infrastructure, consistent behavior
- No code duplication

---

## 🔧 Auto-Configuration (Optional - For Service Consumers)

### Tổng quan

Module `grpc-common` cung cấp auto-configuration cho gRPC clients, giúp các services không phải khai báo lặp lại configuration.

**Lưu ý**: Auto-config và wrapper clients là **OPTIONAL**. Services có thể:
1. Dùng `@GrpcClient` trực tiếp với stub
2. Dùng wrapper client (`FileGrpcClient`) để code đơn giản hơn

### Khi nào cần?

- ✅ Khi user-service cần gọi file-service
- ✅ Khi notification-service cần gọi user-service  
- ✅ Khi có >= 2 services cần gRPC client config

### Cách sử dụng

#### Option 1: Direct Stub (Verbose)
```java
import net.devh.boot.grpc.client.inject.GrpcClient;
import com.infinite.grpc.constant.GrpcServiceNames;

@Service
public class UserService {
    
    @GrpcClient(GrpcServiceNames.FILE_SERVICE)  // "file-service"
    private FileServiceRpcGrpc.FileServiceRpcBlockingStub fileStub;
    
    public void uploadAvatar(byte[] data, String userId) {
        UploadFileRequest request = UploadFileRequest.newBuilder()
            .setFileData(ByteString.copyFrom(data))
            .setCategory("avatars")
            .setUserId(userId)
            .build();
            
        UploadFileResponse response = fileStub.uploadFile(request);
        // Handle response
    }
}
```

#### Option 2: Wrapper Client (Recommended - Simpler)
```java
import com.infinite.grpc.client.file.FileGrpcClient;

@Service
public class UserService {
    
    @Autowired
    private FileGrpcClient fileClient;
    
    public void uploadAvatar(byte[] data, String userId) {
        try {
            // Much simpler - no protobuf building
            FileInfo fileInfo = fileClient.uploadFile(
                data, 
                "avatar.jpg", 
                "avatars", 
                userId, 
                "image/jpeg"
            );
            
            String fileUrl = fileInfo.getFileUrl();
        } catch (GrpcClientException e) {
            // Structured error handling
            log.error("Upload failed: service={}, code={}, message={}", 
                e.getServiceName(), e.getBusinessCode(), e.getBusinessMessage());
        }
    }
}
```

### Configuration

**Default (grpc-client.yml trong grpc-common):**
```yaml
grpc:
  client:
    file-service:
      address: ${GRPC_FILE_SERVICE_ADDRESS:static://localhost:18083}
      negotiation-type: plaintext
```

**Override via Environment Variables:**
```bash
# Docker
GRPC_FILE_SERVICE_ADDRESS=static://file-service:18083

# Kubernetes
GRPC_FILE_SERVICE_ADDRESS=static://file-service.default.svc.cluster.local:18083

# Service Discovery
GRPC_FILE_SERVICE_ADDRESS=discovery:///file-service
```

### Error Handling

Wrapper clients throw `GrpcClientException` với structured information:

```java
try {
    fileClient.uploadFile(...);
} catch (GrpcClientException e) {
    // Access structured error info
    String service = e.getServiceName();      // "file-service"
    int code = e.getBusinessCode();           // 2001 (FILE_NOT_EXISTED)
    String message = e.getBusinessMessage();  // "File not found"
    
    // Handle specific errors
    if (code == StatusCode.FILE_NOT_EXISTED.getCode()) {
        // Handle file not found
    }
}
```

### Available Clients

- `FileGrpcClient` - File operations (upload, download, delete, info)
- `UserGrpcClient` - (Future) User operations
- `NotificationGrpcClient` - (Future) Notification operations

**Chi tiết**: Xem `README_AUTO_CONFIG.md` để biết thêm về auto-configuration.

---

## 🚀 Implementation Status

### ⚙️ Version Compatibility

**Current Versions:**
- Spring Boot: `4.0.5` (Spring Boot 3.x series)
- grpc-spring-boot-starter: `3.1.0.RELEASE`
- gRPC Java: `1.65.1`
- Protobuf: `3.25.1`

**Compatibility Notes:**
- ✅ Spring Boot 3.x (4.0.5) works with grpc-spring-boot-starter 3.1.0.RELEASE
- ⚠️ Services without Spring Security need to exclude `SecurityAutoConfiguration`
- ✅ All gRPC versions are aligned across the project

**Upgrade Considerations:**
- When upgrading Spring Boot, check grpc-spring-boot-starter compatibility
- Verify protobuf and gRPC Java versions match
- Test all services after version changes

---

### ✅ Phase 1 - Completed
- [x] GrpcMetadataKeys - Common metadata constants
- [x] GrpcServiceNames - Service naming conventions (match với grpc-client.yml)
- [x] GrpcExceptionTranslator - Centralized error handling
- [x] GrpcLocaleInterceptor - i18n support
- [x] GrpcContextInboundInterceptor - Context extraction
- [x] GrpcContextOutboundInterceptor - Context propagation
- [x] FileServiceGrpcImpl - Updated to use common infrastructure
- [x] GrpcClientAutoConfiguration - Auto-load client config
- [x] FileGrpcClient - Wrapper client (optional)
- [x] GrpcClientException - Structured error handling
- [x] .editorconfig - Encoding configuration

### 🔄 Phase 2 - Planned
- [ ] Update proto contract (file_path → category + file_name)
- [ ] Add user metadata save trong upload
- [ ] Implement real GetFileInfo với MinIO metadata
- [ ] Add tenant-id support

### 🔮 Phase 3 - Future
- [ ] Client wrapper abstractions
- [ ] JWT/Auth interceptor
- [ ] Latency/tracing metadata
- [ ] google.rpc.Status detailed errors

---

## 🔧 Troubleshooting

### Issue 1: IllegalStateException - SecurityAutoConfiguration not found

**Error:**
```
java.lang.IllegalStateException: Failed to generate bean name for imported class 'GrpcServerSecurityAutoConfiguration'
Caused by: java.lang.ClassNotFoundException: org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration
```

**Nguyên nhân chính**: 
- **Version compatibility issue** giữa Spring Boot và grpc-spring-boot-starter
- Project đang dùng Spring Boot **4.0.5** (Spring Boot 3.x series)
- grpc-common đang dùng `net.devh:grpc-server-spring-boot-starter:3.1.0.RELEASE`
- Có thể có incompatibility giữa 2 versions này

**Kiểm tra:**
1. ✅ Verify `spring-boot-starter-security` có trong service chưa (nếu service cần security)
2. ✅ Check version compatibility giữa Spring Boot và grpc-spring-boot-starter
3. ✅ Xem service có thực sự cần Spring Security không

**Giải pháp:**

**Cách 1: Thêm Spring Security dependency (Recommended - Clean Solution)**

Thêm vào `pom.xml` của service:
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-security</artifactId>
</dependency>
```

Nếu service không cần authentication, disable security:
```java
@Configuration
@EnableWebSecurity
public class SecurityConfig {
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
        return http.build();
    }
}
```

**Cách 2: Exclude SecurityAutoConfiguration (Workaround)**

```java
@SpringBootApplication(
    scanBasePackages = {"com.infinite.file", "com.infinite.common", "com.infinite.grpc"},
    exclude = {SecurityAutoConfiguration.class}
)
```

**Áp dụng cho services:**

| Service | Spring Security? | Giải pháp đã áp dụng |
|---------|-----------------|----------------------|
| **file-service** | ✅ Có (disabled) | Thêm dependency + disable security |
| **user-service** | ✅ Có (enabled) | Không cần fix |
| **notification-service** | ✅ Có | Không cần fix |

**Current Project Status:**
- Spring Boot: `4.0.5`
- grpc-spring-boot-starter: `3.1.0.RELEASE`
- Đã áp dụng exclude cho file-service
- Hoạt động ổn định

---

### Issue 2: gRPC Port Already in Use

**Error:**
```
java.io.IOException: Failed to bind
Caused by: java.net.BindException: Address already in use
```

**Solution**: 
- Check port trong .env.properties
- Verify không có service nào khác dùng port 18083
- Restart service sau khi đổi port

---

### Issue 3: Proto File Changes Not Reflected

**Solution**:
```bash
cd grpc-common
mvn clean install
# Rebuild services using grpc-common
```

---

## 📚 Key Learnings

### 1. Interceptor Lifecycle
- ❌ **Wrong**: Set/clear context in `try/finally` after `next.startCall()`
- ✅ **Correct**: Wrap listener callback, set/clear in `onMessage()`

### 2. Port Configuration
- ❌ **Wrong**: Hardcode ports in application.properties
- ✅ **Correct**: Use environment variables from .env.properties

### 3. Error Handling
- ❌ **Wrong**: Manual try/catch in every gRPC method
- ✅ **Correct**: Centralized interceptor handles all exceptions

### 4. Contract Design
- ❌ **Wrong**: `file_path` string requiring parsing
- ✅ **Correct**: Separate `category` and `file_name` fields

---

## 🎯 Next Steps

1. **Test thoroughly**: Verify all gRPC methods với Postman
2. **Update proto**: Migrate to category + file_name contract
3. **Add metadata**: Save user metadata during upload
4. **Expand to other services**: Apply same pattern to user-service, notification-service

---

## 📞 Support

For questions or issues:
- Check logs với request-id for tracing
- Verify interceptor order (@Order annotations)
- Ensure .env.properties has correct ports
- Test with Postman server reflection

---

**Document Version**: 1.0 (Phase 1 Complete)  
**Last Updated**: 2024-04-25  
**Status**: Production Ready
