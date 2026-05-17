# Thiáº¿t káº¿ chuáº©n cho `notification-service` cá»§a Infinite World

## 1. Má»¥c tiÃªu cuá»‘i cÃ¹ng

TÃ i liá»‡u nÃ y chá»‘t thiáº¿t káº¿ `notification-service` theo hÆ°á»›ng dÃ¹ng lÃ¢u dÃ i, Ä‘á»ƒ cÃ¡c service khÃ¡c chá»‰ cáº§n tÃ­ch há»£p theo contract chung rá»“i dÃ¹ng á»•n Ä‘á»‹nh vá» sau, khÃ´ng pháº£i sá»­a kiáº¿n trÃºc liÃªn tá»¥c.

Má»¥c tiÃªu chÃ­nh:

- `notification-service` lÃ  service chuyÃªn trÃ¡ch duy nháº¥t cho thÃ´ng bÃ¡o
- cÃ¡c service khÃ¡c khÃ´ng tá»± gá»­i email/WebSocket trá»±c tiáº¿p
- má»i loáº¡i thÃ´ng bÃ¡o Ä‘á»u Ä‘i qua contract chuáº©n
- scale tá»‘t tá»›i 1.000.000 user
- maintain tá»‘t, code clean, dá»… audit, dá»… retry, dá»… má»Ÿ rá»™ng

Má»¥c tiÃªu ká»¹ thuáº­t:

- khÃ´ng fanout trong request
- khÃ´ng gá»­i trÃ¹ng khi retry
- cÃ³ delivery log Ä‘áº§y Ä‘á»§
- cÃ³ mÃ´ hÃ¬nh tÃ­ch há»£p dÃ¹ng chung cho táº¥t cáº£ service
- cÃ³ versioning contract Ä‘á»ƒ khÃ´ng phÃ¡ vá»¡ tÃ­ch há»£p cÅ©

---

## 2. Quyáº¿t Ä‘á»‹nh kiáº¿n trÃºc chá»‘t

`notification-service` sáº½ lÃ  ná»n táº£ng thÃ´ng bÃ¡o dÃ¹ng chung cho toÃ n há»‡ thá»‘ng.

CÃ¡c service khÃ¡c nhÆ°:

- `user-service`
- `game-service`
- `payment-service`
- `event-service`
- `admin-service`

khÃ´ng nÃªn tá»± xá»­ lÃ½ gá»­i notification ra ngoÃ i, mÃ  chá»‰:

- gá»i API cá»§a `notification-service`, hoáº·c
- publish event chuáº©n dÃ¹ng chung vÃ o Kafka

`notification-service` chá»‹u trÃ¡ch nhiá»‡m:

- nháº­n yÃªu cáº§u gá»­i thÃ´ng bÃ¡o
- validate request
- persist dá»¯ liá»‡u
- fanout tá»›i user
- push realtime
- gá»­i email
- ghi log delivery
- retry / DLQ / audit

ÄÃ¢y lÃ  hÆ°á»›ng tá»‘t nháº¥t náº¿u muá»‘n vá» sau chá»‰ má»Ÿ rá»™ng chá»© khÃ´ng pháº£i sá»­a láº¡i ná»n.

---

## 3. PhÃ¢n lá»›p trÃ¡ch nhiá»‡m

Äá»ƒ code clean vÃ  service boundary rÃµ rÃ ng, pháº£i tÃ¡ch 3 lá»›p:

### 3.1. Notification Request Layer

LÃ  nÆ¡i nháº­n yÃªu cáº§u tá»« service khÃ¡c.

Nguá»“n vÃ o cÃ³ thá»ƒ lÃ :

- REST API Ä‘á»“ng bá»™
- Kafka event báº¥t Ä‘á»“ng bá»™

Layer nÃ y chá»‰ lÃ m:

- xÃ¡c thá»±c caller
- validate contract
- ghi nháº­n request
- táº¡o command/job xá»­ lÃ½ tiáº¿p

### 3.2. Notification Domain Layer

LÃ  lÃµi nghiá»‡p vá»¥ cá»§a `notification-service`.

Phá»¥ trÃ¡ch:

- phÃ¢n loáº¡i notification
- Ã¡p rule ngÆ°á»i nháº­n
- táº¡o campaign
- táº¡o inbox records
- cáº­p nháº­t tráº¡ng thÃ¡i read/claim
- táº¡o delivery logs

### 3.3. Notification Delivery Layer

Phá»¥ trÃ¡ch gá»­i ra ngoÃ i:

- WebSocket / realtime
- email

Layer nÃ y khÃ´ng tá»± quyáº¿t Ä‘á»‹nh nghiá»‡p vá»¥, chá»‰ lÃ m delivery theo command chuáº©n.

---

## 4. Nhá»¯ng nguyÃªn táº¯c pháº£i giá»¯ Ä‘á»ƒ khÃ´ng pháº£i sá»­a kiáº¿n trÃºc vá» sau

### 4.1. Táº¥t cáº£ notification pháº£i Ä‘i qua contract chuáº©n

