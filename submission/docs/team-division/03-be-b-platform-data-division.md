# BE-B 后端平台支撑与数据资产分工任务书

本文档按已完成后端交付重新组织 BE-B 分工。BE-B 负责 LabelHub 的平台底座、数据资产、模板契约、文件导出、奖励统计、看板聚合、审计通知和基础设施能力，为 FE 的多角色工作台和 BE-A 的审核智能业务引擎提供稳定支撑。

## 0. BE-B 目标

BE-B 的交付目标包括：

- Auth/RBAC、注册登录、当前用户、密码和资料维护。
- Admin 用户、角色、审核分配查询和平台看板。
- 数据集导入、题目预览、批量追加、批量更新、批量删除。
- 模板资源、模板版本、任务模板绑定和 Schema 校验。
- 答案 Schema 校验服务，供提交链路调用。
- 对象存储、文件上传和签名下载。
- 异步导出任务、直接导出、多格式文件生成和导出历史。
- 奖励规则、贡献统计、趋势统计、奖励流水和奖励结算。
- Owner、Labeler、Reviewer 多角色看板。
- 审计日志查询和统一追加能力。
- 媒体处理、通知流和未读消息。
- Redis/Redisson 锁、限流、队列和通用异步任务能力。
- 数据库迁移安全、命名和注释校验。

BE-B 不直接替代 BE-A 推进提交审核结论，也不在前端层面组织页面交互。BE-B 的职责是提供平台数据、校验、存储、统计和基础设施能力。

## 1. Auth/RBAC 与 Admin 模块

### 1.1 大功能：注册、登录与当前用户

交付效果：

- 用户可以注册和登录。
- 登录后后端可以识别 userId、角色和 token 状态。
- 业务接口通过统一安全上下文判断访问权限。

小功能：

1. 注册
   - 用户名唯一。
   - 邮箱唯一。
   - 密码加密保存。
   - 默认进入普通标注员角色。

2. 登录
   - 校验用户名或邮箱。
   - 校验密码。
   - 签发 accessToken。
   - 返回用户基本信息和角色信息。

3. Token 刷新
   - 使用 refresh 接口换取新 token。
   - tokenVersion 用于控制旧 token 可用性。

4. 当前用户
   - 查询当前登录用户。
   - 修改密码。
   - 修改个人资料。

已完成实现：

- `AuthController`
- `AuthService`
- `SystemPrincipalService`
- `SecurityConfigCorsTest`
- `SecurityConfigSwaggerTest`
- `GlobalExceptionHandlerTest`

已完成接口：

```Plaintext
POST /api/v1/auth/register
POST /api/v1/auth/login
POST /api/v1/auth/refresh
GET /api/v1/users/me
PUT /api/v1/users/me/password
PUT /api/v1/users/me/profile
```

验收：

```Plaintext
用户可以注册。
错误密码不能登录。
登录成功返回 token 和用户信息。
当前用户接口能返回 userId 和 roles。
修改密码后旧登录凭据按 tokenVersion 规则处理。
```

### 1.2 大功能：Admin 用户管理

交付效果：

- Admin 可以管理平台用户、角色和账号状态。
- 系统用户不作为普通用户维护对象展示。

小功能：

1. 用户列表
   - 分页查询。
   - 搜索用户。
   - 展示角色。
   - 展示启用状态。

2. 账号状态
   - 启用用户。
   - 禁用用户。
   - 禁用后阻止登录或业务访问。

3. 角色维护
   - 修改用户角色。
   - 授权 Owner。
   - 授权 Reviewer。
   - 保持 Admin 管理安全约束。

4. Reviewer 创建
   - Admin 可创建 Reviewer 用户。
   - 用于审核队列和任务分配。

已完成实现：

- `AdminUserController`
- `AdminUserService`
- `AdminUserControllerTest`
- `AdminUserServiceTest`

已完成接口：

