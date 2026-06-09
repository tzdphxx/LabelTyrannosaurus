# LabelHub AI 使用场景与实现对比

> 本文档对比课题要求与现有代码中的 AI/LLM 使用场景，明确实现状态、差异和待调整项。

## 一、课题对 AI 的要求

课题文档中明确要求的 AI 相关能力：

| # | 课题章节 | 要求 | 重要程度 |
|---|----------|------|----------|
| 1 | 4.4 AI 自动预审 Agent | 可配置评测标准的审核 Agent，按维度打分，输出通过/打回/人工复核 | ⭐⭐⭐ 核心难点 |
| 2 | 4.2 LLM 交互组件 | 模板物料之一，字段级模型调用，输出可作为标注参考或预填 | ⭐⭐ 必须实现的物料 |
| 3 | 4.4 实现建议 | 异步任务队列 + Function Calling / 结构化输出 + 失败重试 + 幂等性 | 工程化要求 |
| 4 | 4.5 审核流转 | AI 预审结果可在审核工作台查看 AI 评语与原始 Prompt | 可追溯性要求 |

**课题未要求但有加分价值的**：
- Pre-Annotation（题目级预标注）
- SupervisorAgent（工具调用型 Agent）
- 多模态支持（图片/视频输入）

---

## 二、现有代码实现的 AI 场景

### 2.1 场景总览

| # | 场景 | 实现状态 | 课题对应 | 核心类 |
|---|------|----------|----------|--------|
| 1 | AI 自动预审 | ✅ 主体完成 | 4.4 核心难点 | `AiAutoReviewService` |
| 2 | LLM Trigger（字段级辅助） | ✅ 完整，已异步化 | 4.2 物料 | `LlmTriggerService` + `llm_trigger_runs` |
| 3 | Pre-Annotation（题目级预填） | ✅ 完整，已异步化 | 额外加分 | `PreAnnotationService` |
| 4 | SupervisorAgent（工具调用） | ✅ 完整 | 额外加分 | `SupervisorAgent` (182行) |
| 5 | LLM Provider 管理 | ✅ 已迁移 OWNER 私有隔离 | 基础设施 | `LlmProviderService` |
| 6 | LLM Gateway 基础设施 | ✅ 完整 | 基础设施 | `DefaultLlmGateway` (137行) |

---

## 三、各场景详细对比

### 3.1 AI 自动预审（课题 4.4 — 核心难点）

#### 课题要求

> 实现一个可配置评测标准的审核 Agent：
> - 任务负责人在后台配置审核 Prompt 模板与评分维度
> - 标注员提交后，自动入队，Agent 调用大模型按维度打分并给出通过/打回/人工复核结论
> - 审核结果与原数据一并入库，可在审核工作台查看 AI 评语与原始 Prompt
> - 异步任务队列 + Function Calling / 结构化输出 + 失败重试 + 幂等性

#### 代码实现

**流程**：
```
Labeler 提交 → AiReviewDispatcher.enqueue()
  → AiAutoReviewService.reviewSubmission()
    → 加载 AiReviewConfig（Provider + Prompt + 维度 + 阈值）
    → 构造 Prompt（含 item 数据 + answer + 模板）
    → 调用 LlmGateway.review()（支持 SupervisorAgent 模式）
    → 校验结构化输出
    → AiFlowDecisionService 决策（通过/打回/人工复核）
    → 写入 AiReviewResult + AgentRun
    → 推进 submission 状态到 PENDING_FINAL
```

**配置字段**（`AiReviewConfig`）：

| 字段 | 说明 |
|------|------|
| `providerId` | 使用的 LLM Provider |
| `modelName` | 具体模型名 |
| `promptTemplate` | 任务级 Prompt 模板 |
| `scoringDimensions` | 评分维度列表（如 accuracy, completeness, safety） |
| `passThreshold` | 通过阈值 |
| `manualReviewThreshold` | 人工复核阈值 |
| `rejectThreshold` | 打回阈值 |
| `outputSchema` | 期望的 LLM JSON 输出结构 |
| `maxRetry` | 最大重试次数 |
| `aiFlowPolicy` | 流转策略（见下方） |
| `allowAiDirectApprove` | 是否允许 AI 直接通过 |
| `allowAiDirectReject` | 是否允许 AI 直接打回 |
| `confidenceThreshold` | 置信度阈值 |
| `riskFlagsForceManual` | 风险标记强制人工 |
| `multimodalEnabled` | 是否启用多模态 |
| `degradationPenalty` | 降级惩罚系数（默认 0.2） |
| `visionDetail` | 图片精度（auto/low/high） |
| `maxImagesPerRequest` | 单次最大图片数 |

