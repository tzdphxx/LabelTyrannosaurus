# 审核员工作流接口文档

> 覆盖审核员完整工作流：领取任务 → 审核提交 → AI 预审辅助 → 批量操作

---

## 1. 获取当前可以领取的审核任务

审核员浏览尚未被任何人领取的任务广场，选择任务进行领取。

**GET** `/api/v1/reviewer/submissions?scope=AVAILABLE`

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| scope | String | 是 | 固定传 `AVAILABLE` |
| taskId | Long | 否 | 按任务 ID 筛选 |
| submissionStatus | String | 否 | 按提交状态筛选 |
| aiDecision | String | 否 | 按 AI 结论筛选：PASS / REJECT / MANUAL_REVIEW |
| aiReviewStatus | String | 否 | 按 AI 审核状态筛选 |
| conflictStatus | String | 否 | 按冲突状态筛选 |
| reviewLevel | Integer | 否 | 按审核级别筛选 |
| page | int | 否 | 页码，默认 1 |
| size | int | 否 | 每页条数，默认 20，最大 100 |

**筛选逻辑**：`assigned_reviewer_id IS NULL` 且 `task + reviewLevel` 未被任何审核员在 `review_task_claims` 中领取。

**响应**：
```json
{
  "code": 0,
  "data": {
    "items": [
      {
        "submissionId": 100,
        "taskId": 1,
        "datasetItemId": 10,
        "labelerId": 20,
        "submissionStatus": "PENDING_FINAL",
        "aiReviewStatus": "COMPLETED",
        "aiDecision": "PASS",
        "conflictStatus": null,
        "reviewLevel": 1,
        "assignedReviewerId": null,
        "createdAt": "2026-06-01T10:00:00",
        "updatedAt": "2026-06-01T10:30:00"
      }
    ],
    "page": 1,
    "size": 20,
    "total": 50
  }
}
```

**Swagger Tag**：`审核`

---

## 2. 获取已领取的审核任务

审核员查看自己已领取的任务下所有待审提交。

**GET** `/api/v1/reviewer/submissions?scope=CLAIMED`

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| scope | String | 是 | 固定传 `CLAIMED` |
| taskId | Long | 否 | 按任务 ID 筛选 |
| submissionStatus | String | 否 | 按提交状态筛选 |
| aiDecision | String | 否 | 按 AI 结论筛选 |
| aiReviewStatus | String | 否 | 按 AI 审核状态筛选 |
| conflictStatus | String | 否 | 按冲突状态筛选 |
| reviewLevel | Integer | 否 | 按审核级别筛选 |
| page | int | 否 | 页码，默认 1 |
| size | int | 否 | 每页条数，默认 20，最大 100 |

**筛选逻辑**：`assigned_reviewer_id = {当前审核员 ID}`（该审核员已 claim 的 task+level 下所有待审提交）。

**响应**：同上 `PageResponse<ReviewerSubmissionListItem>`

**Swagger Tag**：`审核`

---

## 3. 获取审核任务详情

查看单个提交的完整审核详情，包含标注答案、AI 评分、审核历史、版本历史。

**GET** `/api/v1/reviewer/submissions/{submissionId}`

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| submissionId | Long | 是 | 路径参数，提交 ID |

