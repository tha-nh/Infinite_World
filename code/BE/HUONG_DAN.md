# Hướng dẫn sử dụng Notification Service - BUOC2

## Tổng quan chức năng

Sau khi hoàn thành BUOC2, notification-service có 3 nhóm API chính:

1. **Internal API** - Service khác gọi vào để tạo notification
2. **Admin API** - Admin quản lý notification
3. **Client API** - User đọc inbox và claim reward

---

## Chuẩn bị môi trường

### 1. Khởi động PostgreSQL và tạo schema

```bash
# Kết nối PostgreSQL
psql -U infinite -d infinite_world

# Tạo schema (nếu chưa có)
CREATE SCHEMA IF NOT EXISTS INF_NOTI;

# Chạy migration
\i notification/db/migration/V001__create_schema_and_tables.sql

# Hoặc dùng psql command
psql -U infinite -d infinite_world -f notification/db/migration/V001__create_schema_and_tables.sql
```

### 2. Build và chạy service

```bash
# Build toàn bộ project
mvn clean install -DskipTests

# Chạy notification-service
cd notification
mvn spring-boot:run

# Service sẽ chạy trên port 8084 (theo NOTIFICATION_SERVICE trong .env)
```

### 3. Insert test data (để test Client API)

**Lưu ý:** Fanout worker chưa có trong BUOC2, nên cần insert `user_notification` thủ công.

```sql
-- Kết nối database
psql -U infinite -d infinite_world

-- Insert test notifications cho user 123
INSERT INTO INF_NOTI.user_notification 
(user_id, notification_id, title, body, type, priority, status, is_deleted, is_claimed, created_at)
VALUES 
(123, 1, 'Chào mừng bạn', 'Tài khoản đã được tạo thành công', 'SYSTEM', 1, 'UNREAD', false, false, NOW()),
(123, 2, 'Sự kiện đặc biệt', 'Tham gia để nhận quà', 'PROMOTION', 2, 'UNREAD', false, false, NOW());

-- Insert notification có reward để test claim
INSERT INTO INF_NOTI.user_notification 
(user_id, notification_id, title, body, type, priority, reward_payload, status, is_deleted, is_claimed, created_at)
VALUES 
(123, 3, 'Phần thưởng đặc biệt', 'Nhận ngay 1000 vàng', 'REWARD', 3, 
 '{"type": "GOLD", "amount": 1000}'::jsonb, 'UNREAD', false, false, NOW());
```

---

## 1. Internal API - Tạo notification request

### Endpoint
```
POST http://localhost:8084/api/internal/notifications/requests
```

### Mục đích
Service khác (user-service, payment-service...) gọi vào để tạo notification.

### Curl command

```bash
curl -X POST http://localhost:8084/api/internal/notifications/requests \
  -H "Content-Type: application/json" \
  -d '{
    "schemaVersion": "v1",
    "eventId": "evt-001",
    "requestId": "req-001",
    "traceId": "trace-001",
    "sourceService": "user-service",
    "sourceModule": "auth",
    "sourceAction": "user_registered",
    "idempotencyKey": "user_registered:user-123",
    "channels": ["INBOX", "REALTIME"],
    "target": {
      "type": "USER_IDS",
      "userIds": [123, 456]
    },
    "content": {
      "type": "SYSTEM",
      "priority": 1,
      "title": "Chào mừng bạn đến với hệ thống",
      "body": "Tài khoản của bạn đã được tạo thành công"
    },
    "action": {
      "actionType": "OPEN_SCREEN",
      "screen": "profile",
      "payload": {}
    },
    "metadata": {
      "businessId": "user-123"
    }
  }'
```

### Response mẫu
```json
{
  "code": 200,
  "message": "Notification request accepted",
  "result": {
    "notificationId": 1,
    "eventId": "evt-001",
    "status": "ACCEPTED"
  }
}
```