**AI 流转策略**（`aiFlowPolicy`）：

| 策略 | 说明 |
|------|------|
| `MANUAL_FIRST` | AI 只建议，始终进入人工审核 |
| `AI_PASS_ONLY` | AI 可直接通过，打回需人工确认 |
| `AI_REJECT_ONLY` | AI 可直接打回，通过需人工确认 |
| `AI_PASS_AND_REJECT` | AI 可直接通过和打回 |
| `ALWAYS_MANUAL` | 始终人工，AI 结果仅供参考 |

**结构化输出 Schema**：

```json
{
  "decision": "PASS | REJECT | MANUAL_REVIEW",
  "averageScore": 0.85,
  "dimensionScores": {"accuracy": 0.9, "completeness": 0.8},
  "riskFlags": ["potential_bias"],
  "suggestion": "建议通过，但需注意...",
  "confidence": 0.92,
  "limitations": ["无法验证外部链接"]
}
```

#### 对比结论

| 课题要求 | 实现情况 | 状态 |
|----------|----------|------|
| 可配置评测标准 | ✅ 完整的 AiReviewConfig，支持维度/阈值/策略 | 超出要求 |
| 按维度打分 | ✅ dimensionScores 字段 | 满足 |
| 输出通过/打回/人工复核 | ✅ 三种 decision + 五种 aiFlowPolicy | 超出要求 |
| 异步任务队列 | ✅ AI Review、Pre-Annotation、LlmTrigger 均已接入 Redis Stream 业务队列；Provider 连通性测试保留同步 | 满足课题工程化要求 |
| Function Calling / 结构化输出 | ✅ outputSchema + JSON 提取 + SupervisorAgent 工具调用 | 超出要求 |
| 失败重试 | ✅ 指数退避 + 限流延迟 + 最大次数 | 满足 |
| 幂等性 | ✅ hash 去重 | 满足 |
| 审核工作台查看 AI 评语 | ✅ AiReviewResult 入库，含 Prompt + rawResponse | 满足 |

**结论**：主体满足课题要求，并在多模态、策略灵活性、SupervisorAgent 方面做了增强。AI 直接通过 / 直接打回属于增强策略，默认仍建议以人工复核为安全基线。

---

### 3.2 LLM 交互组件 / LlmTrigger（课题 4.2 — 必须物料）

#### 课题要求

> LLM 交互组件：字段级模型调用，输出可作为标注参考或预填。

#### 代码实现

**流程**：
```
Owner 在 Designer 中拖入 LlmTrigger 组件
  → 配置 providerId、modelName、promptTemplate、targetFields
  → 保存到模板 schema JSON

Labeler 在标注工作台点击触发按钮
  → POST /api/v1/llm/triggers/run
  → LlmTriggerService.run()
    → 从模板 schema 递归查找 componentId 对应的 LlmTrigger 组件
    → 校验访问权限（Labeler 拥有 assignment / Owner 预览模式）
    → 创建 llm_trigger_runs 与 AgentRun
    → 写入 Redis Stream，接口立即返回 RUNNING
    → LlmTaskWorker 消费后检查限流
    → 构造 Prompt（item 数据 + 当前答案 + 组件模板）
    → 调用 LlmGateway.review()
    → 返回结构化结果（targetFields 值映射）
    → 写 AgentRun 审计
```

**请求参数**（`LlmTriggerRunRequest`）：

| 字段 | 说明 |
|------|------|
| `taskId` | 任务 ID |
| `templateVersionId` | 模板版本 ID |
| `datasetItemId` | 当前题目 ID |
| `componentId` | LlmTrigger 组件 ID |
| `currentAnswerJson` | 当前已填写的答案 |
| `previewMode` | 是否预览模式（Owner 用） |
| `assignmentId` | 领取记录 ID（Labeler 用） |

**模板 Schema 中的组件定义**：

```json
{
  "type": "LlmTrigger",
  "id": "ai_assist_1",
  "label": "AI 辅助填写",
  "providerId": 123,
  "modelName": "gpt-4o",
  "promptTemplate": "根据题目内容，为 {{targetField}} 生成建议答案...",
  "targetFields": ["answer_text", "quality_score"]
}
```

#### 对比结论