**响应**：
```json
{
  "code": 0,
  "data": {
    "submissionId": 100,
    "taskId": 1,
    "assignmentId": 50,
    "datasetItemId": 10,
    "labelerId": 20,
    "versionNo": 1,
    "submissionStatus": "PENDING_FINAL",
    "answerJson": "{\"category\": \"电子产品\"}",
    "itemJson": "{\"text\": \"这个手机很好用\"}",
    "templateVersionId": 5,
    "schemaJson": "{\"type\": \"object\", \"properties\": {...}}",
    "aiReviewResult": {
      "aiReviewResultId": 30,
      "agentRunId": 40,
      "status": "COMPLETED",
      "decision": "PASS",
      "averageScore": "85.50",
      "riskFlags": "[]",
      "suggestion": null,
      "errorCode": null,
      "promptMode": "DEEP_DIMENSION",
      "degraded": false,
      "limitations": null
    },
    "agentRunSummary": {
      "agentRunId": 40,
      "agentType": "AI_REVIEW",
      "modelName": "qwen-max",
      "status": "COMPLETED",
      "startedAt": "2026-06-01T09:55:00",
      "finishedAt": "2026-06-01T09:55:30"
    },
    "reviewRecords": [
      {
        "reviewRecordId": 60,
        "reviewerId": 1,
        "action": "APPROVE",
        "reviewLevel": 1,
        "reason": "标注正确",
        "reviewComment": "无问题",
        "createdAt": "2026-06-01T11:00:00"
      }
    ],
    "versionHistory": [
      {
        "submissionId": 100,
        "versionNo": 1,
        "status": "PENDING_FINAL",
        "isGolden": false,
        "createdAt": "2026-06-01T10:00:00"
      }
    ],
    "latestPreAnnotation": {
      "preAnnotationId": 70,
      "agentRunId": 80,
      "status": "COMPLETED",
      "suggestedAnswerJson": "{\"category\": \"电子产品\"}",
      "fieldSuggestions": null,
      "riskFlags": "[]",
      "overallConfidence": "0.92",
      "limitations": null,
      "promptMode": "STANDARD",
      "degraded": false,
      "ignoredFields": null,
      "mediaUnderstanding": null,
      "finalDiff": "{\"suggestedAnswerJson\": \"...\", \"finalAnswerJson\": \"...\"}"
    }
  }
}
```

> **注意**：已领取和未领取的审核任务详情共用此接口，不区分领取状态。

**Swagger Tag**：`审核`

---

## 4. 已领取的审核任务详情

同接口 #3，不区分领取状态。

**GET** `/api/v1/reviewer/submissions/{submissionId}`

---

## 5. 直接修订当前题目

审核员直接修改标注答案并创建新版本。修订作为通过（approve）的附属动作，传入修订后的答案 JSON 即可。

**POST** `/api/v1/reviewer/submissions/{submissionId}/approve`

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| submissionId | Long | 是 | 路径参数，提交 ID |

**请求体**：
```json
{
  "reviewComment": "修改了分类标签",
  "reviewLevel": 1,
  "revisedAnswerJson": "{\"category\": \"数码产品\", \"sentiment\": \"正面\"}"
}
```

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| reviewComment | String | 否 | 审核备注 |
| reviewLevel | int | 是 | 审核级别，>= 1 |
| revisedAnswerJson | String | 否 | **修订后的答案 JSON 字符串**。传入即触发修订逻辑，创建新版提交后通过；不传则纯通过不修订 |

**修订逻辑**：
1. 对 `revisedAnswerJson` 做标准化（canonicalize）+ SHA256 哈希
2. 如果答案没变（哈希相同），跳过修订
3. 将旧版本置为 `SUPERSEDED`
4. 创建新版提交（versionNo + 1），`createdBy = reviewerId`
5. 新版本状态为 `PENDING_FINAL`
6. 审核通过该新版本

**响应**：
```json
{
  "code": 0,
  "data": {
    "submissionId": 101,
    "submissionStatus": "APPROVED",
    "reviewRecordId": 61
  }
}
```

> **注意**：目前没有独立的"仅修订不通过"接口，修订和通过是绑定操作。

**Swagger Tag**：`审核`

---

## 6. 打回当前题目

审核员驳回提交，标注员需要重新修改。

**POST** `/api/v1/reviewer/submissions/{submissionId}/reject`

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| submissionId | Long | 是 | 路径参数，提交 ID |

**请求体**：
```json
{
  "reason": "分类标签错误",
  "reviewLevel": 1
}
```

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| reason | String | 是 | 驳回原因，必填 |
| reviewLevel | int | 是 | 审核级别，>= 1 |

**响应**：
```json
{
  "code": 0,
  "data": {
    "submissionId": 100,
    "submissionStatus": "REJECTED",
    "reviewRecordId": 62
  }
}
```

**Swagger Tag**：`审核`

---

## 7. 通过当前题目

审核员审核通过提交（不修订答案）。

**POST** `/api/v1/reviewer/submissions/{submissionId}/approve`

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| submissionId | Long | 是 | 路径参数，提交 ID |

**请求体**（纯通过，不修订）：
```json
{
  "reviewComment": "标注正确",
  "reviewLevel": 1
}
```

> 不传 `revisedAnswerJson` 即为纯通过。

