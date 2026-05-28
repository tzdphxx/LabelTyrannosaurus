# ai\-review

# AI Review / LlmTrigger API Contract

Owner：BE\-A

## POST /api/v1/tasks/\{taskId\}/ai\-review\-configs

权限：OWNER。

请求字段：

```Plaintext
providerId
modelName
promptTemplate
dimensions[]
decisionPolicy
```

响应字段：

```Plaintext
aiReviewConfigId
taskId
enabled
```

## POST /api/v1/llm/triggers/run

权限：OWNER / LABELER。

使用场景：

```Plaintext
Designer 预览 LlmTrigger。
Labeler 工作台字段级模型辅助。
```

请求字段：

```Plaintext
taskId
templateVersionId
componentId
datasetItemId optional
assignmentId optional
currentAnswerJson
previewMode
```

响应字段：

```Plaintext
agentRunId
componentId
suggestionJson
displayText
targetFields
rawModelSummary
```

规则：

```Plaintext
componentId 必须指向 LlmTrigger。
输出只作为参考或预填建议。
前端必须用户确认后才写入 answerJson。
Designer previewMode=true 时不产生 submission。
每次调用产生 agentRun。
调用走 RateLimitService。
```

## GET /api/v1/submissions/\{submissionId\}/ai\-review

权限：REVIEWER / OWNER。

响应字段：

```Plaintext
aiReviewStatus
decision
averageScore
dimensionScores
riskFlags
suggestion
agentRunId
promptSnapshot
rawResponse
```

状态规则：

```Plaintext
AI 失败兜底时 aiReview.status=MANUAL_REQUIRED。
submission.status 仍为 PENDING_FINAL。
AI 不直接设置 APPROVED。
```

