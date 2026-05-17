# Review HD notification

Khong con finding blocker sau khi trien khai BUOC1, BUOC2, BUOC3 va phase tich hop `user-service`.

Da xac nhan:

- `CreateNotificationRequestUseCase` da publish fanout event, khong con dung o TODO.
- Co `NotificationDeliveryJobWorker` va `NotificationDeliveryBatchWorker`.
- Inbox insert dung JDBC batch + `ON CONFLICT DO NOTHING`.
- Realtime chi dispatch sau inbox insert.
- Email delivery co `EmailDeliveryLog`, retry va audit API.
- Unread count co Redis cache va fallback DB.
- Co metrics/log co ban va cleanup scheduler.
- Co DLQ topic co ban cho request payload loi, delivery job terminal fail, delivery batch terminal fail.
- `user-service` da publish `NotificationRequestEvent` vao `notification.request.v1`, khong publish truc tiep email/websocket event cu tu business flow.
- Build thanh cong voi `mvn -pl user-service,notification,gateway -am -DskipTests clean compile`.

Residual risk khong chan hoan thanh HD:

- Fanout hien ho tro chac `USER_IDS`; `ROLE`, `SEGMENT`, `ALL_USERS` can read-side target source rieng truoc khi bat delivery that.
- Email channel voi `USER_IDS` can caller cung cap `emailByUserId` trong target payload. `user-service` da cung cap map nay khi tao email notification request.
- Cac downstream event cu van deprecated nhung duoc giu lam adapter noi bo/backward compatibility.
- Dashboard kich thuoc that la phan van hanh infra, code hien co metrics/log/retry/DLQ co ban de gan dashboard sau.
