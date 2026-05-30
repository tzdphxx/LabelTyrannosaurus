# LabelHub详细设计与开发文档

# LabelHub 数据标注平台详细设计与开发文档

## 0\. 设计边界

本文档面向前后端和移动端协作开发，重点描述业务模块、状态流转、核心实现逻辑和异常边界。接口与表结构只保留开发必需粒度，不展开大量字段样例和 SQL DDL。

已确认的关键决策：

- 分发策略：先到先得 \+ 任务级配额 \+ 用户领取锁定。

- 多人标注：任务级 `overlap\_count` 可配置，默认 `1`。

- 冲突仲裁：系统计算一致性；冲突样本进入人工终审，由 Reviewer 选择最佳版本为金标。

- 审核流：`Labeler 提交 \-\&gt; AI 预审 \-\&gt; 人工终审 \-\&gt; APPROVED 可导出`。

- AI 权限：AI 只给建议，不直接终审。

- 模板版本：任务发布后不可原地修改，只能 fork 新版本。

- 注册登录：开放注册，新用户默认 `Labeler`。

- 权限角色：`Admin`、`Owner`、`Labeler`、`Reviewer`、`SYSTEM\_AGENT`。

- LLM：Spring AI \+ OpenAI\-compatible Adapter，支持配置级热插拔，不支持 Java jar 热加载。

- 限流：Redisson `RRateLimiter`。

- 存储：对象存储，开发默认 MinIO。

- 导出：默认只导出 `APPROVED` 金标数据，可选包含 `auditTrail` / `aiReview`。

- 移动端：不单独做 App，React 响应式兼容。

- 奖励：只做平台内虚拟奖励；最终通过一条计一条，打回不计。

## 1\. 全局业务流

```mermaid
flowchart TD
    Admin[Admin] --> UserRole[用户与角色管理]
    Admin --> Provider[LLM Provider 配置]

    Owner[Owner] --> Task[创建任务]
    Task --> Dataset[导入数据集]
    Task --> Template[搭建模板并生成版本]
    Task --> AiConfig[配置 AI 审核规则]
    AiConfig --> Publish[发布任务]

    Labeler[Labeler] --> Market[任务广场]
    Market --> Claim[领取题目]
    Claim --> Draft[作答与草稿]
    Draft --> Submit[提交标注版本]

    Submit --> AiQueue[AI 预审队列]
    AiQueue --> AiReview[LLM 结构化审核]
    AiReview --> FinalReview[人工终审]

    FinalReview -->|通过| Golden[金标数据]
    FinalReview -->|打回| Revise[标注员修改]
    Revise --> Submit

    Golden --> Export[异步导出]```

核心原则：

- 任务、题目、领取、提交、审核全部用状态机驱动，禁止靠前端按钮状态推断业务状态。

- 所有关键状态迁移写 `audit\_log`。

- AI 审核、导入、导出都异步化，前端轮询任务状态。

- 用户提交的数据永不覆盖历史版本，打回修改产生新的 `submission\_version`。

## 2\. 账户与权限模块

### 2\.1 角色职责

|角色|职责|
|---|---|
|Admin|用户管理、角色授权、LLM 厂商配置、平台级限流配置|
|Owner|创建任务、导入数据、搭建模板、发布任务、查看结果、导出数据|
|Labeler|浏览任务、领取题目、保存草稿、提交标注、修改打回数据|
|Reviewer|查看 AI 预审结果、人工终审、打回、解决冲突、选择金标|
|SYSTEM\_AGENT|后台 AI 审核主体，不允许登录，只用于系统审计|

### 2\.2 注册登录逻辑

开放注册只创建 `Labeler`。如果用户需要成为 `Owner` 或 `Reviewer`，必须由 `Admin` 授权。

实现要点：

- 登录使用 JWT，包含 `userId`、`roles`、`tokenVersion`。

- 角色变更后递增 `tokenVersion`，旧 token 自动失效。

- Admin 不能删除最后一个 Admin，避免系统无管理员。

- 密码只存 BCrypt hash。

核心接口：

```Plaintext
POST /api/v1/auth/register
POST /api/v1/auth/login
POST /api/v1/auth/refresh
GET  /api/v1/users/me
PUT  /api/v1/admin/users/{userId}/roles
```

### 2\.3 System Agent 身份

AI Agent 必须具备独立账户视角，但不能作为普通用户登录后台。

采用 `System Principal \+ Agent Run` 模型：

