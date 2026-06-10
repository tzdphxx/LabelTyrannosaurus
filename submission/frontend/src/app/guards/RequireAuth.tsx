import type { ReactNode } from 'react'
import { Navigate, useLocation } from 'react-router'
import { useAuthStore } from '../../stores/authStore'
import { getRoleHomePath, isRolePathAllowed } from '../../utils/roles'

interface RequireAuthProps {
  children: ReactNode
}

export function RequireAuth({ children }: RequireAuthProps) {
  const location = useLocation()
  const isAuthenticated = useAuthStore((state) => state.isAuthenticated)
  const currentRole = useAuthStore((state) => state.currentRole)

  if (!isAuthenticated || !currentRole) {
    return <Navigate replace state={{ from: location }} to="/login" />
  }

  if (!isRolePathAllowed(currentRole, location.pathname)) {
    return <Navigate replace to={getRoleHomePath(currentRole)} />
  }

  return children
}

