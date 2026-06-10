export type Role = 'ADMIN' | 'OWNER' | 'LABELER' | 'REVIEWER'

export interface User {
  id: string
  name: string
  role: Role
  title: string
}

export interface AuthTokenResponse {
  accessToken: string
  refreshToken: string
  tokenVersion: number
}

export interface LoginResponse extends AuthTokenResponse {
  role: Role
}

export interface LoginCredentials {
  account: string
  password: string
}

export interface RegisterCredentials {
  username: string
  email: string
  password: string
  role: Role
}

export interface AuthState {
  currentUser: User | null
  currentRole: Role | null
  isAuthenticated: boolean
  loginError: string | null
  accessToken: string | null
  refreshToken: string | null
  tokenVersion: number | null
}
