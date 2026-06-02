export type LlmGatewayStatus =
  | 'SUCCESS'
  | 'PROVIDER_UNAVAILABLE'
  | 'PROVIDER_ERROR'
  | 'TIMEOUT'
  | 'INVALID_JSON'
  | 'RATE_LIMITED'

export interface LlmTriggerRunRequest {
  taskId: number
  templateVersionId: number
  componentId: string
  datasetItemId?: number
  assignmentId?: number
  currentAnswerJson: Record<string, unknown>
  previewMode: boolean
}

export interface LlmTriggerRunResponse {
  agentRunId: number | null
  componentId: string
  suggestionJson: Record<string, unknown>
  displayText: string | null
  targetFields: string[]
  rawModelSummary: string | null
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
