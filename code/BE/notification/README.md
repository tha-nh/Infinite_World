# Notification Service

Cap nhat: 2026-05-17

## Tong quan

`notification` la service trung tam phu trach notification cua Infinite World. Service nay nhan request notification tu service khac, luu audit request, tao template/target rule, fanout inbox, dispatch realtime, enqueue email, quan ly email delivery log, cache unread count, retry, DLQ, metric va cleanup retention.

Service nay la entrypoint notification duy nhat. Cac service business nhu `user-service` khong gui email/websocket truc tiep nua, ma gui `NotificationRequestEvent`.

## Trach nhiem chinh

- Public notification contract consumer:
  - Kafka topic `notification.request.v1`
  - Internal REST API `/api/internal/notifications/requests`
- Admin API:
  - tao notification
  - search/detail notification
  - cancel/retry
  - delivery summary
- Client inbox API:
  - lay inbox
  - unread count
  - read/read-all/delete
  - claim reward
- Fanout worker:
  - tao delivery job
  - chia batch
  - bulk insert inbox
- Email delivery:
  - tao `email_delivery_log`
  - publish email event noi bo
  - gui SMTP
  - update SENT/FAILED
  - audit/retry email
- Realtime delivery:
  - publish downstream websocket topic sau khi inbox insert thanh cong
- Cache:
  - Redis unread count
  - fallback DB khi cache miss
- Reliability:
  - idempotency key
  - inbox unique
  - retry co gioi han
  - DLQ co ban
- Observability:
  - Micrometer metrics
  - structured logs co context
- Cleanup:
  - inbox cu
  - email log cu
  - delivery job/batch cu

## Khong phu trach

- Khong quan ly user profile.
- Khong tu query user-service de lay email/user list cho moi loai target.
- Khong lam email provider rieng; hien gui qua Spring Mail/SMTP.
- Khong luu file/image notification.
- Khong thay gateway xac thuc JWT; client API tin `X-USER-ID` do gateway set.

## Runtime

Port local:

```text
8084
```

Spring app name:

```yaml
spring.application.name: notification-service
```

Schema DB:

```properties
NOTI_SCHEMA=INF_NOTI
```

Messaging provider mac dinh:

```properties
MESSAGING_PROVIDER=kafka
```

## Cau truc module

```text
notification/src/main/java/com/infinite/notification
|-- api
|   |-- admin
|   |   |-- AdminNotificationController.java
|   |   `-- AdminEmailDeliveryController.java
|   |-- client
|   |   `-- ClientNotificationController.java
|   `-- internal
|       `-- InternalNotificationController.java
|-- application
|   |-- command
|   `-- query
|-- consumer
|-- worker
|   |-- NotificationDeliveryJobWorker.java
|   `-- NotificationDeliveryBatchWorker.java
|-- scheduler
|-- infrastructure
|   |-- messaging
|   |-- observability
|   |-- persistence
|   `-- redis
|-- service
|-- dto
|-- messaging
`-- config
```

## Contract va public entrypoint

Public contract nam o module rieng:

```text
notification-contract
```

Contract chinh:

- `NotificationRequestEvent`
- `NotificationTarget`
- `NotificationContent`
- `NotificationAction`
- `NotificationReward`

Enum chinh:

- `NotificationChannel`: `INBOX`, `REALTIME`, `EMAIL`
- `NotificationTargetType`: `USER_IDS`, `ROLE`, `ALL`, `SEGMENT`, `QUERY`
- `NotificationType`: `SYSTEM`, `ACCOUNT`, `GAME`, `PAYMENT`, `SOCIAL`, `PROMOTION`, `ANNOUNCEMENT`
- `NotificationPriority`: `LOW`, `NORMAL`, `HIGH`, `URGENT`

Public topic:

```text
notification.request.v1
```

Internal REST endpoint:

```text
POST /api/internal/notifications/requests
```

## Flow tong quat

```text
Caller service
  -> notification.request.v1 hoac /api/internal/notifications/requests
  -> CreateNotificationRequestUseCase
  -> notification_request
  -> notification_template
  -> notification_target_rule
  -> notification_delivery_job
  -> notification.delivery.requested
  -> NotificationDeliveryJobWorker
  -> notification_delivery_batch
  -> notification.delivery.batch.requested
  -> NotificationDeliveryBatchWorker
  -> user_notification
  -> unread cache
  -> realtime/email downstream
```

Kafka event duoc publish sau DB transaction commit de worker khong doc record chua visible.

## API hien co

Base URL:

```text
http://localhost:8084
```

## Chuc nang va cach test bang API

### 1. Nhan notification request noi bo

Muc dich:

- Nhan `NotificationRequestEvent`.
- Luu audit request.
- Tao template/target rule/delivery job.
- Publish fanout event sau DB commit.

Curl:

```bash
curl --location 'http://localhost:8084/api/internal/notifications/requests' \
  --header 'Content-Type: application/json' \
  --data-raw '{
    "eventId": "manual-001",
    "requestId": "req-manual-001",
    "traceId": "trace-manual-001",
    "sourceService": "manual-test",
    "sourceModule": "readme",
    "sourceAction": "test_notification",
    "schemaVersion": "v1",
    "idempotencyKey": "manual-001",
    "channels": ["INBOX", "EMAIL", "REALTIME"],
    "target": {
      "type": "USER_IDS",
      "userIds": [1],
      "queryParams": {
        "emailByUserId": {
          "1": "user@example.com"
        }
      }
    },
    "content": {
      "type": "ACCOUNT",
      "priority": "NORMAL",
      "title": "Test notification",
      "body": "Test body",
      "locale": "vi",
      "templateVars": {
        "emailType": "FORGOT_PASSWORD_OTP",
        "otp": "123456",
        "expirationMinutes": 5
      }
    },
    "metadata": {
      "test": true
    }
  }'
```

Kiem tra DB:

```sql
SELECT * FROM INF_NOTI.notification_request ORDER BY id DESC LIMIT 5;
SELECT * FROM INF_NOTI.notification_delivery_job ORDER BY id DESC LIMIT 5;
SELECT * FROM INF_NOTI.notification_delivery_batch ORDER BY id DESC LIMIT 5;
SELECT * FROM INF_NOTI.user_notification ORDER BY id DESC LIMIT 5;
SELECT * FROM INF_NOTI.email_delivery_log ORDER BY id DESC LIMIT 5;
```

### 2. Admin tao notification

Muc dich:

- Admin tao notification bang cung contract voi internal API.
- Dung chung `CreateNotificationRequestUseCase`.

Curl:

```bash
curl --location 'http://localhost:8084/api/admin/notifications' \
  --header 'Content-Type: application/json' \
  --data-raw '{
    "eventId": "admin-001",
    "sourceService": "admin",
    "sourceModule": "notification",
    "sourceAction": "create_manual",
    "schemaVersion": "v1",
    "idempotencyKey": "admin-001",
    "channels": ["INBOX"],
    "target": {
      "type": "USER_IDS",
      "userIds": [1]
    },
    "content": {
      "type": "ANNOUNCEMENT",
      "priority": "NORMAL",
      "title": "Admin announcement",
      "body": "Thong bao tu admin",
      "locale": "vi"
    }
  }'
```

### 3. Admin search/detail/cancel/retry/delivery summary

Search:

```bash
curl --location 'http://localhost:8084/api/admin/notifications?page=0&size=20'
```

Search theo status:

```bash
curl --location 'http://localhost:8084/api/admin/notifications?page=0&size=20&status=PENDING'
```

Detail:

```bash
curl --location 'http://localhost:8084/api/admin/notifications/1'
```

Cancel:

```bash
curl --location --request POST 'http://localhost:8084/api/admin/notifications/1/cancel'
```

Retry:

```bash
curl --location --request POST 'http://localhost:8084/api/admin/notifications/1/retry'
```

Delivery summary:

```bash
curl --location 'http://localhost:8084/api/admin/notifications/1/delivery-summary'
```

### 4. Client inbox

Muc dich:

- User doc inbox cua chinh minh.
- Service tin `X-USER-ID` do gateway set.

Get inbox:

```bash
curl --location 'http://localhost:8084/api/notifications?page=0&size=20' \
  --header 'X-USER-ID: 1'
```

Unread count:

```bash
curl --location 'http://localhost:8084/api/notifications/unread-count' \
  --header 'X-USER-ID: 1'
```

Mark read:

```bash
curl --location --request POST 'http://localhost:8084/api/notifications/10/read' \
  --header 'X-USER-ID: 1'
```

Read all:

```bash
curl --location --request POST 'http://localhost:8084/api/notifications/read-all' \
  --header 'X-USER-ID: 1'
```

Delete inbox item:

```bash
curl --location --request POST 'http://localhost:8084/api/notifications/10/delete' \
  --header 'X-USER-ID: 1'
```

Claim reward:

```bash
curl --location --request POST 'http://localhost:8084/api/notifications/10/claim' \
  --header 'X-USER-ID: 1'
```

### 5. Email delivery audit

Muc dich:

- Xem email log.
- Loc theo status.
- Retry email failed.

Search:

```bash
curl --location 'http://localhost:8084/api/admin/email-deliveries?page=0&size=20'
```

Search failed:

```bash
curl --location 'http://localhost:8084/api/admin/email-deliveries?page=0&size=20&status=FAILED'
```

Detail:

```bash
curl --location 'http://localhost:8084/api/admin/email-deliveries/1'
```

Retry:

```bash
curl --location --request POST 'http://localhost:8084/api/admin/email-deliveries/1/retry'
```

### 6. Actuator

Health:

```bash
curl --location 'http://localhost:8084/actuator/health'
```

Metrics:

```bash
curl --location 'http://localhost:8084/actuator/metrics'
```

### Internal API

