import type { ReactNode } from 'react'
import type { Role } from './auth'

export interface NavItem {
  key: string
  label: string
  path: string
  role: Role
  icon?: ReactNode
}

