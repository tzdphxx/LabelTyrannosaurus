import { useLocation } from 'react-router'
import { getActiveNavKey, getRoleNavigation } from '../app/navigation'
import { useAuthStore } from '../stores/authStore'

export function useRoleNavigation() {
  const location = useLocation()
  const role = useAuthStore((state) => state.currentRole)

  if (!role) {
    return {
      activeKey: '',
      items: [],
    }
  }

  return {
    activeKey: getActiveNavKey(role, location.pathname),
    items: getRoleNavigation(role),
  }
}

