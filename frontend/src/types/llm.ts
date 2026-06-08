export type LlmGatewayStatus =
  | 'PENDING'
  | 'RUNNING'
  | 'SUCCESS'
  | 'FAILED'
  | 'MANUAL_REQUIRED'
  | 'PROVIDER_UNAVAILABLE'
  | 'PROVIDER_ERROR'
  | 'TIMEOUT'
  | 'INVALID_JSON'
  | 'RATE_LIMITED'

export interface LlmTriggerRunRequest {
  taskId?: number
  templateVersionId?: number
  componentId?: string | number
  datasetItemId?: number
  assignmentId?: number
  currentAnswerJson: Record<string, unknown>
  previewMode: boolean
  userInstruction?: string
}

export interface LlmTriggerRunResponse {
  triggerRunId: number | null
  agentRunId: number | null
  componentId: string
  suggestionJson: Record<string, unknown>
  patch: Record<string, unknown>
  displayText: string | null
  targetFields: string[]
  rawModelSummary: string | null
  confidence: number | null
  warnings: string[]
  traceId: string | null
  status: LlmGatewayStatus
  latencyMs: number | null
  errorCode: string | null
  errorMessage: string | null
}

export interface LlmTriggerContext {
  taskId?: number | string | null
  templateVersionId?: number | string | null
  datasetItemId?: number | string | null
  assignmentId?: number | string | null
  previewMode?: boolean
}
