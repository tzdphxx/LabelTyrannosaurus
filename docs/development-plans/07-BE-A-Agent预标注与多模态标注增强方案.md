# 07-BE-A Agent 预标注与多模态标注增强方案

> 面向 AI 开发工具与课题答辩：本文档是在现有 LabelHub 任务书基础上的新增增强方案，不覆盖原有 FE、BE-A、BE-B 职责划分。本文重点描述 BE-A 作为“审核智能业务引擎”可以扩展的 Agent 预标注能力，以及多模态题目如何进入标注、AI 辅助、AI 预审和人工终审链路。

## 0. 文档定位

本文档用于回答：作为 BE-A 成员，在现有任务状态机、提交版本、AI 预审、人工终审、冲突仲裁已经基本成型的情况下，还可以围绕智能业务引擎继续扩展什么功能。

推荐扩展方向为：

1. **Agent 预标注**：Labeler 在提交前主动调用 AI Agent，生成整题级标注建议。
2. **多模态标注扩展**：让 `text / image / markdown / video` 题目进入统一标注与 AI 辅助链路。

这两个方向不是独立堆功能，而是组合成一条新的可演示闭环：

```text
Owner 导入含图片或图文的样例数据
  -> Owner 搭建标注模板
  -> Owner 配置支持多模态的 LLM Provider
  -> Labeler 领取题目
  -> Labeler 点击 AI 预标注
  -> AI 根据题目文本、图片/图文/视频辅助信息和模板生成建议
  -> Labeler 采纳、修改或忽略建议
  -> Labeler 手动提交
  -> AI 自动预审
  -> Reviewer 查看原始媒体、预标注记录、最终提交、AI 预审结果
  -> Reviewer 终审通过或打回
  -> 生成金标并导出
```

核心原则：**AI 只辅助，不替代人类决策。**

---

## 1. 与课题现有设计的契合点

### 1.1 现有项目基础

当前 LabelHub 已经设计或实现了以下能力：

- 任务生命周期状态机：`DRAFT -> PUBLISHED -> PAUSED / ENDED`。
- Labeler 领取题目、保存草稿、提交版本。
- Submission 状态流转：提交后进入 AI 预审，再进入人工终审。
- LLM Provider 管理和 OpenAI-compatible 调用链路。
- `AgentRun`：记录每一次 AI Agent 运行。
- `LlmTrigger`：字段级 AI 辅助组件。
- AI 自动预审：提交后生成 AI 审核建议。
- Reviewer 人工终审、批量审核、冲突仲裁、金标选择。
- BE-B 提供数据集、模板、对象存储、Schema 校验、奖励、导出等平台能力。

这些能力说明项目已经具备“AI 审核”基础，但 AI 目前主要发生在两个位置：

| 位置 | 现有能力 | 局限 |
|---|---|---|
| 标注过程中 | `LlmTrigger` 字段级辅助 | 只针对单个组件，不生成整题初稿 |
| 提交之后 | AI 自动预审 | 只辅助 Reviewer，不帮助 Labeler 提高标注效率 |

Agent 预标注正好补齐“提交前整题级 AI 辅助”。

### 1.2 与示例数据的契合

本课题目录下的 `datasets` 示例数据包含两类任务：

- `qa_quality`：问答质量标注。
- `preference_compare`：偏好对比标注。

其中 `qa_quality` 已经包含以下字段：

```json
{
  "media_type": "text",
  "media_url": "",
  "content_markdown": "",
  "prompt": "...",
  "model_answer": "...",
  "reference": "...",
  "expected_dimensions": ["相关性", "准确性"]
}
```

对应标注要求文档也明确写到：部分题目可能是 **视频 / 图片 / Markdown 图文**，需要先查看媒体素材再标注。

因此，多模态扩展不是凭空添加，而是把示例数据里已经存在的字段真正纳入业务链路。

### 1.3 与 BE-A 职责的契合

BE-A 原任务书强调：

- LLM Provider / Gateway。
- System Agent。
- Agent Run。
- AI 审核配置。
- AI 自动预审。
- 结构化输出校验。
- 失败重试和人工兜底。
- AI 结果到人工终审的状态推进。

Agent 预标注和多模态 AI 辅助仍然属于这些范围：

- 仍然通过 LLM Provider 调模型。
- 每次运行仍然生成 AgentRun。
- 仍然需要结构化输出校验。
- 仍然需要审计和限流。
- 仍然坚持 AI 不能终审。

BE-A 不需要接管 BE-B 的数据导入、对象存储和文件管理职责。

---

## 2. 增强目标

### 2.1 总体目标

在现有 LabelHub 基础上新增一条“提交前智能辅助”链路，使平台支持：

- Labeler 领取题目后可以一键生成 AI 预标注建议。
- AI 建议以结构化 JSON 返回，能够映射到模板字段。
- Labeler 可以逐字段采纳、忽略或修改 AI 建议。
- 采纳后的内容只进入草稿，不直接提交。
- 提交后仍然进入原有 AI 预审和人工终审流程。
- 对图片、Markdown、视频类题目，AI Prompt 能携带相应媒体上下文。
- Reviewer 能看到预标注记录、AI 预审记录和最终人工提交之间的关系。

### 2.2 角色收益

| 角色 | 收益 |
|---|---|
| Owner | 可以设计更复杂的文本、图片、视频、图文混合标注任务 |
| Labeler | 可以借助 AI 快速生成初稿，减少重复性评分和说明工作 |
| Reviewer | 可以看到 AI 预标注和最终提交的差异，判断标注质量更有依据 |
| Admin | 可以配置支持多模态的模型 Provider，控制模型可用性 |
| BE-A | 扩展智能业务引擎，体现 AgentRun、LLM、审计、状态机的综合能力 |
| BE-B | 继续提供数据资产、媒体存储、Schema 校验、导出等平台能力 |
| FE | 提供更完整的人机协同标注体验 |

### 2.3 答辩目标

答辩时可以重点展示：

1. AI 不仅在提交后审核，还可以在提交前辅助标注。
2. 多模态题目可以被平台接入和展示。
3. 图片题可以由 AI 根据图片内容和模型回答生成评分建议。
4. 所有 AI 结果都有 AgentRun，可追踪、可审计、可回放。
5. AI 的建议必须经过 Labeler 和 Reviewer 确认，不会替代人工。

---

## 3. 职责边界

### 3.1 BE-A 负责

BE-A 负责智能业务链路：

