# BÆ¯á»šC 2: Dá»±ng lÃµi `notification` tá»« module Ä‘áº¿n API inbox

## 1. Má»¥c tiÃªu cá»§a bÆ°á»›c nÃ y

`BUOC2.md` táº­p trung vÃ o giai Ä‘oáº¡n triá»ƒn khai lÃµi cá»§a `notification-service`, tÆ°Æ¡ng á»©ng:

- Phase 3 trong `HD.md`
- Phase 4 trong `HD.md`
- Phase 5 trong `HD.md`
- Phase 6 trong `HD.md`
- Phase 7 trong `HD.md`
- Phase 8 trong `HD.md`

ÄÃ¢y lÃ  bÆ°á»›c biáº¿n pháº§n contract Ä‘Ã£ chuáº©n hÃ³a á»Ÿ `BUOC1.md` thÃ nh má»™t service cháº¡y Ä‘Æ°á»£c vá»›i:

- module `notification` Ä‘Æ°á»£c chuáº©n bá»‹ Ä‘Ãºng ná»n
- database schema rÃµ rÃ ng
- persistence layer Ä‘á»§ dÃ¹ng
- inbound flow nháº­n request tá»« service khÃ¡c
- admin API Ä‘á»ƒ táº¡o vÃ  quáº£n lÃ½ notification
- client inbox API Ä‘á»ƒ user Ä‘á»c vÃ  thao tÃ¡c notification

Theo hÆ°á»›ng Ä‘Ã£ chá»‘t trong `DE_XUAT.md`, bÆ°á»›c nÃ y pháº£i bÃ¡m Ä‘Ãºng cÃ¡c nguyÃªn táº¯c sau:

- `notification-service` lÃ  entrypoint duy nháº¥t cho notification
- service khÃ¡c chá»‰ giao tiáº¿p qua contract public hoáº·c internal API
- má»i request pháº£i Ä‘i qua persistence trÆ°á»›c khi fanout/delivery
- business flow pháº£i gom vá» application service chung, khÃ´ng tÃ¡ch nhiá»u luá»“ng xá»­ lÃ½ rá»i ráº¡c

---

## 2. Pháº¡m vi cá»§a bÆ°á»›c 2

BÆ°á»›c nÃ y lÃ m cÃ¡c pháº§n sau:

- chuáº©n bá»‹ module `notification` Ä‘á»ƒ dÃ¹ng PostgreSQL + JPA + validation
- tá»• chá»©c package theo `api / application / domain / infrastructure`
- táº¡o database schema vÃ  migration cho notification
- táº¡o entity, repository vÃ  query ná»n
- lÃ m internal inbound flow nháº­n `NotificationRequestEvent`
- lÃ m admin API dÃ¹ng láº¡i cÃ¹ng business flow
- lÃ m client inbox API cho user Ä‘á»c, read, delete, claim

BÆ°á»›c nÃ y chÆ°a lÃ m:

- fanout worker
- realtime dispatch
- email delivery Ä‘áº§y Ä‘á»§
- retry / DLQ hoÃ n chá»‰nh
- unread count cache
- observability Ä‘áº§y Ä‘á»§
- retention cleanup tá»± Ä‘á»™ng
- tÃ­ch há»£p diá»‡n rá»™ng vá»›i cÃ¡c service khÃ¡c

---

## 3. Káº¿t quáº£ cáº§n cÃ³ sau bÆ°á»›c 2

Sau khi hoÃ n thÃ nh `BUOC2.md`, há»‡ thá»‘ng pháº£i Ä‘áº¡t:

- module `notification` cÃ³ dependency vÃ  cáº¥u trÃºc Ä‘á»§ Ä‘á»ƒ phÃ¡t triá»ƒn dÃ i háº¡n
- cÃ³ schema `INF_NOTI` vÃ  cÃ¡c báº£ng ná»n theo hÆ°á»›ng trong `DE_XUAT.md`
- cÃ³ persistence layer cho request, template, job, batch, inbox, claim log, email log
- service khÃ¡c cÃ³ thá»ƒ gá»i `POST /api/internal/notifications/requests`
- admin cÃ³ thá»ƒ táº¡o, xem, retry, cancel notification qua API
- user cÃ³ thá»ƒ láº¥y inbox, unread count, read, read-all, delete, claim
- inbound REST vÃ  inbound Kafka dÃ¹ng chung má»™t application flow

---

## 4. Phase 3: Chuáº©n bá»‹ module `notification`

