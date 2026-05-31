export type Role = 'owner' | 'labeler' | 'reviewer'

export interface User {
  id: string
  name: string
  role: Role
  title: string
}

export interface AuthState {
  currentUser: User | null
  currentRole: Role | null
  isAuthenticated: boolean
  loginError: string | null
}