**响应**：同 #5 直接修订。

**Swagger Tag**：`审核`

---

## 8. 获取 AI 审核队列

审核员查看自己负责的提交的 AI 预审状态。

**GET** `/api/v1/reviewer/ai-review-status`

无需参数，自动取当前登录审核员的数据。

**筛选逻辑**：
- `submission.status IN ('PENDING_FINAL', 'AI_REVIEWING')`
- 且 `assigned_reviewer_id = {当前审核员}` 或通过 `review_tasks` 关联到当前审核员

**响应**：
```json
{
  "code": 0,
  "data": [
    {
      "submissionId": 100,
      "taskId": 1,
      "taskTitle": "商品分类标注",
      "submissionStatus": "PENDING_FINAL",
      "aiReviewStatus": "COMPLETED",
      "aiDecision": "PASS",
      "averageScore": "85.50",
      "assignedToMe": true,
      "submittedAt": "2026-06-01T10:00:00"
    }
  ]
}
```

| 字段 | 说明 |
|------|------|
| submissionId | 提交 ID |
| taskId | 任务 ID |
| taskTitle | 任务标题 |
| submissionStatus | PENDING_FINAL / AI_REVIEWING |
| aiReviewStatus | AI 审核状态 |
| aiDecision | PASS / REJECT / MANUAL_REVIEW |
| averageScore | AI 平均评分 |
| assignedToMe | 是否已分配给当前审核员 |
| submittedAt | 提交时间 |

> 这是审核员个人视角的 AI 预审状态列表，不是全平台队列。

**Swagger Tag**：`审核员工作台`

---

## 9. 获取 AI 审核队列中某个题目的详情

查看单个提交的 AI 审核完整结果，包含评分维度、原始 Prompt 和响应。

**GET** `/api/v1/submissions/{submissionId}/ai-review`

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| submissionId | Long | 是 | 路径参数，提交 ID |

**响应**：
```json
{
  "code": 0,
  "data": {
    "aiReviewResultId": 30,
    "submissionId": 100,
    "status": "COMPLETED",
    "decision": "PASS",
    "averageScore": "85.50",
    "dimensions": [
      {
        "name": "准确性",
        "score": "90.00",
        "comment": "分类正确"
      }
    ],
    "riskFlags": "[]",
    "suggestion": null,
    "errorCode": null,
    "promptMode": "DEEP_DIMENSION",
    "degraded": false,
    "limitations": null,
    "rawPrompt": "你是一个数据标注审核专家...",
    "rawResponse": "{...}",
    "createdAt": "2026-06-01T09:55:30"
  }
}
```

> **关联数据获取**：
> - **历史答案**：调用 `GET /api/v1/submissions/{submissionId}/versions` 获取版本列表（含 `submissionId`），再逐个调 `GET /api/v1/reviewer/submissions/{submissionId}` 取各版本的 `answerJson`
> - **审核记录**：`GET /api/v1/reviewer/submissions/{submissionId}` 的 `reviewRecords` 字段
> - **Prompt 模板**：`GET /api/v1/tasks/{taskId}/ai-review-configs` 获取任务的 AI 审核配置（含 prompt 模板）

**Swagger Tag**：`AI 审核结果`

---

## 10. 审核失败重试

审核员手动触发 AI 预审重试。

**POST** `/api/v1/submissions/{submissionId}/ai-review/retry`

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| submissionId | Long | 是 | 路径参数，提交 ID |

**请求体**：无

**响应**：
```json
{
  "code": 0,
  "data": {
    "agentRunId": 41,
    "submissionId": 100,
    "status": "PENDING"
  }
}
```

每次重试产生新的 `AgentRun` 记录，可通过 `GET /api/v1/agent-runs/{agentRunId}` 查询运行状态和结果。

**Swagger Tag**：`AI 审核结果`

---

## 11. 批量打回题目

**POST** `/api/v1/reviewer/submissions/batch/reject`

**请求体**：
```json
{
  "submissionIds": [100, 101, 102],
  "reason": "批量驳回-分类错误",
  "reviewLevel": 1
}
```

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| submissionIds | List\<Long\> | 是 | 提交 ID 列表 |
| reason | String | 是 | 批量驳回原因 |
| reviewLevel | int | 是 | 审核级别 |