## 4.1. ThÃªm dependency cáº§n thiáº¿t

Trong module `notification`, cáº§n bá»• sung tá»‘i thiá»ƒu:

- `spring-boot-starter-data-jpa`
- `postgresql`
- `spring-boot-starter-validation`
- dependency tá»›i `notification-contract`

Giá»¯ láº¡i cÃ¡c pháº§n Ä‘Ã£ cÃ³ giÃ¡ trá»‹ cho roadmap sau:

- Kafka
- Redis
- Mail

Má»¥c tiÃªu cá»§a phase nÃ y khÃ´ng pháº£i lÃ m xong delivery, mÃ  lÃ  dá»±ng ná»n Ä‘Ãºng Ä‘á»ƒ cÃ¡c phase sau khÃ´ng pháº£i sá»­a cáº¥u trÃºc build thÃªm láº§n ná»¯a.

## 4.2. Cáº¥u hÃ¬nh datasource vÃ  JPA

Cáº§n bá»• sung vÃ o `notification/src/main/resources/application.yml`:

- datasource PostgreSQL
- cáº¥u hÃ¬nh JPA
- schema máº·c Ä‘á»‹nh `INF_NOTI`
- cÃ¡c setting batch cÆ¡ báº£n náº¿u dÃ¹ng Hibernate

LÆ°u Ã½ theo `DE_XUAT.md`:

- JPA chá»‰ dÃ¹ng cho pháº§n persistence chuáº©n
- cÃ¡c bÃ i toÃ¡n fanout lá»›n vá» sau váº«n nÃªn Æ°u tiÃªn JDBC/native batch
- khÃ´ng thiáº¿t káº¿ theo hÆ°á»›ng save tá»«ng record má»™t cho workload lá»›n

## 4.3. Tá»• chá»©c láº¡i package

Khuyáº¿n nghá»‹ package nhÆ° sau:

```text
notification
â””â”€â”€ src/main/java/com/infinite/notification
    â”œâ”€â”€ api
    â”‚   â”œâ”€â”€ admin
    â”‚   â”œâ”€â”€ client
    â”‚   â””â”€â”€ internal
    â”œâ”€â”€ application
    â”‚   â”œâ”€â”€ command
    â”‚   â”œâ”€â”€ query
    â”‚   â””â”€â”€ mapper
    â”œâ”€â”€ domain
    â”‚   â”œâ”€â”€ model
    â”‚   â”œâ”€â”€ service
    â”‚   â””â”€â”€ policy
    â”œâ”€â”€ infrastructure
    â”‚   â”œâ”€â”€ persistence
    â”‚   â”œâ”€â”€ messaging
    â”‚   â”œâ”€â”€ redis
    â”‚   â”œâ”€â”€ email
    â”‚   â””â”€â”€ websocket
    â”œâ”€â”€ worker
    â”œâ”€â”€ scheduler
    â””â”€â”€ config
```

Má»¥c tiÃªu:

- khÃ´ng dá»“n toÃ n bá»™ logic vÃ o `service/impl`
- tÃ¡ch rÃµ orchestration, domain rule, persistence, messaging
- chuáº©n bá»‹ chá»— Ä‘á»©ng rÃµ rÃ ng cho fanout, realtime, email á»Ÿ bÆ°á»›c sau

---

## 5. Checklist Phase 3

- [ ] thÃªm dependency cho `notification`
- [ ] thÃªm dependency `notification-contract`
- [ ] cáº¥u hÃ¬nh PostgreSQL
- [ ] cáº¥u hÃ¬nh JPA vÃ  schema `INF_NOTI`
- [ ] chuáº©n hÃ³a package structure

---

## 6. Phase 4: Táº¡o database schema vÃ  migration

## 6.1. Táº¡o thÆ° má»¥c migration

Khá»Ÿi Ä‘áº§u cÃ³ thá»ƒ dÃ¹ng thÆ° má»¥c riÃªng trong module `notification`, vÃ­ dá»¥:

- `notification/db`

Náº¿u repo Ä‘Ã£ cÃ³ chuáº©n migration khÃ¡c thÃ¬ bÃ¡m theo chuáº©n Ä‘Ã³. Náº¿u chÆ°a cÃ³, nÃªn thiáº¿t káº¿ theo hÆ°á»›ng cÃ³ thá»ƒ nÃ¢ng cáº¥p sang Flyway sau nÃ y mÃ  khÃ´ng pháº£i viáº¿t láº¡i toÃ n bá»™ script.

