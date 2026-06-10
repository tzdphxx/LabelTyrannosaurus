import type { OwnerModelOptionResponse } from '../../types/task'
import { isRealServiceMode, request } from '../http'

const mockModelOptions: OwnerModelOptionResponse[] = [
  {
    id: 1,
    providerCode: 'mock-openai',
    providerName: 'Mock OpenAI',
    baseUrl: 'https://api.openai.com/v1',
    defaultModel: 'gpt-4.1-mini',
    customHeaders: {},
    enabled: true,
    platformRateLimitPerMinute: 120,
    taskRateLimitPerMinute: 60,
    userRateLimitPerMinute: 30,
    supportVision: true,
    supportMultiImage: true,
    maxImageCount: 8,
    visionModel: 'gpt-4.1-mini',
    structuredOutputMode: 'JSON_SCHEMA',
    apiKeyConfigured: true,
    createdBy: 1,
    createdAt: '2026-06-05T18:30:00',
    updatedAt: '2026-06-05T18:30:00',
  },
  {
    id: 2,
    providerCode: 'mock-qwen',
    providerName: 'Mock Qwen',
    baseUrl: 'https://dashscope.aliyuncs.com/compatible-mode/v1',
    defaultModel: 'qwen-plus',
    customHeaders: {},
    enabled: true,
    platformRateLimitPerMinute: 100,
    taskRateLimitPerMinute: 50,
    userRateLimitPerMinute: 20,
    supportVision: false,
    supportMultiImage: false,
    maxImageCount: 0,
    visionModel: null,
    structuredOutputMode: 'JSON_OBJECT',
    apiKeyConfigured: true,
    createdBy: 1,
    createdAt: '2026-06-06T20:00:00',
    updatedAt: '2026-06-06T20:00:00',
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