```text
POST /api/internal/notifications/requests
```

Dung cho service-to-service neu khong publish Kafka truc tiep.

### Admin notification API

Base path:

```text
/api/admin/notifications
```

Endpoint:

```text
POST /
GET  /?page=0&size=20&status=PENDING
GET  /{id}
POST /{id}/cancel
POST /{id}/retry
GET  /{id}/delivery-summary
```

Controller chi tra `ApiResponse<Object>` va delegate sang service. Khong wrap `ResponseEntity`, khong expose `PageResponse` truc tiep tai controller.

### Client inbox API

Base path:

```text
/api/notifications
```

Endpoint:

```text
GET  /?page=0&size=20
GET  /unread-count
POST /{id}/read
POST /read-all
POST /{id}/delete
POST /{id}/claim
```

Client API lay user tu trusted header:

```text
X-USER-ID: {userId}
```

Header nay phai do gateway set sau JWT validation. Client khong duoc tu truyen userId de doc inbox nguoi khac.

### Email delivery admin API

Base path:

```text
/api/admin/email-deliveries
```

Endpoint:

```text
GET  /?page=0&size=20&status=FAILED
GET  /{id}
POST /{id}/retry
```

## Vi du NotificationRequestEvent

```json
{
  "eventId": "manual-001",
  "requestId": "req-manual-001",
  "traceId": "trace-manual-001",
  "sourceService": "manual-test",
  "sourceModule": "readme",
  "sourceAction": "test_notification",
  "schemaVersion": "v1",
  "idempotencyKey": "manual-001",
  "channels": ["INBOX", "EMAIL"],
  "target": {
    "type": "USER_IDS",
    "userIds": [1],
    "queryParams": {
      "emailByUserId": {
        "1": "user@example.com"
      }
    }
  },
  "content": {
    "type": "ACCOUNT",
    "priority": "NORMAL",
    "title": "Test notification",
    "body": "Test body",
    "locale": "vi",
    "templateVars": {
      "emailType": "FORGOT_PASSWORD_OTP",
      "otp": "123456",
      "expirationMinutes": 5
    }
  },
  "metadata": {
    "test": true
  }
}
```

Gui qua REST:

```bash
curl -X POST "http://localhost:8084/api/internal/notifications/requests" \
  -H "Content-Type: application/json" \
  -d @request.json
```

## Target support

Contract da co:

- `USER_IDS`
- `ROLE`
- `ALL`
- `SEGMENT`
- `QUERY`

Hien tai fanout production chac chan cho:

```text
USER_IDS
```

Voi email channel va target `USER_IDS`, caller can cung cap email map:

```json
{
  "queryParams": {
    "emailByUserId": {
      "1": "user@example.com"
    }
  }
}
```

`ROLE`, `SEGMENT`, `ALL`, `QUERY` can target resolver/read-side source rieng truoc khi bat production, de tranh query REST/user-service theo offset lon.

## Database

Migration:

```text
notification/db/migration/V001__create_schema_and_tables.sql
```

Bang chinh:

- `notification_request`: audit request inbound, idempotency.
- `notification_template`: noi dung notification va channel payload.
- `notification_target_rule`: target rule da normalize.
- `notification_delivery_job`: job fanout tong.
- `notification_delivery_batch`: batch fanout.
- `user_notification`: inbox cua user.
- `user_notification_claim_log`: log claim reward.
- `email_delivery_log`: audit email delivery.

Rang buoc quan trong:

- unique `notification_request.event_id`
- unique `notification_request.idempotency_key`
- unique `(user_id, notification_id)` trong `user_notification`
- unique `email_delivery_log.event_id`

### Bang `notification_request`

Muc dich: audit moi inbound notification request va enforce idempotency.

| Column | Type | Nullable | Default | Ghi chu |
| --- | --- | --- | --- | --- |
| `id` | `BIGSERIAL` | No | | Primary key |
| `event_id` | `VARCHAR(100)` | No | | Unique event id tu caller |
| `request_id` | `VARCHAR(100)` | Yes | | Request id tu caller |
| `trace_id` | `VARCHAR(100)` | Yes | | Trace id neu co |
| `source_service` | `VARCHAR(100)` | No | | Service gui request, vi du `user-service` |
| `source_module` | `VARCHAR(100)` | Yes | | Module/domain cua caller |
| `source_action` | `VARCHAR(100)` | Yes | | Action sinh notification |
| `schema_version` | `VARCHAR(20)` | No | | Hien tai `v1` |
| `idempotency_key` | `VARCHAR(255)` | No | | Unique, chong tao trung notification |
| `request_payload` | `JSONB` | No | | Full `NotificationRequestEvent` |
| `status` | `VARCHAR(20)` | No | `ACCEPTED` | Trang thai request |
| `created_at` | `TIMESTAMP` | No | `CURRENT_TIMESTAMP` | Thoi diem tao |
| `updated_at` | `TIMESTAMP` | No | `CURRENT_TIMESTAMP` | Thoi diem cap nhat |

