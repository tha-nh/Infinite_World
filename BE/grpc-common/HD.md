# User-Service Goi File-Service Bang gRPC

## 1. Muc tieu

`user-service` hien co chuc nang upload avatar, nhung dang goi `file-service` qua REST (`RestTemplate`).

Muc tieu cua buoc nay la:

- doi phan giao tiep `user-service -> file-service` tu REST sang gRPC,
- giu thay doi o `user-service` o muc nho nhat,
- uu tien tan dung nhung gi da co trong `grpc-common`,
- khong doi business flow cua `UserServiceImpl`.

## 2. Hien trang trong repo

### 2.1. User-service dang dung REST client

Luong hien tai:

- interface: `user-service/src/main/java/com/infinite/user/client/FileClient.java`
- implementation: `user-service/src/main/java/com/infinite/user/client/impl/FileClientImpl.java`
- config bean: `user-service/src/main/java/com/infinite/user/config/FileClientConfig.java`
- noi su dung:
  - `create(...)`
  - `update(...)`
  - `uploadAvatar(...)`
  trong `user-service/src/main/java/com/infinite/user/service/impl/UserServiceImpl.java`

### 2.2. grpc-common da co san phan dung duoc

Da co cac thanh phan co the tan dung:

- `file_service.proto`
- server adapter cho `file-service`
- `FileGrpcClient`
- `grpc-client.yml`
- auto-config load config gRPC chung

Noi cach khac, phan "server gRPC cua file-service" va "wrapper client de goi file-service" da co khung co ban.

## 3. Cach lam dung cho repo nay

Huong nen lam la:

1. giu interface `FileClient` trong `user-service`,
2. thay implementation REST bang implementation gRPC,
3. de `UserServiceImpl` tiep tuc goi `fileClient.uploadFile(...)` nhu hien tai.

Ly do:

- thay doi nho, it anh huong code business,
- khong phai sua nhieu cho cac flow `create user`, `update user`, `upload avatar`,
- sau nay neu can doi lai cach goi thi chi sua o adapter client.

## 4. Ke hoach trien khai

### Buoc 1. Them `grpc-common` dependency vao `user-service`

Them dependency:

```xml
<dependency>
    <groupId>com.infinite</groupId>
    <artifactId>grpc-common</artifactId>
    <version>${project.version}</version>
</dependency>
```

Muc dich:

- lay proto/generated classes,
- lay `FileGrpcClient`,
- lay auto-config gRPC client dung chung.

### Buoc 2. Cho phep `user-service` nhin thay bean trong `com.infinite.grpc`

Voi current setup, `user-service` chi scan:

```java
@SpringBootApplication(scanBasePackages = {"com.infinite.user", "com.infinite.common"})
```

Neu muon inject truc tiep `FileGrpcClient` tu `grpc-common`, can scan them:

```java
@SpringBootApplication(scanBasePackages = {
        "com.infinite.user",
        "com.infinite.common",
        "com.infinite.grpc"
})
```

Trong context hien tai, cach nay chap nhan duoc va la cach nhanh nhat.

### Buoc 3. Tao implementation gRPC cho `FileClient`

Khong sua interface `FileClient`.

Tao class moi, vi du:

- `user-service/src/main/java/com/infinite/user/client/impl/GrpcFileClientImpl.java`

Class nay se:

- inject `FileGrpcClient`,
- convert `MultipartFile` -> `byte[]`,
- goi `fileGrpcClient.uploadFile(...)`,
- convert ket qua ve `ApiResponse<FileUploadResponse>` de khop contract hien tai cua `user-service`.

Huong mapping:

- `FileInfo.fileUrl` -> `FileUploadResponse.fileUrl`
- `FileInfo.fileName` -> `FileUploadResponse.fileName`
- `FileInfo.originalFileName` -> `FileUploadResponse.originalFileName`
- `FileInfo.fileSize` -> `FileUploadResponse.fileSize`

Neu upload that bai:

- khong hardcode message,
- dung message key / `Response.message(...)` theo chuan chung cua repo.

### Buoc 4. Thay bean config REST hien tai

File hien tai:

- `user-service/src/main/java/com/infinite/user/config/FileClientConfig.java`

Hien file nay tao:

- `RestTemplate`
- `new FileClientImpl(...)`

Khi chuyen sang gRPC, can doi thanh bean `FileClient` moi dung `GrpcFileClientImpl`.

Neu da chuyen hoan toan sang gRPC thi:

- bo `RestTemplate` rieng cho file-service,
- bo `file.service.url`,
- khong dung `FileClientImpl` nua.

### Buoc 5. Dung config gRPC thay vi REST URL

Thay vi:

```properties
file.service.url=http://localhost:8083
```

su dung config gRPC client:

```properties
grpc.client.file-service.address=static://localhost:${GRPC_FILE_SERVICE_PORT:9093}
grpc.client.file-service.negotiation-type=plaintext
```

Neu `grpc-common` da auto-load `grpc-client.yml` thi `user-service` chi can override khi can.

### Buoc 6. Giu nguyen `UserServiceImpl`

