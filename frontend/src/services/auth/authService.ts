import { request } from '../http'
import type { AuthTokenResponse, LoginCredentials, LoginResponse, RegisterCredentials } from '../../types/auth'

export const authService = {
  async login(payload: LoginCredentials): Promise<LoginResponse> {
    return request.post<LoginResponse, LoginCredentials>('/v1/auth/login', payload)
  },

  async register(payload: RegisterCredentials): Promise<AuthTokenResponse> {
    const { email, password, username, role } = payload

    return request.post<AuthTokenResponse, RegisterCredentials>('/v1/auth/register', {
      username,
      email,
      password,
      role,
    })
  },

  async refresh(refreshToken: string): Promise<AuthTokenResponse> {
    return request.post<AuthTokenResponse, { refreshToken: string }>('/v1/auth/refresh', { refreshToken })
  },
}
