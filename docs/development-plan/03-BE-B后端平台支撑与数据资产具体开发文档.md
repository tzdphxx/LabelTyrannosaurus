# BE-B 后端平台支撑与数据资产具体开发文档

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 以 BE-B 身份完成 LabelHub 的平台支撑、数据资产、模板资源、奖励统计、导出、存储、Redis/Redisson、通用异步任务与统一审计能力。

**Architecture:** 项目采用 Spring Boot 3 模块化单体，BE-B 负责可复用平台能力和数据资产服务，BE-A 负责任务、领取、提交、AI 审核、人工终审和冲突仲裁主链路。跨模块协作优先使用 Java Service 接口、领域事件和统一 `AuditAppender`，不直接写对方模块状态。

**Tech Stack:** Java 17、Spring Boot 3、Spring Security、MyBatis-Plus、Flyway、MySQL、Redis/Redisson、Tencent COS 或本地兼容对象存储、JUnit 5。

---

## 1. 文档依据

开发前必须先读以下文件：

- `DEVELOPMENT_GUIDELINES.md`
- `docs/development-plan/04-对接契约与数据结构协作手册.md`
- `backend/sql/script.sql`
- `D:\广工\QG\比赛\AI全栈挑战赛\03-BE-B后端平台支撑与数据资产开发任务书.md`
- `D:\广工\QG\比赛\AI全栈挑战赛\LabelHub详细设计与开发文档.md`

本文件只描述 BE-B 的具体开发落地方式。若接口字段、状态枚举、错误码、事件字段需要变更，先改协作手册或 `docs/api-contracts/`，再改代码。

当前数据库结构以 `backend/sql/script.sql` 为准。该 SQL 是全项目脚本，包含 BE-B 负责表，也包含 BE-A 负责的任务、领取、提交、AI 审核、人工审核和冲突仲裁表。BE-B 开发时可以建立外键映射和只读查询，但不能直接推进 BE-A 拥有的业务状态。

## 2. BE-B 责任边界

BE-B 负责：

- Auth/RBAC、Admin 用户管理、JWT 签发与解析、`CurrentUserContext`
- System Principal：`system_ai_agent`
- `AuditAppender` 与审计查询
- 数据集导入、题目批量编辑、题目快照查询、可领取题目预留能力
- 模板版本管理、Schema 存储、Schema 校验服务
- 奖励规则、奖励流水、贡献统计
- 导出任务、字段映射、多格式文件生成
- 对象存储、文件上传、错误报告
- Redis/Redisson lock、rate limit、草稿缓存基础能力
- 通用异步任务执行器

BE-B 禁止：

- 直接修改 `submission.status`
- 直接修改 `review.status`
- 直接决定或修改 `isGolden`
- 绕过 BE-A 快照查询拼装导出 submission 数据
- 直接操作 BE-A 负责的 AI 审核配置、Agent Run、人工终审和冲突组状态

## 3. 目标目录

后端主目录：

```text
backend/src/main/java/com/labelhub/
  common/
    api/
    audit/
    exception/
    security/
  infrastructure/
    redis/
    storage/
  modules/
    auth/
    admin/
    audit/
    dataset/
    template/
    reward/
    export/
```

数据库迁移：

```text
backend/src/main/resources/db/migration/
```

测试目录：

```text
backend/src/test/java/com/labelhub/
```

接口契约建议补充：

```text
docs/api-contracts/auth.md
docs/api-contracts/admin-user.md
docs/api-contracts/audit.md
docs/api-contracts/dataset.md
docs/api-contracts/template.md
docs/api-contracts/schema-validation.md
docs/api-contracts/reward.md
docs/api-contracts/contribution.md
docs/api-contracts/export.md
docs/api-contracts/storage.md
docs/api-contracts/platform-infra.md
```

## 4. 横向基础能力

### 4.1 统一响应与错误

使用现有 `common/api/ApiResponse.java`。所有 Controller 返回统一结构：

```json
{
  "code": 0,
  "message": "ok",
  "data": {},
  "traceId": "trace-id"
}
```

BE-B 必须覆盖的错误码：

|code|场景|
|---|---|
|401001|未登录、token 失效、tokenVersion 不一致|
|403001|无角色权限|
|400101|状态不允许|
|400102|参数非法|
|409101|版本冲突|
|409201|领取预留冲突|
|409301|Schema 校验失败|
|429001|限流|
|500001|系统错误|

### 4.2 权限角色

角色枚举沿用 `common/security/RoleCode.java`：

