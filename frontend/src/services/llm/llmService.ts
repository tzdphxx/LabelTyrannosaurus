import type { LlmTriggerRunRequest, LlmTriggerRunResponse } from '../../types/llm'
import { request } from '../http'

export const llmService = {
  async runTrigger(payload: LlmTriggerRunRequest): Promise<LlmTriggerRunResponse> {
    return request.post<LlmTriggerRunResponse, LlmTriggerRunRequest>('/v1/llm/triggers/run', payload)
  },
}
