# User Service

Cap nhat: 2026-05-17

## Tong quan

`user-service` la service quan ly user/account cua Infinite World. Service nay phu trach dang nhap, dang ky, quen mat khau, doi mat khau, quan ly user, role, avatar, khoa/mo khoa tai khoan, auto unlock va phat yeu cau notification lien quan den user.

Day la service nguon su that cho user profile co ban trong schema `INF_USER`.

## Trach nhiem chinh

- Authentication:
  - login bang username/password
  - tao JWT
  - doi mat khau
  - forgot password OTP
  - verify forgot password OTP
- Registration:
  - tao user moi
  - upload avatar neu co
  - gui email xac thuc/duyet dang ky
  - verify registration token
  - verify email/password reset token
- User management:
  - search user
  - tao user boi admin
  - cap nhat user
  - reset password user ve default
  - upload avatar
  - lock/unlock user
  - auto unlock user het han khoa
- Role management:
  - CRUD role
  - search role
  - assign/remove role cho user
- Integration:
  - file-service qua gRPC de upload/delete avatar
  - notification-service qua Kafka `notification.request.v1`
  - Redis cho ha tang cache/session neu can

## Khong phu trach

- Khong gui email truc tiep.
- Khong push websocket truc tiep.
- Khong luu inbox notification.
- Khong quan ly file binary truc tiep; file thuoc file-service/MinIO.
- Khong xu ly client inbox; client inbox thuoc notification-service.

## Runtime

Port local:

```text
8081
```

Spring app name:

```properties
spring.application.name=user-service
```

Profile mac dinh:

```properties
SPRING_PROFILES_ACTIVE=dev,kafka
```

Kafka profile la bat buoc neu muon cac flow notification hoat dong.

## Cau truc module

```text
user-service/src/main/java/com/infinite/user
|-- controller/rest
|   |-- AuthController.java
|   |-- UserController.java
|   `-- RoleController.java
|-- service
|   |-- UserService.java
|   |-- RoleService.java
|   |-- NotificationPublisher.java
|   `-- impl
|-- repository
|   |-- UserRepository.java
|   `-- RoleRepository.java
|-- model
|   |-- User.java
|   `-- Role.java
|-- dto
|   |-- request
|   `-- response
|-- client
|   |-- FileClient.java
|   `-- impl
|-- config
|-- scheduler
|-- util
`-- UserServiceApplication.java
```

## API hien co

Base URL:

```text
http://localhost:8081
```

## Chuc nang va cach test bang API

### 1. Dang nhap

Muc dich:

- Kiem tra username/password.
- Kiem tra trang thai account.
- Tao JWT co claim `userId`.
- Phat login alert notification neu login thanh cong.

Curl:

```bash
curl --location 'http://localhost:8081/v1/api/auth/login' \
  --header 'Content-Type: application/json' \
  --header 'Accept-Language: vi' \
  --data-raw '{
    "username": "user@example.com",
    "password": "Password123!",
    "ipAddress": "127.0.0.1",
    "device": "Postman"
  }'
```

Sau khi login thanh cong, notification-service se nhan request co:

```text
sourceService = user-service
sourceAction  = send_login_alert_email
```

### 2. Dang ky user

Muc dich:

- Tao user moi.
- Upload avatar neu request co file.
- Tao user inactive/cho verify theo flow hien tai.
- Gui email verification/approval qua notification-service.

Curl khong avatar:

```bash
curl --location 'http://localhost:8081/v1/api/auth/register' \
  --header 'Accept-Language: vi' \
  --form 'request={"username":"newuser@example.com","password":"Password123!","name":"New User","email":"newuser@example.com","phoneNumber":"0900000000"}'
```

Curl co avatar:

```bash
curl --location 'http://localhost:8081/v1/api/auth/register' \
  --header 'Accept-Language: vi' \
  --form 'request={"username":"newuser@example.com","password":"Password123!","name":"New User","email":"newuser@example.com","phoneNumber":"0900000000"}' \
  --form 'avatar=@avatar.jpg'
