# Thiết kế chuẩn cho `notification-service` của Infinite World

## 1. Mục tiêu cuối cùng

Tài liệu này chốt thiết kế `notification-service` theo hướng dùng lâu dài, để các service khác chỉ cần tích hợp theo contract chung rồi dùng ổn định về sau, không phải sửa kiến trúc liên tục.

Mục tiêu chính:

- `notification-service` là service chuyên trách duy nhất cho thông báo
- các service khác không tự gửi email/WebSocket trực tiếp
- mọi loại thông báo đều đi qua contract chuẩn
- scale tốt tới 1.000.000 user
- maintain tốt, code clean, dễ audit, dễ retry, dễ mở rộng

Mục tiêu kỹ thuật:

- không fanout trong request
- không gửi trùng khi retry
- có delivery log đầy đủ
- có mô hình tích hợp dùng chung cho tất cả service
- có versioning contract để không phá vỡ tích hợp cũ

---

## 2. Quyết định kiến trúc chốt

`notification-service` sẽ là nền tảng thông báo dùng chung cho toàn hệ thống.

Các service khác như:

- `user-service`
- `game-service`
- `payment-service`
- `event-service`
- `admin-service`

không nên tự xử lý gửi notification ra ngoài, mà chỉ:

- gọi API của `notification-service`, hoặc
- publish event chuẩn dùng chung vào Kafka

`notification-service` chịu trách nhiệm:

- nhận yêu cầu gửi thông báo
- validate request
- persist dữ liệu
- fanout tới user
- push realtime
- gửi email
- ghi log delivery
- retry / DLQ / audit

Đây là hướng tốt nhất nếu muốn về sau chỉ mở rộng chứ không phải sửa lại nền.

---

## 3. Phân lớp trách nhiệm

Để code clean và service boundary rõ ràng, phải tách 3 lớp:

### 3.1. Notification Request Layer

Là nơi nhận yêu cầu từ service khác.

Nguồn vào có thể là:

- REST API đồng bộ
- Kafka event bất đồng bộ

Layer này chỉ làm:

- xác thực caller
- validate contract
- ghi nhận request
- tạo command/job xử lý tiếp

### 3.2. Notification Domain Layer

Là lõi nghiệp vụ của `notification-service`.

Phụ trách:

- phân loại notification
- áp rule người nhận
- tạo campaign
- tạo inbox records
- cập nhật trạng thái read/claim
- tạo delivery logs

### 3.3. Notification Delivery Layer

Phụ trách gửi ra ngoài:

- WebSocket / realtime
- email

Layer này không tự quyết định nghiệp vụ, chỉ làm delivery theo command chuẩn.

---

## 4. Những nguyên tắc phải giữ để không phải sửa kiến trúc về sau

### 4.1. Tất cả notification phải đi qua contract chuẩn

Không cho phép mỗi service tự định nghĩa payload notification riêng theo ý mình.

Phải có model dùng chung.

### 4.2. Tách request contract và delivery contract

Service gọi vào chỉ nên biết:

- muốn gửi gì
- gửi cho ai
- metadata nghiệp vụ là gì

Service gọi vào không nên biết quá sâu:

- batch size là bao nhiêu
- Kafka topic nội bộ nào dùng để fanout
- delivery provider nào đang được dùng

### 4.3. Dùng `notification-contract` làm nơi chứa contract dùng chung

Repo hiện tại đã có `common.dto.event`.

Nhưng hướng đúng về lâu dài là tách riêng `notification-contract` để chứa:

- notification request event
- realtime event
- email request event
- enum dùng chung
- idempotency metadata
- tracing metadata

Không để từng service copy/paste DTO.

### 4.4. Contract phải versioned

Nếu sau này thay đổi payload, phải version.

Ví dụ:

- `notification.request.v1`
- `notification.request.v2`

Hoặc trong DTO có:

- `schemaVersion`

Nếu không làm điều này ngay từ đầu thì sau này chỉ cần 2 service nâng cấp lệch version là sẽ rối.

