# LabelHub 前端接口对接文档

> 版本: v2.0  
> 日期: 2026-06-04  
> 说明: 重构后的接口定义，供前端对接使用

---

## 基础信息

- Base URL: `/api/v1`
- 统一响应: `{ "code": 0, "message": "OK", "data": {...}, "traceId": null }`
- 分页响应: `{ "items": [...], "page": 1, "pageSize": 20, "total": 100 }`
- 认证: JWT Bearer Token，Header `Authorization: Bearer <token>`

---

## 一、Owner — 任务管理

### 1.1 我的任务列表

```
GET /api/v1/owner/tasks
权限: OWNER
```

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| status | string | 否 | DRAFT / PUBLISHED / PAUSED / ENDED |
| keyword | string | 否 | 标题或描述搜索 |
| page | int | 否 | 页码，默认 1 |
| size | int | 否 | 每页条数，默认 20，最大 100 |

响应: `PageResponse<TaskSummaryResponse>`

```json
{
  "items": [{
    "taskId": 100,
    "title": "图像分类标注任务",
    "status": "PUBLISHED",
    "tags": ["image", "classification"],
    "quota": 100,
    "claimedCount": 45,
    "overlapCount": 1,
    "strategy": "FCFS",
    "deadlineAt": "2026-06-30T23:59:59",
    "publishedAt": "2026-06-01T10:00:00",
    "endedAt": null,
    "createdAt": "2026-06-01T09:00:00",
    "updatedAt": "2026-06-03T14:30:00"
  }],
  "page": 1,
  "pageSize": 20,
  "total": 5
}
```

### 1.2 创建任务

创建 `ASSIGNED` 策略任务前，可先调用 `GET /api/v1/owner/labelers/assignable` 获取可选标注员，并将返回的 `labelerId` 填入 `assignedLabelerId`。

```
POST /api/v1/tasks
权限: OWNER
```

请求体 `CreateTaskRequest`:

```json
{
  "title": "图像分类标注任务",
  "description": "对商品图片进行类别标注",
  "instructionRichText": "<p>标注说明...</p>",
  "tags": ["image", "classification"],
  "quota": 100,
  "deadlineAt": "2026-06-30T23:59:59",
  "overlapCount": 1,
  "strategy": "FCFS",
  "maxClaimsPerLabeler": 10,
  "assignedLabelerId": null,
  "publishedTemplateVersionId": 20,
  "aiReviewConfigId": 30,
  "reviewLevelCount": 1,
  "datasetFileId": 99,
  "rewardRule": {
    "unitReward": 0.50,
    "rewardCurrency": "POINT",
    "rewardVisible": true
  }
}
```

响应 `CreateTaskResponse`:

```json
{
  "taskId": 100,
  "status": "DRAFT",
  "datasetImportJob": { "jobId": 300, "status": "PENDING", "totalCount": 0, "successCount": 0, "failedCount": 0 },
  "rewardRule": { "id": 500, "taskId": 100, "effectiveVersion": 1, "rewardMode": "APPROVED_ITEM", "unitReward": 0.50, "rewardCurrency": "POINT", "rewardVisible": true }
}
```

### 1.3 任务详情

```
GET /api/v1/tasks/{taskId}
权限: OWNER
```

响应 `TaskResponse`:

```json
{
  "taskId": 100,
  "title": "图像分类标注任务",
  "status": "PUBLISHED",
  "tags": ["image"],
  "quota": 100,
  "claimedCount": 45,
  "overlapCount": 1,
  "strategy": "FCFS",
  "deadlineAt": "2026-06-30T23:59:59",
  "publishedAt": "2026-06-01T10:00:00",
  "endedAt": null,
  "createdAt": "2026-06-01T09:00:00",
  "updatedAt": "2026-06-03T14:30:00",
  "ownerId": 10,
  "description": "对商品图片进行类别标注",
  "instructionRichText": "<p>标注说明...</p>",
  "maxClaimsPerLabeler": 10,
  "assignedLabelerId": null,
  "publishedTemplateVersionId": 20,
  "aiReview": {
    "id": 30,
    "taskId": 100,
    "providerId": 8,
    "modelName": "qwen-plus",
    "promptTemplate": "Review prompt",
    "scoringDimensions": ["accuracy"],
    "passThreshold": 0.80,
    "manualReviewThreshold": 0.60,
    "promptVersion": "v1",
    "maxRetry": 3,
    "aiFlowPolicy": "MANUAL_FIRST",
    "reviewStrategy": "LIGHTWEIGHT"
  },
  "reviewLevelCount": 1,
  "rewardVisible": true,
  "rewardRule": { "id": 500, "taskId": 100, "effectiveVersion": 1, "rewardMode": "APPROVED_ITEM", "unitReward": 0.50 }
}
```

