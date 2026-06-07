import { Empty } from 'antd'
import type { RefObject } from 'react'
import type { DynamicFormSchema } from '../../../../types/dynamicForm'
import { CanvasDropZone } from './CanvasDropZone'
import { CanvasNodeCard } from './CanvasNodeCard'

interface DesignerCanvasProps {
  schema: DynamicFormSchema | null
  scrollRef: RefObject<HTMLDivElement | null>
  selectedNodeId: string | null
  onDelete: (nodeId: string) => void
  onSelect: (nodeId: string) => void
}

export function DesignerCanvas({ schema, scrollRef, selectedNodeId, onDelete, onSelect }: DesignerCanvasProps) {
  if (!schema) {
    return <Empty description="模板 schema 暂不可用" />
  }

  return (
    <div ref={scrollRef} className="designer-canvas-scroll">
      <CanvasDropZone nodes={schema.nodes} parentId={null}>
        {schema.nodes.map((node) => (
          <CanvasNodeCard
            key={node.id}
            node={node}
            parentId={null}
            selectedNodeId={selectedNodeId}
            onDelete={onDelete}
            onSelect={onSelect}
          />
        ))}
      </CanvasDropZone>
    </div>
  )
}
