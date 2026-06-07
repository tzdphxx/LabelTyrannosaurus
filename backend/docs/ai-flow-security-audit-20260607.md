# LabelHub 接口流程与安全审查报告 - 2026-06-07

## 1. 审查范围

- 后端地址：`http://localhost:8080`
- 仓库范围：`backend` 代码只读审查
- 本轮唯一文件改动：新增/维护本审查文档
- 测试数据前缀：`codex-e2e-ai-20260607-*`
- 重点范围：接口流程、AI 预标注、AI 审核、Reviewer 权限、导出、课题要求差距、安全漏洞

## 2. 账号验证

| 角色 | 账号 | 结果 | 说明 |
| --- | --- | --- | --- |
| Owner | `hgb / 87654321` | 通过 | `/api/v1/users/me` 返回 `OWNER` |
| Admin | `1@qq.com / 12345678` | 通过 | `/api/v1/users/me` 返回 `ADMIN` |
| Labeler | `lrt / 123456` | 失败 | 当前数据库返回 401，账号或密码错误 |
| 临时 Labeler | `codex_labeler_* / Codex123456` | 通过 | 通过公开注册接口创建，用于流程测试 |
| 临时 Reviewer | `codex_reviewer_* / Codex123456` | 通过 | 通过 Admin 创建 Reviewer 接口创建 |

注意：登录接口真实字段是 `account`，不是计划里的 `usernameOrEmail`。使用 `usernameOrEmail` 会触发 400 参数校验错误。

## 3. 端到端流程结果

| 流程 | 结果 | 关键证据 |
| --- | --- | --- |
| 健康检查 | 通过 | `GET /actuator/health` 返回 `UP` |
| OpenAPI 基线 | 通过 | `/v3/api-docs` 暴露 117 个路径 |
| Owner 建任务 | 通过 | 创建任务 `910287`，标题 `codex-e2e-ai-20260607-1780821709` |
| 发布前置校验 | 通过 | 缺数据/模板/配置时发布失败，补齐后发布成功 |
| 模板创建 | 通过 | 模板版本 `920067`，包含 `ShowItem`、文本、单选、JSON、`LLMTrigger` |
| 数据追加 | 通过 | 创建数据项 `930717`、`930718`；第二条包含 prompt injection 测试文本 |
| 奖励规则 | 通过 | 规则 `960082`，`APPROVED_ITEM`，`1.00 POINT` |
| AI 审核配置 | 通过 | 配置 `58`，Provider `14`，模型 `qwen-plus`，`MANUAL_FIRST`，未开启 AI 直通过/直拒 |
| 任务发布 | 通过 | 任务 `910287` 状态变为 `PUBLISHED` |
| Labeler 任务广场/详情/模板 | 通过 | 临时标注员可看到任务并读取答题模板 |
| Labeler 领取 | 通过 | Assignment `940183`，数据项 `930717`，草稿版本 `1` |
| 草稿保存 | 通过 | 草稿版本 `1 -> 2`，状态变为 `DRAFTING` |
| 草稿版本冲突 | 通过 | 使用旧 `clientVersion` 返回 409 |
| AI 预标注 | 失败 | 预标注 `12` 创建成功，但最终 `FAILED`，`errorCode=LLM_EXCEPTION`，AgentRun `714` 失败 |
| LLM 字段触发 | 部分通过 | Trigger run `1` 创建成功，但测试窗口内一直 `RUNNING`，AgentRun `715` 无输出 |
| Labeler 提交 | 通过 | Submission `950136`，状态进入 `AI_REVIEWING`，AgentRun `716` |
| AI 审核 | 失败 | AI 审核结果状态 `FAILED`，无 `decision`、分数、`flowAction`、输出快照 |
| Reviewer 查询 | 有安全问题 | 未领取任务的 Reviewer 也能读取 AI 审核、预标注、submission 详情 |
| Reviewer 审核动作 | 通过 | 未分配 Reviewer 执行 approve/reject 被拒绝，返回 `403601` |
| 导出 | 失败 | Owner 创建导出返回 400：审计命令不合法，未创建导出任务 |

## 4. 接口测试矩阵

