# File Service

`file-service` phu trach upload file len MinIO, quan ly metadata file va folder trong database, va cung cap REST + gRPC cho cac thao tac file.

## Tong quan

Service hien co 2 nhom chuc nang:

- Upload, get URL, delete file tren MinIO
- Luu va tra cuu metadata theo `object_path`, `name`, `parent_path`, `path_1 .. path_10`
- Auto-create parent folder nodes neu chua ton tai
- Luu `url` dang relative path khong co host/IP, va ghep host luc response
- Luu `is_url_expirable = false` cho file va folder nodes trong implementation hien tai
- Encode san `url` truoc khi luu DB de ho tro ten file co dau, khoang trang, ngoac, ky tu dac biet
- Tra ve `relativeUrl` da encode cho service khac tai su dung

Metadata duoc luu trong bang:

- `INF_FILE.FILE_RESOURCE`

Script tao bang:

- `file-service/db/INF_FILE_FILE_RESOURCE.sql`

## Kien truc chinh

### 1. REST Controllers

- `FileController`
  - upload file
  - get presigned URL theo `category/fileName`
  - delete file theo `category/fileName`

- `FileMetadataController`
  - get URL theo `name` + path filters
  - load danh sach children theo folder
  - search tree theo `path_1 .. path_10`

### 2. Services

- `FileService`
  - xu ly upload/delete/get URL MinIO
  - tich hop luu/xoa metadata

- `FileResourceService`
  - luu metadata file
  - xoa metadata
  - tra cuu URL theo ten
  - load children theo folder
  - search tree

### 3. Database Layer

- `FileResource`
  - entity map toi `FILE_RESOURCE`
  - `url` luu dang relative URL, vi du `/infinite-world/avatar/file.jpg`
  - `isUrlExpirable` danh dau URL co han hay khong; hien tai luon la `false`

- `FileResourceRepository`
  - query theo `objectPath`
  - query theo `name`
  - query theo `parentPath`
  - dynamic query theo `path1 .. path10`

### 4. Utility

- `PathParser`
  - tach `object_path` thanh `bucket_name`, `parent_path`, `path_1 .. path_10`, `name`

- `TreeBuilder`
  - build tree tu flat list metadata

- `FileNameResolver`
  - resolve ten trung theo hau to `(1)`, `(2)`, ...
  - duoc goi truoc khi upload len MinIO de dam bao object key va metadata luon dong bo

### 5. Configuration

- `FileConfig`
  - MinIO config
  - file size limits
  - allowed file extensions

- `ResourceTypeConfig`
  - map extension -> `resource_type`
  - doc tu `application.properties`

### 6. Common Utility

- `FileUrlBuilder` trong module `common`
  - encode path segments
  - normalize relative path
  - build full URL tu `baseUrl + storedRelativeUrl`
  - extract file name tu stored relative path

## Object Path va Metadata

Quy uoc `object_path`:

```text
bucket/category/fileName
```

Vi du:

```text
infinite-world/avatar/my-avatar.jpg
```

Map metadata:

- `bucket_name = infinite-world`
- `parent_path = infinite-world/avatar`
- `path_1 = infinite-world`
- `path_2 = avatar`
- `name = my-avatar.jpg`

Map URL metadata:

- `url = /infinite-world/avatar/my-avatar.jpg`
- `is_url_expirable = false`

Neu ten file co ky tu dac biet, `url` luu trong DB se la dang encoded san.

Vi du:

- `name = tải xuống (3).jpg`
- `url = /infinite-world/avatar/t%E1%BA%A3i%20xu%E1%BB%91ng%20%283%29.jpg`

Nguyen tac:

- `name` dung de hien thi va search van giu raw value
- `url` dung de tra full URL va chia se lien service se la dang encoded relative path

## Transaction va limitation

### Upload

Flow:

1. resolve ten file unique theo `parent_path`
2. upload object len MinIO voi ten da resolve
3. tao `object_path` raw cho metadata noi bo
4. encode va normalize `url` truoc khi luu DB
5. save metadata vao DB
6. neu save metadata fail thi xoa lai object tren MinIO

