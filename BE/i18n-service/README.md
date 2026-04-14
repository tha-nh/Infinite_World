# I18n Service

Microservice for managing internationalization (i18n) messages with dynamic PostgreSQL tables and Redis caching.

## Architecture

```
┌─────────────────┐         ┌──────────────────┐
│  User Service   │─────┐   │  Gateway         │ ─┐
├─────────────────┤     │   ├──────────────────┤  │
│  Core Service   │─────┼──>│ Redis (i18n)     │<─┤
└─────────────────┘     │   ├──────────────────┤  │
                        │   │  (cache layer)   │  │
                        │   └──────────────────┘  │
                        │            ▲            │
                        └────┐       │            │
                             │       │ (on sync)  │
                             v       │            │
                    ┌──────────────────────────┐  │
                    │  I18n Service            │──┘
                    ├──────────────────────────┤
                    │ PostgreSQL Database      │
                    │ - i18n_en table          │
                    │ - i18n_vi table          │
                    │ - i18n_xx table (dynamic)│
                    │ Redis Cache (24h TTL)    │
                    └──────────────────────────┘
```

## Features

- **Dynamic Tables**: Automatically creates tables per language (i18n_en, i18n_vi, etc.)
- **Dynamic Columns**: Auto-generates key_1, key_2, ... columns based on message key depth
- **CRUD Operations**: Create, Read, Update, Delete i18n messages via API
- **Properties Loader**: Load messages from .properties file to database
- **Redis Caching**: 24-hour cache TTL for performance
- **Fallback**: Return message key if translation not found
- **Soft Deletes**: Uses is_deleted flag instead of hard delete

## Database Schema

### Dynamic Table Creation
Tables are created automatically when I18n Service starts:

```sql
-- Created for each language automatically
CREATE TABLE INFINITE_I18N.i18n_en (
    id SERIAL PRIMARY KEY,
    message_key VARCHAR(255) NOT NULL UNIQUE,
    message TEXT NOT NULL,
    key_1 VARCHAR(255),      -- First level (user)
    key_2 VARCHAR(255),      -- Second level (profile)
    key_3 VARCHAR(255),      -- Third level (name)
    key_4 VARCHAR(255),      -- Fourth level (optional)
    key_5 VARCHAR(255),      -- Fifth level (optional)
    is_deleted BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE INFINITE_I18N.i18n_vi (
    -- Same structure as above
);
```

### Key Parsing Example
```
Message Key: user.profile.name
↓
key_1 = "user"
key_2 = "profile"
key_3 = "name"
↓
Stored in i18n_vi table
```

## Configuration

### .env.properties (root)
```properties
DB_URL=jdbc:postgresql://localhost:5432/infinite_db
DB_USERNAME=root
DB_PASSWORD=123456
I18N_SCHEMA=INFINITE_I18N

REDIS_HOST=localhost
REDIS_PORT=6379
REDIS_PASSWORD=123456
```

### application.properties (i18n-service)
```properties
server.port=8082
spring.application.name=i18n-service

# From .env.properties
spring.datasource.url=${DB_URL}
spring.datasource.username=${DB_USERNAME}
spring.datasource.password=${DB_PASSWORD}

# I18n Configuration
i18n.cache-prefix=i18n:
i18n.cache-expire-hours=24
i18n.languages=en,vi
i18n.default-language=en
i18n.properties-file=/i18n/messages.properties
i18n.schema=${I18N_SCHEMA}

# Redis
spring.data.redis.host=${REDIS_HOST}
spring.data.redis.port=${REDIS_PORT}
spring.data.redis.password=${REDIS_PASSWORD}
```

## API Endpoints

### 1. Create or Update Message
```http
POST /v1/api/i18n/message?language=en
Content-Type: application/json

{
  "messageKey": "user.profile.name",
  "message": "User Profile Name"
}
```

Response:
```json
{
  "code": "200",
  "message": "Message created/updated successfully",
  "result": "user.profile.name"
}
```

### 2. Delete Message
```http
DELETE /v1/api/i18n/message?language=en&messageKey=user.profile.name
```

### 3. Load from Properties File
Loads all messages from `classpath:/i18n/messages.properties` to database:

```http
POST /v1/api/i18n/load-from-properties?language=en
```

Response:
```json
{
  "code": "200",
  "message": "Messages loaded from properties successfully",
  "result": "Loaded 25 messages for language: en"
}
```

