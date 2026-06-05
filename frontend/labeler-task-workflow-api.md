# 标注员任务工作流接口对接文档

## 1. 概览

本文档覆盖标注员从任务广场、领取题目、读取模板、保存草稿、获取 LLM 建议到提交答案的完整接口链路。

统一响应结构：

```json
{
  "code": 0,
  "message": "OK",
  "data": {},
  "traceId": null
}
```

除任务广场列表外，本工作流接口均要求当前用户具备 `LABELER` 角色。非标注员调用会返回业务错误 `403001 Forbidden`。

---

## 2. 接口清单

| 功能 | 状态 | 方法 | 路径 |
|------|------|------|------|
| 获取当前任务广场的所有任务 | 已有 | GET | `/api/v1/market/tasks` |
| 获取标注员已领取的任务 | 已有 | GET | `/api/v1/claims` |
| 获取当前任务详情，包含题目详情 | 新增 | GET | `/api/v1/labeler/tasks/{taskId}/detail` |
| 获取任务的答题模板 | 新增 | GET | `/api/v1/labeler/tasks/{taskId}/answer-template` |
| 读取当前题目的草稿 | 已有 | GET | `/api/v1/claims/{claimId}/draft` |
| 保存标注的草稿 | 已有 | PUT | `/api/v1/claims/{claimId}/draft` |
| 标注员领取任务 | 已有 | POST | `/api/v1/tasks/{taskId}/items/claim` |
| 获取 LLM 清洗建议 | 已有 | POST | `/api/v1/assignments/{assignmentId}/llm-triggers` |
| 标注员提交答案 | 已有 | POST | `/api/v1/claims/{claimId}/submit` |
| 获取标注员已分配的题目 | 已有 | GET | `/api/v1/labeler/assignments` |

---

## 3. 获取当前任务广场的所有任务

标注员进入任务广场时调用，获取当前可领取任务。

**GET** `/api/v1/market/tasks`

### 查询参数

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| keyword | String | 否 | 按标题或描述模糊搜索 |
| tag | String | 否 | 按任务标签筛选 |
| status | String | 否 | 任务状态；传入非 `PUBLISHED` 时返回空列表 |

### 响应字段

`data[]` 为任务列表，单项主要字段：

| 字段 | 类型 | 说明 |
|------|------|------|
| task | Object | 任务摘要 |
| availableCount | Integer | 当前可领取题目数 |
| currentUserClaimedCount | Integer | 当前用户已领取数 |
| rewardSummary | Object | 奖励摘要 |
| description | String | 任务描述 |
| instructionRichText | String | 标注说明 |
| itemsPreview | Array | 可领取题目预览 |

### 响应示例

```json
{
  "code": 0,
  "message": "OK",
  "data": [
    {
      "task": {
        "taskId": 10,
        "title": "图片质量审核",
        "status": "PUBLISHED",
        "tags": ["image"],
        "quota": 100,
        "claimedCount": 20,
        "overlapCount": 1,
        "strategy": "FCFS",
        "deadlineAt": "2026-06-10T10:00:00"
      },
      "availableCount": 80,
      "currentUserClaimedCount": 2,
      "rewardSummary": {
        "rewardMode": "PIECE",
        "unitReward": 0.20,
        "rewardCurrency": "CNY"
      },
      "description": "任务说明",
      "instructionRichText": "<p>请认真标注</p>",
      "itemsPreview": [
        {
          "itemId": 100,
          "externalId": "q-100",
          "itemJson": "{\"text\":\"hello\"}",
          "metadataJson": "{\"source\":\"import\"}"
        }
      ]
    }
  ],
  "traceId": null
}
```

---

## 4. 获取标注员已领取的任务

标注员查看自己领取过的任务，按任务聚合返回题目列表。

**GET** `/api/v1/claims`

### 查询参数