| 模块 | 接口 | 测试角色 | 结果 | 说明 |
| --- | --- | --- | --- | --- |
| health | `GET /actuator/health` | 未登录 | 通过 | 返回 `UP` |
| auth | `POST /api/v1/auth/login` | Owner/Admin/临时 Labeler | 通过 | 请求字段必须为 `account` |
| auth | `POST /api/v1/auth/login` | 提供的 `lrt` | 失败 | 当前数据库 401 |
| auth | `GET /api/v1/users/me` | 已登录用户 | 通过 | 角色返回正确 |
| admin | `GET /api/v1/admin/users` | Admin | 失败 | 返回 400：用户必须且只能拥有一个角色 |
| admin | `POST /api/v1/admin/users/reviewers` | Admin | 通过 | 真实创建路径是 `/reviewers` |
| task | `POST /api/v1/tasks` | Owner | 通过 | 创建任务 `910287` |
| task | `POST /api/v1/tasks/{taskId}/publish` | Owner | 通过 | 前置校验与成功发布均验证 |
| template | `POST /api/v1/tasks/{taskId}/templates` | Owner | 通过 | `schemaJson` 必须是对象；传字符串会 500 |
| dataset | `POST /api/v1/tasks/{taskId}/items/batch-append-json` | Owner | 通过 | `itemJson`/`metadataJson` 必须是对象；传字符串会 500 |
| ai config | `POST /api/v1/tasks/{taskId}/ai-review-configs` | Owner | 通过 | 配置 ID `58` |
| market | `GET /api/v1/market/tasks` | Labeler | 通过 | 已发布任务可见 |
| claim | `POST /api/v1/tasks/{taskId}/items/claim` | Labeler | 通过 | Assignment `940183` |
| draft | `PUT /api/v1/claims/{claimId}/draft` | Labeler | 通过 | 版本冲突返回 409 |
| preannotation | `POST /api/v1/assignments/{assignmentId}/pre-annotations/run` | Labeler | 失败 | 任务创建成功，但最终 LLM 异常 |
| llm trigger | `POST /api/v1/assignments/{assignmentId}/llm-triggers` | Labeler | 部分通过 | 创建成功但长期 `RUNNING` |
| submit | `POST /api/v1/claims/{claimId}/submit` | Labeler | 通过 | Submission `950136` |
| ai review | `GET /api/v1/submissions/{submissionId}/ai-review` | Owner | 失败 | AI 审核状态 `FAILED` |
| ai review | `GET /api/v1/submissions/{submissionId}/ai-review` | Labeler | 通过 | 被拒绝，业务码 `403703` |
| ai review | `GET /api/v1/submissions/{submissionId}/ai-review` | 未分配 Reviewer | 安全失败 | 读取成功，不应允许 |
| agent run | `GET /api/v1/agent-runs/{agentRunId}` | Owner | 通过 | 可见输入快照；失败任务无输出快照 |
| agent run | `GET /api/v1/agent-runs/{agentRunId}` | 未分配 Reviewer | 通过 | 被拒绝 |
| preannotation | `GET /api/v1/pre-annotations/{id}` | 未分配 Reviewer | 安全失败 | 读取成功，不应允许 |
| reviewer | `GET /api/v1/reviewer/submissions/{submissionId}` | 未分配 Reviewer | 安全失败 | 详情读取成功，不应允许 |
| reviewer | `POST /api/v1/reviewer/submissions/{submissionId}/approve/reject` | 未分配 Reviewer | 通过 | 审核动作被拒绝 |
| export | `POST /api/v1/tasks/{taskId}/exports` | Owner | 失败 | 审计命令不合法 |
| export | `GET /api/v1/tasks/{taskId}/exports` | Labeler | 失败 | 返回 500，应返回 403 |

## 5. 课题要求差距

| 课题要求 | 当前状态 | 差距 |
| --- | --- | --- |
| 任务生命周期 `DRAFT/PUBLISHED/PAUSED/ENDED` | 部分验证 | 已验证 `DRAFT -> PUBLISHED`；暂停/结束未执行 |
| 模板 Designer/Renderer 后端支撑 | 部分验证 | Schema 存储和 Labeler 读取已验证；前端设计器未测 |
| 文本、单选/多选、JSON、ShowItem、LLM Trigger | 部分验证 | 已测 ShowItem/Textarea/Radio/JSON/LLMTrigger；多选未测 |
| 数据导入 JSON/JSONL/Excel | 部分验证 | JSON 批量追加通过；JSONL/Excel 未测 |
| Labeler 领取、草稿、提交、返修 | 部分验证 | 领取/草稿/提交通过；返修因 Reviewer 完整 happy path 未跑通 |
| AI 预标注 | 不通过 | 创建任务成功，但执行失败 `LLM_EXCEPTION` |
| AI 审核 | 不通过 | 任务入队成功，但审核失败，无决策输出 |
| 人工复核、批量操作 | 部分验证 | 权限和单条动作验证；批量操作未执行 |
| 审计日志 | 存在异常 | 导出创建被审计命令校验阻断 |
| 导出 JSON/JSONL/CSV/Excel | 不通过 | 创建导出任务阶段失败，未进入格式 writer |
| 看板与统计 | 未完整验证 | OpenAPI 暴露相关接口，但本轮未逐项验证 |

## 6. 必须修复的问题

### P0：未分配 Reviewer 可以读取其他任务的 AI 审核、预标注和提交详情