```

### 3. Verify registration

Muc dich:

- Xu ly link approve/reject trong email dang ky.
- Set locale tu query param `lang`.
- Neu approve thi active user theo flow service.

Curl:

```bash
curl --location 'http://localhost:8081/v1/api/auth/verify-registration?token=TOKEN_HERE&action=approve&lang=vi'
```

Reject:

```bash
curl --location 'http://localhost:8081/v1/api/auth/verify-registration?token=TOKEN_HERE&action=reject&lang=vi'
```

### 4. Forgot password OTP

Muc dich:

- Tao OTP reset password.
- Response i18n theo `Accept-Language`.
- Gui OTP email qua notification-service.

Curl:

```bash
curl --location 'http://localhost:8081/v1/api/auth/forgot-password' \
  --header 'Content-Type: application/json' \
  --header 'Accept-Language: vi' \
  --data-raw '{"email":"user@example.com"}'
```

Notification expected:

```text
sourceAction = send_otp_email
content.locale = vi
templateVars.emailType = FORGOT_PASSWORD_OTP
```

### 5. Verify forgot password OTP

Muc dich:

- Xac thuc OTP.
- Cap nhat password moi.

Curl:

```bash
curl --location 'http://localhost:8081/v1/api/auth/forgot-password/verify' \
  --header 'Content-Type: application/json' \
  --header 'Accept-Language: vi' \
  --data-raw '{
    "email": "user@example.com",
    "otp": "123456",
    "newPassword": "NewPassword123!"
  }'
```

### 6. Search user

Muc dich:

- Tim user theo keyword/status.
- Tra ve page response tu service.

Curl:

```bash
curl --location 'http://localhost:8081/v1/api/user/search?page=0&size=20' \
  --header 'Content-Type: application/json' \
  --data-raw '{
    "searchKey": "user",
    "active": null
  }'
```

### 7. Tao user boi admin

Muc dich:

- Admin tao user.
- Co the upload avatar.
- Co the gan role/status theo request.

Curl:

```bash
curl --location 'http://localhost:8081/v1/api/user/create' \
  --header 'Accept-Language: vi' \
  --form 'request={"username":"admincreated@example.com","password":"Password123!","name":"Admin Created","email":"admincreated@example.com","phoneNumber":"0900000001","nguoithuchien":"admin","roleIds":[1],"active":1}'
```

### 8. Cap nhat user

Muc dich:

- Cap nhat thong tin user.
- Neu active status thay doi, co the kich hoat flow email verification/password reset theo logic service.
- Co the upload avatar moi.

Curl:

```bash
curl --location 'http://localhost:8081/v1/api/user/update' \
  --header 'Accept-Language: vi' \
  --form 'request={"id":1,"name":"Updated Name","email":"user@example.com","phoneNumber":"0900000002","nguoithuchien":"admin","active":1}'
```

### 9. Upload avatar

Muc dich:

- Upload file qua gRPC toi file-service.
- Luu `relativeUrl` vao `USERS.IMAGE_URL`.
- Xoa avatar cu async neu co.

Curl:

```bash
curl --location 'http://localhost:8081/v1/api/user/upload-avatar/1' \
  --form 'file=@avatar.jpg'
```

### 10. Lock user

Muc dich:

- Khoa user tam thoi hoac vinh vien.
- Set `ACTIVE` sang locked status.
- Set `LOCK_TIME` neu khoa tam thoi.
- Gui email status change qua notification-service.

Curl khoa tam thoi:

```bash
curl --location 'http://localhost:8081/v1/api/user/lock' \
  --header 'Content-Type: application/json' \
  --header 'Accept-Language: vi' \
  --data-raw '{
    "userId": 1,
    "lockTime": "2026-05-18T00:00:00",
    "nguoithuchien": "admin"
  }'