| 参数 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|--------|------|
| taskId | Long | 否 | - | 按任务 ID 筛选；传入时返回该任务详情 |
| status | String | 否 | - | 按领取状态筛选 |
| page | int | 否 | `1` | 页码 |
| size | int | 否 | `20` | 每页数量，最大 100 |

### 响应字段

`data[]` 为已领取任务列表，单项主要字段：

| 字段 | 类型 | 说明 |
|------|------|------|
| task | Object | 任务摘要 |
| myClaimedCount | Integer | 当前用户在该任务下已领取数量 |
| mySubmittedCount | Integer | 当前用户在该任务下已提交数量 |
| myApprovedCount | Integer | 当前用户在该任务下已通过数量 |
| items | Array | 已领取题目列表 |

`items[]` 字段：

| 字段 | 类型 | 说明 |
|------|------|------|
| claimId | Long | 领取 ID，也就是后续草稿/提交接口的路径参数 |
| itemId | Long | 题目 ID |
| externalId | String | 题目业务编号 |
| claimStatus | String | 领取状态 |
| itemJson | String | 题目内容 JSON |
| metadataJson | String | 题目元数据 JSON |
| draftVersion | Integer | 当前草稿版本 |
| latestSubmissionStatus | String | 最新提交状态 |
| updatedAt | String | 更新时间 |

> 说明：`task` 摘要中的 `tags`、`strategy`、`claimedCount` 以及 `publishedAt`/`endedAt`/`createdAt` 均为该任务的真实值；`mySubmittedCount`、`myApprovedCount` 按当前用户在该任务下的 assignment 状态实时聚合。

### 响应示例

```json
{
  "code": 0,
  "message": "OK",
  "data": [
    {
      "task": {
        "taskId": 10,
        "title": "图片质量审核",
        "status": "PUBLISHED",
        "tags": ["image", "quality"],
        "quota": 100,
        "claimedCount": 20,
        "overlapCount": 1,
        "strategy": "FCFS",
        "deadlineAt": "2026-06-10T10:00:00",
        "publishedAt": "2026-06-01T09:00:00",
        "endedAt": null,
        "createdAt": "2026-05-30T15:00:00",
        "updatedAt": "2026-06-05T18:30:00"
      },
      "myClaimedCount": 5,
      "mySubmittedCount": 3,
      "myApprovedCount": 2,
      "items": [
        {
          "claimId": 500,
          "itemId": 100,
          "externalId": "q-100",
          "claimStatus": "SUBMITTED",
          "itemJson": "{\"text\":\"hello\"}",
          "metadataJson": "{\"source\":\"import\"}",
          "draftVersion": 2,
          "latestSubmissionStatus": "AI_REVIEWING",
          "updatedAt": "2026-06-05T18:30:00"
        }
      ]
    }
  ],
  "traceId": null
}
```

---

## 5. 获取当前任务详情，包含题目详情

本次新增。用于标注员打开某个任务后，读取任务详情和当前可领取题目详情。

**GET** `/api/v1/labeler/tasks/{taskId}/detail`

### 路径参数

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| taskId | Long | 是 | 任务 ID |

### 查询参数

| 参数 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|--------|------|
| itemPage | int | 否 | `1` | 题目页码，小于 1 时按 1 处理 |
| itemSize | int | 否 | `20` | 题目每页数量，最大 100 |

### 响应字段

| 字段 | 类型 | 说明 |
|------|------|------|
| task | Object | 任务摘要 |
| description | String | 任务描述 |
| instructionRichText | String | 富文本标注说明 |
| templateVersionId | Long | 当前发布模板版本 ID |
| availableCount | Integer | 当前可领取题目数 |
| currentUserClaimedCount | Integer | 当前用户已领取数 |
| rewardSummary | Object | 奖励摘要 |
| items | Array | 当前页可领取题目详情 |

### 响应示例

