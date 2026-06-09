# BE-A 后端审核智能业务引擎分工任务书

本文档按已完成后端交付重新组织 BE-A 分工。BE-A 负责 LabelHub 的业务主链路和审核智能链路，核心是把任务、领取、草稿、提交、AI 预审、人工审核和追溯串成稳定闭环。

## 0. BE-A 目标

BE-A 的交付目标包括：

- 任务生命周期控制。
- Owner 任务管理和标注员查询。
- Labeler 任务市场、领取、草稿、作答工作台。
- Submission 版本、答案哈希、提交幂等和状态推进。
- LLM Provider 查询与 Admin 全局维护接口配合。
- AI 审核配置、Prompt 测试、AI 自动预审。
- AgentRun 运行记录、AI 结果记录、AI 日志查询和可观测字段。
- LLM 任务队列、Worker、重试、恢复和人工审核兜底。
- 字段级 LLM 辅助触发和题目级预标注。
- Reviewer 审核队列、审核详情、通过、打回、批量操作和审核领取。
- 提交版本追溯、字段 diff 和多版本 compare。

BE-A 不负责前端页面实现、数据集原始文件维护、模板资源管理、对象存储文件生成、奖励结算和看板聚合。上述能力由 FE 或 BE-B 提供，BE-A 通过清晰接口复用。

## 1. 任务状态机模块

### 1.1 大功能：任务生命周期控制

交付效果：

- Owner 创建任务后，任务只能按合法路径推进。
- 发布、暂停、恢复、结束等动作由后端统一校验。
- 前端只触发动作，最终状态由后端返回。

小功能：

1. 任务创建
   - 标题。
   - 描述。
   - 任务说明。
   - 标签。
   - 截止时间。
   - 配额。
   - 任务关联 Owner。
   - 可指派标注员字段。

2. 任务编辑
   - 更新基础信息。
   - 校验任务归属。
   - 校验状态约束。
   - 保持已发布任务的关键业务约束。

3. 发布任务
   - 校验任务存在。
   - 校验当前用户为任务 Owner 或具备对应权限。
   - 校验任务具备发布所需基础配置。
   - 推进任务状态。

4. 暂停任务
   - 只对发布中的任务生效。
   - 暂停后任务市场不再作为可继续领取入口。

5. 恢复任务
   - 将暂停任务恢复到发布状态。
   - 恢复后重新进入可参与任务范围。

6. 结束任务
   - 结束后任务进入收口状态。
   - 查看和统计能力仍然可用。

已完成实现：

- `TaskController`
- `OwnerTaskController`
- `OwnerLabelerController`
- `TaskLifecycleService`
- `TaskManagementService`
- `OwnerAssignableLabelerService`
- `TaskLifecycleServiceTest`
- `TaskManagementServiceTest`
- `OwnerAssignableLabelerServiceTest`

已完成接口：

```Plaintext
POST /api/v1/tasks
GET /api/v1/tasks/{taskId}
PUT /api/v1/tasks/{taskId}
DELETE /api/v1/tasks/{taskId}
GET /api/v1/tasks/{taskId}/statistics
GET /api/v1/tasks/{taskId}/labelers
POST /api/v1/tasks/{taskId}/publish
POST /api/v1/tasks/{taskId}/pause
POST /api/v1/tasks/{taskId}/resume
POST /api/v1/tasks/{taskId}/end
GET /api/v1/owner/tasks
GET /api/v1/owner/labelers/assignable
```

验收：

```Plaintext
Owner 可以创建任务。
Owner 可以查询和编辑自己的任务。
非法状态动作返回业务错误。
发布、暂停、恢复、结束动作会更新任务状态。
前端任务列表能读取 Owner 任务集合。
Owner 能查询可分配标注员列表。
```

### 1.2 大功能：任务统计查询

交付效果：

- Owner 和工作台页面可以读取任务统计摘要。
- 统计结果供页面展示，不直接替代状态机动作。

小功能：

1. 任务统计
   - 总题量。
   - 领取量。
   - 提交量。
   - 审核通过量。
   - 打回量。

2. 查询边界
   - 按 taskId 查询。
   - 校验任务归属或角色权限。
   - 返回聚合结果给前端。

3. 与 BE-B 协作
   - 数据集 item 数量由数据集模块维护。
   - 看板级聚合由 BE-B 看板服务提供。
   - BE-A 负责任务状态链路相关的统计入口。

