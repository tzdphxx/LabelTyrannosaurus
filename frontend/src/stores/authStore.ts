import { create } from 'zustand'
import type { AuthState, Role } from '../types/auth'
import { demoUsers, getRoleHomePath } from '../utils/roles'

interface AuthActions {
  loginAsRole: (role: Role) => void
  logout: () => void
  getRoleHomePath: () => string
}

type AuthStore = AuthState & AuthActions

export const useAuthStore = create<AuthStore>((set, get) => ({
  currentUser: null,
  currentRole: null,
  isAuthenticated: false,
  loginError: null,
  loginAsRole: (role) => {
    set({
      currentUser: demoUsers[role],
      currentRole: role,
      isAuthenticated: true,
      loginError: null,
    })
  },
  logout: () => {
    set({
      currentUser: null,
      currentRole: null,
      isAuthenticated: false,
      loginError: null,
    })
  },
  getRoleHomePath: () => {
    const role = get().currentRole

    return role ? getRoleHomePath(role) : '/login'
  },
}))

