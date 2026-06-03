# AI Interface Capability and API Contract

Owner: BE-A

## AI 接口能力总览

当前 AI 接口可以覆盖以下能力：

```Plaintext
1. 模型供应商管理：OWNER 配置、测试、启用、停用 OpenAI-compatible provider，并查看当前可用模型服务。
2. AI 审核配置：OWNER 为任务配置审核 prompt、评分维度、通过阈值、人工复核阈值和结构化输出 schema。
3. AI Prompt 测试：OWNER 在任务发布前用样例输入测试审核 prompt，不产生 submission 或 ai_review_results。
4. AI 自动审核：Labeler 提交后异步调用 LLM，生成评分、结论、风险标记和建议，供 Reviewer 参考。
5. AI 审核查询与重试：OWNER/REVIEWER 查询审核结果；REVIEWER 可手动重试失败或需要重新评估的 AI 审核。
6. 预标注：LABELER 对 assignment 触发整题预标注，查询 latest/detail，获得建议答案、字段建议、置信度和风险标记。
7. 字段级 LlmTrigger：LABELER 作答时触发字段级 AI 辅助；OWNER 在任务设计时测试 trigger prompt。
8. AI 运行日志：OWNER/REVIEWER 按任务分页查询 AI 审核日志和 LlmTrigger 调用日志。
9. AgentRun 链路追踪：通过 agentRunId 查看一次 AI 调用的输入快照、输出快照、状态、traceId 和耗时。
10. AI 性能/可用性埋点：通过 Actuator 查看 labelhub.ai.requests 和 labelhub.ai.latency。
```

AI 结果边界：

```Plaintext
AI 审核只提供 Reviewer 辅助建议，不会直接把 submission 改为 APPROVED。
预标注和 LlmTrigger 输出只作为建议，前端必须等待用户确认后再写入 answerJson。
Provider API key 和敏感 header 不会通过接口返回。
```

## What the AI APIs can do

The current backend AI surface covers five use cases:

```Plaintext
1. LLM provider management: owners configure OpenAI-compatible providers, test connectivity, enable or disable providers, and list usable providers.
2. AI review configuration: owners configure the prompt, scoring dimensions, thresholds, output schema, and run prompt tests before publishing a task.
3. AI auto review: labeler submission can enqueue an AI review. The result is persisted as reviewer guidance and never directly approves a submission.
4. Pre-annotation: labelers can trigger an async whole-assignment suggestion, then poll latest/detail results before manually accepting any content.
5. LlmTrigger assistance: labelers or owners can run field-level AI assistance from template trigger components and query run logs.
```

Observability is exposed through `AgentRun` detail and Actuator metrics:

```Plaintext
GET /api/v1/agent-runs/{agentRunId}
GET /actuator/metrics/labelhub.ai.requests
GET /actuator/metrics/labelhub.ai.latency
```

`AgentRun` now includes trace and timing fields: `traceId`, `queuedAt`, `startedAt`, `endedAt`, and `latencyMs`.

## POST /api/v1/tasks/{taskId}/ai-review-configs

Description: Creates or saves the AI review configuration used when the task later schedules automated review.
Permission: OWNER.

Request fields:
```Plaintext
providerId
modelName
promptTemplate
scoringDimensions[]
passThreshold
manualReviewThreshold
outputSchema
```

Response fields:
```Plaintext
id
taskId
providerId
modelName
promptTemplate
scoringDimensions[]
passThreshold
manualReviewThreshold
outputSchema
promptVersion
```

Rules:
```Plaintext
Only the task owner can save AI review config.
Only DRAFT tasks can be configured.
Provider must exist and be enabled.
promptTemplate, scoringDimensions, and outputSchema are required.
Thresholds must be between 0.00 and 100.00.
manualReviewThreshold must not be greater than passThreshold.
Saving config backfills tasks.aiReviewConfigId for publish validation.
```

## PUT /api/v1/tasks/{taskId}/ai-review-configs/{configId}

Description: Updates an existing draft-task AI review configuration and advances its prompt version.
Permission: OWNER.
Request and response fields are the same as POST.

Rules:
```Plaintext
Only DRAFT tasks can update AI review config.
configId must belong to taskId.
Each update increments promptVersion.
```

## GET /api/v1/tasks/{taskId}/ai-review-configs

Description: Reads the current AI review configuration for an owner task so the editor can reopen saved settings.
Permission: OWNER.
Response fields are the same as POST.

## POST /api/v1/tasks/{taskId}/ai-review-configs/{configId}/test

Description: Runs a one-off model call against sample input to validate prompt behavior before publishing a task.
Permission: OWNER.

Request fields:
```Plaintext
itemSnapshot
answerJson
```

Response fields:
```Plaintext
agentRunId
status
contentText
structuredJson
rawResponse
latencyMs
errorCode
errorMessage
```

Rules:
```Plaintext
Prompt test calls LlmGateway with the selected provider and model.
Prompt test creates an AI_REVIEW_CONFIG_TEST agentRun.
Model call failure is returned with errorCode and errorMessage.
Prompt test does not create submission or ai_review_result records.
```

## AI Auto Review

AI auto review is started by the submission flow. It creates an `AgentRun` with `agentType=AI_REVIEW`, calls the selected provider through `LlmGateway`, and stores an `ai_review_results` row.

Result rules:

```Plaintext
SUCCESS with valid structured output stores scores, decision, risks, suggestion, prompt snapshot, raw response, and agentRunId.
LLM failure, rate limit, timeout, or invalid structured output writes MANUAL_REQUIRED so reviewer workflow remains open.
AI output is guidance only. It never directly sets submission status to APPROVED.
```

