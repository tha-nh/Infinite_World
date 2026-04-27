# Hướng dẫn triển khai `notification-service`

## 1. Mục tiêu của tài liệu này

`HD.md` là tài liệu hướng dẫn thực thi theo từng bước dựa trên thiết kế đã chốt trong `DE_XUAT.md`.

Mục tiêu:

- biết rõ cần làm gì trước, làm gì sau
- tránh sót hạng mục quan trọng
- giúp triển khai đúng nền ngay từ đầu
- giữ code clean, dễ maintain, dễ scale

Tài liệu này giả định quyết định kiến trúc cuối cùng là:

- có `1` module dependency dùng chung là `notification-contract`
- có `1` module service thực thi là `notification`
- không tách `email` thành Maven module riêng ở giai đoạn này

---

## 2. Kết quả cuối cùng cần đạt

Sau khi hoàn thành toàn bộ các bước, hệ thống phải có:

- module `notification-contract`
- module `notification` dùng PostgreSQL + Kafka + Redis
- contract public chuẩn cho các service khác
- internal API để service khác gọi vào
- admin API để tạo và quản lý notification
- client API để lấy inbox và thao tác read/claim
- fanout worker theo batch
- email delivery log
- realtime dispatch
- retry / DLQ / audit cơ bản

---

## 3. Cách làm tổng thể

Không nên code tất cả cùng lúc.

Thứ tự đúng:

1. chuẩn hóa contract
2. thêm module và dependency
3. dựng persistence
4. dựng API
5. dựng worker fanout
6. dựng email delivery
7. thêm cache, metric, cleanup
8. tích hợp dần các service khác

---

## 4. Phase 1: Chuẩn hóa module và contract

## 4.1. Tạo module `notification-contract`

Mục tiêu:

- tạo nơi chứa contract dùng chung
- tách notification ra khỏi `common`

Việc cần làm:

1. tạo module mới `notification-contract` ở root repo
2. thêm module này vào `pom.xml` gốc
3. tạo `pom.xml` cho module mới
4. cho `notification-contract` depend vào `common` nếu cần một số utility rất nhẹ
5. không để `notification-contract` depend ngược vào `notification`

Kết quả mong muốn:

- `user-service` và `notification` đều import được module này

## 4.2. Tạo package structure cho `notification-contract`

Khuyến nghị:

```text
notification-contract
└── src/main/java/com/infinite/notification/contract
    ├── constant
    ├── dto
    ├── enumtype
    ├── event
    ├── metadata
    ├── builder
    └── client
```

Lưu ý:

- `client` ở đây chỉ là helper mỏng
- không chứa implementation Kafka/REST nặng

## 4.3. Tạo các enum dùng chung

Cần tạo:

- `NotificationChannel`
- `NotificationType`
- `NotificationPriority`
- `NotificationStatus`
- `DeliveryStatus`
- `NotificationTargetType`
- `NotificationActionType`

Yêu cầu:

- enum phải đủ rõ nghĩa
- tên ổn định, tránh đổi sau này

## 4.4. Tạo base metadata model

Cần tạo model chung cho các event/request:

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

Khuyến nghị:

- tạo class `BaseNotificationEvent`
- các contract khác kế thừa hoặc chứa composition từ class này

## 4.5. Tạo các DTO contract chính

Cần tạo:

- `NotificationRequestEvent`
- `NotificationTarget`
- `NotificationContent`
- `NotificationAction`
- `NotificationReward`
- `NotificationSourceMetadata`

Yêu cầu:

- field đặt tên rõ ràng
- không thêm field delivery nội bộ vào contract public
- dùng annotation validation nếu hợp lý

## 4.6. Tạo helper mỏng cho caller

Cần tạo:

- `NotificationRequestBuilder`
- `NotificationTopicNames`
- `NotificationPublisher` interface

Mục tiêu:

- service khác build request ít lỗi hơn
- giảm code lặp

Không làm ở bước này:

- Kafka producer implementation nặng
- REST client implementation nặng

## 4.7. Chốt version contract

Cần chốt ngay:

- `schemaVersion = v1`
- topic public: `notification.request.v1`
- topic callback nếu cần: `notification.status.changed.v1`

Checklist phase 1:

