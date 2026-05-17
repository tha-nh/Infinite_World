# BƯỚC 3: Hoàn thiện fanout, delivery, cache, observability và cleanup

## 1. Mục tiêu của bước này

`BUOC3.md` tập trung vào giai đoạn hoàn thiện phần xử lý phía sau của `notification-service`, tương ứng:

- Phase 9 trong `HD.md`
- Phase 10 trong `HD.md`
- Phase 11 trong `HD.md`
- Phase 12 trong `HD.md`
- Phase 13 trong `HD.md`
- Phase 14 trong `HD.md`
- Phase 15 trong `HD.md`

Nếu `BUOC1.md` là bước chốt contract và `BUOC2.md` là bước dựng lõi service, thì bước này là phần đưa hệ thống sang trạng thái có thể chạy production đúng hướng:

- fanout bất đồng bộ theo `job + batch`
- inbox là dữ liệu chính, realtime chỉ là lớp phụ trợ
- email đi qua log và downstream flow chuẩn
- retry có giới hạn, idempotency có ở boundary quan trọng
- unread count dùng Redis đúng vai trò
- metric, structured log, cleanup phải có từ sớm

---

## 2. Bám theo dự án hiện tại

Tài liệu này không viết theo kiểu dự án mẫu, mà bám theo code hiện đang có trong repo `BE`.

## 2.1. Nguyên tắc chốt cho bước này

Hướng tốt nhất cho dự án này không phải là giữ nguyên toàn bộ flow cũ, cũng không phải là đập bỏ hết để viết lại toàn bộ theo flow mới.

Hướng đúng là:

- kiến trúc đi theo flow mới
- implementation tận dụng lại flow/class/service/consumer/repository cũ nếu chúng còn đúng vai trò

Cách hiểu cụ thể:

- `flow mới` là boundary và orchestration mới:
  - service khác gửi `NotificationRequestEvent`
  - `notification-service` là entrypoint duy nhất
  - fanout đi qua `job + batch`
  - inbox là dữ liệu chính
  - email/realtime là downstream delivery

- `flow cũ` chỉ nên được giữ nếu nó là implementation adapter hoặc service nền đã dùng ổn:
  - `EmailNotificationConsumer`
  - `WebSocketNotificationConsumer`
  - `EmailServiceImpl`
  - `WebSocketNotificationServiceImpl`
  - entity/repository hiện có

Ngược lại, không nên giữ flow cũ ở các điểm boundary nếu chúng đi ngược hướng kiến trúc mới:

- không để service khác publish trực tiếp `notification.email`
- không để service khác publish trực tiếp `notification.websocket`
- không dùng email/websocket event cũ làm public request contract chính nữa

Tóm lại:

- cái cũ nào là adapter tốt thì giữ
- cái cũ nào là entry flow sai hướng thì thay
- cái mới chỉ nên thay boundary và orchestration, không thay bừa toàn bộ implementation nếu chưa cần

Hiện trạng của module `notification`:

- đã có `InternalNotificationController`
- đã có `AdminNotificationController`
- đã có `ClientNotificationController`
- đã có `CreateNotificationRequestUseCase`
- đã có entity/repository cho `notification_request`, `notification_template`, `notification_target_rule`, `notification_delivery_job`, `notification_delivery_batch`, `user_notification`, `user_notification_claim_log`, `email_delivery_log`
- đã có `NotificationRequestConsumer`
- đã có consumer cũ cho `notification.email` và `notification.websocket`
- đã có `Topics.java` nhưng mới chứa topic cũ, chưa có topic nội bộ cho fanout

Nguyên tắc bắt buộc khi triển khai bước này trong repo hiện tại:

- ưu tiên dùng lại flow, service, consumer, repository và cách gọi hàm đang có nếu chúng đã đúng vai trò
- không tự tạo thêm một bộ response mới, một bộ config mới, hay một bộ downstream flow mới nếu dự án đã có thứ dùng chung
- các API mới hoặc API được bổ sung phải giữ style hiện tại:
  - `ApiResponse`
  - `PageResponse`
  - `Response.code(...)`
  - `Response.message(...)`
- message thành công và message lỗi phải ưu tiên key i18n của `common`, không hardcode text trong controller
- cấu hình phải bám các key môi trường hiện đang có trong `.env.properties` và `.env_docker.properties`

Các key đang có và cần tiếp tục dùng lại nếu phù hợp:

- `KAFKA_BOOTSTRAP_SERVERS`
- `KAFKA_CONSUMER_GROUP`
- `MESSAGING_PROVIDER`
- `TOPIC_EMAIL`
- `TOPIC_WEBSOCKET`
- `DB_URL`
- `DB_USERNAME`
- `DB_PASSWORD`
- `NOTI_SCHEMA`
- `REDIS_HOST`
- `REDIS_PORT`
- `REDIS_PASSWORD`

Điều đó có nghĩa là ở bước 3, anh không cần dựng mới từ đầu. Việc cần làm là nối nốt các phần còn thiếu và refactor đúng chỗ:

- từ `CreateNotificationRequestUseCase` sang fanout worker
- từ `notification_delivery_job` sang `notification_delivery_batch`
- từ inbox insert sang realtime/email downstream
- từ API hiện có sang cache, retry, observability và cleanup

---

## 3. Kết quả cần có sau bước 3

Sau khi hoàn thành `BUOC3.md`, hệ thống phải đạt:

- request tạo notification sinh ra `notification_delivery_job` và được worker xử lý tiếp
- fanout worker tạo được `user_notification` theo batch lớn
- realtime không làm hỏng inbox nếu lỗi
- email có `EmailDeliveryLog` và retry flow rõ ràng
- duplicate request, duplicate inbox insert, duplicate email event được chặn
- unread count có Redis cache và có fallback DB
- có metric/log đủ để theo dõi request, job, batch, email, realtime
- có cleanup scheduler dọn dữ liệu cũ theo retention rule

---

## 4. Phase 9: Làm fanout worker

## 4.1. Mục tiêu phase này trong dự án hiện tại

Trong code hiện tại, `CreateNotificationRequestUseCase` đã:

1. lưu `NotificationRequest`
2. lưu `NotificationTemplate`
3. lưu `NotificationTargetRule`
4. lưu `NotificationDeliveryJob`

Nhưng đang dừng ở dòng:

- `// TODO: Publish internal event for fanout worker`

Vì vậy, phase 9 của dự án này chính là hoàn thiện nửa sau của flow đó.

## 4.2. Việc cần sửa trước tiên

File cần sửa đầu tiên:

- `notification/src/main/java/com/infinite/notification/application/command/CreateNotificationRequestUseCase.java`
- `notification/src/main/java/com/infinite/notification/messaging/Topics.java`
- `notification/src/main/resources/application.yml`

Cần bổ sung topic nội bộ mới:

- `notification.delivery.requested`
- `notification.delivery.batch.requested`

Nên thêm vào `Topics.java` các hằng số mới thay vì hardcode trong worker:

- `NOTIFICATION_DELIVERY_REQUESTED`
- `NOTIFICATION_DELIVERY_BATCH_REQUESTED`

Và thêm mapping trong `application.yml`:

- `messaging.topics.notification-delivery-requested`
- `messaging.topics.notification-delivery-batch-requested`

Nếu thêm env key cho topic mới, nên theo đúng pattern dự án đang dùng:

- `TOPIC_NOTIFICATION_DELIVERY_REQUESTED`
- `TOPIC_NOTIFICATION_DELIVERY_BATCH_REQUESTED`

Không đổi hoặc phá vỡ các key cũ đang chạy ổn:

- `TOPIC_EMAIL`
- `TOPIC_WEBSOCKET`

## 4.3. Cách làm cụ thể trong repo này

### Bước 1: phát event nội bộ sau khi tạo job

Trong `CreateNotificationRequestUseCase`:

- inject `MessagePublisher` hoặc `KafkaMessagePublisher`
- sau khi `deliveryJobRepository.save(job)` thành công, publish message nội bộ chứa:
  - `jobId`
  - `notificationId`
  - `requestId`
  - `traceId`

Không nên:

- fanout trực tiếp ngay trong `execute(...)`
- query user và insert inbox trong transaction của request nhận vào

### Bước 2: tạo worker tách job thành batch

Nên tạo class mới, ví dụ:

- `notification/src/main/java/com/infinite/notification/worker/NotificationDeliveryJobWorker.java`

Worker này nhận `jobId`, sau đó:

1. load `NotificationDeliveryJob`
2. load `NotificationTemplate`
3. load `NotificationTargetRule`
4. tính `totalTarget`
5. chia batch theo `batchSize`
6. tạo `NotificationDeliveryBatch`
7. publish từng batch sang topic `notification.delivery.batch.requested`