Index/constraint:

- `uk_notification_request_event` unique `event_id`
- `uk_notification_request_idempotency` unique `idempotency_key`
- `idx_notification_request_status(status, created_at DESC)`
- `idx_notification_request_source(source_service, source_action)`

### Bang `notification_template`

Muc dich: noi dung notification da normalize tu request.

| Column | Type | Nullable | Default | Ghi chu |
| --- | --- | --- | --- | --- |
| `id` | `BIGSERIAL` | No | | Primary key |
| `request_id` | `BIGINT` | No | | FK toi `notification_request.id` |
| `code` | `VARCHAR(100)` | Yes | | Thuong lay tu `NotificationType` |
| `title` | `VARCHAR(255)` | No | | Tieu de |
| `body` | `TEXT` | No | | Noi dung |
| `type` | `VARCHAR(30)` | No | | `SYSTEM`, `ACCOUNT`, ... |
| `priority` | `SMALLINT` | No | `0` | Priority numeric |
| `image_url` | `VARCHAR(1000)` | Yes | | Anh minh hoa neu co |
| `action_payload` | `JSONB` | Yes | | Action khi user click |
| `reward_payload` | `JSONB` | Yes | | Reward/claim payload |
| `channel_payload` | `JSONB` | No | | Channel flags va metadata noi bo (`INBOX`, `EMAIL`, `_locale`, `_templateVars`) |
| `start_at` | `TIMESTAMP` | Yes | | Thoi diem bat dau |
| `expire_at` | `TIMESTAMP` | Yes | | Thoi diem het han |
| `status` | `VARCHAR(20)` | No | `PENDING` | Trang thai template |
| `created_at` | `TIMESTAMP` | No | `CURRENT_TIMESTAMP` | Thoi diem tao |
| `updated_at` | `TIMESTAMP` | No | `CURRENT_TIMESTAMP` | Thoi diem cap nhat |

Index/constraint:

- FK `request_id` -> `notification_request(id)` on delete cascade
- `idx_notification_template_request(request_id)`
- `idx_notification_template_status(status, created_at DESC)`
- `idx_notification_template_code(code)`

### Bang `notification_target_rule`

Muc dich: luu target audience/rule.

| Column | Type | Nullable | Default | Ghi chu |
| --- | --- | --- | --- | --- |
| `id` | `BIGSERIAL` | No | | Primary key |
| `notification_id` | `BIGINT` | No | | FK toi `notification_template.id` |
| `rule_type` | `VARCHAR(30)` | No | | `USER_IDS`, `ROLE`, `ALL`, `SEGMENT`, `QUERY` |
| `rule_payload` | `JSONB` | No | | Payload target, vi du `userIds`, `queryParams.emailByUserId` |
| `created_at` | `TIMESTAMP` | No | `CURRENT_TIMESTAMP` | Thoi diem tao |

Index/constraint:

- FK `notification_id` -> `notification_template(id)` on delete cascade
- `idx_target_rule_notification(notification_id)`

### Bang `notification_delivery_job`

Muc dich: theo doi tien do job fanout tong.

| Column | Type | Nullable | Default | Ghi chu |
| --- | --- | --- | --- | --- |
| `id` | `BIGSERIAL` | No | | Primary key |
| `notification_id` | `BIGINT` | No | | FK toi `notification_template.id` |
| `job_type` | `VARCHAR(30)` | No | | Hien dung `INBOX_FANOUT` |
| `status` | `VARCHAR(20)` | No | `PENDING` | `PENDING`, `PROCESSING`, `COMPLETED`, `FAILED`, `CANCELED` |
| `total_target` | `BIGINT` | No | `0` | Tong target |
| `processed_target` | `BIGINT` | No | `0` | Da xu ly |
| `success_target` | `BIGINT` | No | `0` | Thanh cong |
| `failed_target` | `BIGINT` | No | `0` | That bai |
| `retry_count` | `INT` | No | `0` | So lan retry |
| `last_error` | `TEXT` | Yes | | Loi gan nhat |
| `scheduled_at` | `TIMESTAMP` | Yes | | Lich chay |
| `started_at` | `TIMESTAMP` | Yes | | Bat dau |
| `finished_at` | `TIMESTAMP` | Yes | | Ket thuc |
| `created_at` | `TIMESTAMP` | No | `CURRENT_TIMESTAMP` | Thoi diem tao |
| `updated_at` | `TIMESTAMP` | No | `CURRENT_TIMESTAMP` | Thoi diem cap nhat |

Index/constraint:

- FK `notification_id` -> `notification_template(id)` on delete cascade
- `idx_delivery_job_notification(notification_id)`
- `idx_delivery_job_status(status, scheduled_at)`

### Bang `notification_delivery_batch`

Muc dich: chia job fanout thanh batch nho de xu ly.