KhÃ´ng cho phÃ©p má»—i service tá»± Ä‘á»‹nh nghÄ©a payload notification riÃªng theo Ã½ mÃ¬nh.

Pháº£i cÃ³ model dÃ¹ng chung.

### 4.2. TÃ¡ch request contract vÃ  delivery contract

Service gá»i vÃ o chá»‰ nÃªn biáº¿t:

- muá»‘n gá»­i gÃ¬
- gá»­i cho ai
- metadata nghiá»‡p vá»¥ lÃ  gÃ¬

Service gá»i vÃ o khÃ´ng nÃªn biáº¿t quÃ¡ sÃ¢u:

- batch size lÃ  bao nhiÃªu
- Kafka topic ná»™i bá»™ nÃ o dÃ¹ng Ä‘á»ƒ fanout
- delivery provider nÃ o Ä‘ang Ä‘Æ°á»£c dÃ¹ng

### 4.3. DÃ¹ng `notification-contract` lÃ m nÆ¡i chá»©a contract dÃ¹ng chung

Repo hiá»‡n táº¡i Ä‘Ã£ cÃ³ `common.dto.event`.

NhÆ°ng hÆ°á»›ng Ä‘Ãºng vá» lÃ¢u dÃ i lÃ  tÃ¡ch riÃªng `notification-contract` Ä‘á»ƒ chá»©a:

- notification request event
- realtime event
- email request event
- enum dÃ¹ng chung
- idempotency metadata
- tracing metadata

KhÃ´ng Ä‘á»ƒ tá»«ng service copy/paste DTO.

### 4.4. Contract pháº£i versioned

Náº¿u sau nÃ y thay Ä‘á»•i payload, pháº£i version.

VÃ­ dá»¥:

- `notification.request.v1`
- `notification.request.v2`

Hoáº·c trong DTO cÃ³:

- `schemaVersion`

Náº¿u khÃ´ng lÃ m Ä‘iá»u nÃ y ngay tá»« Ä‘áº§u thÃ¬ sau nÃ y chá»‰ cáº§n 2 service nÃ¢ng cáº¥p lá»‡ch version lÃ  sáº½ rá»‘i.

---

## 5. MÃ´ hÃ¬nh tÃ­ch há»£p chuáº©n cho cÃ¡c service khÃ¡c

Vá» lÃ¢u dÃ i nÃªn há»— trá»£ cáº£ 2 kiá»ƒu tÃ­ch há»£p:

## 5.1. CÃ¡ch 1: REST API

PhÃ¹ há»£p khi service caller cáº§n:

- biáº¿t káº¿t quáº£ ngay
- táº¡o notification theo thao tÃ¡c admin
- Ä‘á»“ng bá»™ tráº¡ng thÃ¡i dá»… hÆ¡n

VÃ­ dá»¥:

- admin táº¡o campaign
- game master táº¡o mail/quÃ 

## 5.2. CÃ¡ch 2: Kafka event

PhÃ¹ há»£p khi service caller chá»‰ phÃ¡t sinh domain event.

VÃ­ dá»¥:

- user Ä‘Äƒng kÃ½ thÃ nh cÃ´ng
- user bá»‹ khÃ³a
- event ingame báº¯t Ä‘áº§u
- thanh toÃ¡n thÃ nh cÃ´ng

Service phÃ¡t sinh event chá»‰ publish má»™t event chuáº©n, `notification-service` tá»± quyáº¿t Ä‘á»‹nh gá»­i gÃ¬ tiáº¿p.

## 5.3. Khuyáº¿n nghá»‹ chá»‘t

NÃªn dÃ¹ng cáº£ 2:

- REST cho command nghiá»‡p vá»¥ trá»±c tiáº¿p
- Kafka cho domain event tá»± Ä‘á»™ng

ÄÃ¢y lÃ  mÃ´ hÃ¬nh cÃ¢n báº±ng nháº¥t vÃ  Ã­t pháº£i Ä‘áº­p Ä‘i lÃ m láº¡i sau nÃ y.

---

## 6. Contract dÃ¹ng chung pháº£i cÃ³ trong `notification-contract`

Pháº§n nÃ y lÃ  trá»ng tÃ¢m Ä‘á»ƒ cÃ¡c service khÃ¡c tÃ¡i sá»­ dá»¥ng sáº¡ch sáº½.

## 6.1. Base metadata chung

Má»i request/event gá»­i sang `notification-service` nÃªn cÃ³ chung cÃ¡c field:

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

Ã nghÄ©a:

- `eventId`: id cá»§a event phÃ¡t ra
- `requestId`: id theo request/transaction cá»§a caller
- `traceId`: theo dÃµi xuyÃªn service
- `sourceService`: service nÃ o gá»i
- `sourceModule`: module nghiá»‡p vá»¥ nÃ o gá»i
- `sourceAction`: hÃ nh Ä‘á»™ng nÃ o gÃ¢y ra notification
- `schemaVersion`: version contract
- `idempotencyKey`: chá»‘ng táº¡o trÃ¹ng khi retry

