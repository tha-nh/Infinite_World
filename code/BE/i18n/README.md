# I18n Service

I18n Service là một microservice độc lập để quản lý đa ngôn ngữ (internationalization) trong hệ thống. Service hỗ trợ cả **Properties files** (legacy) và **JSON files** (mới) với khả năng load vào Database và Redis cache.

---

## 🎯 Tính năng

### Core Features
- ✅ **JSON Format Support**: Load messages từ JSON files với nested structure
- ✅ **Properties Support**: Backward compatible với properties files
- ✅ **Database Storage**: Lưu trữ messages trong PostgreSQL với hierarchical keys
- ✅ **Redis Cache**: Cache messages để tăng performance
- ✅ **Smart Sync**: Đồng bộ thông minh (insert mới, update thay đổi, giữ nguyên không đổi)
- ✅ **RESTful API**: CRUD operations và cache management
- ✅ **Hierarchical Keys**: Hỗ trợ key có cấu trúc phân cấp (tối đa 10 levels)
- ✅ **Soft Delete**: Không xóa thật, chỉ đánh dấu is_deleted

### New Features (JSON Support)
- 🆕 **Nested Structure**: Tổ chức messages theo cấu trúc lồng nhau
- 🆕 **Auto-loading**: Tự động load JSON files khi khởi động
- 🆕 **Hot-reload**: Reload messages không cần restart
- 🆕 **Multi-source**: Hỗ trợ JSON, Properties, Database, Redis
- 🆕 **Sync API**: Smart sync chỉ update những gì thay đổi

---

## 📁 Cấu trúc Files

### JSON Files (Recommended)
```
common/src/main/resources/i18n/
├── vi.json          # Tiếng Việt (150+ messages)
├── en.json          # English (150+ messages)
└── README.md        # JSON format guide
```

### Properties Files (Legacy)
```
common/src/main/resources/i18n/
├── messages_vi.properties
├── messages_en.properties
└── messages.properties
```

---

## 🚀 Quick Start

### 1. Load JSON vào Database và Redis
```bash
# Tiếng Việt
curl -X POST "http://localhost:8082/v1/api/i18n/load-json?language=vi"

# Tiếng Anh
curl -X POST "http://localhost:8082/v1/api/i18n/load-json?language=en"
```

### 2. Sử dụng trong Code
```java
// Basic usage
String msg = MessageUtils.getMessage("auth.login.success");
// Output: "Đăng nhập thành công"

// With parameters
String msg = MessageUtils.getMessage(
    "email.otp.content",
    "đăng nhập",  // {0}
    "123456",     // {1}
    "5"           // {2}
);

// Specific language
String msg = MessageUtils.getMessage("auth.login.success", "en");
// Output: "Login successful"
```

### 3. Update Messages
```bash
# 1. Edit vi.json file
# 2. Sync to database (smart update)
curl -X POST "http://localhost:8082/v1/api/i18n/sync-json?language=vi"

# 3. Update Redis cache
curl -X POST "http://localhost:8082/v1/api/i18n/load-json-to-redis?language=vi"
```

---

## 🔧 Cấu hình

### Database
Service sử dụng PostgreSQL và tự động tạo bảng `i18n_{language}` cho mỗi ngôn ngữ.

**Schema:**
```sql
CREATE TABLE i18n_vi (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    key VARCHAR(500) NOT NULL,
    message TEXT,
    key1 VARCHAR(100),  -- Hierarchical level 1
    key2 VARCHAR(100),  -- Hierarchical level 2
    key3 VARCHAR(100),  -- Hierarchical level 3
    key4 VARCHAR(100),  -- Hierarchical level 4
    key5 VARCHAR(100),  -- Hierarchical level 5
    key6 VARCHAR(100),  -- Hierarchical level 6
    key7 VARCHAR(100),  -- Hierarchical level 7
    key8 VARCHAR(100),  -- Hierarchical level 8
    key9 VARCHAR(100),  -- Hierarchical level 9
    key10 VARCHAR(100), -- Hierarchical level 10
    is_deleted BOOLEAN DEFAULT false,
    language VARCHAR(10),
    UNIQUE KEY unique_key (key)
);
```