```json
{
  "code": 0,
  "message": "OK",
  "data": {
    "task": {
      "taskId": 10,
      "title": "图片质量审核",
      "status": "PUBLISHED",
      "tags": ["image"],
      "quota": 100,
      "claimedCount": 20,
      "overlapCount": 1,
      "strategy": "FCFS",
      "deadlineAt": "2026-06-10T10:00:00"
    },
    "description": "任务说明",
    "instructionRichText": "<p>请认真标注</p>",
    "templateVersionId": 40,
    "availableCount": 80,
    "currentUserClaimedCount": 2,
    "rewardSummary": {
      "rewardMode": "PIECE",
      "unitReward": 0.20,
      "rewardCurrency": "CNY"
    },
    "items": [
      {
        "itemId": 100,
        "externalId": "q-100",
        "itemJson": "{\"text\":\"hello\"}",
        "metadataJson": "{\"source\":\"import\"}"
      }
    ]
  },
  "traceId": null
}
```

---

## 6. 获取任务的答题模板

本次新增。用于标注员读取任务当前发布模板的 schema。

**GET** `/api/v1/labeler/tasks/{taskId}/answer-template`

### 路径参数

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| taskId | Long | 是 | 任务 ID |

### 响应字段

| 字段 | 类型 | 说明 |
|------|------|------|
| taskId | Long | 任务 ID |
| templateVersionId | Long | 当前发布模板版本 ID |
| schemaJson | String | 答题模板 JSON Schema |

### 响应示例

```json
{
  "code": 0,
  "message": "OK",
  "data": {
    "taskId": 10,
    "templateVersionId": 40,
    "schemaJson": "{\"type\":\"object\",\"properties\":{\"label\":{\"type\":\"string\"}}}"
  },
  "traceId": null
}
```

---

## 7. 读取当前题目的草稿

标注员进入已领取题目作答页时调用。

**GET** `/api/v1/claims/{claimId}/draft`

### 路径参数

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| claimId | Long | 是 | 领取 ID |

### 响应字段

| 字段 | 类型 | 说明 |
|------|------|------|
| assignmentId | Long | 领取 ID |
| draftAnswerJson | String | 草稿答案 JSON |
| draftVersion | Integer | 草稿版本，保存和提交时需要传回 |
| status | String | 当前领取状态 |
| updatedAt | String | 更新时间 |

---

## 8. 保存标注的草稿

标注员保存当前题目的草稿。

**PUT** `/api/v1/claims/{claimId}/draft`

### 请求体

```json
{
  "answerJson": "{\"label\":\"cat\"}",
  "clientVersion": 1
}
```

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| answerJson | String | 是 | 草稿答案 JSON 字符串 |
| clientVersion | Integer | 是 | 客户端持有的草稿版本，用于乐观锁 |

### 响应说明

保存成功后返回新的 `draftVersion`。前端后续保存或提交必须使用最新版本。

---

## 9. 标注员领取任务

标注员从任务中领取一个可标注题目。

**POST** `/api/v1/tasks/{taskId}/items/claim`

### 路径参数

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| taskId | Long | 是 | 任务 ID |

### 响应字段

| 字段 | 类型 | 说明 |
|------|------|------|
| assignmentId | Long | 领取 ID |
| datasetItemId | Long | 题目 ID |
| templateVersionId | Long | 模板版本 ID |
| schemaJson | String | 答题模板 schema |
| itemJson | String | 题目内容 JSON |
| draftAnswerJson | String | 初始草稿，通常为 `null` |
| draftVersion | Integer | 初始草稿版本 |

---

## 10. 获取 LLM 清洗建议

标注员作答过程中触发 LLM 建议。

**POST** `/api/v1/assignments/{assignmentId}/llm-triggers`

### 路径参数

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| assignmentId | Long | 是 | 领取 ID |

### 请求体