Upload co compensation logic de giam lech giua MinIO va DB.

Luu y:

- upload hien tai uu tien giu ten goc cua file
- neu trung ten trong cung `parent_path`, service tu doi thanh `name(1).ext`, `name(2).ext`, ...
- 1 upload luon tao 1 record metadata cho file
- neu parent folders chua ton tai, he thong se auto-create them folder nodes
- parent folders moi cung duoc set `is_url_expirable = false` de tranh vi pham `NOT NULL`
- neu parent folders da ton tai, service co the partial update lai metadata folder do neu phat hien field khac
- vi vay upload dau tien cua 1 path moi co the tao nhieu records, nhung cac lan sau se khong tao lap lai folder da co
- response upload hien tai tra:
  - `fileUrl`: full URL cho client
  - `relativeUrl`: encoded relative URL cho service khac luu DB

### Delete

Flow:

1. tim metadata trong DB theo `object_path`
2. lay object key tu `object_path`
3. xoa object trong MinIO
4. xoa metadata trong DB

Delete dung hard delete.

Limitation:

- neu MinIO delete thanh cong nhung DB delete fail, co the de lai orphan metadata
- limitation nay da duoc chap nhan o giai doan hien tai

## Trang thai implementation

Da duoc implement:

- upload metadata voi compensation logic neu save DB fail
- resolve duplicate file name truoc khi upload MinIO
- auto-create parent folder nodes
- partial update folder nodes da ton tai neu metadata path khac di
- get URL theo `name` + `path1 .. path10`
- get folder children theo `parentPath` hoac `folderName`
- search tree va build full URL luc response
- luu `url` dang relative path khong co host/IP
- luu `is_url_expirable = false` cho file va folder nodes
- luu `url` dang encoded relative path de ho tro ten file dac biet
- tra `relativeUrl` encoded cho service khac tai su dung
- dong nhat logic URL qua `FileUrlBuilder` trong module `common`

Accepted limitation:

- delete la hard delete theo thu tu MinIO truoc, DB sau
- neu buoc xoa DB fail sau khi object da bi xoa khoi MinIO thi co the con orphan metadata

Future enhancement chua implement:

- retry/eventual consistency cho delete
- soft delete thay vi hard delete

## Resource Types

Resource type duoc doc dong tu config:

```properties
file.resource.types.image=jpg,jpeg,png,gif,bmp,webp,svg,ico
file.resource.types.video=mp4,avi,mov,wmv,flv,mkv,webm
file.resource.types.document=pdf,doc,docx,xls,xlsx,ppt,pptx,txt,csv
file.resource.types.archive=zip,rar,7z
```

Mac dinh:

- khong match config thi la `FILE`
- `FOLDER` duoc dung cho parent folder nodes auto-create

## REST APIs

Base URL mac dinh:

```text
http://localhost:8083
```

### 1. Upload file

API:

```text
POST /v1/api/files/upload
```

Muc dich:

- upload 1 file len MinIO
- luu metadata file vao DB
- tra ve thong tin file da upload

Request params:

- `file`: bat buoc
- `category`: bat buoc theo nghiep vu, neu khong truyen thi mac dinh `avatar`
- `userId`: tuy chon

Giai thich cac truong trong `curl` upload:

- `--form 'file=@"/path/to/avatar.jpg"'`: file thuc te can upload; dau `@` bao cho `curl` biet day la duong dan file tren may
- `--form 'category="avatar"'`: nhom luu tru nghiep vu; anh huong toi object key, validation loai file va gioi han dung luong
- `--form 'userId="1"'`: ma nguoi thuc hien; duoc dung de set `createdBy`/`updatedBy` neu co
- `avatar.jpg` trong duong dan local la ten file goc; he thong se giu ten nay neu khong trung, hoac them hau to duplicate neu can

#### 1.1. Upload avatar

```bash
curl --location 'http://localhost:8083/v1/api/files/upload' \
  --form 'file=@"/path/to/avatar.jpg"' \
  --form 'category="avatar"' \
  --form 'userId="1"'
```