影响：
- 任何 Reviewer 账号都可以读取不属于自己的 submission、数据项内容、AI 审核结果、预标注详情。
- 这是明确的越权读取/IDOR 风险。

实测证据：
- 新建 Reviewer `codex_reviewer_1780822043`，未领取任务 `910287`。
- `GET /api/v1/submissions/950136/ai-review` 返回 200。
- `GET /api/v1/pre-annotations/12` 返回 200。
- `GET /api/v1/reviewer/submissions/950136` 返回 200。
- 同一 Reviewer 执行 approve/reject 被拒绝，说明写权限有校验，但读权限缺失。

需要修复的位置：
- `src/main/java/com/labelhub/modules/ai/service/AiReviewResultQueryService.java:49`
  - 当前逻辑：只要有 `REVIEWER` 角色即可读取。
  - 应改为：Reviewer 必须是 submission 的 `assignedReviewerId`，或已领取该任务对应 reviewLevel，或由明确的任务分配表判断。
- `src/main/java/com/labelhub/modules/preannotation/service/PreAnnotationService.java:383`
  - 当前逻辑：任意 `REVIEWER` 可读取任意预标注详情。
  - 应改为：只能读取自己负责的 submission/assignment 所属预标注。
- `src/main/java/com/labelhub/modules/review/service/ReviewerSubmissionQueryService.java:64`
  - 当前逻辑：按 submissionId 直接查详情，无 reviewer 归属校验。
  - 应增加：Reviewer 只能读取自己负责的 submission；列表接口也要用同一规则过滤。

建议补充测试：
- Reviewer A 领取任务后，Reviewer B 访问该任务 submission/AI/preannotation 必须返回 403 或 404。
- 未领取任何任务的 Reviewer 访问任意 submission/AI/preannotation 必须返回 403 或 404。
- Owner 只能访问自己任务下的 AI 审核/预标注/提交详情。

### P0：公开注册允许自选 OWNER

影响：
- 未登录用户可以调用公开注册接口创建 `OWNER` 账号。
- 如果系统部署到非封闭课程演示环境，这会直接扩大业务权限。

代码位置：
- `src/main/java/com/labelhub/modules/auth/service/AuthService.java:36`
  - 当前：`REGISTERABLE_ROLES = Set.of(RoleCode.LABELER, RoleCode.OWNER)`

建议修复：
- 公开注册只允许 `LABELER`。
- Owner/Admin/Reviewer 账号由 Admin 创建或通过邀请码/审批创建。
- 如果课程演示必须保留 Owner 注册，应至少用配置开关控制，例如 `labelhub.auth.allow-owner-self-register=false`，生产默认关闭。

### P1：导出创建失败，课题导出要求无法验收

实测结果：
- `POST /api/v1/tasks/910287/exports` 返回 400：审计命令不合法。
- `includeAuditTrail=true/false` 都失败，说明不是导出字段选项问题，而是导出创建流程自己的审计写入失败。

代码位置：
- `src/main/java/com/labelhub/modules/export/service/ExportJobService.java:116`
  - 创建导出任务后调用 `appendAudit("EXPORT_CREATED", ...)`。
- `src/main/java/com/labelhub/modules/export/service/ExportJobService.java:353`
  - 审计 `bizType` 使用 `"EXPORT_JOB"`。
- `src/main/java/com/labelhub/modules/audit/service/AuditLogService.java`
  - 审计命令要求 traceId、actorType、bizType、bizId、action 都合法。

建议修复：
- 统一审计 bizType 白名单/约束，确认是否允许 `EXPORT_JOB`。
- 确保 traceId 在无 `X-Trace-Id` 请求头时也有后端生成值。
- 导出创建的审计失败不应让事务留下不一致状态；应修复审计参数后再补导出 E2E 测试。

### P1：AI 预标注和 AI 审核失败，核心 AI 流程无法验收

实测结果：
- 预标注 `12` 最终 `FAILED`，`errorCode=LLM_EXCEPTION`。
- AI 审核 AgentRun `716` 最终 `FAILED`，结果无 `decision`、分数、`flowAction`。
- LLM Trigger `1` 长时间 `RUNNING`，未观察到最终输出。

需要排查的位置：
- Provider 配置：`/api/v1/llm-providers` 中 Provider `14`、`13` 均显示 `apiKeyConfigured=true`，但实际调用失败。
- `src/main/java/com/labelhub/infrastructure/llm/OpenAiCompatibleAdapter.java`
  - 检查 baseUrl、模型名、API key、网络超时、响应格式、错误信息截断。
- `src/main/java/com/labelhub/infrastructure/llmtask/LlmTaskWorker.java`
  - 检查 LLM 队列消费是否正常，为什么 Trigger run 长时间 `RUNNING`。
