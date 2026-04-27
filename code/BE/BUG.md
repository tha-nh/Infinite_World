# Các thiếu sót và sai lệch sau khi triển khai `BUOC1.md`

## 1. Mục đích

File này ghi lại các điểm chưa hoàn tất hoặc mô tả chưa đúng sau khi triển khai:

- `BO_SMS.md`
- `BUOC1.md`
- `KET_QUA.md`

Mục tiêu:

- biết chính xác còn thiếu gì
- tránh hiểu nhầm là đã hoàn tất khi thực tế chưa xong
- xử lý dứt điểm trước khi chuyển sang bước tiếp theo

---

## 2. Kết luận ngắn

Hiện trạng:

- phần tạo module `notification-contract` đã làm khá tốt
- phần bỏ SMS khỏi flow chính của `notification-service` đã gần đúng
- nhưng `KET_QUA.md` đang kết luận mạnh hơn thực tế
- Phase 2 của `BUOC1.md` chưa hoàn tất

Tức là:

- chưa nên coi `BUOC1` là hoàn thành trọn vẹn
- chưa nên coi việc bỏ SMS là sạch hoàn toàn theo checklist

---

## 3. Các lỗi và thiếu sót còn tồn tại

## 3.1. `KET_QUA.md` ghi Phase 2 hoàn thành đầy đủ, nhưng thực tế chưa xong

Theo `BUOC1.md`, Phase 2 không chỉ là thêm dependency, mà còn gồm:

- rà phần notification trong `common`
- phân loại public contract và internal contract
- chuyển dần caller sang `NotificationRequestEvent`

Hiện trạng code:

- `user-service` vẫn đang dùng flow cũ qua `NotificationPublisher`
- vẫn đang import event cũ từ `common`

File liên quan:

- [user-service/src/main/java/com/infinite/user/service/NotificationPublisher.java](/d:/Infinite_World/BE/user-service/src/main/java/com/infinite/user/service/NotificationPublisher.java:1)

Kết luận:

- Phase 2 mới hoàn thành phần dependency
- chưa hoàn thành phần migration caller

## 3.2. `common` vẫn còn notification contracts cũ

`KET_QUA.md` và phần tổng kết đang ngầm cho thấy `common` đã sạch hơn nhiều, nhưng thực tế vẫn còn các notification event cũ trong `common`.

Các file còn tồn tại:

- [common/src/main/java/com/infinite/common/dto/event/EmailNotificationEvent.java](/d:/Infinite_World/BE/common/src/main/java/com/infinite/common/dto/event/EmailNotificationEvent.java:1)
- [common/src/main/java/com/infinite/common/dto/event/WebSocketNotificationEvent.java](/d:/Infinite_World/BE/common/src/main/java/com/infinite/common/dto/event/WebSocketNotificationEvent.java:1)
- [common/src/main/java/com/infinite/common/dto/event/AccountVerificationEvent.java](/d:/Infinite_World/BE/common/src/main/java/com/infinite/common/dto/event/AccountVerificationEvent.java:1)
- [common/src/main/java/com/infinite/common/dto/event/UserStatusChangeEvent.java](/d:/Infinite_World/BE/common/src/main/java/com/infinite/common/dto/event/UserStatusChangeEvent.java:1)

Điều này không sai hoàn toàn ở giai đoạn migration, nhưng phải được ghi nhận đúng:

- chúng chưa được chuyển hết
- chúng vẫn đang được dùng
- không thể nói `common` đã thôi chứa notification contract

## 3.3. Caller trong `user-service` chưa chuyển sang contract mới

Theo thiết kế mới:

- service khác nên dùng `NotificationRequestEvent`

Nhưng hiện tại:

- `user-service` vẫn publish `EmailNotificationEvent`
- `user-service` vẫn publish `WebSocketNotificationEvent`
- vẫn còn dùng event cũ cho các flow xác thực/tài khoản

File liên quan:

- [user-service/src/main/java/com/infinite/user/service/NotificationPublisher.java](/d:/Infinite_World/BE/user-service/src/main/java/com/infinite/user/service/NotificationPublisher.java:44)

Tác động:

- boundary mới chưa thực sự được áp dụng
- `notification-contract` mới đang tồn tại song song với flow cũ, chưa trở thành contract trung tâm thật sự

## 3.4. `notification-contract` chưa khớp hoàn toàn với mô tả trong tài liệu

Theo `BUOC1.md` và `DE_XUAT.md`, phần helper mỏng có thể gồm:

- `NotificationRequestBuilder`
- `NotificationTopicNames`
- `NotificationPublisher` interface

Hiện trạng:

- có `NotificationRequestBuilder`
- có `NotificationTopicNames`
- chưa thấy `NotificationPublisher` interface
- chưa thấy package `client` thực sự có nội dung

Điều này không phải lỗi nghiêm trọng, nhưng là điểm lệch giữa tài liệu và code.

## 3.5. Còn song song nhiều contract realtime/email

Hiện trạng trong repo:

- contract mới: `NotificationRequestEvent` trong `notification-contract`
- contract cũ ở `common`: `EmailNotificationEvent`, `WebSocketNotificationEvent`
- contract DTO riêng trong `notification`: `notification.dto.event.WebSocketNotificationEvent`