| 课题要求 | 实现情况 | 状态 |
|----------|----------|------|
| 字段级模型调用 | ✅ 组件绑定 targetFields，单组件对应特定字段 | 满足 |
| 输出可作为标注参考 | ✅ 返回结果展示给 Labeler | 满足 |
| 输出可预填 | ✅ Labeler 确认后预填到目标字段 | 满足 |
| 不自动提交 | ✅ 需要 Labeler 手动确认 | 满足 |

**结论**：满足课题要求，并已改为 Redis Stream 异步执行。前端通过 `GET /api/v1/llm/triggers/runs/{triggerRunId}` 查询结果。

---

### 3.3 Pre-Annotation / 预标注（课题未要求 — 加分项）

#### 定位

课题未明确要求，但代码中实现了完整的题目级预标注功能。与 LlmTrigger 的区别是粒度：Pre-Annotation 一次生成整题所有字段的建议。

#### 代码实现

**流程**：
```
Labeler 点击「AI 预填」按钮
  → POST /api/v1/assignments/{assignmentId}/pre-annotations/run
  → PreAnnotationService.run()
    → 加载 assignment + task
    → 加载 AiReviewConfig（复用 AI 审核配置）
    → 创建 PreAnnotation 与 AgentRun
    → 写入 Redis Stream，接口立即返回 RUNNING
    → Worker 构造预标注 Prompt 并调用 LlmGateway.review()
    → 校验输出结构（须含 suggestedAnswerJson、fieldSuggestions 等）
    → 过滤字段（移除 ShowItem 等不可提交字段）
    → 应用降级惩罚（如 Provider 不支持图片）
    → 存入 PreAnnotation 表
    → 返回建议给 Labeler
```

**输出结构**：

```json
{
  "suggestedAnswerJson": {"field1": "value1", "field2": "value2"},
  "fieldSuggestions": [
    {"field": "answer_text", "value": "...", "confidence": 0.85, "reason": "基于题目描述推断"}
  ],
  "riskFlags": [],
  "overallConfidence": 0.86,
  "limitations": ["无法解析题目中的图片"],
  "promptMode": "TEXT_ONLY",
  "degraded": false
}
```

**状态机**：`PENDING → RUNNING → SUCCESS / FAILED / RATE_LIMITED / MANUAL_REQUIRED`

#### 与 LlmTrigger 的对比

| 维度 | LlmTrigger（课题要求） | Pre-Annotation（加分项） |
|------|------------------------|------------------------|
| 粒度 | 字段级 | 题目级 |
| 配置来源 | 模板 schema 中的组件属性 | 复用任务的 AI 审核配置 |
| Provider 来源 | 组件内嵌 providerId | AI 审核配置中的 providerId |
| 触发时机 | 标注过程中，针对单个字段 | 开始作答前，一次性生成全部建议 |
| 输出 | 单字段/少数字段的值 | 整题 suggestedAnswerJson + 逐字段 confidence |
| 用户交互 | 点击组件按钮 → 查看结果 → 确认预填 | 点击「AI 预填」→ 查看建议 → 选择性采纳 |

---

### 3.4 SupervisorAgent / 工具调用型 Agent（课题未要求 — 加分项）

#### 定位

在 AI 自动预审的基础上，支持 Agent 通过工具调用（Tool Use）进行多步推理。

#### 代码实现

**流程**：
```
AiAutoReviewService 检测到任务配置启用 SupervisorAgent 模式
  → SupervisorAgent.execute()
    → 初始化 messages（system + user prompt）
    → 循环（最多 maxIterations=10 次）：
      → 调用 LLM（附带 ToolDefinition 列表）
      → 解析响应：
        → 如果有 tool_calls → 执行工具 → 收集结果 → 继续循环
        → 如果有 content（无 tool_calls）→ 解析为 JSON → 返回结果
    → 超时/超次数 → 返回失败
```

**工具注册机制**：
- `ToolRegistry` 管理可用工具
- `ReviewTool` 接口定义工具行为
- `ToolContext` 提供执行上下文（submission, task, item, labeler, answer 等）

**与普通 AI 审核的关系**：
- 普通模式：一次 LLM 调用，直接输出结构化结果
- SupervisorAgent 模式：多轮 LLM 调用，可使用工具查询额外信息后再给出结论

---

## 四、完整操作流程（按角色视角）

### 4.1 Owner 配置流程

Owner 需要完成三项 AI 相关配置才能发布任务：