验收：

```Plaintext
任务详情页能获取统计摘要。
不存在的任务返回明确错误。
非授权用户不能读取不属于自己的任务统计。
```

## 2. 领取、草稿、提交模块

### 2.1 大功能：任务市场

交付效果：

- Labeler 可以查询当前可参与任务。
- 任务市场返回任务摘要、奖励摘要、剩余参与信息和当前用户参与状态。

小功能：

1. 市场列表
   - 查询发布中的任务。
   - 支持任务摘要展示。
   - 聚合可参与数量。
   - 聚合当前用户参与情况。

2. 市场详情
   - 查看任务说明。
   - 查看奖励摘要。
   - 查看截止时间。
   - 查看当前用户是否已有领取记录。

3. 依赖服务
   - `TaskMarketService` 组织市场查询。
   - `DefaultAssignmentMarketStatsService` 提供 assignment 侧统计。
   - `DefaultDatasetMarketStatsService` 提供数据集侧统计。
   - `DefaultRewardSummaryService` 提供奖励摘要。

已完成实现：

- `MarketTaskController`
- `TaskMarketService`
- `DefaultAssignmentMarketStatsService`
- `DefaultDatasetMarketStatsService`
- `DefaultRewardSummaryService`

已完成接口：

```Plaintext
GET /api/v1/market/tasks
GET /api/v1/market/tasks/{taskId}
```

验收：

```Plaintext
Labeler 能查询任务市场。
任务市场只展示可参与任务范围内的数据。
市场详情包含任务摘要、奖励摘要和参与状态。
无可参与数据时返回可展示的空结果。
```

### 2.2 大功能：领取与指派

交付效果：

- Labeler 能领取任务题目。
- Owner 或系统能围绕任务维护指派关系。
- 领取过程通过后端事务和数据集状态控制避免重复领取。

小功能：

1. Labeler 领取
   - 校验任务状态。
   - 校验当前用户身份。
   - 查询可领取 item。
   - 创建 assignment。
   - 更新 item 领取状态。
   - 返回领取结果。

2. 领取查询
   - 查询当前用户 claims。
   - 查询单个 claim 详情。
   - 查询 Labeler 自己的 assignment 列表。

3. 指派管理
   - 为任务创建 dispatch。
   - 查询任务 dispatch 列表。
   - 删除 dispatch。
   - 查询当前用户的 dispatch。
   - 支持任务绑定的标注员策略。

4. 取消领取
   - Labeler 可对满足条件的 assignment 发起取消。
   - 后端负责恢复相应状态。

已完成实现：

- `ClaimController`
- `AssignmentDispatchController`
- `LabelerAssignmentController`
- `AssignmentClaimService`
- `AssignmentDispatchService`
- `AssignedAutoAssignmentService`
- `AssignmentCancelService`
- `DefaultDatasetClaimService`
- `V38__task_assigned_labeler.sql`

已完成接口：

```Plaintext
POST /api/v1/tasks/{taskId}/items/claim
GET /api/v1/claims
GET /api/v1/claims/{claimId}
GET /api/v1/labeler/assignments
POST /api/v1/labeler/assignments/{assignmentId}/cancel
POST /api/v1/tasks/{taskId}/dispatches
GET /api/v1/tasks/{taskId}/dispatches
DELETE /api/v1/tasks/{taskId}/dispatches/{dispatchId}
GET /api/v1/tasks/{taskId}/dispatches/my
```

验收：

```Plaintext
Labeler 可以领取可参与任务中的题目。
同一题目在主链路中只保持一个活跃标注处理人。
重复领取会被后端拒绝或返回明确结果。
领取失败不污染 assignment 和 item 状态。
Owner 能维护任务指派记录。
```

### 2.3 大功能：草稿保存

交付效果：

- Labeler 在作答过程中可以保存草稿，并在刷新或重新进入时恢复。
- 草稿既有持久化记录，也可利用 Redis 缓存加速读取。

小功能：

1. 保存草稿
   - 校验 claim/assignment 属主。
   - 保存 answerJson。
   - 保存草稿版本。
   - 保存更新时间。

2. 读取草稿
   - 优先读取可用缓存。
   - 缓存缺失时读取持久化记录。
   - 返回给前端恢复输入。

