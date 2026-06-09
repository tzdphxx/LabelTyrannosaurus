import { Tag } from 'antd'
import type { Role } from '../../types/auth'
import { roleLabels } from '../../utils/roles'

const roleColors: Record<Role, string> = {
  ADMIN: 'red',
  OWNER: 'blue',
  LABELER: 'green',
  REVIEWER: 'gold',
}

interface RoleBadgeProps {
  role: Role
}

export function RoleBadge({ role }: RoleBadgeProps) {
  return (
    <Tag className="role-badge" color={roleColors[role]}>
      {roleLabels[role]}
    </Tag>
  )
}
