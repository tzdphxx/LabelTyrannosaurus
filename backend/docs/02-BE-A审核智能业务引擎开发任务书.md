# 02\-BE\-A审核智能业务引擎开发任务书

# BE\-A 后端审核智能业务引擎开发任务书

> 面向 AI 开发工具：本文件只描述 BE\-A 负责范围。BE\-A 负责任务状态机、领取提交、AI Agent 审核引擎、LLM 调用、人工终审和金标闭环。

## 0\. BE\-A 目标

BE\-A 负责 LabelHub 的审核智能业务引擎：

- 任务状态机。

- 领取、草稿、提交版本。

- Submission 状态机。

- LLM Provider 管理。

- LlmGateway / OpenAI\-compatible Adapter。

- System Agent 身份。

- Agent Run。

- AI 审核配置。

- AI 自动预审。

- 结构化输出校验。

- 失败重试和人工兜底。

- AI 结果到人工终审的状态推进。

- Reviewer 通过 / 打回。

- 冲突仲裁与金标选择。

边界规则：

- BE\-A 不维护数据集原始文件。

- BE\-A 不维护模板版本表。

- BE\-A 提交时调用 BE\-B 的 schema 校验能力。

- BE\-A 决定 submission、review、isGolden 的状态。

- BE\-A 在通过或金标确定后发布事件，由 BE\-B 结算奖励和统计。

- BE\-A 使用 BE\-B 提供的 Redis lock、rate limit、异步执行器基础能力。

- BE\-A 不直接写审计表，统一调用 BE\-B 提供的 `AuditAppender\.append\(\.\.\.\)`。

- AI 失败兜底时，`aiReview\.status=MANUAL\_REQUIRED`，`submission\.status=PENDING\_FINAL`。

## 1\. 任务状态机模块

### 1\.1 大功能：任务生命周期控制

交付效果：

- Owner 创建任务后，任务状态只能按合法路径迁移。

- 发布任务时，后端确保任务具备数据、模板、AI 配置和奖励规则。

小功能：

1. 创建任务

    - title。

    - description。

    - instructionRichText。

    - tags。

    - quota。

    - deadlineAt。

    - overlapCount。

2. 编辑任务

    - 仅 `DRAFT` 可编辑核心字段。

3. 发布任务

    - 调用 BE\-B 检查数据集是否存在。

    - 调用 BE\-B 检查模板版本是否存在。

    - 检查 AI 审核配置是否存在。

    - 调用 BE\-B 检查奖励规则是否存在。

4. 暂停任务。

5. 恢复任务。

6. 结束任务。

7. 调用 `AuditAppender\.append\(\.\.\.\)` 记录状态迁移。

状态机：

```Plaintext
DRAFT -> PUBLISHED
PUBLISHED -> PAUSED
PAUSED -> PUBLISHED
PUBLISHED -> ENDED
PAUSED -> ENDED
```

验收：

```Plaintext
非法状态迁移失败。
发布缺少数据集失败。
发布缺少模板版本失败。
发布缺少 AI 配置失败。
发布缺少奖励规则失败。
每次状态迁移通过 AuditAppender 写审计。
```

## 2\. 领取、草稿、提交模块

### 2\.1 大功能：任务广场数据

交付效果：

- Labeler 可以看到可领取任务，并了解奖励、截止时间、剩余题量。

小功能：

1. 查询发布中任务。

2. 排除已结束任务。

3. 聚合可领取数量。

4. 聚合当前用户领取数量。

5. 展示奖励摘要。

6. 支持 keyword、tag、状态筛选。

依赖：

- BE\-B 提供任务数据集 item 统计。

- BE\-B 提供奖励规则摘要。

验收：

```Plaintext
只返回 PUBLISHED 且未过期任务。
可领取数量准确。
无可领取题目时返回 availableCount=0。
```

### 2\.2 大功能：领取并发控制

交付效果：

- 多个 Labeler 同时领取时不超发。

小功能：

1. 查询可领取 item 快照。

2. 排除当前用户已领取 item。

3. 使用 BE\-B LockService 加锁。

4. 在模块化单体共享事务内调用 BE\-B `reserveClaimableItem\(\.\.\.\)`。