### Redis
Sử dụng Redis để cache message với pattern: `i18n:{language}:{key}`

**Examples:**
```
i18n:vi:SUCCESS
i18n:vi:auth.login.success
i18n:en:auth.login.success
```

### Application Properties
```properties
# Server
server.port=8082

# Languages
i18n.languages=en,vi

# Cache configuration
i18n.cache-prefix=i18n:
i18n.cache-expire-hours=24

# Properties file path (legacy)
i18n.properties-file=/i18n/messages
```

---

## 📡 API Endpoints

### JSON Operations (New)

#### 1. Load JSON to Database
```http
POST /v1/api/i18n/load-json-to-db?language=vi
```
Load messages từ JSON file vào database (không cache)

#### 2. Load JSON to Database and Cache
```http
POST /v1/api/i18n/load-json?language=vi
```
Load messages từ JSON file vào cả database và Redis

#### 3. Load JSON to Redis
```http
POST /v1/api/i18n/load-json-to-redis?language=vi
```
Load messages từ JSON file vào Redis cache (không database)

#### 4. Sync JSON to Database
```http
POST /v1/api/i18n/sync-json?language=vi
```
Đồng bộ thông minh: insert mới, update thay đổi, giữ nguyên không đổi

**Response:**
```json
{
  "code": 1000,
  "message": "Thành công",
  "result": "Sync completed for language vi: 10 inserted, 5 updated, 135 unchanged"
}
```

---

### Properties Operations (Legacy)

#### 1. Load Properties to Database
```http
POST /v1/api/i18n/load-properties-to-db?language=vi
```

#### 2. Load Properties to Database and Cache
```http
POST /v1/api/i18n/load-properties?language=vi
```

---

### Cache Operations

#### 1. Load Database to Cache
```http
POST /v1/api/i18n/load-db-to-cache?language=vi
```

#### 2. Refresh All Cache
```http
POST /v1/api/i18n/refresh-cache
```

#### 3. Clear Cache by Language
```http
DELETE /v1/api/i18n/cache/clear?language=vi
```

#### 4. Clear All Cache
```http
DELETE /v1/api/i18n/cache/clear/all
```

---

### Query Operations

#### 1. Get Messages from Database
```http
GET /v1/api/i18n/messages/tree/db?language=vi&page=0&size=20&searchKey=auth
```

#### 2. Get Messages from Redis
```http
GET /v1/api/i18n/messages/tree/redis?language=vi&page=0&size=20
```

#### 3. Get Redis Keys
```http
GET /v1/api/i18n/redis/keys?pattern=i18n:vi:*
```

---

### CRUD Operations

#### 1. Create/Update Message
```http
POST /v1/api/i18n/message?language=vi
Content-Type: application/json

{
  "key": "user.profile.name",
  "message": "Họ và tên"
}
```

#### 2. Delete Message
```http
DELETE /v1/api/i18n/message?language=vi&key=user.profile.name
```

#### 3. Delete Multiple Messages
```http
DELETE /v1/api/i18n/messages/db?language=vi
Content-Type: application/json

["key1", "key2", "key3"]
```

---

### Table Management

#### Create Table
```http
POST /v1/api/i18n/create-table?language=vi
```

---

## 📊 JSON Format

### Nested Structure (Recommended)
```json
{
  "auth": {
    "login": {
      "success": "Đăng nhập thành công",
      "fail": "Sai tài khoản hoặc mật khẩu"
    },
    "register": {
      "success": "Đăng ký thành công"
    }
  },
  "email": {
    "user": {
      "locked": {
        "subject": "Tài khoản của bạn đã bị khóa",
        "message": "Tài khoản {0} của bạn đã bị khóa {1} bởi {2}."
      }
    }
  }
}
```