### Verify trong database
```sql
-- Check notification_request
SELECT * FROM INF_NOTI.notification_request WHERE event_id = 'evt-001';

-- Check notification_template
SELECT * FROM INF_NOTI.notification_template WHERE request_id = 1;

-- Check notification_delivery_job
SELECT * FROM INF_NOTI.notification_delivery_job WHERE notification_id = 1;
```

### Test idempotency
```bash
# Gửi lại request với cùng idempotencyKey
# Expected: Không tạo record mới, trả về kết quả cũ
curl -X POST http://localhost:8084/api/internal/notifications/requests \
  -H "Content-Type: application/json" \
  -d '{
    "schemaVersion": "v1",
    "eventId": "evt-002",
    "idempotencyKey": "user_registered:user-123",
    "sourceService": "user-service",
    "channels": ["INBOX"],
    "target": {"type": "USER_IDS", "userIds": [123]},
    "content": {
      "type": "SYSTEM",
      "priority": 1,
      "title": "Test duplicate",
      "body": "Should not create new record"
    }
  }'
```

---

## 2. Admin API

### 2.1. Tạo notification (Admin)

```bash
curl -X POST http://localhost:8084/api/admin/notifications \
  -H "Content-Type: application/json" \
  -d '{
    "schemaVersion": "v1",
    "eventId": "admin-evt-001",
    "sourceService": "admin-service",
    "sourceModule": "campaign",
    "sourceAction": "manual_create",
    "idempotencyKey": "admin_campaign:camp-001",
    "channels": ["INBOX", "EMAIL"],
    "target": {
      "type": "ALL_USERS"
    },
    "content": {
      "type": "PROMOTION",
      "priority": 2,
      "title": "Sự kiện đặc biệt",
      "body": "Tham gia sự kiện để nhận quà hấp dẫn!",
      "imageUrl": "https://example.com/event.jpg"
    },
    "action": {
      "actionType": "OPEN_URL",
      "url": "https://example.com/event"
    },
    "reward": {
      "type": "GOLD",
      "amount": 1000
    },
    "metadata": {
      "campaignId": "camp-001"
    }
  }'
```

### 2.2. Xem danh sách notification

```bash
# Lấy tất cả
curl http://localhost:8084/api/admin/notifications?page=0&size=20

# Filter theo status
curl http://localhost:8084/api/admin/notifications?page=0&size=20&status=PENDING
```

### 2.3. Xem chi tiết notification

```bash
curl http://localhost:8084/api/admin/notifications/1
```

### 2.4. Xem delivery summary

```bash
curl http://localhost:8084/api/admin/notifications/1/delivery-summary
```

### 2.5. Cancel notification (chưa implement)

```bash
curl -X POST http://localhost:8084/api/admin/notifications/1/cancel

# Expected Response (400 BAD REQUEST):
# {
#   "code": 400,
#   "message": "Cancel notification feature is not implemented yet"
# }
```

### 2.6. Retry notification (chưa implement)

```bash
curl -X POST http://localhost:8084/api/admin/notifications/1/retry

# Expected Response (400 BAD REQUEST):
# {
#   "code": 400,
#   "message": "Retry notification feature is not implemented yet"
# }
```

---

## 3. Client API - User Inbox

### 3.1. Lấy inbox của user

```bash
curl "http://localhost:8084/api/notifications?userId=123&page=0&size=20"
```

**Response mẫu:**
```json
{
  "code": 200,
  "message": "Inbox retrieved successfully",
  "result": {
    "content": [
      {
        "id": 3,
        "title": "Phần thưởng đặc biệt",
        "body": "Nhận ngay 1000 vàng",
        "type": "REWARD",
        "priority": 3,
        "rewardPayload": {"type": "GOLD", "amount": 1000},
        "status": "UNREAD",
        "isClaimed": false,
        "createdAt": "2026-04-27T23:30:00"
      },
      {
        "id": 2,
        "title": "Sự kiện đặc biệt",
        "body": "Tham gia để nhận quà",
        "type": "PROMOTION",
        "priority": 2,
        "status": "UNREAD",
        "isClaimed": false,
        "createdAt": "2026-04-27T23:20:00"
      }
    ],
    "totalElements": 3,
    "totalPages": 1
  }
}
```

