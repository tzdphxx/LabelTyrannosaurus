# AI Review / LlmTrigger API Contract

Owner: BE-A

## POST /api/v1/tasks/{taskId}/ai-review-configs

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

Permission: OWNER.
Request and response fields are the same as POST.

Rules:
```Plaintext
Only DRAFT tasks can update AI review config.
configId must belong to taskId.
Each update increments promptVersion.
```

## GET /api/v1/tasks/{taskId}/ai-review-configs

Permission: OWNER.
Response fields are the same as POST.

## POST /api/v1/tasks/{taskId}/ai-review-configs/{configId}/test

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
```

Rules:
```Plaintext
componentId must point to LlmTrigger.
Output is only a reference or prefill suggestion.
Frontend must wait for user confirmation before writing suggestion into answerJson.
Designer previewMode=true does not create submissions.
Every call creates an agentRun.
Calls use RateLimitService.
```

## GET /api/v1/submissions/{submissionId}/ai-review

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