#### 步骤 1：管理 LLM Provider

**现有接口**（✅ 已迁移到 OWNER 权限）：

| 操作 | 方法 | 路径 | 说明 |
|------|------|------|------|
| 查看 Provider 列表 | GET | `/api/v1/llm-providers` | OWNER 只能查看自己的 Provider |
| 创建 Provider | POST | `/api/v1/llm-providers` | 写入 `owner_id`，OWNER 私有 |
| 编辑 Provider | PUT | `/api/v1/llm-providers/{id}` | 仅 Provider Owner 可操作 |
| 启用 Provider | POST | `/api/v1/llm-providers/{id}/enable` | 仅 Provider Owner 可操作 |
| 停用 Provider | POST | `/api/v1/llm-providers/{id}/disable` | 仅 Provider Owner 可操作 |
| 测试连通性 | POST | `/api/v1/llm-providers/{id}/test` | 仅 Provider Owner 可操作 |

**创建 Provider 时需要的字段**：

| 字段 | 类型 | 说明 |
|------|------|------|
| `providerCode` | String | 厂商标识（如 openai、doubao、qwen） |
| `providerName` | String | 显示名称 |
| `baseUrl` | String | API 地址（OpenAI 兼容格式） |
| `apiKey` | String | API Key（加密存储，不回显） |
| `defaultModel` | String | 默认模型名 |
| `customHeaders` | Map | 自定义请求头 |
| `platformRateLimitPerMinute` | Integer | 全局限流 |
| `taskRateLimitPerMinute` | Integer | 任务级限流 |
| `userRateLimitPerMinute` | Integer | 用户级限流 |
| `supportVision` | Boolean | 是否支持图片输入 |
| `supportMultiImage` | Boolean | 是否支持多图 |
| `maxImageCount` | Integer | 最大图片数 |

#### 步骤 2：配置 AI 审核规则

**接口**（✅ 已实现，OWNER 权限）：

| 操作 | 方法 | 路径 |
|------|------|------|
| 保存 AI 审核配置 | POST | `/api/v1/tasks/{taskId}/ai-review-configs` |
| 更新 AI 审核配置 | PUT | `/api/v1/tasks/{taskId}/ai-review-configs/{configId}` |
| 查询 AI 审核配置 | GET | `/api/v1/tasks/{taskId}/ai-review-configs` |
| 测试 Prompt | POST | `/api/v1/tasks/{taskId}/ai-review-configs/{configId}/test` |

**配置字段**：

```json
{
  "providerId": 1,
  "modelName": "gpt-4o",
  "promptTemplate": "你是一个数据标注质量审核员。请根据以下评分维度对标注结果进行评估...",
  "scoringDimensions": ["accuracy", "completeness", "format_compliance", "safety"],
  "passThreshold": 0.8,
  "manualReviewThreshold": 0.6,
  "rejectThreshold": 0.4,
  "outputSchema": {"type": "object", "properties": {...}},
  "maxRetry": 3,
  "aiFlowPolicy": "MANUAL_FIRST",
  "allowAiDirectApprove": false,
  "allowAiDirectReject": false,
  "confidenceThreshold": 0.7,
  "riskFlagsForceManual": true,
  "multimodalEnabled": true,
  "degradationPenalty": 0.2,
  "visionDetail": "auto",
  "maxImagesPerRequest": 5
}
```

**Prompt 测试请求**：

```json
POST /api/v1/tasks/{taskId}/ai-review-configs/{configId}/test
{
  "itemSnapshot": {"question": "示例题目...", "context": "..."},
  "answerJson": {"answer_text": "示例答案..."}
}
```

**Prompt 测试响应**：

```json
{
  "agentRunId": 789,
  "status": "SUCCESS",
  "contentText": "原始 LLM 输出文本",
  "structuredJson": {"decision": "PASS", "averageScore": 0.85, ...},
  "rawResponse": "完整原始响应",
  "latencyMs": 2340,
  "errorCode": null,
  "errorMessage": null
}
```

#### 步骤 3：在模板中配置 LlmTrigger 组件

**方式**：Owner 在 Template Designer 中拖入 LlmTrigger 组件，配置保存在模板 schema JSON 中。

**无独立接口**——LlmTrigger 配置嵌入 `TemplateVersion.schemaJson`：