3. 版本控制
   - 支持前端传入版本信息。
   - 后端避免旧页面覆盖新草稿。

4. 缓存支撑
   - `AssignmentDraftCacheService`
   - `RedissonAssignmentDraftCacheService`
   - `RedisLockService`

已完成实现：

- `ClaimController`
- `AssignmentDraftService`
- `AssignmentDraftCacheService`
- `RedissonAssignmentDraftCacheService`
- `RedisLockServiceTest`
- `RedissonRedisLockServiceTest`

已完成接口：

```Plaintext
PUT /api/v1/claims/{claimId}/draft
GET /api/v1/claims/{claimId}/draft
```

验收：

```Plaintext
Labeler 保存草稿后可重新读取。
非属主不能读取或覆盖草稿。
旧版本草稿不能覆盖新版本草稿。
缓存缺失时仍可从持久化记录恢复。
```

### 2.4 大功能：提交版本

交付效果：

- Labeler 正式提交后形成 Submission 版本。
- 重新提交会保留历史版本，不覆盖已有记录。
- 提交后进入 AI 预审和人工审核链路。

小功能：

1. 提交前校验
   - 校验 claim/assignment 属主。
   - 校验 assignment 状态。
   - 读取任务模板版本。
   - 调用答案 Schema 校验。

2. 版本生成
   - 计算答案哈希。
   - 判断重复提交。
   - 生成 versionNo。
   - 写入 submission。
   - 设置提交创建人。
   - 标记旧版本状态。

3. 状态推进
   - 推进 assignment 状态。
   - 推进 submission 状态。
   - 创建 AI 审核任务。
   - 写入审计。

4. 追溯准备
   - 保存原始 answerJson。
   - 保存 item 引用。
   - 保存模板版本引用。
   - 保留 diff 和版本查询所需字段。

已完成实现：

- `LabelerSubmissionController`
- `SubmissionSubmitService`
- `SubmissionVersionService`
- `SubmissionStatus`
- `AnswerDiffService`
- `V37__submission_created_by.sql`
- `SubmissionSubmitServiceTest`
- `SubmissionVersionServiceTest`
- `SubmissionStatusTest`

已完成接口：

```Plaintext
POST /api/v1/claims/{claimId}/submit
GET /api/v1/labeler/submissions
GET /api/v1/labeler/submissions/{submissionId}
```

验收：

```Plaintext
首次提交生成 versionNo=1。
重新提交生成新的版本记录。
重复答案不会产生重复有效提交。
Schema 校验失败时提交被拒绝。
提交创建人字段能支撑本人提交列表和审核追溯。
```

## 3. LLM Provider 与 Gateway 模块

### 3.1 大功能：Admin 全局 Provider 管理

交付效果：

- Admin 维护全局 LLM Provider。
- Owner 在任务 AI 配置中查询启用的 Provider。
- API Key 不明文返回给前端。

小功能：

1. Admin 管理
   - 查询 Provider 列表。
   - 新建 Provider。
   - 编辑 Provider。
   - 启用 Provider。
   - 停用 Provider。
   - 连通性测试。

2. Owner 查询
   - 查询启用 Provider。
   - 用于 AI 审核配置选择。
   - 停用项不作为可选项。

3. 安全处理
   - API Key 加密或脱敏处理。
   - Provider 测试不写出密钥。
   - 日志不输出明文密钥。

已完成实现：

- `AdminLlmProviderController`
- `LlmProviderController`
- `LlmProviderService`
- `OpenAiCompatibleProviderTester`
- `V29__llm_providers_admin_global.sql`
- `AdminLlmProviderControllerTest`
- `LlmProviderControllerTest`
- `OpenAiCompatibleProviderTesterTest`

已完成接口：

```Plaintext
GET /api/v1/admin/llm-providers
POST /api/v1/admin/llm-providers
PUT /api/v1/admin/llm-providers/{providerId}
POST /api/v1/admin/llm-providers/{providerId}/enable
POST /api/v1/admin/llm-providers/{providerId}/disable
POST /api/v1/admin/llm-providers/{providerId}/test
GET /api/v1/llm-providers
```

验收：

```Plaintext
Admin 能创建和维护 Provider。
Admin 能对 Provider 做连通性测试。
Owner 只能查询启用 Provider。
Provider 响应不回显 API Key 明文。
```

### 3.2 大功能：统一 LLM Gateway

交付效果：