### 3.2. Đếm số notification chưa đọc

```bash
curl "http://localhost:8084/api/notifications/unread-count?userId=123"
```

**Response:**
```json
{
  "code": 200,
  "message": "Unread count retrieved",
  "result": {
    "unreadCount": 3
  }
}
```

### 3.3. Đếm chưa đọc (tiếng Việt)

```bash
curl "http://localhost:8084/api/notifications/unread-count?userId=123" \
  -H "Accept-Language: vi"
```

**Response:**
```json
{
  "code": 200,
  "message": "Lấy số lượng chưa đọc thành công",
  "result": {
    "unreadCount": 3
  }
}
```

### 3.4. Đánh dấu đã đọc (1 notification)

```bash
curl -X POST http://localhost:8084/api/notifications/1/read \
  -H "Content-Type: application/json" \
  -d '{"userId": 123}'
```

**Verify:**
```sql
SELECT id, status, read_at FROM INF_NOTI.user_notification 
WHERE id = 1 AND user_id = 123;
-- Expected: status = 'READ', read_at = timestamp
```

### 3.5. Đánh dấu tất cả đã đọc

```bash
curl -X POST http://localhost:8084/api/notifications/read-all \
  -H "Content-Type: application/json" \
  -d '{"userId": 123}'
```

**Verify:**
```sql
SELECT COUNT(*) FROM INF_NOTI.user_notification 
WHERE user_id = 123 AND status = 'UNREAD';
-- Expected: 0
```

### 3.6. Xóa notification (soft delete)

```bash
curl -X POST http://localhost:8084/api/notifications/1/delete \
  -H "Content-Type: application/json" \
  -d '{"userId": 123}'
```

**Verify:**
```sql
SELECT id, is_deleted FROM INF_NOTI.user_notification 
WHERE id = 1 AND user_id = 123;
-- Expected: is_deleted = true
```

### 3.7. Claim reward

```bash
curl -X POST http://localhost:8084/api/notifications/3/claim \
  -H "Content-Type: application/json" \
  -d '{"userId": 123}'
```

**Response mẫu:**
```json
{
  "code": 200,
  "message": "Reward claimed successfully",
  "result": {
    "notificationId": 3,
    "userId": 123,
    "rewardPayload": {
      "type": "GOLD",
      "amount": 1000
    },
    "referenceCode": "CLAIM-xxx-xxx",
    "claimedAt": "2026-04-27T23:45:00"
  }
}
```

**Verify:**
```sql
-- Check user_notification
SELECT id, is_claimed, claimed_at FROM INF_NOTI.user_notification 
WHERE id = 3 AND user_id = 123;
-- Expected: is_claimed = true, claimed_at = timestamp

-- Check claim log
SELECT * FROM INF_NOTI.user_notification_claim_log 
WHERE user_notification_id = 3 AND user_id = 123;
-- Expected: 1 record với reward_payload và reference_code
```

### 3.8. Claim lại (should fail)

```bash
curl -X POST http://localhost:8084/api/notifications/3/claim \
  -H "Content-Type: application/json" \
  -d '{"userId": 123}'

# Expected Response (400 BAD REQUEST):
# {
#   "code": 400,
#   "message": "Reward already claimed"
# }
```

---

## 4. Test Validation

### 4.1. Missing userId (Error)

```bash
curl -X POST http://localhost:8084/api/notifications/1/read \
  -H "Content-Type: application/json" \
  -d '{}'

# Expected Response (400 BAD REQUEST):
# {
#   "code": 400,
#   "message": "Validation failed",
#   "errors": [{"field": "userId", "message": "must not be null"}]
# }
```

