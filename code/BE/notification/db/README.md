# Database Migration Guide

## Overview

This directory contains database migration scripts for the notification service.

## Schema

- **Schema Name**: `INF_NOTI`
- **Database**: PostgreSQL

## Migration Files

### V001__create_schema_and_tables.sql

Creates the initial schema and all core tables:

- `notification_request` - Audit incoming requests
- `notification_template` - Normalized notification content
- `notification_target_rule` - Target audience rules
- `notification_delivery_job` - Fanout job tracking
- `notification_delivery_batch` - Batch processing
- `user_notification` - User inbox
- `user_notification_claim_log` - Reward claim audit
- `email_delivery_log` - Email delivery tracking

## Running Migrations

### Manual Execution

```bash
psql -U postgres -d infinite_world -f notification/db/migration/V001__create_schema_and_tables.sql
```

### Using Flyway (Future)

This structure is compatible with Flyway. To enable Flyway:

1. Add Flyway dependency to `pom.xml`
2. Configure Flyway in `application.yml`:

```yaml
spring:
  flyway:
    enabled: true
    locations: classpath:db/migration
    schemas: INF_NOTI
    baseline-on-migrate: true
```

## Retention Policy

- **user_notification**: 180 days (6 months)
- **email_delivery_log**: 90 days (3 months)
- **notification_request**: 365 days (1 year) for audit
- **notification_delivery_***: 90 days (3 months)

Cleanup jobs will be implemented in later phases.

## Index Strategy

All critical indexes are created upfront:

- Unique constraints for idempotency
- Indexes for inbox queries
- Indexes for job/batch status tracking
- Indexes for delivery log queries

## Notes

- All timestamps use `TIMESTAMP` type (UTC assumed)
- JSONB columns are used for flexible payload storage
- Foreign keys use `ON DELETE CASCADE` where appropriate
- Soft delete pattern used for user notifications

