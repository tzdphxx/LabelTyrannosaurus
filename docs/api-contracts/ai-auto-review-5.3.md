# 5.3 AI Auto Review Contract

## Flow

Labeler submit creates a submission in `AI_REVIEWING`, then enqueues one AI review for that `submissionId`.

The AI review creates exactly one `ai_review_results` row per submission through the unique `submission_id` idempotency key. It creates and starts an `AgentRun` with `agentType=AI_REVIEW`, calls `LlmGateway.review(...)`, persists `promptSnapshot` and `rawResponse`, and finally moves the submission to `PENDING_FINAL`.

## Result Rules

- LLM success with valid structured output writes `ai_review_results.status=SUCCESS`.
- LLM failure, rate limit, or invalid structured output writes `ai_review_results.status=MANUAL_REQUIRED`.
- Both success and fallback keep the manual review path open by setting `submission.status=PENDING_FINAL`.
- AI output is reviewer guidance only and never writes `APPROVED`.

## Query API

```Plaintext
GET /api/v1/submissions/{submissionId}/ai-review-result
```

Readable by `REVIEWER`, `ADMIN`, and the task `OWNER`.

Response data includes `submissionId`, `agentRunId`, `providerId`, `modelName`, `status`, `decision`, `averageScore`, `dimensionScores`, `riskFlags`, `suggestion`, `errorCode`, and `errorMessage`.