- [ ] tạo module `notification-contract`
- [ ] thêm module vào root `pom.xml`
- [ ] tạo enum chung
- [ ] tạo metadata chung
- [ ] tạo DTO contract chính
- [ ] tạo builder/helper mỏng
- [ ] chốt version/topic public

---

## 5. Phase 2: Làm sạch `common` và cập nhật dependency

## 5.1. Rà soát phần notification đang nằm trong `common`

Việc cần làm:

1. liệt kê toàn bộ DTO/event notification đang nằm trong `common`
2. phân loại:
   - cái nào là public contract
   - cái nào là downstream/internal contract

Public contract cần chuyển sang:

- `notification-contract`

Downstream/internal contract có thể:

- để trong `notification`
- hoặc tạm giữ ở `common` rồi di chuyển sau

## 5.2. Cập nhật dependency các module

Sau khi có `notification-contract`, cập nhật:

- `notification` -> depend `notification-contract`
- `user-service` -> depend `notification-contract`
- service khác về sau -> depend `notification-contract`

Mục tiêu:

- service khác không cần tìm contract trong `common`

## 5.3. Chuyển dần caller sang contract mới

Ví dụ ở `user-service`:

- không build `WebSocketNotificationEvent` trực tiếp từ business flow nữa
- thay bằng `NotificationRequestEvent`

Checklist phase 2:

- [ ] rà soát DTO/event notification trong `common`
- [ ] chuyển public contract sang `notification-contract`
- [ ] cập nhật dependency module
- [ ] đổi caller sang contract mới

---

## 6. Phase 3: Chuẩn bị module `notification`

## 6.1. Thêm dependency cần thiết

Trong module `notification`, thêm:

- `spring-boot-starter-data-jpa`
- `postgresql`
- `spring-boot-starter-validation`
- dependency tới `notification-contract`

Giữ lại:

- Kafka
- Redis
- Mail

## 6.2. Cấu hình datasource

Thêm vào `notification/src/main/resources/application.yml`:

- datasource PostgreSQL
- JPA config
- schema mặc định `INF_NOTIFICATION`
- batch setting cho Hibernate nếu cần

Nhưng lưu ý:

- insert lớn vẫn nên dùng JDBC/native batch, không phụ thuộc JPA thuần

## 6.3. Tổ chức lại package trong `notification`

Khuyến nghị:

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

Mục tiêu:

- không để code dồn vào `service/impl`
- tách rõ orchestration và infrastructure

Checklist phase 3:

- [ ] thêm dependency cho `notification`
- [ ] cấu hình PostgreSQL
- [ ] thêm dependency `notification-contract`
- [ ] chuẩn hóa package structure

---

## 7. Phase 4: Tạo database schema và migration

## 7.1. Tạo thư mục migration

Khuyến nghị ban đầu:

- `notification/db`

Nếu sau này chuẩn hóa hơn:

- thêm Flyway

## 7.2. Tạo các bảng theo thứ tự đúng

Thứ tự nên tạo:

1. `notification_request`
2. `notification_template`
3. `notification_target_rule`
4. `notification_delivery_job`
5. `notification_delivery_batch`
6. `user_notification`
7. `user_notification_claim_log`
8. `email_delivery_log`

## 7.3. Tạo index ngay từ đầu

Không nên đợi chạy chậm rồi mới thêm.

Ít nhất phải có:

- unique cho `event_id`
- unique cho `idempotency_key`
- unique `(user_id, notification_id)`
- index query inbox theo `user_id, created_at`
- index job/batch theo `status`

## 7.4. Chốt retention sơ bộ

Ngay khi tạo schema, phải quyết định sơ bộ:

- `user_notification` giữ bao lâu
- `email_delivery_log` giữ bao lâu

Khuyến nghị:

- inbox: 90-180 ngày
- email delivery log: ít nhất 90 ngày

Checklist phase 4:

- [ ] tạo thư mục migration
- [ ] tạo SQL schema
- [ ] tạo index
- [ ] chốt retention sơ bộ

---

## 8. Phase 5: Tạo persistence layer

## 8.1. Tạo entity

Cần tạo entity cho:

- `NotificationRequest`
- `NotificationTemplate`
- `NotificationTargetRule`
- `NotificationDeliveryJob`
- `NotificationDeliveryBatch`
- `UserNotification`
- `UserNotificationClaimLog`
- `EmailDeliveryLog`