- Agent 预标注服务。
- 预标注运行状态管理。
- 预标注结果结构化校验。
- 预标注结果持久化。
- 预标注与 AgentRun 关联。
- 预标注审计记录。
- 多模态内容进入 Prompt 的规则。
- 图片、Markdown、视频题目在 AI 预标注和 AI 预审中的输入快照。
- Reviewer 查询时返回 AI 预标注摘要。
- 失败、限流、模型不可用等情况的人工兜底。
- 保证 AI 不能直接提交、不能终审、不能生成金标。

### 3.2 BE-B 负责

BE-B 继续负责平台支撑和数据资产：

- 数据集导入。
- `media_type`、`media_url`、`content_markdown` 等字段解析和保存。
- 图片、视频、附件上传。
- 对象存储。
- 预签名 URL。
- 文件类型、大小、访问权限控制。
- 模板版本存储。
- Schema 校验服务。
- 导出任务和文件生成。
- 可选：视频抽帧、视频转写、媒体元数据解析。

### 3.3 FE 负责

FE 负责用户可见交互：

- 标注页展示图片、视频、Markdown。
- AI 预标注按钮。
- AI 建议面板。
- 字段级采纳、忽略、一键采纳。
- 采纳后写入表单和草稿。
- Reviewer 页面展示预标注记录和 AI 预审结果。
- 明确区分“AI 建议”和“人工最终提交”。

### 3.4 明确不做

MVP 阶段不做以下内容：

- AI 自动提交。
- AI 自动终审通过。
- AI 自动选择金标。
- 完整视频理解。
- 视频转码、抽帧、语音识别。
- 媒体文件二进制入库。
- 真实支付或真实奖励结算。
- 上传任意插件或动态执行用户代码。

---

## 4. 功能一：Agent 预标注

### 4.1 功能定义

Agent 预标注是指：Labeler 在正式提交前，主动触发 AI Agent。AI Agent 根据题目内容、媒体素材、标注模板、标注规则和当前草稿，生成一份结构化标注建议。Labeler 必须人工确认后，建议才可以写入表单或草稿。

它不同于现有 AI 自动预审。

| 能力 | 发生时间 | 服务对象 | 输入 | 输出 | 是否影响业务状态 |
|---|---|---|---|---|---|
| Agent 预标注 | 提交前 | Labeler | 题目 + 模板 + 草稿 | 建议答案 | 不改变 submission 状态 |
| AI 自动预审 | 提交后 | Reviewer | 题目 + 最终答案 | 审核建议 | 推进到待人工终审 |
| 人工终审 | AI 预审后 | Reviewer | 完整上下文 | 通过 / 打回 | 改变最终状态 |

### 4.2 典型流程

```text
1. Labeler 领取任务，生成 assignment。
2. Labeler 进入标注页。
3. 前端加载 itemJson、template schema、draftAnswerJson。
4. Labeler 点击“AI 预标注”。
5. BE-A 校验 assignment 归属和任务状态。
6. BE-A 读取 task、templateVersion、datasetItem、currentAnswerJson。
7. BE-A 构造预标注 Prompt。
8. BE-A 创建 AgentRun。
9. BE-A 调用 LLM Provider。
10. BE-A 校验 AI 输出是否符合模板字段。
11. BE-A 保存 ai_pre_annotation。
12. FE 展示建议。
13. Labeler 采纳、修改或忽略。
14. 采纳内容进入草稿。
15. Labeler 手动提交。
```

### 4.3 状态设计

预标注结果状态：

```text
PENDING
RUNNING
SUCCESS
FAILED
RATE_LIMITED
MANUAL_REQUIRED
```

| 状态 | 含义 |
|---|---|
| PENDING | 已创建，等待执行 |
| RUNNING | 正在调用模型 |
| SUCCESS | 成功生成建议 |
| FAILED | 模型调用失败或输出非法 |
| RATE_LIMITED | 限流命中 |
| MANUAL_REQUIRED | AI 无法可靠判断，需要人工处理 |

这些状态只描述预标注运行，不代表 assignment 或 submission 状态。

### 4.4 预标注模式

MVP 只支持一种模式：

```text
SUGGEST_ONLY
```

含义：

- 只生成建议。
- 不保存为正式提交。
- 不自动覆盖草稿。
- 不触发 AI 自动预审。
- 不生成 review record。
- 不影响金标。

后续可扩展：

| 模式 | 说明 | 是否建议 MVP 实现 |
|---|---|---|
| SUGGEST_ONLY | 只生成建议，用户确认后采纳 | 是 |
| PREFILL_DRAFT | 后端直接保存到草稿，但仍需用户提交 | 否 |
| AUTO_SUBMIT | 自动提交 | 不允许 |

---

## 5. API 设计

### 5.1 运行预标注

```text
POST /api/v1/assignments/{assignmentId}/pre-annotations/run
```

请求：

```json
{
  "templateVersionId": 1,
  "datasetItemId": 10,
  "currentAnswerJson": {
    "accuracy_score": "4"
  },
  "mode": "SUGGEST_ONLY"
}
```

字段说明：

| 字段 | 含义 |
|---|---|
| templateVersionId | 当前标注模板版本 |
| datasetItemId | 当前题目 ID |
| currentAnswerJson | 当前草稿，可为空 |
| mode | 固定为 `SUGGEST_ONLY` |

响应：

```json
{
  "preAnnotationId": 1001,
  "agentRunId": 9001,
  "status": "SUCCESS",
  "suggestedAnswerJson": {
    "relevance_score": "5",
    "accuracy_score": "4",
    "issue_tags": ["信息缺失"],
    "summary": "回答整体正确，但细节不够完整。",
    "comment": "模型回答覆盖了核心结论，但缺少对参考答案中关键条件的说明。"
  },
  "fieldSuggestions": [
    {
      "field": "accuracy_score",
      "value": "4",
      "confidence": 0.82,
      "reason": "回答主结论正确，但参考答案中的补充条件未完全覆盖。"
    }
  ],
  "riskFlags": [],
  "overallConfidence": 0.86,
  "ignoredFields": [],
  "limitations": [],
  "adoptionPolicy": "USER_CONFIRM_REQUIRED",
  "errorCode": null,
  "errorMessage": null
}
```

### 5.2 查询最新预标注

```text
GET /api/v1/assignments/{assignmentId}/pre-annotations/latest
```

用途：

- 页面刷新后恢复最近一次 AI 建议。
- Reviewer 查看 Labeler 是否使用过 AI 预标注。
- 避免重复调用模型。

### 5.3 查询预标注详情

```text
GET /api/v1/pre-annotations/{preAnnotationId}
```

返回：

