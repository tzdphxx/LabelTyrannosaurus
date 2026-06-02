# AI Review / LlmTrigger API Contract

Owner: BE-A

## POST /api/v1/tasks/{taskId}/ai-review-configs

Description: Creates or saves the AI review configuration used when the task later schedules automated review.
Permission: OWNER.

Request fields:
```Plaintext
providerId (required) — Admin-enabled provider ID to use as the model
modelName (optional) — if provided, must match the provider's defaultModel; otherwise derived from provider
promptTemplate
scoringDimensions[]
passThreshold
manualReviewThreshold
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
promptVersion
```

Rules:
```Plaintext
Only the task owner can save AI review config.
Only DRAFT tasks can be configured.
Provider must exist and be enabled (Admin-managed, not owner-owned).
modelName is derived from the selected provider's defaultModel.
  If the request includes modelName, it must equal the provider's defaultModel (400402 otherwise).
promptTemplate and scoringDimensions are required.
outputSchema is managed by Admin at the Provider level (structuredOutputMode + outputSchemaJson).
Thresholds must be between 0.00 and 100.00.
manualReviewThreshold must not be greater than passThreshold.
Saving config backfills tasks.aiReviewConfigId for publish validation.
Owner no longer creates or manages API Keys — those are Admin-only.
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

## POST /api/v1/llm/triggers/run

Description: Executes a template LlmTrigger component for designer preview or labeler workbench assistance.
Permission: OWNER / LABELER.
Use cases:
```Plaintext
Designer preview LlmTrigger.
Labeler workbench field-level model assistance.
```

Request fields:
```Plaintext
taskId
templateVersionId
componentId
datasetItemId optional
assignmentId optional
currentAnswerJson
previewMode
```

Response fields:
```Plaintext
agentRunId
componentId
suggestionJson
displayText
targetFields
rawModelSummary
status
latencyMs
errorCode
errorMessage
```

Rules:
```Plaintext
componentId must point to a template schema component whose type is LlmTrigger.
Supported component fields: id/componentId, type, providerId, modelName, promptTemplate, targetFields[].
Designer previewMode=true requires the current user to be the task owner.
Labeler workbench previewMode=false requires an owned assignmentId matching taskId and templateVersionId.
Output is only a reference or prefill suggestion.
Frontend must wait for user confirmation before writing suggestion into answerJson.
Designer previewMode=true does not create submissions.
Every call creates an agentRun.
Calls pass through the LlmTriggerRateLimiter port; the default adapter is no-op until BE-B RateLimitService is wired.
Rate limited calls return status=RATE_LIMITED and do not call LlmGateway.
Successful calls complete the agentRun; failed model calls mark the agentRun FAILED.
Provider must be an Admin-enabled provider (no longer requires task owner to own the provider).
```

## GET /api/v1/submissions/{submissionId}/ai-review

Description: Returns the AI review guidance attached to a submission for owner or reviewer inspection.
Permission: REVIEWER / OWNER.

Response fields:
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

Status rules:
```Plaintext
When AI fails with fallback, aiReview.status=MANUAL_REQUIRED.
submission.status remains PENDING_FINAL.
AI never sets submission status to APPROVED.
```

## Model Selection Flow

```Plaintext
1. ADMIN creates LLM providers with encrypted API keys via /api/v1/admin/llm-providers.
2. ADMIN enables the providers that should be available for task AI configuration.
3. OWNER browses enabled models via GET /api/v1/llm-providers (limited fields, no secrets).
4. OWNER selects a providerId and configures AI review rules via /api/v1/tasks/{taskId}/ai-review-configs.
5. Backend derives modelName from provider.defaultModel; request modelName is validated against it.
6. At runtime, LlmGateway decrypts the API key in-memory using the provider ID.
7. Owner never sees or manages API keys.
```