| Column | Type | Nullable | Default | Ghi chu |
| --- | --- | --- | --- | --- |
| `id` | `BIGSERIAL` | No | | Primary key |
| `job_id` | `BIGINT` | No | | FK toi `notification_delivery_job.id` |
| `batch_no` | `INT` | No | | Thu tu batch trong job |
| `cursor_value` | `BIGINT` | Yes | | Cursor/slice start hien tai |
| `status` | `VARCHAR(20)` | No | `PENDING` | `PENDING`, `PROCESSING`, `COMPLETED`, `FAILED` |
| `expected_count` | `INT` | No | `0` | So target du kien |
| `processed_count` | `INT` | No | `0` | So target da xu ly |
| `success_count` | `INT` | No | `0` | Insert/delivery thanh cong |
| `failed_count` | `INT` | No | `0` | That bai |
| `retry_count` | `INT` | No | `0` | So lan retry |
| `last_error` | `TEXT` | Yes | | Loi gan nhat |
| `started_at` | `TIMESTAMP` | Yes | | Bat dau |
| `finished_at` | `TIMESTAMP` | Yes | | Ket thuc |
| `created_at` | `TIMESTAMP` | No | `CURRENT_TIMESTAMP` | Thoi diem tao |
| `updated_at` | `TIMESTAMP` | No | `CURRENT_TIMESTAMP` | Thoi diem cap nhat |

Index/constraint:

- FK `job_id` -> `notification_delivery_job(id)` on delete cascade
- unique `(job_id, batch_no)`
- `idx_delivery_batch_job(job_id)`
- `idx_delivery_batch_status(status, created_at)`

### Bang `user_notification`

Muc dich: inbox notification da giao cho tung user.

| Column | Type | Nullable | Default | Ghi chu |
| --- | --- | --- | --- | --- |
| `id` | `BIGSERIAL` | No | | Primary key |
| `user_id` | `BIGINT` | No | | User nhan notification |
| `notification_id` | `BIGINT` | No | | Id template/notification |
| `title` | `VARCHAR(255)` | No | | Snapshot title |
| `body` | `TEXT` | No | | Snapshot body |
| `type` | `VARCHAR(30)` | No | | Type snapshot |
| `priority` | `SMALLINT` | No | `0` | Priority snapshot |
| `image_url` | `VARCHAR(1000)` | Yes | | Image snapshot |
| `action_payload` | `JSONB` | Yes | | Action snapshot |
| `reward_payload` | `JSONB` | Yes | | Reward snapshot |
| `status` | `VARCHAR(20)` | No | `UNREAD` | `UNREAD`, `READ` |
| `is_deleted` | `BOOLEAN` | No | `FALSE` | Soft delete trong inbox |
| `is_claimed` | `BOOLEAN` | No | `FALSE` | Da claim reward |
| `read_at` | `TIMESTAMP` | Yes | | Thoi diem read |
| `claimed_at` | `TIMESTAMP` | Yes | | Thoi diem claim |
| `delivered_at` | `TIMESTAMP` | Yes | | Thoi diem delivery |
| `expire_at` | `TIMESTAMP` | Yes | | Het han |
| `created_at` | `TIMESTAMP` | No | `CURRENT_TIMESTAMP` | Thoi diem tao inbox row |

Index/constraint:

- unique `(user_id, notification_id)`
- `idx_un_user_created(user_id, created_at DESC)`
- `idx_un_user_status(user_id, status, is_deleted)`
- `idx_un_notification(notification_id)`
- `idx_un_expire(expire_at)` where not null

### Bang `user_notification_claim_log`

Muc dich: audit claim reward.

| Column | Type | Nullable | Default | Ghi chu |
| --- | --- | --- | --- | --- |
| `id` | `BIGSERIAL` | No | | Primary key |
| `user_notification_id` | `BIGINT` | No | | FK toi `user_notification.id` |
| `user_id` | `BIGINT` | No | | User claim |
| `reward_payload` | `JSONB` | No | | Reward da claim |
| `claimed_result` | `VARCHAR(20)` | No | | Ket qua claim |
| `reference_code` | `VARCHAR(100)` | Yes | | Ma tham chieu neu co |
| `claimed_at` | `TIMESTAMP` | No | `CURRENT_TIMESTAMP` | Thoi diem claim |

Index/constraint:

- FK `user_notification_id` -> `user_notification(id)` on delete cascade
- `idx_claim_log_user_notification(user_notification_id)`
- `idx_claim_log_user(user_id, claimed_at DESC)`

### Bang `email_delivery_log`

Muc dich: audit email delivery va retry.