- 预标注建议。
- AgentRun ID。
- 输入摘要。
- 输出摘要。
- 错误原因。
- 创建人。
- 创建时间。

### 5.4 Reviewer 查询扩展

Reviewer 提交详情接口可扩展返回：

```json
{
  "submissionId": 2001,
  "answerJson": {},
  "aiReviewResult": {},
  "latestPreAnnotation": {
    "preAnnotationId": 1001,
    "agentRunId": 9001,
    "status": "SUCCESS",
    "overallConfidence": 0.86,
    "suggestedAnswerJson": {},
    "fieldSuggestions": []
  }
}
```

这样 Reviewer 能看到：

- AI 预标注建议。
- Labeler 最终提交。
- AI 自动预审建议。

---

## 6. 数据模型建议

### 6.1 ai_pre_annotations

```text
ai_pre_annotations
```

字段建议：

| 字段 | 类型建议 | 含义 |
|---|---|---|
| id | BIGINT | 主键 |
| assignment_id | BIGINT | 领取记录 |
| task_id | BIGINT | 任务 ID |
| dataset_item_id | BIGINT | 题目 ID |
| template_version_id | BIGINT | 模板版本 |
| agent_run_id | BIGINT | 对应 AgentRun |
| status | VARCHAR | 运行状态 |
| suggested_answer_json | JSON | AI 建议答案 |
| field_suggestions_json | JSON | 字段级建议 |
| risk_flags_json | JSON | 风险标签 |
| overall_confidence | DECIMAL | 整体置信度 |
| ignored_fields_json | JSON | 被过滤的非法字段 |
| limitations_json | JSON | 能力限制说明 |
| error_code | VARCHAR | 错误码 |
| error_message | VARCHAR | 错误信息 |
| created_by | BIGINT | 触发用户 |
| created_at | DATETIME | 创建时间 |
| updated_at | DATETIME | 更新时间 |

建议索引：

```text
idx_ai_pre_annotations_assignment_created(assignment_id, created_at)
idx_ai_pre_annotations_task(task_id)
idx_ai_pre_annotations_agent_run(agent_run_id)
```

### 6.2 ai_pre_annotation_adoptions（可选）

如果希望答辩时体现“AI 建议是否被人工采纳”，可以增加采纳记录表：

| 字段 | 含义 |
|---|---|
| id | 主键 |
| pre_annotation_id | 预标注 ID |
| assignment_id | 领取记录 |
| field | 字段名 |
| suggested_value_json | AI 建议值 |
| adopted_value_json | 最终采纳值 |
| adopted | 是否采纳 |
| adopted_by | 采纳用户 |
| adopted_at | 采纳时间 |

MVP 可以先不做该表，只在前端表单和审计日志中体现采纳行为。

---

## 7. 多模态标注扩展

### 7.1 数据字段定义

当前 `qa_quality` 示例数据已经包含：

```json
{
  "media_type": "text",
  "media_url": "",
  "content_markdown": "",
  "prompt": "...",
  "model_answer": "...",
  "reference": "..."
}
```

建议正式定义为：

| 字段 | 含义 |
|---|---|
| media_type | 原始数据类型 |
| media_url | 图片或视频 URL |
| content_markdown | Markdown 图文正文 |
| prompt | 用户问题或任务说明 |
| model_answer | 待评估回答 |
| reference | 参考答案或评判要点 |
| expected_dimensions | 建议评估维度 |
| key_frame_urls | 视频关键帧 URL，视频题可选 |
| video_transcript | 视频转写文本，视频题可选 |
| owner_media_description | Owner 对媒体内容的补充说明，可选 |

### 7.2 media_type 类型

| 类型 | 含义 | MVP 支持情况 |
|---|---|---|
| text | 纯文本题 | 完整支持 |
| image | 图片题 | 完整支持，要求 Provider 支持图片输入 |
| markdown | Markdown 图文题 | 支持，图片可作为 URL 或多图输入 |
| video | 视频题 | 半自动支持，基于关键帧/转写/说明 |

### 7.3 支持优先级

推荐顺序：

1. 文本预标注。
2. 图片预标注。
3. Markdown 图文预标注。
4. 视频关键帧辅助预标注。

原因：

- 文本能力与现有链路最接近。
- 图片多模态最适合答辩展示。
- Markdown 复用文本和图片能力即可。
- 视频完整理解成本较高，MVP 采用关键帧和转写文本更稳妥。

---

## 8. Prompt 构造总规则

### 8.1 Prompt 输入组成

每次预标注 Prompt 包含：

```text
taskContext
itemSnapshot
templateFields
currentAnswerJson
annotationGuideline
mediaContext
outputContract
```

说明：

| 部分 | 内容 |
|---|---|
| taskContext | taskId、datasetType、任务标题、评分维度 |
| itemSnapshot | datasetItem.itemJson |
| templateFields | 可提交字段及类型、options、required |
| currentAnswerJson | 用户当前草稿 |
| annotationGuideline | 标注要求摘要 |
| mediaContext | 图片、Markdown、视频辅助信息 |
| outputContract | AI 必须返回的 JSON 结构 |

### 8.2 系统 Prompt 通用模板

```text
你是 LabelHub 的 AI 预标注 Agent。

你的任务是根据题目内容、媒体素材、参考答案、当前草稿和标注模板，生成结构化标注建议。

规则：
1. 你只能输出 JSON，不要输出 Markdown、解释性前后缀或代码块。
2. 你不能替用户提交答案。
3. 你只能填写模板中允许提交的字段。
4. 不要输出 ShowItem 展示字段。
5. 如果信息不足、媒体无法访问或无法可靠判断，请降低 confidence，并在 limitations 中说明。
6. 不允许编造媒体中不存在的信息。
7. 输出必须包含 suggestedAnswerJson、fieldSuggestions、riskFlags、overallConfidence、limitations。
```

### 8.3 输出契约

```json
{
  "suggestedAnswerJson": {},
  "fieldSuggestions": [
    {
      "field": "字段名",
      "value": "建议值",
      "confidence": 0.85,
      "reason": "为什么建议这个值"
    }
  ],
  "riskFlags": [],
  "overallConfidence": 0.85,
  "limitations": []
}
```

---

## 9. 图片预标注设计

### 9.1 业务场景

图片题示例：

```json
{
  "id": "Q_IMAGE_001",
  "category": "图像理解",
  "media_type": "image",
  "media_url": "https://example.com/image.png",
  "prompt": "请判断模型回答是否准确描述图片内容。",
  "model_answer": "图片中是一只黑色猫坐在沙发上。",
  "reference": "图片中是一只白色狗趴在草地上。",
  "expected_dimensions": ["准确性", "相关性"]
}
```

