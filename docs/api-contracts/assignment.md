# Assignment / Submission API Contract

Owner: BE-A

## GET /api/v1/market/tasks

Description: Lists currently claimable published tasks for labelers in the task marketplace.

Permission: `LABELER`

Response fields:

```text
taskId
title
tags
deadlineAt
availableCount
rewardSummary
```

Constraints:

```text
Only PUBLISHED and non-expired tasks are returned.
Task overlapCount is fixed to 1.
availableCount counts dataset items without a non-CANCELLED assignment.
```

## POST /api/v1/tasks/{taskId}/assignments/claim

Description: Claims one available dataset item for the current labeler and returns the workbench payload.

Permission: `LABELER`

Internal dependencies:

```text
BE-B reserveClaimableItem(taskId, labelerId)
BE-B LockService
```

Claim constraints:

```text
One datasetItemId can have at most one non-CANCELLED assignment.
Unclaimed items are shown as UNCLAIMED in dataset item lists.
After claim succeeds, assignment.status = CLAIMED and itemStatus = CLAIMED.
Overlapping multi-labeler claim is no longer supported.
```

Response fields:

```text
assignmentId
datasetItemId
templateVersionId
schemaJson
itemJson
draftAnswerJson
draftVersion
```

State impact:

```text
assignment.status = CLAIMED
```

## PUT /api/v1/assignments/{assignmentId}/draft

Description: Saves a labeler's draft answer with optimistic version control before final submission.

Request fields:

```text
answerJson
clientVersion
```

State impact:

```text
CLAIMED -> DRAFTING
DRAFTING -> DRAFTING
RETURNED -> DRAFTING
```

Dataset item status impact:

```text
CLAIMED -> DRAFT
RETURNED -> DRAFT
```

Error codes:

```text
409101 draftVersion conflict
```

## POST /api/v1/assignments/{assignmentId}/submit

Description: Validates and submits the assignment answer, creates a submission version, and schedules AI review.

Request fields:

```text
answerJson
clientVersion
```

Internal dependencies:

```text
BE-B validateAnswer(schemaVersionId, answerJson)
```

State impact:

```text
assignment.status = SUBMITTED
dataset itemStatus = SUBMITTED
submission.status = AI_REVIEWING
create submission version
enqueue AI review
```

Resubmit after return:

```text
assignment.status RETURNED -> SUBMITTED
new submission.versionNo = previous + 1
```

## Claim Strategies

The claim behavior is governed by `task.strategy` (set at creation, frozen at publish):

| Strategy | Behavior |
|---|---|
| `FCFS` (default) | Free-for-all. Labelers claim any available item. Quota not enforced. |
| `QUOTA_GRAB` | FCFS + two gates: task-level `quota` (atomic increment) and per-labeler `maxClaimsPerLabeler` (active unfinished count). On cancel, quota is reclaimed. |
| `ASSIGNED` | Labelers can only claim items explicitly dispatched by the owner. Requires dispatches before publish. |

In all strategies, the `POST /api/v1/tasks/{taskId}/assignments/claim` endpoint behaves according to the task's strategy.

## POST /api/v1/tasks/{taskId}/dispatches

Description: Owner batch-assigns dataset items to labelers. Only available for ASSIGNED strategy tasks in DRAFT status.

Permission: `OWNER`

Request:

```text
{
  "dispatches": [
    { "labelerId": 100, "datasetItemId": 500 },
    { "labelerId": 101, "datasetItemId": 501 }
  ]
}
```

Constraints:

```text
Task must use ASSIGNED strategy and be in DRAFT status.
Each datasetItemId must belong to the task and not already have a PENDING dispatch.
Each labelerId must have the LABELER role.
Max 500 entries per request.
Auto-updates task.quota to match total dispatch count.
```

Response:

```text
[
  { "dispatchId": 1, "taskId": 10, "datasetItemId": 500, "labelerId": 100, "status": "PENDING", "dispatchedAt": "...", "claimedAt": null }
]
```

## GET /api/v1/tasks/{taskId}/dispatches

Description: Owner lists all dispatch records for a task.

Permission: `OWNER`

## DELETE /api/v1/tasks/{taskId}/dispatches/{dispatchId}

Description: Owner revokes an unclaimed dispatch. Only PENDING dispatches can be revoked.

Permission: `OWNER`

Effect: Auto-updates task.quota to match remaining dispatch count.

## GET /api/v1/tasks/{taskId}/dispatches/my

Description: Labeler views dispatches assigned to them for a task.

Permission: `LABELER`

Response: List of dispatch records with status PENDING or CLAIMED.
