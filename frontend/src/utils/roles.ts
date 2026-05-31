import type { Role, User } from '../types/auth'

export const roleLabels: Record<Role, string> = {
  owner: '任务负责人',
  labeler: '标注员',
  reviewer: '审核员',
}

export const roleHomePaths: Record<Role, string> = {
  owner: '/app/owner',
  labeler: '/app/labeler',
  reviewer: '/app/reviewer',
}

export const demoUsers: Record<Role, User> = {
  owner: {
    id: 'owner-demo',
    name: 'Owner Demo',
    role: 'owner',
    title: '任务负责人',
  },
  labeler: {
    id: 'labeler-demo',
    name: 'Labeler Demo',
    role: 'labeler',
    title: '标注员',
  },
  reviewer: {
    id: 'reviewer-demo',
    name: 'Reviewer Demo',
    role: 'reviewer',
    title: '审核员',
  },
}

export function getRoleHomePath(role: Role) {
  return roleHomePaths[role]
}

export function isRolePathAllowed(role: Role, pathname: string) {
  return pathname === roleHomePaths[role] || pathname.startsWith(`${roleHomePaths[role]}/`)
}