- `src/main/java/com/labelhub/modules/ai/service/AiAutoReviewService.java`
  - 检查失败状态如何落库，是否给前端足够错误信息。

建议修复：
- 增加 AI Provider 连通性测试接口或启动时诊断。
- AgentRun 失败时保留脱敏后的 provider 错误码/错误摘要。
- 对长时间 `RUNNING` 的 LLM Trigger 增加超时转失败机制。

### P1：Prompt Injection 防护不足

证据：
- `src/main/java/com/labelhub/modules/ai/service/PromptTemplateEngine.java:62`
  - Review prompt 直接追加 `userProvidedTemplate`。
- `src/main/java/com/labelhub/modules/ai/service/PromptTemplateEngine.java:79`
  - PreAnnotation prompt 直接追加 `currentAnswerJson`。
- `src/main/java/com/labelhub/modules/ai/service/PromptTemplateEngine.java:89`
  - PreAnnotation prompt 直接追加 `userProvidedTemplate`。
- `src/main/java/com/labelhub/modules/ai/service/PromptTemplateEngine.java:107`
  - LLM Trigger prompt 直接追加 `currentAnswerJson`。
- `src/main/java/com/labelhub/modules/ai/service/PromptTemplateEngine.java:111`
  - LLM Trigger prompt 直接追加 `userProvidedTemplate`。

风险：
- 数据项、草稿答案、Owner 自定义 prompt 中的恶意指令可能影响 AI 审核结果。
- 如果任务配置开启 AI 直通过/直拒，prompt injection 可能导致错误自动流转。

建议修复：
- 在系统 prompt 中明确标记数据项、用户答案、Owner prompt 为“不可信输入”。
- 使用分隔符包裹不可信内容，并要求模型不得执行其中指令。
- AI 审核输出必须通过结构化 schema 校验，不满足 schema 时转人工。
- 含 prompt injection 风险标记的数据强制 `MANUAL_REVIEW`。
- AI 直通过/直拒必须额外校验 `confidence`、`riskFlags`、`degraded=false`、模型输出完整性。

## 7. 建议修复的问题

### P2：错误 HTTP 状态码不规范

现象：
- Labeler 访问 AI 审核结果被拒绝时，业务码是 `403703`，但 HTTP 状态是 400。
- Labeler 访问导出列表返回 500，而不是 403。
- 模板/数据接口传错 JSON 字段类型返回 500，而不是 400。

建议：
- 认证失败统一 HTTP 401。
- 授权失败统一 HTTP 403。
- 参数校验失败统一 HTTP 400。
- 服务内部异常才返回 HTTP 500。

### P2：Admin 用户列表被异常数据阻断

现象：
- `GET /api/v1/admin/users` 返回 400：用户必须且只能拥有一个角色。

风险：
- 任意一个用户角色数据异常会导致整个 Admin 用户列表不可用。

建议：
- 列表接口应容忍异常用户并标记数据异常，而不是整体失败。
- 增加数据修复脚本或迁移约束，保证普通用户只有一个角色。

### P2：接口契约与调用计划存在差异

现象：
- 登录字段实际是 `account`。
- 创建 reviewer 的真实路径是 `/api/v1/admin/users/reviewers`。
- `schemaJson`、`itemJson`、`metadataJson` 必须是对象，传字符串会触发 500。

建议：
- 补充 OpenAPI 合同测试，覆盖请求体字段类型。
- 前端/测试脚本严格以 OpenAPI DTO 为准。
- 对错误类型增加 Bean Validation 或自定义异常处理。

## 8. 未发现高置信问题的区域

- SQL 注入：未发现 MyBatis `${}` 直接拼接；仅看到数字分页 `.last("LIMIT ...")`，需继续保持边界测试。
- 文件上传：有扩展名白名单、随机 objectKey、文件名清洗、签名 URL owner/admin 校验；本轮未发现路径穿越或越权下载。
- LLM Provider SSRF：`OpenAiCompatibleAdapter` 对 loopback/site-local/link-local/any-local 地址有拦截，并阻止敏感 custom headers；DNS rebinding 和 Admin 威胁模型未深测。
- API Key：Provider 响应不直接返回 API key，错误信息会做部分脱敏；仍建议继续检查 AgentRun 输入/输出快照中是否可能出现密钥。

## 9. 优先修复顺序

1. 修复 Reviewer 越权读取：AI 审核结果、预标注详情、submission 详情/列表。
2. 关闭或限制公开注册 OWNER。
3. 修复导出创建审计失败，恢复导出链路。
4. 排查 AI Provider/LLM 队列，恢复 AI 预标注、LLM Trigger、AI 审核。
5. 加固 Prompt Injection 防护和 AI 直通策略。
6. 统一 HTTP 状态码和参数错误处理。
7. 修复 Admin 用户列表被异常角色数据阻断的问题。