---

## 5. Mô hình tích hợp chuẩn cho các service khác

Về lâu dài nên hỗ trợ cả 2 kiểu tích hợp:

## 5.1. Cách 1: REST API

Phù hợp khi service caller cần:

- biết kết quả ngay
- tạo notification theo thao tác admin
- đồng bộ trạng thái dễ hơn

Ví dụ:

- admin tạo campaign
- game master tạo mail/quà

## 5.2. Cách 2: Kafka event

Phù hợp khi service caller chỉ phát sinh domain event.

Ví dụ:

- user đăng ký thành công
- user bị khóa
- event ingame bắt đầu
- thanh toán thành công

Service phát sinh event chỉ publish một event chuẩn, `notification-service` tự quyết định gửi gì tiếp.

## 5.3. Khuyến nghị chốt

Nên dùng cả 2:

- REST cho command nghiệp vụ trực tiếp
- Kafka cho domain event tự động

Đây là mô hình cân bằng nhất và ít phải đập đi làm lại sau này.

---

## 6. Contract dùng chung phải có trong `notification-contract`

Phần này là trọng tâm để các service khác tái sử dụng sạch sẽ.

## 6.1. Base metadata chung

Mọi request/event gửi sang `notification-service` nên có chung các field:

- `eventId`
- `requestId`
- `traceId`
- `sourceService`
- `sourceModule`
- `sourceAction`
- `schemaVersion`
- `occurredAt`
- `requestedBy`
- `idempotencyKey`

Ý nghĩa:

- `eventId`: id của event phát ra
- `requestId`: id theo request/transaction của caller
- `traceId`: theo dõi xuyên service
- `sourceService`: service nào gọi
- `sourceModule`: module nghiệp vụ nào gọi
- `sourceAction`: hành động nào gây ra notification
- `schemaVersion`: version contract
- `idempotencyKey`: chống tạo trùng khi retry

## 6.2. NotificationTarget dùng chung

Thay vì mỗi event tự nhét `userId`, `roles`, `all`, `segment` theo kiểu khác nhau, nên có target model chuẩn.

Ví dụ:

```json
{
  "type": "USER_IDS",
  "userIds": [1, 2, 3]
}
```

hoặc:

```json
{
  "type": "ROLE",
  "roles": ["ADMIN", "MODERATOR"]
}
```

Nên định nghĩa class chung:

- `NotificationTarget`
- `NotificationTargetType`

## 6.3. NotificationContent dùng chung

Phải có object nội dung thống nhất:

- `title`
- `body`
- `type`
- `priority`
- `imageUrl`
- `action`
- `reward`
- `localePolicy`

## 6.4. NotificationAction dùng chung

Không để `actionTarget` rời rạc.

Nên chuẩn hóa:

- `actionType`
- `screen`
- `url`
- `deeplink`
- `payload`

Ví dụ:

- `OPEN_SCREEN`
- `OPEN_URL`
- `OPEN_DEEPLINK`
- `CLAIM_REWARD`
- `NONE`

## 6.5. NotificationChannel dùng chung

Phải có enum dùng chung:

- `INBOX`
- `REALTIME`
- `EMAIL`

Một request có thể đi nhiều channel cùng lúc.

Ví dụ:

- inbox + realtime
- email only
- inbox + realtime + email

## 6.6. NotificationRequest chuẩn

Đây nên là contract trung tâm dùng chung cho các service gọi vào.

Ví dụ tối thiểu:

```json
{
  "schemaVersion": "v1",
  "eventId": "01HT...",
  "requestId": "req-123",
  "traceId": "trace-abc",
  "sourceService": "user-service",
  "sourceModule": "auth",
  "sourceAction": "user_registered",
  "idempotencyKey": "user_registered:123",
  "channels": ["EMAIL", "REALTIME"],
  "target": {
    "type": "USER_IDS",
    "userIds": [123]
  },
  "content": {
    "type": "SYSTEM",
    "priority": 1,
    "title": "Chào mừng bạn",
    "body": "Tài khoản của bạn đã được tạo thành công"
  },
  "action": {
    "actionType": "OPEN_SCREEN",
    "screen": "profile"
  },
  "metadata": {
    "businessId": "user-123"
  }
}
```