5. 创建 assignment。

6. 提交事务。

7. 事务失败时自动回滚 reservation 和 assignment。

规则：

- `assignedCount \&lt; overlapCount`。

- 同一用户不能重复领取同一 item。

- 任务必须是 `PUBLISHED`。

- 超过截止时间不能领取。

- MVP 采用模块化单体，共享本地事务；未来拆服务时改为 reservation confirm/release 模式。

验收：

```Plaintext
20 并发领取 overlapCount=1 不超发。
同一用户重复领取同 item 失败。
领取失败不污染 assignedCount。
领取成功通过 AuditAppender 写审计。
```

### 2\.3 大功能：草稿保存

交付效果：

- Labeler 输入内容可以恢复，不因刷新或短暂断网丢失。

小功能：

1. 保存草稿。

2. 读取草稿。

3. draftVersion 乐观锁。

4. 使用 BE\-B Redis 能力做热缓存。

5. MySQL 持久化。

6. 草稿保存审计。

验收：

```Plaintext
保存后刷新可恢复。
旧 clientVersion 保存失败。
Redis 缓存丢失时 MySQL 仍可恢复。
非 assignment owner 不能保存草稿。
```

### 2\.4 大功能：提交版本

交付效果：

- 每次提交都形成独立版本，打回修改不覆盖历史。

小功能：

1. 校验 assignment 属主。

2. 校验 assignment 状态。

3. 调用 BE\-B `validateAnswer\(schemaVersionId, answerJson\)`。

4. 计算 answerHash。

5. 幂等检测。

6. 旧版本标记 `SUPERSEDED`。

7. 创建新 submission version。

8. 推进状态到 `AI\_REVIEWING`。

9. 创建 AI 审核任务。

验收：

```Plaintext
首次提交 versionNo=1。
打回后重新提交 versionNo+1。
schema 校验失败不能提交。
重复提交不产生重复有效版本。
提交后进入 AI_REVIEWING。
```

## 3\. LLM Provider 与 Gateway 模块

### 3\.1 大功能：LLM Provider 管理

交付效果：

- Admin 可以配置模型厂商，AI 审核引擎可以选择并调用 Provider。

小功能：

1. 新增 Provider。

2. 编辑 Provider。

3. 启用 Provider。

4. 停用 Provider。

5. Provider 连通性测试。

6. API Key 加密存储。

7. Header 配置。

8. 模型名配置。

9. 限流配置读取。

规则：

- 支持 OpenAI\-compatible HTTP API。

- 不支持 Java jar 热加载。

- 停用 Provider 后不调度新 AI 审核。

- API Key 不明文返回前端。

- API Key 不写入日志。

验收：

```Plaintext
Provider 可新增。
Provider 可测试。
停用 Provider 后 AI 配置不可选择。
日志不出现 API Key。
```

### 3\.2 大功能：LlmGateway / Adapter

交付效果：

- AI 审核引擎通过统一 Gateway 调用不同兼容模型。

小功能：

1. `LlmGateway\.review\(request\)`。

2. OpenAI\-compatible 请求转换。

3. Header 注入。

4. 超时控制。

5. 错误码映射。

6. 原始响应保留。

7. 结构化 JSON 提取。

验收：

```Plaintext
能调用兼容接口。
超时返回明确错误。
非 JSON 输出能记录 rawResponse。
不同 Provider 通过同一 Gateway 调用。
```

## 4\. System Agent 与 Agent Run 模块

### 4\.1 大功能：System Agent 身份

交付效果：

- AI Agent 作为独立系统主体参与审计；系统用户由 BE\-B 创建和维护，BE\-A 只读取并使用。

小功能：

1. 读取 BE\-B 提供的 `system\_ai\_agent` profile。

2. 缓存 agentId。

3. 构造 `SystemActorContext`。

4. AI 审核时使用 `actorType=SYSTEM\_AGENT`。

5. 调用 `AuditAppender\.append\(\.\.\.\)` 写审计。

验收：

```Plaintext
BE-B 创建的 system_ai_agent 可被 BE-A 读取。
AI 审核写审计时 actorType=SYSTEM_AGENT。
actorId 指向 system_ai_agent。
```