### Bước 3: tạo batch worker

Nên tạo class mới, ví dụ:

- `notification/src/main/java/com/infinite/notification/worker/NotificationDeliveryBatchWorker.java`

Worker này:

1. nhận `batchId`
2. load `NotificationDeliveryBatch`
3. dùng `rule_payload` để xác định tập user cần xử lý
4. query user theo cursor
5. build danh sách `UserNotification`
6. bulk insert
7. update `processed_count`, `success_count`, `failed_count`
8. nếu có channel `REALTIME` thì phát command realtime
9. nếu có channel `EMAIL` thì tạo `EmailDeliveryLog` và phát email command

## 4.4. Lấy user target trong dự án này như thế nào

Tài liệu `DE_XUAT.md` chốt rằng không nên fanout lớn bằng REST paging sang `user-service`.

Với repo hiện tại, anh nên chia 2 mức:

- nếu `NotificationTargetType = USER_IDS`: xử lý trực tiếp từ `rule_payload`
- nếu về sau có rule kiểu `ROLE`, `SEGMENT`, `ALL`: cần thêm query read-side hoặc cơ chế lấy target phù hợp, nhưng không nên giải bằng offset lớn qua REST

Ở bước 3 hiện tại, nên ưu tiên hoàn thiện chắc luồng `USER_IDS` trước vì phù hợp nhất với code đã có.

## 4.5. Không save từng record bằng JPA

Repo hiện có `UserNotificationRepository`, nhưng phase này không nên dùng kiểu:

- loop `repository.save(entity)`

Nên làm một custom repository hoặc JDBC insert riêng, ví dụ:

- `infrastructure/persistence/repository/UserNotificationBulkRepository`

Hoặc thêm implementation custom cho `UserNotificationRepository` để dùng:

- `JdbcTemplate.batchUpdate(...)`

Mục tiêu:

- insert nhanh
- không phình persistence context
- đúng hướng scale đã chốt trong `DE_XUAT.md`

---

## 5. Checklist Phase 9

- [ ] thêm topic nội bộ fanout vào `Topics.java`
- [ ] thêm config topic fanout vào `application.yml`
- [ ] sửa `CreateNotificationRequestUseCase` để publish `notification.delivery.requested`
- [ ] tạo `NotificationDeliveryJobWorker`
- [ ] tạo `NotificationDeliveryBatchWorker`
- [ ] bulk insert `UserNotification`
- [ ] update trạng thái job/batch đúng vòng đời

---

## 6. Phase 10: Làm realtime dispatch

## 6.1. Hiện trạng dự án

Repo đang có:

- `consumer/WebSocketNotificationConsumer.java`
- `service/WebSocketNotificationService.java`
- `service/impl/WebSocketNotificationServiceImpl.java`
- topic cũ `notification.websocket`

Điều này có nghĩa là anh đã có delivery adapter cho realtime. Vấn đề còn thiếu là gắn nó đúng vào flow mới của inbox.

## 6.2. Hướng làm phù hợp với code hiện tại

Không nên bỏ hẳn flow cũ ngay. Nên chuyển vai trò của nó:

- trước đây: service khác có thể publish trực tiếp `notification.websocket`
- sau bước 3: chỉ `notification-service` nội bộ mới phát command sang topic này

Tức là:

- `WebSocketNotificationConsumer` vẫn có thể giữ
- nhưng nguồn phát message phải chuyển sang batch worker hoặc realtime dispatcher nội bộ

Nếu payload downstream hiện tại đã được gateway/client khác hiểu, nên giữ nguyên format đó ở lớp consumer/service cũ, chỉ thay nguồn phát event từ bên ngoài sang nội bộ `notification-service`.

## 6.3. Cách triển khai cụ thể

Nên tạo một dispatcher/service mới, ví dụ:

- `notification/src/main/java/com/infinite/notification/application/command/DispatchRealtimeNotificationUseCase.java`

Hoặc:

- `notification/src/main/java/com/infinite/notification/infrastructure/messaging/RealtimeDispatcher.java`

Service này nhận:

- `userId`
- `notificationId`
- payload title/body/action
- unread count hiện tại

Sau đó map sang payload mà `WebSocketNotificationConsumer`/`WebSocketNotificationService` đã hiểu.

## 6.4. Điểm cần chỉnh trong flow