```

Curl khoa vinh vien:

```bash
curl --location 'http://localhost:8081/v1/api/user/lock' \
  --header 'Content-Type: application/json' \
  --header 'Accept-Language: vi' \
  --data-raw '{
    "userId": 1,
    "lockTime": null,
    "nguoithuchien": "admin"
  }'
```

### 11. Unlock user

Muc dich:

- Mo khoa user.
- Gui email unlock qua notification-service.

Curl:

```bash
curl --location --request POST 'http://localhost:8081/v1/api/user/unlock/1?nguoithuchien=admin' \
  --header 'Accept-Language: vi'
```

### 12. Role CRUD va gan role

Tao role:

```bash
curl --location 'http://localhost:8081/v1/api/roles' \
  --header 'Content-Type: application/json' \
  --data-raw '{
    "name": "ADMIN",
    "description": "Administrator"
  }'
```

Update role:

```bash
curl --location --request PUT 'http://localhost:8081/v1/api/roles/1' \
  --header 'Content-Type: application/json' \
  --data-raw '{
    "name": "ADMIN",
    "description": "Administrator updated"
  }'
```

Search role:

```bash
curl --location 'http://localhost:8081/v1/api/roles/search?page=0&size=20' \
  --header 'Content-Type: application/json' \
  --data-raw '{
    "searchKey": "ADMIN"
  }'
```

Assign role:

```bash
curl --location --request POST 'http://localhost:8081/v1/api/roles/assign/1/1'
```

Remove role:

```bash
curl --location --request DELETE 'http://localhost:8081/v1/api/roles/remove/1/1'
```

Delete role:

```bash
curl --location --request DELETE 'http://localhost:8081/v1/api/roles/1'
```

### AuthController

Base path:

```text
/v1/api/auth
```

Endpoint:

```text
POST /login
POST /register
GET  /verify-registration
GET  /verify-email
GET  /get-token
POST /change-password
POST /forgot-password
POST /forgot-password/verify
```

Forgot password:

```bash
curl --location 'http://localhost:8081/v1/api/auth/forgot-password' \
  --header 'Content-Type: application/json' \
  --header 'Accept-Language: vi' \
  --data-raw '{"email":"user@example.com"}'
```

Login:

```bash
curl --location 'http://localhost:8081/v1/api/auth/login' \
  --header 'Content-Type: application/json' \
  --header 'Accept-Language: vi' \
  --data-raw '{
    "username": "user@example.com",
    "password": "password",
    "ipAddress": "127.0.0.1",
    "device": "Postman"
  }'
```

Register multipart:

```bash
curl --location 'http://localhost:8081/v1/api/auth/register' \
  --header 'Accept-Language: vi' \
  --form 'request={"username":"user@example.com","password":"Password123!","name":"Test User","email":"user@example.com","phoneNumber":"0900000000"}' \
  --form 'avatar=@avatar.jpg'
```

### UserController

Base path:

```text
/v1/api/user
```

Endpoint:

```text
POST /search
POST /create
POST /update
POST /change-password
POST /reset-password/{userId}
POST /upload-avatar/{userId}
POST /lock
POST /unlock/{userId}?nguoithuchien={admin}
```

Search user:

```bash
curl --location 'http://localhost:8081/v1/api/user/search?page=0&size=20' \
  --header 'Content-Type: application/json' \
  --data-raw '{
    "searchKey": "",
    "active": null
  }'
```

Lock user:

```bash
curl --location 'http://localhost:8081/v1/api/user/lock' \
  --header 'Content-Type: application/json' \
  --header 'Accept-Language: vi' \
  --data-raw '{
    "userId": 1,
    "lockTime": "2026-05-18T00:00:00",
    "nguoithuchien": "admin"
  }'
```

Unlock user:

```bash
curl --location --request POST 'http://localhost:8081/v1/api/user/unlock/1?nguoithuchien=admin' \
  --header 'Accept-Language: vi'