**响应**：
```json
{
  "code": 0,
  "data": {
    "totalCount": 3,
    "successCount": 3,
    "failCount": 0,
    "results": [
      { "submissionId": 100, "success": true },
      { "submissionId": 101, "success": true },
      { "submissionId": 102, "success": true }
    ]
  }
}
```

**Swagger Tag**：`审核`

---

## 12. 批量通过题目

**POST** `/api/v1/reviewer/submissions/batch/approve`

**请求体**：
```json
{
  "submissionIds": [100, 101, 102],
  "reviewComment": "批量通过",
  "reviewLevel": 1
}
```

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| submissionIds | List\<Long\> | 是 | 提交 ID 列表 |
| reviewComment | String | 否 | 审核备注 |
| reviewLevel | int | 是 | 审核级别 |

> 批量通过不支持附带修订（`revisedAnswerJson`），如需修订请逐个调用 approve 接口。

**响应**：同 #11 批量打回。

**Swagger Tag**：`审核`

---

## 补充接口

### 版本历史与 Diff

| 功能 | 方法 | 路径 | Tag |
|------|------|------|------|
| 版本历史列表 | GET | `/api/v1/submissions/{submissionId}/versions` | 提交追溯 |
| 答案 Diff 对比 | GET | `/api/v1/submissions/{submissionId}/diff?baseVersionNo=1` | 提交追溯 |
| 多版本并排对比 | GET | `/api/v1/submissions/compare?ids=101,102,103` | 提交追溯 |

### 审核领取

| 功能 | 方法 | 路径 | Tag |
|------|------|------|------|
| 领取整任务 | POST | `/api/v1/reviewer/tasks/{taskId}/claim?reviewLevel=1` | 审核领取 |
| 释放整任务 | DELETE | `/api/v1/reviewer/tasks/{taskId}/claim?reviewLevel=1` | 审核领取 |

### 工作台

| 功能 | 方法 | 路径 | Tag |
|------|------|------|------|
| 审核员任务列表 | GET | `/api/v1/reviewer/tasks` | 审核员工作台 |
| 审核员工作统计 | GET | `/api/v1/reviewer/dashboard` | 审核员工作台 |

### 冲突仲裁

| 功能 | 方法 | 路径 | Tag |
|------|------|------|------|
| 冲突组列表 | GET | `/api/v1/reviewer/conflict-groups` | 审核 |
| 冲突组详情 | GET | `/api/v1/reviewer/conflict-groups/{groupId}` | 审核 |
| 解决冲突 | POST | `/api/v1/reviewer/conflict-groups/{groupId}/resolve` | 审核 |

### AI 相关

| 功能 | 方法 | 路径 | Tag |
|------|------|------|------|
| AI 审核日志（任务级） | GET | `/api/v1/tasks/{taskId}/ai-review-logs` | AI 审核日志 |
| Agent 运行详情 | GET | `/api/v1/agent-runs/{agentRunId}` | Agent 运行记录 |

---

## 审核员工作流

```
                    ┌──────────────────────────┐
                    │  浏览任务广场（可领取）      │
                    │  GET /submissions         │
                    │  ?scope=AVAILABLE         │
                    └──────────┬───────────────┘
                               │
                    ┌──────────▼───────────────┐
                    │  领取整任务                │
                    │  POST /tasks/{id}/claim   │
                    └──────────┬───────────────┘
                               │
                    ┌──────────▼───────────────┐
                    │  查看已领取列表            │
                    │  GET /submissions         │
                    │  ?scope=CLAIMED           │
                    └──────────┬───────────────┘
                               │
                    ┌──────────▼───────────────┐
                    │  查看审核详情              │
                    │  GET /submissions/{id}    │
                    │  ← answerJson + AI结果    │
                    └──────────┬───────────────┘
                               │
               ┌───────────────┼───────────────┐
               │               │               │
       ┌───────▼──────┐ ┌──────▼──────┐ ┌──────▼──────┐
       │  通过         │ │  通过+修订   │ │  打回        │
       │  POST approve │ │  POST       │ │  POST reject │
       │  (不传        │ │  approve    │ │              │
       │  revisedJson) │ │  (传        │ │              │
       │               │ │  revisedJson)│ │              │
       └───────────────┘ └─────────────┘ └──────────────┘
```
