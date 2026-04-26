# DB_FILE.md

## 1. Mục tiêu

Tài liệu này mô tả phần mở rộng cho `file-service` để:

- Giữ nguyên các phần upload, get URL, delete đã có.
- Bổ sung lưu metadata file/folder/zip/... đã upload lên MinIO vào DB.
- Hỗ trợ tra cứu theo `name` để trả ra `url` qua HTTP REST.
- Hỗ trợ truyền `folder` để load danh sách bên trong folder qua HTTP REST.
- Hỗ trợ search theo `path_1`, `path_2`, `path_3`, ... hoặc kết hợp nhiều cấp, và trả kết quả dạng tree trong `ApiResponse.result`.

Schema phải bám theo `.env.properties` hiện tại:

```properties
FILE_SCHEMA=INF_FILE
```

## 2. Phần đang có, giữ nguyên

Hiện tại service đã có:

- Upload file lên MinIO.
- Sinh file name mới.
- Get presigned URL theo `category + fileName`.
- Delete file theo `category + fileName`.

Phần mở rộng dưới đây là additive, không thay thế flow đang chạy.

## 3. Vấn đề cần xử lý thêm

MinIO đang giữ object thật, nhưng DB chưa giữ metadata để:

- Tìm file theo tên đã lưu.
- Tìm theo folder.
- Search theo từng cấp thư mục.
- Trả cấu trúc cây.

Ngoài ra, `url` presigned của MinIO có thời gian hết hạn. Vì vậy:

- Vẫn có thể lưu `url` vào DB theo yêu cầu.
- Nhưng khi đọc ra nên ưu tiên regenerate URL mới từ `object_path`.
- Cột `url` nên xem như snapshot gần nhất, không nên coi là giá trị vĩnh viễn.

## 4. Thiết kế dữ liệu đề xuất

### 4.1. Bảng chính

Dùng một bảng chính để lưu mọi node:

- file
- folder
- zip
- object khác trong MinIO

Tên bảng đề xuất:

`INF_FILE.FILE_RESOURCE`

### 4.2. Ý nghĩa các cột chính

- `id`: khóa chính.
- `resource_type`: loại tài nguyên.
  Không hardcode chỉ `FILE`, `FOLDER`, `ZIP`.
  Danh sách loại hợp lệ phải đọc từ `properties` để còn mở rộng thêm về sau.
- `name`: tên node hiện tại.
  Ví dụ: `20260426_013347_8cae9c69.jpg`
- `url`: link truy cập trực tiếp/presigned URL snapshot.
- `object_path`: full path trong MinIO.
  Ví dụ: `infinite-world/avatar/20260426_013347_8cae9c69.jpg`
- `path_1`, `path_2`, `path_3`, ...: từng cấp đường dẫn.
  Ví dụ:
  - `path_1 = infinite-world`
  - `path_2 = avatar`
  - `name = 20260426_013347_8cae9c69.jpg`
- `path_depth`: số cấp thư mục trước `name`.
- `parent_path`: path của folder cha trực tiếp.
  Ví dụ: `infinite-world/avatar`
- `bucket_name`: bucket MinIO.
- `content_type`, `file_size`: metadata file.
- `extension`: đuôi file.
- `created_by`, `updated_by`, `created_at`, `updated_at`: audit.

Các cột không cần trong phạm vi hiện tại:

- `etag`
- `version_id`
- `status`

Lý do:

- Bài toán hiện tại load thẳng hoặc xóa thẳng theo MinIO/object path.
- Chưa cần giữ thêm metadata versioning của object.
- Chưa cần soft delete ở tầng DB trong giai đoạn này.

### 4.3. Vì sao chọn `path_1..path_10`

Yêu cầu của bài toán là filter theo cấp 1, cấp 2, cấp 3... và có thể filter nhiều cấp cùng lúc.  
Để query nhanh và đơn giản ở REST/JPA/SQL, nên có cột riêng cho từng cấp.

Giới hạn đề xuất:

- `path_1` đến `path_10`

Lý do:

- Đủ cho đa số cấu trúc thư mục nghiệp vụ.
- Query/index dễ hơn so với parse string liên tục.
- Vẫn còn `object_path` để giữ full path gốc.

Nếu sau này cần sâu hơn 10 cấp:

- Mở rộng thêm `path_11`, `path_12`, ...
- Hoặc tách thêm bảng con path segments.

## 5. Quy ước map path

Ví dụ object:

```text
infinite-world/avatar/20260426_013347_8cae9c69.jpg
```

Map DB:

- `bucket_name = infinite-world`
- `object_path = infinite-world/avatar/20260426_013347_8cae9c69.jpg`
- `path_1 = infinite-world`
- `path_2 = avatar`
- `name = 20260426_013347_8cae9c69.jpg`
- `parent_path = infinite-world/avatar`
- `path_depth = 2`

