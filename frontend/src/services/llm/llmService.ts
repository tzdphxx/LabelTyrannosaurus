import type { LlmTriggerRunRequest, LlmTriggerRunResponse } from '../../types/llm'
import { request } from '../http'

const POLL_INTERVAL_MS = 1500
const MAX_POLL_ATTEMPTS = 50
const runningStatuses = new Set(['PENDING', 'RUNNING'])

function delay(ms: number) {
  return new Promise((resolve) => {
    globalThis.setTimeout(resolve, ms)
  })
}

function normalizeStatus(status: unknown): LlmTriggerRunResponse['status'] {
  if (typeof status === 'string' && status.trim()) {
    return status as LlmTriggerRunResponse['status']
  }

  if (status && typeof status === 'object') {
    const record = status as Record<string, unknown>
    const value = record.code ?? record.name ?? record.status ?? record.value

    if (typeof value === 'string' && value.trim()) {
      return value as LlmTriggerRunResponse['status']
    }
  }

  return 'FAILED'
}

function normalizeTriggerResponse(response: LlmTriggerRunResponse): LlmTriggerRunResponse {
  return {
    ...response,
    triggerRunId: response.triggerRunId ?? null,
    agentRunId: response.agentRunId ?? null,
    componentId: response.componentId ? String(response.componentId) : '',
    suggestionJson: response.suggestionJson ?? {},
    patch: response.patch ?? {},
    displayText: response.displayText ?? null,
    targetFields: Array.isArray(response.targetFields) ? response.targetFields.map(String) : [],
    rawModelSummary: response.rawModelSummary ?? null,
    confidence: response.confidence ?? null,
    warnings: Array.isArray(response.warnings) ? response.warnings.map(String) : [],
    traceId: response.traceId ?? null,
    status: normalizeStatus(response.status),
    latencyMs: response.latencyMs ?? null,
    errorCode: response.errorCode ?? null,
    errorMessage: response.errorMessage ?? null,
  }
}

function triggerAssignmentLlm(assignmentId: number, payload: LlmTriggerRunRequest): Promise<LlmTriggerRunResponse> {
  console.log('triggerAssignmentLlm', assignmentId, payload)
  return request.post<LlmTriggerRunResponse, Partial<LlmTriggerRunRequest>>(
    `/v1/assignments/${assignmentId}/llm-triggers`,
    {
      componentId: payload.templateVersionId,
      currentAnswerJson: payload.currentAnswerJson,
      datasetItemId: payload.datasetItemId,
      userInstruction: payload.userInstruction,
    },
  )
}

function getTriggerRun(triggerRunId: number): Promise<LlmTriggerRunResponse> {
  return request.get<LlmTriggerRunResponse>(`/v1/llm/triggers/runs/${triggerRunId}`)
}

export const llmService = {
  triggerAssignmentLlm,
  getTriggerRun,

  async runTrigger(payload: LlmTriggerRunRequest): Promise<LlmTriggerRunResponse> {
    if (!payload.assignmentId) {
      throw new Error('缺少 assignmentId，无法触发 LLM 清洗')
    }

    let latestResponse = normalizeTriggerResponse(await triggerAssignmentLlm(payload.assignmentId, payload))

    if (!latestResponse.triggerRunId) {
      return latestResponse
    }

    for (let attempt = 0; attempt < MAX_POLL_ATTEMPTS && runningStatuses.has(latestResponse.status); attempt += 1) {
      const triggerRunId = latestResponse.triggerRunId

      if (!triggerRunId) {
        break
      }

      await delay(POLL_INTERVAL_MS)
      latestResponse = normalizeTriggerResponse(await getTriggerRun(triggerRunId))
    }

    if (runningStatuses.has(latestResponse.status)) {
      return {
        ...latestResponse,
        status: 'FAILED',
        errorMessage: latestResponse.errorMessage ?? 'LLM 清洗仍在运行，请稍后重试',
      }
    }

    return latestResponse
  },
}
