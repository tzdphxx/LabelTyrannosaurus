import { useDroppable } from '@dnd-kit/core'
import { SortableContext, verticalListSortingStrategy } from '@dnd-kit/sortable'
import { Empty } from 'antd'
import type { ReactNode } from 'react'
import type { DynamicSchemaNode } from '../../../../types/dynamicForm'

interface CanvasDropZoneProps {
  children?: ReactNode
  nodes: DynamicSchemaNode[]
  parentId: string | null
}

export function CanvasDropZone({ children, nodes, parentId }: CanvasDropZoneProps) {
  const { isOver, setNodeRef } = useDroppable({
    id: parentId ? `container:${parentId}` : 'canvas-root',
    data: {
      type: 'container',
      parentId,
    },
  })

  return (
    <div ref={setNodeRef} className={['designer-canvas-list', isOver ? 'designer-canvas-list--over' : ''].join(' ')}>
      <SortableContext items={nodes.map((node) => node.id)} strategy={verticalListSortingStrategy}>
        {children}
      </SortableContext>
      {!nodes.length ? <Empty description="拖入物料开始搭建" image={Empty.PRESENTED_IMAGE_SIMPLE} /> : null}
    </div>
  )
}
