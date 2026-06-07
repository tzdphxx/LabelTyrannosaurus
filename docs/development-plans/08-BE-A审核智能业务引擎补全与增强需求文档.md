# 08-BE-A 审核智能业务引擎补全与增强需求文档

> 面向 BE-A 成员与 AI 开发工具：本文档依据《LabelHub 数据标注平台 · AI全栈课题实现要求》、现有 BE-A 任务书、后端功能缺口清单，以及 `07-BE-A-Agent预标注与多模态标注增强方案.md` 合并生成。本文只描述 BE-A 边界内可以补全和增强的功能，不覆盖原有文档，不接管 BE-B 或 FE 职责。

## 0. 文档目标

原课题要求 LabelHub 覆盖完整数据生产生命周期：

```text
数据生产 -> AI 预审 -> 人工审核 -> 多格式导出
```

其中 AI 审核 Agent 是课题核心难点之一。课题原始流程中包含如下分支：

```text
[Labeler 提交] -> [AI 预审]
   ├─ 通过 -> [人工复审]
   │           ├─ 通过 -> [入库 / 可导出]
   │           └─ 打回 -> [Labeler 修改]
   └─ 打回 -> [Labeler 修改] / [人工复核]
```

结合当前后端已有能力，BE-A 需要从“核心 MVP 可跑通”补全到“完整课题演示可支撑”。本文档补充：

- BE-A 仍需补齐的查询、详情、历史、重试和可观测接口。
- AI 自动预审的分流策略，包括 AI 直接通过、AI 直接打回、AI 分配人工审核。
- AgentRun、审计、人工追溯要求。
- Agent 预标注与多模态上下文作为 P2 增强项。
- 与 BE-B / FE 的协作边界。

## 1. BE-A 边界

### 1.1 BE-A 负责

BE-A 负责审核智能业务引擎，包含：

- Assignment / Submission 业务状态。
- 领取、草稿、提交版本。
- AI 自动预审。
- AI 审核结论分流。
- AI 直接通过、AI 直接打回、AI 分配人工审核的状态推进。
- AgentRun 追踪。
- System Agent 审计身份。
- 人工审核流转。
- 打回修改闭环。
- 批量审核。
- 冲突仲裁。
- 金标选择。
- BE-A 侧导出快照查询。
- AI 预标注。
- 多模态 Prompt 上下文进入 AI 链路。

### 1.2 BE-A 不负责

以下能力不纳入 BE-A 主责：

- 数据集导入。
- 数据集原始文件维护。
- 模板 Designer。
- 模板版本存储。
- 通用 Schema 校验引擎维护。
- 对象存储。
- 图片 / 视频文件上传。
- 视频抽帧和转写。
- 奖励结算。
- 多格式导出文件生成。
- 前端页面实现。

BE-A 可以消费 BE-B 提供的能力，例如 `validateAnswer(...)`、对象存储 URL、数据集 item 快照、AuditAppender、异步线程池、RateLimit、Lock 等，但不直接维护这些基础能力。

## 2. 当前 BE-A 完成度判断

当前 BE-A 已基本具备 MVP 主链路：

```text
任务广场 -> 领取 -> 草稿 -> 提交 -> AI 预审 -> PENDING_FINAL -> Reviewer 通过/打回 -> 金标/返回修改
```

已有能力包括：

- 任务领取。
- 草稿保存。
- 提交版本。
- Submission hash 幂等。
- Schema 校验调用。
- AI Review 异步调度。
- LLM Provider / Gateway。
- AgentRun。
- AI Review Result。
- 失败重试与人工兜底。
- 人工终审 approve / reject。
- 批量审核。
- 冲突仲裁。
- 金标快照查询。

但完整课题演示仍缺少：

- Assignment 详情查询。
- Labeler 提交历史与详情。
- Reviewer 提交详情聚合。
- 审核列表筛选与分页。
- AI 审核结果契约统一。
- AI 手动重试入口。
- 首次 AI 审核任务恢复。
- AgentRun 详情查询。
- 版本历史与 diff。
- AI 审核自动分流策略。

## 3. P0 补全需求：完整链路必需

### 3.1 Assignment 详情查询

接口：

```text
GET /api/v1/assignments/{assignmentId}
```

权限：

- Labeler 只能查看自己的 assignment。
- Owner / Reviewer 不通过该接口查看审核详情，避免职责混乱。

返回内容：

```text
assignmentId
taskId
datasetItemId
templateVersionId
assignmentStatus
schemaJson
itemJson
draftAnswerJson
draftVersion
latestSubmissionId
latestSubmissionStatus
returnedReason
returnedAt
createdAt
updatedAt
```

用途：

- Labeler 刷新页面后恢复标注上下文。
- Labeler 查看打回数据并继续修改。
- 前端上一题 / 下一题 / 跳题时恢复草稿。

验收：

```text
本人可查。
非本人不可查。
返回 itemJson、schemaJson、draftAnswerJson。
打回 assignment 能返回最近一次打回理由。
```

### 3.2 Labeler 提交历史

接口：

```text
GET /api/v1/labeler/submissions
```

查询参数：

```text
taskId 可选
submissionStatus 可选
assignmentStatus 可选
page / size
```

返回字段：

```text
submissionId
assignmentId
taskId
datasetItemId
versionNo
submissionStatus
assignmentStatus
aiReviewStatus
aiDecision
reviewSummary
rejectReason
isGolden
createdAt
updatedAt
```

用途：

- 我的数据列表。
- 查看已提交、待审核、通过、打回、待修改。
- 从打回数据进入修改。

验收：

```text
只返回当前 Labeler 的提交。
支持状态筛选。
打回记录包含 reason。
重新提交后能看到 versionNo 递增。
```

### 3.3 Labeler 提交详情

接口：

```text
GET /api/v1/labeler/submissions/{submissionId}
```

返回内容：

```text
submission 基础信息
assignment 基础信息
itemJson
schemaJson
answerJson
aiReviewResult 摘要
reviewRecords 摘要
rejectReason
versionHistory 摘要
可否继续修改
```

权限：

- 只能查看当前用户自己的 submission。

用途：

- Labeler 查看自己的提交详情。
- 查看 AI 预审结果。
- 查看人工审核意见。
- 打回后重新修改。

### 3.4 Reviewer 提交详情

接口：

```text
GET /api/v1/reviewer/submissions/{submissionId}
```

权限：

- REVIEWER。

返回内容：