```text
ADMIN
OWNER
LABELER
REVIEWER
SYSTEM_AGENT
```

接口权限入口必须在 Controller 或 Security 配置中显式声明，不能只依赖前端隐藏按钮。

### 4.3 审计追加

统一接口：

```text
AuditAppender.append(AuditCommand command)
```

`AuditCommand` 字段：

```text
actorType
actorId
bizType
bizId
action
beforeJson
afterJson
traceId
agentRunId
```

审计日志只追加，不更新，不删除。BE-A 和 BE-B 都只能通过 `AuditAppender` 写审计。

### 4.4 注释规范

后端代码必须有规范注释，但不写无意义注释。注释目标是解释业务意图、边界条件和风险点，不重复代码本身。

必须写注释的场景：

- 领域服务、跨模块内部接口、公共基础设施类需要写类级 Javadoc，说明职责、Owner 边界和主要调用方。
- 对外 Controller 方法、Service 用例方法、Mapper 复杂查询方法需要写方法级 Javadoc，说明输入、输出、权限或事务要求。
- 状态机迁移、奖励幂等、事件消费、Redis 锁、限流、异步任务、对象存储 key 生成、导出分页读取等关键逻辑，需要在代码块前写简短说明。
- 使用 `reward_ledger.source_event_id`、`positive_submission_id`、`positive_assignment_id` 等数据库约束兜底幂等时，必须注释说明依赖的唯一约束。
- BE-B 读取 BE-A 表但不推进状态的地方，必须注释说明“只读依赖”和禁止写入的状态字段。
- 临时兼容 SQL 与协作手册差异时，必须在代码注释里写明对应字段和后续清理条件。

禁止的注释：

- 禁止写“设置值”“调用方法”“返回结果”这类重复代码字面含义的注释。
- 禁止用注释掩盖命名不清晰的问题；能通过命名表达的业务含义优先改命名。
- 禁止留下过期注释、误导性注释和没有处理计划的注释。
- 禁止在注释中写密码、JWT、API Key、COS Secret、LLM Key 或用户敏感数据。

推荐示例：

```java
/**
 * 统一追加审计日志。审计表由 BE-B 维护，BE-A/BE-B 均只能通过该接口写入。
 */
public interface AuditAppender {
    void append(AuditCommand command);
}
```

```java
// 奖励幂等依赖 reward_ledger.source_event_id 和 positive_submission_id 唯一约束兜底。
// 这里先查再插只用于提前返回友好结果，最终一致性仍以数据库唯一约束为准。
```

## 5. 数据库迁移规划

`backend/sql/script.sql` 当前已经定义了全量表结构。后续落地到 Flyway 时，建议保留一个 baseline，再按增量改动拆 migration，避免和现有脚本漂移：

```text
V1__baseline.sql                     导入或等价转换 backend/sql/script.sql
V2__beb_auth_admin_audit_delta.sql    只放后续 Auth/Admin/Audit 增量
V3__beb_dataset_template_delta.sql    只放后续 Dataset/Template 增量
V4__beb_reward_export_storage_delta.sql
```

禁止同一字段在 `script.sql`、Flyway migration 和接口契约中出现三套不同命名。若以 Flyway 为最终执行源，需要把 `script.sql` 内容迁移进 `backend/src/main/resources/db/migration/V1__baseline.sql` 或明确废弃其中一个入口。

### 5.0 当前 SQL 归属清单

BE-B 负责维护的表：

- `users`
- `user_roles`
- `audit_logs`
- `object_files`
- `dataset_files`
- `dataset_import_jobs`
- `dataset_items`
- `dataset_item_change_logs`
- `templates`
- `template_versions`
- `reward_rules`
- `reward_ledger`
- `labeler_contribution_stats`
- `labeler_daily_stats`
- `labeler_task_stats`
- `export_jobs`

BE-B 需要读取或通过内部接口协作，但状态 Owner 属于 BE-A 的表：

- `tasks`
- `task_stats`
- `task_tags`
- `assignments`
- `submissions`
- `llm_providers`
- `ai_review_configs`
- `agent_runs`
- `ai_review_results`
- `conflict_groups`
- `review_tasks`
- `review_records`

当前 SQL 尚未提供的 BE-B 任务书能力表：

- `async_jobs`：任务书要求通用异步任务执行器，但 SQL 目前只给导入和导出分别建了 `dataset_import_jobs`、`export_jobs`。
- `domain_event_dedup`：任务书要求奖励事件幂等，SQL 目前使用 `reward_ledger.source_event_id` 唯一约束和正向奖励生成列做奖励幂等，没有独立事件去重表。

