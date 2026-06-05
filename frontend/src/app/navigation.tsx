import {
  AuditOutlined,
  CheckSquareOutlined,
  DashboardOutlined,
  FileDoneOutlined,
  FormOutlined,
  InboxOutlined,
  ProjectOutlined,
  ReadOutlined,
  SafetyCertificateOutlined,
} from '@ant-design/icons'
import type { Role } from '../types/auth'
import type { NavItem } from '../types/navigation'

export const roleNavigation: Record<Role, NavItem[]> = {
  ADMIN: [
    {
      key: 'admin-review-assignment',
      label: '审核分配',
      path: '/app/admin',
      role: 'ADMIN',
      icon: <SafetyCertificateOutlined />,
    },
  ],
  OWNER: [
    {
      key: 'owner-dashboard',
      label: '工作台',
      path: '/app/owner',
      role: 'OWNER',
      icon: <DashboardOutlined />,
    },
    {
      key: 'owner-tasks',
      label: '任务管理',
      path: '/app/owner/tasks',
      role: 'OWNER',
      icon: <ProjectOutlined />,
    },
    {
      key: 'owner-templates',
      label: '模板管理',
      path: '/app/owner/templates',
      role: 'OWNER',
      icon: <FormOutlined />,
    },
    {
      key: 'owner-audit',
      label: '导出与审计',
      path: '/app/owner/audit',
      role: 'OWNER',
      icon: <AuditOutlined />,
    },
  ],
  LABELER: [
    {
      key: 'labeler-dashboard',
      label: '工作台',
      path: '/app/labeler',
      role: 'LABELER',
      icon: <DashboardOutlined />,
    },
    {
      key: 'labeler-market',
      label: '任务广场',
      path: '/app/labeler/market',
      role: 'LABELER',
      icon: <InboxOutlined />,
    },
    {
      key: 'labeler-submissions',
      label: '我的领取',
      path: '/app/labeler/submissions',
      role: 'LABELER',
      icon: <FileDoneOutlined />,
    },
  ],
  REVIEWER: [
    {
      key: 'reviewer-dashboard',
      label: '工作台',
      path: '/app/reviewer',
      role: 'REVIEWER',
      icon: <DashboardOutlined />,
    },
    {
      key: 'reviewer-queue',
      label: '审核队列',
      path: '/app/reviewer/queue',
      role: 'REVIEWER',
      icon: <CheckSquareOutlined />,
    },
    {
      key: 'reviewer-history',
      label: '审核历史',
      path: '/app/reviewer/history',
      role: 'REVIEWER',
      icon: <ReadOutlined />,
    },
  ],
}

export function getRoleNavigation(role: Role) {
  return roleNavigation[role]
}

export function getActiveNavKey(role: Role, pathname: string) {
  const items = roleNavigation[role]
  const matchedItem = [...items].reverse().find((item) => pathname.startsWith(item.path))

  return matchedItem?.key ?? items[0]?.key
}