```

### RoleController

Base path:

```text
/v1/api/roles
```

Endpoint:

```text
POST   /
PUT    /{id}
DELETE /{id}
GET    /{id}
POST   /search
POST   /assign/{userId}/{roleId}
DELETE /remove/{userId}/{roleId}
```

RoleController hien van wrap `ResponseEntity<ApiResponse<Object>>`, khac voi notification controllers.

## Database

Schema:

```properties
USER_SCHEMA=INF_USER
```

Datasource dung chung DB:

```properties
DB_URL=jdbc:postgresql://localhost:5432/infinite_world
DB_USERNAME=infinite
DB_PASSWORD=infinite@123
```

JPA:

```properties
spring.jpa.hibernate.ddl-auto=none
spring.jpa.open-in-view=false
```

Entity chinh:

- `User`
- `Role`

Bang quan trong:

- `USERS`
- `roles`
- `user_roles`

### Bang `USERS`

Entity: `com.infinite.user.model.User`

| Column | Java field | Type gan dung | Nullable | Ghi chu |
| --- | --- | --- | --- | --- |
| `ID` | `id` | `BIGINT` identity | No | Primary key |
| `USERNAME` | `username` | `VARCHAR(50)` | No | Unique, validate email regex trong code |
| `PASSWORD` | `password` | `VARCHAR(60)` | No | BCrypt hash, bi `@JsonIgnore` |
| `NAME` | `name` | `VARCHAR(50)` | Yes | Ten hien thi |
| `EMAIL` | `email` | `VARCHAR(254)` | Yes | Unique, validate email |
| `PHONE_NUMBER` | `phoneNumber` | `VARCHAR(20)` | Yes | So dien thoai |
| `IMAGE_URL` | `imageUrl` | `VARCHAR(1000)` | Yes | Relative URL/avatar path tu file-service |
| `ACTIVE` | `active` | `INTEGER` | No | Trang thai account theo `Contant.IS_ACTIVE` |
| `LOCK_TIME` | `lockTime` | `TIMESTAMP` | Yes | Han khoa tam thoi; null co the la khoa vinh vien tuy flow |
| `IS_DELETE` | `isDelete` | `BOOLEAN` | No | Soft delete flag |
| `CREATE_BY` | `createBy` | `VARCHAR(50)` | Yes | Audit nguoi tao |
| `CREATED_TIME` | `createdTime` | `TIMESTAMP` | Yes | Set boi Spring Data auditing |
| `MODIFIED_BY` | `modifiedBy` | `VARCHAR(50)` | Yes | Audit nguoi sua |
| `MODIFIED_TIME` | `modifiedTime` | `TIMESTAMP` | Yes | Set boi Spring Data auditing |

Quan he:

- Many-to-many voi `roles` qua bang join `user_roles`.
- Fetch role dang la `EAGER`.

### Bang `roles`

Entity: `com.infinite.user.model.Role`

| Column | Java field | Type gan dung | Nullable | Ghi chu |
| --- | --- | --- | --- | --- |
| `id` | `id` | `BIGINT` identity | No | Primary key |
| `name` | `name` | `VARCHAR` | No | Unique role name |
| `description` | `description` | `VARCHAR/TEXT` | Yes | Mo ta role |
| `created_at` | `createdAt` | `TIMESTAMP` | Yes | Set trong `@PrePersist` |
| `updated_at` | `updatedAt` | `TIMESTAMP` | Yes | Set trong `@PrePersist/@PreUpdate` |
| `created_by` | `createdBy` | `VARCHAR` | Yes | Audit nguoi tao |
| `updated_by` | `updatedBy` | `VARCHAR` | Yes | Audit nguoi sua |

Quan he:

- Many-to-many mapped by `User.roles`.

### Bang `user_roles`

Bang join do mapping `@JoinTable` tao/ky vong.

| Column | Type gan dung | Nullable | Ghi chu |
| --- | --- | --- | --- |
| `user_id` | `BIGINT` | No | FK toi `USERS.ID` |
| `role_id` | `BIGINT` | No | FK toi `roles.id` |

Ghi chu:

- Code hien khong khai bao entity rieng cho `user_roles`.
- Neu can them metadata gan role, vi du `assigned_by`, `assigned_at`, nen tao entity join rieng thay vi dung `@ManyToMany` truc tiep.

### Trang thai `ACTIVE`

`ACTIVE` la integer status trong `Contant.IS_ACTIVE`. Cac flow dang dung:

- inactive/unverified account sau khi tao.
- active account sau khi verify/approve.
- locked account khi admin khoa hoac scheduler xu ly unlock.

Khi mo rong, nen tra cuu `Contant.IS_ACTIVE` thay vi hard-code so trong controller/service moi.

## Security va JWT

JWT duoc tao trong `JwtUtil`.

Config:

```properties
SECRET_KEY=infinite_world_super_secure_key_2026_dev
JWT_EXPIRATION_MS=86400000
```

JWT co claim `userId`. Gateway doc JWT va set trusted headers cho downstream, trong do notification client API dung:

```text
X-USER-ID
```

## File/avatar integration

User-service khong luu file truc tiep. Avatar di qua `FileClient`.

Flow:

```text
UserServiceImpl
  -> FileClient
  -> GrpcFileClientImpl
  -> grpc-common
  -> file-service
  -> MinIO
