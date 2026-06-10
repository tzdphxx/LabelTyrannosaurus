import { create } from 'zustand'
import {
  AUTH_ROLE_STORAGE_KEY,
  AUTH_TOKEN_STORAGE_KEY,
  AUTH_USER_STORAGE_KEY,
  REFRESH_TOKEN_STORAGE_KEY,
  TOKEN_VERSION_STORAGE_KEY,
  authService,
  clearAuthTokens,
  storeAuthTokens,
} from '../services'
import type { AuthState, LoginCredentials, RegisterCredentials, Role, User } from '../types/auth'
import { demoUsers, getRoleHomePath, roleLabels } from '../utils/roles'

interface AuthActions {
  loginAsRole: (role: Role) => void
  loginWithPassword: (payload: LoginCredentials) => Promise<Role>
  registerWithPassword: (payload: RegisterCredentials) => Promise<Role>
  logout: () => void
  getRoleHomePath: () => string
}

type AuthStore = AuthState & AuthActions

const roles: Role[] = ['ADMIN', 'OWNER', 'LABELER', 'REVIEWER']

function isRole(value: string | null): value is Role {
  return Boolean(value && roles.includes(value as Role))
}

function readStorageValue(key: string) {
  if (typeof window === 'undefined') {
    return null
  }

  try {
    return window.localStorage.getItem(key)
  } catch {
    return null
  }
}

function storeAuthIdentity(role: Role, user: User) {
  if (typeof window === 'undefined') {
    return
  }

  window.localStorage.setItem(AUTH_ROLE_STORAGE_KEY, role)
  window.localStorage.setItem(AUTH_USER_STORAGE_KEY, JSON.stringify(user))
}

function readStoredUser(role: Role): User {
  const rawUser = readStorageValue(AUTH_USER_STORAGE_KEY)

  if (!rawUser) {
    return demoUsers[role]
  }

  try {
    const user = JSON.parse(rawUser) as User

    if (user?.role === role && user.id && user.name) {
      return user
    }
  } catch {
    return demoUsers[role]
  }

  return demoUsers[role]
}

function getInitialAuthState(): AuthState {
  const accessToken = readStorageValue(AUTH_TOKEN_STORAGE_KEY)
  const refreshToken = readStorageValue(REFRESH_TOKEN_STORAGE_KEY)
  const tokenVersionText = readStorageValue(TOKEN_VERSION_STORAGE_KEY)
  const role = readStorageValue(AUTH_ROLE_STORAGE_KEY)

  if (!accessToken || !refreshToken || !isRole(role)) {
    return {
      currentUser: null,
      currentRole: null,
      isAuthenticated: false,
      loginError: null,
      accessToken: null,
      refreshToken: null,
      tokenVersion: null,
    }
  }

  const tokenVersion = Number(tokenVersionText)

  return {
    currentUser: readStoredUser(role),
    currentRole: role,
    isAuthenticated: true,
    loginError: null,
    accessToken,
    refreshToken,
    tokenVersion: Number.isFinite(tokenVersion) ? tokenVersion : null,
  }
}

const initialAuthState = getInitialAuthState()

export const useAuthStore = create<AuthStore>((set, get) => ({
  ...initialAuthState,
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
    const currentUser = {
      id: payload.account,
      name: payload.account,
      role,
      title: roleLabels[role],
    }

    storeAuthTokens(tokens)
    storeAuthIdentity(role, currentUser)
    set({
      currentUser,
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
    const currentUser = {
      id: payload.username,
      name: payload.username,
      role,
      title: roleLabels[role],
    }

    storeAuthTokens(tokens)
    storeAuthIdentity(role, currentUser)
    set({
      currentUser,
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
