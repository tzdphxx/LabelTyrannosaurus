import type { AxiosRequestConfig } from 'axios'

export type ServiceMode = 'mock' | 'real'

export interface ApiResponseEnvelope<T> {
  code: number
  message?: string
  data?: T
}

export interface ApiErrorOptions {
  code?: number | string
  message: string
  status?: number
  url?: string
  method?: string
  details?: unknown
}

export class ApiError extends Error {
  code?: number | string
  status?: number
  url?: string
  method?: string
  details?: unknown

  constructor(options: ApiErrorOptions) {
    super(options.message)
    this.name = 'ApiError'
    this.code = options.code
    this.status = options.status
    this.url = options.url
    this.method = options.method
    this.details = options.details
  }
}

export type RequestConfig = AxiosRequestConfig
