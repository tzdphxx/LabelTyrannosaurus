import { create } from 'zustand'
import { authService, clearAuthTokens, storeAuthTokens } from '../services'
import type { AuthState, LoginCredentials, RegisterCredentials, Role } from '../types/auth'
import { demoUsers, getRoleHomePath, roleLabels } from '../utils/roles'

interface AuthActions {
  loginAsRole: (role: Role) => void
  loginWithPassword: (payload: LoginCredentials) => Promise<Role>
  registerWithPassword: (payload: RegisterCredentials) => Promise<Role>
  logout: () => void
  getRoleHomePath: () => string
}

type AuthStore = AuthState & AuthActions

export const useAuthStore = create<AuthStore>((set, get) => ({
  currentUser: null,
  currentRole: null,
  isAuthenticated: false,
  loginError: null,
  accessToken: null,
  refreshToken: null,
  tokenVersion: null,
  loginAsRole: (role) => {
    set({
      currentUser: demoUsers[role],
      currentRole: role,
      isAuthenticated: true,
      loginError: null,
    })
  },
  loginWithPassword: async (payload) => {
    const tokens = await authService.login(payload)
    const role = tokens.role

    storeAuthTokens(tokens)
    set({
      currentUser: {
        id: payload.account,
        name: payload.account,
        role,
        title: roleLabels[role],
      },
      currentRole: role,
      isAuthenticated: true,
      loginError: null,
      accessToken: tokens.accessToken,
      refreshToken: tokens.refreshToken,
      tokenVersion: tokens.tokenVersion,
    })

    return role
  },
  registerWithPassword: async (payload) => {
    const tokens = await authService.register(payload)
    const role = payload.role

    storeAuthTokens(tokens)
    set({
      currentUser: {
        id: payload.username,
        name: payload.username,
        role,
        title: roleLabels[role],
      },
      currentRole: role,
      isAuthenticated: true,
      loginError: null,
      accessToken: tokens.accessToken,
      refreshToken: tokens.refreshToken,
      tokenVersion: tokens.tokenVersion,
    })

    return role
  },
  logout: () => {
    clearAuthTokens()
    set({
      currentUser: null,
      currentRole: null,
      isAuthenticated: false,
      loginError: null,
      accessToken: null,
      refreshToken: null,
      tokenVersion: null,
    })
  },
  getRoleHomePath: () => {
    const role = get().currentRole

    return role ? getRoleHomePath(role) : '/login'
  },
}))
