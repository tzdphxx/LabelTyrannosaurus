import { Tag, Typography } from 'antd'
import { dynamicMaterialRegistry } from '../../materialRegistry'
import type { ActiveDragState } from './types'

export function DragPreview({ activeDrag }: { activeDrag: ActiveDragState | null }) {
  if (!activeDrag) {
    return null
  }

  if (activeDrag.kind === 'material') {
    return (
      <div className="designer-drag-overlay">
        <Typography.Text strong>{activeDrag.title}</Typography.Text>
        <Typography.Text type="secondary">{activeDrag.description}</Typography.Text>
      </div>
    )
  }

  return (
    <div className="designer-drag-overlay">
      <Typography.Text strong>{activeDrag.title}</Typography.Text>
      <Typography.Text type="secondary">{activeDrag.keyName}</Typography.Text>
      <Tag>{dynamicMaterialRegistry[activeDrag.type].title}</Tag>
    </div>
  )
}
