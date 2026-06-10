import type { ReactNode } from 'react'
import { Navigate } from 'react-router'
import { useAuthStore } from '../../stores/authStore'

interface PublicOnlyRouteProps {
  children: ReactNode
}

export function PublicOnlyRoute({ children }: PublicOnlyRouteProps) {
  const isAuthenticated = useAuthStore((state) => state.isAuthenticated)
  const roleHomePath = useAuthStore((state) => state.getRoleHomePath())

  if (isAuthenticated) {
    return <Navigate replace to={roleHomePath} />
  }

  return children
}