#### 1.2. Upload image

```bash
curl --location 'http://localhost:8083/v1/api/files/upload' \
  --form 'file=@"/path/to/image.png"' \
  --form 'category="images"' \
  --form 'userId="1"'
```

#### 1.3. Upload video

```bash
curl --location 'http://localhost:8083/v1/api/files/upload' \
  --form 'file=@"/path/to/video.mp4"' \
  --form 'category="videos"' \
  --form 'userId="1"'
```

#### 1.4. Upload document

```bash
curl --location 'http://localhost:8083/v1/api/files/upload' \
  --form 'file=@"/path/to/document.pdf"' \
  --form 'category="documents"' \
  --form 'userId="1"'
```

#### 1.5. Upload archive

```bash
curl --location 'http://localhost:8083/v1/api/files/upload' \
  --form 'file=@"/path/to/archive.zip"' \
  --form 'category="archives"' \
  --form 'userId="1"'
```

#### 1.6. Upload attachment category khac

```bash
curl --location 'http://localhost:8083/v1/api/files/upload' \
  --form 'file=@"/path/to/file.txt"' \
  --form 'category="attachments"' \
  --form 'userId="1"'
```

#### 1.7. Folder upload

Hien tai **khong co API upload folder rieng**.

Folder nodes duoc tao gian tiep khi upload file, neu path cha chua ton tai trong DB.

Vi du upload:

```text
infinite-world/avatar/user123/profile.jpg
```

co the tao:

- `infinite-world` - `FOLDER`
- `infinite-world/avatar` - `FOLDER`
- `infinite-world/avatar/user123` - `FOLDER`
- `infinite-world/avatar/user123/profile.jpg` - `IMAGE`/`FILE`

#### 1.8. Response mau

```json
{
  "code": 1000,
  "message": "File uploaded successfully",
  "result": {
    "fileName": "my-avatar.jpg",
    "originalFileName": "my-avatar.jpg",
    "fileUrl": "http://localhost:9000/...",
    "relativeUrl": "/infinite-world/avatar/my-avatar.jpg",
    "fileType": "image/jpeg",
    "fileSize": 102400,
    "category": "avatar"
  }
}
```

#### 1.9. Luu y ve ten file trung

- API upload hien tai giu ten goc neu ten do chua ton tai trong cung `parent_path`
- neu da ton tai, service resolve thanh `file(1).ext`, `file(2).ext`, ...
- ten da resolve duoc dung cho ca MinIO object key va `object_path` trong DB
- gioi han an toan hien tai la toi da 1000 ban trung ten cho cung mot `parent_path`

#### 1.10. Luu y ve metadata URL

- DB khong luu host/IP trong truong `url`
- DB luu `url` dang encoded relative path, vi du `/infinite-world/avatar/t%E1%BA%A3i%20xu%E1%BB%91ng%20%283%29.jpg`
- response tra ve full URL bang cach ghep `file.public.base-url` + `url`
- `is_url_expirable` hien tai luon la `false`
- `relativeUrl` trong upload response la gia tri encoded relative path de service khac luu lai
- `name` va `object_path` van giu raw value de phuc vu metadata/search nghiep vu

### 2. Get file URL theo category + fileName

API:

```text
GET /v1/api/files/{category}/{fileName}
```

#### 2.1. Vi du avatar

```bash
curl --location 'http://localhost:8083/v1/api/files/avatar/20260426_013347_8cae9c69.jpg'
```

#### 2.2. Vi du image

```bash
curl --location 'http://localhost:8083/v1/api/files/images/20260426_013347_8cae9c69.png'
```

Response: tra ve chuoi URL presigned.

Giai thich cac truong trong `curl`:

- `{category}`: thu muc nghiep vu trong bucket, vi du `avatar`, `images`, `documents`
- `{fileName}`: ten file da luu tren he thong, co the la ten goc hoac ten da duoc resolve thanh `file(1).ext`

Luu y:

- backend se encode path dung theo URL rule khi build ket qua
- ten file co dau/space/ngoac van duoc ho tro