**Access:**
```java
MessageUtils.getMessage("auth.login.success");
MessageUtils.getMessage("email.user.locked.subject");
MessageUtils.getMessage("email.user.locked.message", "john_doe", "tạm thời", "admin");
```

### Flat Keys (Also Supported)
```json
{
  "SUCCESS": "Thành công",
  "auth.login.success": "Đăng nhập thành công"
}
```

---

## 🔄 Common Workflows

### Workflow 1: Initial Setup
```bash
# 1. Create table
curl -X POST "http://localhost:8082/v1/api/i18n/create-table?language=vi"

# 2. Load JSON
curl -X POST "http://localhost:8082/v1/api/i18n/load-json?language=vi"

# 3. Verify
curl "http://localhost:8082/v1/api/i18n/messages/tree/db?language=vi&page=0&size=10"
```

### Workflow 2: Update Messages
```bash
# 1. Edit vi.json file
# 2. Sync (smart update)
curl -X POST "http://localhost:8082/v1/api/i18n/sync-json?language=vi"

# 3. Update cache
curl -X POST "http://localhost:8082/v1/api/i18n/load-json-to-redis?language=vi"
```

### Workflow 3: Add New Language
```bash
# 1. Create ja.json file
# 2. Create table
curl -X POST "http://localhost:8082/v1/api/i18n/create-table?language=ja"

# 3. Load
curl -X POST "http://localhost:8082/v1/api/i18n/load-json?language=ja"
```

---

## 📝 Response Format

### Success Response
```json
{
  "code": 1000,
  "message": "Thành công",
  "result": "Loaded 150 messages to database and 150 to Redis for language: vi"
}
```

### Error Response
```json
{
  "code": 1001,
  "message": "Yêu cầu không hợp lệ",
  "result": null
}
```

### Status Codes
- `1000`: SUCCESS
- `1001`: BAD_REQUEST  
- `1003`: INVALID_KEY
- `1006`: DATA_NOT_EXISTED
- `9999`: INTERNAL_ERROR

---

## 🧪 Testing

### Postman Collection
Import file: `i18n/I18N_JSON_APIs.postman_collection.json`

Collection bao gồm:
- ✅ JSON operations
- ✅ Properties operations (legacy)
- ✅ Cache operations
- ✅ Query operations
- ✅ Complete workflows

### Manual Testing
```bash
# Test load
curl -X POST "http://localhost:8082/v1/api/i18n/load-json?language=vi"

# Test get
curl "http://localhost:8082/v1/api/i18n/messages/tree/db?language=vi&page=0&size=5"

# Test sync
curl -X POST "http://localhost:8082/v1/api/i18n/sync-json?language=vi"
```

---

## 🔍 Monitoring & Debugging

### Check Logs
```bash
tail -f logs/i18n-service.log
```

### Check Database
```sql
-- Count messages
SELECT COUNT(*) FROM i18n_vi WHERE is_deleted = false;

-- Search by key
SELECT * FROM i18n_vi WHERE key LIKE 'auth%';

-- Search by message
SELECT * FROM i18n_vi WHERE message LIKE '%đăng nhập%';
```

### Check Redis
```bash
# Get all keys
redis-cli KEYS "i18n:vi:*"

# Get specific message
redis-cli GET "i18n:vi:SUCCESS"

# Count keys
redis-cli KEYS "i18n:vi:*" | wc -l
```

---

## 🚢 Deployment

### Docker
```bash
# Build
mvn clean package

# Run with Docker
docker-compose up i18n-service
```

### Standalone
```bash
# Build
mvn clean package

# Run
java -jar target/i18n-service-1.0.0.jar
```

### Environment Variables
```bash
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/i18n_db
SPRING_DATASOURCE_USERNAME=postgres
SPRING_DATASOURCE_PASSWORD=password
SPRING_REDIS_HOST=localhost
SPRING_REDIS_PORT=6379
SERVER_PORT=8082
```