```text
submissionId
taskId
assignmentId
datasetItemId
labelerId
versionNo
submissionStatus
answerJson
itemJson
templateVersionId
schemaJson
aiReviewResult
agentRunSummary
reviewRecords
auditTimelineRef
conflictGroupSummary
versionHistory
fieldDiff
```

用途：

- Reviewer 基于完整上下文审核。
- 查看 AI 评分、AI 评语、原始 Prompt、原始响应引用。
- 查看历史版本和 diff。
- 查看冲突组候选。

验收：

```text
Reviewer 可查。
Labeler 不可查他人 submission。
返回 AI 结果、历史版本、review records。
打回必须能看到历史原因。
```

### 3.5 Reviewer 审核列表筛选和分页增强

现有接口：

```text
GET /api/v1/reviewer/submissions
```

增强查询参数：

```text
taskId
submissionStatus
aiDecision
aiReviewStatus
conflictStatus
reviewLevel
assignedReviewerId
page
size
```

默认行为：

- 默认查询待审核范围。
- 默认排除已完成终审数据。

返回字段保持轻量：

```text
submissionId
taskId
datasetItemId
labelerId
submissionStatus
aiReviewStatus
aiDecision
conflictStatus
reviewLevel
assignedReviewerId
createdAt
updatedAt
```

### 3.6 AI 审核结果契约统一

建议统一接口：

```text
GET /api/v1/submissions/{submissionId}/ai-review
```

兼容当前已有：

```text
GET /api/v1/submissions/{submissionId}/ai-review-result
```

返回字段：

```text
aiReviewResultId
submissionId
agentRunId
providerId
modelName
status
decision
averageScore
dimensionScores
riskFlags
suggestion
confidence
flowAction
errorCode
errorMessage
createdAt
updatedAt
```

### 3.7 AI 手动重试

接口：

```text
POST /api/v1/submissions/{submissionId}/ai-review/retry
```

权限：

- REVIEWER。
- 任务 Owner 可选支持。

允许重试状态：

```text
FAILED
RATE_LIMITED
MANUAL_REQUIRED
AI_REVIEWING 超时且无有效结果
```

不允许重试状态：

```text
APPROVED
REJECTED
已存在 SUCCESS 且未显式 force
```

规则：

- 每次重试生成新的 AgentRun。
- 更新 effectiveRunId。
- 保留历史 AgentRun。
- 写审计。
- 重试失败仍不能阻断人工审核。

### 3.8 首次 AI 审核任务恢复

当前 AI 审核使用本地线程池异步执行，服务重启可能导致已提交但未执行的内存任务丢失。

BE-A 需要补充恢复机制：

```text
应用启动后扫描：
submission.status = AI_REVIEWING
且不存在 SUCCESS / MANUAL_REQUIRED / FAILED / RATE_LIMITED 的有效 aiReviewResult
```

处理策略：

- 重新入队 AI 审核；或
- 标记为 `PENDING_FINAL + aiReview.status=MANUAL_REQUIRED`，保证人工可兜底。

推荐默认：重新入队一次，若仍失败则人工兜底。

验收：

```text
服务重启后，AI_REVIEWING 不会永久卡住。
重复恢复不会生成重复有效 aiReviewResult。
恢复过程可审计。
```

## 4. AI 自动预审分流需求

### 4.1 原课题流程解释

原课题要求 AI 预审后存在“通过 / 打回 / 人工复核”分支。为兼顾自动化能力与数据质量，BE-A 将 AI 审核分流定义为以下三种动作：

```text
AI_DIRECT_APPROVE
AI_DIRECT_REJECT
AI_ASSIGN_MANUAL_REVIEW
```

这三个动作是 AI 预审后的业务流转动作，不等同于无条件终审。

### 4.2 三种 AI 分流动作

#### 4.2.1 AI 直接通过

动作：

```text
AI_DIRECT_APPROVE
```

含义：

- AI 判断当前提交满足任务审核标准。
- submission 可直接进入 `APPROVED`。
- assignment 可进入 `APPROVED`。
- submission 可标记 `isGolden=true`。
- 发布 `SubmissionApproved` 事件。

适用场景：

- 低风险任务。
- 标准答案明确。
- 结构化答案容易自动校验。
- AI 置信度达到任务配置阈值。
- Owner 在任务级明确启用 AI 自动通过。

限制：

```text
默认不启用。
必须任务级配置开启。
必须满足 passThreshold 和 confidenceThreshold。
必须保留 AgentRun、aiReviewResult、auditLog。
Reviewer 后续可以追溯 AI 自动通过记录。
冲突组数据不允许 AI 直接通过为最终金标，除非 overlapCount=1 或已达到一致性规则。
```

#### 4.2.2 AI 直接打回

动作：

```text
AI_DIRECT_REJECT
```

含义：

- AI 判断当前提交明显不符合要求。
- submission 进入 `REJECTED`。
- assignment 进入 `RETURNED`。
- Labeler 可看到 AI 打回理由并重新修改。

适用场景：

- 明显空答案。
- 必填字段缺失。
- 枚举值明显不合规。
- 安全违规明显。
- 与参考答案严重不符。
- 任务级启用 AI 自动打回。

限制：

```text
默认不启用。
必须任务级配置开启。
必须满足 rejectThreshold 和 confidenceThreshold。
必须生成可读打回理由。
必须写 review-like record 或 AI reject record，供 Reviewer 追溯。
不能删除 submission。
重新提交必须生成新版本。
```

#### 4.2.3 AI 分配人工审核

动作：

```text
AI_ASSIGN_MANUAL_REVIEW
```

含义：

- AI 无法可靠自动通过或自动打回。
- submission 进入 `PENDING_FINAL`。
- 可根据 AI 风险标签、任务、审核级别、Reviewer 负载等分配人工审核。

适用场景：

- AI 置信度不足。
- 风险标签存在争议。
- 多人标注冲突。
- 模型输出非法或不稳定。
- 任务配置为所有数据人工复审。
- AI 判断为 `MANUAL_REVIEW`。

默认策略：

```text
AI_ASSIGN_MANUAL_REVIEW 是默认兜底动作。
任何 AI 调用失败、限流、超时、输出非法，都应进入人工审核路径。
```

### 4.3 AI 输出结构

AI 结构化输出必须包含：

```json
{
  "decision": "PASS",
  "averageScore": 4.6,
  "dimensionScores": {
    "相关性": 5,
    "准确性": 4,
    "格式合规": 5
  },
  "riskFlags": [],
  "suggestion": "回答整体符合要求，可通过。",
  "confidence": 0.92,
  "reason": "回答覆盖参考答案关键点，格式符合任务要求。"
}
```

