# assignment

# Assignment / Submission API Contract

Owner：BE\-A

## GET /api/v1/market/tasks

Description: Lists currently claimable published tasks for labelers in the task marketplace.

权限：LABELER。

响应字段：

```Plaintext
taskId
title
tags
deadlineAt
availableCount
rewardSummary
```

约束：

```Plaintext
只返回 PUBLISHED 且未过期任务。
MVP 任务分发只做先到先得。
```

## POST /api/v1/tasks/\{taskId\}/assignments/claim

Description: Claims one available dataset item for the current labeler and returns the workbench payload.

权限：LABELER。

内部依赖：

```Plaintext
BE-B reserveClaimableItem(taskId,labelerId)
BE-B LockService
```

响应字段：

```Plaintext
assignmentId
datasetItemId
templateVersionId
schemaJson
itemJson
draftAnswerJson
draftVersion
```

状态影响：

```Plaintext
assignment.status = CLAIMED
```

## PUT /api/v1/assignments/\{assignmentId\}/draft

Description: Saves a labeler's draft answer with optimistic version control before final submission.

请求字段：

```Plaintext
answerJson
clientVersion
```

状态影响：

```Plaintext
CLAIMED -> DRAFTING
DRAFTING -> DRAFTING
```

错误码：

```Plaintext
409101 draftVersion 冲突
```

## POST /api/v1/assignments/\{assignmentId\}/submit

Description: Validates and submits the assignment answer, creates a submission version, and schedules AI review.

请求字段：

```Plaintext
answerJson
clientVersion
```

内部依赖：

```Plaintext
BE-B validateAnswer(schemaVersionId, answerJson)
```

状态影响：

```Plaintext
assignment.status = SUBMITTED
submission.status = AI_REVIEWING
create submission version
enqueue ai review
```

打回后重提：

```Plaintext
assignment.status RETURNED -> SUBMITTED
new submission.versionNo = previous + 1
```

