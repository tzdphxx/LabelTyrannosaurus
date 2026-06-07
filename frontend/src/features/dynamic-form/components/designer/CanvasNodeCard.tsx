import { useSortable } from '@dnd-kit/sortable'
import { CSS } from '@dnd-kit/utilities'
import { DeleteOutlined, DragOutlined } from '@ant-design/icons'
import { Button, Tag, Typography } from 'antd'
import type { DynamicSchemaNode } from '../../../../types/dynamicForm'
import { dynamicMaterialRegistry } from '../../materialRegistry'
import { CanvasDropZone } from './CanvasDropZone'

interface CanvasNodeCardProps {
  node: DynamicSchemaNode
  parentId: string | null
  selectedNodeId: string | null
  onDelete: (nodeId: string) => void
  onSelect: (nodeId: string) => void
}

export function CanvasNodeCard({ node, parentId, selectedNodeId, onDelete, onSelect }: CanvasNodeCardProps) {
  const { attributes, listeners, setNodeRef, transform, transition } = useSortable({
    id: node.id,
    data: {
      type: 'node',
      nodeId: node.id,
      parentId,
    },
  })
  const definition = dynamicMaterialRegistry[node.type]
  const style = {
    transform: CSS.Transform.toString(transform),
    transition,
  }

  return (
    <div
      ref={setNodeRef}
      className={['designer-node', selectedNodeId === node.id ? 'designer-node--selected' : ''].join(' ')}
      data-node-id={node.id}
      onClick={(event) => {
        event.stopPropagation()
        onSelect(node.id)
      }}
      style={style}
    >
      <div className="designer-node__head">
        <button className="designer-node__drag" type="button" {...attributes} {...listeners}>
          <DragOutlined />
        </button>
        <div className="designer-node__title">
          <Typography.Text strong>{node.title}</Typography.Text>
          <Typography.Text type="secondary">{node.key}</Typography.Text>
        </div>
        <Tag>{definition.title}</Tag>
        <Button
          danger
          icon={<DeleteOutlined />}
          onClick={(event) => {
            event.stopPropagation()
            onDelete(node.id)
          }}
          onPointerDown={(event) => event.stopPropagation()}
          size="small"
          type="text"
        />
      </div>

      {node.children ? (
        <CanvasDropZone nodes={node.children} parentId={node.id}>
          {node.children.map((child) => (
            <CanvasNodeCard
              key={child.id}
              node={child}
              parentId={node.id}
              selectedNodeId={selectedNodeId}
              onDelete={onDelete}
              onSelect={onSelect}
            />
          ))}
        </CanvasDropZone>
      ) : null}
    </div>
  )
}