Labeler 需要判断模型回答是否准确描述图片。

### 9.2 Prompt 用户消息文本部分

```json
{
  "task": {
    "taskId": 1,
    "datasetType": "QA_QUALITY",
    "annotationGuideline": "按相关性、准确性、格式合规、安全性评分。"
  },
  "item": {
    "id": "Q_IMAGE_001",
    "media_type": "image",
    "media_url": "https://example.com/signed/image.png",
    "prompt": "请判断模型回答是否准确描述图片内容。",
    "model_answer": "图片中是一只黑色猫坐在沙发上。",
    "reference": "图片中是一只白色狗趴在草地上。"
  },
  "templateFields": [
    {
      "field": "accuracy_score",
      "type": "Radio",
      "options": ["1", "2", "3", "4", "5"],
      "required": true
    },
    {
      "field": "issue_tags",
      "type": "Checkbox",
      "options": ["事实错误", "答非所问", "格式问题", "安全违规"]
    },
    {
      "field": "comment",
      "type": "Textarea",
      "required": true
    }
  ],
  "currentAnswerJson": {}
}
```

### 9.3 图片输入部分

如果 Provider 支持 OpenAI 风格多模态 message，图片部分可表达为：

```json
{
  "type": "image_url",
  "image_url": {
    "url": "https://example.com/signed/image.png"
  }
}
```

如果当前 Gateway 暂未支持多模态 message，可先把图片 URL 放入 prompt 文本，同时要求 Provider 使用可访问 URL 判断。后续再扩展 `LlmMessage` 支持 content array。

### 9.4 AI 输出示例

```json
{
  "suggestedAnswerJson": {
    "accuracy_score": "1",
    "issue_tags": ["事实错误"],
    "comment": "模型回答与图片内容明显不符。图片显示白色狗在草地上，而回答描述为黑色猫在沙发上。"
  },
  "fieldSuggestions": [
    {
      "field": "accuracy_score",
      "value": "1",
      "confidence": 0.95,
      "reason": "图片主体和场景均与模型回答不一致。"
    },
    {
      "field": "issue_tags",
      "value": ["事实错误"],
      "confidence": 0.93,
      "reason": "回答识别了错误的动物、颜色和场景。"
    },
    {
      "field": "comment",
      "value": "模型回答与图片内容明显不符。图片显示白色狗在草地上，而回答描述为黑色猫在沙发上。",
      "confidence": 0.90,
      "reason": "该评语解释了主要错误点。"
    }
  ],
  "riskFlags": [],
  "overallConfidence": 0.94,
  "limitations": []
}
```

---

## 10. Markdown 图文预标注设计

### 10.1 业务场景

Markdown 题可能包含正文、表格、图片链接、代码块等：

```json
{
  "media_type": "markdown",
  "content_markdown": "请阅读下图：![chart](https://example.com/chart.png)\n图中展示了 Q1-Q4 的销售额变化。",
  "prompt": "判断模型回答是否正确概括图表趋势。",
  "model_answer": "销售额逐季度下降。",
  "reference": "销售额整体逐季度上升。"
}
```

### 10.2 Prompt 处理

BE-A 构造 Prompt 时：

- 保留 `content_markdown` 原文。
- 提取 Markdown 中的图片 URL。
- 如果 Provider 支持多图输入，则将图片作为多模态输入。
- 如果 Provider 不支持多图输入，则将图片 URL 作为文本引用，并要求 AI 在能力不足时降低置信度。

### 10.3 AI 输出示例

```json
{
  "suggestedAnswerJson": {
    "accuracy_score": "1",
    "issue_tags": ["事实错误"],
    "comment": "模型回答称销售额下降，但图文参考显示销售额整体上升。"
  },
  "fieldSuggestions": [
    {
      "field": "accuracy_score",
      "value": "1",
      "confidence": 0.88,
      "reason": "回答方向与图文中的趋势相反。"
    }
  ],
  "riskFlags": [],
  "overallConfidence": 0.88,
  "limitations": []
}
```

---

## 11. 视频预标注设计

### 11.1 MVP 边界

视频题不直接要求 AI 分析完整视频。

MVP 使用以下辅助信息：

- 视频 URL。
- 关键帧图片 URL。
- 视频字幕或转写文本。
- Owner 提供的视频说明。
- 题目 prompt。
- 模型回答。
- 参考答案。

### 11.2 推荐数据结构

```json
{
  "media_type": "video",
  "media_url": "https://example.com/video.mp4",
  "key_frame_urls": [
    "https://example.com/frame-01.jpg",
    "https://example.com/frame-02.jpg"
  ],
  "video_transcript": "画面中用户演示如何安装软件，最后出现安装成功页面。",
  "prompt": "判断模型回答是否正确概括视频内容。",
  "model_answer": "视频主要讲解软件安装流程。",
  "reference": "应覆盖安装步骤和最终安装成功。"
}
```

### 11.3 Prompt 规则

系统消息需明确限制：

```text
如果题目是视频题，你只能基于关键帧、转写文本、人工说明和参考答案生成建议。
如果没有完整视频理解能力，不要声称已经看完整个视频。
必须在 limitations 中说明判断依据。
```

### 11.4 AI 输出示例

```json
{
  "suggestedAnswerJson": {
    "relevance_score": "4",
    "accuracy_score": "4",
    "comment": "基于关键帧和转写文本，模型回答基本覆盖视频主题，但没有提到最终安装成功。"
  },
  "fieldSuggestions": [
    {
      "field": "accuracy_score",
      "value": "4",
      "confidence": 0.68,
      "reason": "判断仅基于关键帧和转写文本，未分析完整视频。"
    }
  ],
  "riskFlags": [],
  "overallConfidence": 0.66,
  "limitations": [
    "未直接分析完整视频，仅基于关键帧、转写文本或人工说明生成建议。"
  ]
}
```

---

## 12. 输出校验规则

### 12.1 JSON 校验

BE-A 必须对 AI 输出进行严格校验：

- 输出必须是 JSON object。
- 必须包含：
  - `suggestedAnswerJson`
  - `fieldSuggestions`
  - `riskFlags`
  - `overallConfidence`
  - `limitations`

### 12.2 字段校验

- `suggestedAnswerJson` 只能包含模板中允许提交的字段。
- `ShowItem` 字段不能出现在输出中。
- `Radio` 字段值必须属于 options。
- `Checkbox` 字段值必须是 options 子集。
- `Input`、`Textarea`、`RichText` 必须是字符串。
- `JsonEditor` 必须是合法 JSON。
- `ImageUpload` / `FileUpload` 必须符合 BE-B 定义的文件引用格式。

