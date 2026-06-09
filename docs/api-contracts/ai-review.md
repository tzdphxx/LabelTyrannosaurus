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
6. 标注端 AI 辅助：LABELER 对 assignment 或组件触发 AI 辅助，查询 latest/detail 或 trigger run，获得建议答案、置信度和风险标记；OWNER 在任务设计时测试 trigger prompt。
8. AI 运行日志：OWNER/REVIEWER 按任务分页查询 AI 审核日志和 LlmTrigger 调用日志。
9. AgentRun 链路追踪：通过 agentRunId 查看一次 AI 调用的输入快照、输出快照、状态、traceId 和耗时。
10. AI 性能/可用性埋点：通过 Actuator 查看 labelhub.ai.requests 和 labelhub.ai.latency。
```

AI 结果边界：

```Plaintext
AI 审核只提供 Reviewer 辅助建议，不会直接把 submission 改为 APPROVED。
标注端 AI 辅助输出只作为建议，前端必须等待用户确认后再写入 answerJson。
Provider API key 和敏感 header 不会通过接口返回。
```

## What the AI APIs can do

The current backend AI surface covers five use cases:

```Plaintext
1. LLM provider management: owners configure OpenAI-compatible providers, test connectivity, enable or disable providers, and list usable providers.
2. AI review configuration: owners configure the prompt, scoring dimensions, thresholds, output schema, and run prompt tests before publishing a task.
3. AI auto review: labeler submission can enqueue an AI review. The result is persisted as reviewer guidance and never directly approves a submission.
4. Labeling AI assistance: labelers can trigger async AI assistance from assignments or template trigger components, then query latest/detail results or run logs before manually accepting any content.
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
providerId            (required) LLM 供应商 ID
modelName             (optional, ≤128 chars) 模型名称
promptTemplate        (required, ≤10000 chars) 标注规则说明，用于拼装 AI 审核和预标注的 System Prompt
scoringDimensions[]   (required) 评分维度列表，如 ["准确性","完整性","安全性"]
passThreshold         (required, 0.00-100.00) 通过阈值
manualReviewThreshold (required, 0.00-100.00) 人工复核阈值，低于此值打回
maxRetry              (optional, 0-10, default 3) 失败最大重试次数
aiFlowPolicy          (optional, default MANUAL_FIRST) 流转策略
allowAiDirectApprove  (optional) 是否允许 AI 直接通过
allowAiDirectReject   (optional) 是否允许 AI 直接打回
rejectThreshold       (optional, 0.00-100.00) 打回阈值
confidenceThreshold   (optional, 0.00-1.00) 置信度阈值，低于此值转人工
riskFlagsForceManual[] (optional) 遇这些风险标记则强制转人工
multimodalEnabled     (optional, default true) 是否启用多模态（图片/视频输入）
degradationPenalty    (optional, 0.00-1.00, default 0.20) 降级时 confidence 惩罚系数
visionDetail          (optional, "auto"|"low"|"high") 视觉精度
maxImagesPerRequest   (optional, 0-20, default 5) 单次请求最大图片数
allowAiDirectApproveWhenDegraded (optional) 降级模式下是否仍允许 AI 直接通过

-- v3.6 多策略审核配置 --
reviewStrategy        (optional, default LIGHTWEIGHT) 审核策略
                      LIGHTWEIGHT: 单路 LLM（默认）
                      PARALLEL_VOTE: 多模型并行投票
                      DEEP_DIMENSION: 维度专项模型 + 维度内投票
                      AGENT_DEBATE: 多 Agent 辩论
voteModels[]          (optional) 投票模型列表 [{"providerId":1,"modelName":"qwen-plus"}]
                      仅配 1 个模型时系统自动复制到满足最少票数
voteMinAgreement      (optional, 1-10, default 2) 最少一致票数
dimensionReviewers    (optional) 深度模式维度→模型映射
                      {"accuracy":[{"providerId":1,"modelName":"qwen-plus"}]}
```

Response fields (same as request plus):
```Plaintext
id                    (auto) 配置 ID
taskId                (auto) 任务 ID
outputSchema          (auto) 输出 JSON Schema
promptVersion         (auto) Prompt 版本号，每次更新递增
voteModels[]          (auto) 投票模型列表（含系统自动扩展后的结果）
voteMinAgreement      (auto) 最少一致票数
dimensionReviewers    (auto) 深度模式维度→模型映射
```

Rules:
```Plaintext
Only the task owner can save/update AI review config.
Only DRAFT tasks can be configured.
Provider must exist and be enabled.
promptTemplate, scoringDimensions are required.
Thresholds must be between 0.00 and 100.00.
manualReviewThreshold must not be greater than passThreshold.
Saving config backfills tasks.aiReviewConfigId for publish validation.
reviewStrategy defaults to LIGHTWEIGHT for backward compatibility.
Single-model voteModels auto-duplicated to meet voteMinAgreement.
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
reviewTrace
promptSnapshot
rawResponse
errorCode
errorMessage
createdAt
updatedAt
```

`reviewTrace` structure:
```json
{
  "strategy": "PARALLEL_VOTE",
  "strategyLabel": "Parallel model vote",
  "summary": "3 model branches reviewed in parallel; 2 branch(es) supported the final decision; consensus threshold was met.",
  "steps": [
    {
      "name": "qwen-plus",
      "role": "voter",
      "decision": "PASS",
      "score": "90",
      "confidence": "0.9",
      "status": "SUCCESS",
      "reason": "The submitted answer matches the task requirements."
    }
  ],
  "metrics": {
    "voteCount": 3,
    "topVotes": 2,
    "hasConsensus": true,
    "minAgreement": 2
  }
}
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

Response fields are the same as `GET /api/v1/submissions/{submissionId}/ai-review`, plus:
```Plaintext
rawPrompt              AI review config promptTemplate for the submission task.
answerJson             Labeler submitted answer JSON for this submission.
```

Notes:
```Plaintext
rawPrompt is the user-provided prompt template, not the fully assembled system prompt.
answerJson is returned instead of rawResponse to avoid conflicting with ai_review_results.raw_response, which stores the LLM provider raw response internally.
```

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