- 初始化内置账号 `system\_ai\_agent`。

- `user\_type = SYSTEM`。

- `login\_enabled = false`。

- 角色为 `SYSTEM\_AGENT`。

- 所有 AI 审核造成的状态迁移，`audit\_log\.actor\_id` 指向该账号。

- 每次 AI 审核运行生成一条 `agent\_run`，用于追溯模型、Prompt、输入、输出和错误。

审计语义：

```Plaintext
谁触发状态变化：system_ai_agent
哪次 Agent 运行触发：agent_run_id
使用哪个模型：provider_id + model_name
基于什么输入：input_snapshot
产出什么结论：output_snapshot
是否影响终审：不会，AI 只给建议
```

禁止事项：

- 禁止 `system\_ai\_agent` 获取登录 token。

- 禁止前端把 `SYSTEM\_AGENT` 当普通用户展示在成员列表里。

- 禁止 AI Agent 直接把 submission 改成 `APPROVED`。

## 3\. LLM Provider 模块

### 3\.1 配置级热插拔

系统支持在后台新增、停用、切换模型厂商。这里的热插拔是配置级，不是上传 Java jar 动态加载。

支持范围：

- OpenAI\-compatible HTTP 协议。

- 自定义 `baseUrl`、`apiKey`、`modelName`。

- 自定义 Header。

- Provider 启用/停用。

- 平台级、任务级、用户级限流。

- 结构化输出 JSON Schema 校验。

不支持范围：

- 不支持上传 jar。

- 不支持运行时加载任意 Java 类。

- 不支持模型厂商自定义 Java 代码执行。

### 3\.2 调用链路

```mermaid
sequenceDiagram
    participant Job as AiReviewJob
    participant Rate as RedissonLimiter
    participant Gateway as LlmGateway
    participant Adapter as OpenAiCompatibleAdapter
    participant LLM as Model Provider

    Job->>Rate: acquire(provider/task/user)
    Rate-->>Job: allowed / rejected
    Job->>Gateway: review(request)
    Gateway->>Adapter: chat(config, payload)
    Adapter->>LLM: HTTP Chat Completions
    LLM-->>Adapter: structured JSON
    Adapter-->>Gateway: raw response
    Gateway-->>Job: parsed result```

异常策略：

- 限流命中：AI 任务进入 `RATE\_LIMITED`，延迟重试。

- LLM 超时：重试，超过阈值进入 `MANUAL\_REQUIRED`。

- JSON 解析失败：记录原始响应，进入 `MANUAL\_REQUIRED`。

- Provider 停用：不再调度新任务，运行中的任务失败后人工兜底。

核心接口：

```Plaintext
POST /api/v1/admin/llm-providers
PUT  /api/v1/admin/llm-providers/{id}
POST /api/v1/admin/llm-providers/{id}/enable
POST /api/v1/admin/llm-providers/{id}/disable
POST /api/v1/admin/llm-providers/{id}/test
```

## 4\. 任务管理模块

### 4\.1 任务状态机

```mermaid
stateDiagram-v2
    [*] --> DRAFT
    DRAFT --> PUBLISHED: 发布
    PUBLISHED --> PAUSED: 暂停
    PAUSED --> PUBLISHED: 恢复
    PUBLISHED --> ENDED: 结束
    PAUSED --> ENDED: 结束```

状态规则：

- `DRAFT`：可编辑任务信息、数据集、模板、AI 配置。

- `PUBLISHED`：可被 Labeler 领取；模板版本冻结，不允许原地修改。

- `PAUSED`：不可新领取；已领取题目允许继续提交，具体由任务配置控制。

- `ENDED`：不可领取、不可提交，只能查看和导出。

发布前必须校验：

- 存在至少一个数据集 item。

- 存在已保存的 `template\_version`。

- 存在 AI 审核配置，或明确关闭 AI 审核。

- `quota \&gt; 0`。

- `overlap\_count \&gt;= 1`。

- 截止时间晚于当前时间。

核心接口：

```Plaintext
POST /api/v1/tasks
PUT  /api/v1/tasks/{taskId}
POST /api/v1/tasks/{taskId}/publish
POST /api/v1/tasks/{taskId}/pause
POST /api/v1/tasks/{taskId}/resume
POST /api/v1/tasks/{taskId}/end
GET  /api/v1/tasks/{taskId}
GET  /api/v1/owner/tasks
```

### 4\.2 奖励规则

奖励只做平台内虚拟统计，不涉及真实支付、提现、账单和财务结算。