### 12.3 异常处理

| 异常 | 处理 |
|---|---|
| 非 JSON 输出 | 预标注失败，记录 AgentRun |
| 字段不存在 | 丢弃字段，记录 ignoredFields |
| enum 非法 | 丢弃字段建议 |
| 图片不可访问 | 返回 `MEDIA_ACCESS_FAILED` |
| Provider 不支持图片 | 返回 `MULTIMODAL_NOT_SUPPORTED` |
| 限流 | 返回 `RATE_LIMITED` |
| 模型低置信度 | 返回 SUCCESS，但标记 limitations |

---

## 13. 与现有能力的关系

### 13.1 与 LlmTrigger 的关系

| 能力 | 粒度 | 触发位置 | 输出 |
|---|---|---|---|
| LlmTrigger | 单字段 | 某个模板组件 | 字段建议 |
| Agent 预标注 | 整题 | 标注页整体 | 完整建议答案 |

两者不冲突。LlmTrigger 更像“局部助手”，Agent 预标注更像“整题初稿生成”。

### 13.2 与 AI 自动预审的关系

| 能力 | 时间 | 目标 |
|---|---|---|
| Agent 预标注 | 提交前 | 帮 Labeler 写初稿 |
| AI 自动预审 | 提交后 | 帮 Reviewer 做审核判断 |

预标注不能替代预审，预审也不能替代人工终审。

### 13.3 与 SupervisorAgent 的关系

MVP 可以先使用直接 LLM 调用。

增强版可复用 `SupervisorAgent`：

```text
SupervisorAgent
  -> query_task_guidelines
  -> verify_format
  -> check_media_context
  -> generate_pre_annotation
```

这样可以在答辩中体现“Agent 不是简单 Prompt，而是可扩展的工具编排架构”。

---

## 14. 权限与审计

### 14.1 权限规则

- Labeler 只能对自己的 assignment 触发预标注。
- Owner 只能在模板预览或任务预览中测试预标注。
- Reviewer 只能查看预标注记录，不能修改预标注。
- Admin 不直接参与预标注业务，只负责 Provider 配置。
- System Agent 不能被普通用户登录。

### 14.2 审计规则

每次预标注需要记录：

- 谁触发。
- 哪个 assignment。
- 哪个 dataset item。
- 哪个 template version。
- 哪个 provider。
- 哪个 model。
- 哪个 agentRun。
- 输入快照。
- 输出摘要。
- 成功或失败原因。

审计动作建议：

```text
AI_PRE_ANNOTATION_RUN
AI_PRE_ANNOTATION_SUCCESS
AI_PRE_ANNOTATION_FAILED
AI_PRE_ANNOTATION_ADOPTED
AI_PRE_ANNOTATION_IGNORED
```

---

## 15. 前端展示设计

### 15.1 Labeler 标注页布局

```text
左侧：题目内容
- prompt
- model_answer / response_a / response_b
- reference
- 图片 / 视频 / Markdown

右侧：标注表单
- AI 预标注按钮
- AI 建议面板
- 字段表单
- 保存草稿
- 提交
```

### 15.2 AI 建议面板

```text
AI 预标注完成
整体置信度：0.86
AgentRun：#9001

建议：
1. 准确性评分：4
   理由：回答主结论正确，但缺少关键条件。
   [采纳]

2. 问题标签：信息缺失
   理由：参考答案中的要点未完全覆盖。
   [采纳]

3. 详细评语：
   模型回答整体可接受，但缺少对关键条件的说明。
   [采纳]
```

### 15.3 失败展示

```text
AI 预标注失败
原因：当前 Provider 不支持图片输入。
建议：请人工标注，或切换支持多模态的模型。
```

### 15.4 Reviewer 审核页

Reviewer 需要看到：

```text
AI 辅助记录
- 预标注 AgentRun：#9001
- 自动预审 AgentRun：#9020
- 预标注整体置信度：0.86
- Labeler 是否采纳 AI 建议
- Labeler 最终提交与 AI 建议的差异
```

---

## 16. BE-B 配套需求

### 16.1 数据集导入

导入时保留：

```text
media_type
media_url
content_markdown
key_frame_urls
video_transcript
owner_media_description
```

### 16.2 对象存储

需要支持：

- 图片上传。
- 视频上传。
- 文件类型校验。
- 文件大小限制。
- 预签名 URL。
- URL 有效期配置。
- 文件元数据查询。

### 16.3 Schema 校验

需要支持：

- `ImageUpload` 输出格式。
- `FileUpload` 输出格式。
- `JsonEditor` 输出格式。
- `ShowItem` 不参与提交。
- 媒体字段只展示，不作为普通答案字段提交。

---

## 17. FE 配套需求

FE 需要实现：

- 图片预览。
- 视频播放。
- Markdown 安全渲染。
- AI 预标注触发按钮。
- AI 建议 Loading 状态。
- 字段级建议展示。
- 字段级采纳。
- 一键采纳。
- 采纳后自动保存草稿。
- Reviewer 查看预标注记录。
- 错误状态展示。

特别注意：

- AI 建议不能自动覆盖用户已填写内容。
- 若字段已有人工输入，采纳时需要二次确认。
- 页面必须明确显示“AI 结果仅供参考”。

---

## 18. 测试方案

### 18.1 BE-A 测试

- Labeler 可以触发自己的 assignment 预标注。
- 非本人 assignment 触发失败。
- Owner 预览模式不产生 submission。
- 预标注成功生成 AgentRun。
- 预标注失败记录错误。
- 限流时返回 `RATE_LIMITED`。
- 图片题 Prompt 包含图片 URL。
- Markdown 题 Prompt 包含 Markdown 正文。
- 视频题 Prompt 包含 limitations。
- AI 输出非法字段会被过滤。
- AI 输出非法 JSON 会失败。
- 预标注不会改变 submission 状态。

### 18.2 FE 联调测试

- 点击 AI 预标注后展示 Loading。
- 成功后展示字段建议。
- 采纳后表单字段变化。
- 不采纳则表单不变。
- 表单已有值时采纳需要确认。
- 图片可正常预览。
- Markdown 可正常渲染。
- 视频可正常播放或打开。
- 错误信息可读。

### 18.3 端到端演示测试