`decision` 允许值：

```text
PASS
REJECT
MANUAL_REVIEW
```

BE-A 根据 `decision + 任务分流配置 + 阈值 + 风险规则` 映射为 `flowAction`：

```text
AI_DIRECT_APPROVE
AI_DIRECT_REJECT
AI_ASSIGN_MANUAL_REVIEW
```

### 4.4 任务级 AI 分流配置

建议在 AI 审核配置中增加：

```json
{
  "aiFlowPolicy": "MANUAL_FIRST",
  "allowAiDirectApprove": false,
  "allowAiDirectReject": false,
  "passThreshold": 4.5,
  "rejectThreshold": 2.0,
  "confidenceThreshold": 0.85,
  "manualReviewThreshold": 0.6,
  "riskFlagsForceManual": ["安全风险", "冲突", "低置信度"]
}
```

策略枚举：

| 策略 | 说明 |
|---|---|
| `MANUAL_FIRST` | AI 只建议，全部进入人工审核，最安全 |
| `AI_PASS_ONLY` | AI 可直接通过高置信样本，打回仍人工复核 |
| `AI_REJECT_ONLY` | AI 可直接打回明显错误样本，通过仍人工复核 |
| `AI_PASS_AND_REJECT` | AI 可直接通过或打回，高风险进入人工 |
| `ALWAYS_MANUAL` | 所有 AI 结果只作为建议 |

默认：

```text
MANUAL_FIRST 或 ALWAYS_MANUAL
```

课题演示可配置为：

```text
AI_PASS_AND_REJECT
```

用于展示自动化分流能力。

### 4.5 状态流转

#### AI 直接通过

```text
submission.status: AI_REVIEWING -> APPROVED
assignment.status: SUBMITTED -> APPROVED
aiReview.status: SUCCESS
flowAction: AI_DIRECT_APPROVE
isGolden: true
emit SubmissionApproved
```

#### AI 直接打回

```text
submission.status: AI_REVIEWING -> REJECTED
assignment.status: SUBMITTED -> RETURNED
aiReview.status: SUCCESS
flowAction: AI_DIRECT_REJECT
rejectReason: AI suggestion / reason
```

#### AI 分配人工审核

```text
submission.status: AI_REVIEWING -> PENDING_FINAL
assignment.status: SUBMITTED
aiReview.status: SUCCESS / MANUAL_REQUIRED
flowAction: AI_ASSIGN_MANUAL_REVIEW
assignedReviewerId: optional
```

### 4.6 审计要求

AI 自动分流必须写审计：

```text
AI_REVIEW_COMPLETED
AI_DIRECT_APPROVED
AI_DIRECT_REJECTED
AI_ASSIGNED_MANUAL_REVIEW
AI_REVIEW_MANUAL_REQUIRED
```

审计内容：

```json
{
  "submissionId": 1001,
  "agentRunId": 9001,
  "decision": "PASS",
  "flowAction": "AI_DIRECT_APPROVE",
  "confidence": 0.92,
  "averageScore": 4.6,
  "policy": "AI_PASS_AND_REJECT"
}
```

## 5. P1 补全需求：审核可追踪

### 5.1 Submission 版本历史

能力：

```text
查询同一 assignment 下所有 submission version。
```

返回：

```text
submissionId
versionNo
status
answerHash
isGolden
createdAt
reviewSummary
aiReviewSummary
```

规则：

- 打回后重新提交必须生成新版本。
- 旧版本不能被覆盖。
- 当前有效版本和历史版本要能区分。

### 5.2 字段级 answerJson diff

Reviewer 详情中返回：

```json
{
  "changedFields": [
    {
      "field": "accuracy_score",
      "before": "2",
      "after": "4",
      "changeType": "MODIFIED"
    }
  ]
}
```

用途：

- Reviewer 快速看出 Labeler 修改了什么。
- 打回后复审更高效。

### 5.3 Review record 查询

返回：

```text
reviewRecordId
submissionId
reviewerId
action
reviewLevel
reason
reviewComment
createdAt
```

### 5.4 打回原因查询

Labeler 需要能看到：

- 最近一次打回原因。
- 打回人。
- 打回时间。
- 关联 submission version。
- AI 直接打回或人工打回的来源区分。

来源类型：

```text
HUMAN_REVIEW_REJECT
AI_DIRECT_REJECT
```

### 5.5 批量审核逐条结果

批量操作返回：

```text
successCount
failureCount
items[]
  submissionId
  success
  status
  failureCode
  failureReason
```

规则：

- 部分失败不影响其他条目。
- 冲突组不可批量通过。
- 已终审数据不可重复审核。

## 6. 冲突仲裁与金标快照补全

### 6.1 冲突组详情增强

接口：

```text
GET /api/v1/reviewer/conflict-groups/{groupId}
```

返回：

```text
groupId
taskId
datasetItemId
consensusScore
status
goldenSubmissionId
candidateSubmissions[]
  submissionId
  labelerId
  answerJson
  aiReviewSummary
  reviewRecords
  versionNo
```

### 6.2 仲裁规则

- 只能选择当前 conflictGroup 内的 submission。
- 同一 item 最终只能一个 `isGolden=true`。
- 仲裁完成后发布 `GoldenSelected`。
- 写 review record 和 audit log。

### 6.3 金标快照查询增强

BE-A 侧只负责业务快照：

```text
GET /api/v1/owner/export/golden-submissions
```

返回范围：

```text
submission.status = APPROVED
isGolden = true
```

返回内容：

```text
submissionId
taskId
datasetItemId
itemJsonRef
answerJson
aiReviewSummary
reviewSummary
auditRef
```

不负责：

- JSON/JSONL/CSV/Excel 文件生成。
- 下载 URL。
- 导出任务状态。

这些属于 BE-B。

## 7. AgentRun 可观测需求

### 7.1 AgentRun 详情

接口：

```text
GET /api/v1/agent-runs/{agentRunId}
```

返回：

```text
agentRunId
agentType
submissionId
providerId
modelName
promptVersion
status
inputSnapshot
outputSnapshot
errorMessage
startedAt
finishedAt
```

### 7.2 权限

- Owner 可查看自己任务下的 AgentRun。
- Reviewer 可查看审核范围内的 AgentRun。
- Labeler 只能查看自己 assignment 相关的脱敏摘要。

### 7.3 脱敏

不得返回：