Owner 在任务中配置奖励规则：

- `reward\_mode`：按通过条数计费。

- `unit\_reward`：单条通过奖励。

- `reward\_currency`：虚拟币种或积分名，例如 `POINT`。

- `reward\_visible`：是否对 Labeler 展示。

计奖规则：

- 只有人工终审通过且成为金标的数据计奖励。

- 打回不计奖励。

- Labeler 修改后重新提交，最终通过时只按最终通过版本计一次。

- 同一 assignment 多版本只允许产生一次有效奖励。

- 如果冲突仲裁选择某个提交为金标，只奖励被选为金标的提交者。

- 如果 `overlap\_count \&gt; 1` 且多个版本都被 Reviewer 认可，仍以 `is\_golden = true` 的版本作为奖励依据，避免重复结算。

奖励发放时机：

```Plaintext
Reviewer approve / resolve conflict
  -> 标记 golden submission
  -> 创建 reward_ledger
  -> 更新 labeler_contribution_stats
  -> 写 audit_log
```

异常处理：

- 已发奖励的 submission 被管理员撤销时，追加一条负向流水，不直接删除旧流水。

- 重复回调或重复审核使用 `submission\_id \+ reward\_type` 幂等。

- 任务奖励规则发布后不允许影响历史已通过数据；修改规则只对后续通过生效。

### 4\.3 我的贡献统计

Labeler 工作台需要展示个人贡献，不依赖实时扫全表，采用统计快照 \+ 流水增量更新。

统计指标：

- 已领取数。

- 已提交数。

- 待审核数。

- 通过数。

- 打回数。

- 通过率。

- 累计奖励。

- 今日提交。

- 近 7 日趋势。

- 各任务贡献明细。

统计口径：

- 已领取数：创建 assignment 即计入。

- 已提交数：产生 submission version 即计入；同一 assignment 多次提交可按版本数统计，任务明细中同时展示去重 assignment 数。

- 待审核数：最新 submission 处于 `AI\_REVIEWING` / `PENDING\_FINAL`。

- 通过数：人工终审通过且 `is\_golden = true`。

- 打回数：Reviewer 执行 reject 的次数。

- 通过率：`通过数 / 已终审数`，未终审数据不进入分母。

- 累计奖励：`reward\_ledger` 正负流水汇总。

核心接口：

```Plaintext
GET /api/v1/labeler/contribution/overview
GET /api/v1/labeler/contribution/trend?days=7
GET /api/v1/labeler/contribution/tasks
GET /api/v1/labeler/rewards/ledger
```

### 4\.4 任务数据结构要点

核心表：

- `tasks`：任务主表，保存 owner、状态、配额、截止时间、分发策略、当前模板版本、AI 配置。

- `task\_tags`：任务标签。

- `task\_stats`：任务统计快照，用于看板查询。

- `reward\_rules`：任务奖励规则版本。

- `reward\_ledger`：奖励流水，正向发放和负向冲正都记录。

- `labeler\_contribution\_stats`：Labeler 维度统计快照。

- `labeler\_task\_stats`：Labeler \+ Task 维度贡献明细。

关键索引：

- `tasks\(owner\_id, status\)`

- `tasks\(status, deadline\_at\)`

- `task\_stats\(task\_id\)`

## 5\. 数据集管理模块

### 5\.1 支持的数据类型

当前数据集目录包含两类：

|类型|说明|标注核心|
|---|---|---|
|`QA\_QUALITY`|问答质量标注|对 `model\_answer` 按维度评分|
|`PREFERENCE\_COMPARE`|偏好对比标注|对 `response\_a` / `response\_b` 做偏好判断|

导入格式：

- JSON

- JSONL

- Excel

导入后统一落库为 `dataset\_items\.item\_json`，业务不依赖原始文件格式。

### 5\.2 导入流程

```mermaid
sequenceDiagram
    participant Owner
    participant API
    participant Storage
    participant Job
    participant DB

    Owner->>API: 上传文件
    API->>Storage: 保存原始文件
    API->>Job: 创建导入任务
    Job->>Storage: 读取文件
    Job->>Job: 解析和校验
    Job->>DB: 批量写入 dataset_items
    Job->>DB: 写入导入结果```

校验规则：

- `external\_id` 在同一任务下唯一。

- JSONL 一行一条，单行失败不影响其他行，失败行写入错误报告。

- Excel 表头必须能映射到字段。

