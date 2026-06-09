# Submission Item History API Design

## Goal

Add one shared HTTP API that lets users inspect the history of a dataset item from the context of a submission.

The API must support two caller roles:

- Labeler: sees only their own submissions for the same task item.
- Reviewer: while reviewing a submission, sees the item history needed for review, across submissions for the same task item.

## Endpoint

`GET /api/v1/submissions/{submissionId}/item-history`

This endpoint belongs with the existing submission trace APIs under `SubmissionTraceController`.

## Lookup Scope

The service first loads the current `Submission` by `submissionId`.

It then uses the submission's `taskId` and `datasetItemId` as the item identity:

- Labeler scope: `task_id = current.taskId AND dataset_item_id = current.datasetItemId AND labeler_id = currentUserId`
- Reviewer scope: `task_id = current.taskId AND dataset_item_id = current.datasetItemId`

## Authorization

- `LABELER`: allowed only if the requested submission belongs to the current user. Returned history is limited to the current user's own submissions for the item.
- `REVIEWER`: allowed only if the requested submission is assigned to the current reviewer, matching the existing reviewer detail access rule. Returned history includes all submissions for the item.
- `OWNER` and `ADMIN`: allowed to read all submissions for the item.
- Other roles are rejected.

## Response Shape

```json
{
  "taskId": 1,
  "datasetItemId": 10,
  "histories": [
    {
      "submissionId": 100,
      "assignmentId": 20,
      "versionNo": 1,
      "status": "PENDING_FINAL",
      "submittedBy": 7,
      "submittedByName": "labelerA",
      "submittedAt": "2026-06-08T10:00:00",
      "aiReview": {
        "aiReviewResultId": 300,
        "agentRunId": 400,
        "status": "SUCCESS",
        "decision": "PASS",
        "reviewedAt": "2026-06-08T10:01:00"
      },
      "reviewRounds": [
        {
          "reviewRecordId": 500,
          "reviewLevel": 1,
          "reviewerId": 30,
          "reviewerName": "reviewerA",
          "action": "APPROVE",
          "reason": null,
          "reviewComment": "ok",
          "reviewedAt": "2026-06-08T10:10:00"
        }
      ]
    }
  ]
}
```

AI reviewed time uses `agent_runs.finished_at` first. If that value is unavailable, it falls back to `ai_review_results.updated_at`, then `ai_review_results.created_at`.

## Data Sources

- `submissions`: submitter, assignment, task item identity, status, version, submitted time.
- `ai_review_results`: AI status, decision, result update timestamps, effective agent run.
- `agent_runs`: AI execution finished time.
- `review_records`: manual review rounds, reviewer, action, reason, comment, reviewed time.
- `users`: display names for submitters and reviewers.

## Implementation Components

- Add DTO `SubmissionItemHistoryResponse`.
- Add service `SubmissionItemHistoryService`.
- Add mapper method on `SubmissionMapper` to select submissions by `taskId`, `datasetItemId`, and optional `labelerId`.
- Reuse `AiReviewResultMapper.selectBySubmissionIds`.
- Reuse `ReviewRecordMapper.selectBySubmissionIds`.
- Reuse or extend user-name resolution so both submitter names and reviewer names are returned.
- Add `GET /{submissionId}/item-history` to `SubmissionTraceController`.

## Sorting

- Submission history: `submittedAt ASC`, then `submissionId ASC`.
- Manual review rounds within each submission: `reviewLevel ASC`, then `createdAt ASC`, then `reviewRecordId ASC`.

## Error Handling

- Missing submission: return business exception consistent with existing submission trace APIs.
- Labeler reading another labeler's requested submission: forbidden.
- Reviewer reading an unassigned requested submission: forbidden.
- Unsupported role: forbidden.

## Tests

Add focused unit tests for `SubmissionItemHistoryService`:

- Labeler sees only their own submissions for the same item.
- Labeler cannot request another user's submission.
- Reviewer sees all submissions for the item when assigned to the requested submission.
- Reviewer cannot read an unassigned submission.
- AI reviewed time prefers `agentRun.finishedAt` and falls back to AI result timestamps.
- Review records are grouped by submission and sorted by review level.

Add controller coverage if existing web-test setup makes this practical without broad fixture work.