- API Key。
- 私密 Header。
- Provider 密钥。

Prompt 和 rawResponse 可以返回，但需要保留脱敏扩展点。

## 8. P2 增强：Agent 预标注

### 8.1 接口

```text
POST /api/v1/assignments/{assignmentId}/pre-annotations/run
GET  /api/v1/assignments/{assignmentId}/pre-annotations/latest
GET  /api/v1/pre-annotations/{preAnnotationId}
```

### 8.2 规则

- 预标注只生成 `suggestedAnswerJson`。
- Labeler 必须确认后才写入草稿。
- 预标注不创建 submission。
- 预标注不触发终审。
- 每次预标注生成 AgentRun。
- Reviewer 可查看预标注记录与最终提交差异。

### 8.3 输出

```json
{
  "suggestedAnswerJson": {},
  "fieldSuggestions": [],
  "riskFlags": [],
  "overallConfidence": 0.86,
  "limitations": []
}
```

## 9. P2 增强：多模态 AI 上下文

### 9.1 非多模态模式

如果不做多模态，图片和视频只会以 URL 字符串进入 Prompt。AI 不一定真正打开链接，也不能可靠判断图片或视频内容。

适合：

- 文本题。
- 已有媒体文字描述的题。
- 只演示 AI 预标注流程。

不适合：

- OCR。
- 图像描述。
- 图片安全审核。
- 视频画面质检。

### 9.2 多模态模式

图片作为 `image_url` 输入：

```json
{
  "type": "image_url",
  "image_url": {
    "url": "https://example.com/image.jpg"
  }
}
```

视频采用关键帧、转写、说明：

```json
{
  "media_type": "video",
  "media_url": "https://example.com/video.mp4",
  "key_frame_urls": [],
  "video_transcript": "...",
  "owner_media_description": "..."
}
```

### 9.3 BE-A 负责

- 根据 `media_type` 构造 Prompt。
- 记录 `promptMode`。
- 记录是否发生降级。
- 在 AgentRun 中保存媒体输入摘要。
- 在 AI 输出中保留 `limitations`。

### 9.4 协作备注

FE / BE-B 配套要求单独列为备注，不纳入 BE-A 主责：

- FE 展示图片、视频、Markdown。
- FE 展示 AI 建议和 limitations。
- BE-B 保留媒体字段。
- BE-B 提供对象存储 URL。
- BE-B 或媒体模块生成关键帧和转写。

### 9.5 多模态必要性与可行性评估

#### 必要性分析

经评估现有数据集样本（`datasets/qa_quality/json/qa_quality.json`），平台已包含以下多模态数据：

| media_type | 数量 | 典型 Prompt | AI 无视觉能力时的表现 |
|---|---|---|---|
| `text` | 20 条 | 知识问答 | 正常工作 |
| `image` | 4 条 | "请描述这张图片的主要内容" | **完全无法完成任务**，只能看到 URL 字符串 |
| `video` | 3 条 | "请观看视频，判断是否包含违规或不适画面" | **完全无法完成任务** |
| `markdown` | 3 条 | "请审核以下图文混排内容的排版质量与合规性" | 只能审核文字部分，**图片内容无法判断** |

结论：

- 当前 `AiAutoReviewService.buildPromptSnapshot()` 将 `itemJson` 整体序列化为文本传入 LLM，`media_url` 仅作为字符串出现在 Prompt 中，LLM 不会真正访问链接。
- `LlmMessage` 仅支持 `String content`，不支持 OpenAI 风格的 `content[]` 多模态消息体。
- 对于 image/video/markdown 类型的数据项，AI 预审和 AI 预标注在当前实现下**无法产生有意义的审核结论**。
- 课题要求"AI 自动预审 Agent"为核心难点之一，若演示时 AI 对图片/视频题目只能输出"无法判断"，将严重削弱课题完成度。
- 数据集中 image + video + markdown 占比 10/30 = 33%，不是边缘场景。

**必要性结论：多模态支持是课题演示的刚需，不是锦上添花。**

#### 可行性分析

| 维度 | 评估 |
|---|---|
| Provider 支持 | 通义千问 qwen-vl-plus、OpenAI gpt-4o、豆包 doubao-vision 均支持 image_url 输入 |
| 改动范围 | 仅涉及 BE-A 的 AI 模块（LlmMessage、PromptBuilder、AiAutoReviewService），不越界 |
| 降级策略 | Provider 不支持视觉时自动降级为纯文本 + limitations 标记，不阻断流程 |
| 视频处理 | 不要求实时视频理解，使用关键帧 + 转写 + 描述即可（BE-B 提供） |
| 工作量 | 核心改动约 4 个类，预计 1-2 天可完成 MVP |
| 风险 | 低。降级机制保证非视觉 Provider 不受影响，现有文本审核逻辑零改动 |

**可行性结论：技术可行，改动隔离，风险可控。**

#### 实施要点

1. **扩展 `LlmMessage`**：content 字段从 `String` 改为支持 `List<ContentPart>`（文本块 + image_url 块）。
2. **新增 `MediaPromptContextBuilder`**：根据 `media_type` 构造多模态消息体。
3. **Provider 能力标记**：`LlmProvider` 增加 `supportVision` 字段。
4. **降级逻辑**：Provider 不支持视觉时，将 image_url 降级为文本描述 + limitations。
5. **AgentRun 记录**：保存 `promptMode`（TEXT / IMAGE / VIDEO / MARKDOWN）和是否降级。
6. **AI 预标注复用**：`MediaPromptContextBuilder` 同时服务于预审和预标注。

#### 优先级调整建议

鉴于数据集已包含多模态数据且 AI 无视觉能力时无法产生有效审核结论，建议将多模态上下文从 P2 提升至 **P1**，与 AI 直接通过/打回同优先级。最小可行范围：

- P1 MVP：image 类型支持（image_url 输入）+ 降级机制 + promptMode 记录。
- P2 增强：video 关键帧 + markdown 图片提取 + 多图输入。

## 10. 接口清单

### 10.1 P0/P1 接口

```text
GET  /api/v1/assignments/{assignmentId}
GET  /api/v1/labeler/submissions
GET  /api/v1/labeler/submissions/{submissionId}

GET  /api/v1/reviewer/submissions
GET  /api/v1/reviewer/submissions/{submissionId}
POST /api/v1/reviewer/submissions/{submissionId}/approve
POST /api/v1/reviewer/submissions/{submissionId}/reject

GET  /api/v1/submissions/{submissionId}/ai-review
POST /api/v1/submissions/{submissionId}/ai-review/retry

GET  /api/v1/agent-runs/{agentRunId}

GET  /api/v1/reviewer/conflict-groups
GET  /api/v1/reviewer/conflict-groups/{groupId}
POST /api/v1/reviewer/conflict-groups/{groupId}/resolve

GET  /api/v1/owner/export/golden-submissions
```