- 媒体字段只保存 URL 或对象存储 key，不把文件塞进数据库。

核心接口：

```Plaintext
POST /api/v1/tasks/{taskId}/dataset/import
GET  /api/v1/tasks/{taskId}/dataset/import-jobs/{jobId}
GET  /api/v1/tasks/{taskId}/dataset/items
GET  /api/v1/tasks/{taskId}/dataset/items/{itemId}
```

### 5\.3 批量编辑题目

批量编辑必须受任务状态约束，核心目标是避免发布后篡改已领取或已提交题目的原始数据。

`DRAFT` 状态允许：

- 批量编辑字段。

- 批量删除题目。

- 覆盖导入。

- 批量追加题目。

- 批量修改标签、难度、分类等元数据。

`PUBLISHED` 状态允许：

- 追加新题目。

- 修正未领取题目。

`PUBLISHED` 状态禁止：

- 修改已领取题目的 `item\_json`。

- 修改已提交题目的 `item\_json`。

- 删除已领取或已提交题目。

- 覆盖导入导致历史 assignment 找不到原始数据。

批量编辑流程：

```mermaid
sequenceDiagram
    participant Owner
    participant API
    participant Validator
    participant DB
    participant Audit

    Owner->>API: 提交批量编辑请求
    API->>Validator: 校验任务状态和 item 状态
    Validator-->>API: 返回可编辑集合和拒绝集合
    API->>DB: 事务内更新可编辑 item
    API->>Audit: 记录 before/after
    API-->>Owner: 返回成功数、失败数、失败原因```

实现细节：

- 批量编辑必须返回逐行结果，不允许只返回整体成功/失败。

- 批量删除采用软删除，保留审计。

- 覆盖导入只允许在 `DRAFT`。

- 发布后追加的题目使用当前发布模板版本。

- 对 `item\_json` 的局部字段修改要记录 JSON Patch，便于审计和回滚。

核心接口：

```Plaintext
POST /api/v1/tasks/{taskId}/dataset/items/batch-update
POST /api/v1/tasks/{taskId}/dataset/items/batch-delete
POST /api/v1/tasks/{taskId}/dataset/items/batch-append
POST /api/v1/tasks/{taskId}/dataset/import/overwrite
```

### 5\.4 数据结构要点

核心表：

- `dataset\_items`：题目主数据，包含 `external\_id`、`dataset\_type`、`item\_json`、领取/提交/通过计数。

- `dataset\_import\_jobs`：导入任务状态、成功数、失败数、错误报告地址。

- `dataset\_files`：原始上传文件信息。

- `dataset\_item\_change\_logs`：题目批量编辑审计，记录 JSON Patch、操作者和失败原因。

关键索引：

- `dataset\_items\(task\_id, external\_id\)` 唯一。

- `dataset\_items\(task\_id, assigned\_count\)` 用于领取扫描。

## 6\. 模板 Designer / Renderer 模块

### 6\.1 模板设计目标

模板必须做到 Designer 与 Renderer 解耦：

- Designer 负责编辑 schema。

- Renderer 只消费 schema。

- 同一份 schema 可在 Owner 预览和 Labeler 工作台运行。

- 发布后的模板版本冻结，后续修改必须 fork 新版本。

### 6\.2 Schema 结构

模板 schema 分三层：

```Plaintext
TemplateSchema
  version
  layout        布局树：分组、Tab、顺序、嵌套
  components    组件定义：类型、字段、属性、校验、联动
```

组件类型：

- `ShowItem`：展示题目原始字段，不参与提交。

- `Input` / `Textarea`：文本采集。

- `Radio` / `Checkbox` / `TagSelect`：枚举类标注。

- `RichText`：长文本修订建议。

- `FileUpload` / `ImageUpload`：证据素材。

- `JsonEditor`：结构化答案。

- `LlmTrigger`：字段级 AI 辅助。

- `Group` / `Tabs`：布局容器。

### 6\.3 运行时逻辑

Renderer 输入：

- `schemaJson`

- `itemJson`

- `draftAnswerJson`

- `readonly`

Renderer 输出：

- `answerJson`

- `clientVersion`

关键规则：

- `ShowItem\.dataPath` 从 `itemJson` 读取。

- 有 `field` 的组件写入 `answerJson`。

- 联动规则只影响显示、禁用、校验，不直接改写用户答案。

- 前端提交前校验，后端必须再次校验。

- 自定义函数校验前端只做受控表达式，不执行任意 JS 字符串。