Khuyến nghị:

- tạo class `NotificationRequestEvent` trong `notification-contract`
- đây là payload chuẩn nhất cho các service khác dùng

---

## 7. Những contract nào nên có ngay trong `notification-contract`

Để dùng chung và clean code, nên chốt bộ contract sau:

- `NotificationRequestEvent`
- `NotificationTarget`
- `NotificationContent`
- `NotificationAction`
- `NotificationReward`
- `NotificationChannel`
- `NotificationPriority`
- `NotificationType`
- `NotificationStatus`
- `DeliveryStatus`
- `NotificationSourceMetadata`

Ngoài ra nên có interface hoặc abstract base:

- `BaseNotificationEvent`

để các event khác như email/realtime kế thừa pattern field thống nhất.

---

## 8. Quan hệ giữa contract tổng và các event delivery hiện có

Repo hiện tại đã có:

- `EmailNotificationEvent`
- `WebSocketNotificationEvent`

Các event này không nên là contract đầu vào chính cho các service khác nữa.

Chúng nên trở thành:

- contract nội bộ của `notification-service`
- hoặc contract delivery downstream

Hướng chuẩn:

1. service khác gửi `NotificationRequestEvent`
2. `notification-service` xử lý nghiệp vụ
3. nếu cần email thì chuyển thành `EmailNotificationEvent`
5. nếu cần realtime thì chuyển thành `WebSocketNotificationEvent`

Như vậy boundary sẽ sạch hơn:

- service khác không cần biết email template cụ thể
- service khác không cần biết websocket payload cụ thể
- mọi logic mapping nằm tập trung trong `notification-service`

Đây là điểm rất quan trọng nếu muốn tránh sửa đi sửa lại sau này.

---

## 9. Thiết kế dữ liệu chuẩn cho production

Schema đề xuất: `INF_NOTIFICATION`

## 9.1. `notification_request`

Lưu request gốc từ service khác.

```sql
CREATE TABLE INF_NOTIFICATION.notification_request (
    id BIGSERIAL PRIMARY KEY,
    event_id VARCHAR(100) NOT NULL,
    request_id VARCHAR(100),
    trace_id VARCHAR(100),
    source_service VARCHAR(100) NOT NULL,
    source_module VARCHAR(100),
    source_action VARCHAR(100),
    schema_version VARCHAR(20) NOT NULL,
    idempotency_key VARCHAR(255) NOT NULL,
    request_payload JSONB NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACCEPTED',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_notification_request_event UNIQUE (event_id),
    CONSTRAINT uk_notification_request_idempotency UNIQUE (idempotency_key)
);
```

Lợi ích:

- audit request đầu vào
- chống duplicate từ caller
- truy vết đầy đủ theo service nguồn

## 9.2. `notification_template`

Lưu thông báo gốc đã chuẩn hóa.

```sql
CREATE TABLE INF_NOTIFICATION.notification_template (
    id BIGSERIAL PRIMARY KEY,
    request_id BIGINT NOT NULL,
    code VARCHAR(100),
    title VARCHAR(255) NOT NULL,
    body TEXT NOT NULL,
    type VARCHAR(30) NOT NULL,
    priority SMALLINT NOT NULL DEFAULT 0,
    image_url VARCHAR(1000),
    action_payload JSONB,
    reward_payload JSONB,
    channel_payload JSONB NOT NULL,
    start_at TIMESTAMP NULL,
    expire_at TIMESTAMP NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_notification_template_request
        FOREIGN KEY (request_id)
        REFERENCES INF_NOTIFICATION.notification_request(id)
);
```

`channel_payload` dùng để lưu cấu hình channel đã chuẩn hóa:

- inbox
- realtime
- email

## 9.3. `notification_target_rule`

