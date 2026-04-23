# 📁 FILE SERVICE - Upload API Examples

Base URL: `http://localhost:8083`

---

## 🖼️ IMAGE FILES

### 1. Upload JPG/JPEG
```bash
curl --location 'http://localhost:8083/v1/api/files/upload' \
--header 'Authorization: Bearer YOUR_JWT_TOKEN' \
--form 'file=@"/path/to/image.jpg"' \
--form 'category="images"' \
--form 'userId="1"'
```

### 2. Upload PNG
```bash
curl --location 'http://localhost:8083/v1/api/files/upload' \
--header 'Authorization: Bearer YOUR_JWT_TOKEN' \
--form 'file=@"/path/to/image.png"' \
--form 'category="images"' \
--form 'userId="1"'
```

### 3. Upload GIF
```bash
curl --location 'http://localhost:8083/v1/api/files/upload' \
--header 'Authorization: Bearer YOUR_JWT_TOKEN' \
--form 'file=@"/path/to/animation.gif"' \
--form 'category="images"' \
--form 'userId="1"'
```

### 4. Upload BMP
```bash
curl --location 'http://localhost:8083/v1/api/files/upload' \
--header 'Authorization: Bearer YOUR_JWT_TOKEN' \
--form 'file=@"/path/to/image.bmp"' \
--form 'category="images"' \
--form 'userId="1"'
```

### 5. Upload WEBP
```bash
curl --location 'http://localhost:8083/v1/api/files/upload' \
--header 'Authorization: Bearer YOUR_JWT_TOKEN' \
--form 'file=@"/path/to/image.webp"' \
--form 'category="images"' \
--form 'userId="1"'
```

### 6. Upload SVG
```bash
curl --location 'http://localhost:8083/v1/api/files/upload' \
--header 'Authorization: Bearer YOUR_JWT_TOKEN' \
--form 'file=@"/path/to/icon.svg"' \
--form 'category="images"' \
--form 'userId="1"'
```

### 7. Upload ICO (Icon)
```bash
curl --location 'http://localhost:8083/v1/api/files/upload' \
--header 'Authorization: Bearer YOUR_JWT_TOKEN' \
--form 'file=@"/path/to/favicon.ico"' \
--form 'category="images"' \
--form 'userId="1"'
```

### 8. Upload Avatar
```bash
curl --location 'http://localhost:8083/v1/api/files/upload' \
--header 'Authorization: Bearer YOUR_JWT_TOKEN' \
--form 'file=@"/path/to/avatar.jpg"' \
--form 'category="avatar"' \
--form 'userId="1"'
```

---

## 🎥 VIDEO FILES

### 9. Upload MP4
```bash
curl --location 'http://localhost:8083/v1/api/files/upload' \
--header 'Authorization: Bearer YOUR_JWT_TOKEN' \
--form 'file=@"/path/to/video.mp4"' \
--form 'category="videos"' \
--form 'userId="1"'
```

### 10. Upload AVI
```bash
curl --location 'http://localhost:8083/v1/api/files/upload' \
--header 'Authorization: Bearer YOUR_JWT_TOKEN' \
--form 'file=@"/path/to/video.avi"' \
--form 'category="videos"' \
--form 'userId="1"'
```

### 11. Upload MOV
```bash
curl --location 'http://localhost:8083/v1/api/files/upload' \
--header 'Authorization: Bearer YOUR_JWT_TOKEN' \
--form 'file=@"/path/to/video.mov"' \
--form 'category="videos"' \
--form 'userId="1"'
```

### 12. Upload WMV
```bash
curl --location 'http://localhost:8083/v1/api/files/upload' \
--header 'Authorization: Bearer YOUR_JWT_TOKEN' \
--form 'file=@"/path/to/video.wmv"' \
--form 'category="videos"' \
--form 'userId="1"'
```