## 6.2. Táº¡o cÃ¡c báº£ng theo thá»© tá»± Ä‘Ãºng

Theo hÆ°á»›ng trong `DE_XUAT.md`, nÃªn táº¡o:

1. `notification_request`
2. `notification_template`
3. `notification_target_rule`
4. `notification_delivery_job`
5. `notification_delivery_batch`
6. `user_notification`
7. `user_notification_claim_log`
8. `email_delivery_log`

Ã nghÄ©a cá»§a nhÃ³m báº£ng nÃ y:

- `notification_request`: audit request Ä‘áº§u vÃ o vÃ  chá»‘t idempotency boundary
- `notification_template`: lÆ°u báº£n chuáº©n hÃ³a cá»§a notification
- `notification_target_rule`: lÆ°u rule target
- `notification_delivery_job` vÃ  `notification_delivery_batch`: chuáº©n bá»‹ ná»n cho fanout lá»›n
- `user_notification`: inbox thá»±c táº¿ cá»§a user
- `user_notification_claim_log`: chá»‘ng claim trÃ¹ng vÃ  audit reward
- `email_delivery_log`: ná»n cho email delivery vÃ  retry

## 6.3. Táº¡o index ngay tá»« Ä‘áº§u

Ãt nháº¥t pháº£i cÃ³:

- unique cho `event_id`
- unique cho `idempotency_key`
- unique `(user_id, notification_id)`
- index inbox theo `user_id, created_at`
- index theo tráº¡ng thÃ¡i cho job vÃ  batch

KhÃ´ng nÃªn Ä‘á»£i Ä‘áº¿n lÃºc cháº¡y cháº­m má»›i thÃªm index, vÃ¬ cÃ¡c báº£ng notification thÆ°á»ng tÄƒng nhanh náº¿u tÃ­ch há»£p nhiá»u service.

## 6.4. Chá»‘t retention sÆ¡ bá»™

Ngay khi táº¡o schema pháº£i cÃ³ Ä‘á»‹nh hÆ°á»›ng giá»¯ dá»¯ liá»‡u:

- `user_notification` giá»¯ bao lÃ¢u
- `email_delivery_log` giá»¯ bao lÃ¢u
- `notification_request` vÃ  `notification_delivery_*` giá»¯ bao lÃ¢u Ä‘á»ƒ phá»¥c vá»¥ audit

Khuyáº¿n nghá»‹ bÃ¡m theo hÆ°á»›ng trong `DE_XUAT.md`:

- inbox: 90-180 ngÃ y
- email delivery log: Ã­t nháº¥t 90 ngÃ y

á»ž phase nÃ y chá»‰ cáº§n chá»‘t chÃ­nh sÃ¡ch sÆ¡ bá»™ trong tÃ i liá»‡u hoáº·c comment migration. Cleanup tá»± Ä‘á»™ng sáº½ lÃ m á»Ÿ bÆ°á»›c sau.

---

## 7. Checklist Phase 4

- [ ] táº¡o thÆ° má»¥c migration
- [ ] táº¡o SQL schema
- [ ] táº¡o cÃ¡c báº£ng ná»n
- [ ] táº¡o unique constraint vÃ  index quan trá»ng
- [ ] chá»‘t retention sÆ¡ bá»™

---

## 8. Phase 5: Táº¡o persistence layer

## 8.1. Táº¡o entity

Cáº§n táº¡o entity cho:

- `NotificationRequest`
- `NotificationTemplate`
- `NotificationTargetRule`
- `NotificationDeliveryJob`
- `NotificationDeliveryBatch`
- `UserNotification`
- `UserNotificationClaimLog`
- `EmailDeliveryLog`

YÃªu cáº§u:

- mapping rÃµ rÃ ng, dá»… Ä‘á»c
- khÃ´ng láº¡m dá»¥ng cascade bá»«a bÃ£i
- Æ°u tiÃªn mÃ´ hÃ¬nh Ä‘Æ¡n giáº£n, Ä‘Ãºng aggregate hÆ¡n lÃ  phá»©c táº¡p hÃ³a quan há»‡

## 8.2. Táº¡o repository

Tá»‘i thiá»ƒu cáº§n repository cho:

- request repository
- template repository
- job repository
- batch repository
- user notification repository
- email delivery log repository