```sql
CREATE TABLE INF_NOTIFICATION.notification_target_rule (
    id BIGSERIAL PRIMARY KEY,
    notification_id BIGINT NOT NULL,
    rule_type VARCHAR(30) NOT NULL,
    rule_payload JSONB NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_target_rule_template
        FOREIGN KEY (notification_id)
        REFERENCES INF_NOTIFICATION.notification_template(id)
        ON DELETE CASCADE
);
```

## 9.4. `notification_delivery_job`

```sql
CREATE TABLE INF_NOTIFICATION.notification_delivery_job (
    id BIGSERIAL PRIMARY KEY,
    notification_id BIGINT NOT NULL,
    job_type VARCHAR(30) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    total_target BIGINT NOT NULL DEFAULT 0,
    processed_target BIGINT NOT NULL DEFAULT 0,
    success_target BIGINT NOT NULL DEFAULT 0,
    failed_target BIGINT NOT NULL DEFAULT 0,
    retry_count INT NOT NULL DEFAULT 0,
    last_error TEXT,
    scheduled_at TIMESTAMP NULL,
    started_at TIMESTAMP NULL,
    finished_at TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_delivery_job_template
        FOREIGN KEY (notification_id)
        REFERENCES INF_NOTIFICATION.notification_template(id)
        ON DELETE CASCADE
);
```

## 9.5. `notification_delivery_batch`

```sql
CREATE TABLE INF_NOTIFICATION.notification_delivery_batch (
    id BIGSERIAL PRIMARY KEY,
    job_id BIGINT NOT NULL,
    batch_no INT NOT NULL,
    cursor_value BIGINT,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    expected_count INT NOT NULL DEFAULT 0,
    processed_count INT NOT NULL DEFAULT 0,
    success_count INT NOT NULL DEFAULT 0,
    failed_count INT NOT NULL DEFAULT 0,
    retry_count INT NOT NULL DEFAULT 0,
    last_error TEXT,
    started_at TIMESTAMP NULL,
    finished_at TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_delivery_batch_job
        FOREIGN KEY (job_id)
        REFERENCES INF_NOTIFICATION.notification_delivery_job(id)
        ON DELETE CASCADE,
    CONSTRAINT uk_delivery_batch UNIQUE (job_id, batch_no)
);
```

## 9.6. `user_notification`

```sql
CREATE TABLE INF_NOTIFICATION.user_notification (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    notification_id BIGINT NOT NULL,
    title VARCHAR(255) NOT NULL,
    body TEXT NOT NULL,
    type VARCHAR(30) NOT NULL,
    priority SMALLINT NOT NULL DEFAULT 0,
    image_url VARCHAR(1000),
    action_payload JSONB,
    reward_payload JSONB,
    status VARCHAR(20) NOT NULL DEFAULT 'UNREAD',
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    is_claimed BOOLEAN NOT NULL DEFAULT FALSE,
    read_at TIMESTAMP NULL,
    claimed_at TIMESTAMP NULL,
    delivered_at TIMESTAMP NULL,
    expire_at TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_user_notification UNIQUE (user_id, notification_id)
);

CREATE INDEX idx_un_user_created
    ON INF_NOTIFICATION.user_notification(user_id, created_at DESC);
CREATE INDEX idx_un_user_status
    ON INF_NOTIFICATION.user_notification(user_id, status, is_deleted);
```

## 9.7. `user_notification_claim_log`

```sql
CREATE TABLE INF_NOTIFICATION.user_notification_claim_log (
    id BIGSERIAL PRIMARY KEY,
    user_notification_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    reward_payload JSONB NOT NULL,
    claimed_result VARCHAR(20) NOT NULL,
    reference_code VARCHAR(100),
    claimed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_claim_user_notification
        FOREIGN KEY (user_notification_id)
        REFERENCES INF_NOTIFICATION.user_notification(id)
        ON DELETE CASCADE
);
```

## 9.8. `email_delivery_log`

