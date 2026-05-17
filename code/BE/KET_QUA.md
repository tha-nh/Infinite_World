# Ket qua trien khai Notification HD

Ngay cap nhat: 2026-05-17

## Tong quan

BUOC1, BUOC2, BUOC3 va phan tich hop con lai trong `HD.md` da duoc trien khai. Notification-service hien la entrypoint notification duy nhat cho `user-service`: business flow cua user-service khong publish truc tiep `EmailNotificationEvent`/`WebSocketNotificationEvent` nua, ma build `NotificationRequestEvent` va publish vao `notification.request.v1`.

Pham vi fanout production hien tai: `USER_IDS`. Cac rule lon hon nhu `ROLE`, `SEGMENT`, `ALL_USERS` da co contract, nhung can read-side target source rieng truoc khi bat delivery that.

## Build status

Da verify:

```bash
mvn -pl user-service,notification,gateway -am -DskipTests clean compile
```

Ket qua: BUILD SUCCESS.

Con warning deprecation tu cac event downstream cu (`EmailNotificationEvent`, `UserStatusChangeEvent`, serializer Kafka cu). Day la adapter/backward compatibility hien co, khong lam fail build.

## Phase 9 - Fanout worker

Hoan thanh cho luong `USER_IDS`.

- Them topic noi bo:
  - `notification.delivery.requested`
  - `notification.delivery.batch.requested`
  - `notification.dlq`
- Them config topic trong `application.yml`, `.env.properties`, `.env_docker.properties`.
- `CreateNotificationRequestUseCase` publish `NotificationDeliveryRequestedEvent` sau khi tao `NotificationDeliveryJob`.
- `NotificationDeliveryJobWorker`:
  - doc job event
  - load job/target rule
  - tinh total target
  - chia batch theo `notification.fanout.batch-size`
  - tao `NotificationDeliveryBatch`
  - publish batch event
- `NotificationDeliveryBatchWorker`:
  - doc batch event
  - lay slice user theo cursor/batch size
  - bulk insert `user_notification`
  - update batch/job status va counters
- `UserNotificationBulkRepository` dung JDBC batch insert voi `ON CONFLICT (user_id, notification_id) DO NOTHING`.

## Phase 10 - Realtime dispatch

Hoan thanh hook noi bo.

- `notification.websocket` duoc dung nhu downstream topic noi bo cua notification-service.
- Them `RealtimeDispatcher`.
- Batch worker chi publish realtime sau khi inbox insert thanh cong.
- Loi realtime duoc log warning, khong rollback inbox.

## Phase 11 - Email delivery

Hoan thanh nen email orchestration va audit.

- Them `EnqueueEmailDeliveryUseCase`.
- Tao `EmailDeliveryLog` truoc khi publish downstream email event.
- `EmailNotificationConsumer` cap nhat log:
  - success -> `SENT`, set `sentAt`
  - fail -> tang `retryCount`, luu `errorMessage`, qua 3 lan -> `FAILED`
- Them admin email audit API:
  - `GET /api/admin/email-deliveries`
  - `GET /api/admin/email-deliveries/{id}`
  - `POST /api/admin/email-deliveries/{id}/retry`
- Gateway them route `notification-email-admin`.

Ghi chu: email enqueue can co email nguoi nhan. Voi `USER_IDS`, batch worker ho tro doc `emailByUserId` trong `rule_payload.emailByUserId` hoac `rule_payload.queryParams.emailByUserId`. Neu khong co email, email delivery se skip thay vi tao email sai.

## Phase 12 - Idempotency va retry

Hoan thanh muc can thiet.

- Request boundary van dung unique/idempotency key.
- Inbox insert dung `ON CONFLICT DO NOTHING`, retry batch khong tao duplicate.
- Email event id on dinh theo `notificationId + userId`, co unique `event_id`.
- Batch/job retry count co max retry config `notification.fanout.max-retry`.
- `NotificationRequestConsumer` khong ack khi loi retryable de Kafka redeliver.
- Worker ack khi thanh cong hoac khi loi terminal.
- DLQ co ban da co cho:
  - request payload deserialize loi
  - delivery job terminal fail
  - delivery batch terminal fail
- Metric `notification.dlq.published` duoc ghi theo stage.

## Phase 13 - Unread count cache

Hoan thanh.

- Them `UnreadCountCacheService`.
- Cache key: `notification:unread:{userId}`.
- `GetUserInboxQuery.getUnreadCount` doc cache truoc, miss thi fallback DB va set cache.
- `MarkNotificationAsReadCommand` update cache khi read/read-all/delete.
- Batch worker set lai unread cache theo DB sau khi insert inbox.

## Phase 14 - Observability

Hoan thanh muc co ban.

- Them `spring-boot-starter-actuator`.
- Them `NotificationMetrics` dung Micrometer counter prefix `notification.*`.
- Them metrics cho request, duplicate request, job created, batch completed/failed, inbox inserted, email sent/failed.
- Log cac diem chinh co context: eventId, requestId, traceId, notificationId, jobId, batchId, userId.

## Phase 15 - Cleanup va retention

Hoan thanh.

- Them `NotificationRetentionCleanupScheduler`.
- Them config:
  - `notification.retention.inbox-days`
  - `notification.retention.email-log-days`
  - `notification.retention.delivery-days`
  - `notification.cleanup.enabled`
  - `notification.cleanup.cron`
- Cleanup xoa:
  - `user_notification` cu
  - `email_delivery_log` cu
  - delivery batch/job da `COMPLETED`, `FAILED`, `CANCELED`

## Phase 16 - Tich hop user-service

Hoan thanh service pilot theo `HD.md`.

- `user-service` da depend `notification-contract`.
- `NotificationPublisher` cua `user-service` giu nguyen method public de khong phai sua business flow, nhung ben trong da publish `NotificationRequestEvent`.
- Cac flow da chuyen sang topic public `notification.request.v1`:
  - OTP email
  - account registration verification email
  - password reset verification email
  - login alert email
  - user status change email
  - realtime welcome notification
- Them config `notification.topics.request` trong `user-service/src/main/resources/application-kafka.yml`.
- Them env `TOPIC_NOTIFICATION_REQUEST=notification.request.v1` cho local va docker.

## Cach test nhanh BUOC3

1. Start Kafka, Redis, PostgreSQL, notification-service.
2. Goi `POST /api/internal/notifications/requests` voi target `USER_IDS`.
3. Verify:

```sql
SELECT * FROM INF_NOTI.notification_delivery_job ORDER BY id DESC LIMIT 5;
SELECT * FROM INF_NOTI.notification_delivery_batch ORDER BY id DESC LIMIT 5;
SELECT * FROM INF_NOTI.user_notification ORDER BY id DESC LIMIT 5;
```

4. Test unread count:

```bash
curl "http://localhost:8084/api/notifications/unread-count" -H "X-USER-ID: 123"
```

5. Test email audit neu target co `emailByUserId` va channel `EMAIL`:

```bash
curl "http://localhost:8084/api/admin/email-deliveries?page=0&size=20"
```

## Ket luan

Theo pham vi code hien tai cua `HD.md`, cac hang muc nen tang da hoan thanh: contract -> API -> persistence -> worker -> inbox -> realtime/email -> cache/metrics/cleanup -> tich hop `user-service`.