Náº¿u cÃ³ báº£ng chá»‰ dÃ¹ng ná»™i bá»™ ráº¥t Ä‘Æ¡n giáº£n thÃ¬ cÃ³ thá»ƒ trÃ¬ hoÃ£n repository riÃªng, nhÆ°ng cÃ¡c aggregate chÃ­nh pháº£i cÃ³ abstraction Ä‘á»§ rÃµ Ä‘á»ƒ application layer khÃ´ng phá»¥ thuá»™c truy váº¥n táº£n máº¡n.

## 8.3. Táº¡o query riÃªng cho cÃ¡c bÃ i toÃ¡n lá»›n

KhÃ´ng nÃªn cá»‘ nhÃ©t toÃ n bá»™ query vÃ o method name dÃ i cá»§a Spring Data.

Cáº§n chuáº©n bá»‹ sá»›m query cho:

- láº¥y inbox theo user
- unread count
- láº¥y batch pending
- láº¥y job failed
- láº¥y email delivery failed

ÄÃ¢y lÃ  chá»— nÃªn chá»§ Ä‘á»™ng tÃ¡ch query object hoáº·c custom repository náº¿u cÃ¢u query báº¯t Ä‘áº§u vÆ°á»£t má»©c CRUD Ä‘Æ¡n giáº£n.

---

## 9. Checklist Phase 5

- [ ] táº¡o entity
- [ ] táº¡o repository
- [ ] táº¡o custom query cáº§n thiáº¿t
- [ ] chuáº©n hÃ³a mapping giá»¯a báº£ng vÃ  entity

---

## 10. Phase 6: LÃ m inbound request flow

## 10.1. Táº¡o internal API cho service khÃ¡c gá»i

Táº¡o endpoint:

- `POST /api/internal/notifications/requests`

Request body:

- `NotificationRequestEvent`

Flow cá»§a endpoint nÃ y pháº£i bÃ¡m Ä‘Ãºng hÆ°á»›ng trong `DE_XUAT.md`:

1. validate contract
2. validate `idempotencyKey`
3. lÆ°u `notification_request`
4. map sang `notification_template`
5. táº¡o `notification_target_rule`
6. táº¡o `notification_delivery_job`
7. phÃ¡t event ná»™i bá»™ Ä‘á»ƒ xá»­ lÃ½ tiáº¿p

Äiá»ƒm quan trá»ng:

- khÃ´ng fanout ngay trong request thread
- khÃ´ng cho tá»«ng controller tá»± xá»­ lÃ½ business flow riÃªng
- má»i request Ä‘á»u pháº£i Ä‘Æ°á»£c persist Ä‘á»ƒ audit vÃ  retry an toÃ n

## 10.2. Táº¡o inbound Kafka consumer cho topic public

Náº¿u há»‡ thá»‘ng há»— trá»£ async integration, táº¡o consumer cho:

- `notification.request.v1`

Consumer nÃ y khÃ´ng Ä‘Æ°á»£c duplicate business logic. NÃ³ pháº£i gá»i láº¡i cÃ¹ng application service mÃ  internal API Ä‘ang dÃ¹ng.

## 10.3. Táº¡o application service dÃ¹ng chung

NÃªn cÃ³ má»™t use case trung tÃ¢m, vÃ­ dá»¥:

- `CreateNotificationRequestUseCase`

Use case nÃ y chá»‹u trÃ¡ch nhiá»‡m:

- validate nghiá»‡p vá»¥ Ä‘áº§u vÃ o
- check idempotency
- persist request vÃ  cÃ¡c báº£n ghi liÃªn quan
- publish command/event ná»™i bá»™ cho bÆ°á»›c sau

Má»¥c tiÃªu:

- REST inbound dÃ¹ng cÃ¹ng flow
- Kafka inbound dÃ¹ng cÃ¹ng flow
- admin create cÅ©ng cÃ³ thá»ƒ dÃ¹ng láº¡i flow nÃ y

---

## 11. Checklist Phase 6

- [ ] táº¡o internal API
- [ ] nháº­n `NotificationRequestEvent`
- [ ] validate idempotency
- [ ] persist request/template/target/job
- [ ] táº¡o public Kafka consumer
- [ ] gom logic vÃ o application service chung

---

## 12. Phase 7: LÃ m admin API

## 12.1. Táº¡o admin API cÆ¡ báº£n

Táº¡o cÃ¡c endpoint:

- `POST /api/admin/notifications`
- `GET /api/admin/notifications`
- `GET /api/admin/notifications/{id}`
- `POST /api/admin/notifications/{id}/cancel`
- `POST /api/admin/notifications/{id}/retry`
- `GET /api/admin/notifications/{id}/delivery-summary`