- AI 审核、字段级辅助、预标注和 Provider 测试通过统一 LLM 基础设施调用兼容模型。

小功能：

1. Gateway
   - `DefaultLlmGateway`
   - 统一请求模型。
   - 统一响应模型。
   - 统一错误映射。

2. Adapter
   - `OpenAiCompatibleAdapter`
   - 适配 OpenAI-compatible HTTP API。
   - 构造 headers。
   - 处理超时。
   - 提取结构化响应。
   - 保存 rawResponse。

3. 测试覆盖
   - 默认网关测试。
   - Adapter 单元测试。
   - HTTP E2E 风格测试。

已完成实现：

- `DefaultLlmGateway`
- `OpenAiCompatibleAdapter`
- `OpenAiCompatibleAdapterTest`
- `OpenAiCompatibleAdapterHttpE2ETest`
- `DefaultLlmGatewayTest`

验收：

```Plaintext
不同业务模块通过统一 Gateway 调用模型。
兼容模型响应能转换为统一结果。
超时和非预期响应能转换为明确错误。
原始响应用于排查和审核追溯。
```

## 4. AgentRun 与 LLM 异步任务

### 4.1 大功能：AgentRun 运行记录

交付效果：

- 每次 AI 相关运行都有可查询的运行记录。
- 运行记录与 AI 审核结果分层保存，便于追踪输入、输出、状态和错误。

小功能：

1. 创建运行记录
   - agentType。
   - bizType。
   - bizId。
   - providerId。
   - modelName。
   - promptVersion。
   - inputSnapshot。
   - outputSnapshot。
   - status。
   - errorMessage。
   - startedAt。
   - finishedAt。

2. 状态推进
   - PENDING。
   - RUNNING。
   - SUCCESS。
   - FAILED。
   - MANUAL_REQUIRED。

3. 查询与脱敏
   - 按运行 ID 查询。
   - 输出快照脱敏。
   - 对前端隐藏敏感字段。

已完成实现：

- `AgentRunController`
- `AgentRunService`
- `AgentRunQueryService`
- `AgentRunControllerTest`
- `V2__seed_system_agent.sql`

已完成接口：

```Plaintext
GET /api/v1/agent-runs/{agentRunId}
```

验收：

```Plaintext
AI 审核会产生 AgentRun。
失败重试会形成新的运行记录。
运行记录可查询。
快照输出不会暴露 Provider 密钥。
```

### 4.2 大功能：LLM 任务队列与 Worker

交付效果：

- 业务型 LLM 调用通过异步任务执行，避免阻塞提交和页面动作。
- Worker 能消费任务、构造上下文、调用对应 handler 并记录结果。

小功能：

1. 队列
   - `LlmTaskQueueService`
   - `RedissonLlmTaskQueueService`
   - Redis Stream 消息投递。
   - 任务类型区分。

2. Worker
   - `LlmTaskWorker`
   - 构造 `LlmTaskExecutionContext`。
   - 根据 taskType 分派 handler。
   - 处理成功、失败和重试信息。

3. Handler
   - `AiReviewLlmTaskHandler`
   - `LlmTriggerTaskHandler`
   - `PreAnnotationLlmTaskHandler`

已完成实现：

- `LlmTaskWorker`
- `LlmTaskExecutionContext`
- `LlmTaskQueueService`
- `RedissonLlmTaskQueueService`
- `LlmTaskWorkerTest`

验收：

```Plaintext
AI 审核任务能进入 LLM 队列。
Worker 能根据任务类型调用对应处理器。
任务失败会保留错误信息。
队列执行不阻塞 Labeler 正式提交请求。
```

## 5. AI 审核配置与自动预审模块

### 5.1 大功能：AI 审核配置

交付效果：

- Owner 能为任务配置 AI 预审规则。
- 配置保存后用于提交后的 AI 自动预审。

小功能：

1. 配置保存
   - Provider。
   - model。
   - Prompt 模板。
   - 评分维度。
   - 阈值。
   - 流转策略。
   - 输出契约字段。

2. 配置更新
   - 按 taskId 和 configId 更新。
   - 校验任务归属。
   - 校验 Provider 可用。

3. 配置查询
   - 查询当前任务 AI 配置。
   - 前端用于任务编辑页回显。

4. Prompt 测试
   - 使用当前配置和输入样例调用 LLM。
   - 返回测试结果。
   - 不生成正式 submission。