### 4\.2 大功能：Agent Run

交付效果：

- 每一次 AI 审核运行都有独立运行实例。

小功能：

1. 创建 agentRun。

2. 记录 agentType。

3. 记录 submissionId。

4. 记录 providerId。

5. 记录 modelName。

6. 记录 promptVersion。

7. 记录 inputSnapshot。

8. 记录 outputSnapshot。

9. 记录 status。

10. 记录 errorMessage。

11. 记录 startedAt / finishedAt。

状态：

```Plaintext
PENDING
RUNNING
SUCCESS
FAILED
RATE_LIMITED
MANUAL_REQUIRED
```

验收：

```Plaintext
每次 AI 审核至少一条 agentRun。
失败重试产生新的 agentRun。
最终有效结果能指向 effectiveRunId。
audit_log 能引用 agentRunId。
```

## 5\. AI 审核配置与自动预审模块

### 5\.1 大功能：AI 审核配置

交付效果：

- Owner 可以为任务配置 AI 预审规则。

小功能：

1. 保存 AI 配置。

2. 选择 Provider。

3. 选择 model。

4. Prompt 模板。

5. 评分维度。

6. 通过阈值。

7. 人工复核阈值。

8. 结构化输出 JSON Schema。

9. Prompt 测试。

规则：

- 禁用 Provider 不能保存配置。

- 维度不能为空。

- Prompt 不能为空。

- AI 配置是任务发布前必需项。

验收：

```Plaintext
Owner 能保存 AI 配置。
禁用 Provider 不能选择。
Prompt 测试失败展示原因。
任务发布能读取配置。
```

### 5\.2 大功能：字段级 LlmTrigger

交付效果：

- Designer 预览和 Labeler 工作台可以触发模型辅助，输出作为标注参考或预填建议。

接口：

```Plaintext
POST /api/v1/llm/triggers/run
```

小功能：

1. 校验当前用户是否有任务访问权限。

2. 读取 task、templateVersion、componentId。

3. 校验 component 类型必须是 `LlmTrigger`。

4. 读取 Provider 和 model。

5. 组装字段级 Prompt。

6. 调用 LlmGateway。

7. 返回结构化辅助结果。

8. 写 agentRun。

9. 调用 AuditAppender 写审计。

规则：

- LlmTrigger 输出只作为参考，不直接提交。

- Labeler 必须确认后才可预填到 answerJson。

- Designer 预览调用不产生 submission。

- 调用也走 BE\-B RateLimitService。

- Prompt、输入、输出要可追溯。

验收：

```Plaintext
Designer 预览可触发 LlmTrigger。
Labeler 工作台可触发 LlmTrigger。
非 LlmTrigger componentId 调用失败。
输出不会自动绕过用户确认写入提交。
每次调用产生 agentRun。
```

### 5\.3 大功能：AI 自动预审

交付效果：

- Labeler 提交后，系统异步生成 AI 预审结果，并推进到人工终审。

流程：

```Plaintext
submission submitted
  -> create aiReviewResult PENDING
  -> create agentRun PENDING
  -> acquire rate limit from BE-B
  -> build prompt
  -> call LlmGateway
  -> validate structured output
  -> write aiReviewResult
  -> move submission to PENDING_FINAL
  -> if AI failed, set aiReview.status=MANUAL_REQUIRED and keep submission.status=PENDING_FINAL
```

小功能：

1. 审核任务入队。

2. 幂等键。

3. 调用 BE\-B RateLimitService。

4. Prompt 构造。

5. LLM 调用。

6. 结构化输出校验。

7. AI 结果入库。

8. submission 状态推进。

9. 调用 AuditAppender 写审计。

规则：

- AI `PASS` 只表示建议通过。

- AI 不允许写 `APPROVED`。

- Prompt 和 rawResponse 必须保存。

- 失败不能阻断人工审核。

- AI 失败兜底只改变 aiReview\.status，不把 MANUAL\_REQUIRED 写入 submission\.status。

验收：

```Plaintext
提交后生成 AI 审核任务。
AI 成功后 submission 进入 PENDING_FINAL。
AI 失败超过重试后 aiReview.status=MANUAL_REQUIRED，submission.status=PENDING_FINAL。
Reviewer 能看到 AI 建议。
```