```Plaintext
GET /api/v1/admin/users
POST /api/v1/admin/users/{userId}/enable
POST /api/v1/admin/users/{userId}/disable
POST /api/v1/admin/users/reviewers
PUT /api/v1/admin/users/{userId}/roles
```

验收：

```Plaintext
Admin 能查询用户列表。
Admin 能启用和禁用用户。
Admin 能修改用户角色。
非 Admin 不能调用用户管理接口。
```

### 1.3 大功能：Admin 审核分配查询

交付效果：

- Admin 可以查看哪些任务可分配审核员、哪些 Reviewer 可被分配，以及 Reviewer 当前进度。
- 该模块服务于平台运维和审核资源管理。

小功能：

1. 可分配任务
   - 查询需要审核资源的任务。
   - 返回任务摘要和待审量。

2. 可分配 Reviewer
   - 查询具备 Reviewer 角色的用户。
   - 返回可用于分配的基础信息。

3. Reviewer 进度
   - 查询 Reviewer 的审核进度。
   - 展示处理量、待处理量和任务关系。

已完成实现：

- `AdminReviewAssignmentController`
- `AdminReviewAssignmentQueryService`
- `AdminReviewAssignmentControllerTest`
- `AdminReviewAssignmentQueryServiceTest`

已完成接口：

```Plaintext
GET /api/v1/admin/review/tasks/assignable
GET /api/v1/admin/review/reviewers/assignable
GET /api/v1/admin/review/reviewers/progress
```

验收：

```Plaintext
Admin 能查询可分配审核任务。
Admin 能查询可分配 Reviewer。
Admin 能查看 Reviewer 审核进度。
非 Admin 调用返回权限错误。
```

### 1.4 大功能：平台看板

交付效果：

- Admin 能查看平台级任务、用户、提交、审核和 AI 运行摘要。
- 看板只做只读聚合，不改变业务状态。

小功能：

1. 总览指标
   - 用户数。
   - 任务数。
   - 提交数。
   - 审核处理量。
   - AI 执行摘要。

2. 时间范围
   - 支持按 range 查询。
   - 默认范围在后端处理。

3. DTO 稳定性
   - 返回结构有独立 DTO。
   - 测试覆盖 DTO schema 和 range 解析。

已完成实现：

- `AdminDashboardController`
- `AdminDashboardService`
- `AdminDashboardControllerTest`
- `AdminDashboardServiceTest`
- `AdminDashboardRangeTest`
- `AdminDashboardDtoSchemaTest`

已完成接口：

```Plaintext
GET /api/v1/admin/dashboard/overview
```

验收：

```Plaintext
Admin 能查询平台总览。
range 参数能被正确解析。
返回 DTO 字段稳定。
看板查询不修改业务数据。
```

## 2. 数据资产模块

### 2.1 大功能：数据集导入

交付效果：

- Owner 可以把原始数据导入为任务题目资产。
- 导入过程保存导入任务、导入状态、错误信息和题目数据。

小功能：

1. 导入任务
   - 按 taskId 创建导入任务。
   - 上传文件或提交数据。
   - 保存导入状态。
   - 返回 jobId。

2. 导入格式
   - JSON。
   - JSONL。
   - Excel。
   - 通用 itemJson 结构。

3. 错误处理
   - 单行解析错误形成错误记录。
   - 返回错误数量和错误摘要。
   - 保存错误报告文件引用。

4. 覆盖导入
   - 支持任务数据覆盖导入接口。
   - 后端按任务状态和数据状态做约束。

已完成实现：

- `DatasetImportController`
- `DatasetImportService`
- `DatasetParserTest`
- `DatasetImportServiceTest`
- `ObjectFileEntity`

已完成接口：

```Plaintext
POST /api/v1/tasks/{taskId}/imports
POST /api/v1/tasks/{taskId}/imports/overwrite
GET /api/v1/tasks/{taskId}/imports/{jobId}
```

验收：

```Plaintext
Owner 可以创建数据导入任务。
JSON、JSONL、Excel 数据能进入解析流程。
导入任务状态可查询。
错误行会被记录并返回摘要。
```