### 3. Delete file theo category + fileName

API:

```text
DELETE /v1/api/files/{category}/{fileName}
```

#### 3.1. Vi du avatar

```bash
curl --location --request DELETE 'http://localhost:8083/v1/api/files/avatar/20260426_013347_8cae9c69.jpg'
```

#### 3.2. Vi du document

```bash
curl --location --request DELETE 'http://localhost:8083/v1/api/files/documents/20260426_013347_8cae9c69.pdf'
```

Giai thich cac truong trong `curl`:

- `{category}`: thu muc nghiep vu cua file can xoa
- `{fileName}`: ten file chinh xac da luu trong MinIO va metadata DB

### 4. Get URL theo name + path filters

API:

```text
GET /v1/api/files/url
```

#### 4.1. Theo `name + path1 + path2`

```bash
curl --location 'http://localhost:8083/v1/api/files/url?name=20260426_013347_8cae9c69.jpg&path1=infinite-world&path2=avatar'
```

#### 4.2. Theo nhieu cap path

```bash
curl --location 'http://localhost:8083/v1/api/files/url?name=report.zip&path1=infinite-world&path2=project&path3=a&path4=b'
```

Co the truyen toi da:

- `path1`
- `path2`
- `path3`
- `path4`
- `path5`
- `path6`
- `path7`
- `path8`
- `path9`
- `path10`

Neu trung ten va chua du filter, API tra loi yeu cau bo sung path filters.

Giai thich cac truong trong `curl`:

- `name`: ten file can tim
- `path1 .. path10`: tung cap cua duong dan metadata; dung de thu hep ket qua khi co nhieu file trung ten
- thong thuong `path1` la bucket, `path2` la category

Response mau:

```json
{
  "code": 1000,
  "message": "File URL retrieved successfully",
  "result": "http://localhost:9000/..."
}
```

Luu y:

- API nay search bang `name` raw trong DB
- URL tra ve cho client la full URL build tu `url` da duoc encode san trong DB

### 5. Get folder children theo `parentPath`

API:

```text
GET /v1/api/files/folder/children
```

#### 5.1. Theo `parentPath`

```bash
curl --location 'http://localhost:8083/v1/api/files/folder/children?parentPath=infinite-world/avatar'
```

Response mau:

```json
{
  "code": 1000,
  "message": "Folder children retrieved successfully",
  "result": [
    {
      "folderName": "avatar",
      "folderPath": "infinite-world/avatar",
      "children": [
        {
          "id": 1,
          "resourceType": "IMAGE",
          "name": "20260426_013347_8cae9c69.jpg",
          "url": "http://localhost:9000/...",
          "objectPath": "infinite-world/avatar/20260426_013347_8cae9c69.jpg",
          "parentPath": "infinite-world/avatar",
          "fileSize": 102400,
          "contentType": "image/jpeg",
          "extension": "jpg"
        }
      ]
    }
  ]
}
```

Luu y:

- day la cach dung on dinh nhat voi code hien tai
- API nay query truc tiep theo `parent_path`
- do folder nodes duoc auto-create, API nay co the tra ca folder va file con ben trong
- neu child la file co ten dac biet, field `url` trong response van la full URL hop le da encode dung

Giai thich cac truong trong `curl`:

- `parentPath`: duong dan folder cha can xem noi dung, vi du `infinite-world/avatar`

### 6. Get folder children theo `folderName`

#### 6.1. Theo ten folder co the trung nhau

```bash
curl --location 'http://localhost:8083/v1/api/files/folder/children?folderName=avatar'
```

Luu y quan trong:

- code hien tai tim `folderName` tren cac record co `resourceType = FOLDER`
- vi folder nodes duoc auto-create, API nay hoat dong tot hon trong mo hinh hien tai
- neu co nhieu folder trung ten, API van tra list cac folder match

Giai thich cac truong trong `curl`:

- `folderName`: ten folder can tim; khong phai khoa duy nhat, co the tra ve nhieu ket qua

### 7. Search tree