### 5\.4 大功能：失败重试与人工兜底

交付效果：

- AI 服务不稳定时，审核链路仍能继续。

小功能：

1. 指数退避。

2. 最大重试次数。

3. RATE\_LIMITED 延迟重试。

4. FAILED 重试。

5. `aiReview\.status=MANUAL\_REQUIRED` 兜底。

6. 错误原因记录。

验收：

```Plaintext
限流命中不会丢任务。
超时会重试。
重试超过上限后 aiReview.status=MANUAL_REQUIRED，submission.status=PENDING_FINAL。
每次重试有独立 agentRun。
```

### 5\.5 大功能：Supervisor Agent 多轮审核

交付效果：

- AI 审核支持 Supervisor Agent 模式，LLM 作为编排者动态调用 tools 收集信息后做出审核决策。

- 向后兼容：通过 `agent_mode` 字段区分 DIRECT（单次调用）和 SUPERVISOR（多轮 Agent）模式。

- 可插拔 Tool 架构，新增 tool 只需实现接口并注册为 Spring Bean。

流程：

```Plaintext
submission submitted
  -> executeAttempt 根据 config.agentMode 分流
  -> DIRECT: 现有单次调用逻辑
  -> SUPERVISOR:
       -> acquire rate limit
       -> 组装 messages + tools schema
       -> Loop (max N iterations):
            -> 调用 LLM (function calling)
            -> 解析响应:
               - tool_calls → ToolRegistry 执行 tool → observation 回传
               - content (final answer) → 解析结构化结果
       -> 写 aiReviewResult
       -> move submission to PENDING_FINAL
```

小功能：

1. LLM 协议扩展（function calling）。

    - LlmMessage 支持 tool\_calls、tool role。

    - OpenAiCompatibleAdapter 支持 tools 参数和 tool\_calls 响应解析。

    - ToolCall、FunctionCall、ToolDefinition 协议记录。

2. Tool 框架。

    - ReviewTool 接口：name、description、parametersSchema、execute。

    - ToolContext：传递 submissionId、taskId、answerJson 等上下文。

    - ToolResult：success/error 结果封装。

    - ToolRegistry：自动发现 Spring Bean，按任务配置过滤可用 tools。

3. SupervisorAgent 执行循环。

    - 多轮对话循环，LLM 通过 function calling 选择 tool。

    - MAX\_ITERATIONS 上限兜底（默认 10）。

    - MAX\_TOOL\_CALLS\_PER\_TURN 单轮限制（默认 5）。

    - 超过迭代上限返回 MANUAL\_REQUIRED。

4. 初始 Tool 集合。

    - query\_submission\_history：查询 labeler 历史提交和通过率。

    - query\_task\_guidelines：获取任务标注规范。

    - detect\_anomaly\_pattern：检测答案异常模式。

    - verify\_format：校验答案 JSON 格式。

5. 配置模型。

    - ai\_review\_configs 增加 agent\_mode、enabled\_tools、max\_iterations 字段。

    - enabled\_tools 为 JSON 数组，null 表示全部启用。

6. 与现有代码集成。

    - AiAutoReviewService\.executeAttempt 按 agentMode 分流。

    - 重试逻辑（5.4）对 Supervisor 透明。

    - AgentRun 记录完整多轮对话历史。

规则：

- Tool 执行失败不触发重试，由 Supervisor LLM 自行决定如何处理。

- LLM 调用失败（超时、provider error）触发现有重试机制。

- Tool 只能访问当前 submission 相关数据（权限隔离）。

- 单个 tool 执行超时 5 秒。

- Supervisor 最终输出格式与 DIRECT 模式一致（decision、averageScore、dimensionScores、riskFlags、suggestion）。

验收：

```Plaintext
agent_mode=DIRECT 时走现有单次调用逻辑，行为不变。
agent_mode=SUPERVISOR 时 LLM 能通过 function calling 调用 tools。
Supervisor 超过 max_iterations 后 aiReview.status=MANUAL_REQUIRED。
新增 tool 只需实现 ReviewTool 接口 + @Component 注解即可自动注册。
enabled_tools 配置能限制可用 tool 范围。
每次 Supervisor 执行产生一条 AgentRun，outputSnapshot 包含完整对话历史。
```