```json
{
  "components": [
    {
      "type": "ShowItem",
      "id": "show_question",
      "label": "题目内容",
      "dataPath": "question"
    },
    {
      "type": "Textarea",
      "id": "answer_text",
      "label": "标注答案",
      "field": "answer_text",
      "required": true
    },
    {
      "type": "LlmTrigger",
      "id": "ai_assist_1",
      "label": "AI 辅助填写",
      "providerId": 1,
      "modelName": "gpt-4o",
      "promptTemplate": "根据题目 {{question}}，为 answer_text 字段生成高质量答案",
      "targetFields": ["answer_text"]
    }
  ]
}
```

**Owner 预览测试**：

```json
POST /api/v1/llm/triggers/run
{
  "taskId": 1,
  "templateVersionId": 5,
  "componentId": "ai_assist_1",
  "datasetItemId": 100,
  "currentAnswerJson": {},
  "previewMode": true
}
```

---

### 4.2 Labeler 使用 AI 辅助的流程

Labeler 有两种方式获得 AI 辅助：

#### 方式 A：LlmTrigger 字段级辅助（课题要求）

**触发**：Labeler 在标注工作台点击 LlmTrigger 组件上的「AI 辅助」按钮。

**请求**：

```json
POST /api/v1/llm/triggers/run
{
  "taskId": 1,
  "templateVersionId": 5,
  "componentId": "ai_assist_1",
  "datasetItemId": 100,
  "assignmentId": 200,
  "currentAnswerJson": {"answer_text": "已填写的部分内容..."},
  "previewMode": false
}
```

**权限校验**：
- `CurrentUserContext.requireCurrentUser()` 获取当前用户
- 校验 `assignmentId` 属于当前 Labeler
- 校验 assignment 的 taskId、templateVersionId 与请求一致

**响应**：

```json
{
  "agentRunId": 456,
  "componentId": "ai_assist_1",
  "suggestionJson": {"answer_text": "AI 生成的建议答案内容..."},
  "displayText": "基于题目内容，建议答案如下...",
  "targetFields": ["answer_text"],
  "rawModelSummary": "模型输出摘要",
  "status": "SUCCESS",
  "latencyMs": 1850,
  "errorCode": null,
  "errorMessage": null
}
```

**前端交互**：
1. 展示 `suggestionJson` 中的建议值
2. Labeler 查看后点击「采纳」→ 预填到对应字段
3. 或点击「忽略」→ 不做任何操作
4. **绝不自动写入提交**

#### 方式 B：Pre-Annotation 题目级预填（加分项）

**触发**：Labeler 在标注工作台点击「AI 预填」按钮。

**请求**：

```json
POST /api/v1/assignments/{assignmentId}/pre-annotations/run
{
  "templateVersionId": 5,
  "datasetItemId": 100,
  "currentAnswerJson": "{}",
  "mode": null
}
```

**权限校验**：
- `CurrentUserContext.requireRole(RoleCode.LABELER)`
- 校验 assignment 属于当前 Labeler

**响应**：

```json
{
  "preAnnotationId": 789,
  "assignmentId": 200,
  "agentRunId": 790,
  "status": "SUCCESS",
  "suggestedAnswerJson": {
    "answer_text": "AI 建议的完整答案...",
    "quality_score": "HIGH"
  },
  "fieldSuggestions": [
    {"field": "answer_text", "value": "AI 建议的完整答案...", "confidence": 0.88, "reason": "基于题目描述和上下文推断"},
    {"field": "quality_score", "value": "HIGH", "confidence": 0.72, "reason": "答案结构完整，逻辑清晰"}
  ],
  "riskFlags": [],
  "overallConfidence": 0.80,
  "limitations": ["无法验证外部链接的有效性"],
  "promptMode": "TEXT_ONLY",
  "degraded": false,
  "ignoredFields": ["show_question"],
  "createdAt": "2026-06-01T10:30:00"
}
```

**查询最新预标注结果**：

```
GET /api/v1/assignments/{assignmentId}/pre-annotations/latest
```

**前端交互**：
1. 展示 `fieldSuggestions` 列表，每个字段显示建议值 + 置信度 + 理由
2. Labeler 可逐字段选择「采纳」或「忽略」
3. 采纳后预填到表单对应字段
4. **绝不自动提交**

---

### 4.3 AI 自动预审执行流程（系统自动）

**触发时机**：Labeler 提交答案后，系统自动执行。

#### 完整执行链路