核心接口：

```Plaintext
POST /api/v1/tasks/{taskId}/templates
POST /api/v1/templates/{templateId}/fork
GET  /api/v1/templates/{templateId}/versions
GET  /api/v1/template-versions/{versionId}
```

### 6\.4 数据结构要点

核心表：

- `templates`：模板主表。

- `template\_versions`：模板版本表，保存 `schema\_json`。

关键约束：

- `template\_versions\(template\_id, version\_no\)` 唯一。

- `tasks\.published\_template\_version\_id` 指向发布时冻结版本。

## 7\. 任务领取与标注模块

### 7\.1 领取流程

```mermaid
sequenceDiagram
    participant Labeler
    participant API
    participant Redis
    participant DB

    Labeler->>API: 领取下一题
    API->>DB: 查询可领取 item
    API->>Redis: lock item
    API->>DB: 创建 assignment
    API->>DB: assigned_count + 1
    API->>Redis: unlock item
    API-->>Labeler: 返回 schema + item + draft```

可领取条件：

- 任务状态为 `PUBLISHED`。

- 未超过截止时间。

- item 的 `assigned\_count \&lt; overlap\_count`。

- 当前用户没有领取过该 item。

- 任务剩余配额未耗尽。

并发控制：

- Redis 锁控制抢题瞬间并发。

- MySQL 唯一索引兜底：`dataset\_item\_id \+ labeler\_id`。

- 更新 `assigned\_count` 必须在事务内完成。

### 7\.2 草稿保存

草稿保存策略：

- 前端字段变更后 debounce 1 秒保存。

- 页面空闲时每 10 秒补偿保存。

- 后端写 MySQL，同时写 Redis 热缓存。

- `draft\_version` 做乐观并发控制。

- 断网时前端写 localStorage，恢复后同步。

冲突处理：

- 如果 `clientVersion \&lt; serverVersion`，前端提示存在服务端新版本。

- 用户可选择覆盖本地、覆盖服务端或手动合并。

- 提交必须基于最新服务端版本。

### 7\.3 提交流程

提交步骤：

1. 校验 assignment 属于当前用户。

2. 校验 assignment 状态允许提交。

3. 使用模板 schema 做后端校验。

4. 计算 `answer\_hash`，防止重复提交。

5. 当前旧版本标记为 `SUPERSEDED`。

6. 创建新的 `submission`，`version\_no \+ 1`。

7. assignment 状态变更为 `SUBMITTED`。

8. 写 audit log。

9. 投递 AI 审核任务。

核心接口：

```Plaintext
GET  /api/v1/market/tasks
POST /api/v1/tasks/{taskId}/assignments/claim
PUT  /api/v1/assignments/{assignmentId}/draft
POST /api/v1/assignments/{assignmentId}/submit
GET  /api/v1/labeler/submissions
GET  /api/v1/labeler/assignments/{assignmentId}
```

## 8\. AI 自动预审模块

### 8\.1 审核配置

Owner 在任务中配置：

- LLM Provider。

- 模型名称。

- Prompt 模板。

- 评分维度。

- 通过/打回/人工复核阈值。

- 是否启用 AI 审核。

AI 输出必须是结构化 JSON：

```Plaintext
decision: PASS / REJECT / MANUAL_REVIEW
averageScore
dimensionScores
riskFlags
suggestion
```

AI 的 `PASS` 只表示建议通过，不代表终审通过。

### 8\.2 异步审核流程

```mermaid
stateDiagram-v2
    [*] --> PENDING
    PENDING --> RUNNING
    RUNNING --> SUCCESS
    RUNNING --> FAILED
    RUNNING --> RATE_LIMITED
    FAILED --> RUNNING: retry
    RATE_LIMITED --> RUNNING: delayed retry
    FAILED --> MANUAL_REQUIRED: max retry exceeded
    SUCCESS --> [*]
    MANUAL_REQUIRED --> [*]```

实现细节：

- 使用 `submission\_id` 做幂等键。

- 创建 `agent\_run`，绑定 submission、provider、model、prompt version。

- Prompt 中必须包含题目原始数据、标注答案、评分维度、输出 JSON Schema。

- 原始 Prompt 和原始响应都要入库，保证可追溯。

- AI 审核造成的状态变化使用 `system\_ai\_agent` 写 audit log。

- 失败重试使用指数退避。

- 超过最大重试次数后进入人工兜底，不阻塞审核链路。