### 10.2 P2 增强接口

```text
POST /api/v1/assignments/{assignmentId}/pre-annotations/run
GET  /api/v1/assignments/{assignmentId}/pre-annotations/latest
GET  /api/v1/pre-annotations/{preAnnotationId}
```

## 11. 测试与验收

### 11.1 完整链路

```text
Labeler 可完成领取、作答、提交、查看打回、修改、重新提交。
Reviewer 可查看完整提交详情。
Reviewer 可通过、打回、批量操作。
Owner 可查询 BE-A 金标快照。
```

### 11.2 AI 预审分流

```text
AI 预审可输出 PASS / REJECT / MANUAL_REVIEW。
启用 AI_DIRECT_APPROVE 时，高置信 PASS 可直接 APPROVED。
启用 AI_DIRECT_REJECT 时，高置信 REJECT 可直接 RETURNED 给 Labeler。
默认策略下，AI 结果进入人工审核。
AI 失败不阻断人工审核。
AI 自动动作必须写 AgentRun 和 auditLog。
```

### 11.3 审核追踪

```text
Submission versionNo 递增。
旧版本不覆盖。
Reviewer 能看到版本历史和 diff。
Labeler 能看到打回原因。
AgentRun 可查询且不泄露 API Key。
```

### 11.4 冲突与金标

```text
冲突仲裁只能选择组内 submission。
同一 item 最终只有一个 isGolden=true。
金标快照只返回 APPROVED + isGolden=true。
```

### 11.5 增强项

```text
预标注只作建议，不直接提交。
多模态降级时必须显示 limitations。
图片/视频资源处理不落入 BE-A 主责。
```

## 12. 实施优先级

### P0

- Assignment 详情。
- Labeler 提交历史和详情。
- Reviewer 提交详情。
- AI 审核结果统一查询。
- AI 分配人工审核。
- 首次 AI 审核任务恢复。

### P1

- AI 直接通过。
- AI 直接打回。
- AI 手动重试。
- 版本历史和 diff。
- AgentRun 详情。
- 冲突详情增强。
- 多模态上下文 MVP（image 类型 + 降级机制 + promptMode 记录）。

### P2

- Agent 预标注。
- 多模态增强（video 关键帧 + markdown 图片提取 + 多图输入）。
- SupervisorAgent 工具化审核。

## 13. 关键约束

- AI 自动通过和自动打回必须是任务级配置，不应默认开启。
- AI 自动通过必须保留完整 AgentRun、aiReviewResult、auditLog。
- AI 自动打回必须给 Labeler 可读理由。
- AI 分配人工审核是默认安全兜底。
- 所有最终导出仍以 BE-A 金标快照为业务来源。
- 不越界实现 BE-B 的导入、模板、奖励、对象存储和文件导出职责。

## 14. 多模态 AI 审核与预标注完整需求

### 14.1 需求背景

#### 14.1.1 现状问题

当前 AI 审核链路（`AiAutoReviewService.buildPromptSnapshot`）将 `datasetItem.itemJson` 整体序列化为 JSON 文本传入 LLM。对于 image/video/markdown 类型的数据项，`media_url` 仅作为字符串出现在 Prompt 中，LLM 不会真正访问链接，无法理解媒体内容。

`LlmMessage` 仅支持 `String content`，不支持 OpenAI 风格的多模态消息体。

#### 14.1.2 数据集现状

平台示例数据集 `qa_quality.json` 包含：

```text
text:     20 条（纯文本问答）
image:    4 条（图像描述、图片审核）
video:    3 条（视频内容审核）
markdown: 3 条（图文混排审核）
```

image/video/markdown 合计占比 33%。这些数据项的 Prompt 明确要求 AI 理解媒体内容：

```text
image: "请描述这张图片的主要内容"
video: "请观看视频，判断是否包含违规或不适画面"
markdown: "请审核以下图文混排内容的排版质量与合规性"
```

#### 14.1.3 目标

使 AI 预审和 AI 预标注能够真正理解图片、视频关键帧和 Markdown 中嵌入的媒体内容，同时保证非视觉 Provider 的优雅降级。

### 14.2 媒体类型分类与处理策略

#### 14.2.1 媒体类型枚举

```java
public enum MediaType {
    TEXT,
    IMAGE,
    VIDEO,
    MARKDOWN
}
```

#### 14.2.2 各类型处理策略

| media_type | 输入方式 | 视觉 Provider | 非视觉 Provider |
|---|---|---|---|
| `text` | 纯文本 Prompt | 正常 | 正常 |
| `image` | `image_url` content block | 图片直接输入 LLM | 降级为 URL 字符串 + limitations |
| `video` | 关键帧 `image_url[]` + transcript 文本 | 关键帧图片 + 转写文本 | 仅转写文本 + limitations |
| `markdown` | 提取嵌入图片为 `image_url[]` + 原始 Markdown 文本 | 图片 + 文本混合输入 | 仅 Markdown 文本 + limitations |

#### 14.2.3 媒体字段约定

`itemJson` 中与媒体相关的字段：

```json
{
  "media_type": "image",
  "media_url": "https://example.com/image.jpg",
  "content_markdown": "",
  "key_frame_urls": [],
  "video_transcript": "",
  "owner_media_description": ""
}
```

| 字段 | 用途 | 适用类型 |
|---|---|---|
| `media_type` | 媒体类型标识 | 所有 |
| `media_url` | 主媒体资源 URL | image, video |
| `content_markdown` | Markdown 正文（可能嵌入图片/视频标签） | markdown |
| `key_frame_urls` | 视频关键帧图片 URL 列表 | video |
| `video_transcript` | 视频语音转写文本 | video |
| `owner_media_description` | Owner 提供的媒体补充描述 | video, image |

### 14.3 LlmMessage 多模态扩展

#### 14.3.1 当前结构

```java
public record LlmMessage(String role, String content,
                          List<ToolCall> toolCalls, String toolCallId, String name) {}
```

仅支持纯文本 `content`。

#### 14.3.2 扩展设计

新增 `contentParts` 字段，兼容 OpenAI / 通义千问 / 豆包的多模态消息格式：