File liên quan:

- [notification-contract/src/main/java/com/infinite/notification/contract/event/NotificationRequestEvent.java](/d:/Infinite_World/BE/notification-contract/src/main/java/com/infinite/notification/contract/event/NotificationRequestEvent.java:1)
- [common/src/main/java/com/infinite/common/dto/event/EmailNotificationEvent.java](/d:/Infinite_World/BE/common/src/main/java/com/infinite/common/dto/event/EmailNotificationEvent.java:1)
- [common/src/main/java/com/infinite/common/dto/event/WebSocketNotificationEvent.java](/d:/Infinite_World/BE/common/src/main/java/com/infinite/common/dto/event/WebSocketNotificationEvent.java:1)
- [notification/src/main/java/com/infinite/notification/dto/event/WebSocketNotificationEvent.java](/d:/Infinite_World/BE/notification/src/main/java/com/infinite/notification/dto/event/WebSocketNotificationEvent.java:1)

Tác động:

- boundary chưa sạch
- người đọc code dễ nhầm đâu là contract public, đâu là internal contract

## 3.6. Phần bỏ SMS chưa sạch hoàn toàn theo checklist

Mặc dù flow chính của `notification-service` đã bỏ SMS, repo vẫn còn dấu vết SMS ngoài phần code Java chính.

### Config còn sót

- [.env.properties](/d:/Infinite_World/BE/.env.properties:30)
- [.env_docker.properties](/d:/Infinite_World/BE/.env_docker.properties:30)

Vẫn còn:

- `SMS_PROVIDER_URL`
- `SMS_ACCOUNT_SID`
- `SMS_AUTH_TOKEN`
- `SMS_FROM_NUMBER`
- `SMS_MOCK_MODE`
- `TOPIC_SMS`

### Tài liệu/i18n còn sót

- [user-service/README.md](/d:/Infinite_World/BE/user-service/README.md:43)
- [common/src/main/resources/i18n/messages_en.properties](/d:/Infinite_World/BE/common/src/main/resources/i18n/messages_en.properties:41)
- [common/src/main/resources/i18n/messages_vi.properties](/d:/Infinite_World/BE/common/src/main/resources/i18n/messages_vi.properties:41)

Tác động:

- không ảnh hưởng trực tiếp tới flow chính của `notification`
- nhưng mâu thuẫn với kết luận “đã xóa sạch SMS khỏi code, config, contract”

## 3.7. `KET_QUA.md` đang mô tả mạnh hơn thực tế

Một số nhận định trong `KET_QUA.md` chưa phản ánh đúng code hiện tại:

- “Phase 2 hoàn thành đầy đủ”
- “mọi service đều dùng chung `NotificationRequestEvent`”
- “common không còn chứa notification contract”
- “đã xóa sạch tất cả dấu vết SMS từ code, config, và contract”

Đây là lỗi tài liệu tổng kết, không hẳn là lỗi code, nhưng cần sửa để tránh hiểu nhầm tiến độ.

---

## 4. Mức độ ưu tiên xử lý

## 4.1. Ưu tiên cao

- sửa `KET_QUA.md` cho đúng thực trạng
- rà và chuyển caller trong `user-service` sang `NotificationRequestEvent`
- phân biệt rõ public contract và internal/downstream contract

## 4.2. Ưu tiên trung bình

- quyết định có cần thêm `NotificationPublisher` interface trong `notification-contract` hay cập nhật lại tài liệu
- làm rõ package `client` nếu muốn giữ trong structure

## 4.3. Ưu tiên thấp nhưng nên dọn

- xóa SMS env còn sót trong `.env.properties`
- xóa SMS env còn sót trong `.env_docker.properties`
- dọn i18n/doc còn nhắc SMS nếu thật sự không còn dùng nữa

---

## 5. Checklist cần xử lý tiếp

- [ ] sửa `KET_QUA.md` để không kết luận quá mức
- [ ] xác nhận Phase 2 của `BUOC1` chưa hoàn tất
- [ ] rà `NotificationPublisher` trong `user-service`
- [ ] lên kế hoạch chuyển caller sang `NotificationRequestEvent`
- [ ] xác định event nào trong `common` còn phải giữ tạm
- [ ] đánh dấu deprecated rõ cho contract cũ nếu chưa xóa ngay
- [ ] quyết định có thêm `NotificationPublisher` interface trong `notification-contract` hay sửa tài liệu
- [ ] dọn nốt SMS config còn sót trong env files
- [ ] dọn nốt tài liệu/i18n còn nhắc SMS nếu scope thật sự đã bỏ

---

## 6. Kết luận

`BUOC1` hiện tại ở trạng thái:

- nền contract đã có
- dependency đã nối đúng
- nhưng migration caller và dọn boundary cũ chưa xong

Việc đúng lúc này không phải sang bước mới, mà là:

- sửa tài liệu tổng kết
- xử lý dứt điểm các thiếu sót ở trên
- sau đó mới coi `BUOC1` hoàn tất thật sự
