# AI 预审接口契约

## 适用范围

- Owner 模块：BE-A。
- 支撑模块：BE-B 提供 Redis Stream 队列、RateLimitService、AuditAppender、System Principal 和补偿扫描基础能力。
- 前端使用页面：Labeler 提交结果页、Reviewer 审核详情页、Owner 任务配置页。

## AI 预审结论

`ai_review_results.decision` 只允许三种结论：

|值|含义|后续流转|
|---|---|---|
|`PASS`|通过 AI 预审|`submission.status` 推进到 `PENDING_FINAL`，进入人工终审或后续审核队列。|
|`RETURN`|AI 打回|`assignment.status` 推进到 `AI_RETURNED`，标注员按建议修改后重新提交新版本。|
|`MANUAL_REVIEW`|需要人工复核|`submission.status` 推进到 `PENDING_FINAL`，并标记人工优先处理。|

说明：
- 不再使用 `REJECT` 表达 AI 预审结论。
- `MANUAL_REVIEW` 既可以来自模型判断，也可以作为模型连续失败、限流耗尽或输出不可解析后的兜底结论。
- `ai_reject_action` 是历史字段名；后续业务语义按“非通过结果路由策略”理解，不把 AI 打回展示为最终拒绝。

## 可靠队列

AI 预审排队使用 Redis Stream，按任务维度隔离：

```text
ai:review:stream:task:{taskId}
consumer group: ai-review-workers
```

提交成功后，BE-A 必须先在数据库事务中写入 `submissions`、`ai_review_results(status=PENDING)`，再向 Redis Stream 写入消息。若 Redis 入队失败，数据库中的 `AI_REVIEWING/PENDING` 记录由补偿扫描重新入队，业务任务不丢失。

消息字段：

```json
{
  "submissionId": 200,
  "taskId": 1,
  "assignmentId": 100,
  "labelerId": 20,
  "retryCount": 0,
  "traceId": "trace-1",
  "createdAt": "2026-05-30T10:00:00"
}
```

消费规则：
- 同一 `taskId` 队列内按 Stream 写入顺序 FIFO。
- 消费者只有在 `agent_runs`、`ai_review_results`、`submissions/assignments` 状态和审计全部落库后才能 ACK。
- 未 ACK、消费者宕机、`RUNNING` 超时或漏入队记录，由启动补偿和定时补偿重新投递。
- 重试次数使用 `ai_review_configs.max_retry`，超限后落 `MANUAL_REVIEW`。

## 查询 AI 预审结果

- URL：`/api/v1/submissions/{submissionId}/ai-review`
- Method：`GET`
- 权限角色：`LABELER` 可查自己的提交，`REVIEWER`/`OWNER`/`ADMIN` 可查授权范围内提交。

响应：

```json
{
  "code": 0,
  "message": "ok",
  "data": {
    "submissionId": 200,
    "status": "SUCCESS",
    "decision": "RETURN",
    "averageScore": 72.5,
    "dimensionScores": {
      "accuracy": 68,
      "format": 90
    },
    "riskFlags": ["LOW_CONFIDENCE"],
    "suggestion": "答案证据不足，请补充理由。",
    "effectiveRunId": 501,
    "retryCount": 1,
    "updatedAt": "2026-05-30T10:01:00"
  },
  "traceId": "trace-1"
}
```

## 手动重试 AI 预审

- URL：`/api/v1/submissions/{submissionId}/ai-review/retry`
- Method：`POST`
- 权限角色：`REVIEWER`、`OWNER`、`ADMIN`

规则：
- 只允许对 `FAILED`、`RATE_LIMITED`、`MANUAL_REQUIRED` 或明确需要重跑的结果重试。
- 重试会创建新的 `agent_runs`，并更新 `ai_review_results.effective_run_id`。
- 重试消息仍进入 `ai:review:stream:task:{taskId}`，不绕过队列。

## 错误码

|code|场景|
|---|---|
|`400101`|当前提交状态不允许重试或重复入队。|
|`400102`|参数非法、提交不存在或任务不匹配。|
|`403001`|无权查看或重试该提交。|
|`429001`|LLM 平台、任务或用户维度限流。|
|`500001`|AI 预审执行异常，响应中必须带 `traceId`。|