Ví dụ object sâu hơn:

```text
infinite-world/project/a/b/report.zip
```

Map DB:

- `path_1 = infinite-world`
- `path_2 = project`
- `path_3 = a`
- `path_4 = b`
- `name = report.zip`
- `parent_path = infinite-world/project/a/b`
- `path_depth = 4`

## 6. API REST cần bổ sung

### 6.1. Get URL theo tên

Mục tiêu:

- Truyền `name`
- Tìm bản ghi trong DB
- Trả URL truy cập

Đề xuất endpoint:

```http
GET /v1/api/files/by-name/{name}/url
```

Lưu ý:

- `name` có thể trùng giữa nhiều folder.
- Vì vậy nên hỗ trợ thêm filter tùy chọn:
  - `path1`
  - `path2`
  - `path3`
  - ...

Đề xuất an toàn hơn:

```http
GET /v1/api/files/url?name=20260426_013347_8cae9c69.jpg&path1=infinite-world&path2=avatar
```

### 6.2. Get list theo tên folder

Mục tiêu:

- Truyền tên folder hoặc full parent path.
- Trả list object nằm trực tiếp bên trong folder đó.

Đề xuất endpoint:

```http
GET /v1/api/files/folder/children?folderName=avatar
```

Khuyến nghị tốt hơn:

```http
GET /v1/api/files/folder/children?parentPath=infinite-world/avatar
```

Lý do:

- `folderName` có thể trùng ở nhiều nơi.
- `parentPath` là duy nhất và query rõ ràng hơn.

Nếu vẫn dùng input là `folderName` thì phải chấp nhận khả năng có nhiều folder trùng tên.
Khi đó API không trả một folder duy nhất, mà trả danh sách các folder match và danh sách con tương ứng.

Ví dụ:

```http
GET /v1/api/files/folder/children?folderName=avatar
```

Có thể match:

- `infinite-world/avatar`
- `infinite-world/user/avatar`
- `infinite-world/tmp/avatar`

Khi đó response nên là list:

```json
{
  "code": 1000,
  "message": "Success",
  "result": [
    {
      "folderName": "avatar",
      "folderPath": "infinite-world/avatar",
      "children": []
    },
    {
      "folderName": "avatar",
      "folderPath": "infinite-world/user/avatar",
      "children": []
    }
  ]
}
```

Khuyến nghị nghiệp vụ:

- Nếu client biết full path thì dùng `parentPath`.
- Nếu client chỉ biết tên folder thì API trả `list`, không tự chọn 1 kết quả duy nhất.

### 6.3. Search tree theo nhiều cấp

Mục tiêu:

- Filter theo `path_1`
- Filter theo `path_2`
- Filter theo `path_3`
- ...
- Có thể truyền một cấp hoặc nhiều cấp cùng lúc
- Trả tree trong `ApiResponse.result`

Đề xuất endpoint:

```http
GET /v1/api/files/tree/search?path1=infinite-world&path2=avatar
```

Hoặc:

```http
POST /v1/api/files/tree/search
```

Body đề xuất:

```json
{
  "path1": "infinite-world",
  "path2": "avatar",
  "path3": null,
  "name": null,
  "resourceType": null,
  "status": "ACTIVE"
}
```

## 7. Response tree đề xuất

Do `ApiResponse` đã có `result`, nên nên trả tree tại `result`.

Ví dụ:

```json
{
  "code": 1000,
  "message": "Success",
  "result": [
    {
      "name": "infinite-world",
      "type": "FOLDER",
      "path": "infinite-world",
      "url": null,
      "children": [
        {
          "name": "avatar",
          "type": "FOLDER",
          "path": "infinite-world/avatar",
          "url": null,
          "children": [
            {
              "name": "20260426_013347_8cae9c69.jpg",
              "type": "FILE",
              "path": "infinite-world/avatar/20260426_013347_8cae9c69.jpg",
              "url": "http://localhost:9000/..."
            }
          ]
        }
      ]
    }
  ]
}
```

## 8. Cách làm theo từng bước

### Bước 1. Tạo DB schema và table

- Tạo schema `INF_FILE` nếu chưa có.
- Tạo bảng `FILE_RESOURCE`.
- Tạo unique constraint cho `object_path`.
- Tạo index cho `name`, `parent_path`, `path_1..path_10`.
- Không thêm dữ liệu insert mẫu trong tài liệu triển khai.

### Bước 2. Mở rộng flow upload

Sau khi upload MinIO thành công:

1. Sinh `object_path`.
2. Tách path thành segments.
3. Gán vào `path_1..path_10`.
4. Lưu 1 record vào `FILE_RESOURCE`.
5. Nếu folder cha chưa có record mà muốn quản lý tree đầy đủ:
   - upsert folder node cho từng cấp cha.