### 2.2 大功能：题目管理与批量操作

交付效果：

- Owner 能查看任务题目，并在允许范围内进行批量追加、更新和删除。
- 已进入标注处理的数据由后端保护，避免修改破坏提交追溯。

小功能：

1. 题目列表
   - 按 taskId 分页查询。
   - 展示 externalId。
   - 展示 itemJson 摘要。
   - 展示当前处理状态。

2. 批量追加
   - 支持结构化请求追加。
   - 支持 JSON 数据追加。
   - 逐条校验。
   - 返回成功数和失败原因。

3. 批量更新
   - 按 itemId 更新 itemJson 或元数据。
   - 校验题目是否可编辑。
   - 返回逐条结果。

4. 批量删除
   - 按 itemId 删除或标记删除。
   - 校验题目是否可删除。
   - 返回逐条结果。

5. 数据集快照能力
   - 供 BE-A 领取、提交、审核时读取 itemJson。
   - 供任务市场和看板聚合统计。

已完成实现：

- `DatasetItemController`
- `DatasetItemService`
- `DatasetItemMapper`
- `DatasetSnapshotService`
- `DefaultDatasetClaimService`
- `DefaultDatasetMarketStatsService`
- `DatasetItemServiceTest`
- `DatasetItemControllerTest`
- `DatasetItemRequestValidationTest`

已完成接口：

```Plaintext
GET /api/v1/tasks/{taskId}/items
POST /api/v1/tasks/{taskId}/items/batch-append
POST /api/v1/tasks/{taskId}/items/batch-append-json
POST /api/v1/tasks/{taskId}/items/batch-update
POST /api/v1/tasks/{taskId}/items/batch-delete
```

验收：

```Plaintext
Owner 能分页查看任务题目。
批量追加返回逐条处理结果。
批量更新会校验题目当前处理状态。
批量删除会校验题目当前处理状态。
BE-A 能读取稳定 itemJson 快照。
```

## 3. 模板资源与 Schema 校验模块

### 3.1 大功能：Owner 模板库

交付效果：

- Owner 可以维护自己的模板资源。
- 模板可在任务之间复用，并可进入前端 Designer 编辑。

小功能：

1. 创建模板
   - 模板名称。
   - 模板描述。
   - Owner 归属。

2. 查询模板
   - 查询 Owner 模板列表。
   - 返回模板摘要。
   - 前端模板库使用。

3. fork 模板
   - 基于已有模板生成新模板。
   - 保留 schema 快照。

已完成实现：

- `TemplateController`
- `TemplateService`
- `DefaultTemplateSchemaService`
- `V24__templates_owner_id.sql`
- `V27__owner_template_library.sql`
- `TemplateVersionServiceTest`

已完成接口：

```Plaintext
POST /api/v1/owner/templates
GET /api/v1/owner/templates
POST /api/v1/templates/{templateId}/fork
```

验收：

```Plaintext
Owner 能创建模板。
Owner 能查询自己的模板库。
模板 fork 会生成新的模板资源。
模板归属字段用于权限校验。
```

### 3.2 大功能：任务模板版本

交付效果：

- 任务可以绑定模板版本。
- 模板版本保存 schemaJson，并作为提交答案校验契约。

小功能：

1. 任务模板保存
   - 按 taskId 保存模板。
   - 保存 schemaJson。
   - 生成版本记录。
   - 保存 Owner 归属。

2. 版本查询
   - 查询任务模板版本。
   - 查询指定 versionId。
   - 返回 schemaJson 给前端 Renderer 或 BE-A 提交校验。

3. 版本服务
   - `TemplateVersionService` 维护版本递增。
   - 保存前调用 schema 校验。
   - 已关联任务的模板通过版本快照保持稳定。

已完成实现：

- `TemplateController`
- `TemplateVersionService`
- `TemplateSchemaService`
- `V25__template_versions_owner_id.sql`
- `TemplateVersionServiceTest`
- `DefaultTemplateSchemaServiceTest`