| Column | Type | Nullable | Default | Ghi chu |
| --- | --- | --- | --- | --- |
| `id` | `BIGSERIAL` | No | | Primary key |
| `event_id` | `VARCHAR(100)` | No | | Unique email event id |
| `notification_id` | `BIGINT` | Yes | | Notification/template lien quan |
| `user_id` | `BIGINT` | Yes | | User lien quan |
| `source_service` | `VARCHAR(100)` | Yes | | Service nguon neu co |
| `source_action` | `VARCHAR(100)` | Yes | | Action nguon neu co |
| `to_email` | `VARCHAR(254)` | No | | Dia chi nhan |
| `email_type` | `VARCHAR(50)` | Yes | | Loai email/template |
| `template_code` | `VARCHAR(100)` | Yes | | Template code neu co |
| `subject` | `VARCHAR(255)` | Yes | | Subject email |
| `payload` | `JSONB` | Yes | | Payload render/retry, gom title/body/locale/variables |
| `provider` | `VARCHAR(50)` | Yes | | SMTP/provider |
| `provider_message_id` | `VARCHAR(255)` | Yes | | Message id tu provider |
| `status` | `VARCHAR(20)` | No | `PENDING` | `PENDING`, `SENT`, `DELIVERED`, `FAILED` |
| `retry_count` | `INT` | No | `0` | So lan retry |
| `error_message` | `TEXT` | Yes | | Loi gan nhat |
| `requested_at` | `TIMESTAMP` | No | `CURRENT_TIMESTAMP` | Thoi diem enqueue |
| `sent_at` | `TIMESTAMP` | Yes | | Thoi diem gui thanh cong |
| `delivered_at` | `TIMESTAMP` | Yes | | Callback delivered neu co |
| `created_at` | `TIMESTAMP` | No | `CURRENT_TIMESTAMP` | Thoi diem tao log |

Index/constraint:

- unique `event_id`
- `idx_email_delivery_status(status, requested_at)`
- `idx_email_delivery_notification(notification_id)` where not null
- `idx_email_delivery_user(user_id, requested_at DESC)` where not null

## Kafka topics

Public:

```text
notification.request.v1
```

Internal:

```text
notification.delivery.requested
notification.delivery.batch.requested
notification.email
notification.websocket
notification.dlq
```

Env:

```properties
TOPIC_NOTIFICATION_REQUEST=notification.request.v1
TOPIC_NOTIFICATION_DELIVERY_REQUESTED=notification.delivery.requested
TOPIC_NOTIFICATION_DELIVERY_BATCH_REQUESTED=notification.delivery.batch.requested
TOPIC_NOTIFICATION_DLQ=notification.dlq
TOPIC_EMAIL=notification.email
TOPIC_WEBSOCKET=notification.websocket
```

### Kafka flow chi tiet

| Topic | Producer | Consumer | Payload | Muc dich |
| --- | --- | --- | --- | --- |
| `notification.request.v1` | user-service/service khac/admin caller neu async | `NotificationRequestConsumer` | `NotificationRequestEvent` JSON | Entry async tao notification |
| `notification.delivery.requested` | `CreateNotificationRequestUseCase` | `NotificationDeliveryJobWorker` | `NotificationDeliveryRequestedEvent` | Bat dau fanout job sau commit |
| `notification.delivery.batch.requested` | `NotificationDeliveryJobWorker` | `NotificationDeliveryBatchWorker` | `NotificationDeliveryBatchRequestedEvent` | Xu ly tung batch |
| `notification.email` | `EnqueueEmailDeliveryUseCase`, email retry admin | `EmailNotificationConsumer` | `EmailNotificationEvent` | Gui email downstream |
| `notification.websocket` | `RealtimeDispatcher` | `WebSocketNotificationConsumer` | `WebSocketNotificationEvent` | Push realtime downstream |
| `notification.dlq` | request/job/batch error handlers | tool/consumer van hanh | `NotificationDlqEvent` | Luu loi terminal/invalid payload |

### Cach test Kafka bang API

Flow API tao request se tu dong publish cac topic internal:

```text
POST /api/internal/notifications/requests
  -> notification.delivery.requested
  -> notification.delivery.batch.requested
  -> notification.email / notification.websocket neu channel bat
```

Kiem tra bang DB:

```sql
SELECT id, status, retry_count, last_error
FROM INF_NOTI.notification_delivery_job
ORDER BY id DESC
LIMIT 5;

SELECT id, status, retry_count, last_error
FROM INF_NOTI.notification_delivery_batch
ORDER BY id DESC
LIMIT 5;
```

### Cach test DLQ

Gui payload sai vao `notification.request.v1`, vi du message khong phai JSON. Consumer se ack va publish DLQ:

```text
failureStage = request-deserialize
sourceTopic  = notification.request.v1
```

DLQ cho job/batch se duoc publish khi retry vuot nguong.

## Fanout worker

`NotificationDeliveryJobWorker`:

- consume `notification.delivery.requested`
- load delivery job
- resolve target user IDs
- tao delivery batch theo `notification.fanout.batch-size`
- publish `notification.delivery.batch.requested` sau commit

`NotificationDeliveryBatchWorker`:

- consume `notification.delivery.batch.requested`
- bulk insert `user_notification`
- dung `ON CONFLICT (user_id, notification_id) DO NOTHING`
- update unread cache
- dispatch realtime neu channel `REALTIME`
- enqueue email neu channel `EMAIL`
- update job/batch counters