### Bước 3. Mở rộng flow delete

Khi xóa object khỏi MinIO:

- Không nên xóa cứng ngay record DB.
- Nên update `status = DELETED`.
- Có thể thêm job cleanup vật lý sau.

### Bước 4. API get URL theo tên

- Query theo `name`.
- Nếu có thêm `path_1..path_n` thì áp các điều kiện đó.
- Nếu ra nhiều bản ghi:
  - trả lỗi dữ liệu không đủ điều kiện định danh
  - hoặc bắt buộc client truyền thêm path filter
- Khi đọc bản ghi:
  - regenerate presigned URL từ `object_path`
  - update lại cột `url` nếu cần

### Bước 5. API list theo folder

- Input tốt nhất: `parent_path`
- Query:
  - `parent_path = ?`
- Sort:
  - folder trước
  - file sau
  - rồi theo `name`

Nếu input là `folderName`:

- Query tất cả folder có `name = folderName`
- Với mỗi folder lấy list con trực tiếp của folder đó
- Trả `result` là danh sách folder match, không ép về một record

### Bước 6. API search tree

- Build query động từ các tham số `path_1..path_n`.
- Lấy toàn bộ record match.
- Convert flat list sang tree ở service layer.

Rule build tree:

- Nếu record là folder thì node có `children`.
- Nếu record là file thì leaf node.
- Node key tốt nhất là `object_path`.

## 9. SQL script

File script đi kèm:

- [INF_FILE_FILE_RESOURCE.sql](/d:/Infinite_World/BE/file-service/db/INF_FILE_FILE_RESOURCE.sql)

Script này gồm:

- Tạo schema `INF_FILE`
- Tạo bảng `FILE_RESOURCE`
- Tạo constraint/index
- Query mẫu cho:
  - get url theo name
  - list theo folder
  - search theo nhiều cấp

Lưu ý cập nhật theo yêu cầu hiện tại:

- Không cần dữ liệu `insert` mẫu.
- Không khóa cứng `resource_type` bằng 3 giá trị cố định.
- Danh sách `resource_type` hợp lệ phải do `properties` quản lý ở tầng ứng dụng.

## 10. Lưu ý nghiệp vụ quan trọng

### 10.1. `url` không bền

Presigned URL có expiry, nên:

- DB vẫn lưu `url` theo yêu cầu.
- Nhưng logic trả ra API nên sinh lại URL mới từ `object_path`.

### 10.2. `name` không unique toàn hệ thống

Không nên coi `name` là duy nhất.  
Định danh đúng nên là:

- `object_path`

Nếu API vẫn muốn tìm theo `name`, nên cho phép filter thêm theo `path_1..path_n`.

### 10.3. Folder name có thể trùng nhau

Không nên giả định `folderName` là duy nhất.

Với API `Get list theo tên folder`:

- Nếu nhận `folderName`, response phải là `list`
- Mỗi phần tử trong list phải có `folderPath` để client phân biệt
- Không tự lấy bản ghi đầu tiên

### 10.4. Folder trên MinIO có thể là logical folder

MinIO/S3 thường không bắt buộc folder object vật lý.  
Vì vậy trong DB có 2 hướng:

- Chỉ lưu file, folder suy ra từ path.
- Hoặc lưu thêm folder node để build tree nhanh hơn.

Khuyến nghị hiện tại:

- Lưu cả folder node bằng cơ chế upsert khi upload thành công.

### 10.5. Message response phải dùng i18n của common

Tất cả response message ở REST sau này phải đi qua cơ chế chung:

- dùng `Response.message("message.key")`
- key đặt trong i18n properties của module `common`

Không hardcode:

- `"Success"`
- `"Folder found"`
- `"File url retrieved"`

Ví dụ hướng dùng sau này:

```java
message("file.folder.children.success")
message("file.url.by.name.success")
message("file.tree.search.success")
```

Ở giai đoạn hiện tại chỉ cần chốt yêu cầu tài liệu, chưa cần code.

## 11. Kết luận

Hướng triển khai phù hợp nhất cho bài toán hiện tại là:

- Giữ MinIO làm nơi lưu object.
- Dùng `INF_FILE.FILE_RESOURCE` để lưu metadata và hỗ trợ search.
- Dùng `object_path` làm định danh chuẩn.
- Dùng `path_1..path_10` để filter nhanh theo từng cấp.
- Trả tree ở `ApiResponse.result`.
- Dùng `resource_type` theo cấu hình `properties`, không khóa cứng 3 loại.
- Với folder trùng tên, API phải trả `list`.
- Response message phải dùng `Response.message(...)` và key trong i18n `common`.

Chưa cần code ngay. Bước tiếp theo hợp lý là:

1. chốt cấu trúc bảng
2. chốt contract REST mới
3. sau đó mới code entity/repository/service/controller