落地建议：MVP 先按现有 SQL 实现，不新增表；导入和导出使用各自 job 表记录状态，奖励消费用 `reward_ledger.source_event_id`、`positive_submission_id`、`positive_assignment_id` 保证幂等。若后续需要 AI 审核也复用通用异步 job 追踪，再补 `async_jobs` 增量 migration。

### 5.1 Auth/Admin/Audit 表

需要表：

- `users`
- `user_roles`
- `audit_logs`

关键约束：

- `users.username` 唯一
- `users.email` 唯一
- `users.user_type` 只存 `USER` 或 `SYSTEM`
- `users.login_enabled=false` 的用户禁止登录
- `users.token_version` 用于角色变更后旧 token 失效
- `users.password_hash` 允许为空，仅 `SYSTEM` 用户可以为空；普通 `USER` 注册必须写 BCrypt hash
- `users.display_name`、`avatar_url`、`last_login_at` 可用于当前用户信息和后台用户列表
- `user_roles.role_code` 只允许 `ADMIN`、`OWNER`、`LABELER`、`REVIEWER`、`SYSTEM_AGENT`
- `audit_logs.actor_type` 只允许 `USER`、`SYSTEM_AGENT`
- `audit_logs.agent_run_id` 外键指向 BE-A 的 `agent_runs`，BE-B 只负责记录和查询

### 5.2 Dataset/Template 表

需要表：

- `dataset_items`
- `dataset_import_jobs`
- `dataset_files`
- `dataset_item_change_logs`
- `templates`
- `template_versions`

关键约束：

- `dataset_items(task_id, external_id)` 唯一
- `dataset_items.dataset_type` 只允许 `QA_QUALITY`、`PREFERENCE_COMPARE`
- `dataset_files.file_format` 只允许 `JSON`、`JSONL`、`EXCEL`、`CSV`
- `dataset_import_jobs.import_mode` 只允许 `APPEND`、`OVERWRITE`
- `dataset_import_jobs.status` 只允许 `PENDING`、`RUNNING`、`SUCCESS`、`FAILED`、`PARTIAL_SUCCESS`
- `dataset_items.deleted` 软删除
- `dataset_items.assigned_count`、`submitted_count`、`approved_count` 是 BE-B 维护的数据资产计数
- `template_versions(template_id, version_no)` 唯一
- `templates.current_version_no` 记录当前版本号，创建新版本时必须同步递增
- `template_versions.published_snapshot=true` 后不可修改 `schema_json`
- `tasks.published_template_version_id` 外键指向 `template_versions.id`，发布后由 BE-A 冻结任务引用

### 5.3 Reward/Contribution 表

需要表：

- `reward_rules`
- `reward_ledger`
- `labeler_contribution_stats`
- `labeler_daily_stats`
- `labeler_task_stats`

关键约束：

- `reward_rules(task_id, effective_version)` 唯一
- `reward_rules.reward_mode` 当前只允许 `APPROVED_ITEM`
- `reward_rules.unit_reward >= 0`
- `reward_ledger.source_event_id` 唯一，作为事件重复投递的第一层幂等
- `reward_ledger.positive_submission_id` 是生成列，保证同一 `submission_id` 只能产生一条正向奖励流水
- `reward_ledger.positive_assignment_id` 是生成列，保证同一 assignment 多版本最终只发一条有效正向奖励
- `reward_ledger.direction` 只允许 `CREDIT`、`DEBIT`
- `reward_ledger.reward_type` 只允许 `SUBMISSION_APPROVED`、`GOLDEN_SELECTED`、`REWARD_REVERSED`
- 撤销奖励只能追加负向流水
- `labeler_daily_stats(labeler_id, stat_date)` 唯一，用于近 7 日趋势补零和增量统计
- `labeler_task_stats(labeler_id, task_id)` 唯一，用于各任务贡献明细

### 5.4 Storage/Export/Async 表

需要表：

- `object_files`
- `export_jobs`

关键约束：

- 文件二进制不入库，只存 object key、bucket、content type、size、hash
- `object_files(bucket_name, object_key)` 唯一
- `object_files.storage_provider` 默认 `MINIO`
- `export_jobs.export_format` 只允许 `JSON`、`JSONL`、`CSV`、`EXCEL`
- `export_jobs.status` 只允许 `PENDING`、`RUNNING`、`SUCCESS`、`FAILED`
- 导出任务状态可查询，失败必须保存错误信息和 traceId
- 通用异步执行器当前不落独立 `async_jobs` 表；导入使用 `dataset_import_jobs`，导出使用 `export_jobs`