## 6.2. NotificationTarget dÃ¹ng chung

Thay vÃ¬ má»—i event tá»± nhÃ©t `userId`, `roles`, `all`, `segment` theo kiá»ƒu khÃ¡c nhau, nÃªn cÃ³ target model chuáº©n.

VÃ­ dá»¥:

```json
{
  "type": "USER_IDS",
  "userIds": [1, 2, 3]
}
```

hoáº·c:

```json
{
  "type": "ROLE",
  "roles": ["ADMIN", "MODERATOR"]
}
```

NÃªn Ä‘á»‹nh nghÄ©a class chung:

- `NotificationTarget`
- `NotificationTargetType`

## 6.3. NotificationContent dÃ¹ng chung

Pháº£i cÃ³ object ná»™i dung thá»‘ng nháº¥t:

- `title`
- `body`
- `type`
- `priority`
- `imageUrl`
- `action`
- `reward`
- `localePolicy`

## 6.4. NotificationAction dÃ¹ng chung

KhÃ´ng Ä‘á»ƒ `actionTarget` rá»i ráº¡c.

NÃªn chuáº©n hÃ³a:

- `actionType`
- `screen`
- `url`
- `deeplink`
- `payload`

VÃ­ dá»¥:

- `OPEN_SCREEN`
- `OPEN_URL`
- `OPEN_DEEPLINK`
- `CLAIM_REWARD`
- `NONE`

## 6.5. NotificationChannel dÃ¹ng chung

Pháº£i cÃ³ enum dÃ¹ng chung:

- `INBOX`
- `REALTIME`
- `EMAIL`

Má»™t request cÃ³ thá»ƒ Ä‘i nhiá»u channel cÃ¹ng lÃºc.

VÃ­ dá»¥:

- inbox + realtime
- email only
- inbox + realtime + email

## 6.6. NotificationRequest chuáº©n

ÄÃ¢y nÃªn lÃ  contract trung tÃ¢m dÃ¹ng chung cho cÃ¡c service gá»i vÃ o.

VÃ­ dá»¥ tá»‘i thiá»ƒu:

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
    "title": "ChÃ o má»«ng báº¡n",
    "body": "TÃ i khoáº£n cá»§a báº¡n Ä‘Ã£ Ä‘Æ°á»£c táº¡o thÃ nh cÃ´ng"
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

Khuyáº¿n nghá»‹:

- táº¡o class `NotificationRequestEvent` trong `notification-contract`
- Ä‘Ã¢y lÃ  payload chuáº©n nháº¥t cho cÃ¡c service khÃ¡c dÃ¹ng

---

## 7. Nhá»¯ng contract nÃ o nÃªn cÃ³ ngay trong `notification-contract`

Äá»ƒ dÃ¹ng chung vÃ  clean code, nÃªn chá»‘t bá»™ contract sau:

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

NgoÃ i ra nÃªn cÃ³ interface hoáº·c abstract base:

- `BaseNotificationEvent`

Ä‘á»ƒ cÃ¡c event khÃ¡c nhÆ° email/realtime káº¿ thá»«a pattern field thá»‘ng nháº¥t.

---

## 8. Quan há»‡ giá»¯a contract tá»•ng vÃ  cÃ¡c event delivery hiá»‡n cÃ³

Repo hiá»‡n táº¡i Ä‘Ã£ cÃ³:

- `EmailNotificationEvent`
- `WebSocketNotificationEvent`

CÃ¡c event nÃ y khÃ´ng nÃªn lÃ  contract Ä‘áº§u vÃ o chÃ­nh cho cÃ¡c service khÃ¡c ná»¯a.

ChÃºng nÃªn trá»Ÿ thÃ nh:

- contract ná»™i bá»™ cá»§a `notification-service`
- hoáº·c contract delivery downstream

HÆ°á»›ng chuáº©n:

1. service khÃ¡c gá»­i `NotificationRequestEvent`
2. `notification-service` xá»­ lÃ½ nghiá»‡p vá»¥
3. náº¿u cáº§n email thÃ¬ chuyá»ƒn thÃ nh `EmailNotificationEvent`
5. náº¿u cáº§n realtime thÃ¬ chuyá»ƒn thÃ nh `WebSocketNotificationEvent`

NhÆ° váº­y boundary sáº½ sáº¡ch hÆ¡n:

- service khÃ¡c khÃ´ng cáº§n biáº¿t email template cá»¥ thá»ƒ
- service khÃ¡c khÃ´ng cáº§n biáº¿t websocket payload cá»¥ thá»ƒ
- má»i logic mapping náº±m táº­p trung trong `notification-service`

ÄÃ¢y lÃ  Ä‘iá»ƒm ráº¥t quan trá»ng náº¿u muá»‘n trÃ¡nh sá»­a Ä‘i sá»­a láº¡i sau nÃ y.

---

## 9. Thiáº¿t káº¿ dá»¯ liá»‡u chuáº©n cho production

Schema Ä‘á» xuáº¥t: `INF_NOTI`