`UserServiceImpl` hien dang goi qua abstraction:

- `fileClient.uploadFile(...)`
- `fileClient.deleteFileAsync(...)`

Day la diem tot. Muc tieu la giu nguyen cac doan nay, chi thay implementation ben duoi.

Neu lam dung, business flow sau se khong can sua nhieu:

- tao user co avatar
- update user co avatar moi
- upload avatar rieng
- xoa avatar cu bat dong bo

## 5. Khuyen nghi ve thiet ke

### 5.1. Khong de `UserServiceImpl` goi protobuf truc tiep

Khong nen viet kieu:

```java
@GrpcClient("file-service")
private FileServiceRpcGrpc.FileServiceRpcBlockingStub stub;
```

ngay trong service business cua `user-service`.

Nen de no di qua 1 adapter:

- `FileClient` la business-facing abstraction
- `GrpcFileClientImpl` la integration adapter
- `FileGrpcClient` trong `grpc-common` la wrapper level thap hon

Nhu vay `user-service` khong bi dinh chat vao protobuf.

### 5.2. Giu async delete o tang `user-service`

`deleteFileAsync(...)` hien dang hop ly cho avatar cu.

Nen tiep tuc de `@Async` o adapter cua `user-service`, khong can day xuong `grpc-common`.

Ly do:

- day la quyet dinh business flow cua `user-service`,
- `grpc-common` chi nen lo giao tiep, khong lo orchestration cua use case.

### 5.3. Upload thi nen de sync

Upload avatar phuc vu ngay cho viec cap nhat `imageUrl`, nen van nen de sync.

Flow hop ly:

1. upload file qua gRPC,
2. nhan `fileUrl`,
3. luu `imageUrl` vao user,
4. sau do moi xoa avatar cu bat dong bo.

## 6. Cac diem can sua truoc khi dung that

Day la cac diem can chot truoc khi `user-service` dung `FileGrpcClient` that:

### 6.1. Success code phai theo `StatusCode.SUCCESS`

Khong check success bang `200`.

Repo nay dang dung `StatusCode.SUCCESS.getCode()`.

Cho nen:

- `FileGrpcClient`
- `GrpcFileClientImpl`
- logic xu ly response trong `user-service`

phai dung cung 1 chuan code.

### 6.2. Message phai di qua key

Khi build `ApiResponse`, message nen di qua co che message key cua repo, khong hardcode string.

Huong dung:

- neu thanh cong: dung key thanh cong da co
- neu that bai: dung key loi phu hop
- neu map exception: lay message tu `AppException`/`GrpcClientException` theo chuan chung

Muc tieu la giu dong bo giua REST va gRPC.

### 6.3. File-service dang co mot so han che

Phan avatar chi can:

- upload file
- delete file
- lay file url

Nen co the dung duoc som.

Nhung can biet:

- `GetFileInfo` chua that su hoan chinh
- `ListFiles` chua xong
- `FileServiceImpl` van co cho bat het `Exception` roi doi thanh `INTERNAL_ERROR`

Cho nen migration avatar sang gRPC lam duoc, nhung khong nen coi file-service gRPC la da hoan thien toan bo.

## 7. Trinh tu sua code de an toan

Thu tu nen lam:

1. them dependency `grpc-common` vao `user-service`
2. mo scan package de thay `com.infinite.grpc`
3. tao `GrpcFileClientImpl`
4. doi bean `FileClient` sang implementation moi
5. giu nguyen `UserServiceImpl`
6. test create/update/upload avatar
7. sau khi on dinh moi xoa REST-based `FileClientImpl`

Khong nen xoa implementation cu ngay tu dau. Nen doi bean xong, test flow on roi moi don dep.

## 8. Test can chay

Can test toi thieu 4 case:

1. Tao user moi co avatar
   - file duoc upload
   - `imageUrl` duoc luu

2. Update user voi avatar moi
   - avatar moi upload thanh cong
   - `imageUrl` moi duoc luu
   - avatar cu duoc goi xoa async

3. Upload avatar bang endpoint rieng
   - response thanh cong
   - user duoc cap nhat `imageUrl`

4. File-service down / gRPC loi
   - khong lam crash flow ngoai y muon
   - log ro rang
   - message/response dung chuan cua repo

## 9. Muc tieu implementation cuoi cung

Trang thai nen dat toi:

- `user-service` khong con goi `file-service` qua REST cho avatar
- `UserServiceImpl` van giu abstraction `FileClient`
- phan gRPC duoc gom vao `grpc-common` + adapter `GrpcFileClientImpl`
- config endpoint file-service di theo `grpc.client.file-service.*`

## 10. Ket luan

Huong dung nhat cho repo nay la:

- khong cho business service goi protobuf truc tiep,
- giu `FileClient` lam abstraction o `user-service`,
- doi implementation tu REST sang gRPC,
- tan dung `FileGrpcClient` trong `grpc-common`,
- migrate theo tung buoc, khong big bang.

Neu can lam tiep, buoc sau la implement thuc te theo dung ke hoach nay trong `user-service`.
