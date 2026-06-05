import type { OwnerModelOptionResponse } from '../../types/task'
import { isRealServiceMode, request } from '../http'

const mockModelOptions: OwnerModelOptionResponse[] = [
  {
    id: 1,
    providerCode: 'mock-openai',
    providerName: 'Mock OpenAI',
    defaultModel: 'gpt-4.1-mini',
    supportVision: true,
    supportMultiImage: true,
    maxImageCount: 8,
    visionModel: 'gpt-4.1-mini',
    structuredOutputMode: 'function_call',
  },
  {
    id: 2,
    providerCode: 'mock-qwen',
    providerName: 'Mock Qwen',
    defaultModel: 'qwen-plus',
    supportVision: false,
    supportMultiImage: false,
    maxImageCount: 0,
    visionModel: '',
    structuredOutputMode: 'json_schema',
  },
]

export const ownerModelService = {
  async listModelOptions(): Promise<OwnerModelOptionResponse[]> {
    if (isRealServiceMode()) {
      return request.get<OwnerModelOptionResponse[]>('/v1/llm-providers')
    }

    return mockModelOptions.map((option) => ({ ...option }))
  },
}
