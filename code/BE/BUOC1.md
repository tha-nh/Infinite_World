# BƯỚC 1: Chuẩn hóa contract và làm sạch nền tích hợp

## 1. Mục tiêu của bước này

`BUOC1.md` chỉ tập trung vào giai đoạn đầu tiên của lộ trình triển khai `notification-service`, tương ứng:

- Phase 1 trong `HD.md`
- Phase 2 trong `HD.md`

Mục tiêu của bước này:

- tạo được module `notification-contract`
- chuẩn hóa contract dùng chung cho notification
- làm sạch `common` để không tiếp tục ôm notification contract
- cập nhật dependency cho các service liên quan
- chuẩn bị nền tích hợp ổn định trước khi làm database, API, worker

Đây là bước bắt buộc phải làm đúng ngay từ đầu. Nếu bước này làm ẩu thì về sau sẽ phải sửa rất nhiều ở `notification`, `user-service`, và các service khác.

---

## 2. Phạm vi của bước 1

Bước này chỉ làm các phần sau:

- tạo module `notification-contract`
- thêm module mới vào `pom.xml` gốc
- tạo package structure cho module mới
- tạo enum, metadata, DTO contract chính
- tạo helper mỏng cho caller
- chốt version contract và topic public
- rà phần notification đang nằm trong `common`
- chuyển public contract sang `notification-contract`
- cập nhật dependency của `notification` và `user-service`
- đổi dần caller sang contract mới

Bước này chưa làm:

- database schema
- entity/repository
- internal/admin/client API
- fanout worker
- realtime dispatch
- email delivery log

---

## 3. Kết quả cần có sau bước 1

Sau khi hoàn thành `BUOC1.md`, hệ thống phải đạt:

- repo có module mới `notification-contract`
- root `pom.xml` nhận module này
- `notification-contract` chứa contract public chuẩn
- `notification` import được `notification-contract`
- `user-service` import được `notification-contract`
- các service khác về sau chỉ cần depend `notification-contract`
- `common` không tiếp tục là nơi phát triển contract notification mới

---

## 4. Phase 1: Tạo `notification-contract`

## 4.1. Tạo module mới

Việc cần làm:

1. tạo thư mục module mới ở root repo:
   - `D:\Infinite_World\BE\notification-contract`
2. tạo `pom.xml` cho module này
3. thêm `notification-contract` vào danh sách `<modules>` trong `pom.xml` gốc

Kết quả mong muốn:

- module build được độc lập
- các module khác có thể depend vào nó

## 4.2. Chốt package gốc

Khuyến nghị package gốc:

- `com.infinite.notification.contract`

Không nên dùng:

- `com.infinite.common.notification`
- package có dấu `-`

Ví dụ cấu trúc:

```text
notification-contract
└── src/main/java/com/infinite/notification/contract
    ├── builder
    ├── client
    ├── constant
    ├── dto
    ├── enumtype
    ├── event
    └── metadata
```

Lưu ý:

- `client` trong module này chỉ là helper mỏng
- không phải implementation Kafka/REST đầy đủ

## 4.3. Tạo enum dùng chung

Cần tạo tối thiểu:

- `NotificationChannel`
- `NotificationType`
- `NotificationPriority`
- `NotificationStatus`
- `DeliveryStatus`
- `NotificationTargetType`
- `NotificationActionType`

Yêu cầu:

- enum phải rõ nghĩa
- tên phải ổn định
- tránh dùng tên mơ hồ hoặc phụ thuộc implementation nội bộ

## 4.4. Tạo metadata chung

Cần có model metadata chung cho mọi request/event:

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

- tạo `BaseNotificationEvent`

Mục tiêu:

- mọi service gọi vào có format thống nhất
- tracing/idempotency rõ ràng ngay từ đầu

## 4.5. Tạo DTO contract chính

Cần tạo:

- `NotificationRequestEvent`
- `NotificationTarget`
- `NotificationContent`
- `NotificationAction`
- `NotificationReward`
- `NotificationSourceMetadata`

Yêu cầu:

- chỉ chứa public contract
- không nhét business logic
- không nhét field nội bộ của worker/fanout

## 4.6. Tạo helper mức nhẹ cho caller

Cần tạo:

- `NotificationRequestBuilder`
- `NotificationTopicNames`
- `NotificationPublisher` interface

Mục tiêu:

- service khác dùng dễ hơn
- giảm lặp code khi build request
- hạn chế sai contract

Không làm trong bước này:

- Kafka producer implementation nặng
- REST client implementation nặng
- retry/fallback logic

## 4.7. Chốt version contract

Phải chốt ngay:

- `schemaVersion = v1`
- topic public chính:
  - `notification.request.v1`
- topic callback nếu cần:
  - `notification.status.changed.v1`

Nguyên tắc:

- mọi thay đổi public về sau phải theo version

---

## 5. Checklist Phase 1