### 13. Upload FLV
```bash
curl --location 'http://localhost:8083/v1/api/files/upload' \
--header 'Authorization: Bearer YOUR_JWT_TOKEN' \
--form 'file=@"/path/to/video.flv"' \
--form 'category="videos"' \
--form 'userId="1"'
```

### 14. Upload MKV
```bash
curl --location 'http://localhost:8083/v1/api/files/upload' \
--header 'Authorization: Bearer YOUR_JWT_TOKEN' \
--form 'file=@"/path/to/video.mkv"' \
--form 'category="videos"' \
--form 'userId="1"'
```

### 15. Upload WEBM
```bash
curl --location 'http://localhost:8083/v1/api/files/upload' \
--header 'Authorization: Bearer YOUR_JWT_TOKEN' \
--form 'file=@"/path/to/video.webm"' \
--form 'category="videos"' \
--form 'userId="1"'
```

---

## 📄 DOCUMENT FILES

### 16. Upload PDF
```bash
curl --location 'http://localhost:8083/v1/api/files/upload' \
--header 'Authorization: Bearer YOUR_JWT_TOKEN' \
--form 'file=@"/path/to/document.pdf"' \
--form 'category="documents"' \
--form 'userId="1"'
```

### 17. Upload DOC
```bash
curl --location 'http://localhost:8083/v1/api/files/upload' \
--header 'Authorization: Bearer YOUR_JWT_TOKEN' \
--form 'file=@"/path/to/document.doc"' \
--form 'category="documents"' \
--form 'userId="1"'
```

### 18. Upload DOCX
```bash
curl --location 'http://localhost:8083/v1/api/files/upload' \
--header 'Authorization: Bearer YOUR_JWT_TOKEN' \
--form 'file=@"/path/to/document.docx"' \
--form 'category="documents"' \
--form 'userId="1"'
```

### 19. Upload XLS
```bash
curl --location 'http://localhost:8083/v1/api/files/upload' \
--header 'Authorization: Bearer YOUR_JWT_TOKEN' \
--form 'file=@"/path/to/spreadsheet.xls"' \
--form 'category="documents"' \
--form 'userId="1"'
```

### 20. Upload XLSX
```bash
curl --location 'http://localhost:8083/v1/api/files/upload' \
--header 'Authorization: Bearer YOUR_JWT_TOKEN' \
--form 'file=@"/path/to/spreadsheet.xlsx"' \
--form 'category="documents"' \
--form 'userId="1"'
```

### 21. Upload PPT
```bash
curl --location 'http://localhost:8083/v1/api/files/upload' \
--header 'Authorization: Bearer YOUR_JWT_TOKEN' \
--form 'file=@"/path/to/presentation.ppt"' \
--form 'category="documents"' \
--form 'userId="1"'
```

### 22. Upload PPTX
```bash
curl --location 'http://localhost:8083/v1/api/files/upload' \
--header 'Authorization: Bearer YOUR_JWT_TOKEN' \
--form 'file=@"/path/to/presentation.pptx"' \
--form 'category="documents"' \
--form 'userId="1"'
```

### 23. Upload TXT
```bash
curl --location 'http://localhost:8083/v1/api/files/upload' \
--header 'Authorization: Bearer YOUR_JWT_TOKEN' \
--form 'file=@"/path/to/notes.txt"' \
--form 'category="documents"' \
--form 'userId="1"'
```

### 24. Upload CSV
```bash
curl --location 'http://localhost:8083/v1/api/files/upload' \
--header 'Authorization: Bearer YOUR_JWT_TOKEN' \
--form 'file=@"/path/to/data.csv"' \
--form 'category="documents"' \
--form 'userId="1"'
```

---

## 📦 ARCHIVE FILES

### 25. Upload ZIP
```bash
curl --location 'http://localhost:8083/v1/api/files/upload' \
--header 'Authorization: Bearer YOUR_JWT_TOKEN' \
--form 'file=@"/path/to/archive.zip"' \
--form 'category="archives"' \
--form 'userId="1"'
```