API:

```text
POST /v1/api/files/tree/search
```

#### 7.1. Search theo `path1 + path2`

```bash
curl --location 'http://localhost:8083/v1/api/files/tree/search' \
  --header 'Content-Type: application/json' \
  --data '{
    "path1": "infinite-world",
    "path2": "avatar",
    "path3": null,
    "path4": null,
    "path5": null,
    "path6": null,
    "path7": null,
    "path8": null,
    "path9": null,
    "path10": null,
    "name": null,
    "resourceType": null
  }'
```

#### 7.2. Search theo `resourceType`

```bash
curl --location 'http://localhost:8083/v1/api/files/tree/search' \
  --header 'Content-Type: application/json' \
  --data '{
    "path1": "infinite-world",
    "resourceType": "IMAGE"
  }'
```

Response tra ve tree trong `ApiResponse.result`.

Giai thich cac truong trong JSON body:

- `path1 .. path10`: bo loc theo tung cap duong dan
- `name`: loc theo ten file/folder
- `resourceType`: loc theo loai, vi du `FOLDER`, `IMAGE`, `VIDEO`, `DOCUMENT`, `FILE`
- cac truong khong can loc co the bo trong hoac de `null`

Luu y quan trong:

- `TreeBuilder` hien tai build tree tu cac node dang ton tai trong danh sach query
- vi he thong dang auto-create folder nodes, ket qua tree co day du hon cho cac cap thu muc trung gian
- de tree hien thi day du, can dam bao folder nodes da duoc tao trong cac lan upload truoc do
- URL trong ket qua tree duoc build tu stored relative URL da encode san

## gRPC

Service van ho tro gRPC thong qua:

- `grpc-common/src/main/proto/file_service.proto`
- `FileServiceGrpc`
- `FileServiceImpl`

gRPC hien tai chu yeu phuc vu:

- upload file
- get file URL
- delete file

Luu y lien service:

- upload response co `relativeUrl`
- service khac nen luu `relativeUrl` thay vi luu full `fileUrl`
- khi can tra ra client, service khac chi can ghep host qua utility dung chung

## Cau hinh chinh

### MinIO

```properties
minio.endpoint=${MINIO_ENDPOINT:http://localhost:9000}
minio.access-key=${MINIO_ACCESS_KEY:minioadmin}
minio.secret-key=${MINIO_SECRET_KEY:minioadmin}
minio.bucket-name=${MINIO_BUCKET_NAME:infinite-world}
file.public.base-url=${FILE_PUBLIC_BASE_URL:http://localhost:9000}
```

### Database

```properties
spring.datasource.url=${DB_URL:jdbc:postgresql://localhost:5432/infinite_world}
spring.datasource.username=${DB_USERNAME:infinite}
spring.datasource.password=${DB_PASSWORD:infinite@123}
spring.jpa.properties.hibernate.default_schema=${FILE_SCHEMA:INF_FILE}
```

### File limits

```properties
file.upload.max-size.images=10485760
file.upload.max-size.avatar=5242880
file.upload.max-size.videos=104857600
file.upload.max-size.documents=20971520
file.upload.max-size.archives=52428800
file.upload.max-size.default=52428800
```

## Build

```bash
mvn -q -pl file-service -DskipTests compile
```

Neu build ca luong dung chung URL:

```bash
mvn -q -pl common,file-service,user-service -DskipTests compile
```

## Ghi chu

- Response message phai di qua `Response.message(...)` va i18n cua `common`
- `folderName` khong duoc coi la duy nhat
- `name` khong duoc coi la duy nhat
- `object_path` la dinh danh metadata chuan
- `url` la encoded relative path, khong luu host/IP
- `is_url_expirable` hien tai luon la `false` cho ca file va folder nodes
- `relativeUrl` la gia tri encoded relative path de service khac luu lai
- `FileUrlBuilder` trong `common` la utility dung chung cho viec build full URL
- `parentPath` la cach query folder chinh xac nhat
- README nay la tai lieu tong hop chinh cho implementation hien tai cua `file-service`