### 5.5 BE-A 表的 BE-B 使用边界

`script.sql` 中以下字段会影响 BE-B 实现，但状态推进不属于 BE-B：

- `tasks.status` 只允许 `DRAFT`、`PUBLISHED`、`PAUSED`、`ENDED`。BE-B 在数据集覆盖导入、模板修改、导出时只读取它做权限和状态校验。
- `assignments.status` 当前 SQL 包含 `AI_RETURNED`，比协作手册多。BE-B 只读取领取/提交/通过计数相关状态，不直接更新。
- `submissions.status` 当前 SQL 包含 `AI_REJECTED`，比协作手册多。BE-B 导出和奖励只接受 BE-A 事件或快照，不直接判断完整审核流。
- `submissions.review_flow_status`、`current_review_level`、`assigned_reviewer_id`、`review_version` 属于 BE-A Reviewer 工作台和多级审核扩展字段，BE-B 不写。
- `submissions.is_golden` 和生成列 `golden_dataset_item_id` 保证同一题目只有一个金标。BE-B 导出只能通过 BE-A 的 `queryExportableGoldenSubmissions` 获取快照。
- `review_tasks`、`review_records` 属于 BE-A；BE-B 审计查询可展示相关 audit，不维护审核队列。

需要团队确认的契约差异：

- 协作手册中 `AssignmentStatus` 未包含 `AI_RETURNED`，但 SQL 已包含。
- 协作手册中 `SubmissionStatus` 未包含 `AI_REJECTED`，但 SQL 已包含。
- 任务书要求通用 `AsyncExecutor`，但 SQL 未建 `async_jobs`。
- 任务书要求事件幂等能力，SQL 未建独立事件去重表，而是靠 `reward_ledger` 约束承担奖励幂等。

## 6. 模块开发任务

### Task 1: Auth/RBAC 与 Admin

**Files:**

- Create: `backend/src/main/java/com/labelhub/modules/auth/controller/AuthController.java`
- Create: `backend/src/main/java/com/labelhub/modules/auth/service/AuthService.java`
- Create: `backend/src/main/java/com/labelhub/modules/auth/domain/UserType.java`
- Create: `backend/src/main/java/com/labelhub/modules/auth/dto/RegisterRequest.java`
- Create: `backend/src/main/java/com/labelhub/modules/auth/dto/LoginRequest.java`
- Create: `backend/src/main/java/com/labelhub/modules/auth/dto/TokenResponse.java`
- Create: `backend/src/main/java/com/labelhub/modules/admin/controller/AdminUserController.java`
- Create: `backend/src/main/java/com/labelhub/modules/admin/service/AdminUserService.java`
- Create: `backend/src/main/java/com/labelhub/common/security/JwtTokenService.java`
- Create: `backend/src/main/java/com/labelhub/common/security/CurrentUserContext.java`
- Modify: `backend/src/main/java/com/labelhub/common/security/CurrentUser.java`
- Modify: `backend/src/main/java/com/labelhub/common/security/RoleCode.java`
- Test: `backend/src/test/java/com/labelhub/modules/auth/AuthServiceTest.java`
- Test: `backend/src/test/java/com/labelhub/modules/admin/AdminUserServiceTest.java`

- [ ] 注册接口：`POST /api/v1/auth/register`，username/email 唯一，密码 BCrypt，默认角色 `LABELER`。
- [ ] 登录接口：`POST /api/v1/auth/login`，校验 `enabled=true`、`loginEnabled=true`、密码正确，返回 accessToken、refreshToken、tokenVersion。
- [ ] 刷新接口：`POST /api/v1/auth/refresh`，校验 refresh token 与 tokenVersion。
- [ ] 当前用户接口：`GET /api/v1/users/me`，返回 userId、username、email、roles。
- [ ] 登出接口：MVP 可通过前端清 token 完成；若实现服务端失效，则递增 tokenVersion。
- [ ] Admin 用户列表：`GET /api/v1/admin/users`，默认过滤 `userType=SYSTEM`。
- [ ] Admin 修改角色：`PUT /api/v1/admin/users/{userId}/roles`，递增 tokenVersion。
- [ ] Admin 启用/禁用：禁用后用户不能登录。
- [ ] 禁止移除最后一个 `ADMIN`。
- [ ] 测试错误密码不能登录、非 Admin 不能改角色、角色变更后旧 token 失效。

### Task 2: System Principal 与 AuditAppender

**Files:**