### 26. Upload RAR
```bash
curl --location 'http://localhost:8083/v1/api/files/upload' \
--header 'Authorization: Bearer YOUR_JWT_TOKEN' \
--form 'file=@"/path/to/archive.rar"' \
--form 'category="archives"' \
--form 'userId="1"'
```

### 27. Upload 7Z
```bash
curl --location 'http://localhost:8083/v1/api/files/upload' \
--header 'Authorization: Bearer YOUR_JWT_TOKEN' \
--form 'file=@"/path/to/archive.7z"' \
--form 'category="archives"' \
--form 'userId="1"'
```

---

## 🗑️ DELETE FILE

### 28. Delete File by Path
```bash
curl --location --request DELETE 'http://localhost:8083/v1/api/files/images/20240423_123456_abc12345.jpg' \
--header 'Authorization: Bearer YOUR_JWT_TOKEN'
```

---

## 🔗 GET FILE URL

### 29. Get File URL
```bash
curl --location 'http://localhost:8083/v1/api/files/images/20240423_123456_abc12345.jpg' \
--header 'Authorization: Bearer YOUR_JWT_TOKEN'
```

---

## ⚠️ VALIDATION BY CATEGORY

API tự động validate file type dựa trên category:

### Category "images" hoặc "avatar" - Chỉ chấp nhận ảnh
```bash
# ✅ OK - Upload ảnh vào category images
curl --location 'http://localhost:8083/v1/api/files/upload' \
--header 'Authorization: Bearer YOUR_JWT_TOKEN' \
--form 'file=@"/path/to/image.jpg"' \
--form 'category="images"' \
--form 'userId="1"'

# ❌ ERROR - Upload video vào category images
curl --location 'http://localhost:8083/v1/api/files/upload' \
--header 'Authorization: Bearer YOUR_JWT_TOKEN' \
--form 'file=@"/path/to/video.mp4"' \
--form 'category="images"' \
--form 'userId="1"'

# Response (tiếng Việt):
{
    "code": 400,
    "message": "Chỉ chấp nhận file ảnh (jpg, jpeg, png, gif, bmp, webp, svg, ico)"
}

# Response (tiếng Anh):
{
    "code": 400,
    "message": "Only image files are allowed (jpg, jpeg, png, gif, bmp, webp, svg, ico)"
}
```

### Category "videos" - Chỉ chấp nhận video
```bash
# ✅ OK
curl --location 'http://localhost:8083/v1/api/files/upload' \
--header 'Authorization: Bearer YOUR_JWT_TOKEN' \
--form 'file=@"/path/to/video.mp4"' \
--form 'category="videos"' \
--form 'userId="1"'

# ❌ ERROR - Upload ảnh vào category videos
# Response: "Chỉ chấp nhận file video (mp4, avi, mov, wmv, flv, mkv, webm)"
```

### Category "documents" - Chỉ chấp nhận tài liệu
```bash
# ✅ OK
curl --location 'http://localhost:8083/v1/api/files/upload' \
--header 'Authorization: Bearer YOUR_JWT_TOKEN' \
--form 'file=@"/path/to/document.pdf"' \
--form 'category="documents"' \
--form 'userId="1"'

# ❌ ERROR - Upload ảnh vào category documents
# Response: "Chỉ chấp nhận file tài liệu (pdf, doc, docx, xls, xlsx, ppt, pptx, txt, csv)"
```

### Category "archives" - Chỉ chấp nhận file nén
```bash
# ✅ OK
curl --location 'http://localhost:8083/v1/api/files/upload' \
--header 'Authorization: Bearer YOUR_JWT_TOKEN' \
--form 'file=@"/path/to/archive.zip"' \
--form 'category="archives"' \
--form 'userId="1"'

# ❌ ERROR - Upload ảnh vào category archives
# Response: "Chỉ chấp nhận file nén (zip, rar, 7z)"
```