### 4. Refresh Cache
Reload all messages from database to Redis:

```http
POST /v1/api/i18n/refresh-cache
```

## Usage in Other Services

### Example 1: User Service Response
```java
@Data
public class UserResponse {
    private Long id;
    private String name;
    
    @Transient
    private String successMessage;
    
    public void resolveMessages(I18nRedisClient i18n, String userLanguage) {
        this.successMessage = i18n.getMessage("success.operation_completed", userLanguage);
    }
}
```

### Example 2: In Controller
```java
@Autowired
private I18nRedisClient i18nClient;

@GetMapping("/profile")
public ResponseEntity<ApiResponse<UserResponse>> getProfile(
        @RequestParam(defaultValue = "en") String language) {
    UserResponse response = new UserResponse(1L, "John");
    response.resolveMessages(i18nClient, language);
    return ResponseEntity.ok(ApiResponse.builder()
            .code("200")
            .message(i18nClient.getMessage("success.operation_completed", language))
            .result(response)
            .build());
}
```

### Example 3: With Language Fallback
```java
String message = i18nClient.getMessageWithFallback(
    "user.profile.name",  // key
    "vi",                 // requested language
    "en"                  // fallback
);
// Returns Vietnamese translation if available, else English
```

## i18n/messages.properties

The properties file defines all available message keys:

```properties
# User Messages
user.created=User created successfully
user.updated=User updated successfully
user.profile.name=User Profile Name
user.profile.email=Email Address
user.profile.phone=Phone Number

# Auth Messages
auth.login_success=Login successful
auth.login_failed=Login failed
auth.unauthorized=Unauthorized access

# Error Messages
error.validation_failed=Validation failed
error.not_found=Resource not found
error.internal_error=Internal server error

# Success Messages
success.operation_completed=Operation completed successfully
success.saved=Data saved successfully
```

## Workflow

### 1. **Initial Setup**
```bash
# I18n Service starts
→ Creates tables for each language (i18n_en, i18n_vi, etc.)
→ Tables ready for data insertion
```

### 2. **Load Messages from Properties (Option 1)**
```bash
POST /v1/api/i18n/load-from-properties?language=en
→ Reads i18n/messages.properties
→ Inserts all keys into i18n_en table
→ Caches all messages to Redis
→ Done
```

### 3. **Add/Update via API (Option 2)**
```bash
POST /v1/api/i18n/message?language=vi
{
  "messageKey": "user.newfield",
  "message": "Vietnamese translation..."
}
→ Inserts into i18n_vi table
→ Caches to Redis
→ Done
```

### 4. **Services Use Cached Messages**
```bash
User Service requests message:
user-service → I18nRedisClient.getMessage("user.profile.name", "vi")
             → Redis (fast lookup)
             → Returns cached message
             → Done
```

## Example Data

After running initial setup and API calls:

```ruby
# i18n_en table
| id | message_key          | message                  | key_1    | key_2   | key_3  | is_deleted |
|----|----------------------|--------------------------|----------|---------|--------|------------|
| 1  | user.profile.name    | User Profile Name        | user     | profile | name   | false      |
| 2  | auth.login_success   | Login successful         | auth     | login   | success| false      |

# i18n_vi table
| id | message_key          | message                       | key_1    | key_2    | key_3  | is_deleted |
|----|----------------------|-------------------------------|----------|----------|--------|------------|
| 1  | user.profile.name    | Tên Hồ Sơ Người Dùng        | user     | profile  | name   | false      |
| 2  | auth.login_success   | Đăng nhập thành công         | auth     | login    | success| false      |
```

## PostgreSQL Database Setup

```sql
-- Create schema
CREATE SCHEMA INFINITE_I18N;

-- I18n Service will automatically create language tables:
-- CREATE TABLE INFINITE_I18N.i18n_en (...)
-- CREATE TABLE INFINITE_I18N.i18n_vi (...)
-- etc.
```

## Notes

- Tables are created automatically by I18n Service on startup
- Each language has its own table (i18n_en, i18n_vi, i18n_zh, etc.)
- Dynamic columns (key_1, key_2, ...) are populated based on message key depth
- Cache TTL is 24 hours (configurable)
- Soft deletes preserve data history
- Services only import `common` module, not `i18n-service`
- I18n Service runs independently and can be deployed separately