已完成接口：

```Plaintext
POST /api/v1/tasks/{taskId}/templates
GET /api/v1/tasks/{taskId}/templates
GET /api/v1/template-versions/{versionId}
```

验收：

```Plaintext
Owner 能为任务保存模板。
任务模板版本可查询。
versionId 能返回对应 schemaJson。
模板版本保存前会校验 schema 结构。
```

### 3.3 大功能：Schema 校验服务

交付效果：

- 后端提供统一 Schema 校验和答案校验能力。
- BE-A 提交链路调用 `AnswerSchemaValidator`，保证正式提交符合模板契约。

小功能：

1. Schema 校验
   - 校验 schemaJson 基本结构。
   - 校验字段重复。
   - 校验组件类型。
   - 校验 required、enum、regex 等约束结构。

2. 答案校验
   - 按 schemaVersionId 读取 schema。
   - 校验 answerJson 字段。
   - 校验 required。
   - 校验 enum。
   - 校验 regex。
   - 返回字段路径和错误消息。

3. 展示字段处理
   - 展示类字段用于渲染题目内容。
   - 展示类字段不作为普通答案字段要求提交。

已完成实现：

- `SchemaValidationController`
- `SchemaValidationService`
- `AnswerSchemaValidator`
- `TemplateSchemaValidator`
- `SchemaValidationServiceTest`

已完成接口：

```Plaintext
POST /api/v1/schema/validate-answer
```

验收：

```Plaintext
非法 schema 保存会被拒绝。
必填缺失返回字段路径。
非法枚举值返回字段路径。
非法正则格式返回字段路径。
BE-A 正式提交能复用答案校验服务。
```

## 4. 文件存储与对象文件模块

### 4.1 大功能：文件上传

交付效果：

- 前端可以上传数据集文件、标注附件文件或其他业务文件。
- 文件二进制不直接存入业务表，业务表保存对象文件引用。

小功能：

1. 上传
   - multipart 文件上传。
   - 生成 objectKey。
   - 保存文件元数据。
   - 返回 fileId。

2. 元数据
   - 文件名。
   - contentType。
   - size。
   - bucket。
   - objectKey。
   - createdBy。

3. 下载授权
   - 根据 fileId 生成签名 URL。
   - 控制 URL 有效期。

已完成实现：

- `FileController`
- `FileService`
- `ObjectFileEntity`
- `ObjectStorageService`
- `CosObjectStorageService`
- `FileControllerTest`
- `FileServiceTest`
- `ObjectStorageServiceTest`

已完成接口：

```Plaintext
POST /api/v1/files/upload
GET /api/v1/files/{fileId}/signed-url
```

验收：

```Plaintext
文件可上传并返回 fileId。
文件元数据能入库。
签名 URL 可生成。
业务表只保存文件引用。
```

## 5. 导出模块

### 5.1 大功能：异步导出任务

交付效果：

- Owner 可以创建任务导出作业。
- 后端生成导出文件、记录状态，并提供历史查询。

小功能：

1. 创建导出
   - 按 taskId 创建导出任务。
   - 选择导出格式。
   - 选择包含字段。
   - 写入 export job。

2. 导出执行
   - 查询 BE-A 提供的审核通过提交快照。
   - 根据格式生成文件。
   - 上传对象存储。
   - 更新 job 状态和 fileId。

3. 导出历史
   - 查询任务导出列表。
   - 查询单个导出作业。
   - 返回状态、创建时间、完成时间和下载信息。

已完成实现：

- `ExportController`
- `ExportJobService`
- `SubmissionExportQueryService`
- `ExportJobServiceTest`
- `ExportControllerTest`

已完成接口：

```Plaintext
POST /api/v1/tasks/{taskId}/exports
GET /api/v1/tasks/{taskId}/exports
GET /api/v1/tasks/{taskId}/exports/{exportJobId}
```

验收：

```Plaintext
Owner 能创建导出任务。
导出任务状态可查询。
导出成功后保存文件引用。
导出数据来自审核通过提交快照。
```

