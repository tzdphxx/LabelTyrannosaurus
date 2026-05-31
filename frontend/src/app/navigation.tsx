import {
  AuditOutlined,
  CheckSquareOutlined,
  DashboardOutlined,
  FileDoneOutlined,
  FormOutlined,
  InboxOutlined,
  ProfileOutlined,
  ProjectOutlined,
  ReadOutlined,
} from '@ant-design/icons'
import type { Role } from '../types/auth'
import type { NavItem } from '../types/navigation'

export const roleNavigation: Record<Role, NavItem[]> = {
  owner: [
    {
      key: 'owner-dashboard',
      label: '工作台',
      path: '/app/owner',
      role: 'owner',
      icon: <DashboardOutlined />,
    },
    {
      key: 'owner-tasks',
      label: '任务管理',
      path: '/app/owner/tasks',
      role: 'owner',
      icon: <ProjectOutlined />,
    },
    {
      key: 'owner-templates',
      label: '模板管理',
      path: '/app/owner/templates',
      role: 'owner',
      icon: <FormOutlined />,
    },
    {
      key: 'owner-audit',
      label: '导出与审计',
      path: '/app/owner/audit',
      role: 'owner',
      icon: <AuditOutlined />,
    },
  ],
  labeler: [
    {
      key: 'labeler-dashboard',
      label: '工作台',
      path: '/app/labeler',
      role: 'labeler',
      icon: <DashboardOutlined />,
    },
    {
      key: 'labeler-market',
      label: '任务广场',
      path: '/app/labeler/market',
      role: 'labeler',
      icon: <InboxOutlined />,
    },
    {
      key: 'labeler-submissions',
      label: '我的提交',
      path: '/app/labeler/submissions',
      role: 'labeler',
      icon: <FileDoneOutlined />,
    },
  ],
  reviewer: [
    {
      key: 'reviewer-dashboard',
      label: '工作台',
      path: '/app/reviewer',
      role: 'reviewer',
      icon: <DashboardOutlined />,
    },
    {
      key: 'reviewer-queue',
      label: '审核队列',
      path: '/app/reviewer/queue',
      role: 'reviewer',
      icon: <CheckSquareOutlined />,
    },
    {
      key: 'reviewer-history',
      label: '审核历史',
      path: '/app/reviewer/history',
      role: 'reviewer',
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