- Create: `backend/src/main/java/com/labelhub/modules/auth/service/SystemPrincipalService.java`
- Create: `backend/src/main/java/com/labelhub/modules/auth/config/SystemPrincipalInitializer.java`
- Create: `backend/src/main/java/com/labelhub/common/audit/AuditCommand.java`
- Modify: `backend/src/main/java/com/labelhub/common/audit/AuditAppender.java`
- Create: `backend/src/main/java/com/labelhub/modules/audit/controller/AuditLogController.java`
- Create: `backend/src/main/java/com/labelhub/modules/audit/service/AuditLogService.java`
- Test: `backend/src/test/java/com/labelhub/modules/audit/AuditAppenderTest.java`

- [ ] 应用启动时确保 `system_ai_agent` 存在。
- [ ] `system_ai_agent` 字段固定为 `userType=SYSTEM`、`loginEnabled=false`、`enabled=true`、角色 `SYSTEM_AGENT`。
- [ ] 提供 `SystemPrincipalService.getSystemAgentProfile()` 给 BE-A 调用。
- [ ] `AuditAppender.append` 写入 `audit_logs`，必须带 traceId。
- [ ] 审计查询接口：`GET /api/v1/audit-logs?bizType={bizType}&bizId={bizId}`。
- [ ] 测试 system 用户不能登录，AuditAppender 能追加 AI 审计且带 agentRunId。

### Task 3: Redis/Redisson 与平台基础设施

**Files:**

- Modify: `backend/src/main/java/com/labelhub/infrastructure/redis/RedisLockService.java`
- Create: `backend/src/main/java/com/labelhub/infrastructure/redis/RateLimitService.java`
- Create: `backend/src/main/java/com/labelhub/infrastructure/redis/RedisKeyBuilder.java`
- Create: `backend/src/main/java/com/labelhub/infrastructure/async/AsyncJobService.java`
- Create: `backend/src/main/java/com/labelhub/infrastructure/async/AsyncJobExecutor.java`
- Test: `backend/src/test/java/com/labelhub/infrastructure/redis/RedisLockServiceTest.java`
- Test: `backend/src/test/java/com/labelhub/infrastructure/redis/RateLimitServiceTest.java`
- Test: `backend/src/test/java/com/labelhub/infrastructure/async/AsyncJobServiceTest.java`

- [ ] 实现 Redis key 规范：`lock:claim:task:{taskId}:item:{itemId}`、`draft:assignment:{assignmentId}`、`llm:rate:*`、`event:dedup:*`。
- [ ] `RedisLockService` 提供 tryLock、unlock、withLock。
- [ ] `RateLimitService` 使用 Redisson `RRateLimiter`，返回 allowed/retryAfter。
- [ ] `AsyncJobService` 先做执行编排接口，不新建 `async_jobs` 表；导入状态写 `dataset_import_jobs`，导出状态写 `export_jobs`。
- [ ] `AsyncJobExecutor` 执行导入、导出等后台任务，失败不影响主事务，并把错误写回对应 job 表。
- [ ] 测试锁超时释放、限流命中、异步任务失败状态可查。

### Task 4: 对象存储与文件上传

**Files:**

- Modify: `backend/src/main/java/com/labelhub/infrastructure/storage/ObjectStorageService.java`
- Create: `backend/src/main/java/com/labelhub/modules/storage/controller/FileController.java`
- Create: `backend/src/main/java/com/labelhub/modules/storage/service/FileService.java`
- Create: `backend/src/main/java/com/labelhub/modules/storage/dto/FileUploadResponse.java`
- Test: `backend/src/test/java/com/labelhub/modules/storage/FileServiceTest.java`

- [ ] 上传接口：`POST /api/v1/files/upload`，校验文件类型、大小和归属权限。
- [ ] 下载签名接口：`GET /api/v1/files/{fileId}/signed-url`。
- [ ] 文件元数据写入 `object_files`。
- [ ] object key 统一由后端生成，包含业务目录、日期和随机后缀。
- [ ] 禁止数据库存储文件二进制。
- [ ] 测试上传、元数据保存、预签名 URL 生成。

### Task 5: 数据集导入与错误报告

**Files:**

- Create: `backend/src/main/java/com/labelhub/modules/dataset/controller/DatasetImportController.java`
- Create: `backend/src/main/java/com/labelhub/modules/dataset/service/DatasetImportService.java`
- Create: `backend/src/main/java/com/labelhub/modules/dataset/service/DatasetParser.java`
- Create: `backend/src/main/java/com/labelhub/modules/dataset/service/JsonDatasetParser.java`
- Create: `backend/src/main/java/com/labelhub/modules/dataset/service/JsonlDatasetParser.java`
- Create: `backend/src/main/java/com/labelhub/modules/dataset/service/ExcelDatasetParser.java`
- Modify: `backend/src/main/java/com/labelhub/modules/dataset/domain/DatasetType.java`
- Test: `backend/src/test/java/com/labelhub/modules/dataset/DatasetImportServiceTest.java`