### 5.2 大功能：直接导出

交付效果：

- 对于演示或轻量数据，Owner 可以触发直接导出接口。
- 直接导出仍沿用后端权限和数据范围校验。

小功能：

1. 直接导出请求
   - 按 taskId 请求。
   - 选择格式。
   - 读取导出快照。

2. 文件生成
   - JSON。
   - JSONL。
   - CSV。
   - Excel。

3. 响应
   - 返回文件结果或下载信息。
   - 出错时返回明确错误。

已完成实现：

- `TaskExportController`
- `DirectTaskExportService`
- `TaskExportControllerTest`

已完成接口：

```Plaintext
POST /api/v1/tasks/{taskId}/exports/direct
```

验收：

```Plaintext
Owner 能触发直接导出。
直接导出仍校验任务归属。
导出格式与请求参数一致。
审核未通过的数据不进入默认导出范围。
```

## 6. 奖励与贡献统计模块

### 6.1 大功能：奖励规则

交付效果：

- Owner 可以为任务配置虚拟奖励规则。
- Labeler 工作台和任务市场可以展示奖励摘要。

小功能：

1. 保存规则
   - 单条通过奖励。
   - 奖励名称。
   - 是否对 Labeler 展示。
   - 任务归属校验。

2. 查询规则
   - 按 taskId 查询奖励配置。
   - 用于任务编辑页回显。
   - 用于市场奖励摘要。

3. 并发控制
   - 保存规则时使用锁服务保护任务维度更新。
   - 保持规则写入的一致性。

已完成实现：

- `RewardRuleController`
- `RewardRuleService`
- `RewardSummaryService`
- `DefaultRewardSummaryService`
- `RewardRuleControllerTest`
- `RewardRuleServiceTest`

已完成接口：

```Plaintext
POST /api/v1/tasks/{taskId}/reward-rule
GET /api/v1/tasks/{taskId}/reward-rule
```

验收：

```Plaintext
Owner 能保存奖励规则。
Owner 能查询奖励规则。
任务市场能读取奖励摘要。
规则保存会校验任务归属。
```

### 6.2 大功能：奖励结算

交付效果：

- 人工审核通过后，BE-B 生成虚拟奖励流水。
- 奖励结算保持幂等，避免重复奖励。

小功能：

1. 结算入口
   - 接收审核通过后的业务调用。
   - 读取任务奖励规则。
   - 读取提交人。

2. 流水生成
   - 正向奖励流水。
   - 幂等键。
   - 关联 taskId、submissionId、labelerId。

3. 统计更新
   - 更新贡献统计。
   - 更新累计奖励。
   - 写入审计记录。

已完成实现：

- `RewardSettlementService`
- `RewardSettlementServiceTest`
- `ContributionStatsService`

验收：

```Plaintext
审核通过后能生成奖励流水。
同一 submission 不重复生成正向奖励。
打回不产生正向奖励。
奖励流水能被 Labeler 查询。
```

### 6.3 大功能：Labeler 贡献统计

交付效果：

- Labeler 能查看自己的贡献概览、趋势、任务明细和奖励流水。

小功能：

1. 概览
   - 已领取。
   - 已提交。
   - 待审核。
   - 通过。
   - 打回。
   - 通过率。
   - 累计奖励。

2. 趋势
   - 今日提交。
   - 近 7 日趋势。
   - 无数据日期补零。

3. 任务明细
   - 各任务贡献。
   - 通过率。
   - 奖励汇总。

4. 奖励流水
   - 奖励来源。
   - 金额。
   - 创建时间。
   - 关联任务和提交。

已完成实现：

- `ContributionController`
- `ContributionStatsService`
- `ContributionControllerTest`
- `ContributionStatsServiceTest`

已完成接口：

```Plaintext
GET /api/v1/labeler/contribution/overview
GET /api/v1/labeler/contribution/trend
GET /api/v1/labeler/contribution/tasks
GET /api/v1/labeler/rewards/ledger
```