```java
public record LlmMessage(String role,
                          String content,
                          List<ContentPart> contentParts,
                          List<ToolCall> toolCalls,
                          String toolCallId,
                          String name) {

    public sealed interface ContentPart permits TextPart, ImageUrlPart {}

    public record TextPart(String text) implements ContentPart {}

    public record ImageUrlPart(String url, String detail) implements ContentPart {}
}
```

#### 14.3.3 兼容规则

- 若 `contentParts` 非空，优先使用 `contentParts` 构造请求体。
- 若 `contentParts` 为空或 null，回退到 `content` 字符串（向后兼容）。
- `LlmGateway` 根据 Provider 能力决定序列化方式。

#### 14.3.4 序列化为 OpenAI 格式

```json
{
  "role": "user",
  "content": [
    {"type": "text", "text": "请描述这张图片的主要内容。"},
    {"type": "image_url", "image_url": {"url": "https://example.com/image.jpg", "detail": "auto"}}
  ]
}
```

### 14.4 Provider 能力模型

#### 14.4.1 LlmProvider 扩展字段

在 `llm_providers` 表增加能力标记：

```sql
ALTER TABLE llm_providers
    ADD COLUMN support_vision TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否支持图片输入',
    ADD COLUMN support_multi_image TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否支持多图输入',
    ADD COLUMN max_image_count INT DEFAULT 10 COMMENT '单次请求最大图片数',
    ADD COLUMN vision_model VARCHAR(100) DEFAULT NULL COMMENT '视觉模型名称（若与默认模型不同）';
```

#### 14.4.2 Provider 能力枚举

```java
public record ProviderCapability(
    boolean supportVision,
    boolean supportMultiImage,
    int maxImageCount,
    String visionModel
) {}
```

#### 14.4.3 已知 Provider 能力参考

| Provider | 视觉模型 | 多图 | 最大图片数 |
|---|---|---|---|
| 通义千问 | qwen-vl-plus / qwen-vl-max | 是 | 10 |
| OpenAI | gpt-4o / gpt-4o-mini | 是 | 20 |
| 豆包 | doubao-vision-pro | 是 | 8 |
| 本地部署 | 取决于模型 | 取决于模型 | 配置决定 |

### 14.5 MediaPromptContextBuilder

#### 14.5.1 职责

根据 `itemJson` 中的 `media_type` 和 Provider 能力，构造多模态 `LlmMessage` 列表。同时服务于 AI 预审和 AI 预标注。

#### 14.5.2 接口定义

```java
public interface MediaPromptContextBuilder {

    MediaPromptResult build(MediaPromptInput input);
}

public record MediaPromptInput(
    String itemJson,
    String answerJson,
    String promptTemplate,
    ProviderCapability providerCapability
) {}

public record MediaPromptResult(
    List<LlmMessage> messages,
    PromptMode promptMode,
    boolean degraded,
    List<String> limitations
) {}

public enum PromptMode {
    TEXT_ONLY,
    IMAGE_SINGLE,
    IMAGE_MULTI,
    VIDEO_KEYFRAMES,
    MARKDOWN_WITH_IMAGES
}
```

#### 14.5.3 构造逻辑

```text
1. 解析 itemJson，提取 media_type。
2. 根据 media_type 分支：
   - TEXT → 纯文本 Prompt，promptMode = TEXT_ONLY。
   - IMAGE → 检查 Provider.supportVision：
     - 支持 → 构造 [TextPart(prompt), ImageUrlPart(media_url)]，promptMode = IMAGE_SINGLE。
     - 不支持 → 降级为纯文本 + limitations，promptMode = TEXT_ONLY，degraded = true。
   - VIDEO → 检查 Provider.supportVision：
     - 支持且有 key_frame_urls → 构造 [TextPart(prompt + transcript), ImageUrlPart(frame1), ...]，promptMode = VIDEO_KEYFRAMES。
     - 支持但无关键帧 → 仅 transcript + limitations。
     - 不支持 → 仅 transcript + owner_media_description + limitations。
   - MARKDOWN → 提取 content_markdown 中的 ![](url) 图片链接：
     - 支持视觉且有图片 → 构造 [TextPart(markdown_text), ImageUrlPart(img1), ...]，promptMode = MARKDOWN_WITH_IMAGES。
     - 不支持 → 仅 Markdown 文本 + limitations。
3. 返回 MediaPromptResult。
```

#### 14.5.4 Markdown 图片提取规则

```java
// 提取 Markdown 中的图片 URL
Pattern IMG_PATTERN = Pattern.compile("!\\[.*?\\]\\((https?://[^)]+)\\)");
// 提取 HTML img 标签
Pattern HTML_IMG_PATTERN = Pattern.compile("<img[^>]+src=[\"'](https?://[^\"']+)[\"']");
```

限制：

- 单次请求图片数不超过 `Provider.maxImageCount`。
- 超出部分截断，并在 limitations 中说明。
- 不处理 base64 内联图片（体积过大）。

### 14.6 AI 预审多模态集成

#### 14.6.1 改造点

`AiAutoReviewService.buildPromptSnapshot()` 改为调用 `MediaPromptContextBuilder`：

```text
原流程：
  buildPromptSnapshot() → JSON 文本 → LlmMessage("user", text)

新流程：
  MediaPromptContextBuilder.build(input) → MediaPromptResult
    → messages (含 contentParts)
    → promptMode / degraded / limitations
  LlmGateway.review(request) 使用多模态 messages
```

#### 14.6.2 Prompt 模板适配

AI 审核 Prompt 模板需要感知媒体类型，增加条件段：

```text
[通用段]
你是 LabelHub AI 审核员。请根据以下数据项和标注答案进行评审。

[媒体段 - 仅 image/video/markdown 时插入]
注意：本题包含 {media_type} 类型的媒体内容。
- 请仔细观察媒体内容后再做判断。
- 如果媒体无法加载或内容不清晰，请在 riskFlags 中标记 "MEDIA_UNCLEAR"。

[降级段 - 仅降级时插入]
注意：当前模型不支持直接查看媒体内容，仅提供了媒体的文字描述。
请基于文字描述进行评审，并在 limitations 中说明无法验证的部分。
```

#### 14.6.3 AI 输出扩展

AI 结构化输出增加 `limitations` 字段：

