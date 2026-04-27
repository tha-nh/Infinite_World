# Notification Contract

Module chứa contract dùng chung cho tích hợp notification service.

## Mục đích

Module này cung cấp:
- DTO và event chuẩn cho notification requests
- Enum types cho channels, priorities, statuses
- Builder helpers để tạo notification requests dễ dàng
- Topic names và schema version constants

## Sử dụng

### Thêm dependency

```xml
<dependency>
    <groupId>com.infinite</groupId>
    <artifactId>notification-contract</artifactId>
    <version>${project.version}</version>
</dependency>
```

### Tạo notification request

```java
import com.infinite.notification.contract.builder.NotificationRequestBuilder;
import com.infinite.notification.contract.dto.*;
import com.infinite.notification.contract.enumtype.*;
import com.infinite.notification.contract.event.NotificationRequestEvent;

// Tạo notification request
NotificationRequestEvent event = NotificationRequestBuilder.create()
    .sourceService("user-service")
    .sourceModule("auth")
    .sourceAction("user_registered")
    .idempotencyKey("user_registered:123")
    .addChannel(NotificationChannel.EMAIL)
    .addChannel(NotificationChannel.REALTIME)
    .target(NotificationTarget.builder()
        .type(NotificationTargetType.USER_IDS)
        .userIds(List.of(123L))
        .build())
    .content(NotificationContent.builder()
        .type(NotificationType.ACCOUNT)
        .priority(NotificationPriority.HIGH)
        .title("Chào mừng bạn")
        .body("Tài khoản của bạn đã được tạo thành công")
        .build())
    .action(NotificationAction.builder()
        .actionType(NotificationActionType.OPEN_SCREEN)
        .screen("profile")
        .build())
    .build();
```

### Public Topics

```java
import com.infinite.notification.contract.constant.NotificationTopicNames;

// Topic để publish notification requests
String requestTopic = NotificationTopicNames.NOTIFICATION_REQUEST_V1;

// Topic để nhận status updates
String statusTopic = NotificationTopicNames.NOTIFICATION_STATUS_CHANGED_V1;
```

## Cấu trúc

```
notification-contract/
├── builder/          # Helper builders
├── constant/         # Constants (topics, versions)
├── dto/             # Data transfer objects
├── enumtype/        # Enum types
├── event/           # Event definitions
└── metadata/        # Base metadata classes
```

## Schema Version

Current version: **v1**

Mọi thay đổi breaking changes phải tăng version.

## Nguyên tắc

- Module này chỉ chứa contract và helper mỏng
- Không chứa business logic
- Không chứa implementation (Kafka, REST, etc.)
- Không depend vào Spring Boot starters