```text
1. Admin 配置支持图片输入的 LLM Provider。
2. Owner 创建 QA 质量标注任务。
3. Owner 导入含图片字段的数据集。
4. Owner 搭建模板。
5. Owner 配置 AI 审核。
6. Owner 发布任务。
7. Labeler 领取图片题。
8. Labeler 点击 AI 预标注。
9. AI 输出评分、标签、评语。
10. Labeler 采纳并修改。
11. Labeler 提交。
12. AI 自动预审。
13. Reviewer 查看媒体、预标注、最终答案、AI 预审。
14. Reviewer 通过。
15. Owner 导出金标数据。
```

---

## 19. 风险与取舍

### 19.1 Provider 不支持多模态

风险：OpenAI-compatible Provider 不一定支持图片输入。

应对：

- Provider 配置中增加能力标记，例如 `supportVision=true`。
- 不支持图片时，前端隐藏图片预标注入口。
- 或降级为文本上下文预标注。

### 19.2 图片 URL 访问失败

风险：

- 预签名 URL 过期。
- 模型侧无法访问内网地址。

应对：

- BE-B 生成外部可访问短期 URL。
- BE-A 在 inputSnapshot 中记录 URL 生成时间。
- 失败时返回 `MEDIA_ACCESS_FAILED`。

### 19.3 AI 输出不可靠

风险：

- AI 可能误判图片。
- AI 可能输出模板外字段。
- AI 可能编造信息。

应对：

- 严格 JSON Schema 校验。
- 置信度展示。
- limitations 展示。
- 必须人工确认。
- Reviewer 可查看 AI 预标注记录。

### 19.4 视频理解复杂

风险：

- 完整视频理解成本高。
- 视频模型支持不稳定。

应对：

- MVP 只做关键帧 / 转写 / 人工说明辅助。
- 明确在 limitations 中说明没有完整分析视频。
- 后续再扩展视频抽帧和 ASR。

---

## 20. 实施优先级建议

### P0：最小可演示闭环

- 新增 Agent 预标注接口。
- 复用现有 LLM Provider、LlmGateway、AgentRun、RateLimit、AuditAppender。
- 支持文本题预标注。
- 支持图片 URL 进入 Prompt。
- 返回结构化 `suggestedAnswerJson` 和 `fieldSuggestions`。
- 前端可展示建议并手动采纳。

### P1：多模态增强

- Provider 增加是否支持视觉输入的配置。
- 图片题使用多模态 message。
- Markdown 提取内嵌图片 URL。
- Reviewer 查看预标注与最终提交差异。

### P2：视频和可观测增强

- 视频关键帧输入。
- 视频 transcript 输入。
- AgentRun 回放页面。
- 预标注采纳率统计。
- SupervisorAgent 工具编排模式。

---

## 21. 答辩亮点总结

本增强方案可以作为答辩中的重点创新点：

1. **从 AI 预审扩展到 AI 预标注**  
   AI 不只是在提交后给 Reviewer 建议，也能在提交前辅助 Labeler 生成初稿。

2. **从纯文本扩展到多模态标注**  
   支持图片、Markdown 图文、视频关键帧等更复杂的数据标注场景。

3. **AI 可追踪、可审计、可回放**  
   每次预标注都对应 AgentRun，保留输入、输出、错误和模型信息。

4. **坚持人机协同边界**  
   AI 只建议，人工确认后才提交；AI 不能终审，也不能生成金标。

5. **与现有课题设计自然衔接**  
   复用 LLM Provider、AgentRun、LlmGateway、RateLimit、AuditAppender、模板 Schema 和数据集字段。

6. **职责边界清楚**  
   BE-A 做智能业务链路，BE-B 做数据资产和存储，FE 做交互展示。

---

## 22. 推荐交付文件

本文档建议作为新增开发计划文件提交：

```text
docs/development-plans/07-BE-A-Agent预标注与多模态标注增强方案.md
```

它不替代原有任务书，而是作为 BE-A 的增强功能方案，用于后续开发拆解、团队协作和课题答辩说明。

---

## 23. 非多模态与多模态方案差异

### 23.1 不做多模态时的实际含义

如果不做多模态能力，预标注和 AI 审核仍然可以处理 `media_type=image/video/markdown` 的题目，但处理方式本质上是**文本化处理**。

也就是说，BE-A 在构造 Prompt 时只会把媒体字段作为普通文本放进去：

```json
{
  "media_type": "image",
  "media_url": "https://example.com/image.jpg",
  "prompt": "请描述这张图片的主要内容。",
  "model_answer": "一条夜间公路伸向远方，天空出现绚丽的极光。",
  "reference": "需指出公路与极光/夜空等关键元素。"
}
```

在这种模式下，LLM 实际看到的是：

- `prompt` 文本。
- `model_answer` 文本。
- `reference` 文本。
- `media_url` 这个字符串。
- `content_markdown` 这个字符串。

LLM 通常**不会真正打开图片或视频链接查看内容**。除非模型厂商自身支持联网拉取 URL，并且 URL 对模型服务端可访问，否则它只能根据链接文本、文件名、上下文或参考答案推测。

因此，不做多模态时的 AI 预标注和 AI 审核能力边界如下：

| 场景 | 能力表现 |
|---|---|
| 文本题 | 可正常预标注和审核 |
| 图片题 | 只能根据题干、回答、参考答案和 URL 字符串判断，不能可靠看图 |
| OCR 题 | 如果 URL 中带 `text=test` 等参数，模型可能猜中，但不是视觉识别 |
| 视频题 | 只能看到视频 URL，不能理解完整视频内容 |
| Markdown 图文 | 可以理解 Markdown 文本，但不能可靠判断内嵌图片/视频内容 |
| 内容安全审核 | 只能审核文本描述，不能真正判断图片或视频画面是否违规 |

这种方案适合快速演示“AI 预标注流程”，但不能宣称平台已经具备真正的图像理解、OCR、视频理解能力。

### 23.2 做多模态后的实际区别

做多模态后，BE-A 不再只是把媒体 URL 当字符串放进 Prompt，而是将图片、关键帧或图文中的媒体资源作为模型可理解的多模态输入。

以图片题为例，Prompt 会从纯文本：

```json
{
  "media_url": "https://example.com/image.jpg"
}
```

升级为多模态消息：

```json
[
  {
    "type": "text",
    "text": "请判断模型回答是否准确描述图片内容，并按模板字段输出 JSON。"
  },
  {
    "type": "image_url",
    "image_url": {
      "url": "https://example.com/image.jpg"
    }
  }
]
```

此时模型看到的不只是 URL 字符串，而是图片视觉内容。它可以基于图片本身判断：

- 图片主体是什么。
- 图片场景是否与模型回答一致。
- 图片是否包含文字。
- 图片是否是占位图或测试图。
- 图片是否包含明显违规、敏感或不适内容。