### 1.4 编辑草稿 / 删除草稿

```
PUT  /api/v1/tasks/{taskId}    # 编辑（仅 DRAFT）
DELETE /api/v1/tasks/{taskId}  # 删除（仅 DRAFT）
权限: OWNER
```

### 1.5 任务生命周期

```
POST /api/v1/tasks/{taskId}/publish   # 发布 DRAFT → PUBLISHED
POST /api/v1/tasks/{taskId}/pause     # 暂停 PUBLISHED → PAUSED
POST /api/v1/tasks/{taskId}/resume    # 恢复 PAUSED → PUBLISHED
POST /api/v1/tasks/{taskId}/end       # 结束 PUBLISHED/PAUSED → ENDED
权限: OWNER
```

响应 `TaskStatusResponse`:

```json
{ "taskId": 100, "status": "PUBLISHED" }
```

### 1.6 任务统计

```
GET /api/v1/tasks/{taskId}/statistics
权限: OWNER
```

```json
{
  "taskId": 100,
  "totalItems": 1280,
  "claimedCount": 960,
  "submittedCount": 640,
  "approvedCount": 420,
  "rejectedCount": 40,
  "pendingReviewCount": 180,
  "passRate": "91.30%"
}
```

### 1.7 标注员列表

```
GET /api/v1/tasks/{taskId}/labelers
权限: OWNER
```

```json
[{
  "labelerId": 200,
  "username": "labeler01",
  "displayName": "张标注",
  "claimedCount": 50,
  "submittedCount": 45,
  "approvedCount": 40,
  "rejectedCount": 3,
  "cancelledCount": 2,
  "firstClaimedAt": "2026-06-01T10:30:00",
  "lastActivityAt": "2026-06-03T16:00:00"
}]
```

---

## 二、Owner — 题目管理 & 导入导出

### 2.1 题目列表

```
GET /api/v1/tasks/{taskId}/dataset/items
权限: ADMIN / OWNER
```

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| page | int | 否 | 页码，默认 1 |
| pageSize | int | 否 | 每页条数，默认 20，最大 100 |
| externalId | string | 否 | 按业务编号搜索 |

响应 `PageResponse<ItemResponse>`:

```json
{
  "items": [{
    "itemId": 100,
    "taskId": 10,
    "externalId": "q1",
    "itemJson": { "question": "示例问题" },
    "metadataJson": { "source": "manual" },
    "assignedCount": 1,
    "submittedCount": 1,
    "approvedCount": 0,
    "itemStatus": "SUBMITTED",
    "labelerId": 200,
    "createdAt": "2026-06-01T09:00:00",
    "updatedAt": "2026-06-03T15:00:00"
  }],
  "page": 1,
  "pageSize": 20,
  "total": 1280
}
```

ItemStatus 取值: `UNCLAIMED` | `CLAIMED` | `DRAFT` | `SUBMITTED` | `RETURNED` | `APPROVED`

### 2.2 批量操作题目

```
POST /api/v1/tasks/{taskId}/dataset/items/batch-append   # 批量追加（传 List）
POST /api/v1/tasks/{taskId}/dataset/items/batch-update   # 批量更新（传 List，按 itemId 修改）
POST /api/v1/tasks/{taskId}/dataset/items/batch-delete   # 批量删除（传 List<itemId>）
权限: ADMIN / OWNER
```

**batch-append** 请求体:

```json
{
  "items": [
    { "externalId": "q1", "itemJson": { "question": "问题1" }, "metadataJson": { "source": "manual" } },
    { "externalId": "q2", "itemJson": { "question": "问题2" }, "metadataJson": {} }
  ]
}
```

**batch-update** 请求体:

```json
{
  "items": [
    { "itemId": 100, "itemJson": { "question": "更新后的问题" }, "metadataJson": {} }
  ]
}
```

**batch-delete** 请求体:

```json
{
  "itemIds": [100, 101, 102]
}
```

响应 `List<BatchItemResult>`:

```json
[
  { "itemId": 100, "externalId": "q1", "success": true, "errorCode": null, "errorMessage": null },
  { "itemId": null, "externalId": "q2", "success": false, "errorCode": "400102", "errorMessage": "externalId already exists" }
]
```

### 2.3 导入

```
POST /api/v1/tasks/{taskId}/imports           # 追加导入（传 fileId）
POST /api/v1/tasks/{taskId}/imports/overwrite  # 覆盖导入（仅 DRAFT，传 fileId）
GET  /api/v1/tasks/{taskId}/imports/{jobId}    # 查询导入状态
权限: ADMIN / OWNER
```

请求体:

```json
{ "fileId": 99 }
```

响应 `DatasetImportJobResponse`:

```json
{
  "jobId": 300,
  "taskId": 10,
  "status": "PARTIAL_SUCCESS",
  "importMode": "APPEND",
  "totalCount": 100,
  "successCount": 98,
  "failedCount": 2,
  "errorReportFileId": 120,
  "errorReportUrl": "https://cos.example.com/signed-url",
  "errorMessage": null,
  "startedAt": "2026-06-01T10:00:00",
  "finishedAt": "2026-06-01T10:00:05",
  "createdAt": "2026-06-01T09:59:59"
}
```

ImportStatus: `PENDING` | `RUNNING` | `SUCCESS` | `FAILED` | `PARTIAL_SUCCESS`

### 2.4 导出

```
POST /api/v1/tasks/{taskId}/exports             # 创建导出任务
GET  /api/v1/tasks/{taskId}/exports             # 导出历史
GET  /api/v1/tasks/{taskId}/exports/{jobId}     # 导出详情/下载地址
权限: ADMIN / OWNER
```

请求体:

```json
{
  "exportFormat": "JSONL",
  "includeAiReview": true,
  "includeAuditTrail": true,
  "includeReviewComment": true,
  "includeLabelerInfo": false
}
```

---

## 三、Labeler — 任务市场 & 领取

### 3.1 任务市场

```
GET /api/v1/market/tasks
GET /api/v1/market/tasks/{taskId}?itemPage=1&itemSize=20
权限: LABELER
```

响应 `List<TaskMarketResponse>` 或单个 `TaskMarketResponse`:

```json
{
  "task": {
    "taskId": 100,
    "title": "图像分类标注任务",
    "status": "PUBLISHED",
    "tags": ["image"],
    "quota": 100,
    "claimedCount": 45,
    "overlapCount": 1,
    "strategy": "FCFS",
    "deadlineAt": "2026-06-30T23:59:59",
    "publishedAt": "2026-06-01T10:00:00",
    "endedAt": null,
    "createdAt": "2026-06-01T09:00:00",
    "updatedAt": "2026-06-03T14:30:00"
  },
  "availableCount": 55,
  "currentUserClaimedCount": 3,
  "rewardSummary": { "unitReward": 0.50, "currency": "POINT", "description": "按通过审核的提交结算" },
  "description": "对商品图片进行类别标注",
  "instructionRichText": "<p>标注说明...</p>",
  "itemsPreview": [
    { "itemId": 100, "externalId": "q1", "itemJson": "{\"question\":\"...\"}", "metadataJson": "{}" }
  ]
}
```

### 3.2 领取题目

```
POST /api/v1/tasks/{taskId}/items/claim
权限: LABELER
```

响应 `AssignmentClaimResponse`:

```json
{
  "assignmentId": 500,
  "taskId": 100,
  "datasetItemId": 100,
  "status": "CLAIMED",
  "itemJson": "{\"question\":\"...\"}",
  "metadataJson": "{}",
  "templateVersionId": 20,
  "claimedAt": "2026-06-03T15:30:00"
}
```

### 3.3 我的领取列表

```
GET /api/v1/claims?taskId=100&status=CLAIMED&page=1&size=20
权限: LABELER
```

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| taskId | long | 否 | 按任务筛选，不传返回所有任务的领取 |
| status | string | 否 | CLAIMED / DRAFTING / SUBMITTED / RETURNED / APPROVED |
| page | int | 否 | 页码 |
| size | int | 否 | 每页条数 |

