# review

# Review / Conflict API Contract

Owner：BE\-A

## 领取模型（整任务领取）

审核员按"整任务 + 审核级别"领取，而非逐题领取。一个 `(taskId, reviewLevel)` 组合
只能被一名审核员领取（数据库唯一约束保证排他）。领取后，该任务该级别下当前以及
后续进入待审池（`PENDING_FINAL`）的提交都会自动归属给该审核员。

- 多级审核：同一审核员不能同时持有同一任务的多个级别；不同级别由不同审核员领取。
- 后续提交：AI 预审完成转入 `PENDING_FINAL`、升级到下一级时，按已存在的领取记录自动归属。
- 已停用按题目的自动分配调度器（`labelhub.review.auto-assign-enabled` 默认 `false`）。

## POST /api/v1/reviewer/tasks/\{taskId\}/claim

Description: Reviewer claims a whole task at a given review level; all current and future
PENDING\_FINAL submissions at that level are assigned to the reviewer.

权限：REVIEWER。

请求参数（query）：

```Plaintext
reviewLevel = 1   # 默认 1；多级审核任务可领取更高级别
```

响应字段：

```Plaintext
taskId
reviewLevel
claimedSubmissionCount   # 本次归属到该审核员名下的待审提交数
```

错误码：

```Plaintext
409201  该任务该级别已被其他审核员领取
403601  同一审核员不能领取同一任务的多个级别
400601  草稿任务无可审提交
400603  审核级别非法（超出 reviewLevelCount）
```

## DELETE /api/v1/reviewer/tasks/\{taskId\}/claim

Description: Releases a claimed task level; the reviewer's still-pending submissions at that
level return to the unassigned pool and can be re-claimed.

权限：REVIEWER。

请求参数（query）：`reviewLevel = 1`。

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
reviewLevel 取自 submission.current_review_level。
单级任务固定为 1；多级任务随升级递增。
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

