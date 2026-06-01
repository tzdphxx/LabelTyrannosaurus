import type { Role, User } from '../types/auth'

export const roleLabels: Record<Role, string> = {
  OWNER: '任务负责人',
  LABELER: '标注员',
  REVIEWER: '审核员',
}

export const roleHomePaths: Record<Role, string> = {
  OWNER: '/app/owner',
  LABELER: '/app/labeler',
  REVIEWER: '/app/reviewer',
}

export const demoUsers: Record<Role, User> = {
  OWNER: {
    id: 'owner-demo',
    name: 'Owner Demo',
    role: 'OWNER',
    title: '任务负责人',
  },
  LABELER: {
    id: 'labeler-demo',
    name: 'Labeler Demo',
    role: 'LABELER',
    title: '标注员',
  },
  REVIEWER: {
    id: 'reviewer-demo',
    name: 'Reviewer Demo',
    role: 'REVIEWER',
    title: '审核员',
  },
}

export function getRoleHomePath(role: Role) {
  return roleHomePaths[role]
}

export function isRolePathAllowed(role: Role, pathname: string) {
  return pathname === roleHomePaths[role] || pathname.startsWith(`${roleHomePaths[role]}/`)
}