```

Config lien quan:

```properties
FILE_SERVICE=http://localhost:8083
GRPC_FILE_SERVICE_ADDRESS=static://localhost:18083
MINIO_ENDPOINT=http://localhost:9000
MINIO_BUCKET_NAME=infinite-world
file.upload.max-size=52428800
```

Khi upload avatar moi, service cap nhat `User.imageUrl` va xoa avatar cu bat dong bo neu co.

## gRPC integration

User-service hien dang la gRPC client cua file-service. Service khong expose gRPC business API rieng cho user, nhung van co config `grpc.server.port` theo infra chung.

### Module lien quan

```text
grpc-common
|-- src/main/proto/file_service.proto
|-- src/main/resources/grpc-client.yml
|-- com.infinite.grpc.client.file.FileGrpcClient
|-- com.infinite.grpc.exception.GrpcClientException
|-- com.infinite.grpc.interceptor.*
`-- com.infinite.grpc.service.file.*

user-service
|-- config/FileClientConfig.java
|-- client/FileClient.java
`-- client/impl/GrpcFileClientImpl.java
```

### File gRPC contract

Proto:

```text
grpc-common/src/main/proto/file_service.proto
```

Service:

```protobuf
service FileServiceRpc {
  rpc UploadFile(UploadFileRequest) returns (UploadFileResponse);
  rpc GetFileUrl(GetFileUrlRequest) returns (GetFileUrlResponse);
  rpc DeleteFile(DeleteFileRequest) returns (DeleteFileResponse);
  rpc ListFiles(ListFilesRequest) returns (ListFilesResponse);
  rpc GetFileInfo(GetFileInfoRequest) returns (GetFileInfoResponse);
}
```

User-service dang dung cac operation:

- `UploadFile`: upload avatar.
- `DeleteFile`: xoa avatar cu bat dong bo.
- `GetFileUrl`: lay URL file neu can.

`ListFiles` va `GetFileInfo` co trong proto/client wrapper, nhung user-service chua dung trong flow hien tai.

### Client wiring

`FileClientConfig` tao bean:

```text
FileClient -> GrpcFileClientImpl -> FileGrpcClient -> FileServiceRpcBlockingStub
```

`FileGrpcClient` trong `grpc-common` dung:

```java
@GrpcClient("file-service")
private FileServiceRpcGrpc.FileServiceRpcBlockingStub stub;
```

Ten `"file-service"` phai khop config trong `grpc-client.yml`.

### gRPC client config

`grpc-common/src/main/resources/grpc-client.yml` duoc auto-load khi import `grpc-common`.

Config lien quan:

```yaml
grpc:
  client:
    file-service:
      address: ${GRPC_FILE_SERVICE_ADDRESS:static://localhost:18083}
      negotiation-type: plaintext
      enable-keep-alive: true
      keep-alive-time: 30s
      keep-alive-timeout: 5s
      max-inbound-message-size: 104857600
```

Env local/docker:

```properties
GRPC_FILE_SERVICE_ADDRESS=static://localhost:18083
GRPC_USER_SERVICE_PORT=18081
```

Trong docker:

```properties
GRPC_FILE_SERVICE_ADDRESS=static://inf-file:18083
```

### Error handling

`FileGrpcClient` nem `GrpcClientException` khi file-service tra business code khac success hoac gRPC transport loi.

`GrpcFileClientImpl` map exception ve `ApiResponse<FileUploadResponse>` de `UserServiceImpl` xu ly nhu mot client adapter binh thuong.

### Context/locale propagation

`grpc-common` co cac interceptor dung chung:

- `GrpcContextOutboundInterceptor`
- `GrpcContextInboundInterceptor`
- `GrpcLocaleInterceptor`
- `GrpcExceptionTranslator`

Khi mo rong gRPC moi, nen dung lai cac interceptor/metadata key trong `grpc-common` thay vi tu tao convention rieng.

### Nguyen tac mo rong gRPC cho user-service

- Neu user-service can goi service khac qua gRPC, them proto/wrapper client vao `grpc-common` truoc.
- Business code trong user-service chi phu thuoc interface noi bo, vi du `FileClient`, khong dung protobuf stub truc tiep trong `UserServiceImpl`.
- Ten `@GrpcClient(...)` phai khop `grpc-client.yml`.
- Khong de logic business phu thuoc `StreamObserver`/protobuf generated class.
- Neu user-service sau nay expose gRPC API cho service khac, tao interface service rieng trong `grpc-common` va implementation adapter, khong expose truc tiep repository/entity.

## Notification integration

`NotificationPublisher` la adapter notification cua user-service.

Business flow user-service goi cac method nhu:

- `sendOtpEmail`
- `sendAccountVerificationEmail`
- `sendPasswordResetVerificationEmail`
- `sendLoginAlertEmail`
- `sendUserStatusChangeNotification`
- `sendWebSocketToUser`

Ben trong, publisher build `NotificationRequestEvent` tu `notification-contract` va publish vao:

```text
notification.request.v1
```

Config:

```yaml
notification:
  topics:
    request: ${TOPIC_NOTIFICATION_REQUEST:notification.request.v1}
```

Quan trong:

- User-service khong publish truc tiep `EmailNotificationEvent`.
- User-service khong publish truc tiep `WebSocketNotificationEvent`.
- Email nguoi nhan duoc dat trong `target.queryParams.emailByUserId`.
- Locale duoc lay tu `Accept-Language`, dua vao `NotificationContent.locale`.

Locale flow:

```text
Accept-Language
  -> NotificationPublisher.currentLocale()
  -> NotificationContent.locale
  -> notification-service
  -> EmailNotificationEvent.locale
```

### Kafka cua user-service

User-service hien chi publish notification request moi qua Kafka topic public:

```text
notification.request.v1
```

Producer config:

```yaml
spring:
  kafka:
    producer:
      acks: all
      retries: 3
      properties:
        enable.idempotence: true
        max.in.flight.requests.per.connection: 5
```

Topic config:

```yaml
notification:
  topics:
    request: ${TOPIC_NOTIFICATION_REQUEST:notification.request.v1}
```

Cac business action publish Kafka:

| Flow | Method | `sourceAction` |
| --- | --- | --- |
| Forgot password OTP | `sendOtpEmail` | `send_otp_email` |
| Registration OTP | `sendOtpEmail` | `send_otp_email` |
| Login OTP | `sendOtpEmail` | `send_otp_email` |
| Account verification | `sendAccountVerificationEmail` | `send_account_verification_email` |
| Password reset verification | `sendPasswordResetVerificationEmail` | `send_password_reset_verification_email` |
| Login alert | `sendLoginAlertEmail` | `send_login_alert_email` |
| Lock/unlock/update user | `sendUserStatusChangeNotification` | `send_user_status_email` |
| Welcome/realtime user message | `sendWebSocketToUser` | `send_realtime_to_user` |

Sau khi publish, notification-service se xu ly tiep:

```text
notification.request.v1
  -> notification_request
  -> delivery job/batch
  -> inbox/email/realtime
```

### Cach kiem tra Kafka publish

Sau khi goi API user-service, kiem tra DB notification:

```sql
SELECT id, source_service, source_action, request_payload
FROM INF_NOTI.notification_request
WHERE source_service = 'user-service'
ORDER BY id DESC
LIMIT 10;
```

Neu can consume truc tiep Kafka, consume topic:

```text
notification.request.v1
```

## Scheduler

`UserUnlockScheduler` chay moi ngay luc 00:00:

```text
0 0 0 * * ?
```

Nhiem vu:

- tim user dang `LOCKED`
- `lockTime <= now`
- mo khoa user
- gui notification auto unlock

### Background jobs cua user-service

| Job/background work | Class | Trigger | Viec lam |
| --- | --- | --- | --- |
| Auto unlock user | `UserUnlockScheduler` | Cron `0 0 0 * * ?` | Mo khoa user co `LOCK_TIME <= now`, gui notification auto unlock |
| Upload file async callback | `GrpcFileClientImpl.uploadFileAsync` | `@Async` khi caller dung async upload | Upload file qua gRPC va callback ket qua |
| Delete old avatar async | `GrpcFileClientImpl.deleteFileAsync` | Sau khi avatar moi duoc luu | Xoa avatar cu qua gRPC, loi chi log |

Luu y:

- Scheduler can `@EnableScheduling` trong app/config lien quan.
- Async task can `@EnableAsync`/async config.
- Job auto unlock chi xu ly user co lock time; khoa vinh vien `lockTime = null` se khong auto unlock.

## I18n

Response message dung common i18n va `Accept-Language`.

Vi du:

```http
Accept-Language: vi
```

Forgot password co the tra response tieng Viet, dong thoi notification/email cung nhan locale `vi`.

## External dependencies

- PostgreSQL
- Redis
- Kafka
- file-service
- notification-service
- MinIO thong qua file-service
- common
- grpc-common
- notification-contract

## Build va run

Build rieng user-service:

```bash
mvn -pl user-service -am -DskipTests clean compile
```

Build cung notification:

```bash
mvn -pl user-service,notification -am -DskipTests clean compile
```

Run local can dam bao:

- PostgreSQL dang chay
- Redis dang chay
- Kafka dang chay
- file-service dang chay neu test avatar
- notification-service dang chay neu test email/realtime notification

## Test nhanh service

1. Start infra va service lien quan.
2. Goi forgot password:

```bash
curl --location 'http://localhost:8081/v1/api/auth/forgot-password' \
  --header 'Content-Type: application/json' \
  --header 'Accept-Language: vi' \
  --data-raw '{"email":"user@example.com"}'
```

3. Kiem tra user-service response dung ngon ngu.
4. Kiem tra notification-service co request tu user-service:

```sql
SELECT id, source_service, source_action, request_payload
FROM INF_NOTI.notification_request
ORDER BY id DESC
LIMIT 5;
```

5. Kiem tra email log:

```sql
SELECT id, to_email, status, payload
FROM INF_NOTI.email_delivery_log
ORDER BY id DESC
LIMIT 5;
```

## Luu y khi sua code

- Neu them flow user moi can gui email/realtime, them helper vao `NotificationPublisher` hoac dung `publishNotificationRequest`.
- Neu flow can email template, dat `emailType` va bien template vao `content.templateVars`.
- Neu flow can locale, giu `Accept-Language` tu request.
- Neu can query user theo role/segment cho notification, khong lam trong user-service bang cach publish event cu; can thiet ke target resolver/read model cho notification-service.