响应 `List<ClaimedTaskResponse>`（不传taskId时）或单个 `ClaimedTaskResponse`（传taskId时）:

```json
{
  "task": {
    "taskId": 100,
    "title": "图像分类标注任务",
    "status": "PUBLISHED",
    "tags": [],
    "quota": 100,
    "claimedCount": 45,
    "overlapCount": 1,
    "strategy": "FCFS",
    "deadlineAt": "2026-06-30T23:59:59",
    "publishedAt": "2026-06-01T10:00:00",
    "endedAt": null,
    "createdAt": "2026-06-01T09:00:00",
    "updatedAt": "2026-06-03T14:30:00"
  },
  "myClaimedCount": 5,
  "mySubmittedCount": 3,
  "myApprovedCount": 2,
  "items": [
    {
      "claimId": 500,
      "itemId": 100,
      "externalId": "q1",
      "claimStatus": "CLAIMED",
      "itemJson": "{\"question\":\"...\"}",
      "metadataJson": "{}",
      "draftVersion": 3,
      "latestSubmissionStatus": null,
      "updatedAt": "2026-06-03T15:30:00"
    }
  ]
}
```

### 3.4 领取详情

```
GET /api/v1/claims/{claimId}
权限: LABELER
```

响应 `AssignmentDetailResponse` — 包含完整题目数据、模板schema、草稿、提交状态。

### 3.5 保存草稿 / 读取草稿

```
PUT  /api/v1/claims/{claimId}/draft   # 保存草稿
GET  /api/v1/claims/{claimId}/draft   # 读取草稿
权限: LABELER
```

请求体（保存草稿）:

```json
{
  "answerJson": { "category": "电子产品", "confidence": "high" },
  "draftVersion": 3
}
```

### 3.6 提交标注

```
POST /api/v1/claims/{claimId}/submit
权限: LABELER
```

请求体:

```json
{
  "answerJson": { "category": "电子产品", "confidence": "high" }
}
```

响应 `SubmissionSubmitResponse`:

```json
{
  "submissionId": 700,
  "status": "AI_REVIEWING",
  "submittedAt": "2026-06-03T16:00:00"
}
```

### 3.7 我的提交历史

```
GET /api/v1/labeler/submissions?taskId=100&submissionStatus=APPROVED&page=1&size=20
权限: LABELER
```

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| taskId | long | 否 | 按任务筛选 |
| submissionStatus | string | 否 | AI_REVIEWING / PENDING_FINAL / APPROVED / REJECTED |
| assignmentStatus | string | 否 | CLAIMED / SUBMITTED / RETURNED / APPROVED |
| page | int | 否 | 页码 |
| size | int | 否 | 每页条数 |

---

## 四、Reviewer — 审核

### 4.1 工作台

```
GET /api/v1/reviewer/tasks              # 待审任务导航
GET /api/v1/reviewer/dashboard          # 工作统计概览
GET /api/v1/reviewer/ai-review-status   # AI预审状态总览
权限: REVIEWER
```

`GET /reviewer/tasks` 响应:

```json
[{
  "taskId": 100,
  "taskTitle": "图像分类标注任务",
  "pendingCount": 180,
  "myPendingCount": 45,
  "totalReviewedCount": 120,
  "claimed": true,
  "claimedByMe": true
}]
```

`GET /reviewer/dashboard` 响应:

```json
{
  "pendingCount": 180,
  "todayReviewedCount": 15,
  "totalApprovedCount": 420,
  "totalRejectedCount": 40,
  "approvalRate": 91.30
}
```

### 4.2 领取整任务审核权

```
POST   /api/v1/reviewer/tasks/{taskId}/claims?reviewLevel=1
DELETE /api/v1/reviewer/tasks/{taskId}/claims?reviewLevel=1
权限: REVIEWER
```

响应:

```json
{
  "taskId": 100,
  "reviewLevel": 1,
  "claimedSubmissionCount": 25
}
```

### 4.3 审核广场