---

## 📚 Documentation

| File | Description |
|------|-------------|
| **README.md** | This file - Service overview |
| **common/QUICK_REFERENCE.md** | Quick commands và examples |
| **common/MIGRATION_GUIDE.md** | Migration từ properties sang JSON |
| **common/JSON_LOADER_API_GUIDE.md** | Detailed API documentation |
| **common/I18N_JSON_SUMMARY.md** | Features summary |
| **common/I18N_IMPLEMENTATION_COMPLETE.md** | Complete implementation details |
| **common/src/main/resources/i18n/README.md** | JSON format guide |

---

## 🔒 Security

- ✅ Authentication required for all admin APIs
- ✅ Authorization check (admin role only)
- ✅ SQL injection protected (PreparedStatement)
- ✅ XSS protected
- ✅ Input validation

---

## 📈 Performance

| Operation | Time | Messages |
|-----------|------|----------|
| Load JSON to Memory | ~50ms | 150 |
| Load JSON to DB | ~500ms | 150 |
| Load JSON to Redis | ~200ms | 150 |
| Sync JSON | ~300ms | 150 |
| Get Message (Memory) | ~1μs | 1 |
| Get Message (Redis) | ~1ms | 1 |
| Get Message (DB) | ~5ms | 1 |

---

## 🎓 Best Practices

1. ✅ **Use JSON for new messages** - Easier to manage with nested structure
2. ✅ **Use sync for updates** - Safer, no data loss
3. ✅ **Load to DB first, then cache** - Ensure persistence
4. ✅ **Verify after load** - Always check results
5. ✅ **Monitor logs** - Watch for errors
6. ✅ **Test in staging first** - Before production
7. ✅ **Use nested structure** - Better organization

---

## 🐛 Troubleshooting

### Problem: Message không hiển thị
```bash
# Check JSON file
cat common/src/main/resources/i18n/vi.json | jq .auth.login.success

# Check database
SELECT * FROM i18n_vi WHERE key = 'auth.login.success';

# Check Redis
redis-cli GET "i18n:vi:auth.login.success"

# Reload
curl -X POST "http://localhost:8082/v1/api/i18n/load-json?language=vi"
```

### Problem: Load failed
```bash
# Check logs
tail -f logs/i18n-service.log

# Create table first
curl -X POST "http://localhost:8082/v1/api/i18n/create-table?language=vi"

# Try sync
curl -X POST "http://localhost:8082/v1/api/i18n/sync-json?language=vi"
```

---

## 🔄 Migration từ Common Module

Service này đã được tách ra từ common module để:
- ✅ Tăng tính độc lập và khả năng scale
- ✅ Giảm coupling giữa các service
- ✅ Dễ dàng maintain và phát triển tính năng mới
- ✅ Có thể sử dụng technology stack riêng nếu cần
- ✅ Hỗ trợ JSON format hiện đại

---

## 📞 Support

### Contact
- Team Lead: Development Team
- Slack: #i18n-support
- Email: support@infinite.com

### Resources
- Postman Collection: `i18n/I18N_JSON_APIs.postman_collection.json`
- Documentation: `common/` folder
- Logs: `logs/i18n-service.log`

---

## 📝 Changelog

### Version 1.1.0 (2026-05-17)
- 🆕 Added JSON format support
- 🆕 Added smart sync functionality
- 🆕 Added nested structure support
- 🆕 Added 4 new APIs for JSON operations
- 🆕 Full documentation
- ✅ Backward compatible with properties files

### Version 1.0.0
- ✅ Initial release
- ✅ Properties file support
- ✅ Database storage
- ✅ Redis cache
- ✅ RESTful API

---

**Service Port:** 8082  
**Version:** 1.1.0  
**Status:** ✅ Production Ready  
**Last Updated:** 2026-05-17