```
┌─────────────────────────────────────────────────────────────────┐
│ Labeler 点击「提交」                                              │
│   → SubmissionSubmitService.submit()                            │
│     → 校验 assignment 属主                                       │
│     → 校验 assignment 状态（CLAIMED / RETURNED）                  │
│     → 调用 schema 校验（validateAnswer）                          │
│     → 计算 answerHash（SHA-256）                                 │
│     → 幂等检测（相同 hash 不重复创建）                              │
│     → 旧版本标记 SUPERSEDED                                      │
│     → 创建 Submission（status = AI_REVIEWING）                   │
│     → 创建 AgentRun（status = PENDING）                          │
│     → aiReviewDispatcher.enqueue(submissionId)                  │
│     → 写审计日志                                                  │
└─────────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────────┐
│ AiReviewDispatcher 调度                                          │
│   → 当前主链路：写入 Redis Stream 业务队列                           │
│   → LlmTaskWorker 按 taskType 分发到 AI Review / 预标注 / LlmTrigger │
│   → Provider 连通性测试保留同步，用于配置页即时反馈                  │
└─────────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────────┐
│ AiAutoReviewService.reviewSubmission(submissionId)               │
│                                                                  │
│ 【事务 1：准备阶段】                                               │
│   → 幂等检查：AiReviewResult 已存在则直接返回                       │
│   → 加载 Submission + Task + AiReviewConfig + DatasetItem        │
│   → MediaPromptContextBuilder 构造 Prompt（支持多模态）             │
│   → 启动 AgentRun（status → RUNNING）                            │
│                                                                  │
│ 【无事务：远程 LLM 调用】                                          │
│   → 获取限流令牌（AiReviewRateLimiter）                            │
│   → 分支：                                                       │
│     → Direct 模式：单次 LlmGateway.review() 调用                  │
│     → SupervisorAgent 模式：多轮调用 + 工具执行循环                  │
│   → 校验结构化输出（必须有 decision 字段）                           │
│   → 应用降级惩罚（如 Provider 不支持视觉）                          │
│                                                                  │
│ 【事务 2：结果持久化】                                              │
│   → 成功：写入 AiReviewResult + 完成 AgentRun                     │
│   → 失败：                                                       │
│     → 可重试 → 指数退避调度重试（新 AgentRun）                      │
│     → 不可重试/超次数 → MANUAL_REQUIRED                           │
│   → AiFlowDecisionService 决策：                                 │
│     → AI_DIRECT_APPROVE → 单人任务可直接 APPROVED + isGolden       │
│     → AI_DIRECT_REJECT → submission 直接 REJECTED 并退回修改       │
│     → AI_ASSIGN_MANUAL_REVIEW → submission 进入 PENDING_FINAL     │
│   → 写审计日志（actorType = SYSTEM_AGENT）                        │
└─────────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────────┐
│ Reviewer 在审核工作台可见：                                        │
│   → AI 评分（各维度 + 总分）                                       │
│   → AI 结论（PASS / REJECT / MANUAL_REVIEW）                     │
│   → AI 置信度                                                    │
│   → 风险标记                                                     │
│   → AI 建议文本                                                  │
│   → 原始 Prompt                                                  │
│   → 原始 LLM 响应（rawResponse）                                  │
│   → AgentRun 详情（耗时、重试次数、错误信息）                        │
└─────────────────────────────────────────────────────────────────┘
```

#### AI 决策逻辑（AiFlowDecisionService）

```
输入：decision + averageScore + confidence + riskFlags + aiFlowPolicy

if (riskFlagsForceManual && riskFlags 非空):
    → MANUAL_REVIEW（强制人工）

switch (aiFlowPolicy):
  case ALWAYS_MANUAL:
    → MANUAL_REVIEW

  case MANUAL_FIRST:
    → MANUAL_REVIEW（AI 结果仅供参考）

  case AI_PASS_ONLY:
    if (decision == PASS && score >= passThreshold && confidence >= confidenceThreshold):
      → AI_DIRECT_APPROVE
    else:
      → MANUAL_REVIEW

  case AI_REJECT_ONLY:
    if (decision == REJECT && score <= rejectThreshold):
      → AI_DIRECT_REJECT
    else:
      → MANUAL_REVIEW

  case AI_PASS_AND_REJECT:
    if (decision == PASS && score >= passThreshold && confidence >= confidenceThreshold):
      → AI_DIRECT_APPROVE
    elif (decision == REJECT && score <= rejectThreshold):
      → AI_DIRECT_REJECT
    else:
      → MANUAL_REVIEW
```