多模态前后的差异如下：

| 维度 | 不做多模态 | 做多模态 |
|---|---|---|
| 图片输入 | URL 字符串 | 图片视觉内容 |
| OCR | 基本不可靠，可能猜 URL 参数 | 可基于图片内容识别文字 |
| 图像描述 | 依赖参考答案或上下文 | 可看图判断回答是否准确 |
| 图片内容审核 | 只能审核文字描述 | 可判断画面内容是否安全 |
| Markdown 图文 | 只能读 Markdown 文本 | 可读取文本并分析内嵌图片 |
| 视频题 | 只能看到视频 URL | MVP 可分析关键帧和转写文本 |
| AI 结论可信度 | 对媒体题较低 | 对图片/关键帧题更高 |
| 工程要求 | 普通文本 LLM 即可 | Provider、Gateway、Prompt、FE/BE-B 都要配套 |

### 23.3 视频题的特殊边界

即使做多模态，MVP 也不建议直接把完整视频 URL 交给 LLM 并宣称“模型看完整视频”。视频理解涉及：

- 视频文件访问。
- 视频抽帧。
- 时间轴信息。
- 语音转写。
- 多帧时序理解。
- 画面与字幕对齐。

因此，视频题建议采用“关键帧 + 转写 + 元信息”的保守方案：

```json
{
  "media_type": "video",
  "media_url": "https://example.com/video.mp4",
  "key_frame_urls": [
    "https://example.com/frame-001.jpg",
    "https://example.com/frame-002.jpg"
  ],
  "video_transcript": "画面中展示海洋生物和海底场景。",
  "owner_media_description": "该视频为海洋纪录短片。"
}
```

BE-A 在 Prompt 中必须要求 AI 说明判断依据：

```json
{
  "limitations": [
    "未直接分析完整视频，仅基于关键帧、转写文本和题目说明生成建议。"
  ]
}
```

这能避免答辩时被追问“模型是否真的看了完整视频”时边界不清。

---

## 24. BE-A 需要新增或调整的要求

本节只列 BE-A 主责范围内的要求。

### 24.1 Prompt 输入策略要求

BE-A 需要根据 `media_type` 选择不同 Prompt 构造策略：

| media_type | BE-A Prompt 策略 |
|---|---|
| `text` | 传入题目文本、模型回答、参考答案、模板字段 |
| `image` | 传入题目文本、模型回答、参考答案、图片 URL；若 Provider 支持视觉输入，则使用 `image_url` 多模态消息 |
| `markdown` | 传入 Markdown 原文；提取其中图片和视频链接；图片可作为多模态输入，视频作为 URL/说明输入 |
| `video` | 传入视频 URL、关键帧 URL、转写文本、Owner 描述；明确 limitations |

BE-A 需要在 `AgentRun.inputSnapshot` 中记录：

```json
{
  "mediaType": "image",
  "mediaUrl": "https://example.com/image.jpg",
  "contentMarkdown": "",
  "keyFrameUrls": [],
  "videoTranscriptPresent": false,
  "providerVisionEnabled": true,
  "promptMode": "MULTIMODAL_IMAGE"
}
```

这样 Reviewer 和答辩时可以解释：这次 AI 预标注到底是文本判断，还是视觉判断。

### 24.2 Provider 能力判断要求

BE-A 在调用前需要知道当前 Provider 是否支持视觉输入。

建议在 Provider 运行配置或 AI 配置中增加能力标记：

```json
{
  "supportVision": true,
  "supportMultiImage": true,
  "supportVideo": false
}
```

MVP 可以只做：

```text
supportVision: true / false
```

调用策略：

| 条件 | 处理 |
|---|---|
| `media_type=text` | 普通文本调用 |
| `media_type=image` 且 `supportVision=true` | 使用多模态图片输入 |
| `media_type=image` 且 `supportVision=false` | 降级为文本 URL 输入，并在 `limitations` 标记 |
| `media_type=video` | 默认关键帧/转写模式，不宣称完整视频理解 |

### 24.3 多模态 LLM 消息结构要求

现有 LLM 调用如果只支持：

```java
new LlmMessage("user", "文本内容")
```

则多模态扩展需要让用户消息支持 content array。概念结构如下：

```json
{
  "role": "user",
  "content": [
    {
      "type": "text",
      "text": "请根据图片判断回答是否准确，并输出 JSON。"
    },
    {
      "type": "image_url",
      "image_url": {
        "url": "https://example.com/image.jpg"
      }
    }
  ]
}
```

BE-A 需要和基础 LLM Gateway 约定：

- 文本模型继续使用 string content。
- 视觉模型使用 array content。
- 不支持 array content 的 Provider 必须降级或拒绝。

### 24.4 预标注输出限制要求

BE-A 需要在 AI 输出校验中增加媒体相关字段：

```json
{
  "suggestedAnswerJson": {},
  "fieldSuggestions": [],
  "riskFlags": [],
  "overallConfidence": 0.86,
  "limitations": [],
  "mediaUnderstanding": {
    "mode": "VISION_IMAGE",
    "usedMedia": true,
    "usedKeyFrames": false,
    "usedTranscript": false
  }
}
```

字段说明：

| 字段 | 含义 |
|---|---|
| `mode` | `TEXT_ONLY` / `VISION_IMAGE` / `MARKDOWN_TEXT` / `VIDEO_KEY_FRAMES` |
| `usedMedia` | 是否使用了媒体输入 |
| `usedKeyFrames` | 是否使用了视频关键帧 |
| `usedTranscript` | 是否使用了视频转写文本 |

### 24.5 AI 预审也要复用同一媒体上下文

预标注发生在提交前，AI 预审发生在提交后。二者都应该使用一致的媒体上下文构造逻辑。

建议 BE-A 抽象一个内部能力：

```text
MediaPromptContextBuilder
```

职责：

- 从 `itemJson` 提取 `media_type`、`media_url`、`content_markdown`、`key_frame_urls`、`video_transcript`。
- 判断媒体输入模式。
- 生成 Prompt 中的媒体上下文。
- 生成 AgentRun 输入快照中的媒体摘要。

这样预标注和 AI 预审可以共用，避免两条链路对媒体题判断标准不一致。

### 24.6 审计与可解释要求

每次媒体题预标注需要在审计或 AgentRun 中说明：

- 是否启用了多模态。
- 使用了哪些媒体输入。
- 是否发生降级。
- 如果降级，原因是什么。
- AI 输出中是否声明 limitations。

示例：