### Category khác - Không validate
```bash
# ✅ OK - Category "attachments" chấp nhận mọi file type trong allowed list
curl --location 'http://localhost:8083/v1/api/files/upload' \
--header 'Authorization: Bearer YOUR_JWT_TOKEN' \
--form 'file=@"/path/to/any-file.jpg"' \
--form 'category="attachments"' \
--form 'userId="1"'
```

---

## 📋 CATEGORIES

Các category được khuyến nghị:
- `avatar` - Ảnh đại diện user
- `images` - Ảnh thông thường
- `videos` - Video files
- `documents` - Tài liệu
- `archives` - File nén
- `attachments` - File đính kèm
- `temp` - File tạm thời

---

## 📝 NOTES

### Thay thế các giá trị:
- `YOUR_JWT_TOKEN`: Token nhận được từ API login
- `/path/to/file.ext`: Đường dẫn thực tế đến file
- `userId`: ID của user đang upload
- `category`: Loại file/thư mục lưu trữ

### File size limit:
- **Images:** 10MB
- **Avatar:** 5MB
- **Videos:** 100MB
- **Documents:** 20MB
- **Archives:** 50MB
- **Default (other categories):** 50MB

Có thể thay đổi trong `application.properties`:
```properties
file.upload.max-size.images=10485760      # 10MB
file.upload.max-size.avatar=5242880       # 5MB
file.upload.max-size.videos=104857600     # 100MB
file.upload.max-size.documents=20971520   # 20MB
file.upload.max-size.archives=52428800    # 50MB
file.upload.max-size.default=52428800     # 50MB
```

### Allowed file types:
```
Images:    jpg, jpeg, png, gif, bmp, webp, svg, ico
Videos:    mp4, avi, mov, wmv, flv, mkv, webm
Documents: pdf, doc, docx, xls, xlsx, ppt, pptx, txt, csv
Archives:  zip, rar, 7z
```

### Response format:
```json
{
    "code": 200,
    "message": "File uploaded successfully",
    "result": {
        "fileName": "20240423_123456_abc12345.jpg",
        "originalFileName": "my-photo.jpg",
        "fileUrl": "http://localhost:9000/infinite-world/images/20240423_123456_abc12345.jpg?...",
        "fileType": "image/jpeg",
        "fileSize": 1024000,
        "category": "images"
    }
}
```

### Error responses:
```json
{
    "code": 400,
    "message": "Invalid file type: .xyz. Allowed types: jpg, jpeg, png, ..."
}
```

```json
{
    "code": 400,
    "message": "File size exceeds maximum limit"
}
```

```json
{
    "code": 400,
    "message": "File is empty"
}
```

---

## 🧪 TESTING TIPS

1. **Test với Postman:**
   - Import các curl commands vào Postman
   - Tạo environment variable cho `BASE_URL` và `JWT_TOKEN`
   - Sử dụng Collection Runner để test hàng loạt

2. **Test file size limit:**
   ```bash
   # Upload file > 50MB sẽ bị reject
   curl --location 'http://localhost:8083/v1/api/files/upload' \
   --header 'Authorization: Bearer YOUR_JWT_TOKEN' \
   --form 'file=@"/path/to/large-file.mp4"' \
   --form 'category="videos"' \
   --form 'userId="1"'
   ```

3. **Test invalid file type:**
   ```bash
   # Upload .exe sẽ bị reject
   curl --location 'http://localhost:8083/v1/api/files/upload' \
   --header 'Authorization: Bearer YOUR_JWT_TOKEN' \
   --form 'file=@"/path/to/program.exe"' \
   --form 'category="files"' \
   --form 'userId="1"'
   ```

4. **Test without authentication:**
   ```bash
   # Không có token sẽ bị reject
   curl --location 'http://localhost:8083/v1/api/files/upload' \
   --form 'file=@"/path/to/image.jpg"' \
   --form 'category="images"' \
   --form 'userId="1"'
   ```

---

## 🔧 CONFIGURATION