#### 失败重试机制

```
第 1 次失败 → 等待 2s → 重试
第 2 次失败 → 等待 4s → 重试
第 3 次失败 → 等待 8s → 重试
超过 maxRetry → 标记 MANUAL_REQUIRED
  → submission.status 仍为 PENDING_FINAL
  → Reviewer 可见，可手动触发重试或直接终审
```

每次重试产生新的 AgentRun 记录，确保完整可追溯。

---

## 四、LLM 基础设施

### 4.1 LlmGateway

**统一接口**，所有 AI 场景通过此 Gateway 调用 LLM：

```java
public interface LlmGateway {
    LlmGatewayResponse review(LlmGatewayRequest request);
}
```

**请求结构**：

| 字段 | 说明 |
|------|------|
| `providerId` | Provider ID |
| `modelName` | 模型名 |
| `messages` | 消息列表（system + user，支持多模态） |
| `toolDefinitions` | 工具定义（SupervisorAgent 用） |

**响应结构**：

| 字段 | 说明 |
|------|------|
| `status` | SUCCESS / RATE_LIMITED / FAILED |
| `contentText` | LLM 原始文本输出 |
| `structuredJson` | 提取的结构化 JSON |
| `rawResponse` | 完整原始响应（可追溯） |
| `latencyMs` | 调用耗时 |
| `errorCode` / `errorMessage` | 错误信息 |

### 4.2 OpenAiCompatibleAdapter

**协议适配器**，支持所有 OpenAI 兼容 API（通义千问、豆包等）：

- HTTP 客户端实现
- 多模态消息序列化（`contentParts`：TextPart / ImageUrlPart）
- 工具/函数调用序列化
- 超时控制（文本 30s / 视觉 60s）
- API Key 脱敏（错误日志中不暴露）
- URL 安全校验（生产环境禁止私有/回环地址）

### 4.3 多模态支持

| 能力 | 说明 |
|------|------|
| 图片输入 | 通过 ImageUrlPart 传入图片 URL |
| 视觉精度 | auto / low / high 三档 |
| Provider 能力检测 | `supportVision`、`supportMultiImage`、`maxImageCount` |
| 优雅降级 | Provider 不支持视觉时自动降级为纯文本，并对 confidence 施加惩罚 |
| 降级惩罚 | 默认 0.2，可配置 |

### 4.4 限流机制

| 层级 | 作用 | 实现 |
|------|------|------|
| Provider 级 | 全局 QPS 保护 | `platformRateLimitPerMinute` |
| 任务级 | 防止单任务占满配额 | `taskRateLimitPerMinute` |
| 用户级 | 防止单用户滥用 | `userRateLimitPerMinute` |

两套独立限流器：
- `AiReviewRateLimiter` — AI 自动预审 + Pre-Annotation
- `LlmTriggerRateLimiter` — 字段级 LlmTrigger

### 4.5 重试与恢复

| 机制 | 说明 |
|------|------|
| 指数退避 | `AiReviewRetryStrategy` |
| 限流延迟重试 | RATE_LIMITED 状态延后重试 |
| 最大重试次数 | 超过后标记 MANUAL_REQUIRED |
| 手动重试 | Reviewer 可触发手动重试 |
| 恢复运行器 | `AiReviewRecoveryRunner` 处理卡住的任务 |

---

## 五、课题切合度总结

### 5.1 已满足或主体满足的要求

| 课题要求 | 对应实现 | 评价 |
|----------|----------|------|
| 可配置评测标准 | AiReviewConfig 15+ 配置字段 | 远超最低要求 |
| 按维度打分 | dimensionScores | 完整 |
| 通过/打回/人工复核三结论 | decision 字段 + aiFlowPolicy 5 种策略 | 远超最低要求 |
| 异步任务队列 | `LlmTaskQueueService` + Redis Stream + `LlmTaskWorker`，覆盖 AI Review、预标注和 LlmTrigger | 完整 |
| Function Calling / 结构化输出 | outputSchema + SupervisorAgent 工具调用 | 远超最低要求 |
| 失败重试 + 幂等性 | 指数退避 + hash 去重 | 完整 |
| 审核工作台可查看 AI 评语 | AiReviewResult 含 Prompt + rawResponse | 完整 |
| LLM 交互组件 | LlmTriggerService | 完整 |
| 字段级模型调用 | 组件绑定 targetFields | 完整 |
| 输出可作为参考或预填 | 确认后预填，不自动提交 | 完整 |