```sql
CREATE TABLE INF_NOTIFICATION.email_delivery_log (
    id BIGSERIAL PRIMARY KEY,
    event_id VARCHAR(100) NOT NULL,
    notification_id BIGINT NULL,
    user_id BIGINT NULL,
    source_service VARCHAR(100),
    source_action VARCHAR(100),
    to_email VARCHAR(254) NOT NULL,
    email_type VARCHAR(50),
    template_code VARCHAR(100),
    subject VARCHAR(255),
    payload JSONB,
    provider VARCHAR(50),
    provider_message_id VARCHAR(255),
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    retry_count INT NOT NULL DEFAULT 0,
    error_message TEXT,
    requested_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    sent_at TIMESTAMP NULL,
    delivered_at TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_email_delivery_event UNIQUE (event_id)
);
```

## 10. Tại sao phải có `notification_request`

Đây là bảng rất quan trọng để hệ thống dùng chung cho nhiều service mà vẫn sạch.

Nếu không có bảng này:

- khó audit service nào yêu cầu gì
- khó xử lý idempotency từ caller
- khó hỗ trợ retry an toàn
- khó debug tích hợp xuyên service

Vì `notification-service` là nền tảng dùng chung, bảng này nên được xem là bắt buộc.

---

## 11. Quy trình xử lý chuẩn

## 11.1. Inbound flow

1. service khác gọi REST hoặc publish `NotificationRequestEvent`
2. `notification-service` validate contract
3. kiểm tra `idempotencyKey`
4. insert `notification_request`
5. chuẩn hóa thành `notification_template`
6. tạo `notification_target_rule`
7. tạo `notification_delivery_job`
8. publish job vào Kafka

## 11.2. Fanout flow

1. worker đọc `notification.delivery.requested`
2. đếm target
3. chia thành `notification_delivery_batch`
4. publish từng batch
5. batch worker query user theo cursor
6. bulk insert `user_notification`
7. update unread count cache
8. push realtime nếu có channel `REALTIME`

## 11.3. External delivery flow

1. domain layer xác định cần email
2. map sang `EmailNotificationEvent`
3. insert delivery log trạng thái `PENDING`
4. publish topic email
5. consumer gửi provider
6. update log `SENT` / `FAILED`
7. nếu có callback provider thì update `DELIVERED`

---

## 12. Tích hợp với các service khác như thế nào cho sạch nhất

Đây là phần cần làm đúng ngay từ đầu.

## 12.1. Không cho service khác gọi Kafka topic nội bộ lung tung

Service khác không nên biết:

- `notification.delivery.batch.requested`
- `notification.realtime.dispatch`
- `notification.dead-letter`

Các topic này là nội bộ của `notification-service`.

Service ngoài chỉ nên biết:

- REST public API của notification
- hoặc topic contract public như `notification.request.v1`

## 12.2. Tạo một integration SDK nhẹ dùng chung

Nên dùng trực tiếp module `notification-contract`.

Phần helper mức nhẹ trong module này cung cấp:

- request DTO
- event DTO
- helper builder
- topic names public
- validation cơ bản

Ví dụ:

- `NotificationRequestBuilder`
- `NotificationTargetBuilder`
- `NotificationChannelSet`

Mục tiêu:

- giảm code lặp ở các service caller
- giảm sai contract
- clean code hơn

## 12.3. Chuẩn hóa topic public

Nên chỉ public 1 hoặc 2 topic ở boundary:

- `notification.request.v1`
- `notification.status.changed.v1` nếu cần callback trạng thái

Không public toàn bộ topic nội bộ.

## 12.4. Callback trạng thái nếu cần

Một số service có thể muốn biết kết quả notification.

Ví dụ:

- payment service muốn biết email invoice đã gửi chưa
- game service muốn biết mail reward đã tạo inbox xong chưa

Khi đó `notification-service` có thể phát event dùng chung:

- `notification.status.changed.v1`

Payload gồm:

