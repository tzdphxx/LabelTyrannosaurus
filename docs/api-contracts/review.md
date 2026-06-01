# review

# Review / Conflict API Contract

Owner：BE\-A

## GET /api/v1/reviewer/submissions

Description: Lists submissions that need reviewer attention, including AI and conflict context for triage.

权限：REVIEWER。

响应字段：

```Plaintext
submissionId
taskId
datasetItemId
labelerId
submissionStatus
aiDecision
conflictStatus
reviewLevel
```

审核级别：

```Plaintext
MVP 单 Reviewer 终审。
reviewLevel 固定为 1。
P1 扩展初审/复审/终审。
```

## POST /api/v1/reviewer/submissions/\{submissionId\}/approve

Description: Approves a reviewed submission, marks it as golden, and emits downstream approval events.

权限：REVIEWER。

请求字段：

```Plaintext
reviewComment
reviewLevel = 1
```

状态影响：

```Plaintext
submission.status = APPROVED
assignment.status = APPROVED
isGolden = true
emit SubmissionApproved
```

## POST /api/v1/reviewer/submissions/\{submissionId\}/reject

Description: Rejects a submission and returns the related assignment to the labeler for correction.

请求字段：

```Plaintext
reason
reviewLevel = 1
```

状态影响：

```Plaintext
submission.status = REJECTED
assignment.status = RETURNED
```

## POST /api/v1/reviewer/conflict\-groups/\{groupId\}/resolve

Description: Resolves a conflict group by selecting the golden submission and publishing the resolution event.

请求字段：

```Plaintext
goldenSubmissionId
reason
```

状态影响：

```Plaintext
selected submission.isGolden = true
emit GoldenSelected
```