### 4.2. Claim notification không có reward (Error)

```bash
# Insert notification không có reward
psql -U infinite -d infinite_world -c "
INSERT INTO INF_NOTI.user_notification 
(user_id, notification_id, title, body, type, priority, reward_payload, status, is_deleted, is_claimed, created_at)
VALUES (123, 4, 'No reward', 'Test', 'SYSTEM', 1, NULL, 'UNREAD', false, false, NOW());
"

# Claim
curl -X POST http://localhost:8084/api/notifications/4/claim \
  -H "Content-Type: application/json" \
  -d '{"userId": 123}'

# Expected Response (400 BAD REQUEST):
# {
#   "code": 400,
#   "message": "No reward available"
# }
```

### 4.3. Claim notification đã hết hạn (Error)

```bash
# Insert notification đã hết hạn
psql -U infinite -d infinite_world -c "
INSERT INTO INF_NOTI.user_notification 
(user_id, notification_id, title, body, type, priority, reward_payload, expire_at, status, is_deleted, is_claimed, created_at)
VALUES (123, 5, 'Expired', 'Test', 'REWARD', 3, 
 '{\"type\": \"GOLD\", \"amount\": 500}'::jsonb, 
 NOW() - INTERVAL '1 day', 'UNREAD', false, false, NOW() - INTERVAL '2 days');
"

# Claim
curl -X POST http://localhost:8084/api/notifications/5/claim \
  -H "Content-Type: application/json" \
  -d '{"userId": 123}'

# Expected Response (400 BAD REQUEST):
# {
#   "code": 400,
#   "message": "Notification has expired"
# }
```

---

## 5. Kafka Integration (Optional)

### Publish message vào Kafka topic

```bash
# Dùng kafka-console-producer
kafka-console-producer --broker-list localhost:9092 --topic notification.request.v1

# Paste JSON message:
{
  "schemaVersion": "v1",
  "eventId": "kafka-evt-001",
  "sourceService": "payment-service",
  "sourceAction": "payment_success",
  "idempotencyKey": "payment_success:txn-001",
  "channels": ["INBOX", "EMAIL"],
  "target": {"type": "USER_IDS", "userIds": [123]},
  "content": {
    "type": "TRANSACTION",
    "priority": 2,
    "title": "Thanh toán thành công",
    "body": "Giao dịch của bạn đã được xử lý"
  }
}
```

### Verify trong log
```
[INFO] NotificationRequestConsumer - Received notification request: eventId=kafka-evt-001
[INFO] CreateNotificationRequestUseCase - Creating notification request
```

### Verify trong database
```sql
SELECT * FROM INF_NOTI.notification_request WHERE event_id = 'kafka-evt-001';
```

---

## 6. Queries hữu ích

### Check tất cả notifications của user
```sql
SELECT id, title, type, status, is_claimed, is_deleted, created_at 
FROM INF_NOTI.user_notification 
WHERE user_id = 123 
ORDER BY created_at DESC;
```

### Check unread count
```sql
SELECT COUNT(*) as unread_count 
FROM INF_NOTI.user_notification 
WHERE user_id = 123 AND status = 'UNREAD' AND is_deleted = false;
```

### Check claimable rewards
```sql
SELECT id, title, reward_payload, expire_at 
FROM INF_NOTI.user_notification 
WHERE user_id = 123 
  AND reward_payload IS NOT NULL 
  AND is_claimed = false 
  AND is_deleted = false
  AND (expire_at IS NULL OR expire_at > NOW());
```

### Check claim logs
```sql
SELECT * FROM INF_NOTI.user_notification_claim_log 
WHERE user_id = 123 
ORDER BY claimed_at DESC;
```

### Check notification requests
```sql
SELECT id, event_id, source_service, source_action, status, created_at 
FROM INF_NOTI.notification_request 
ORDER BY created_at DESC 
LIMIT 10;
```