```
GET /api/v1/reviewer/submissions?scope=AVAILABLE    # 可领取的待审（广场）
GET /api/v1/reviewer/submissions?scope=CLAIMED       # 我已领的待审
GET /api/v1/reviewer/submissions                     # 全部
GET /api/v1/reviewer/submissions/{submissionId}      # 审核详情
权限: REVIEWER
```

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| taskId | long | 否 | 按任务筛选 |
| submissionStatus | string | 否 | 按提交状态筛选 |
| aiDecision | string | 否 | PASS / REJECT / MANUAL_REVIEW |
| aiReviewStatus | string | 否 | 按AI审核状态筛选 |
| conflictStatus | string | 否 | 按冲突状态筛选 |
| reviewLevel | int | 否 | 按审核级别筛选 |
| scope | string | 否 | AVAILABLE（可领取广场）/ CLAIMED（我的待审）/ 不传全查 |
| page | int | 否 | 页码 |
| size | int | 否 | 每页条数 |

**审核详情** 包含: 标注答案、AI各维度评分、历史审核记录、使用的prompt模板、冲突信息。

### 4.4 审核操作

```
POST /api/v1/reviewer/submissions/{id}/approve      # 通过
POST /api/v1/reviewer/submissions/{id}/reject        # 驳回
POST /api/v1/reviewer/submissions/batch/approve      # 批量通过
POST /api/v1/reviewer/submissions/batch/reject       # 批量驳回
POST /api/v1/reviewer/submissions/batch/mark-manual  # 批量转人工
权限: REVIEWER
```

**通过请求**:

```json
{
  "reviewComment": "标注准确",
  "reviewLevel": 1,
  "revisedAnswerJson": null
}
```

**驳回请求**:

```json
{
  "reason": "分类错误，应为电子产品",
  "reviewLevel": 1
}
```

**批量通过**:

```json
{
  "submissionIds": [700, 701, 702],
  "reviewComment": "批量通过",
  "reviewLevel": 1
}
```

**批量驳回**:

```json
{
  "submissionIds": [700, 701],
  "reason": "批量驳回原因",
  "reviewLevel": 1
}
```

### 4.5 AI 审核重试

```
POST /api/v1/submissions/{submissionId}/ai-review/retry
权限: REVIEWER
```

用于 AI 审核调用失败时手动重试。

---

## 五、状态值速查

### TaskStatus
| 值 | 说明 |
|----|------|
| DRAFT | 草稿 |
| PUBLISHED | 已发布 |
| PAUSED | 已暂停 |
| ENDED | 已结束 |

### ClaimStrategy
| 值 | 说明 |
|----|------|
| FCFS | 先到先得 |
| QUOTA_GRAB | 配额抢单 |
| ASSIGNED | 指派 |

### ItemStatus
| 值 | 说明 |
|----|------|
| UNCLAIMED | 未领取 |
| CLAIMED | 已领取待标注 |
| DRAFT | 草稿中 |
| SUBMITTED | 已提交 |
| RETURNED | 已打回 |
| APPROVED | 已通过 |

### SubmissionStatus
| 值 | 说明 |
|----|------|
| SUBMITTED | 已提交 |
| AI_REVIEWING | AI预审中 |
| PENDING_FINAL | 待人工终审 |
| APPROVED | 已通过 |
| REJECTED | 已驳回 |
| SUPERSEDED | 已废弃 |

---

## 六、与旧接口对照

| 旧路径 | 新路径 |
|--------|--------|
| `POST /tasks/{id}/assignments/claim` | `POST /tasks/{id}/items/claim` |
| `GET /assignments/{id}` | `GET /claims/{id}` |
| `PUT /assignments/{id}/draft` | `PUT /claims/{id}/draft` |
| `POST /assignments/{id}/submit` | `POST /claims/{id}/submit` |
| `GET /labeler/claimed-tasks` | `GET /claims` |
| `GET /tasks/{id}/dataset/items` | `GET /tasks/{id}/items` |
| `POST /tasks/{id}/dataset/items/batch-*` | `POST /tasks/{id}/items/batch-*` |
| `POST /tasks/{id}/dataset/import` | `POST /tasks/{id}/imports` |
| `POST /tasks/{id}/reviewers` | ❌ 已删除 |
| `GET /tasks/{id}/reviewers` | ❌ 已删除 |