Config:

```properties
NOTIFICATION_FANOUT_BATCH_SIZE=500
NOTIFICATION_FANOUT_MAX_RETRY=3
```

## Email delivery

Email la downstream delivery, khong phai entrypoint cho service khac.

Flow:

```text
NotificationDeliveryBatchWorker
  -> EnqueueEmailDeliveryUseCase
  -> email_delivery_log PENDING
  -> notification.email
  -> EmailNotificationConsumer
  -> EmailServiceImpl
  -> SMTP
  -> email_delivery_log SENT/FAILED
```

Email consumer ho tro:

- event co `emailType`: dung template unified.
- event khong co `emailType`: legacy fallback/backward compatibility.

`EmailDeliveryAdminService` co retry bang cach publish lai event tu `email_delivery_log`.

## Realtime delivery

Realtime chi la delivery phu tro sau inbox.

Flow:

```text
Batch worker insert inbox thanh cong
  -> RealtimeDispatcher
  -> notification.websocket
  -> WebSocketNotificationConsumer
```

Neu realtime loi, inbox khong bi rollback.

## Locale

Locale di theo:

```text
Caller Accept-Language
  -> NotificationContent.locale
  -> notification_template.channel_payload._locale
  -> EmailNotificationEvent.locale
  -> LocaleContextHolder
```

Test voi `Accept-Language: vi` tu user-service thi log email consumer ky vong:

```text
Set locale to: vi
```

## Redis unread cache

Cache key:

```text
notification:unread:{userId}
```

Cache duoc update khi:

- fanout insert inbox moi
- mark notification read
- read all
- delete notification

Neu cache miss, service fallback DB va set lai cache.

## Retry, idempotency va DLQ

Idempotency:

- request boundary dung `idempotencyKey`
- inbox insert dung unique `(user_id, notification_id)`
- email delivery log dung unique `event_id`

Retry:

- Kafka request consumer khong ack loi retryable.
- Job/batch worker co retry count va max retry.
- Email delivery failed tang retry count, qua nguong thi `FAILED`.

DLQ:

```text
notification.dlq
```

Dang publish DLQ cho:

- request payload deserialize loi
- delivery job terminal fail
- delivery batch terminal fail

## Cleanup retention

Scheduler:

```text
NotificationRetentionCleanupScheduler
```

Config:

```properties
NOTIFICATION_RETENTION_INBOX_DAYS=180
NOTIFICATION_RETENTION_EMAIL_LOG_DAYS=90
NOTIFICATION_RETENTION_DELIVERY_DAYS=90
NOTIFICATION_CLEANUP_ENABLED=true
NOTIFICATION_CLEANUP_CRON=0 30 2 * * *
```

## Scheduler va background jobs

| Job/background work | Class | Trigger | Viec lam |
| --- | --- | --- | --- |
| Consume public notification request | `NotificationRequestConsumer` | Kafka `notification.request.v1` | Parse `NotificationRequestEvent`, goi `CreateNotificationRequestUseCase` |
| Tao delivery job | `CreateNotificationRequestUseCase` | REST/Kafka inbound | Luu request/template/rule/job, publish delivery event sau commit |
| Split job thanh batch | `NotificationDeliveryJobWorker` | Kafka `notification.delivery.requested` | Resolve target, tao batch, publish batch event sau commit |
| Insert inbox theo batch | `NotificationDeliveryBatchWorker` | Kafka `notification.delivery.batch.requested` | Bulk insert inbox, update cache, enqueue email/realtime |
| Gui email | `EmailNotificationConsumer` | Kafka `notification.email` | Set locale, render template, gui SMTP, update email log |
| Push realtime | `WebSocketNotificationConsumer` | Kafka `notification.websocket` | Xu ly downstream websocket event hien co |
| Cleanup retention | `NotificationRetentionCleanupScheduler` | Cron `${notification.cleanup.cron}` | Xoa inbox/email log/job/batch cu |

Cron cleanup default:

```text
0 30 2 * * *
```

Tat cleanup:

```properties
NOTIFICATION_CLEANUP_ENABLED=false
```

Background rule quan trong:

- Kafka publish trong flow DB phai sau transaction commit.
- Email/realtime la downstream; loi downstream khong duoc lam mat inbox.
- Batch retry khong tao duplicate inbox vi co unique `(user_id, notification_id)`.

## Observability

Co actuator va `NotificationMetrics`.

Metric/log phu cac diem:

- request received/duplicate/kafka processed/failed
- job created/batches created/failed
- batch completed/failed
- inbox inserted
- email sent/failed
- dlq published

## Legacy compatibility

Trong code van con mot so consumer/event cu:

- `EmailNotificationEvent`
- `AccountVerificationEvent`
- `UserStatusChangeEvent`
- `WebSocketNotificationEvent`

