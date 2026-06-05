# Task API Contract

Owner: BE-A

Task APIs are owned by the task lifecycle module. Dataset, template, assignment,
reward, AI review, and export APIs may use `taskId` in their paths, but their
contracts stay in their own module documents.

## GET /api/v1/owner/tasks

Description: Lists tasks created by the current OWNER user for the owner task dashboard.

- Method: `GET`
- Permission: `OWNER`
- Request: none
- Response: `OwnerTaskSummaryResponse[]`

Response fields:

```text
taskId
title
status
tags
quota
claimedCount
overlapCount
strategy
deadlineAt
publishedAt
endedAt
createdAt
updatedAt
```

Rules:

```text
Only tasks whose ownerId equals the current user are returned.
Results are ordered by updatedAt desc, then id desc.
```

## POST /api/v1/tasks

Description: Creates an OWNER task draft that can later be configured, supplied with data/template/rules, and published.

- Method: `POST`
- Permission: `OWNER`
- Request: `CreateTaskRequest`
- Response: `TaskLifecycleResponse`

Request fields:

```text
title required, max 200
description optional
instructionRichText optional
tags optional, each tag max 64
quota required for FCFS/QUOTA_GRAB (>= 1), auto-derived for ASSIGNED
deadlineAt required, must be future time
overlapCount required, must be 1
strategy optional, default FCFS. Values: FCFS | QUOTA_GRAB | ASSIGNED
maxClaimsPerLabeler optional, >= 1. Only effective for QUOTA_GRAB
publishedTemplateVersionId optional, must belong to current OWNER
aiReviewConfigId optional
aiProviderId / aiModelName / aiPrompt / aiScoringDimensions / aiPassThreshold / aiManualReviewThreshold optional, for inline AI config
reviewLevelCount optional, >= 1, default 1
datasetFileId optional
rewardRule optional
```

Claim strategy behavior:

```text
FCFS (default): Labelers freely claim any available item until the dataset runs out.
                 Quota is not enforced; maxClaimsPerLabeler is ignored.

QUOTA_GRAB:      Like FCFS, but with two additional gates:
                 1. Task claimed_count < quota (atomic increment, rejects when full).
                 2. Labeler's active unfinished claims < maxClaimsPerLabeler.

ASSIGNED:        Labelers can only claim items explicitly dispatched to them by the owner.
                 Quota is automatically derived from the total dispatch count.
                 Requires dispatches to be created via the /dispatches API before publish.
```

Response fields:

```text
taskId
status = DRAFT
```

Effects:

```text
Creates a tasks row with ownerId = current user and status = DRAFT.
Strategy defaults to FCFS if not specified.
If publishedTemplateVersionId is provided, verifies the template version belongs to current OWNER.
Normalizes non-blank tags into task_tags.
Appends TASK_CREATED audit log.
```

## GET /api/v1/tasks/{taskId}

Description: Returns detail for one task owned by the current OWNER user.

- Method: `GET`
- Permission: `OWNER`
- Path variable: `taskId`
- Request: none
- Response: `TaskDetailResponse`

Response fields:

```text
taskId
ownerId
title
description
instructionRichText
status
tags
quota
claimedCount
overlapCount
strategy
maxClaimsPerLabeler
deadlineAt
publishedTemplateVersionId
aiReview (AiReviewConfigResponse, nullable)
reviewLevelCount
rewardVisible
rewardRule
publishedAt
endedAt
createdAt
updatedAt
```

Errors:

```text
404001 Task not found or not owned by current user.
```

## PUT /api/v1/tasks/{taskId}

Description: Updates an owned DRAFT task before it is published.

- Method: `PUT`
- Permission: `OWNER`
- Path variable: `taskId`
- Request: `UpdateTaskRequest`
- Response: `TaskLifecycleResponse`

Request fields:

```text
title required, max 200
description optional
instructionRichText optional
tags optional, each tag max 64
quota required for FCFS/QUOTA_GRAB (>= 1), auto-derived for ASSIGNED
deadlineAt required, must be future time
overlapCount required, must be 1
strategy optional. Values: FCFS | QUOTA_GRAB | ASSIGNED. Only mutable while DRAFT
maxClaimsPerLabeler optional, >= 1. Only effective for QUOTA_GRAB
publishedTemplateVersionId optional, must belong to current OWNER
aiReviewConfigId optional
reviewLevelCount optional, >= 1
rewardRule optional
```

Response fields:

```text
taskId
status = DRAFT
```

Rules:

```text
Only DRAFT tasks can be edited.
If publishedTemplateVersionId is provided, verifies the template version belongs to current OWNER.
Replaces the task tag set with normalized non-blank tags from the request.
Appends TASK_UPDATED audit log.
```

Errors:

```text
400101 Task status does not allow editing.
404001 Task not found or not owned by current user.
```

## POST /api/v1/tasks/{taskId}/publish

Description: Publishes a draft task after verifying all cross-module prerequisites are ready.

- Method: `POST`
- Permission: `OWNER`
- Path variable: `taskId`
- Request: none
- Response: `TaskLifecycleResponse`

Publish checks:

```text
Task status must be DRAFT.
Task overlapCount must be 1.
Task deadlineAt must be in the future.
Task quota must be > 0 (FCFS/QUOTA_GRAB only; ASSIGNED auto-syncs from dispatch count).
For ASSIGNED strategy: at least one dispatch must exist.
BE-B dataset must be ready.
BE-B template version must exist and belong to the task owner.
BE-B reward rule must exist.
BE-A AI review config must exist.
```

State impact:

```text
DRAFT -> PUBLISHED
publishedAt = now
```

Errors:

```text
400101 Task status transition is not allowed.
400102 Publish prerequisite is missing or template version is not usable by this task.
404001 Task not found or not owned by current user.
```

## POST /api/v1/tasks/{taskId}/pause

Description: Temporarily pauses a published task so labelers cannot continue claiming new work.

- Method: `POST`
- Permission: `OWNER`
- Path variable: `taskId`
- Request: none
- Response: `TaskLifecycleResponse`

State impact:

```text
PUBLISHED -> PAUSED
```

Errors:

```text
400101 Task status transition is not allowed.
404001 Task not found or not owned by current user.
```

## POST /api/v1/tasks/{taskId}/resume

Description: Resumes a paused task and makes it available for labeler work again.

- Method: `POST`
- Permission: `OWNER`
- Path variable: `taskId`
- Request: none
- Response: `TaskLifecycleResponse`

State impact:

```text
PAUSED -> PUBLISHED
```

Errors:

```text
400101 Task status transition is not allowed.
404001 Task not found or not owned by current user.
```

## POST /api/v1/tasks/{taskId}/end

Description: Ends a task permanently from active distribution and review progression.

- Method: `POST`
- Permission: `OWNER`
- Path variable: `taskId`
- Request: none
- Response: `TaskLifecycleResponse`

State impact:

```text
PUBLISHED/PAUSED -> ENDED
endedAt = now
```

Errors:

```text
400101 Task status transition is not allowed.
404001 Task not found or not owned by current user.
```

## Shared Response Types

`TaskLifecycleResponse`:

```text
taskId
status
```

`TaskStatus` values:

```text
DRAFT
PUBLISHED
PAUSED
ENDED
```

## Error Codes

```text
400101 Task status does not allow the requested operation.
400102 Publish prerequisite is missing or invalid.
401001 User is unauthenticated or token is invalid.
403001 Current user is not allowed to operate the task.
404001 Task does not exist or is not owned by current user.
500001 System error.
```