Redisson 限流：

- `llm:rate:platform:\{providerCode\}`

- `llm:rate:task:\{taskId\}`

- `llm:rate:user:\{userId\}`

核心接口：

```Plaintext
POST /api/v1/tasks/{taskId}/ai-review-configs
PUT  /api/v1/tasks/{taskId}/ai-review-configs/{configId}
GET  /api/v1/submissions/{submissionId}/ai-review
POST /api/v1/submissions/{submissionId}/ai-review/retry
```

### 8\.3 Agent Run 追溯

`agent\_run` 是一次 AI Agent 执行实例，不等同于 AI 审核结果。

它记录运行过程：

- Agent 类型：`AI\_REVIEW`。

- 关联 submission。

- 使用的 provider 和 model。

- Prompt 模板版本。

- 输入快照。

- 输出快照。

- 运行状态。

- 开始和结束时间。

- 错误信息。

`ai\_review\_results` 记录业务结论，`agent\_runs` 记录执行过程，`audit\_logs` 记录状态影响。

三者关系：

```Plaintext
agent_runs 1 -> 1 ai_review_results
agent_runs 1 -> N audit_logs
submissions 1 -> N agent_runs
```

当 AI 失败重试时：

- 每次重试都生成新的 `agent\_run`。

- `ai\_review\_results\.retry\_count` 递增。

- 最终成功的 run 标记为 `effective\_run\_id`。

这样可以回答四个审计问题：

- 哪个系统主体执行了审核。

- 哪次 Agent 运行执行了审核。

- 运行时用了什么模型和 Prompt。

- 运行输出如何影响后续人工审核。

## 9\. 人工终审与冲突仲裁模块

### 9\.1 终审工作台

Reviewer 需要看到：

- 题目原始数据。

- 当前提交答案。

- 历史版本。

- 与上一版本的 diff。

- AI 评分、建议、原始 Prompt、原始响应。

- Labeler 历史打回记录。

- 如果多人标注，展示同 item 的所有提交版本。

终审动作：

- `APPROVE`：通过，生成或更新金标。

- `REJECT`：打回，必须填写理由。

- `RESOLVE\_CONFLICT`：冲突组选择金标版本。

- `MARK\_MANUAL\_REQUIRED`：标记需要人工重点复核。

- `ASSIGN\_REVIEWER`：分配 Reviewer。

### 9\.2 审核员批量操作

批量操作只面向普通待审 submission，不允许绕过冲突仲裁。

支持动作：

- 批量通过。

- 批量打回。

- 批量标记人工复核。

- 批量分配 Reviewer。

批量通过限制：

- submission 必须处于 `PENDING\_FINAL`。

- `conflict\_status` 必须为 `NONE` 或 `CONSENSUS\_REACHED`。

- 冲突组不允许批量通过。

- 每条 submission 仍需单独写 review record 和 audit log。

批量打回限制：

- 必须填写统一打回理由。

- 前端允许 Reviewer 对单条追加个性化备注，但统一理由必填。

- 打回后 assignment 进入 `RETURNED`。

批量分配限制：

- 只能分配给 `Reviewer` 角色用户。

- 已 `APPROVED` / `REJECTED` 的 submission 不允许重新分配，除非 Admin 执行特殊重开流程。

批量操作流程：

```mermaid
sequenceDiagram
    participant Reviewer
    participant API
    participant DB
    participant Audit

    Reviewer->>API: 批量操作 submission ids
    API->>DB: 加载并校验状态
    API->>DB: 逐条执行状态迁移
    API->>Audit: 逐条写 review_record 和 audit_log
    API-->>Reviewer: 返回成功列表和失败列表```

核心原则：

- 批量操作不是“大事务全成功”；采用逐条处理，返回部分成功和失败原因。

- 对同一 submission 使用乐观锁，避免两个 Reviewer 同时审核。

- 批量通过后触发奖励结算。

- 批量打回不计奖励。

核心接口：

```Plaintext
POST /api/v1/reviewer/submissions/batch-approve
POST /api/v1/reviewer/submissions/batch-reject
POST /api/v1/reviewer/submissions/batch-mark-manual
POST /api/v1/reviewer/submissions/batch-assign
```

### 9\.3 打回修改

打回后：

- submission 状态变为 `REJECTED`。

- assignment 状态变为 `RETURNED`。

- Labeler 可查看打回理由。

- Labeler 修改后重新提交，产生新的 `submission\_version`。

- 新版本重新进入 AI 预审。