已完成实现：

- `AiReviewConfigController`
- `AiReviewConfigService`
- `AiFlowDecisionService`
- `V9__ai_flow_policy.sql`
- `V26__ai_review_config_contract_columns.sql`
- `V36__review_strategy.sql`
- `AiReviewConfigControllerTest`

已完成接口：

```Plaintext
POST /api/v1/tasks/{taskId}/ai-review-configs
PUT /api/v1/tasks/{taskId}/ai-review-configs/{configId}
GET /api/v1/tasks/{taskId}/ai-review-configs
POST /api/v1/tasks/{taskId}/ai-review-configs/{configId}/test
```

验收：

```Plaintext
Owner 能保存任务 AI 配置。
禁用 Provider 不能作为任务 AI 配置选择。
Prompt 测试返回结构化结果或明确错误。
AI 流转策略字段能被审核服务读取。
```

### 5.2 大功能：AI 自动预审

交付效果：

- Labeler 提交后，系统生成 AI 预审任务。
- AI 预审结果保存为独立记录，并进入 Reviewer 可查看范围。
- AI 结果只作为建议，人工审核动作仍由 Reviewer 执行。

小功能：

1. 任务创建
   - 提交成功后创建 AI 审核任务。
   - 创建初始 AI 结果记录。
   - 创建或关联 AgentRun。
   - 投递 LLM 队列。

2. Prompt 构造
   - 读取任务配置。
   - 读取题目快照。
   - 读取提交答案。
   - 组合评分维度和输出契约。

3. LLM 调用
   - 通过 `DefaultLlmGateway` 调用 Provider。
   - 保存 rawResponse。
   - 解析结构化输出。

4. 结果写入
   - 维度评分。
   - 总体建议。
   - 错误字段。
   - trace 指标。
   - provider/model 信息。

5. 状态推进
   - AI 成功后提交进入人工审核队列。
   - AI 失败时保留人工审核入口。
   - AI 结果和 submission 状态分层保存。

已完成实现：

- `AiReviewController`
- `AiReviewResultController`
- `AiReviewLogController`
- `AiAutoReviewService`
- `AiReviewDispatcher`
- `AsyncAiReviewDispatcher`
- `AiReviewLlmTaskHandler`
- `AiReviewResultQueryService`
- `AiReviewLogQueryService`
- `AiFlowDecisionService`
- `V30__ai_review_result_error_fields.sql`
- `V33__ai_observability_trace_metrics.sql`
- `AiAutoReviewServiceTest`
- `AiReviewResultControllerTest`

已完成接口：

```Plaintext
GET /api/v1/submissions/{submissionId}/ai-review
POST /api/v1/submissions/{submissionId}/ai-review/retry
GET /api/v1/submissions/{submissionId}/ai-review-result
GET /api/v1/tasks/{taskId}/ai-review-logs
```

验收：

```Plaintext
提交成功后能触发 AI 预审任务。
AI 成功结果能被 Reviewer 查看。
AI 错误信息能通过日志或结果查询定位。
AI 建议不会直接替代 Reviewer 审核动作。
```

### 5.3 大功能：失败重试与恢复

交付效果：

- LLM 服务超时、限流或异常时，AI 链路能记录错误并进行重试或恢复。
- 失败不会阻塞人工审核主链路。

小功能：

1. 重试策略
   - `AiReviewRetryStrategy`
   - 最大次数。
   - 延迟计算。
   - 错误分类。

2. 调度
   - `AiReviewRetryScheduler`
   - 扫描待重试记录。
   - 重新投递任务。

3. 恢复
   - `AiReviewRecoveryRunner`
   - 处理异常中断状态。
   - 保证提交仍可进入人工处理。

4. 手动重试
   - `AiReviewManualRetryService`
   - Reviewer 或具备权限的用户可触发重试入口。

已完成实现：

- `AiReviewRetryService`
- `AiReviewRetryStrategy`
- `AiReviewRetryScheduler`
- `AiReviewRecoveryRunner`
- `AiReviewManualRetryService`
- `AiReviewRetryStrategyTest`
- `AiReviewRetrySchedulerTest`
- `AiReviewRecoveryRunnerTest`

验收：

```Plaintext
AI 超时会记录错误原因。
可重试错误会被调度器重新投递。
超过重试范围后仍保留人工审核入口。
手动重试能生成新的 AI 执行记录。
```