- [ ] 导入接口：`POST /api/v1/tasks/{taskId}/dataset/import`。
- [ ] 状态查询：`GET /api/v1/tasks/{taskId}/dataset/import-jobs/{jobId}`。
- [ ] 支持 JSON、JSONL、Excel。
- [ ] 支持 `QA_QUALITY` 和 `PREFERENCE_COMPARE`。
- [ ] 同任务 `externalId` 唯一，重复行记录失败原因。
- [ ] 单行失败不影响其他行。
- [ ] 原始文件和错误报告走对象存储。
- [ ] 测试 qa_quality.jsonl、preference_compare.jsonl、重复 externalId、错误报告下载地址。

### Task 6: 题目批量编辑与快照服务

**Files:**

- Create: `backend/src/main/java/com/labelhub/modules/dataset/controller/DatasetItemController.java`
- Create: `backend/src/main/java/com/labelhub/modules/dataset/service/DatasetItemService.java`
- Create: `backend/src/main/java/com/labelhub/modules/dataset/service/DatasetSnapshotService.java`
- Create: `backend/src/main/java/com/labelhub/modules/dataset/dto/BatchItemResult.java`
- Test: `backend/src/test/java/com/labelhub/modules/dataset/DatasetItemServiceTest.java`

- [ ] 列表接口：`GET /api/v1/tasks/{taskId}/dataset/items`。
- [ ] 批量更新：`POST /api/v1/tasks/{taskId}/dataset/items/batch-update`。
- [ ] 批量删除：`POST /api/v1/tasks/{taskId}/dataset/items/batch-delete`，软删除。
- [ ] 批量追加：`POST /api/v1/tasks/{taskId}/dataset/items/batch-append`。
- [ ] 覆盖导入：`POST /api/v1/tasks/{taskId}/dataset/import/overwrite`，只允许 DRAFT。
- [ ] `DatasetSnapshotService.getDatasetItemSnapshot(itemId)` 给 BE-A 使用。
- [ ] `DatasetSnapshotService.reserveClaimableItem(taskId, labelerId)` 给 BE-A 领取编排使用。
- [ ] `increaseSubmittedCount(itemId)` 和 `increaseApprovedCount(itemId)` 只作为明确入口，不由 BE-B 私自推 submission 状态。
- [ ] 测试 DRAFT 可覆盖导入，PUBLISHED 已领取题不可改，批量操作返回逐条结果。

### Task 7: 模板版本管理

**Files:**

- Create: `backend/src/main/java/com/labelhub/modules/template/controller/TemplateController.java`
- Create: `backend/src/main/java/com/labelhub/modules/template/service/TemplateService.java`
- Create: `backend/src/main/java/com/labelhub/modules/template/service/TemplateVersionService.java`
- Create: `backend/src/main/java/com/labelhub/modules/template/domain/TemplateVersionState.java`
- Test: `backend/src/test/java/com/labelhub/modules/template/TemplateVersionServiceTest.java`

- [ ] 创建模板：`POST /api/v1/tasks/{taskId}/templates`。
- [ ] 查询任务模板：`GET /api/v1/tasks/{taskId}/templates`。
- [ ] 查询版本：`GET /api/v1/template-versions/{versionId}`。
- [ ] fork 版本：`POST /api/v1/templates/{templateId}/fork`。
- [ ] 保存 schema 前调用 Schema 校验。
- [ ] `versionNo` 单调递增。
- [ ] `publishedSnapshot=true` 的版本不可原地修改。
- [ ] 测试已发布版本修改失败、fork 后生成新版本、BE-A 可按 versionId 读取 schema。

### Task 8: Schema 校验服务

**Files:**

- Create: `backend/src/main/java/com/labelhub/modules/template/controller/SchemaValidationController.java`
- Create: `backend/src/main/java/com/labelhub/modules/template/service/SchemaValidationService.java`
- Create: `backend/src/main/java/com/labelhub/modules/template/dto/SchemaValidationError.java`
- Create: `backend/src/main/java/com/labelhub/modules/template/dto/ValidateAnswerRequest.java`
- Test: `backend/src/test/java/com/labelhub/modules/template/SchemaValidationServiceTest.java`