Chung duoc giu de backward compatibility/downstream internal. Business service moi khong nen publish truc tiep cac event nay; hay dung `NotificationRequestEvent`.

## External dependencies

- PostgreSQL
- Kafka
- Redis
- SMTP/mail provider
- common
- notification-contract
- gateway cho route va trusted user header

## gRPC integration

Notification-service hien tai **chua expose gRPC API va chua consume gRPC client truc tiep** trong notification flow. Tat ca integration chinh hien dung REST/Kafka:

- inbound async: `notification.request.v1`
- inbound sync noi bo: `POST /api/internal/notifications/requests`
- client/admin: HTTP qua gateway

Tuy vay repo da co ha tang gRPC dung chung trong `grpc-common`, va `.env.properties` da co port/address danh cho notification-service:

```properties
GRPC_NOTIFICATION_SERVICE_PORT=18082
GRPC_NOTIFICATION_SERVICE_ADDRESS=static://localhost:18082
```

Trong docker hien dang khai bao:

```properties
GRPC_NOTIFICATION_SERVICE_ADDRESS=static://inf-notification:18084
```

Can kiem tra lai port nay khi that su bat gRPC server cho notification, vi service notification hien chua co gRPC implementation tuong ung.

### Nen dung gRPC khi nao

Nen can nhac gRPC cho notification khi co nhu cau:

- service khac can sync query nhanh delivery status/summary
- service khac can internal command co response typed ro rang
- can callback/status API noi bo voi contract protobuf
- can high-throughput internal read API khong phu hop REST

Khong nen thay the Kafka fanout bang gRPC. Request notification van nen di async qua Kafka/REST internal, vi fanout/email/realtime la xu ly bat dong bo.

### Huong mo rong gRPC dung cach

Neu muon them gRPC cho notification-service:

1. Them proto vao `grpc-common/src/main/proto`, vi du `notification_service.proto`.
2. Them wrapper client trong `grpc-common`, vi du `NotificationGrpcClient`.
3. Khai bao service name khop `grpc-client.yml`, vi du `notification-service`.
4. Trong notification-service, tao adapter gRPC rieng goi vao application service hien co.
5. Khong de gRPC implementation thao tac truc tiep repository/entity.
6. Dung lai interceptor/metadata/error handling cua `grpc-common`.

Mau layering nen giu:

```text
Proto generated class
  -> grpc adapter
  -> application service/use case
  -> repository/infrastructure
```

Va voi caller:

```text
Business service
  -> local interface
  -> grpc wrapper client
  -> generated stub
```

### grpc-common hien co

`grpc-common` hien cung cap:

- `file_service.proto`
- generated FileServiceRpc classes
- `FileGrpcClient`
- `GrpcClientException`
- `GrpcContextInboundInterceptor`
- `GrpcContextOutboundInterceptor`
- `GrpcLocaleInterceptor`
- `GrpcExceptionTranslator`
- `grpc-client.yml` auto-loaded

Day la pattern nen copy khi them notification gRPC sau nay.

## Build va run

Build notification:

```bash
mvn -pl notification -am -DskipTests clean compile
```

Build cung cac module lien quan:

```bash
mvn -pl user-service,notification,gateway -am -DskipTests clean compile
```

Run local can co:

- PostgreSQL
- Kafka
- Redis
- SMTP config neu test gui mail that

## Test nhanh end-to-end

1. Start infra va notification-service.
2. Gui request `USER_IDS` vao internal API.
3. Kiem tra DB:

```sql
SELECT * FROM INF_NOTI.notification_request ORDER BY id DESC LIMIT 5;
SELECT * FROM INF_NOTI.notification_template ORDER BY id DESC LIMIT 5;
SELECT * FROM INF_NOTI.notification_delivery_job ORDER BY id DESC LIMIT 5;
SELECT * FROM INF_NOTI.notification_delivery_batch ORDER BY id DESC LIMIT 5;
SELECT * FROM INF_NOTI.user_notification ORDER BY id DESC LIMIT 5;
SELECT * FROM INF_NOTI.email_delivery_log ORDER BY id DESC LIMIT 5;
```

4. Kiem tra inbox:

```bash
curl "http://localhost:8084/api/notifications?page=0&size=20" \
  -H "X-USER-ID: 1"
```

5. Kiem tra unread:

```bash
curl "http://localhost:8084/api/notifications/unread-count" \
  -H "X-USER-ID: 1"
```

6. Kiem tra email audit:

```bash
curl "http://localhost:8084/api/admin/email-deliveries?page=0&size=20"
```

## Luu y khi sua code

- Them notification entrypoint moi thi uu tien `NotificationRequestEvent`.
- Khong dua public notification contract moi vao `common`; dat trong `notification-contract`.
- Khong publish Kafka truoc DB commit trong flow co transaction.
- Khong de realtime/email loi lam rollback inbox.
- Neu mo target `ROLE`, `SEGMENT`, `ALL`, can thiet ke resolver va pagination/cursor dung nghia truoc.