Yêu cầu:

- mapping rõ ràng
- không lạm dụng cascade bừa bãi
- ưu tiên đơn giản, dễ đọc

## 8.2. Tạo repository

Tạo repository cho từng aggregate cần query/command.

Tối thiểu:

- request repository
- template repository
- job repository
- batch repository
- user notification repository
- email delivery log repository

## 8.3. Tạo query riêng cho các bài toán lớn

Không cố nhét mọi thứ vào method name của Spring Data.

Các query quan trọng:

- lấy inbox theo user
- unread count
- lấy batch pending
- lấy job failed
- lấy email delivery failed

Checklist phase 5:

- [ ] tạo entity
- [ ] tạo repository
- [ ] tạo custom query cần thiết

---

## 9. Phase 6: Làm inbound request flow

## 9.1. Tạo internal API cho service khác gọi

Tạo endpoint:

- `POST /api/internal/notifications/requests`

Request body:

- `NotificationRequestEvent`

Việc endpoint phải làm:

1. validate contract
2. validate `idempotencyKey`
3. lưu `notification_request`
4. map sang `notification_template`
5. tạo `notification_target_rule`
6. tạo `notification_delivery_job`
7. publish event nội bộ để xử lý tiếp

## 9.2. Tạo inbound Kafka consumer cho topic public

Nếu dùng async integration:

- consumer topic `notification.request.v1`

Consumer này phải dùng cùng logic application service như internal API, không duplicate business flow.

## 9.3. Tạo application service dùng chung cho inbound flow

Nên có use case kiểu:

- `CreateNotificationRequestUseCase`

Để:

- internal API gọi vào
- Kafka consumer cũng gọi vào

Mục tiêu:

- chỉ có một nơi xử lý business flow

Checklist phase 6:

- [ ] tạo internal API
- [ ] tạo public Kafka consumer
- [ ] gom chung logic vào application service

---

## 10. Phase 7: Làm admin API

## 10.1. Tạo admin API cơ bản

Tạo:

- `POST /api/admin/notifications`
- `GET /api/admin/notifications`
- `GET /api/admin/notifications/{id}`
- `POST /api/admin/notifications/{id}/cancel`
- `POST /api/admin/notifications/{id}/retry`
- `GET /api/admin/notifications/{id}/delivery-summary`

## 10.2. Chốt quyền truy cập

Cần định nghĩa tối thiểu:

- `NOTIFICATION_ADMIN_CREATE`
- `NOTIFICATION_ADMIN_VIEW`
- `NOTIFICATION_ADMIN_CANCEL`
- `NOTIFICATION_ADMIN_RETRY`

## 10.3. Dùng cùng flow tạo notification

Admin API không nên có flow riêng khác hẳn internal API.

Nó cũng nên:

- tạo `NotificationRequest`
- tạo template
- tạo job

Checklist phase 7:

- [ ] tạo admin controller
- [ ] thêm phân quyền
- [ ] dùng lại application service chung

---

## 11. Phase 8: Làm client inbox API

## 11.1. Tạo các endpoint cơ bản

Tạo:

- `GET /api/notifications`
- `GET /api/notifications/unread-count`
- `POST /api/notifications/{id}/read`
- `POST /api/notifications/read-all`
- `POST /api/notifications/{id}/delete`
- `POST /api/notifications/{id}/claim`

## 11.2. Quy tắc bảo mật

Client API phải:

- lấy user hiện tại từ JWT/session
- không nhận `userId` từ client để query inbox người khác

## 11.3. Read và claim phải an toàn

`read`:

- chỉ update nếu notification thuộc user hiện tại

`claim`:

- dùng transaction
- lock row
- kiểm tra `isClaimed`

Checklist phase 8:

- [ ] tạo client controller
- [ ] gắn auth context đúng
- [ ] làm logic read
- [ ] làm logic claim an toàn

---

## 12. Phase 9: Làm fanout worker

## 12.1. Tạo topic nội bộ

Tối thiểu:

- `notification.delivery.requested`
- `notification.delivery.batch.requested`

## 12.2. Tạo job splitter worker

Worker đầu tiên phải:

1. đọc job
2. xác định tổng user match rule
3. chia thành batch
4. tạo `notification_delivery_batch`
5. publish từng batch

## 12.3. Tạo batch worker

Batch worker phải:

1. query user theo cursor
2. build record `user_notification`
3. insert theo batch
4. update batch status
5. update job counters
6. bắn realtime nếu cần

## 12.4. Không dùng offset lớn

Bắt buộc dùng cursor:

- `WHERE id > :lastId ORDER BY id LIMIT :batchSize`

## 12.5. Không save từng record bằng JPA

Phải ưu tiên:

- JDBC batch insert
- native bulk insert

Checklist phase 9:

- [ ] tạo topic nội bộ fanout
- [ ] tạo job splitter worker
- [ ] tạo batch worker
- [ ] dùng cursor
- [ ] dùng batch insert

---

## 13. Phase 10: Làm realtime dispatch

## 13.1. Chốt realtime là phụ trợ

Nguyên tắc:

- push realtime lỗi không được làm mất inbox

## 13.2. Tạo realtime dispatcher

Dispatcher nhận command từ worker và:

- build payload realtime chuẩn
- publish Redis pub/sub hoặc kênh websocket hiện có

## 13.3. Chốt payload dùng chung

Payload cần có:

- `eventType`
- `userId`
- `notification`
- `unreadCount`

Checklist phase 10:

- [ ] tạo realtime dispatcher
- [ ] chuẩn hóa payload
- [ ] đảm bảo realtime không làm hỏng flow inbox

---

## 14. Phase 11: Làm email delivery

## 14.1. Chốt email là downstream delivery

Service khác không gửi email notification trực tiếp nữa.

Flow đúng:

- service khác gửi `NotificationRequestEvent`
- `notification-service` quyết định có gửi email hay không

## 14.2. Tạo email delivery log flow

Khi cần gửi email:

1. tạo `EmailDeliveryLog` trạng thái `PENDING`
2. publish email event nội bộ
3. consumer gửi email provider
4. update trạng thái `SENT` hoặc `FAILED`
5. nếu có callback thì update `DELIVERED`

## 14.3. Tạo API audit email

Tạo:

- `GET /api/admin/email-deliveries`
- `GET /api/admin/email-deliveries/{id}`
- `POST /api/admin/email-deliveries/{id}/retry`

Checklist phase 11:

- [ ] tạo email delivery log
- [ ] tạo email producer/consumer nội bộ
- [ ] update trạng thái gửi
- [ ] tạo email audit API

---

## 15. Phase 12: Làm idempotency và retry

## 15.1. Idempotency ở request boundary

Yêu cầu:

- `idempotencyKey` là bắt buộc
- nếu request trùng thì trả lại kết quả cũ

## 15.2. Idempotency ở inbox insert

Yêu cầu:

- unique `(user_id, notification_id)`
- insert conflict thì bỏ qua hoặc update theo policy rõ ràng

## 15.3. Idempotency ở email delivery

Yêu cầu:

- unique `event_id` trong `email_delivery_log`

## 15.4. Retry

Làm retry cho:

- batch worker lỗi tạm thời
- email send lỗi tạm thời

Không làm retry vô hạn.

Khuyến nghị:

- retry ngắn 1-3 lần
- sau đó chuyển `FAILED` hoặc DLQ

Checklist phase 12:

- [ ] khóa `idempotencyKey`
- [ ] chống trùng inbox
- [ ] chống trùng email event
- [ ] thêm retry policy

---

## 16. Phase 13: Làm cache unread count

## 16.1. Dùng Redis đúng vai trò

Redis chỉ nên dùng cho:

- unread count
- realtime helper

Không dùng Redis làm inbox store.

## 16.2. Tạo cache key chuẩn

Ví dụ:

- `notification:unread:{userId}`

## 16.3. Cập nhật cache khi nào

Phải cập nhật cache khi:

- insert inbox mới
- read notification
- delete notification
- read all

## 16.4. Có fallback DB khi cache miss

Nếu cache miss:

- query DB
- set lại cache

Checklist phase 13:

- [ ] tạo unread cache key
- [ ] update cache ở các flow chính
- [ ] fallback DB khi cache miss

---

## 17. Phase 14: Làm observability

## 17.1. Thêm metric

Tối thiểu:

- request count theo `sourceService`
- duplicate request count
- job count theo status
- batch count theo status
- fanout throughput
- inbox insert latency
- email success/failure rate
- realtime success/failure rate

## 17.2. Structured log

Mọi flow chính phải log được:

- `eventId`
- `requestId`
- `traceId`
- `sourceService`
- `notificationId`
- `jobId`
- `batchId`
- `userId`

## 17.3. Dashboard cơ bản

Ít nhất cần xem được:

- job pending/failed
- batch failed
- email failed
- DLQ size

Checklist phase 14:

- [ ] thêm metric
- [ ] thêm structured log
- [ ] có dashboard cơ bản

---

## 18. Phase 15: Cleanup và retention

## 18.1. Tạo scheduler cleanup

Scheduler nên chạy:

- ban đêm
- ngoài giờ cao điểm

## 18.2. Dọn dữ liệu theo retention

Dọn:

- `user_notification`
- `email_delivery_log`
- dữ liệu job/batch cũ nếu cần

## 18.3. Partition nếu cần

Nếu dữ liệu tăng nhanh:

- partition `user_notification` theo tháng

Checklist phase 15:

- [ ] tạo cleanup scheduler
- [ ] chốt retention rule
- [ ] xem xét partition

---

## 19. Phase 16: Tích hợp service khác

## 19.1. Chọn service pilot đầu tiên

Khuyến nghị:

- bắt đầu từ `user-service`

Vì:

- đã có notification publisher cũ
- dễ thay thế dần sang contract mới

## 19.2. Thay cách gọi notification

Từ:

- publish email/websocket event trực tiếp

Sang:

- tạo `NotificationRequestEvent`
- gọi internal API hoặc publish topic public

## 19.3. Kiểm tra backward compatibility

Trong giai đoạn đầu:

- có thể duy trì flow cũ song song một thời gian ngắn
- sau đó cắt dần

Checklist phase 16:

- [ ] chọn service pilot
- [ ] chuyển sang `NotificationRequestEvent`
- [ ] test end-to-end tích hợp

---

## 20. Thứ tự triển khai thực tế tôi khuyến nghị

Nếu cần làm tuần tự ngoài đời thật, nên đi theo thứ tự sau:

1. tạo `notification-contract`
2. thêm dependency vào `notification` và `user-service`
3. tạo schema DB
4. tạo entity + repository
5. tạo internal API nhận request
6. tạo flow lưu `notification_request` và `notification_template`
7. tạo admin API
8. tạo client inbox API
9. tạo job splitter worker
10. tạo batch worker
11. tạo realtime dispatch
12. tạo email delivery log + consumer
13. thêm unread cache
14. thêm retry/DLQ
15. thêm metric/log/dashboard
16. tích hợp `user-service`
17. cleanup/retention

---

## 21. Những lỗi dễ mắc phải

Không nên làm:

- nhét tiếp contract notification vào `common`
- cho business service publish trực tiếp `WebSocketNotificationEvent`
- cho business service gọi provider email trực tiếp
- dùng `OFFSET` lớn để fanout
- save từng record inbox bằng JPA
- dùng Redis làm inbox chính
- không có `idempotencyKey`
- không có delivery log
- tách quá nhiều Maven module ngay từ đầu

---

## 22. Definition of Done cho giai đoạn đầu

Có thể coi phase đầu đạt nếu:

- `notification-contract` build được
- `notification` chạy được với PostgreSQL/Kafka/Redis
- internal API nhận được `NotificationRequestEvent`
- tạo được `notification_request`, `notification_template`, `notification_delivery_job`
- worker fanout tạo được `user_notification`
- client đọc được inbox
- email log tạo được khi gửi email
- retry cơ bản hoạt động
- có metric/log tối thiểu

---

## 23. Kết luận

Nếu làm theo tài liệu này đúng thứ tự, anh sẽ đi từ nền contract đến service implementation một cách sạch và ổn định, thay vì dựng tính năng rời rạc rồi phải đập lại sau.

Điểm quan trọng nhất cần giữ xuyên suốt là:

- contract public phải ổn định
- toàn bộ request phải có idempotency
- notification-service phải là entrypoint duy nhất
- fanout phải async theo batch
- email/realtime chỉ là delivery layer phía sau