```json
{
  "componentId": "summary",
  "currentAnswerJson": {
    "label": "cat"
  },
  "userInstruction": "请帮我检查答案是否规范"
}
```

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| componentId | String | 否 | 被点击的模板组件 ID |
| currentAnswerJson | Object | 否 | 当前草稿答案 |
| userInstruction | String | 否 | 用户补充指令 |

### 响应字段

| 字段 | 类型 | 说明 |
|------|------|------|
| triggerRunId | Long | LLM 触发运行 ID |
| agentRunId | Long | Agent 运行 ID |
| componentId | String | 组件 ID |
| suggestionJson | Object | 建议结果 |
| patch | Object | 建议 patch |
| displayText | String | 展示文案 |
| targetFields | Array | 目标字段 |
| confidence | Decimal | 置信度 |
| warnings | Array | 警告信息 |
| status | String/Object | 运行状态 |
| errorCode | String | 错误码 |
| errorMessage | String | 错误信息 |

---

## 11. 标注员提交答案

标注员提交当前题目的最终答案。

**POST** `/api/v1/claims/{claimId}/submit`

### 请求体

```json
{
  "answerJson": "{\"label\":\"cat\"}",
  "clientVersion": 2
}
```

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| answerJson | String | 是 | 最终答案 JSON 字符串 |
| clientVersion | Integer | 是 | 客户端持有的草稿版本 |

### 响应说明

提交成功后生成 submission，并进入 AI 预审流程。前端应刷新该题目的领取详情或提交记录。

---

## 12. 获取标注员已分配的题目

可选接口。用于平铺查看当前标注员的 assignment 列表。

**GET** `/api/v1/labeler/assignments`

### 查询参数

| 参数 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|--------|------|
| taskId | Long | 否 | - | 按任务筛选 |
| status | String | 否 | - | 按 assignment 状态筛选 |
| page | int | 否 | `1` | 页码 |
| size | int | 否 | `20` | 每页数量，最大 100 |

### 响应字段

`data[]` 单项主要字段：

| 字段 | 类型 | 说明 |
|------|------|------|
| assignmentId | Long | 领取 ID |
| taskId | Long | 任务 ID |
| taskTitle | String | 任务标题 |
| datasetItemId | Long | 题目 ID |
| status | String | assignment 状态 |
| draftVersion | Integer | 草稿版本 |
| claimedAt | String | 领取时间 |
| returnedAt | String | 打回时间 |
| updatedAt | String | 更新时间 |

---

## 13. 推荐前端调用顺序

1. 进入任务广场：调用 `GET /api/v1/market/tasks`。
2. 打开任务详情：调用 `GET /api/v1/labeler/tasks/{taskId}/detail`。
3. 如需单独加载模板：调用 `GET /api/v1/labeler/tasks/{taskId}/answer-template`。
4. 领取题目：调用 `POST /api/v1/tasks/{taskId}/items/claim`。
5. 打开作答页：使用领取响应中的 `assignmentId/schemaJson/itemJson/draftVersion`。
6. 读取草稿：调用 `GET /api/v1/claims/{claimId}/draft`。
7. 保存草稿：调用 `PUT /api/v1/claims/{claimId}/draft`。
8. 获取 LLM 建议：调用 `POST /api/v1/assignments/{assignmentId}/llm-triggers`。
9. 提交答案：调用 `POST /api/v1/claims/{claimId}/submit`。
10. 查看已领取/已分配：调用 `GET /api/v1/claims` 或 `GET /api/v1/labeler/assignments`。

---

## 14. 注意事项

- 草稿保存和提交都需要传 `clientVersion`，否则会被后端拒绝。
- 前端每次保存草稿成功后，应使用后端返回的新 `draftVersion`。
- 任务详情接口中的 `items` 是分页可领取题目，不是一次性返回全量题目。
- 如果任务已过期、未发布或不存在，标注员任务详情和模板接口会返回 `404501`。
- 如果任务没有发布模板，答题模板接口会返回 `404502`。