- [ ] `validateSchema(schemaJson)` 校验布局和组件结构。
- [ ] `validateAnswer(schemaVersionId, answerJson)` 给 BE-A 提交时调用。
- [ ] required、enum、regex、JSON 字段校验。
- [ ] 检测重复 field。
- [ ] `ShowItem` 不允许出现在答案字段中。
- [ ] 返回错误路径，错误码使用 `409301`。
- [ ] 测试必填缺失、非法 enum、ShowItem 提交、schema 非法保存失败。

### Task 9: 奖励规则、奖励结算与贡献统计

**Files:**

- Create: `backend/src/main/java/com/labelhub/modules/reward/controller/RewardRuleController.java`
- Create: `backend/src/main/java/com/labelhub/modules/reward/controller/ContributionController.java`
- Create: `backend/src/main/java/com/labelhub/modules/reward/service/RewardRuleService.java`
- Create: `backend/src/main/java/com/labelhub/modules/reward/service/RewardSettlementService.java`
- Create: `backend/src/main/java/com/labelhub/modules/reward/service/ContributionStatsService.java`
- Modify: `backend/src/main/java/com/labelhub/modules/reward/domain/RewardDirection.java`
- Test: `backend/src/test/java/com/labelhub/modules/reward/RewardSettlementServiceTest.java`
- Test: `backend/src/test/java/com/labelhub/modules/reward/ContributionStatsServiceTest.java`

- [ ] 奖励规则保存：`POST /api/v1/tasks/{taskId}/reward-rule`。
- [ ] 奖励规则查询：`GET /api/v1/tasks/{taskId}/reward-rule`。
- [ ] 发布时冻结规则快照，历史流水不受后续规则变更影响。
- [ ] 消费 `SubmissionApproved`、`GoldenSelected`、`RewardReversed`。
- [ ] 幂等键：`SubmissionApproved + submissionId`、`GoldenSelected + conflictGroupId`。
- [ ] 同一 submission 先通过后又选为金标，不重复计奖。
- [ ] 冲正只追加负向流水，不删除正向流水。
- [ ] 使用 `reward_ledger.source_event_id` 唯一约束处理重复事件。
- [ ] 使用 `positive_submission_id` 和 `positive_assignment_id` 生成列处理同一 submission 或同一 assignment 多版本重复计奖。
- [ ] 贡献接口：overview、trend、tasks、ledger。
- [ ] 近 7 日趋势无数据日期补零。
- [ ] 测试重复事件不重复发奖励、累计奖励等于流水汇总、待审核不进入通过率分母。

### Task 10: 导出任务

**Files:**

- Create: `backend/src/main/java/com/labelhub/modules/export/controller/ExportController.java`
- Create: `backend/src/main/java/com/labelhub/modules/export/service/ExportJobService.java`
- Create: `backend/src/main/java/com/labelhub/modules/export/service/ExportFileWriter.java`
- Create: `backend/src/main/java/com/labelhub/modules/export/service/JsonExportFileWriter.java`
- Create: `backend/src/main/java/com/labelhub/modules/export/service/JsonlExportFileWriter.java`
- Create: `backend/src/main/java/com/labelhub/modules/export/service/CsvExportFileWriter.java`
- Create: `backend/src/main/java/com/labelhub/modules/export/service/ExcelExportFileWriter.java`
- Test: `backend/src/test/java/com/labelhub/modules/export/ExportJobServiceTest.java`

- [ ] 创建导出：`POST /api/v1/tasks/{taskId}/exports`。
- [ ] 查询历史：`GET /api/v1/tasks/{taskId}/exports`。
- [ ] 查询详情：`GET /api/v1/tasks/{taskId}/exports/{exportJobId}`。
- [ ] 默认只导出 `APPROVED + isGolden=true`。
- [ ] 导出数据必须调用 BE-A 的 `queryExportableGoldenSubmissions(taskId, pageRequest)`。
- [ ] 支持字段映射、JSON、JSONL、CSV、Excel。
- [ ] 可选包含 `aiReview`、`auditTrail`。
- [ ] 大文件分页读取，不一次性加载全部数据。
- [ ] 文件上传对象存储，返回签名下载链接。
- [ ] 测试不导出打回数据、导出来自 BE-A 快照、失败任务可查错误信息。

## 7. 内部对接接口

BE-B 给 BE-A 的 Java Service 能力：

