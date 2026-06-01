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
