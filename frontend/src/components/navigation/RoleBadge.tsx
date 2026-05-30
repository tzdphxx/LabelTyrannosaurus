import { Tag } from 'antd'
import type { Role } from '../../types/auth'
import { roleLabels } from '../../utils/roles'

const roleColors: Record<Role, string> = {
  owner: 'blue',
  labeler: 'green',
  reviewer: 'gold',
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