Má»¥c tiÃªu cá»§a nhÃ³m API nÃ y:

- cho phÃ©p admin táº¡o notification thá»§ cÃ´ng
- theo dÃµi tráº¡ng thÃ¡i xá»­ lÃ½
- retry hoáº·c há»§y khi cáº§n
- xem tÃ³m táº¯t delivery

## 12.2. Chá»‘t quyá»n truy cáº­p

Cáº§n Ä‘á»‹nh nghÄ©a tá»‘i thiá»ƒu:

- `NOTIFICATION_ADMIN_CREATE`
- `NOTIFICATION_ADMIN_VIEW`
- `NOTIFICATION_ADMIN_CANCEL`
- `NOTIFICATION_ADMIN_RETRY`

Quyá»n nÃªn rÃµ ngay tá»« Ä‘áº§u Ä‘á»ƒ trÃ¡nh viá»‡c admin API cháº¡y táº¡m báº±ng quyá»n quÃ¡ rá»™ng rá»“i khÃ³ siáº¿t láº¡i vá» sau.

## 12.3. DÃ¹ng láº¡i cÃ¹ng flow táº¡o notification

Admin API khÃ´ng nÃªn táº¡o má»™t business flow riÃªng tÃ¡ch biá»‡t vá»›i internal API.

NÃ³ cÅ©ng nÃªn:

- táº¡o `NotificationRequest`
- táº¡o template
- táº¡o job

KhÃ¡c biá»‡t chá»§ yáº¿u náº±m á»Ÿ auth, validation nghiá»‡p vá»¥ admin vÃ  response model, khÃ´ng náº±m á»Ÿ lÃµi xá»­ lÃ½.

---

## 13. Checklist Phase 7

- [ ] táº¡o admin controller
- [ ] thÃªm phÃ¢n quyá»n
- [ ] dÃ¹ng láº¡i application service chung
- [ ] há»— trá»£ list/detail/cancel/retry/delivery-summary

---

## 14. Phase 8: LÃ m client inbox API

## 14.1. Táº¡o cÃ¡c endpoint cÆ¡ báº£n

Táº¡o:

- `GET /api/notifications`
- `GET /api/notifications/unread-count`
- `POST /api/notifications/{id}/read`
- `POST /api/notifications/read-all`
- `POST /api/notifications/{id}/delete`
- `POST /api/notifications/{id}/claim`

CÃ¡c API nÃ y lÃ  máº·t client cá»§a inbox, nÃªn pháº£i bÃ¡m trá»±c tiáº¿p vÃ o `user_notification` thay vÃ¬ suy luáº­n tá»« request gá»‘c.

## 14.2. Quy táº¯c báº£o máº­t

Client API pháº£i:

- láº¥y user hiá»‡n táº¡i tá»« JWT hoáº·c auth context
- khÃ´ng nháº­n `userId` tá»« client Ä‘á»ƒ query inbox ngÆ°á»i khÃ¡c
- chá»‰ thao tÃ¡c trÃªn notification thuá»™c user hiá»‡n táº¡i

ÄÃ¢y lÃ  pháº§n ráº¥t dá»… sai náº¿u lÃ m nhanh kiá»ƒu CRUD thÆ°á»ng, nÃªn pháº£i khÃ³a boundary ngay tá»« controller vÃ  query layer.

## 14.3. Read vÃ  claim pháº£i an toÃ n

`read`:

- chá»‰ update náº¿u notification thuá»™c user hiá»‡n táº¡i
- khÃ´ng lÃ m áº£nh hÆ°á»Ÿng báº£n ghi user khÃ¡c

`claim`:

- dÃ¹ng transaction
- lock row náº¿u cáº§n
- kiá»ƒm tra `isClaimed`
- ghi `user_notification_claim_log`

Theo hÆ°á»›ng trong `DE_XUAT.md`, `claim` pháº£i Ä‘Æ°á»£c xem lÃ  thao tÃ¡c nghiá»‡p vá»¥ cÃ³ rá»§i ro trÃ¹ng, khÃ´ng pháº£i chá»‰ lÃ  má»™t update cá» Ä‘Æ¡n giáº£n.

---

## 15. Checklist Phase 8

- [ ] táº¡o client controller
- [ ] gáº¯n auth context Ä‘Ãºng
- [ ] lÃ m API láº¥y inbox
- [ ] lÃ m API unread count
- [ ] lÃ m logic read
- [ ] lÃ m logic read-all
- [ ] lÃ m logic delete
- [ ] lÃ m logic claim an toÃ n