## 9.1. `notification_request`

LÆ°u request gá»‘c tá»« service khÃ¡c.

```sql
CREATE TABLE INF_NOTI.notification_request (
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

Lá»£i Ã­ch:

- audit request Ä‘áº§u vÃ o
- chá»‘ng duplicate tá»« caller
- truy váº¿t Ä‘áº§y Ä‘á»§ theo service nguá»“n

## 9.2. `notification_template`

LÆ°u thÃ´ng bÃ¡o gá»‘c Ä‘Ã£ chuáº©n hÃ³a.

```sql
CREATE TABLE INF_NOTI.notification_template (
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
        REFERENCES INF_NOTI.notification_request(id)
);
```

`channel_payload` dÃ¹ng Ä‘á»ƒ lÆ°u cáº¥u hÃ¬nh channel Ä‘Ã£ chuáº©n hÃ³a:

- inbox
- realtime
- email

## 9.3. `notification_target_rule`

```sql
CREATE TABLE INF_NOTI.notification_target_rule (
    id BIGSERIAL PRIMARY KEY,
    notification_id BIGINT NOT NULL,
    rule_type VARCHAR(30) NOT NULL,
    rule_payload JSONB NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_target_rule_template
        FOREIGN KEY (notification_id)
        REFERENCES INF_NOTI.notification_template(id)
        ON DELETE CASCADE
);
```

## 9.4. `notification_delivery_job`

```sql
CREATE TABLE INF_NOTI.notification_delivery_job (
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
        REFERENCES INF_NOTI.notification_template(id)
        ON DELETE CASCADE
);
```

## 9.5. `notification_delivery_batch`

```sql
CREATE TABLE INF_NOTI.notification_delivery_batch (
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
        REFERENCES INF_NOTI.notification_delivery_job(id)
        ON DELETE CASCADE,
    CONSTRAINT uk_delivery_batch UNIQUE (job_id, batch_no)
);
```

## 9.6. `user_notification`

```sql
CREATE TABLE INF_NOTI.user_notification (
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
    ON INF_NOTI.user_notification(user_id, created_at DESC);
CREATE INDEX idx_un_user_status
    ON INF_NOTI.user_notification(user_id, status, is_deleted);
```

## 9.7. `user_notification_claim_log`

```sql
CREATE TABLE INF_NOTI.user_notification_claim_log (
    id BIGSERIAL PRIMARY KEY,
    user_notification_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    reward_payload JSONB NOT NULL,
    claimed_result VARCHAR(20) NOT NULL,
    reference_code VARCHAR(100),
    claimed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_claim_user_notification
        FOREIGN KEY (user_notification_id)
        REFERENCES INF_NOTI.user_notification(id)
        ON DELETE CASCADE
);
```

## 9.8. `email_delivery_log`

```sql
CREATE TABLE INF_NOTI.email_delivery_log (
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

## 10. Táº¡i sao pháº£i cÃ³ `notification_request`

ÄÃ¢y lÃ  báº£ng ráº¥t quan trá»ng Ä‘á»ƒ há»‡ thá»‘ng dÃ¹ng chung cho nhiá»u service mÃ  váº«n sáº¡ch.

Náº¿u khÃ´ng cÃ³ báº£ng nÃ y:

- khÃ³ audit service nÃ o yÃªu cáº§u gÃ¬
- khÃ³ xá»­ lÃ½ idempotency tá»« caller
- khÃ³ há»— trá»£ retry an toÃ n
- khÃ³ debug tÃ­ch há»£p xuyÃªn service

VÃ¬ `notification-service` lÃ  ná»n táº£ng dÃ¹ng chung, báº£ng nÃ y nÃªn Ä‘Æ°á»£c xem lÃ  báº¯t buá»™c.

---

## 11. Quy trÃ¬nh xá»­ lÃ½ chuáº©n

## 11.1. Inbound flow

1. service khÃ¡c gá»i REST hoáº·c publish `NotificationRequestEvent`
2. `notification-service` validate contract
3. kiá»ƒm tra `idempotencyKey`
4. insert `notification_request`
5. chuáº©n hÃ³a thÃ nh `notification_template`
6. táº¡o `notification_target_rule`
7. táº¡o `notification_delivery_job`
8. publish job vÃ o Kafka

## 11.2. Fanout flow

1. worker Ä‘á»c `notification.delivery.requested`
2. Ä‘áº¿m target
3. chia thÃ nh `notification_delivery_batch`
4. publish tá»«ng batch
5. batch worker query user theo cursor
6. bulk insert `user_notification`
7. update unread count cache
8. push realtime náº¿u cÃ³ channel `REALTIME`

## 11.3. External delivery flow

1. domain layer xÃ¡c Ä‘á»‹nh cáº§n email
2. map sang `EmailNotificationEvent`
3. insert delivery log tráº¡ng thÃ¡i `PENDING`
4. publish topic email
5. consumer gá»­i provider
6. update log `SENT` / `FAILED`
7. náº¿u cÃ³ callback provider thÃ¬ update `DELIVERED`

---

## 12. TÃ­ch há»£p vá»›i cÃ¡c service khÃ¡c nhÆ° tháº¿ nÃ o cho sáº¡ch nháº¥t

ÄÃ¢y lÃ  pháº§n cáº§n lÃ m Ä‘Ãºng ngay tá»« Ä‘áº§u.

## 12.1. KhÃ´ng cho service khÃ¡c gá»i Kafka topic ná»™i bá»™ lung tung

Service khÃ¡c khÃ´ng nÃªn biáº¿t:

- `notification.delivery.batch.requested`
- `notification.realtime.dispatch`
- `notification.dead-letter`

CÃ¡c topic nÃ y lÃ  ná»™i bá»™ cá»§a `notification-service`.

Service ngoÃ i chá»‰ nÃªn biáº¿t:

- REST public API cá»§a notification
- hoáº·c topic contract public nhÆ° `notification.request.v1`

## 12.2. Táº¡o má»™t integration SDK nháº¹ dÃ¹ng chung

NÃªn dÃ¹ng trá»±c tiáº¿p module `notification-contract`.

Pháº§n helper má»©c nháº¹ trong module nÃ y cung cáº¥p:

- request DTO
- event DTO
- helper builder
- topic names public
- validation cÆ¡ báº£n

VÃ­ dá»¥:

- `NotificationRequestBuilder`
- `NotificationTargetBuilder`
- `NotificationChannelSet`

Má»¥c tiÃªu:

- giáº£m code láº·p á»Ÿ cÃ¡c service caller
- giáº£m sai contract
- clean code hÆ¡n

## 12.3. Chuáº©n hÃ³a topic public

NÃªn chá»‰ public 1 hoáº·c 2 topic á»Ÿ boundary:

- `notification.request.v1`
- `notification.status.changed.v1` náº¿u cáº§n callback tráº¡ng thÃ¡i

KhÃ´ng public toÃ n bá»™ topic ná»™i bá»™.

## 12.4. Callback tráº¡ng thÃ¡i náº¿u cáº§n

Má»™t sá»‘ service cÃ³ thá»ƒ muá»‘n biáº¿t káº¿t quáº£ notification.

VÃ­ dá»¥:

- payment service muá»‘n biáº¿t email invoice Ä‘Ã£ gá»­i chÆ°a
- game service muá»‘n biáº¿t mail reward Ä‘Ã£ táº¡o inbox xong chÆ°a

Khi Ä‘Ã³ `notification-service` cÃ³ thá»ƒ phÃ¡t event dÃ¹ng chung:

- `notification.status.changed.v1`

Payload gá»“m:

- `eventId`
- `sourceService`
- `sourceAction`
- `businessId`
- `channel`
- `status`
- `errorCode`
- `occurredAt`

KhÃ´ng nÃªn callback quÃ¡ chi tiáº¿t ngay tá»« Ä‘áº§u. Chá»‰ publish tráº¡ng thÃ¡i nghiá»‡p vá»¥ cáº§n thiáº¿t.

---

## 13. Chiáº¿n lÆ°á»£c scale 1 triá»‡u user

## 13.1. KhÃ´ng dÃ¹ng offset lá»›n

Fanout pháº£i dÃ¹ng cursor:

- `WHERE id > :lastId ORDER BY id LIMIT :batchSize`

## 13.2. Batch insert báº±ng JDBC/native

KhÃ´ng save tá»«ng record qua JPA.

## 13.3. Partition `user_notification`

Khuyáº¿n nghá»‹:

- partition theo thÃ¡ng báº±ng `created_at`

## 13.4. Read replica cho user source

Náº¿u sá»‘ campaign lá»›n:

- `notification-service` nÃªn Ä‘á»c user target tá»« read replica hoáº·c quyá»n read-only schema user

KhÃ´ng nÃªn gá»i REST paging sang `user-service` Ä‘á»ƒ fanout lá»›n.

## 13.5. Redis chá»‰ cache unread count

KhÃ´ng dÃ¹ng Redis lÃ m inbox store.

## 13.6. Realtime chá»‰ lÃ  phá»¥ trá»£

Náº¿u realtime lá»—i:

- inbox váº«n cÃ²n
- user váº«n Ä‘á»c láº¡i Ä‘Æ°á»£c

---

## 14. Idempotency chuáº©n há»‡ thá»‘ng

VÃ¬ nhiá»u service cÃ¹ng gá»i vÃ o, pháº§n nÃ y pháº£i chá»‘t ráº¥t ká»¹.

## 14.1. á»ž boundary request

Má»—i request pháº£i cÃ³ `idempotencyKey`.

VÃ­ dá»¥:

- `user_registered:123`
- `invoice_paid:INV-0001`
- `reward_event:evt-2026-01:user-99`

Náº¿u caller retry cÃ¹ng key:

- khÃ´ng táº¡o campaign má»›i
- tráº£ láº¡i káº¿t quáº£ request cÅ©

## 14.2. á»ž inbox delivery

Chá»‘ng trÃ¹ng báº±ng:

- unique `(user_id, notification_id)`

## 14.3. á»ž email delivery

Chá»‘ng trÃ¹ng báº±ng:

- unique `event_id` trong delivery log

## 14.4. á»ž reward claim

Chá»‘ng claim trÃ¹ng báº±ng:

- transaction
- row lock
- claim log

---

## 15. API public nÃªn cÃ³

## 15.1. Admin / internal command API

- `POST /api/admin/notifications`
- `GET /api/admin/notifications`
- `GET /api/admin/notifications/{id}`
- `POST /api/admin/notifications/{id}/cancel`
- `POST /api/admin/notifications/{id}/retry`
- `GET /api/admin/notifications/{id}/delivery-summary`

## 15.2. Service integration API

ÄÃ¢y lÃ  API cÃ¡c service khÃ¡c gá»i vÃ o trá»±c tiáº¿p náº¿u khÃ´ng dÃ¹ng Kafka.

- `POST /api/internal/notifications/requests`

Request body lÃ  `NotificationRequestEvent`.

Response nÃªn tráº£:

- `requestId`
- `eventId`
- `status`
- `notificationId` náº¿u Ä‘Ã£ táº¡o Ä‘Æ°á»£c

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
## 16. Versioning vÃ  backward compatibility

Náº¿u muá»‘n khÃ´ng pháº£i sá»­a Ä‘i sá»­a láº¡i, pháº£i chá»‘t nguyÃªn táº¯c nÃ y ngay.

NguyÃªn táº¯c:

- contract public pháº£i versioned
- field má»›i chá»‰ Ä‘Æ°á»£c thÃªm theo hÆ°á»›ng backward compatible
- khÃ´ng Ä‘á»•i nghÄ©a field cÅ©
- khÃ´ng xÃ³a field public Ä‘ang dÃ¹ng náº¿u chÆ°a qua deprecation cycle

Khuyáº¿n nghá»‹:

- má»i public event cÃ³ `schemaVersion`
- topic public cÃ³ suffix version

VÃ­ dá»¥:

- `notification.request.v1`
- `notification.status.changed.v1`

---

## 17. Observability báº¯t buá»™c pháº£i cÃ³

Metric tá»‘i thiá»ƒu:

- sá»‘ request nháº­n vÃ o theo `sourceService`
- sá»‘ request bá»‹ duplicate theo `idempotencyKey`
- sá»‘ campaign táº¡o má»›i
- sá»‘ batch Ä‘ang `PENDING`, `PROCESSING`, `FAILED`
- fanout throughput
- inbox insert latency
- unread cache hit rate
- email success/failure rate
- realtime push success/failure rate
- DLQ size

Structured log pháº£i cÃ³:

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

## 18. Cáº¥u trÃºc code Ä‘á» xuáº¥t Ä‘á»ƒ clean nháº¥t

```text
notification
â””â”€â”€ src/main/java/com/infinite/notification
    â”œâ”€â”€ api
    â”‚   â”œâ”€â”€ admin
    â”‚   â”œâ”€â”€ client
    â”‚   â””â”€â”€ internal
    â”œâ”€â”€ application
    â”‚   â”œâ”€â”€ command
    â”‚   â”œâ”€â”€ query
    â”‚   â””â”€â”€ mapper
    â”œâ”€â”€ domain
    â”‚   â”œâ”€â”€ model
    â”‚   â”œâ”€â”€ service
    â”‚   â””â”€â”€ policy
    â”œâ”€â”€ infrastructure
    â”‚   â”œâ”€â”€ persistence
    â”‚   â”œâ”€â”€ messaging
    â”‚   â”œâ”€â”€ redis
    â”‚   â”œâ”€â”€ email
    â”‚   â””â”€â”€ websocket
    â”œâ”€â”€ worker
    â”œâ”€â”€ scheduler
    â””â”€â”€ config
```

Khuyáº¿n nghá»‹ rÃµ:

- `api`: nháº­n request/response
- `application`: orchestration use case
- `domain`: business rule
- `infrastructure`: JPA/Kafka/Redis/provider

KhÃ´ng nÃªn nhÃ©t háº¿t vÃ o `service/impl` nhÆ° kiá»ƒu CRUD thÆ°á»ng tháº¥y, vÃ¬ sau nÃ y service nÃ y sáº½ lá»›n.

---

## 19. Nhá»¯ng thá»© nÃªn Ä‘Æ°a vÃ o `notification-contract`

Chá»‘t danh sÃ¡ch nÃªn chuáº©n hÃ³a ngay trong `notification-contract`:

- DTO request/event public
- enum channel/type/status/priority
- base metadata model
- topic name public
- builder/helper cho request
- constants vá» schema version

KhÃ´ng nÃªn Ä‘Æ°a vÃ o `notification-contract`:

- entity JPA cá»§a `notification-service`
- repository
- logic fanout
- logic provider email

NguyÃªn táº¯c:

- `notification-contract` chá»‰ chá»©a contract vÃ  helper tÃ­ch há»£p má»©c nháº¹
- khÃ´ng biáº¿n module nÃ y thÃ nh nÆ¡i nhÃ©t logic nghiá»‡p vá»¥ notification

---

## 20. CÃ¡ch tÃ¡ch module chá»‘t cuá»‘i cÃ¹ng

Quyáº¿t Ä‘á»‹nh cuá»‘i cÃ¹ng cho repo nÃ y:

- chá»‰ tÃ¡ch Ä‘Ãºng `1` module dependency dÃ¹ng chung: `notification-contract`
- `notification-service` váº«n giá»¯ lÃ  `1` module triá»ƒn khai duy nháº¥t
- chÆ°a tÃ¡ch `email`, `realtime`, `inbox`, `admin` thÃ nh cÃ¡c Maven module riÃªng trong giai Ä‘oáº¡n nÃ y

ÄÃ¢y lÃ  phÆ°Æ¡ng Ã¡n cÃ¢n báº±ng nháº¥t giá»¯a:

- sáº¡ch kiáº¿n trÃºc
- dá»… tÃ¬m kiáº¿m
- dá»… tÃ­ch há»£p
- khÃ´ng over-engineer
- khÃ´ng lÃ m build/dependency phá»©c táº¡p quÃ¡ sá»›m

## 20.1. Module `notification-contract`

ÄÃ¢y lÃ  module dependency duy nháº¥t mÃ  cÃ¡c service khÃ¡c cáº§n dÃ¹ng.

Module nÃ y chá»©a:

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
- builder/helper má»©c nháº¹
- client helper má»©c nháº¹ náº¿u cáº§n

Pháº§n "client helper má»©c nháº¹" Ä‘Æ°á»£c phÃ©p gá»™p luÃ´n vÃ o `notification-contract`, vÃ­ dá»¥:

- `NotificationRequestBuilder`
- `NotificationTopicNames`
- `NotificationPublisher` interface

NhÆ°ng chá»‰ dá»«ng á»Ÿ má»©c má»ng:

- helper build request
- interface publish/call
- constants vÃ  validation nháº¹

KhÃ´ng Ä‘Æ°a vÃ o Ä‘Ã¢y:

- Kafka implementation náº·ng
- REST implementation náº·ng
- retry policy phá»©c táº¡p
- fallback logic
- business logic notification

## 20.2. Nhá»¯ng gÃ¬ Ä‘á»ƒ láº¡i trong `notification-service`

ToÃ n bá»™ pháº§n nghiá»‡p vá»¥ tháº­t pháº£i á»Ÿ láº¡i trong `notification-service`:

- entity JPA
- repository
- request persistence
- template persistence
- delivery job/batch worker
- target resolution logic
- inbox query logic
- unread count logic
- email delivery log persistence
- mapping tá»« request contract sang delivery command
- realtime dispatch
- admin API
- client API
- internal API

LÃ½ do:

- Ä‘Ã¢y lÃ  lÃµi domain cá»§a notification
- tÃ¡ch ra sá»›m thÃ nh nhiá»u Maven module sáº½ lÃ m boundary rá»‘i hÆ¡n lá»£i Ã­ch nháº­n Ä‘Æ°á»£c

## 20.3. CÃ³ nÃªn tÃ¡ch email thÃ nh module riÃªng khÃ´ng

Hiá»‡n táº¡i: `khÃ´ng nÃªn`.

NÃªn lÃ m:

- giá»¯ email trong `notification-service`
- tÃ¡ch package rÃµ trong service

VÃ­ dá»¥:

- `infrastructure.email`
- `application.command.email`
- `domain.delivery`

Chá»‰ nÃªn tÃ¡ch email thÃ nh Maven module riÃªng sau nÃ y náº¿u cÃ³ Ä‘á»§ dáº¥u hiá»‡u:

- nhiá»u provider
- logic template/rendering ráº¥t lá»›n
- cáº§n scale/deploy Ä‘á»™c láº­p
- team phá»¥ trÃ¡ch riÃªng

á»ž giai Ä‘oáº¡n hiá»‡n táº¡i, tÃ¡ch package lÃ  Ä‘á»§ tá»‘t vÃ  sáº¡ch hÆ¡n nhiá»u so vá»›i tÃ¡ch module.

## 20.4. Cáº¥u trÃºc module thá»±c táº¿ nÃªn dÃ¹ng

NÃªn chá»‘t nhÆ° sau:

- `common`
  - giá»¯ utility/global components chung toÃ n há»‡ thá»‘ng
- `notification-contract`
  - module dependency dÃ¹ng chung cho táº¥t cáº£ service cáº§n tÃ­ch há»£p notification
- `notification`
  - service triá»ƒn khai toÃ n bá»™ nghiá»‡p vá»¥ notification

Tá»©c lÃ :

- khÃ´ng tiáº¿p tá»¥c nhÃ©t contract notification vÃ o `common`
- khÃ´ng táº¡o thÃªm `notification-client` thÃ nh module riÃªng
- khÃ´ng tÃ¡ch `notification-email` thÃ nh module riÃªng á»Ÿ thá»i Ä‘iá»ƒm nÃ y

ÄÃ¢y lÃ  cáº¥u trÃºc Ã­t rá»§i ro nháº¥t vÃ  dá»… giá»¯ á»•n Ä‘á»‹nh lÃ¢u dÃ i nháº¥t.

---

## 21. Nhá»¯ng gÃ¬ cáº§n sá»­a trong repo Ä‘á»ƒ Ä‘Ãºng hÆ°á»›ng nÃ y

### 21.1. Trong `common`

Cáº§n giá»¯ `common` gá»n láº¡i.

KhÃ´ng nÃªn tiáº¿p tá»¥c Ä‘Æ°a thÃªm contract notification má»›i vÃ o `common`.

Náº¿u hiá»‡n táº¡i Ä‘ang cÃ³ DTO/event notification trong `common`, hÆ°á»›ng Ä‘Ãºng lÃ  chuyá»ƒn dáº§n chÃºng sang `notification-contract`.

NÃªn xem `EmailNotificationEvent` vÃ  `WebSocketNotificationEvent` lÃ  downstream/internal contracts, khÃ´ng pháº£i request contract public chÃ­nh ná»¯a.

### 21.1.a. Trong `notification-contract`

Cáº§n bá»• sung:

- `NotificationRequestEvent`
- `NotificationTarget`
- `NotificationContent`
- `NotificationAction`
- `NotificationChannel`
- `DeliveryStatus`
- `BaseNotificationEvent`
- builder/helper má»©c nháº¹ cho caller
- public topic names
- schema version constants

### 21.2. Trong `notification`

Cáº§n bá»• sung:

- JPA + PostgreSQL
- báº£ng request/template/job/batch/inbox/log
- internal REST API
- admin API
- client API
- fanout worker
- email delivery log service
- status callback publisher náº¿u cáº§n
- tÃ¡ch package ná»™i bá»™ rÃµ rÃ ng theo `api / application / domain / infrastructure`

### 21.3. Trong cÃ¡c service caller

Cáº§n thay Ä‘á»•i dáº§n:

- khÃ´ng publish trá»±c tiáº¿p `WebSocketNotificationEvent` tá»« business service ná»¯a
- khÃ´ng business service nÃ o gá»i provider email trá»±c tiáº¿p
- dÃ¹ng `NotificationRequestEvent` hoáº·c internal notification API

---

## 22. Lá»™ trÃ¬nh triá»ƒn khai Ä‘Ãºng Ä‘á»ƒ khÃ´ng pháº£i lÃ m láº¡i

### Phase 1

Chuáº©n hÃ³a contract.

- bá»• sung DTO/event chung trong `notification-contract`
- chá»‘t topic public
- chá»‘t REST internal API

### Phase 2

XÃ¢y lÃµi `notification-service`.

- thÃªm persistence
- thÃªm request/template/job/batch/inbox
- thÃªm fanout worker

### Phase 3

Chuáº©n hÃ³a external delivery.

- email delivery log
- retry / DLQ
- callback tráº¡ng thÃ¡i náº¿u cáº§n

### Phase 4

Tá»‘i Æ°u quy mÃ´ lá»›n.

- partition
- read replica
- tuning Kafka
- tuning retention
- metrics/dashboard Ä‘áº§y Ä‘á»§

---

## 23. Káº¿t luáº­n cuá»‘i cÃ¹ng

Náº¿u má»¥c tiÃªu lÃ  â€œlÃ m má»™t láº§n cho Ä‘Ãºng ná»n, vá» sau cá»© tÃ­ch há»£p mÃ  dÃ¹ngâ€, thÃ¬ báº£n thiáº¿t káº¿ chuáº©n nháº¥t cho dá»± Ã¡n nÃ y lÃ :

- `notification-service` lÃ  entrypoint duy nháº¥t cho thÃ´ng bÃ¡o
- service khÃ¡c chá»‰ giao tiáº¿p qua contract public chuáº©n
- contract public Ä‘Æ°á»£c Ä‘áº·t á»Ÿ `notification-contract` vÃ  cÃ³ version
- `NotificationRequestEvent` lÃ  request model trung tÃ¢m
- email/realtime event chá»‰ lÃ  delivery contracts phÃ­a trong
- toÃ n bá»™ request Ä‘á»u cÃ³ idempotency, source metadata, trace metadata
- fanout dÃ¹ng job + batch + cursor + bulk insert
- má»i delivery Ä‘á»u cÃ³ log riÃªng Ä‘á»ƒ audit vÃ  retry
- code tá»• chá»©c theo hÆ°á»›ng application/domain/infrastructure, khÃ´ng lÃ m kiá»ƒu service CRUD phÃ¬nh to

ÄÃ¢y lÃ  hÆ°á»›ng Ã­t rá»§i ro nháº¥t, dá»… scale nháº¥t, vÃ  sáº¡ch nháº¥t Ä‘á»ƒ giá»¯ á»•n Ä‘á»‹nh lÃ¢u dÃ i.