### Check delivery jobs
```sql
SELECT nj.id, nj.notification_id, nj.job_type, nj.status, 
       nj.total_target, nj.processed_target, nj.success_target, nj.failed_target
FROM INF_NOTI.notification_delivery_job nj
ORDER BY nj.created_at DESC;
```

---

## 7. Postman Collection

Import file `notification-service.postman_collection.json` vào Postman để test nhanh.

**Variables trong collection:**
- `baseUrl`: http://localhost:8084
- `userId`: 123
- `notificationId`: 1

---

## 8. Tính năng đã có

### ✅ Internal API
- Nhận notification request qua REST/Kafka
- Idempotency (không tạo trùng)
- Audit đầy đủ (source service, action, metadata)

### ✅ Admin API
- Tạo notification thủ công
- Xem danh sách (phân trang, filter)
- Xem chi tiết
- Xem delivery summary
- Cancel/retry (endpoint có, logic chưa)

### ✅ Client API
- Lấy inbox (phân trang)
- Đếm chưa đọc
- Đánh dấu đã đọc (1 hoặc tất cả)
- Xóa notification (soft delete)
- Claim reward an toàn (pessimistic lock)

### ✅ Database
- 8 bảng với indexes đầy đủ
- Idempotency constraints
- JSONB support
- Audit trail

### ✅ Kỹ thuật
- Pessimistic lock cho claim
- Bean validation
- I18n (en/vi)
- Exception handling rõ ràng

---

## 9. Những gì chưa có (ngoài scope BUOC2)

### ⏳ Fanout Worker
- Batch processing để tạo `user_notification`
- Hiện tại: Cần insert `user_notification` thủ công

### ⏳ Realtime Dispatch
- WebSocket push
- Hiện tại: Channel REALTIME được lưu nhưng chưa push thật

### ⏳ Email Delivery
- Email template rendering
- Provider integration
- Hiện tại: Email delivery log được tạo nhưng chưa gửi thật

### ⏳ Unread Count Cache
- Redis cache
- Hiện tại: Query trực tiếp từ database

### ⏳ Admin Features
- Cancel/retry implementation
- Hiện tại: Endpoints trả lỗi "chưa được triển khai"

---

## 10. Troubleshooting

### Service không khởi động được
```bash
# Check PostgreSQL
psql -U infinite -d infinite_world -c "SELECT 1"

# Check schema exists
psql -U infinite -d infinite_world -c "\dn INF_NOTI"

# Check tables
psql -U infinite -d infinite_world -c "\dt INF_NOTI.*"
```

### API trả 404
```bash
# Check service đang chạy
curl http://localhost:8084/actuator/health

# Check port đúng chưa (8084 theo .env)
netstat -an | grep 8084
```

### Inbox trống
```bash
# Check có data không
psql -U infinite -d infinite_world -c "
SELECT COUNT(*) FROM INF_NOTI.user_notification WHERE user_id = 123;
"

# Nếu = 0, cần insert test data (xem phần Chuẩn bị môi trường)
```

### Claim không được
```bash
# Check notification có reward không
psql -U infinite -d infinite_world -c "
SELECT id, reward_payload, is_claimed, expire_at 
FROM INF_NOTI.user_notification 
WHERE id = 3 AND user_id = 123;
"

# Check đã claim chưa
# Check hết hạn chưa
```

---

## Kết luận

BUOC2 đã hoàn thành nền tảng notification với:
- ✅ 3 nhóm API đầy đủ
- ✅ Database schema hoàn chỉnh
- ✅ Idempotency và audit trail
- ✅ Claim reward an toàn
- ✅ I18n support

Sẵn sàng cho:
- Tích hợp với các service khác
- Phát triển fanout worker (BUOC3)
- Phát triển realtime dispatch (BUOC4)
- Phát triển email delivery (BUOC5)