验收：

```Plaintext
Labeler 能查询贡献概览。
通过数和打回数按审核结果聚合。
待审核不计入通过率分母。
奖励流水汇总等于累计奖励。
```

## 7. 多角色看板模块

### 7.1 大功能：Owner 看板

交付效果：

- Owner 能查看自己任务范围内的进度、质量和待处理摘要。

小功能：

1. 概览指标
   - 任务数量。
   - 题目数量。
   - 领取进度。
   - 提交进度。
   - 审核进度。
   - AI 摘要。

2. 待处理事项
   - 待发布任务。
   - 待导入数据。
   - 待配置模板或 AI。
   - 待审核相关摘要。

已完成实现：

- `OwnerDashboardController`
- `OwnerDashboardService`
- `OwnerDashboardControllerTest`
- `OwnerDashboardServiceTest`

已完成接口：

```Plaintext
GET /api/v1/owner/dashboard/overview
```

验收：

```Plaintext
Owner 能查询自己的看板总览。
Owner 看板不返回其他 Owner 的任务数据。
看板查询不修改任务状态。
```

### 7.2 大功能：Labeler 看板

交付效果：

- Labeler 能查看个人参与、提交、审核结果和奖励摘要。

小功能：

1. 概览指标
   - 当前任务。
   - 已领取。
   - 已提交。
   - 待审核。
   - 通过。
   - 打回。
   - 奖励。

2. 工作入口
   - 任务市场入口。
   - 作答工作台入口。
   - 我的提交入口。

已完成实现：

- `LabelerDashboardController`
- `LabelerDashboardService`
- `LabelerDashboardControllerTest`

已完成接口：

```Plaintext
GET /api/v1/labeler/dashboard/overview
```

验收：

```Plaintext
Labeler 能查询个人看板。
Labeler 看板只返回当前用户相关数据。
看板能支撑前端角色首页展示。
```

### 7.3 大功能：Reviewer 看板

交付效果：

- Reviewer 能查看审核任务、待处理量、已处理量和 AI 相关摘要。

小功能：

1. 概览指标
   - 待审任务。
   - 待审提交。
   - 已处理提交。
   - 打回数量。
   - AI 建议分布。

2. 工作入口
   - 审核队列。
   - 审核历史。
   - 任务领取入口。

已完成实现：

- `ReviewerDashboardController`
- `ReviewerDashboardService`
- `ReviewerDashboardControllerTest`

已完成接口：

```Plaintext
GET /api/v1/reviewer/dashboard/overview
```

验收：

```Plaintext
Reviewer 能查询个人审核看板。
Reviewer 看板只返回当前审核范围内数据。
看板能支撑审核队列入口展示。
```

## 8. 审计、媒体与通知模块

### 8.1 大功能：审计日志

交付效果：

- 平台关键业务动作有审计记录。
- 前端或审核详情可以按业务对象查询时间线。

小功能：

1. 统一追加
   - `AuditAppender`
   - actorType。
   - actorId。
   - bizType。
   - bizId。
   - action。
   - beforeJson。
   - afterJson。
   - traceId。
   - agentRunId。

2. 查询
   - 按 bizType 查询。
   - 按 bizId 查询。
   - 分页返回。
   - 供审核追溯或详情页展示。

已完成实现：

- `AuditLogController`
- `AuditLogService`
- `AuditAppender`
- `AuditLogControllerTest`

已完成接口：

```Plaintext
GET /api/v1/audit-logs
```

验收：

```Plaintext
业务动作能通过 AuditAppender 写审计。
审计查询能按业务对象返回记录。
AI 相关审计能关联 agentRunId。
审计记录保持追加式写入。
```

### 8.2 大功能：媒体处理

交付效果：

- 平台能维护题目关联媒体资产、处理状态和 AI Prompt 上下文。
- 媒体上下文可为预标注或 AI 辅助提供素材摘要。

小功能：

1. 媒体处理
   - 记录媒体处理状态。
   - 刷新上下文。
   - 查询处理结果。