### 5.2 超出课题要求的实现（答辩加分项）

| 额外实现 | 技术亮点 | 答辩话术 |
|----------|----------|----------|
| Pre-Annotation 预标注 | 题目级整体预填，提升标注效率 | 「除了字段级辅助，我们还实现了题目级预标注，一键生成全部字段建议」 |
| SupervisorAgent 工具调用 | 多步推理，可调用外部工具 | 「审核 Agent 支持 Tool Use，可在审核过程中查询额外信息后再给出结论」 |
| 多模态支持 | 图片输入 + 视频关键帧/转写/说明消费 + 优雅降级 | 「支持多模态标注数据的 AI 审核；视频场景基于 BE-B/FE 提供的关键帧、转写或人工说明进入 AI 链路」 |
| AI 流转策略 | 5 种 Policy 灵活配置 | 「Owner 可灵活配置 AI 的决策权限，从纯建议到自动审批均可」 |
| 置信度 + 风险标记 | confidence + riskFlags 细粒度控制 | 「AI 输出带置信度和风险标记，低置信度自动转人工」 |

### 5.3 与课题存在差异或后续可增强的点

| 差异项 | 现状 | 课题期望 | 调整方案 |
|--------|------|----------|----------|
| LLM Provider 权限 | 已迁移到 OWNER 管理 | Owner 配置审核标准（隐含 Provider 管理） | 已通过 `/api/v1/llm-providers` 和 `owner_id` 隔离实现 |
| Provider 隔离 | OWNER 私有 | 每个 Owner 独立管理自己的 Provider | 已在 Provider 管理、AI 审核配置和 LlmTrigger 调用处校验 |
| AI 审核队列 | 主链路为 Redis Stream 业务队列 | 课题建议异步任务队列 | 已接入主 dispatcher；Redis 消息只保存业务 ID，不保存 Prompt、答案或 API Key |
| 视频多模态 | 消费关键帧、转写文本和人工说明 | 多模态数据进入 AI 审核链路 | BE-A 不生成关键帧或 ASR，只负责消费 BE-B/FE 提供的媒体上下文并记录降级信息 |

---

## 六、代码文件索引

| 文件 | 路径 | 职责 |
|------|------|------|
| AiAutoReviewService | `modules/ai/service/AiAutoReviewService.java` | AI 自动预审主流程 |
| AiReviewConfigService | `modules/ai/service/AiReviewConfigService.java` | AI 审核配置管理 |
| AiFlowDecisionService | `modules/ai/service/AiFlowDecisionService.java` | AI 流转决策 |
| AiReviewDispatcher | `modules/ai/service/AiReviewDispatcher.java` | 调度接口（同步/异步） |
| AiReviewRetryService | `modules/ai/service/AiReviewRetryService.java` | 重试逻辑 |
| LlmTriggerService | `modules/ai/service/LlmTriggerService.java` | 字段级 LLM 触发 |
| LlmTriggerController | `modules/ai/web/LlmTriggerController.java` | LlmTrigger API |
| PreAnnotationService | `modules/preannotation/service/PreAnnotationService.java` | 预标注服务 |
| PreAnnotationController | `modules/preannotation/web/PreAnnotationController.java` | 预标注 API |
| SupervisorAgent | `modules/ai/service/SupervisorAgent.java` | 工具调用型 Agent |
| AgentRunService | `modules/agent/service/AgentRunService.java` | Agent 运行记录管理 |
| DefaultLlmGateway | `infrastructure/llm/DefaultLlmGateway.java` | LLM 统一网关 |
| OpenAiCompatibleAdapter | `infrastructure/llm/OpenAiCompatibleAdapter.java` | OpenAI 协议适配 |
| LlmProviderService | `modules/ai/service/LlmProviderService.java` | Provider 管理 |
| LlmProviderController | `modules/ai/web/LlmProviderController.java` | Provider API |
| AiReviewRateLimiter | `modules/ai/service/AiReviewRateLimiter.java` | AI 审核限流 |
| LlmTriggerRateLimiter | `modules/ai/service/LlmTriggerRateLimiter.java` | Trigger 限流 |
| MediaPromptResult | `modules/ai/service/MediaPromptResult.java` | 多模态 Prompt 构建结果 |

---

## 七、变更记录

| 日期 | 变更内容 |
|------|----------|
| 2026-06-01 | 初始版本，完成课题要求与代码实现的全面对比 |