Worker batch chỉ nên phát realtime sau khi inbox insert thành công.

Nếu realtime lỗi:

- log lỗi
- tăng metric lỗi
- không rollback inbox đã insert

Nghĩa là trong dự án này, realtime phải đứng sau `user_notification`, không đứng song song ở request boundary nữa.

---

## 7. Checklist Phase 10

- [ ] chuyển `notification.websocket` thành topic nội bộ của `notification-service`
- [ ] tạo realtime dispatcher/use case nội bộ
- [ ] phát realtime sau khi insert inbox thành công
- [ ] log lỗi realtime nhưng không rollback inbox

---

## 8. Phase 11: Làm email delivery

## 8.1. Hiện trạng dự án

Repo hiện có:

- `consumer/EmailNotificationConsumer.java`
- `service/EmailService.java`
- `service/impl/EmailServiceImpl.java`
- topic cũ `notification.email`
- entity `EmailDeliveryLog`
- repository `EmailDeliveryLogRepository`

Như vậy phần gửi email thực tế đã có nền, nhưng chưa được buộc chặt vào flow mới của `NotificationRequestEvent`.

## 8.2. Hướng chuyển đúng cho code hiện tại

Không để service ngoài publish `EmailNotificationEvent` trực tiếp nữa.

Sau bước 3:

- service ngoài gửi `NotificationRequestEvent`
- `notification-service` quyết định có `EMAIL` channel hay không
- batch worker hoặc email orchestration service tạo `EmailDeliveryLog`
- sau đó mới publish sang topic `notification.email`

Tức là:

- `EmailNotificationConsumer` có thể giữ
- nhưng nó trở thành consumer downstream nội bộ, không còn là entrypoint public

## 8.3. Cách triển khai cụ thể trong repo này

Nên tạo một lớp orchestration mới, ví dụ:

- `notification/src/main/java/com/infinite/notification/application/command/EnqueueEmailDeliveryUseCase.java`

Lớp này làm:

1. nhận `notificationId`, `userId`, `toEmail`, metadata
2. tạo `EmailDeliveryLog` với `PENDING`
3. publish message sang topic `notification.email`

Sau đó sửa `EmailNotificationConsumer`:

- khi gửi thành công thì update `EmailDeliveryLog` sang `SENT`
- khi lỗi thì tăng `retryCount`, lưu `errorMessage`
- nếu có callback provider về sau thì update `DELIVERED`

## 8.4. Vấn đề cần xử lý ở dự án này

`EmailNotificationConsumer` hiện đang hỗ trợ cả format cũ và mới để backward compatibility.

Ở bước 3 anh nên giữ backward compatibility, nhưng phải thêm ghi chú rõ:

- flow mới ưu tiên đi từ `NotificationRequestEvent`
- flow cũ chỉ giữ tạm để migration

Không nên tiếp tục mở rộng logic nghiệp vụ dựa trên event email cũ.

## 8.5. API audit email

Phase này nên bổ sung thêm admin API mới, ví dụ:

- `GET /api/admin/email-deliveries`
- `GET /api/admin/email-deliveries/{id}`
- `POST /api/admin/email-deliveries/{id}/retry`

Có thể đặt ở:

- controller mới dưới `api/admin`
- hoặc thêm vào `AdminNotificationController` nếu muốn đi nhanh

Nhưng về cấu trúc sạch hơn, nên tách controller riêng cho email audit.

Khi thêm API mới trong dự án này, phải giữ cùng style response hiện có:

- success trả `ApiResponse`
- phân trang trả `PageResponse`
- message gọi qua `Response.message("...")`
- lỗi nghiệp vụ dùng exception đang có của repo, ví dụ `BadRequestException` với `I18n.msg("...")`

---

## 9. Checklist Phase 11

- [ ] chuyển `notification.email` thành downstream topic nội bộ
- [ ] tạo use case enqueue email delivery
- [ ] tạo `EmailDeliveryLog` trước khi publish email event
- [ ] sửa `EmailNotificationConsumer` để update log gửi
- [ ] thêm email audit API

---

## 10. Phase 12: Làm idempotency và retry

## 10.1. Request boundary trong dự án hiện tại

`CreateNotificationRequestUseCase` đã check:

- `requestRepository.existsByIdempotencyKey(...)`

Đây là nền đúng. Ở bước 3 chỉ cần siết chặt thêm:

- bảo đảm DB có unique constraint thật trên `idempotency_key`
- trả về kết quả cũ ổn định
- log duplicate request có đủ `eventId`, `requestId`, `traceId`

## 10.2. Inbox insert

Entity/bảng `user_notification` đã được thiết kế với unique `(user_id, notification_id)` theo tài liệu.

Nhưng để đúng production, phần bulk insert cũng phải tôn trọng điều đó:

- dùng `ON CONFLICT DO NOTHING` nếu phù hợp PostgreSQL
- hoặc bắt conflict theo policy rõ ràng

Không được để retry của batch worker tạo duplicate inbox.

## 10.3. Email delivery

`email_delivery_log` phải có unique `event_id`.

Ở dự án này, nên sinh `eventId` cho email delivery theo rule ổn định, ví dụ gắn với:

- `notificationId`
- `userId`
- `channel`

Mục tiêu là cùng một email delivery không bị gửi trùng chỉ vì consumer redeliver.

## 10.4. Retry policy cần gắn vào đâu

Trong repo hiện tại, retry nên được đặt ở hai nơi:

- worker xử lý `notification.delivery.batch.requested`
- `EmailNotificationConsumer`

Khuyến nghị:

- retry tối đa 3 lần
- tăng `retry_count`
- quá ngưỡng thì chuyển `FAILED`
- nếu đã có hạ tầng phù hợp thì đẩy DLQ

## 10.5. Điều chỉnh consumer hiện tại

`NotificationRequestConsumer` hiện đang `acknowledge()` cả khi lỗi và có comment `TODO: Send to DLQ`.

Đây là điểm cần sửa theo đúng phase 12:

- lỗi retryable thì không nên nuốt im rồi ack luôn
- phải có chiến lược retry/DLQ rõ ràng

Tương tự với `WebSocketNotificationConsumer` và `EmailNotificationConsumer`, cần thống nhất chính sách retry thay vì để mỗi consumer tự xử lý khác nhau.

Khi sửa các consumer này:

- ưu tiên giữ nguyên `@KafkaListener`, method signature và service dependency hiện có nếu chưa cần đổi
- chỉ bổ sung orchestration, retry handling, log context và update trạng thái
- không tự đổi key config hay tên topic cũ nếu môi trường đang dùng các key đó

---

## 11. Checklist Phase 12

- [ ] bảo đảm unique constraint thực tế cho `idempotency_key`
- [ ] dùng insert policy chống trùng cho `user_notification`
- [ ] dùng unique `event_id` cho `email_delivery_log`
- [ ] thêm retry count và failed policy cho batch worker
- [ ] sửa các Kafka consumer để có retry/DLQ policy rõ ràng

---

## 12. Phase 13: Làm cache unread count

## 12.1. Hiện trạng dự án

Module `notification` đã có dependency:

- `spring-boot-starter-data-redis`

và trong `application.yml` đã có:

- `spring.data.redis.*`

Nhưng hiện tại flow inbox API vẫn đang lấy unread count trực tiếp qua query.

## 12.2. Cách làm phù hợp với repo này

Nên tạo service riêng, ví dụ:

- `notification/src/main/java/com/infinite/notification/infrastructure/redis/UnreadCountCacheService.java`

Service này chịu trách nhiệm:

- `get(userId)`
- `set(userId, count)`
- `increment(userId, delta)`
- `evict(userId)`

Key cache nên chốt rõ và ổn định:

- `notification:unread:{userId}`

Nhưng cấu hình kết nối Redis vẫn phải dùng lại key môi trường đang có:

- `REDIS_HOST`
- `REDIS_PORT`
- `REDIS_PASSWORD`

## 12.3. Nối vào flow hiện tại

Cần sửa các chỗ sau:

- batch worker: sau khi insert inbox thành công thì tăng unread count
- `MarkNotificationAsReadCommand`: khi mark read thì giảm unread count hoặc evict cache
- `executeAll(...)`: update cache tương ứng
- delete notification: update cache tương ứng
- `GetUserInboxQuery.getUnreadCount(...)`: đọc cache trước, cache miss thì fallback DB

Tức là phase này sẽ đụng trực tiếp vào:

- `application/command/MarkNotificationAsReadCommand.java`
- `application/query/GetUserInboxQuery.java`
- worker mới của phase 9

## 12.4. Một điểm lệch hiện tại cần lưu ý