2. Prompt 上下文
   - `DefaultMediaPromptContextBuilder`
   - 组织媒体摘要。
   - 提供给 AI 相关模块使用。

已完成实现：

- `MediaProcessingController`
- `MediaProcessingService`
- `DefaultMediaPromptContextBuilder`
- `VideoKeyFrameService`

已完成接口：

```Plaintext
POST /api/v1/dataset-items/{itemId}/media/process
GET /api/v1/dataset-items/{itemId}/media-context
GET /api/v1/media-processing/jobs/{jobId}
```

验收：

```Plaintext
数据项媒体能触发处理。
媒体上下文可查询。
媒体处理任务状态可查询。
媒体上下文能被 AI Prompt 构造能力读取。
```

### 8.3 大功能：通知

交付效果：

- 平台提供通知流、通知历史和未读数，支撑任务、审核和系统消息提醒。

小功能：

1. 通知流
   - SSE 连接。
   - 按当前用户推送。

2. 通知历史
   - 分页查询。
   - 按已读状态查询。

3. 未读数
   - 查询当前用户未读数量。
   - 标记已读。

已完成实现：

- `NotificationController`
- `NotificationService`

已完成接口：

```Plaintext
GET /api/v1/notifications/stream
GET /api/v1/notifications
GET /api/v1/notifications/unread-count
POST /api/v1/notifications/{notificationId}/read
POST /api/v1/notifications/read-all
```

验收：

```Plaintext
用户能建立通知流。
用户能查询通知历史。
用户能查询未读数。
用户能标记通知已读。
```

## 9. 平台基础设施模块

### 9.1 大功能：Redis/Redisson 锁与限流

交付效果：

- 领取、奖励规则保存、AI 调用等需要并发控制的场景可以复用统一 Redis/Redisson 能力。

小功能：

1. 锁服务
   - `RedisLockService`
   - `RedissonRedisLockService`
   - 支持 tryLock。
   - 支持自动释放。

2. 限流服务
   - `RateLimitService`
   - `RedissonRateLimitService`
   - 支持按 key 限流。
   - 支持 AI Provider 或任务维度限流。

3. Key 规范
   - AI 审核 Redis key。
   - assignment 草稿 key。
   - 任务维度锁 key。

已完成实现：

- `RedisLockService`
- `RedissonRedisLockService`
- `RateLimitService`
- `RedissonRateLimitService`
- `AiReviewRedisKeyBuilderTest`
- `RedisLockServiceTest`
- `RedissonRedisLockServiceTest`
- `RateLimitServiceTest`

验收：

```Plaintext
锁服务能用于领取和奖励规则保存。
锁超时后可释放。
限流服务能对指定 key 生效。
Redis key 构造规则有测试覆盖。
```

### 9.2 大功能：AI 队列与通用异步任务

交付效果：

- AI 审核队列和通用异步任务能够支撑导入、AI 和导出等耗时操作。

小功能：

1. AI 队列
   - `AiReviewQueueService`
   - `RedissonAiReviewQueueService`
   - 投递 AI 审核消息。
   - 消费 AI 审核消息。

2. 通用 LLM 队列
   - `LlmTaskQueueService`
   - `RedissonLlmTaskQueueService`
   - 支撑 AI 审核、字段级辅助和预标注。

3. 通用异步任务
   - `AsyncJobService`
   - 提交任务。
   - 更新状态。
   - 保存错误信息。

已完成实现：

- `AiReviewQueueService`
- `RedissonAiReviewQueueService`
- `LlmTaskQueueService`
- `RedissonLlmTaskQueueService`
- `AsyncJobService`
- `RedissonAiReviewQueueServiceTest`
- `AsyncJobServiceTest`

验收：

```Plaintext
AI 审核消息可投递。
LLM 任务消息可投递。
异步任务失败能保存错误信息。
异步任务状态可查询。
```

### 9.3 大功能：数据库迁移质量

交付效果：