## 6. LLM 辅助与预标注模块

### 6.1 大功能：字段级 LLM 辅助触发

交付效果：

- Designer 预览和 Labeler 作答工作台可以触发字段级 LLM 辅助。
- 输出作为参考或预填建议展示，由用户确认后进入答案输入。

小功能：

1. Assignment 场景
   - 根据 assignmentId 校验访问权限。
   - 读取任务、模板、组件和当前答案上下文。
   - 投递 LLM 任务。
   - 返回触发运行 ID 或执行结果。

2. Task 测试场景
   - 根据 taskId 进行测试调用。
   - 用于模板设计器或任务编辑中的效果预览。
   - 不生成正式提交。

3. 运行日志
   - 查询单次 trigger run。
   - 查询任务维度 trigger run 列表。
   - 保留输入、输出、状态和错误摘要。

已完成实现：

- `LlmTriggerController`
- `LlmTriggerRunLogController`
- `LlmTriggerService`
- `LlmTriggerTaskHandler`
- `LlmTriggerRunQueryService`
- `LlmTriggerControllerTest`

已完成接口：

```Plaintext
POST /api/v1/assignments/{assignmentId}/llm-triggers
POST /api/v1/tasks/{taskId}/llm-triggers/test
GET /api/v1/llm/triggers/runs/{triggerRunId}
GET /api/v1/tasks/{taskId}/llm-trigger-runs
```

验收：

```Plaintext
Labeler 能在有权限的 assignment 上触发字段级 LLM 辅助。
Designer 或任务编辑场景能进行测试触发。
运行结果可查询。
辅助输出不绕过用户确认直接形成正式提交。
```

### 6.2 大功能：题目级预标注

交付效果：

- Owner 或相关工作流可以为题目生成预标注建议。
- 预标注结果服务于作答参考和任务质量提升。

小功能：

1. 请求处理
   - 根据 taskId 和 itemId 读取题目上下文。
   - 读取任务 AI 配置。
   - 构造预标注 Prompt。

2. LLM 执行
   - 投递 LLM 队列。
   - 使用 `PreAnnotationLlmTaskHandler` 执行。
   - 保存建议结果和执行状态。

3. 查询
   - 查询预标注结果。
   - 查询错误信息。
   - 支持前端展示建议。

已完成实现：

- `PreAnnotationController`
- `PreAnnotationService`
- `PreAnnotationLlmTaskHandler`
- `PreAnnotationControllerTest`

已完成接口：

```Plaintext
POST /api/v1/assignments/{assignmentId}/pre-annotations/run
GET /api/v1/assignments/{assignmentId}/pre-annotations/latest
GET /api/v1/pre-annotations/{preAnnotationId}
```

验收：

```Plaintext
Labeler 可基于自己的 assignment 触发预标注任务。
预标注任务复用 LLM 执行基础设施。
预标注结果可查询。
预标注建议与正式 Labeler 提交保持区分。
```

## 7. 人工审核模块

### 7.1 大功能：Reviewer 工作台

交付效果：

- Reviewer 能查看分配给自己的任务、待审 item、审核概览和 AI 状态摘要。

小功能：

1. 任务列表
   - 查询 Reviewer 可处理任务。
   - 返回任务摘要、待审数量和处理进度。

2. 任务 item
   - 按 taskId 查询待处理 item。
   - 返回提交摘要和 AI 状态。

3. 工作台摘要
   - Reviewer dashboard。
   - AI review status。
   - 当前待处理量。

已完成实现：

- `ReviewerWorkspaceController`
- `ReviewerTaskItemQueryService`
- `ReviewerPoolService`
- `ReviewerWorkspaceControllerTest`

已完成接口：

```Plaintext
GET /api/v1/reviewer/tasks
GET /api/v1/reviewer/tasks/{taskId}/items
GET /api/v1/reviewer/dashboard
GET /api/v1/reviewer/ai-review-status
```

验收：

```Plaintext
Reviewer 可以查询自己的审核任务范围。
Reviewer 可以看到任务下待处理 item。
AI 状态摘要可用于审核队列展示。
非授权用户不能读取 Reviewer 工作台数据。
```

### 7.2 大功能：审核领取

交付效果：