禁止：

- 禁止直接覆盖旧 submission。

- 禁止 Reviewer 无理由打回。

- 禁止已通过金标被普通 Labeler 修改。

### 9\.4 多人标注与一致性

当 `overlap\_count \&gt; 1` 时，同一 item 可以被多个 Labeler 独立领取和提交。

一致性计算：

- 单选字段：完全相等为 `1`，否则 `0`。

- 多选字段：Jaccard 相似度。

- 数字评分：按分值距离换算相似度。

- 文本字段：默认不参与自动一致性，只展示给 Reviewer。

- 总分：按字段权重加权平均。

判定：

- `consensusScore \&gt;= 0\.8`：认为达成一致。

- `\&lt; 0\.8`：进入冲突组。

冲突解决：

- Reviewer 横向对比多个提交。

- 选择一个 `submission` 作为 `goldenSubmission`。

- 写入解决理由和 audit log。

核心接口：

```Plaintext
GET  /api/v1/reviewer/submissions
GET  /api/v1/reviewer/submissions/{submissionId}
POST /api/v1/reviewer/submissions/{submissionId}/approve
POST /api/v1/reviewer/submissions/{submissionId}/reject
GET  /api/v1/reviewer/conflict-groups
GET  /api/v1/reviewer/conflict-groups/{groupId}
POST /api/v1/reviewer/conflict-groups/{groupId}/resolve
```

## 10\. 数据导出模块

### 10\.1 导出范围

默认导出：

```Plaintext
status = APPROVED
is_golden = true
```

可选附加：

- `aiReview`

- `auditTrail`

- `reviewComment`

- `labelerInfo`

支持格式：

- JSON

- JSONL

- CSV

- Excel

### 10\.2 导出流程

```mermaid
sequenceDiagram
    participant Owner
    participant API
    participant Job
    participant DB
    participant Storage

    Owner->>API: 创建导出任务
    API->>Job: 投递异步任务
    Job->>DB: 查询 APPROVED 金标数据
    Job->>Job: 字段映射和格式转换
    Job->>Storage: 上传导出文件
    Job->>DB: 更新下载地址和状态
    Owner->>API: 查询导出历史```

字段映射：

- `source` 使用 JSONPath 表达式。

- `target` 是导出字段名。

- 数组字段 CSV/Excel 中用 `;` 拼接。

- 无法展平的对象字段输出 JSON 字符串。

异常处理：

- 导出失败保留错误信息。

- 大文件分页读取，禁止一次性加载全部数据到内存。

- 下载链接使用对象存储预签名 URL。

核心接口：

```Plaintext
POST /api/v1/tasks/{taskId}/exports
GET  /api/v1/tasks/{taskId}/exports
GET  /api/v1/tasks/{taskId}/exports/{exportJobId}
```

## 11\. 核心数据模型概览

只列开发必须关注的核心表，不展开 DDL：

|表|作用|关键字段|
|---|---|---|
|`users`|用户|username、email、password\_hash、enabled|
|`user\_roles`|用户角色|user\_id、role\_code|
|`llm\_providers`|模型厂商|provider\_code、base\_url、encrypted\_api\_key、default\_model、rate\_limit|
|`agent\_runs`|AI Agent 运行实例|agent\_type、submission\_id、provider\_id、model\_name、status、input\_snapshot、output\_snapshot|
|`tasks`|任务|owner\_id、status、quota、overlap\_count、published\_template\_version\_id|
|`reward\_rules`|奖励规则|task\_id、unit\_reward、reward\_currency、effective\_version|
|`reward\_ledger`|奖励流水|task\_id、labeler\_id、submission\_id、amount、direction、reason|
|`labeler\_contribution\_stats`|贡献统计|labeler\_id、claimed\_count、submitted\_count、approved\_count、rejected\_count、total\_reward|
|`dataset\_items`|题目数据|task\_id、external\_id、dataset\_type、item\_json、assigned\_count|
|`dataset\_item\_change\_logs`|题目编辑审计|task\_id、item\_id、change\_type、json\_patch、actor\_id|
|`templates`|模板主表|task\_id、name|
|`template\_versions`|模板版本|template\_id、version\_no、schema\_json|
|`assignments`|领取记录|task\_id、dataset\_item\_id、labeler\_id、status、draft\_answer\_json|
|`submissions`|提交版本|assignment\_id、version\_no、answer\_json、status、is\_golden|
|`ai\_review\_results`|AI 预审结果|submission\_id、decision、dimension\_scores、prompt\_snapshot、raw\_response|
|`review\_records`|人工审核记录|submission\_id、reviewer\_id、action、comment|
|`conflict\_groups`|冲突组|task\_id、dataset\_item\_id、consensus\_score、golden\_submission\_id|
|`audit\_logs`|审计日志|biz\_type、biz\_id、actor\_id、action、before\_json、after\_json|
|`export\_jobs`|导出任务|task\_id、format、status、download\_url|