## 6\. 人工终审与金标模块

### 6\.1 大功能：人工终审

交付效果：

- Reviewer 能根据 AI 建议和原始数据做终审。

- MVP 实现单 Reviewer 终审；数据模型保留 `reviewLevel`，P1 扩展初审、复审、终审。

小功能：

1. 待审列表。

2. 提交详情。

3. 历史版本。

4. AI 结果聚合。

5. 通过。

6. 打回。

7. 审计时间线。

8. reviewLevel。

规则：

- AI 结果只做建议。

- 打回必须有理由。

- 通过后生成金标。

- 通过后发布 `SubmissionApproved` 事件给 BE\-B。

- MVP 中 `reviewLevel=1` 表示终审。

- P1 可扩展 `reviewLevel=1/2/3` 对应初审、复审、终审。

验收：

```Plaintext
PENDING_FINAL 可通过。
通过后 APPROVED。
打回后 submission.status=REJECTED，assignment.status=RETURNED。
打回理由 Labeler 可见。
通过后发出 SubmissionApproved 事件。
reviewRecord 写入 reviewLevel。
```

### 6\.2 大功能：审核员批量操作

交付效果：

- Reviewer 可以批量处理普通待审样本。

小功能：

1. 批量通过。

2. 批量打回。

3. 批量标记人工复核。

4. 批量分配 Reviewer。

5. 逐条处理。

6. 部分成功返回。

规则：

- 冲突组不能批量通过。

- 批量打回必须有统一理由。

- 每条写 reviewRecord。

- 每条写 auditLog。

- 批量通过逐条发布 `SubmissionApproved`。

验收：

```Plaintext
普通待审样本可批量通过。
冲突样本批量通过失败。
部分失败不影响其他样本。
返回每条失败原因。
```

### 6\.3 大功能：冲突仲裁与金标选择

交付效果：

- 多人标注不一致时，Reviewer 可以选择金标。

小功能：

1. 聚合同 item submissions。

2. 计算 consensusScore。

3. 生成 conflictGroup。

4. 查询冲突详情。

5. 选择 goldenSubmission。

6. 仲裁理由。

7. 发布 `GoldenSelected` 事件给 BE\-B。

验收：

```Plaintext
overlapCount=1 不生成冲突。
overlapCount=2 且答案不一致生成冲突组。
选择金标后只有一个 isGolden。
选择金标后发出 GoldenSelected 事件。
```

### 6\.4 大功能：导出快照查询

交付效果：

- BE\-B 导出模块可以读取 BE\-A 管理的金标提交、AI 结果和审核引用。

小功能：

1. `queryExportableGoldenSubmissions\(taskId, pageRequest\)`。

2. 只返回 `submission\.status=APPROVED`。

3. 只返回 `isGolden=true`。

4. 返回 item 引用、answerJson、aiReview 摘要、audit 引用。

5. 支持分页游标。

规则：

- BE\-B 负责格式化和文件生成。

- BE\-A 负责导出范围的业务正确性。

- 导出查询不能绕过任务 owner 权限。

验收：

```Plaintext
导出快照只包含 APPROVED 金标。
分页查询稳定。
包含 AI 审核摘要。
包含 audit 引用而不是复制完整审计链。
```

## 7\. BE\-A 自测清单

```Plaintext
任务状态机非法迁移测试通过。
发布前跨模块检查测试通过。
领取并发测试通过。
草稿版本冲突测试通过。
提交调用 schema 校验测试通过。
提交版本递增测试通过。
LLM Provider 不回显 API Key。
SystemAgent profile 读取测试通过。
Agent Run 生命周期测试通过。
AI 审核成功链路测试通过。
AI 限流延迟重试测试通过。
AI 失败后 aiReview.status=MANUAL_REQUIRED 且 submission.status=PENDING_FINAL。
人工终审状态迁移测试通过。
批量审核部分成功测试通过。
冲突仲裁测试通过。
审核通过事件发布测试通过。
导出快照查询测试通过。
```