```json
{
  "decision": "MANUAL_REVIEW",
  "averageScore": 3.5,
  "dimensionScores": {"相关性": 4, "准确性": 3, "格式合规": 4},
  "riskFlags": ["MEDIA_UNCLEAR"],
  "suggestion": "图片内容无法完全确认，建议人工复核。",
  "confidence": 0.65,
  "reason": "...",
  "limitations": ["视频内容仅基于转写文本判断，未直接观看画面"]
}
```

#### 14.6.4 分流策略适配

多模态场景下的分流规则补充：

```text
- 降级模式下，confidence 自动降低 0.2（可配置）。
- 降级模式下，不允许 AI_DIRECT_APPROVE（必须人工复核）。
- 降级模式下，AI_DIRECT_REJECT 仅在明显违规时允许（如空答案）。
- limitations 非空时，flowAction 倾向 AI_ASSIGN_MANUAL_REVIEW。
```

### 14.7 AI 预标注多模态集成

#### 14.7.1 改造点

预标注 Agent 同样使用 `MediaPromptContextBuilder` 构造多模态输入：

```text
POST /api/v1/assignments/{assignmentId}/pre-annotations/run
  → 解析 datasetItem.itemJson 的 media_type
  → MediaPromptContextBuilder.build(input)
  → LLM 调用（多模态消息）
  → 返回 suggestedAnswerJson
```

#### 14.7.2 预标注输出扩展

```json
{
  "suggestedAnswerJson": {"description": "图片展示了一条夜间公路..."},
  "fieldSuggestions": [
    {"field": "description", "confidence": 0.88, "source": "VISION"}
  ],
  "riskFlags": [],
  "overallConfidence": 0.85,
  "limitations": [],
  "promptMode": "IMAGE_SINGLE",
  "degraded": false
}
```

#### 14.7.3 降级时的预标注行为

- 降级时 `overallConfidence` 自动降低。
- 降级时 `fieldSuggestions` 中与媒体内容直接相关的字段标记 `"source": "DEGRADED"`。
- 前端展示时对 `DEGRADED` 来源的建议增加警告提示。

### 14.8 LlmGateway 多模态适配

#### 14.8.1 请求构造

`LlmGateway` 根据 `LlmMessage.contentParts` 是否存在决定请求格式：

```java
// 纯文本模式（向后兼容）
{"role": "user", "content": "..."}

// 多模态模式
{"role": "user", "content": [
    {"type": "text", "text": "..."},
    {"type": "image_url", "image_url": {"url": "...", "detail": "auto"}}
]}
```

#### 14.8.2 Provider 适配层

不同 Provider 的多模态格式差异：

| Provider | 图片格式 | 差异点 |
|---|---|---|
| OpenAI | `image_url.url` | 标准格式 |
| 通义千问 | `image_url.url` | 兼容 OpenAI 格式 |
| 豆包 | `image_url.url` | 兼容 OpenAI 格式 |
| Ollama 本地 | `images: [base64]` | 需要下载图片转 base64 |

MVP 阶段仅支持 OpenAI 兼容格式（URL 直传），不做 base64 转换。

#### 14.8.3 超时配置

多模态请求通常耗时更长，需要独立超时配置：

```yaml
labelhub:
  llm:
    gateway:
      timeout-ms: 30000
      vision-timeout-ms: 60000
```

### 14.9 AgentRun 元数据扩展

#### 14.9.1 inputSnapshot 扩展

AgentRun 的 `inputSnapshot` 增加多模态元数据：

```json
{
  "promptMode": "IMAGE_SINGLE",
  "degraded": false,
  "mediaType": "image",
  "mediaUrl": "https://example.com/image.jpg",
  "imageCount": 1,
  "hasTranscript": false,
  "limitations": []
}
```

#### 14.9.2 审计字段

审计日志增加多模态相关信息：

```json
{
  "submissionId": 1001,
  "agentRunId": 9001,
  "decision": "PASS",
  "flowAction": "AI_ASSIGN_MANUAL_REVIEW",
  "promptMode": "IMAGE_SINGLE",
  "degraded": false,
  "confidence": 0.85,
  "limitations": []
}
```

### 14.10 降级策略详细设计

#### 14.10.1 降级触发条件

| 条件 | 降级行为 |
|---|---|
| Provider 不支持视觉 (`supportVision=false`) | 所有媒体降级为纯文本 |
| 图片 URL 无法访问（超时/404） | 该图片跳过，标记 limitation |
| 图片数超过 `maxImageCount` | 截断多余图片，标记 limitation |
| 视频无关键帧且无转写 | 仅使用 `owner_media_description`，标记 limitation |
| `media_url` 为空 | 标记 `MEDIA_URL_MISSING`，降级为纯文本 |

#### 14.10.2 降级后的 Prompt 补充

降级时在 Prompt 末尾追加：

```text
[系统提示]
当前模型无法直接查看以下媒体内容：
- 图片: {media_url}
请仅基于文字信息进行判断。如果无法可靠评估，请将 decision 设为 MANUAL_REVIEW，
并在 limitations 中说明原因。
```

#### 14.10.3 降级对分流的影响

```java
// 降级时的 confidence 惩罚
if (degraded) {
    effectiveConfidence = Math.max(0, rawConfidence - degradationPenalty);
}

// 降级时禁止 AI 直接通过
if (degraded && flowAction == AI_DIRECT_APPROVE) {
    flowAction = AI_ASSIGN_MANUAL_REVIEW;
}
```

`degradationPenalty` 默认 0.2，可在 `AiReviewConfig` 中配置。

### 14.11 配置模型扩展

#### 14.11.1 AiReviewConfig 新增字段

```sql
ALTER TABLE ai_review_configs
    ADD COLUMN multimodal_enabled TINYINT(1) NOT NULL DEFAULT 1 COMMENT '是否启用多模态',
    ADD COLUMN degradation_penalty DECIMAL(3,2) DEFAULT 0.20 COMMENT '降级时 confidence 惩罚',
    ADD COLUMN vision_detail VARCHAR(20) DEFAULT 'auto' COMMENT '图片细节级别: auto/low/high',
    ADD COLUMN max_images_per_request INT DEFAULT 5 COMMENT '单次请求最大图片数';
```

#### 14.11.2 任务级配置示例

```json
{
  "multimodalEnabled": true,
  "degradationPenalty": 0.2,
  "visionDetail": "auto",
  "maxImagesPerRequest": 5,
  "allowAiDirectApproveWhenDegraded": false
}
```

### 14.12 错误处理

#### 14.12.1 错误码定义