`ClientNotificationController` hiện đang nhận `userId` trực tiếp từ request param/body.

Theo `HD.md`, đây không phải hướng cuối cùng.

Nếu anh chưa sửa auth context ngay ở bước này, vẫn có thể làm cache trước. Nhưng trong tài liệu cần ghi rõ:

- unread cache phải gắn theo user thực
- khi chuyển sang auth context thật, key cache vẫn giữ dạng `notification:unread:{userId}`

---

## 13. Checklist Phase 13

- [ ] tạo `UnreadCountCacheService`
- [ ] nối cache vào `GetUserInboxQuery`
- [ ] nối cache vào `MarkNotificationAsReadCommand`
- [ ] nối cache vào batch worker
- [ ] có fallback DB khi cache miss

---

## 14. Phase 14: Làm observability

## 14.1. Mức tối thiểu phù hợp với dự án này

Repo hiện có logging bằng `Slf4j` ở controller, consumer và use case. Nhưng hiện tại log vẫn rời rạc, chủ yếu mới có message text.

Ở bước 3, cần chuẩn hóa log trên các điểm sau:

- `CreateNotificationRequestUseCase`
- `NotificationRequestConsumer`
- `NotificationDeliveryJobWorker`
- `NotificationDeliveryBatchWorker`
- `EmailNotificationConsumer`
- `WebSocketNotificationConsumer`
- `MarkNotificationAsReadCommand`
- `ClaimNotificationCommand`

## 14.2. Những field phải log thống nhất

Ít nhất cần giữ được:

- `eventId`
- `requestId`
- `traceId`
- `sourceService`
- `notificationId`
- `jobId`
- `batchId`
- `userId`

Nếu chưa có logging framework nâng cao, chỉ cần thống nhất format `log.info(...)` trước cũng được. Nhưng nên log theo cùng key để về sau dễ đẩy sang ELK/Grafana/Loki.

## 14.3. Metric nên thêm ở đâu

Nếu dự án chưa có Micrometer/Actuator thì phase này nên bổ sung luôn.

Chỗ tăng metric:

- khi nhận request mới
- khi phát hiện duplicate request
- khi tạo job
- khi batch thành công/thất bại
- khi insert inbox xong
- khi email gửi thành công/thất bại
- khi realtime thành công/thất bại

## 14.4. Dashboard cơ bản

Với dự án hiện tại, dashboard ban đầu chỉ cần đủ để trả lời 4 câu hỏi:

1. có request nào đang vào nhiều bất thường không
2. có job/batch nào đang pending hoặc failed không
3. email có đang fail tăng đột biến không
4. consumer có bị kẹt retry hoặc DLQ không

---

## 15. Checklist Phase 14

- [ ] chuẩn hóa log ở use case, worker, consumer
- [ ] thêm metric cho request/job/batch/email/realtime
- [ ] thêm duplicate request metric
- [ ] có dashboard cơ bản để theo dõi pending/failed

---

## 16. Phase 15: Cleanup và retention

## 16.1. Hiện trạng dự án

Module `notification` đã có package:

- `scheduler`

theo cấu trúc tài liệu đề xuất, nhưng hiện tại chưa thấy scheduler cleanup tương ứng.

Vì vậy phase này nên tận dụng đúng chỗ đó, không đặt logic cleanup lẫn vào service linh tinh.

## 16.2. Cách làm cụ thể trong repo này

Nên tạo scheduler mới, ví dụ:

- `notification/src/main/java/com/infinite/notification/scheduler/NotificationRetentionCleanupScheduler.java`

Scheduler này gọi xuống service/repository để:

- xóa hoặc archive `user_notification` cũ
- xóa `email_delivery_log` cũ
- dọn `notification_delivery_job` và `notification_delivery_batch` cũ nếu policy cho phép

## 16.3. Cần cấu hình gì

Nên thêm cấu hình trong `application.yml`, ví dụ nhóm:

- `notification.retention.inbox-days`
- `notification.retention.email-log-days`
- `notification.cleanup.enabled`
- `notification.cleanup.cron`

Mục tiêu:

- không hardcode retention trong scheduler
- dễ chỉnh theo môi trường

Nếu cần override theo môi trường, nên làm đúng pattern dự án:

- thêm placeholder `${...}` trong `application.yml`
- bổ sung key tương ứng vào `.env.properties`
- bổ sung key tương ứng vào `.env_docker.properties`