- 数据库迁移文件命名、注释和安全规则可被测试保护。
- 当前迁移序列包含 V39，提交材料按当前迁移号描述。

小功能：

1. 命名检查
   - 迁移文件版本号。
   - 文件命名格式。
   - 重复版本检测。

2. 安全检查
   - 危险 DDL 识别。
   - 必要索引和约束检查。
   - 迁移顺序检查。

3. 注释检查
   - 表注释。
   - 字段注释。
   - 关键业务字段说明。

已完成实现：

- `DatabaseMigrationNamingTest`
- `DatabaseMigrationSafetyTest`
- `DatabaseCommentMigrationTest`
- `MyBatisAnnotationSqlTest`
- `backend/src/main/resources/db/migration/V39__submission_review_claim_indexes.sql`

验收：

```Plaintext
迁移文件命名测试通过。
迁移安全测试通过。
关键表和字段注释测试通过。
当前迁移口径包含 V39。
```

## 10. BE-B 与其他分工边界

### 10.1 与 FE 的边界

- FE 负责页面和交互。
- BE-B 提供 Auth、数据集、模板、文件、导出、奖励、看板、审计、通知等接口。
- FE 的表单校验只做体验提示，正式 Schema 和答案校验以后端返回为准。

### 10.2 与 BE-A 的边界

- BE-A 负责任务流转、提交、AI 预审和人工审核。
- BE-B 提供题目资产、模板契约、答案校验、文件、导出、奖励和统计支撑。
- BE-A 审核通过后，BE-B 根据业务数据结算奖励和聚合统计。
- BE-B 导出模块读取 BE-A 提供的审核通过提交快照，再生成文件。

### 10.3 当前关键迁移

BE-B 相关关键迁移包括：

- `V1__baseline.sql`
- `V2__seed_system_agent.sql`
- `V3__add_table_column_comments.sql`
- `V21__remove_dataset_type.sql`
- `V24__templates_owner_id.sql`
- `V25__template_versions_owner_id.sql`
- `V27__owner_template_library.sql`
- `V28__single_labeler_dataset_item_status.sql`
- `V29__llm_providers_admin_global.sql`
- `V33__ai_observability_trace_metrics.sql`
- `V34__review_task_claims.sql`
- `V37__submission_created_by.sql`
- `V38__task_assigned_labeler.sql`
- `V39__submission_review_claim_indexes.sql`

## 11. BE-B 自测清单

```Plaintext
AuthService 和 AuthController 相关测试覆盖注册登录、当前用户和鉴权错误。
AdminUserServiceTest 覆盖用户角色和账号状态管理。
AdminReviewAssignmentQueryServiceTest 覆盖审核分配查询。
AdminDashboardServiceTest 覆盖平台看板聚合。
DatasetParserTest 覆盖数据格式解析。
DatasetImportServiceTest 覆盖导入任务。
DatasetItemServiceTest 覆盖题目批量操作和状态约束。
TemplateVersionServiceTest 覆盖模板版本保存和查询。
SchemaValidationServiceTest 覆盖 schema 与 answerJson 校验。
FileServiceTest 和 FileControllerTest 覆盖上传和签名下载。
ExportJobServiceTest 和 ExportControllerTest 覆盖异步导出。
TaskExportControllerTest 覆盖直接导出。
RewardRuleServiceTest 覆盖奖励规则。
RewardSettlementServiceTest 覆盖奖励结算幂等。
ContributionStatsServiceTest 覆盖贡献统计。
OwnerDashboardControllerTest、LabelerDashboardControllerTest、ReviewerDashboardControllerTest 覆盖角色看板。
AuditLogControllerTest 覆盖审计查询。
RedisLockServiceTest、RedissonRedisLockServiceTest、RateLimitServiceTest 覆盖锁和限流。
RedissonAiReviewQueueServiceTest 和 AsyncJobServiceTest 覆盖异步基础设施。
DatabaseMigrationNamingTest、DatabaseMigrationSafetyTest、DatabaseCommentMigrationTest 覆盖迁移质量。
```