## GET /api/v1/submissions/{submissionId}/ai-review

Description: Returns the AI review guidance attached to a submission for owner or reviewer inspection.
Permission: REVIEWER / OWNER.

Response fields:
```Plaintext
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
promptSnapshot
rawResponse
errorCode
errorMessage
createdAt
updatedAt
```

Status rules:
```Plaintext
When AI fails with fallback, aiReview.status=MANUAL_REQUIRED.
submission.status remains PENDING_FINAL.
AI never sets submission status to APPROVED.
```

## GET /api/v1/submissions/{submissionId}/ai-review-result

Description: Compatibility read endpoint for the same persisted AI review result.
Permission: REVIEWER / ADMIN / task OWNER.

Response fields are the same as `GET /api/v1/submissions/{submissionId}/ai-review`.

## POST /api/v1/submissions/{submissionId}/ai-review/retry

Description: Reviewer-triggered manual retry for a failed or stale AI review result.
Permission: REVIEWER.

Rules:
```Plaintext
Each retry creates a new AgentRun.
The persisted ai_review_results row is updated with the latest retry result.
FAILED and RATE_LIMITED worker results are terminal for that queue attempt; scheduled/manual retry owns the next execution.
```

## GET /api/v1/tasks/{taskId}/ai-review-logs

Description: Paged task-level AI review result log.
Permission: ADMIN / OWNER / REVIEWER.

Query fields:
```Plaintext
page
pageSize
status
decision
startTime ISO_DATE_TIME
endTime ISO_DATE_TIME
```

## POST /api/v1/assignments/{assignmentId}/pre-annotations/run

Description: Triggers async pre-annotation for the current assignment and returns the created run/result shell.
Permission: LABELER.

Request fields:
```Plaintext
force optional
```

Response fields:
```Plaintext
id
assignmentId
taskId
status
suggestedAnswerJson
fieldSuggestions
confidence
riskFlags
modelSummary
agentRunId
traceId
latencyMs
errorCode
errorMessage
createdAt
updatedAt
```

Rules:
```Plaintext
Only the assignment owner labeler can trigger or read their own latest pre-annotation.
One assignment can only have one running pre-annotation at the same time.
The response is a suggestion only; frontend must keep user confirmation before writing it into answerJson.
```

## GET /api/v1/assignments/{assignmentId}/pre-annotations/latest

Description: Returns the latest pre-annotation result for the current labeler's assignment.
Permission: LABELER.

Response fields are the same as pre-annotation run.

## GET /api/v1/pre-annotations/{preAnnotationId}

Description: Returns full pre-annotation detail.
Permission: LABELER can read own assignment result. OWNER and REVIEWER can inspect task results.

Response fields are the same as pre-annotation run.

## POST /api/v1/assignments/{assignmentId}/llm-triggers

Description: Runs field-level LlmTrigger assistance while a labeler is answering an assignment.
Permission: LABELER.

Request fields:
```Plaintext
componentId
providerId
modelName
promptTemplate
targetFields[]
currentAnswerJson
itemSnapshot optional
```

Response fields:
```Plaintext
id
agentRunId
taskId
assignmentId
componentId
suggestionJson
displayText
targetFields
rawModelSummary
status
latencyMs
errorCode
errorMessage
traceId
createdAt
updatedAt
```

Rules:
```Plaintext
The assignment must belong to the current labeler.
Output is only a reference or prefill suggestion.
Frontend must wait for user confirmation before writing suggestion into answerJson.
Every call creates an agentRun.
Calls pass through the LlmTriggerRateLimiter port; the default adapter is no-op until BE-B RateLimitService is wired.
Rate limited calls return status=RATE_LIMITED and do not call LlmGateway.
Successful calls complete the agentRun; failed model calls mark the agentRun FAILED.
```

## POST /api/v1/tasks/{taskId}/llm-triggers/test

Description: Owner preview/test for an LlmTrigger prompt while designing a task/template.
Permission: OWNER.

Request and response fields are the same as assignment LlmTrigger run.

Rules:
```Plaintext
The task must belong to the current owner.
The test does not create a submission.
Every call creates an agentRun and a trigger run record.
```

## GET /api/v1/llm/triggers/runs/{triggerRunId}

Description: Polls a single LlmTrigger run result.
Permission: OWNER / REVIEWER / LABELER, constrained by task or assignment ownership.

Response fields are the same as LlmTrigger run.

## GET /api/v1/tasks/{taskId}/llm-trigger-runs

Description: Paged task-level LlmTrigger run log.
Permission: ADMIN / OWNER / REVIEWER.

Query fields:
```Plaintext
page
pageSize
status
componentId
startTime ISO_DATE_TIME
endTime ISO_DATE_TIME
```

## GET /api/v1/agent-runs/{agentRunId}

Description: Unified trace detail for AI review, pre-annotation, prompt test, and LlmTrigger executions.
Permission: users who can access the linked task/submission/assignment.

Response fields:
```Plaintext
id
agentType
businessType
businessId
taskId
assignmentId
submissionId
agentRunId
traceId
providerId
modelName
status
inputSnapshot
outputSnapshot
errorCode
errorMessage
queuedAt
startedAt
endedAt
latencyMs
createdAt
```

Privacy rules:
```Plaintext
LABELER responses are permission-scoped to the user's own assignment data.
Provider API keys are never returned.
Sensitive provider headers are masked by provider APIs.
```