## 16.4. Partition

Nếu dữ liệu `user_notification` tăng nhanh, tài liệu này vẫn nên ghi rõ hướng chuẩn:

- ưu tiên partition theo tháng

Nhưng với dự án hiện tại, đây là bước đánh dấu hướng triển khai tiếp theo, không bắt buộc code ngay nếu volume chưa tới mức đó.

---

## 17. Checklist Phase 15

- [ ] tạo cleanup scheduler trong package `scheduler`
- [ ] thêm config retention vào `application.yml`
- [ ] dọn `user_notification` cũ
- [ ] dọn `email_delivery_log` cũ
- [ ] có ghi chú rõ về partition khi volume tăng

---

## 18. Thứ tự làm chi tiết tôi khuyến nghị cho repo này

Nên làm đúng theo thứ tự dưới đây để ít đập đi sửa lại nhất:

1. mở `Topics.java` và `application.yml`, thêm topic nội bộ fanout
2. sửa `CreateNotificationRequestUseCase` để publish `notification.delivery.requested`
3. tạo `NotificationDeliveryJobWorker`
4. tạo `NotificationDeliveryBatchWorker`
5. làm bulk insert cho `UserNotification`
6. nối realtime dispatch vào sau inbox insert
7. tạo use case enqueue email delivery + update `EmailNotificationConsumer`
8. siết idempotency và retry policy cho worker/consumer
9. thêm `UnreadCountCacheService`
10. nối cache vào query/command inbox hiện có
11. thêm metric và structured log
12. tạo cleanup scheduler

Nếu làm ngược, ví dụ thêm cache/metric trước khi fanout flow xong, anh sẽ phải sửa lại rất nhiều điểm hook.

Trong toàn bộ quá trình này, phải giữ 3 nguyên tắc style của repo:

1. chỗ nào đã có service, consumer, repository hay DTO dùng chung thì tận dụng lại trước khi tạo class mới
2. key cấu hình phải bám `.env.properties`, `.env_docker.properties` và `application.yml` hiện có
3. API response và error message phải dùng `ApiResponse`, `PageResponse`, `Response.message(...)`, `I18n.msg(...)` theo pattern đang chạy trong dự án

---

## 19. Điều kiện hoàn thành bước 3

Có thể coi `BUOC3.md` hoàn tất khi:

- `CreateNotificationRequestUseCase` không còn dừng ở TODO fanout
- đã có worker xử lý `notification_delivery_job` và `notification_delivery_batch`
- `user_notification` được insert theo batch lớn
- realtime chỉ chạy sau inbox insert
- `EmailDeliveryLog` được tạo và cập nhật xuyên suốt flow email
- retry/idempotency hoạt động đúng ở request, inbox, email
- unread count có Redis cache và fallback DB
- có log/metric đủ để debug và vận hành
- có cleanup scheduler chạy theo retention config

---

## 20. Những lỗi dễ mắc trong bước này trên chính dự án này

Không nên làm:

- tiếp tục để `notification.email` và `notification.websocket` như entrypoint public cho service khác
- publish realtime/email ngay từ controller hoặc `CreateNotificationRequestUseCase`
- dùng `UserNotificationRepository.save(...)` trong loop lớn
- để `NotificationRequestConsumer` lỗi mà vẫn ack thẳng không có policy rõ ràng
- thêm cache unread count nhưng quên cập nhật ở `read-all` hoặc `delete`
- để `ClientNotificationController` tiếp tục là chuẩn cuối cùng với `userId` từ request mà không ghi rõ đây mới là trạng thái tạm

---

## 21. Kết luận

`BUOC3.md` cho repo này thực chất là bước nối các phần đã có thành một flow hoàn chỉnh.

Trục chính cần giữ là:

- `InternalNotificationController` và `NotificationRequestConsumer` chỉ là entrypoint
- `CreateNotificationRequestUseCase` là chỗ persist request và khởi tạo job
- worker mới là nơi fanout thật sự
- `notification.email` và `notification.websocket` trở thành downstream topic nội bộ
- Redis chỉ làm unread cache
- scheduler xử lý retention

Nếu anh làm đúng thứ tự này, code hiện tại của `notification` sẽ đi từ trạng thái “đã có nền” sang trạng thái “đủ vòng đời production” mà không phải đập lại kiến trúc đã dựng ở `BUOC1.md` và `BUOC2.md`.