```json
{
  "action": "AI_PRE_ANNOTATION_SUCCESS",
  "mediaType": "image",
  "promptMode": "VISION_IMAGE",
  "providerVisionEnabled": true,
  "agentRunId": 9001,
  "limitations": []
}
```

降级示例：

```json
{
  "action": "AI_PRE_ANNOTATION_SUCCESS",
  "mediaType": "image",
  "promptMode": "TEXT_WITH_MEDIA_URL",
  "providerVisionEnabled": false,
  "limitations": [
    "当前 Provider 不支持图片输入，本次建议仅基于题目文本、参考答案和媒体 URL 生成。"
  ]
}
```

### 24.7 错误码要求

BE-A 可新增以下错误码语义：

| 错误码 | 场景 |
|---|---|
| `MULTIMODAL_NOT_SUPPORTED` | 当前 Provider 不支持视觉输入且业务要求必须看图 |
| `MEDIA_URL_MISSING` | 媒体题缺少 `media_url` 或 Markdown 内容 |
| `MEDIA_ACCESS_UNVERIFIED` | BE-A 无法确认媒体 URL 对模型可访问 |
| `KEY_FRAME_MISSING` | 视频题缺少关键帧，无法进行视觉辅助判断 |
| `MEDIA_PROMPT_BUILD_FAILED` | 媒体上下文构造失败 |

---

## 25. 其他成员需要配套实现的要求

本节不是 BE-A 主责，但为了完整落地多模态预标注，需要 FE 和 BE-B 配合。实现时应作为跨成员协作事项，不应混入 BE-A 的核心交付范围。

### 25.1 FE 需要实现的事项

#### 25.1.1 标注页媒体展示

FE 需要根据 `media_type` 渲染不同原料：

| media_type | FE 展示要求 |
|---|---|
| `text` | 展示 prompt、model_answer、reference |
| `image` | 展示图片，支持放大预览、加载失败提示 |
| `video` | 展示视频播放器或外链播放入口 |
| `markdown` | 安全渲染 Markdown，展示内嵌图片和视频 |

图片展示要求：

- 支持等比缩放。
- 支持点击放大。
- 加载失败时显示 URL 和错误提示。
- 不让图片撑破标注页面布局。

视频展示要求：

- 支持播放、暂停、进度条。
- 如果浏览器无法播放，提供打开原链接。
- 如果后端提供关键帧，展示关键帧缩略图。

Markdown 展示要求：

- 安全渲染 Markdown。
- 禁止执行任意脚本。
- 图片和视频需要可预览。
- 链接需要新窗口打开。

#### 25.1.2 AI 预标注展示

FE 需要展示 BE-A 返回的媒体理解模式：

```text
AI 预标注完成
理解模式：图片视觉输入
整体置信度：0.91
```

如果发生降级：

```text
AI 预标注完成，但未启用图片理解
原因：当前模型不支持图片输入，本次建议仅基于文本和图片链接生成。
```

#### 25.1.3 采纳交互

FE 需要保证：

- AI 建议不会自动覆盖用户已填写字段。
- 已有人工输入的字段，采纳时需要二次确认。
- 支持逐字段采纳。
- 支持一键采纳空字段。
- 采纳后触发草稿保存。
- 页面明确显示“AI 结果仅供参考”。

### 25.2 BE-B 需要实现的事项

#### 25.2.1 数据集导入保留媒体字段

BE-B 导入数据集时，需要保留以下字段到 `dataset_items.item_json`：

```text
media_type
media_url
content_markdown
key_frame_urls
video_transcript
owner_media_description
```

对于 JSON / JSONL / Excel 三种导入格式，都要保证字段语义一致。

#### 25.2.2 图片和视频对象存储

BE-B 需要提供：

- 图片上传。
- 视频上传。
- 文件类型校验。
- 文件大小限制。
- 文件元数据记录。
- 预签名 URL 生成。
- URL 过期时间配置。

BE-A 只消费 URL，不直接处理文件二进制。

#### 25.2.3 关键帧生成（视频题增强）

如果要把视频题做得更像真正多模态，BE-B 或独立媒体处理模块需要提供关键帧生成能力。

建议输出字段：

```json
{
  "key_frame_urls": [
    "https://example.com/video-001-frame-001.jpg",
    "https://example.com/video-001-frame-002.jpg",
    "https://example.com/video-001-frame-003.jpg"
  ]
}
```

关键帧策略建议：

- 短视频：按固定时间间隔抽 3-5 帧。
- 长视频：按开头、中段、结尾抽帧，或按场景变化抽帧。
- 每帧保留时间戳，方便 Reviewer 对照。

增强结构：

```json
{
  "key_frames": [
    {
      "url": "https://example.com/frame-001.jpg",
      "timestampMs": 0
    },
    {
      "url": "https://example.com/frame-002.jpg",
      "timestampMs": 5000
    }
  ]
}
```

#### 25.2.4 视频转写（可选增强）

如果视频包含语音，BE-B 或独立媒体处理模块可提供转写文本：

```json
{
  "video_transcript": "视频中讲解了软件安装流程，最后展示安装成功页面。"
}
```

BE-A 可以把 transcript 作为文本上下文传给 LLM，但不负责 ASR。

#### 25.2.5 Markdown 资源处理

对于 Markdown 图文题，BE-B 可选提供：

- 提取 Markdown 中的图片 URL。
- 提取 Markdown 中的视频 URL。
- 校验链接是否可访问。
- 将外部资源转存到对象存储。
- 生成安全可访问的预签名 URL。

### 25.3 协作边界总结

| 能力 | BE-A | BE-B | FE |
|---|---|---|---|
| 判断是否需要多模态 Prompt | 负责 | 不负责 | 不负责 |
| 构造 AI 预标注 Prompt | 负责 | 不负责 | 不负责 |
| 调用 LLM 并记录 AgentRun | 负责 | 不负责 | 不负责 |
| 校验 AI 输出字段 | 负责 | 提供 Schema 校验能力 | 展示错误 |
| 图片/视频上传 | 不负责 | 负责 | 调用上传接口 |
| 预签名 URL | 消费 | 负责生成 | 消费展示 |
| 视频关键帧 | 消费 | 负责或由媒体模块负责 | 展示 |
| 视频转写 | 消费 | 负责或由媒体模块负责 | 展示 |
| 图片/视频/Markdown 渲染 | 不负责 | 提供资源 | 负责 |
| AI 建议采纳交互 | 提供接口结果 | 不负责 | 负责 |
| Reviewer 查看 AI 辅助记录 | 提供数据 | 不负责 | 负责展示 |
