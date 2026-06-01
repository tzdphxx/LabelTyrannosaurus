import axios, {
  AxiosError,
  AxiosHeaders,
  type AxiosInstance,
  type AxiosResponse,
  type InternalAxiosRequestConfig,
} from 'axios'
import { ApiError, type ApiResponseEnvelope, type RequestConfig } from './httpTypes'

export const AUTH_TOKEN_STORAGE_KEY = 'labelhub_auth_token'
export const REFRESH_TOKEN_STORAGE_KEY = 'labelhub_refresh_token'
export const TOKEN_VERSION_STORAGE_KEY = 'labelhub_token_version'
const DEFAULT_TIMEOUT = 15000
const SUCCESS_CODES = new Set([0, 200])

interface StoredAuthTokens {
  accessToken: string
  refreshToken: string
  tokenVersion: number
}

interface RetriableRequestConfig extends InternalAxiosRequestConfig {
  _retry?: boolean
}

function getBaseURL() {
  return import.meta.env.VITE_API_BASE_URL ?? '/api'
}

function buildApiUrl(path: string) {
  return `${getBaseURL().replace(/\/$/, '')}${path}`
}

function getAuthToken() {
  if (typeof window === 'undefined') {
    return null
  }

  try {
    return window.localStorage.getItem(AUTH_TOKEN_STORAGE_KEY)
  } catch {
    return null
  }
}

function getRefreshToken() {
  if (typeof window === 'undefined') {
    return null
  }

  try {
    return window.localStorage.getItem(REFRESH_TOKEN_STORAGE_KEY)
  } catch {
    return null
  }
}

export function storeAuthTokens(tokens: StoredAuthTokens) {
  if (typeof window === 'undefined') {
    return
  }

  window.localStorage.setItem(AUTH_TOKEN_STORAGE_KEY, tokens.accessToken)
  window.localStorage.setItem(REFRESH_TOKEN_STORAGE_KEY, tokens.refreshToken)
  window.localStorage.setItem(TOKEN_VERSION_STORAGE_KEY, String(tokens.tokenVersion))
}

export function clearAuthTokens() {
  if (typeof window === 'undefined') {
    return
  }

  window.localStorage.removeItem(AUTH_TOKEN_STORAGE_KEY)
  window.localStorage.removeItem(REFRESH_TOKEN_STORAGE_KEY)
  window.localStorage.removeItem(TOKEN_VERSION_STORAGE_KEY)
}

function isApiResponseEnvelope<T>(value: unknown): value is ApiResponseEnvelope<T> {
  return Boolean(
    value &&
    typeof value === 'object' &&
    'code' in value,
  )
}

function unwrapBody<T>(body: ApiResponseEnvelope<T> | T) {
  if (!isApiResponseEnvelope<T>(body)) {
    return body as T
  }

  if (SUCCESS_CODES.has(body.code)) {
    return body.data as T
  }

  throw createApiError({
    code: body.code,
    message: body.message,
    details: body,
  })
}

function createApiError(options: {
  code?: number | string
  message?: string
  response?: AxiosResponse
  requestConfig?: InternalAxiosRequestConfig
  details?: unknown
}) {
  const config = options.requestConfig ?? options.response?.config

  return new ApiError({
    code: options.code,
    message: options.message ?? '请求失败，请稍后重试',
    status: options.response?.status,
    url: config?.url,
    method: config?.method,
    details: options.details,
  })
}

function applyAuthHeader(config: InternalAxiosRequestConfig) {
  const token = getAuthToken()
  console.log('token', token)
  if (!token) {
    return config
  }

  if (config.headers instanceof AxiosHeaders) {
    config.headers.set('Authorization', `Bearer ${token}`)
    return config
  }

  config.headers = AxiosHeaders.from(config.headers)
  // config.headers.set('Authorization', `Bearer ${token}`)

  return config
}

function unwrapResponse<T>(response: AxiosResponse<ApiResponseEnvelope<T> | T>) {
  return unwrapBody(response.data)
}

function normalizeAxiosError(error: AxiosError<ApiResponseEnvelope<unknown>>) {
  if (error.response?.status === 401 && typeof window !== 'undefined') {
    clearAuthTokens()
  }

  const responseBody = error.response?.data
  const message =
    responseBody?.message ||
    error.message ||
    (error.response ? '服务端响应异常' : '网络连接异常')

  return createApiError({
    code: responseBody?.code ?? error.code,
    message,
    response: error.response,
    requestConfig: error.config,
    details: responseBody ?? error.toJSON(),
  })
}

function isAuthEndpoint(url?: string) {
  return Boolean(url && /\/auth\/(login|register|refresh)/.test(url))
}

async function refreshAuthTokens() {
  const refreshToken = getRefreshToken()

  if (!refreshToken) {
    return null
  }

  const response = await axios.post<ApiResponseEnvelope<StoredAuthTokens> | StoredAuthTokens>(
    buildApiUrl('/v1/auth/refresh'),
    { refreshToken },
  )
  const tokens = unwrapBody<StoredAuthTokens>(response.data)
  storeAuthTokens(tokens)

  return tokens
}

export const httpClient: AxiosInstance = axios.create({
  baseURL: getBaseURL(),
  timeout: DEFAULT_TIMEOUT,
})

const responseInterceptor = ((response: AxiosResponse<ApiResponseEnvelope<unknown> | unknown>) =>
  unwrapResponse(response)) as unknown as (response: AxiosResponse) => AxiosResponse

httpClient.interceptors.request.use((config) => applyAuthHeader(config))

httpClient.interceptors.response.use(
  responseInterceptor,
  async (error: AxiosError<ApiResponseEnvelope<unknown>>) => {
    const originalConfig = error.config as RetriableRequestConfig | undefined

    if (error.response?.status === 401 && originalConfig && !originalConfig._retry && !isAuthEndpoint(originalConfig.url)) {
      originalConfig._retry = true

      try {
        const tokens = await refreshAuthTokens()

        if (tokens) {
          return httpClient.request(originalConfig)
        }
      } catch {
        clearAuthTokens()
      }
    }

    return Promise.reject(normalizeAxiosError(error))
  },
)

export const request = {
  async get<T>(url: string, config?: RequestConfig): Promise<T> {
    return httpClient.get<ApiResponseEnvelope<T> | T, T>(url, config)
  },
  async post<T, D = unknown>(url: string, data?: D, config?: RequestConfig): Promise<T> {
    return httpClient.post<ApiResponseEnvelope<T> | T, T, D>(url, data, config)
  },
  async put<T, D = unknown>(url: string, data?: D, config?: RequestConfig): Promise<T> {
    return httpClient.put<ApiResponseEnvelope<T> | T, T, D>(url, data, config)
  },
  async patch<T, D = unknown>(url: string, data?: D, config?: RequestConfig): Promise<T> {
    return httpClient.patch<ApiResponseEnvelope<T> | T, T, D>(url, data, config)
  },
  async delete<T>(url: string, config?: RequestConfig): Promise<T> {
    return httpClient.delete<ApiResponseEnvelope<T> | T, T>(url, config)
  },
}