```text
CurrentUserContext.getUserId()
CurrentUserContext.getRoles()
CurrentUserContext.getTokenVersion()
SystemPrincipalService.getSystemAgentProfile()
AuditAppender.append(AuditCommand)
DatasetSnapshotService.reserveClaimableItem(taskId, labelerId)
DatasetSnapshotService.getDatasetItemSnapshot(itemId)
DatasetSnapshotService.increaseSubmittedCount(itemId)
DatasetSnapshotService.increaseApprovedCount(itemId)
TemplateVersionService.getTemplateSchema(templateVersionId)
SchemaValidationService.validateAnswer(schemaVersionId, answerJson)
RedisLockService.withLock(key, waitTime, leaseTime, action)
RateLimitService.tryAcquire(key, permits)
AsyncJobService.submit(command)
```

BE-B 依赖 BE-A 的内部能力：

```text
queryExportableGoldenSubmissions(taskId, pageRequest)
```

BE-A 生产、BE-B 消费的事件：

```text
SubmissionApproved
GoldenSelected
RewardReversed
```

## 8. MVP 开发顺序

1. Auth/RBAC、Admin、`CurrentUserContext`
2. System Principal、AuditAppender、审计查询
3. Redis/Redisson、RateLimitService、AsyncJob
4. ObjectStorage、文件上传
5. Dataset 导入、题目列表、错误报告
6. Dataset 批量编辑、快照服务、领取预留入口
7. Template Version、Schema Validation
8. Reward Rule、事件幂等奖励结算、贡献统计
9. Export Job、字段映射、多格式文件生成
10. 补齐 API 契约文档、联调自测记录

## 9. 测试与验收命令

后端全量测试：

```powershell
cd backend
mvn -q test
```

跳过测试打包：

```powershell
cd backend
mvn -q -DskipTests package
```

每个模块最低测试项：

- Auth/RBAC：注册默认 Labeler、错误密码失败、旧 token 失效、未登录 401、无权限 403。
- Admin：修改角色、启用禁用、禁止移除最后一个 Admin、过滤 system 用户。
- Audit：追加审计、按 bizType/bizId 查询、AI 审计携带 agentRunId。
- Dataset：JSON/JSONL/Excel 导入、重复 externalId、单行失败不中断、错误报告、发布后已领取题不可改。
- Template/Schema：已发布版本不可改、fork 新版本、required/enum/regex/ShowItem 校验。
- Redis/Async：锁超时释放、限流命中、异步任务失败可查。
- Reward/Contribution：重复事件幂等、通过计奖、金标不重复计奖、冲正负流水、趋势补零。
- Export：只导出 APPROVED 金标、分页读取、字段映射、对象存储下载链接。

## 10. Git 提交与推送习惯

个人开发习惯必须写入每次执行约束：

- 每次修改代码后必须提交一次 commit，不把多个不相关功能混在同一个 commit。
- commit 信息必须使用中文，且能说明本次变更意图。
- 推荐 commit 格式：`类型: 中文说明`。
- 常用类型：`feat`、`fix`、`docs`、`test`、`refactor`、`chore`。
- 示例：`feat: 增加数据集导入任务状态查询接口`。
- 提交前必须检查工作区，只暂存本次任务相关文件，不提交无关改动。
- 提交前至少运行相关测试；如果无法运行，必须在最终说明里写明原因。
- 推送远程时只允许推送到远程 `yeqian` 分支。
- 推荐推送命令：`git push origin HEAD:yeqian`。
- 禁止在未明确要求时推送到 `main`、`master` 或其他队友分支。

执行代码变更时的最小流程：

```powershell
git status --short
git add <本次任务相关文件>
git commit -m "中文提交信息"
git push origin HEAD:yeqian
```

说明：文档类变更使用 `docs: 中文说明`；后端功能代码使用 `feat:` 或 `fix:`；测试补充使用 `test:`。

## 11. Definition of Done

BE-B 任一功能完成必须满足：

- 接口、状态、事件、错误码与协作手册一致。
- Controller 有权限校验。
- DTO 与 Entity 分离。
- 关键动作写审计或事件。
- 涉及金额、奖励、分数使用 `BigDecimal`。
- 文件不以二进制形式进入数据库。
- 异步任务失败保存错误信息和 traceId。
- 不输出密码、JWT、API Key、COS Secret。
- 关键类、公共方法、复杂业务规则、并发控制和跨模块边界已有规范注释。
- 核心路径有单元测试或集成测试。
- 修改接口时同步 `docs/api-contracts/`。
- 至少运行 `mvn -q test` 或说明无法运行的原因。
- 代码变更已按个人习惯提交 commit，commit 信息为中文。