关键约束：

- 同一任务下 `external\_id` 唯一。

- 同一 item 同一 labeler 只能领取一次。

- 同一 assignment 的 `version\_no` 单调递增。

- 一个 item 最终只能有一个金标 submission。

- 发布任务绑定的模板版本不可修改。

- 同一 submission 只能产生一次正向奖励。

- `system\_ai\_agent` 不允许登录，只允许作为系统审计主体。

## 12\. 前端模块设计

### 12\.1 目录建议

```Plaintext
src/
  api/
  pages/
    admin/
    owner/
    labeler/
    reviewer/
  features/
    designer/
    renderer/
    assignment/
    review/
    export/
  stores/
```

### 12\.2 状态管理

建议：

- `authStore`：用户、角色、token。

- `designerStore`：当前 schema、选中组件、撤销/重做栈。

- `assignmentStore`：当前题目、草稿版本、本地离线草稿。

- `reviewStore`：审核筛选条件、当前提交详情。

- 服务端数据优先用 TanStack Query 管理缓存和重试。

### 12\.3 前端调用时序

Labeler 作答：

```Plaintext
任务广场 -> 领取题目 -> Renderer 渲染 -> 自动保存草稿 -> 提交 -> 查看审核状态
```

Reviewer 审核：

```Plaintext
审核列表 -> 提交详情 -> 查看 AI 预审与 diff -> 通过/打回/批量操作 -> 冲突时选择金标
```

Owner 发布任务：

```Plaintext
创建任务 -> 配置奖励 -> 导入/批量编辑数据 -> 搭建模板 -> 配置 AI 审核 -> 发布 -> 查看统计 -> 导出
```

### 12\.4 极端场景

断网：

- 草稿写 localStorage。

- 网络恢复后比较 `clientVersion` 和 `serverVersion`。

- 提交失败必须保留本地答案。

超时：

- 草稿保存失败只提示，不阻断继续作答。

- 提交超时后查询 assignment 最新状态，避免重复提交。

- AI 审核超时不暴露给 Labeler，只展示“等待审核”。

冲突：

- 如果服务端返回版本冲突，前端提供“使用本地 / 使用服务端 / 手动合并”。

- Reviewer 页面必须显示不同提交版本的字段级差异。

## 13\. 测试重点

后端：

- 任务状态机非法迁移。

- 发布后模板不可原地修改。

- 并发领取不超过 `overlap\_count`。

- 同一用户不能重复领取同一 item。

- 草稿版本冲突。

- submission version 递增。

- AI 审核幂等。

- Redisson 限流命中。

- AI 失败后人工兜底。

- Reviewer 打回后重新提交。

- 冲突一致性计算。

- 仅 `APPROVED \+ is\_golden=true` 可导出。

前端：

- Designer 组件拖拽、属性编辑、撤销重做。

- Renderer 渲染全部物料。

- 条件显示和联动校验。

- 草稿自动保存和断网恢复。

- 提交校验错误定位。

- Reviewer diff 和冲突选择。

- Reviewer 批量操作部分成功回显。

- 导出字段映射配置。

- 我的贡献统计和奖励流水。

## 14\. MVP 开发顺序

1. Auth / RBAC / Admin 用户管理。

2. System Agent 初始化与审计上下文。

3. LLM Provider 配置与 Redisson 限流骨架。

4. Owner 任务管理与奖励规则。

5. Dataset 导入、预览与批量编辑。

6. Template Designer / Renderer 核心 schema。

7. Labeler 领取、草稿、提交。

8. AI Review Config \+ LlmGateway \+ Agent Run。

9. AI 异步审核队列。

10. Reviewer 终审与批量操作。

11. ConflictGroup 与金标选择。

12. 奖励结算与贡献统计。

13. Export Job。

开发时必须优先跑通端到端闭环：建任务、导入数据、搭模板、发布、领取、提交、AI 预审、人工终审、导出。Designer 高级能力和看板统计可以后置。