### MinIO Configuration (.env.properties):
```properties
MINIO_ENDPOINT=http://localhost:9000
MINIO_ACCESS_KEY=minioadmin
MINIO_SECRET_KEY=minioadmin
MINIO_BUCKET_NAME=infinite-world
```

### File Upload Configuration (application.properties):
```properties
file.upload.max-size=52428800
file.upload.allowed-types=jpg,jpeg,png,gif,bmp,webp,svg,ico,mp4,avi,mov,wmv,flv,mkv,webm,pdf,doc,docx,xls,xlsx,ppt,pptx,txt,csv,zip,rar,7z
```

---

## 📊 CATEGORY VALIDATION RULES

API tự động validate file type và size dựa trên category:

| Category | Allowed File Types | Max Size | Validation |
|----------|-------------------|----------|------------|
| `images` | jpg, jpeg, png, gif, bmp, webp, svg, ico | 10MB | ✅ Strict |
| `avatar` | jpg, jpeg, png, gif, bmp, webp, svg, ico | 5MB | ✅ Strict |
| `videos` | mp4, avi, mov, wmv, flv, mkv, webm | 100MB | ✅ Strict |
| `documents` | pdf, doc, docx, xls, xlsx, ppt, pptx, txt, csv | 20MB | ✅ Strict |
| `archives` | zip, rar, 7z | 50MB | ✅ Strict |
| `attachments` | All allowed types | 50MB | ❌ No type validation |
| `temp` | All allowed types | 50MB | ❌ No type validation |
| Other | All allowed types | 50MB | ❌ No type validation |

### Lợi ích:
1. **Tự động validate:** Không cần endpoint riêng
2. **i18n support:** Error messages theo ngôn ngữ user
3. **Flexible:** Category khác không bị validate
4. **Clear errors:** Thông báo lỗi rõ ràng với size limit cụ thể
5. **Size optimization:** Mỗi category có giới hạn phù hợp

### Test với Accept-Language:
```bash
# Tiếng Việt
curl --location 'http://localhost:8083/v1/api/files/upload' \
--header 'Authorization: Bearer YOUR_JWT_TOKEN' \
--header 'Accept-Language: vi' \
--form 'file=@"/path/to/video.mp4"' \
--form 'category="images"'

# Response: "Chỉ chấp nhận file ảnh (jpg, jpeg, png, gif, bmp, webp, svg, ico)"

# Tiếng Anh
curl --location 'http://localhost:8083/v1/api/files/upload' \
--header 'Authorization: Bearer YOUR_JWT_TOKEN' \
--header 'Accept-Language: en' \
--form 'file=@"/path/to/video.mp4"' \
--form 'category="images"'

# Response: "Only image files are allowed (jpg, jpeg, png, gif, bmp, webp, svg, ico)"
```

### Test size limit:
```bash
# Upload ảnh 15MB vào category "images" (max 10MB) - ERROR
curl --location 'http://localhost:8083/v1/api/files/upload' \
--header 'Authorization: Bearer YOUR_JWT_TOKEN' \
--header 'Accept-Language: vi' \
--form 'file=@"/path/to/large-image.jpg"' \
--form 'category="images"'

# Response: "Kích thước file vượt quá giới hạn 10.00 MB"

# Upload ảnh 15MB vào category "attachments" (max 50MB) - OK
curl --location 'http://localhost:8083/v1/api/files/upload' \
--header 'Authorization: Bearer YOUR_JWT_TOKEN' \
--form 'file=@"/path/to/large-image.jpg"' \
--form 'category="attachments"'

# Response: Success
```

---

## 📊 FILE TYPE DETECTION

Service tự động phát hiện loại file:

```java
// Check if image
boolean isImage = fileService.isImageFile(file);

// Check if video
boolean isVideo = fileService.isVideoFile(file);

// Check if document
boolean isDocument = fileService.isDocumentFile(file);

// Check if archive
boolean isArchive = fileService.isArchiveFile(file);

// Check if valid (in allowed types)
boolean isValid = fileService.isValidFile(file);
```
