# LLM Task Reliability Design

## Context

LabelHub runs AI review and pre-annotation through Redis Stream tasks. Redis is the asynchronous delivery mechanism; MySQL remains the source of truth for business state:

- `agent_runs` records concrete LLM execution attempts.
- `ai_review_results.effective_run_id` points to the currently effective AI review attempt.
- `pre_annotations.agent_run_id` points to the current pre-annotation attempt.
- `audit_logs.trace_id` and `audit_logs.agent_run_id` connect business events to execution details.

The failure fixed by this design happened when a stale Redis pending message was claimed again with `agentRunId=3`, while `agent_runs.id=3` was already `FAILED`. Replaying that message called `AgentRunService.start(3)`, which correctly rejects `FAILED -> RUNNING`.

## Design

Redis Stream messages are treated as retryable delivery envelopes, not as authoritative execution state. Before a queued task reuses an `agentRunId`, the service must reload that run from MySQL and verify the status is still compatible with the operation.

AI review uses stricter reuse:

- Reuse queued `agentRunId` only when the run is still `PENDING`.
- If the queued run is missing or already terminal, create a new `AI_REVIEW` run.
- If an AI review result is already terminal (`SUCCESS` or `MANUAL_REQUIRED`), return it without calling the LLM again so the worker can ack the stale message.

Pre-annotation accepts active runs:

- Continue when `pre_annotations.agent_run_id` points to a `PENDING` or `RUNNING` run.
- Start a `PENDING` run before executing.
- If the referenced run is missing or terminal while the pre-annotation record is non-terminal, create a new `PRE_ANNOTATION` run and update the record.
- If the pre-annotation record is terminal or missing, treat the message as completed.

## Traceability

Every Redis LLM task message carries a `traceId`. `LlmTaskWorker` installs that traceId in a lightweight per-thread execution context while the handler runs. `RequestTraceIdProvider` reads that queued traceId before falling back to the HTTP request header or a generated UUID.

This keeps one trace id across:

- Redis task payload.
- AI/pre-annotation service execution.
- Audit log writes.
- AgentRun and result records linked by `agent_run_id`.

Sensitive provider data remains excluded from snapshots: API keys and sensitive headers are never written to prompts, audit logs, or output snapshots.

## Ack and Retry Rules

The worker acks a message only after either:

- The handler reports the business state is already completed.
- The handler completes successfully.

If the handler throws, the worker logs a warning and does not ack. Redis Stream keeps the message pending, and `claimStale()` can later transfer it to another worker. Because handlers re-check MySQL state, replaying old messages is safe and does not restart failed runs.

## Acceptance Criteria

- Stale AI review messages with a failed `agentRunId` create a new run instead of throwing `Cannot start AgentRun ... expected PENDING`.
- AI review dispatcher only carries pending run ids in Redis messages.
- Terminal AI review and pre-annotation records cause stale messages to ack without duplicate LLM calls.
- Queue traceId is visible through `TraceIdProvider` during worker execution.
- Existing Redis Stream architecture remains unchanged.