- [ ] tạo thư mục module `notification-contract`
- [ ] tạo `notification-contract/pom.xml`
- [ ] thêm `notification-contract` vào `pom.xml` gốc
- [ ] tạo package gốc `com.infinite.notification.contract`
- [ ] tạo enum dùng chung
- [ ] tạo `BaseNotificationEvent`
- [ ] tạo `NotificationRequestEvent`
- [ ] tạo `NotificationTarget`
- [ ] tạo `NotificationContent`
- [ ] tạo `NotificationAction`
- [ ] tạo `NotificationReward`
- [ ] tạo helper mỏng cho caller
- [ ] chốt `schemaVersion = v1`
- [ ] chốt topic public

---

## 6. Phase 2: Làm sạch `common` và cập nhật caller

## 6.1. Rà phần notification đang nằm trong `common`

Việc cần làm:

1. tìm toàn bộ DTO/event liên quan notification trong `common`
2. phân loại:
   - public contract
   - internal/downstream contract

Public contract:

- phải chuyển sang `notification-contract`

Internal/downstream contract:

- có thể giữ tạm trong `notification`
- hoặc chuyển sau khi refactor sâu hơn

## 6.2. Chốt nguyên tắc cho `common`

Sau bước này:

- không thêm contract notification mới vào `common`
- `common` chỉ giữ utility/global component thật sự dùng toàn hệ thống

Điều này rất quan trọng để `common` không phình lên thành “kho tạp hóa”.

## 6.3. Cập nhật dependency module

Sau khi có `notification-contract`, cần cập nhật:

- `notification` -> depend `notification-contract`
- `user-service` -> depend `notification-contract`

Về sau:

- service nào tích hợp notification cũng depend `notification-contract`

## 6.4. Chuyển caller sang contract mới

Rà trong `user-service` và các service khác nếu có:

- chỗ nào đang publish trực tiếp event delivery như email/websocket
- chỗ nào đang tự build payload notification cục bộ

Hướng xử lý:

- chuyển dần sang `NotificationRequestEvent`

Mục tiêu:

- business service chỉ biết public contract
- không biết chi tiết delivery nội bộ

## 6.5. Chưa cần xóa gấp mọi thứ cũ

Nếu hiện tại code còn dùng một số event cũ:

- có thể giữ tạm trong thời gian migration
- nhưng không được tiếp tục phát triển thêm dựa trên flow cũ

Nguyên tắc:

- flow mới dùng `NotificationRequestEvent`
- flow cũ chỉ tồn tại tạm để chuyển đổi

---

## 7. Checklist Phase 2

- [ ] rà toàn bộ contract notification trong `common`
- [ ] phân loại public contract và internal contract
- [ ] chuyển public contract sang `notification-contract`
- [ ] dừng việc thêm contract notification mới vào `common`
- [ ] cập nhật dependency `notification -> notification-contract`
- [ ] cập nhật dependency `user-service -> notification-contract`
- [ ] rà caller trong `user-service`
- [ ] chuyển dần caller sang `NotificationRequestEvent`
- [ ] xác định rõ phần cũ nào được giữ tạm để migration

---

## 8. Thứ tự làm chi tiết tôi khuyến nghị

Nên làm đúng theo thứ tự này:

1. tạo module `notification-contract`
2. thêm module vào root `pom.xml`
3. tạo package structure
4. tạo enum
5. tạo metadata base
6. tạo `NotificationRequestEvent` và DTO liên quan
7. tạo helper mỏng
8. chốt topic/version
9. rà `common`
10. chuyển contract public từ `common` sang `notification-contract`
11. cập nhật dependency cho `notification`
12. cập nhật dependency cho `user-service`
13. đổi caller sang contract mới

Nếu làm ngược, ví dụ sửa caller trước khi chốt contract, rất dễ phải sửa lại lần nữa.

---

## 9. Điều kiện hoàn thành bước 1

Có thể coi `BUOC1.md` hoàn tất khi:

- module `notification-contract` đã tồn tại và build được
- `notification` depend được vào `notification-contract`
- `user-service` depend được vào `notification-contract`
- đã có `NotificationRequestEvent` và các DTO nền
- đã chốt topic public và version `v1`
- `common` không còn là nơi phát triển contract notification mới
- đã có kế hoạch migration rõ cho caller cũ

---

## 10. Những lỗi dễ mắc trong bước này

Không nên làm:

- tạo package kiểu `common-notification`
- tiếp tục nhét contract notification vào `common`
- tách thêm `notification-client` thành module riêng ở giai đoạn này
- cho helper mỏng phình thành implementation nặng
- để business service biết topic nội bộ của `notification`
- đổi contract public liên tục khi chưa chốt version

---

## 11. Kết luận

`BUOC1.md` là bước nền quan trọng nhất. Nếu làm đúng, các bước sau như database, API, worker, email delivery sẽ đi rất sạch. Nếu làm sai, gần như chắc chắn sẽ phải sửa kiến trúc ở giữa chừng.

Điểm cần nhớ:

- chỉ có `1` module dependency dùng chung là `notification-contract`
- `common` phải được giữ gọn
- `NotificationRequestEvent` là trung tâm của tích hợp mới
- contract phải versioned ngay từ đầu