- `eventId`
- `sourceService`
- `sourceAction`
- `businessId`
- `channel`
- `status`
- `errorCode`
- `occurredAt`

Không nên callback quá chi tiết ngay từ đầu. Chỉ publish trạng thái nghiệp vụ cần thiết.

---

## 13. Chiến lược scale 1 triệu user

## 13.1. Không dùng offset lớn

Fanout phải dùng cursor:

- `WHERE id > :lastId ORDER BY id LIMIT :batchSize`

## 13.2. Batch insert bằng JDBC/native

Không save từng record qua JPA.

## 13.3. Partition `user_notification`

Khuyến nghị:

- partition theo tháng bằng `created_at`

## 13.4. Read replica cho user source

Nếu số campaign lớn:

- `notification-service` nên đọc user target từ read replica hoặc quyền read-only schema user

Không nên gọi REST paging sang `user-service` để fanout lớn.

## 13.5. Redis chỉ cache unread count

Không dùng Redis làm inbox store.

## 13.6. Realtime chỉ là phụ trợ

Nếu realtime lỗi:

- inbox vẫn còn
- user vẫn đọc lại được

---

## 14. Idempotency chuẩn hệ thống

Vì nhiều service cùng gọi vào, phần này phải chốt rất kỹ.

## 14.1. Ở boundary request

Mỗi request phải có `idempotencyKey`.

Ví dụ:

- `user_registered:123`
- `invoice_paid:INV-0001`
- `reward_event:evt-2026-01:user-99`

Nếu caller retry cùng key:

- không tạo campaign mới
- trả lại kết quả request cũ

## 14.2. Ở inbox delivery

Chống trùng bằng:

- unique `(user_id, notification_id)`

## 14.3. Ở email delivery

Chống trùng bằng:

- unique `event_id` trong delivery log

## 14.4. Ở reward claim

Chống claim trùng bằng:

- transaction
- row lock
- claim log

---

## 15. API public nên có

## 15.1. Admin / internal command API

- `POST /api/admin/notifications`
- `GET /api/admin/notifications`
- `GET /api/admin/notifications/{id}`
- `POST /api/admin/notifications/{id}/cancel`
- `POST /api/admin/notifications/{id}/retry`
- `GET /api/admin/notifications/{id}/delivery-summary`

## 15.2. Service integration API

Đây là API các service khác gọi vào trực tiếp nếu không dùng Kafka.

- `POST /api/internal/notifications/requests`

Request body là `NotificationRequestEvent`.

Response nên trả:

- `requestId`
- `eventId`
- `status`
- `notificationId` nếu đã tạo được

## 15.3. Client API

- `GET /api/notifications`
- `GET /api/notifications/unread-count`
- `POST /api/notifications/{id}/read`
- `POST /api/notifications/read-all`
- `POST /api/notifications/{id}/delete`
- `POST /api/notifications/{id}/claim`

## 15.4. Delivery audit API

- `GET /api/admin/email-deliveries`
- `GET /api/admin/email-deliveries/{id}`
- `POST /api/admin/email-deliveries/{id}/retry`
## 16. Versioning và backward compatibility

Nếu muốn không phải sửa đi sửa lại, phải chốt nguyên tắc này ngay.

Nguyên tắc:

- contract public phải versioned
- field mới chỉ được thêm theo hướng backward compatible
- không đổi nghĩa field cũ
- không xóa field public đang dùng nếu chưa qua deprecation cycle

Khuyến nghị:

- mọi public event có `schemaVersion`
- topic public có suffix version

Ví dụ:

- `notification.request.v1`
- `notification.status.changed.v1`

---

## 17. Observability bắt buộc phải có

Metric tối thiểu:

- số request nhận vào theo `sourceService`
- số request bị duplicate theo `idempotencyKey`
- số campaign tạo mới
- số batch đang `PENDING`, `PROCESSING`, `FAILED`
- fanout throughput
- inbox insert latency
- unread cache hit rate
- email success/failure rate
- realtime push success/failure rate
- DLQ size

Structured log phải có:

- `eventId`
- `requestId`
- `traceId`
- `sourceService`
- `sourceAction`
- `notificationId`
- `jobId`
- `batchId`
- `userId`

---

## 18. Cấu trúc code đề xuất để clean nhất

```text
notification
└── src/main/java/com/infinite/notification
    ├── api
    │   ├── admin
    │   ├── client
    │   └── internal
    ├── application
    │   ├── command
    │   ├── query
    │   └── mapper
    ├── domain
    │   ├── model
    │   ├── service
    │   └── policy
    ├── infrastructure
    │   ├── persistence
    │   ├── messaging
    │   ├── redis
    │   ├── email
    │   └── websocket
    ├── worker
    ├── scheduler
    └── config
```

Khuyến nghị rõ:

- `api`: nhận request/response
- `application`: orchestration use case
- `domain`: business rule
- `infrastructure`: JPA/Kafka/Redis/provider

Không nên nhét hết vào `service/impl` như kiểu CRUD thường thấy, vì sau này service này sẽ lớn.

---

## 19. Những thứ nên đưa vào `notification-contract`

Chốt danh sách nên chuẩn hóa ngay trong `notification-contract`:

- DTO request/event public
- enum channel/type/status/priority
- base metadata model
- topic name public
- builder/helper cho request
- constants về schema version

Không nên đưa vào `notification-contract`:

- entity JPA của `notification-service`
- repository
- logic fanout
- logic provider email

Nguyên tắc:

- `notification-contract` chỉ chứa contract và helper tích hợp mức nhẹ
- không biến module này thành nơi nhét logic nghiệp vụ notification

---

## 20. Cách tách module chốt cuối cùng

Quyết định cuối cùng cho repo này:

- chỉ tách đúng `1` module dependency dùng chung: `notification-contract`
- `notification-service` vẫn giữ là `1` module triển khai duy nhất
- chưa tách `email`, `realtime`, `inbox`, `admin` thành các Maven module riêng trong giai đoạn này

Đây là phương án cân bằng nhất giữa:

- sạch kiến trúc
- dễ tìm kiếm
- dễ tích hợp
- không over-engineer
- không làm build/dependency phức tạp quá sớm

## 20.1. Module `notification-contract`

Đây là module dependency duy nhất mà các service khác cần dùng.

Module này chứa:

- `NotificationRequestEvent`
- `NotificationTarget`
- `NotificationContent`
- `NotificationAction`
- `NotificationReward`
- `NotificationChannel`
- `NotificationPriority`
- `NotificationType`
- `NotificationStatus`
- `DeliveryStatus`
- `BaseNotificationEvent`
- public topic names
- schema version constants
- builder/helper mức nhẹ
- client helper mức nhẹ nếu cần

Phần "client helper mức nhẹ" được phép gộp luôn vào `notification-contract`, ví dụ:

- `NotificationRequestBuilder`
- `NotificationTopicNames`
- `NotificationPublisher` interface

Nhưng chỉ dừng ở mức mỏng:

- helper build request
- interface publish/call
- constants và validation nhẹ

Không đưa vào đây:

- Kafka implementation nặng
- REST implementation nặng
- retry policy phức tạp
- fallback logic
- business logic notification

## 20.2. Những gì để lại trong `notification-service`

Toàn bộ phần nghiệp vụ thật phải ở lại trong `notification-service`:

- entity JPA
- repository
- request persistence
- template persistence
- delivery job/batch worker
- target resolution logic
- inbox query logic
- unread count logic
- email delivery log persistence
- mapping từ request contract sang delivery command
- realtime dispatch
- admin API
- client API
- internal API

Lý do:

- đây là lõi domain của notification
- tách ra sớm thành nhiều Maven module sẽ làm boundary rối hơn lợi ích nhận được

## 20.3. Có nên tách email thành module riêng không

Hiện tại: `không nên`.

Nên làm:

- giữ email trong `notification-service`
- tách package rõ trong service

Ví dụ:

- `infrastructure.email`
- `application.command.email`
- `domain.delivery`

Chỉ nên tách email thành Maven module riêng sau này nếu có đủ dấu hiệu:

- nhiều provider
- logic template/rendering rất lớn
- cần scale/deploy độc lập
- team phụ trách riêng

Ở giai đoạn hiện tại, tách package là đủ tốt và sạch hơn nhiều so với tách module.

## 20.4. Cấu trúc module thực tế nên dùng

Nên chốt như sau:

- `common`
  - giữ utility/global components chung toàn hệ thống
- `notification-contract`
  - module dependency dùng chung cho tất cả service cần tích hợp notification
- `notification`
  - service triển khai toàn bộ nghiệp vụ notification

Tức là:

- không tiếp tục nhét contract notification vào `common`
- không tạo thêm `notification-client` thành module riêng
- không tách `notification-email` thành module riêng ở thời điểm này

Đây là cấu trúc ít rủi ro nhất và dễ giữ ổn định lâu dài nhất.

---

## 21. Những gì cần sửa trong repo để đúng hướng này

### 21.1. Trong `common`

Cần giữ `common` gọn lại.

Không nên tiếp tục đưa thêm contract notification mới vào `common`.

Nếu hiện tại đang có DTO/event notification trong `common`, hướng đúng là chuyển dần chúng sang `notification-contract`.

Nên xem `EmailNotificationEvent` và `WebSocketNotificationEvent` là downstream/internal contracts, không phải request contract public chính nữa.

### 21.1.a. Trong `notification-contract`

Cần bổ sung:

- `NotificationRequestEvent`
- `NotificationTarget`
- `NotificationContent`
- `NotificationAction`
- `NotificationChannel`
- `DeliveryStatus`
- `BaseNotificationEvent`
- builder/helper mức nhẹ cho caller
- public topic names
- schema version constants

### 21.2. Trong `notification`

Cần bổ sung:

- JPA + PostgreSQL
- bảng request/template/job/batch/inbox/log
- internal REST API
- admin API
- client API
- fanout worker
- email delivery log service
- status callback publisher nếu cần
- tách package nội bộ rõ ràng theo `api / application / domain / infrastructure`

### 21.3. Trong các service caller

Cần thay đổi dần:

- không publish trực tiếp `WebSocketNotificationEvent` từ business service nữa
- không business service nào gọi provider email trực tiếp
- dùng `NotificationRequestEvent` hoặc internal notification API

---

## 22. Lộ trình triển khai đúng để không phải làm lại

### Phase 1

Chuẩn hóa contract.

- bổ sung DTO/event chung trong `notification-contract`
- chốt topic public
- chốt REST internal API

### Phase 2

Xây lõi `notification-service`.

- thêm persistence
- thêm request/template/job/batch/inbox
- thêm fanout worker

### Phase 3

Chuẩn hóa external delivery.

- email delivery log
- retry / DLQ
- callback trạng thái nếu cần

### Phase 4

Tối ưu quy mô lớn.

- partition
- read replica
- tuning Kafka
- tuning retention
- metrics/dashboard đầy đủ

---

## 23. Kết luận cuối cùng

Nếu mục tiêu là “làm một lần cho đúng nền, về sau cứ tích hợp mà dùng”, thì bản thiết kế chuẩn nhất cho dự án này là:

- `notification-service` là entrypoint duy nhất cho thông báo
- service khác chỉ giao tiếp qua contract public chuẩn
- contract public được đặt ở `notification-contract` và có version
- `NotificationRequestEvent` là request model trung tâm
- email/realtime event chỉ là delivery contracts phía trong
- toàn bộ request đều có idempotency, source metadata, trace metadata
- fanout dùng job + batch + cursor + bulk insert
- mọi delivery đều có log riêng để audit và retry
- code tổ chức theo hướng application/domain/infrastructure, không làm kiểu service CRUD phình to

Đây là hướng ít rủi ro nhất, dễ scale nhất, và sạch nhất để giữ ổn định lâu dài.