- Reviewer 对任务进行领取或释放，减少待审数据处理过程中的并发操作干扰。
- 数据库为提交审核领取查询提供索引支撑。

小功能：

1. 领取
   - Reviewer 对指定任务创建 claim。
   - 记录 reviewerId、taskId 和领取时间。
   - 返回领取结果。

2. 释放
   - Reviewer 释放当前任务 claim。
   - 后端更新领取状态。

3. 查询支撑
   - 审核队列按领取关系过滤或排序。
   - 提交查询通过索引提升效率。

已完成实现：

- `ReviewTaskClaimController`
- `ReviewTaskClaimService`
- `ReviewTaskClaim`
- `V34__review_task_claims.sql`
- `V39__submission_review_claim_indexes.sql`

已完成接口：

```Plaintext
POST /api/v1/reviewer/tasks/{taskId}/claim
DELETE /api/v1/reviewer/tasks/{taskId}/claim
```

验收：

```Plaintext
Reviewer 可以领取审核任务。
Reviewer 可以释放审核任务。
审核任务领取记录能用于队列查询。
V39 索引支撑提交审核领取查询。
```

### 7.3 大功能：审核详情与单条审核

交付效果：

- Reviewer 能查看提交详情，并执行通过或打回。
- 审核记录、提交状态和 assignment 状态由后端事务更新。

小功能：

1. 审核列表
   - 查询待审核 submission。
   - 支持任务、状态、AI 结果等条件。
   - 返回分页结果。

2. 审核详情
   - 题目原始数据。
   - Labeler 提交答案。
   - AI 审核结果。
   - 历史提交版本。
   - 审计时间线。

3. 通过
   - 校验 submission 可审核。
   - 写入 reviewRecord。
   - 更新 submission 状态。
   - 更新 assignment 状态。
   - 触发奖励结算入口。

4. 打回
   - 校验打回理由。
   - 写入 reviewRecord。
   - 更新 submission 状态。
   - 更新 assignment 状态为可修改口径。
   - Labeler 可在提交记录中查看原因。

已完成实现：

- `ReviewController`
- `ReviewService`
- `ReviewRecord`
- `ReviewerSubmissionQueryService`
- `ReviewControllerTest`

已完成接口：

```Plaintext
GET /api/v1/reviewer/submissions
GET /api/v1/reviewer/submissions/{submissionId}
POST /api/v1/reviewer/submissions/{submissionId}/approve
POST /api/v1/reviewer/submissions/{submissionId}/reject
```

验收：

```Plaintext
Reviewer 能查询待审提交。
Reviewer 能查看单条提交详情。
通过后 submission 状态更新为通过口径。
打回必须有理由。
打回理由能被 Labeler 查询到。
审核记录会写入 reviewRecord。
```

### 7.4 大功能：批量审核

交付效果：

- Reviewer 可以批量处理审核队列中的普通提交。
- 批量操作逐条执行，返回成功数、失败数和失败原因。

小功能：

1. 批量通过
   - 校验每条 submission 状态。
   - 逐条写 reviewRecord。
   - 逐条更新状态。
   - 返回逐条结果。

2. 批量打回
   - 校验统一打回理由。
   - 逐条更新状态。
   - 返回逐条结果。

3. 批量标记人工处理
   - 用于将需要人工重点处理的数据标记到审核队列。
   - 保留逐条操作结果。

已完成实现：

- `BatchReviewService`
- `ReviewController`
- `ReviewControllerTest`

已完成接口：

```Plaintext
POST /api/v1/reviewer/submissions/batch/approve
POST /api/v1/reviewer/submissions/batch-approve
POST /api/v1/reviewer/submissions/batch/reject
POST /api/v1/reviewer/submissions/batch-reject
POST /api/v1/reviewer/submissions/batch/mark-manual
POST /api/v1/reviewer/submissions/batch-mark-manual
```

验收：

```Plaintext
批量通过返回每条处理结果。
批量打回要求统一理由。
部分失败不影响其他可处理提交。
每条成功处理都产生审核记录。
```

## 8. 提交追溯与导出快照查询

### 8.1 大功能：提交版本追溯

交付效果：

- Reviewer、Owner 或相关页面可以回溯提交版本、字段差异和多版本对比。

小功能：

1. 版本列表
   - 查询某 submission 的历史版本。
   - 返回 versionNo、状态、提交时间和创建人。