---

## 16. Thá»© tá»± lÃ m chi tiáº¿t tÃ´i khuyáº¿n nghá»‹

NÃªn lÃ m theo thá»© tá»± nÃ y:

1. hoÃ n táº¥t dependency vÃ  cáº¥u trÃºc module `notification`
2. cáº¥u hÃ¬nh datasource vÃ  JPA
3. táº¡o migration schema
4. táº¡o entity
5. táº¡o repository vÃ  query ná»n
6. lÃ m application service cho inbound flow
7. má»Ÿ internal API
8. thÃªm inbound Kafka consumer
9. lÃ m admin API dÃ¹ng láº¡i flow chung
10. lÃ m client inbox API

Náº¿u lÃ m ngÆ°á»£c, vÃ­ dá»¥ má»Ÿ admin/client API trÆ°á»›c khi chá»‘t entity vÃ  flow inbound, ráº¥t dá»… bá»‹ duplicate logic vÃ  pháº£i sá»­a controller/service thÃªm láº§n ná»¯a.

---

## 17. Äiá»u kiá»‡n hoÃ n thÃ nh bÆ°á»›c 2

CÃ³ thá»ƒ coi `BUOC2.md` hoÃ n táº¥t khi:

- module `notification` Ä‘Ã£ cÃ³ ná»n dependency vÃ  package structure Ä‘Ãºng hÆ°á»›ng
- schema `INF_NOTI` vÃ  cÃ¡c báº£ng chÃ­nh Ä‘Ã£ tá»“n táº¡i
- persistence layer Ä‘Ã£ Ä‘á»§ cho request, template, job, batch, inbox, claim log, email log
- cÃ³ internal API nháº­n `NotificationRequestEvent`
- inbound Kafka vÃ  internal API dÃ¹ng chung má»™t application flow
- admin API cÆ¡ báº£n Ä‘Ã£ dÃ¹ng Ä‘Æ°á»£c
- client inbox API cÆ¡ báº£n Ä‘Ã£ dÃ¹ng Ä‘Æ°á»£c

---

## 18. Nhá»¯ng lá»—i dá»… máº¯c trong bÆ°á»›c nÃ y

KhÃ´ng nÃªn lÃ m:

- Ä‘á»ƒ controller tá»± xá»­ lÃ½ quÃ¡ nhiá»u business logic
- fanout trá»±c tiáº¿p trong request thread
- Ä‘á»ƒ admin API vÃ  internal API cÃ³ hai flow táº¡o notification khÃ¡c nhau
- cho client truyá»n `userId` Ä‘á»ƒ query inbox
- coi `claim` nhÆ° má»™t update cá» Ä‘Æ¡n giáº£n khÃ´ng transaction
- táº¡o schema xong nhÆ°ng chÆ°a cÃ³ unique/index cho idempotency vÃ  inbox
- nhÃ©t toÃ n bá»™ query phá»©c táº¡p vÃ o Spring Data method name

---

## 19. Káº¿t luáº­n

`BUOC2.md` lÃ  bÆ°á»›c dá»±ng lÃµi thá»±c thi cá»§a `notification-service`. Náº¿u `BUOC1.md` lÃ  bÆ°á»›c chá»‘t Ä‘Ãºng boundary vÃ  contract, thÃ¬ bÆ°á»›c nÃ y lÃ  pháº§n biáº¿n boundary Ä‘Ã³ thÃ nh má»™t service cÃ³ database, persistence, inbound flow, admin API vÃ  client inbox API Ä‘á»§ sáº¡ch Ä‘á»ƒ má»Ÿ rá»™ng tiáº¿p.

Äiá»ƒm cáº§n giá»¯ cháº·t:

- inbound flow pháº£i dÃ¹ng chung má»™t application service
- má»i request pháº£i Ä‘Æ°á»£c persist trÆ°á»›c khi xá»­ lÃ½ sÃ¢u hÆ¡n
- admin vÃ  internal chá»‰ khÃ¡c entrypoint, khÃ´ng khÃ¡c business core
- inbox client pháº£i an toÃ n theo user context
- toÃ n bá»™ schema vÃ  persistence pháº£i bÃ¡m hÆ°á»›ng scale, audit vÃ  idempotency Ä‘Ã£ chá»‘t trong `DE_XUAT.md`