| 错误码 | 含义 | 处理 |
|---|---|---|
| `MULTIMODAL_NOT_SUPPORTED` | Provider 不支持视觉但数据含媒体 | 降级为纯文本，标记 limitation |
| `MEDIA_URL_MISSING` | itemJson 中 media_url 为空 | 降级为纯文本，标记 limitation |
| `MEDIA_ACCESS_FAILED` | 媒体 URL 无法访问 | 降级为纯文本，标记 limitation |
| `IMAGE_COUNT_EXCEEDED` | 图片数超过 Provider 限制 | 截断，标记 limitation |
| `KEY_FRAME_MISSING` | 视频无关键帧 | 仅用 transcript，标记 limitation |
| `TRANSCRIPT_MISSING` | 视频无转写文本 | 仅用 description，标记 limitation |
| `VISION_TIMEOUT` | 视觉请求超时 | 重试一次，仍失败则降级 |

#### 14.12.2 错误处理原则

- 所有媒体相关错误**不阻断审核流程**。
- 降级后仍可产出审核结论（基于文本信息）。
- 降级信息完整记录在 AgentRun 和 AiReviewResult 中。
- Reviewer 可在详情中看到降级标记和 limitations。

### 14.13 数据库变更汇总

```sql
-- V9__multimodal_support.sql

ALTER TABLE llm_providers
    ADD COLUMN support_vision TINYINT(1) NOT NULL DEFAULT 0,
    ADD COLUMN support_multi_image TINYINT(1) NOT NULL DEFAULT 0,
    ADD COLUMN max_image_count INT DEFAULT 10,
    ADD COLUMN vision_model VARCHAR(100) DEFAULT NULL;

ALTER TABLE ai_review_configs
    ADD COLUMN multimodal_enabled TINYINT(1) NOT NULL DEFAULT 1,
    ADD COLUMN degradation_penalty DECIMAL(3,2) DEFAULT 0.20,
    ADD COLUMN vision_detail VARCHAR(20) DEFAULT 'auto',
    ADD COLUMN max_images_per_request INT DEFAULT 5;
```

### 14.14 实现文件清单

| 文件 | 变更类型 | 说明 |
|---|---|---|
| `LlmMessage.java` | 修改 | 增加 `contentParts`、`ContentPart`、`TextPart`、`ImageUrlPart` |
| `MediaPromptContextBuilder.java` | 新增 | 多模态 Prompt 构造器接口 |
| `DefaultMediaPromptContextBuilder.java` | 新增 | 默认实现 |
| `MediaPromptInput.java` | 新增 | 构造器输入 |
| `MediaPromptResult.java` | 新增 | 构造器输出 |
| `PromptMode.java` | 新增 | Prompt 模式枚举 |
| `ProviderCapability.java` | 新增 | Provider 能力记录 |
| `LlmProvider.java` | 修改 | 增加视觉能力字段 |
| `LlmGateway.java` | 修改 | 支持多模态消息序列化 |
| `AiAutoReviewService.java` | 修改 | 使用 MediaPromptContextBuilder |
| `AiReviewConfig.java` | 修改 | 增加多模态配置字段 |
| `AiReviewConfigRequest.java` | 修改 | 增加多模态配置参数 |
| `AiReviewResultResponse.java` | 修改 | 增加 promptMode、degraded、limitations |
| `AgentRun inputSnapshot` | 修改 | 记录多模态元数据 |
| `V9__multimodal_support.sql` | 新增 | 数据库迁移 |

### 14.15 测试与验收

#### 14.15.1 单元测试

```text
MediaPromptContextBuilder：
  - text 类型 → 纯文本消息，promptMode=TEXT_ONLY。
  - image 类型 + 视觉 Provider → image_url content block。
  - image 类型 + 非视觉 Provider → 降级为纯文本 + limitations。
  - video 类型 + 有关键帧 → 多图 content blocks。
  - video 类型 + 无关键帧有转写 → 纯文本 + transcript。
  - markdown 类型 → 提取嵌入图片为 image_url blocks。
  - 图片数超限 → 截断 + limitation。
  - media_url 为空 → 降级 + MEDIA_URL_MISSING。
```

#### 14.15.2 集成测试

```text
AI 预审完整链路：
  - image 数据项 + 视觉 Provider → AI 能描述图片内容。
  - image 数据项 + 非视觉 Provider → 降级，decision 倾向 MANUAL_REVIEW。
  - 降级模式下 AI_DIRECT_APPROVE 被拦截为 AI_ASSIGN_MANUAL_REVIEW。
  - AgentRun.inputSnapshot 包含 promptMode 和 degraded 信息。
  - AiReviewResult 包含 limitations。

AI 预标注完整链路：
  - image 数据项 → suggestedAnswerJson 包含图片描述。
  - 降级模式 → fieldSuggestions 标记 source=DEGRADED。
  - overallConfidence 在降级时自动降低。
```

#### 14.15.3 验收标准

```text
功能验收：
  - 图片类型数据项，AI 预审能基于图片内容给出评分和建议。
  - 视频类型数据项，AI 预审能基于关键帧/转写给出评分。
  - Markdown 类型数据项，AI 预审能识别嵌入图片内容。
  - 非视觉 Provider 不报错，优雅降级并标记 limitations。
  - Reviewer 详情页能看到 promptMode 和降级信息。
  - Owner 可配置是否启用多模态、降级惩罚系数。

性能验收：
  - 多模态请求超时独立配置，不影响纯文本请求。
  - 单次请求图片数可控，不超过 Provider 限制。

安全验收：
  - 不下载外部图片到服务器（URL 直传给 LLM Provider）。
  - 不在日志中打印完整图片 URL（脱敏）。
  - media_url 仅接受 http/https 协议。
```

### 14.16 实施分期

#### P1 MVP（建议与 AI 直接通过/打回同期）

- `LlmMessage` 多模态扩展（contentParts）。
- `MediaPromptContextBuilder` 实现（image 类型）。
- `LlmProvider` 增加 `supportVision` 字段。
- `LlmGateway` 多模态序列化。
- `AiAutoReviewService` 集成 MediaPromptContextBuilder。
- 降级机制（非视觉 Provider 回退纯文本）。
- AgentRun 记录 promptMode。
- 数据库迁移 V9。

#### P2 增强

- video 关键帧多图输入。
- markdown 嵌入图片提取。
- AI 预标注多模态集成。
- `vision_detail` 配置（low/high/auto）。
- 多图输入优化（图片数量控制、截断策略）。
- 降级 confidence 惩罚可配置化。
- Reviewer 详情展示 limitations 和 promptMode。
