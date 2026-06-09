export type StructuredOutputMode = 'NONE' | 'JSON_OBJECT' | 'JSON_SCHEMA'

export type CustomHeaders = Record<string, string>

export interface LlmProviderResponse {
  id: number
  providerCode: string
  providerName: string
  baseUrl: string
  defaultModel: string
  customHeaders: CustomHeaders
  enabled: boolean
  platformRateLimitPerMinute: number
  taskRateLimitPerMinute: number
  userRateLimitPerMinute: number
  supportVision: boolean
  supportMultiImage: boolean
  maxImageCount: number
  visionModel: string | null
  structuredOutputMode: StructuredOutputMode | null
  apiKeyConfigured: boolean
  createdBy: number
  createdAt: string
  updatedAt: string
}

export interface LlmProviderUpsertRequest {
  providerCode: string
  providerName: string
  baseUrl: string
  apiKey?: string | null
  defaultModel: string
  customHeaders?: CustomHeaders
  platformRateLimitPerMinute?: number
  taskRateLimitPerMinute?: number
  userRateLimitPerMinute?: number
  supportVision?: boolean
  supportMultiImage?: boolean
  maxImageCount?: number
  visionModel?: string | null
  structuredOutputMode?: StructuredOutputMode | null
}

export interface LlmProviderTestRequest {
  apiKey?: string | null
  modelName?: string | null
  customHeaders?: CustomHeaders
}

export interface LlmProviderTestResponse {
  success: boolean
  latencyMs: number
  message: string
}