2. 字段 diff
   - 对比当前版本与历史版本。
   - 返回字段级差异。
   - 支撑审核详情页展示。

3. 多版本 compare
   - 按多个 submission 或版本进行比较。
   - 支撑复杂审核和追溯场景。

已完成实现：

- `SubmissionTraceController`
- `SubmissionVersionService`
- `AnswerDiffService`
- `SubmissionVersionServiceTest`

已完成接口：

```Plaintext
GET /api/v1/submissions/{submissionId}/diff
GET /api/v1/submissions/{submissionId}/versions
GET /api/v1/submissions/compare
```

验收：

```Plaintext
提交详情能查询版本列表。
字段差异能按 answerJson 生成。
多版本 compare 返回可展示结构。
追溯接口不暴露无权限提交。
```

### 8.2 大功能：导出快照查询

交付效果：

- BE-B 导出模块可以读取审核通过的提交快照。
- BE-A 负责导出范围的业务正确性，BE-B 负责格式化和文件生成。

小功能：

1. 查询范围
   - 按 taskId 查询。
   - 只返回审核通过的提交。
   - 校验 Owner 权限。

2. 快照内容
   - item 引用。
   - answerJson。
   - AI 审核摘要。
   - 审核记录引用。
   - 提交版本信息。

3. 分页
   - 支持大任务分页读取。
   - 保持导出顺序稳定。

已完成实现：

- `SubmissionExportQueryService`
- `ExportSnapshotService`
- `SubmissionExportQueryServiceTest`
- `TaskExportControllerTest`

验收：

```Plaintext
导出查询只返回审核通过提交。
导出查询校验任务归属。
导出快照包含答案和 AI 摘要。
分页查询结果稳定。
```

## 9. BE-A 与其他分工边界

### 9.1 与 FE 的边界

- FE 负责页面、路由、表单、交互状态和服务调用。
- BE-A 负责业务状态、权限校验、事务更新和审核结论。
- FE 不在本地伪造任务、提交或审核状态。

### 9.2 与 BE-B 的边界

- BE-B 提供 Auth/RBAC、模板、Schema 校验、数据集、存储、导出、奖励、看板和审计基础能力。
- BE-A 在提交时调用答案校验能力。
- BE-A 在领取时依赖数据集 item 状态。
- BE-A 在审核通过后为奖励和导出提供业务数据。
- BE-A 不直接生成导出文件，也不维护对象存储。

### 9.3 与数据库迁移的关系

当前 BE-A 关键迁移包括：

- `V9__ai_flow_policy.sql`
- `V26__ai_review_config_contract_columns.sql`
- `V30__ai_review_result_error_fields.sql`
- `V33__ai_observability_trace_metrics.sql`
- `V34__review_task_claims.sql`
- `V36__review_strategy.sql`
- `V37__submission_created_by.sql`
- `V38__task_assigned_labeler.sql`
- `V39__submission_review_claim_indexes.sql`

## 10. BE-A 自测清单

```Plaintext
TaskLifecycleServiceTest 覆盖任务状态动作。
TaskManagementServiceTest 覆盖任务创建和管理。
OwnerAssignableLabelerServiceTest 覆盖 Owner 可分配标注员查询。
SubmissionSubmitServiceTest 覆盖提交、版本、幂等和校验。
SubmissionVersionServiceTest 覆盖提交版本能力。
AiReviewConfigControllerTest 覆盖 AI 配置接口。
AiAutoReviewServiceTest 覆盖 AI 自动预审链路。
AiReviewRetryStrategyTest 覆盖重试策略。
AiReviewRetrySchedulerTest 覆盖重试调度。
AiReviewRecoveryRunnerTest 覆盖恢复逻辑。
LlmTaskWorkerTest 覆盖 LLM 任务 Worker 分派。
AdminLlmProviderControllerTest 和 LlmProviderControllerTest 覆盖 Provider 管理与查询。
LlmTriggerControllerTest 覆盖字段级 LLM 辅助触发。
PreAnnotationControllerTest 覆盖预标注接口。
ReviewerWorkspaceControllerTest 覆盖 Reviewer 工作台。
ReviewControllerTest 覆盖单条和批量审核。
SubmissionExportQueryServiceTest 覆盖导出快照查询。
DatabaseMigrationSafetyTest 和 DatabaseMigrationNamingTest 覆盖迁移安全与命名。
```
