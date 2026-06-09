import type {
  LlmProviderResponse,
  LlmProviderTestRequest,
  LlmProviderTestResponse,
  LlmProviderUpsertRequest,
} from '../../types/llmProvider'
import { isRealServiceMode, request } from '../http'

let mockProviders: LlmProviderResponse[] = [
  {
    id: 30,
    providerCode: 'dashscope',
    providerName: 'DashScope Qwen Plus',
    baseUrl: 'https://dashscope.aliyuncs.com/compatible-mode/v1',
    defaultModel: 'qwen-plus',
    customHeaders: {
      Authorization: '******',
      'X-Trace-Source': 'labelhub',
    },
    enabled: true,
    platformRateLimitPerMinute: 100,
    taskRateLimitPerMinute: 50,
    userRateLimitPerMinute: 20,
    supportVision: false,
    supportMultiImage: false,
    maxImageCount: 10,
    visionModel: null,
    structuredOutputMode: 'JSON_OBJECT',
    apiKeyConfigured: true,
    createdBy: 1,
    createdAt: '2026-06-06T20:00:00',
    updatedAt: '2026-06-06T20:00:00',
  },
  {
    id: 31,
    providerCode: 'openai',
    providerName: 'OpenAI GPT-4.1 Mini',
    baseUrl: 'https://api.openai.com/v1',
    defaultModel: 'gpt-4.1-mini',
    customHeaders: {},
    enabled: false,
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
]

function nowIso() {
  return new Date().toISOString().slice(0, 19)
}

function cloneProvider(provider: LlmProviderResponse): LlmProviderResponse {
  return {
    ...provider,
    customHeaders: { ...provider.customHeaders },
  }
}

function maskHeaders(customHeaders?: Record<string, string>) {
  return Object.fromEntries(
    Object.entries(customHeaders ?? {}).filter(([key, value]) => key.trim() && value.trim()),
  )
}

function toMockProvider(payload: LlmProviderUpsertRequest, existing?: LlmProviderResponse): LlmProviderResponse {
  const now = nowIso()

  return {
    id: existing?.id ?? Math.max(0, ...mockProviders.map((provider) => provider.id)) + 1,
    providerCode: payload.providerCode.trim(),
    providerName: payload.providerName.trim(),
    baseUrl: payload.baseUrl.trim().replace(/\/+$/, ''),
    defaultModel: payload.defaultModel.trim(),
    customHeaders: maskHeaders(payload.customHeaders),
    enabled: existing?.enabled ?? true,
    platformRateLimitPerMinute: payload.platformRateLimitPerMinute ?? 0,
    taskRateLimitPerMinute: payload.taskRateLimitPerMinute ?? 0,
    userRateLimitPerMinute: payload.userRateLimitPerMinute ?? 0,
    supportVision: payload.supportVision ?? false,
    supportMultiImage: payload.supportMultiImage ?? false,
    maxImageCount: payload.maxImageCount ?? 10,
    visionModel: payload.visionModel?.trim() || null,
    structuredOutputMode: payload.structuredOutputMode ?? null,
    apiKeyConfigured: Boolean(payload.apiKey?.trim()) || existing?.apiKeyConfigured || false,
    createdBy: existing?.createdBy ?? 1,
    createdAt: existing?.createdAt ?? now,
    updatedAt: now,
  }
}

export const adminLlmProviderService = {
  async listProviders(): Promise<LlmProviderResponse[]> {
    if (isRealServiceMode()) {
      return request.get<LlmProviderResponse[]>('/v1/admin/llm-providers')
    }

    return mockProviders.map(cloneProvider).sort((left, right) => right.updatedAt.localeCompare(left.updatedAt))
  },

  async createProvider(payload: LlmProviderUpsertRequest): Promise<LlmProviderResponse> {
    if (isRealServiceMode()) {
      return request.post<LlmProviderResponse, LlmProviderUpsertRequest>('/v1/admin/llm-providers', payload)
    }

    const provider = toMockProvider(payload)
    mockProviders = [provider, ...mockProviders]

    return cloneProvider(provider)
  },

  async updateProvider(providerId: number, payload: LlmProviderUpsertRequest): Promise<LlmProviderResponse> {
    if (isRealServiceMode()) {
      return request.put<LlmProviderResponse, LlmProviderUpsertRequest>(`/v1/admin/llm-providers/${providerId}`, payload)
    }

    const provider = mockProviders.find((item) => item.id === providerId)

    if (!provider) {
      throw new Error('Provider 不存在')
    }

    const nextProvider = toMockProvider(payload, provider)
    mockProviders = mockProviders.map((item) => (item.id === providerId ? nextProvider : item))

    return cloneProvider(nextProvider)
  },

  async enableProvider(providerId: number): Promise<LlmProviderResponse> {
    if (isRealServiceMode()) {
      return request.post<LlmProviderResponse>(`/v1/admin/llm-providers/${providerId}/enable`)
    }

    return this.toggleProvider(providerId, true)
  },

  async disableProvider(providerId: number): Promise<LlmProviderResponse> {
    if (isRealServiceMode()) {
      return request.post<LlmProviderResponse>(`/v1/admin/llm-providers/${providerId}/disable`)
    }

    return this.toggleProvider(providerId, false)
  },

  async testProvider(providerId: number, payload: LlmProviderTestRequest): Promise<LlmProviderTestResponse> {
    if (isRealServiceMode()) {
      return request.post<LlmProviderTestResponse, LlmProviderTestRequest>(`/v1/admin/llm-providers/${providerId}/test`, payload)
    }

    const provider = mockProviders.find((item) => item.id === providerId)

    return {
      success: Boolean(provider),
      latencyMs: 328,
      message: provider ? `OK: ${payload.modelName || provider.defaultModel}` : 'Provider 不存在',
    }
  },

  async toggleProvider(providerId: number, enabled: boolean): Promise<LlmProviderResponse> {
    const provider = mockProviders.find((item) => item.id === providerId)

    if (!provider) {
      throw new Error('Provider 不存在')
    }

    const nextProvider = {
      ...provider,
      enabled,
      updatedAt: nowIso(),
    }
    mockProviders = mockProviders.map((item) => (item.id === providerId ? nextProvider : item))

    return cloneProvider(nextProvider)
  },
}
