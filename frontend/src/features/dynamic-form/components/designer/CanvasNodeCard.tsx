import { DeleteOutlined, DragOutlined, PlusOutlined } from '@ant-design/icons'
import { useSortable } from '@dnd-kit/sortable'
import { CSS } from '@dnd-kit/utilities'
import { Button, Typography } from 'antd'
import { memo } from 'react'
import type { DynamicSchemaNode } from '../../../../types/dynamicForm'
import { dynamicMaterialRegistry } from '../../materialRegistry'
import { CanvasFieldPreview } from './CanvasFieldPreview'
import { CanvasDropZone } from './CanvasDropZone'

interface CanvasNodeCardProps {
  node: DynamicSchemaNode
  parentId: string | null
  selectedNodeId: string | null
  onDelete: (nodeId: string) => void
  onAddTabPane: (parentId: string) => void
  onSelect: (nodeId: string) => void
}

function CanvasNodeCardComponent({ node, parentId, selectedNodeId, onDelete, onAddTabPane, onSelect }: CanvasNodeCardProps) {
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
      <div className="designer-node__toolbar">
        <button aria-label="拖拽字段" className="designer-node__drag" type="button" {...attributes} {...listeners}>
          <DragOutlined />
        </button>
        <Typography.Text className="designer-node__type" type="secondary">
          {definition.title}
        </Typography.Text>
        {node.type === 'tabs' ? (
          <Button
            aria-label="新增 Tab"
            icon={<PlusOutlined />}
            onClick={(event) => {
              event.stopPropagation()
              onAddTabPane(node.id)
            }}
            onPointerDown={(event) => event.stopPropagation()}
            size="small"
            type="text"
          />
        ) : null}
        <Button
          aria-label="删除字段"
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

      <div className="designer-node__content">
        <CanvasFieldPreview node={node}>
          {node.children ? (
            <CanvasDropZone nodes={node.children} parentId={node.id}>
              {node.children.map((child) => (
                <CanvasNodeCard
                  key={child.id}
                  node={child}
                  parentId={node.id}
                  selectedNodeId={selectedNodeId}
                  onAddTabPane={onAddTabPane}
                  onDelete={onDelete}
                  onSelect={onSelect}
                />
              ))}
            </CanvasDropZone>
          ) : null}
        </CanvasFieldPreview>
      </div>
    </div>
  )
}

function areCanvasNodeCardPropsEqual(previous: CanvasNodeCardProps, next: CanvasNodeCardProps) {
  const previousSelected = previous.selectedNodeId === previous.node.id
  const nextSelected = next.selectedNodeId === next.node.id

  return (
    previous.node === next.node &&
    previous.parentId === next.parentId &&
    previous.onDelete === next.onDelete &&
    previous.onAddTabPane === next.onAddTabPane &&
    previous.onSelect === next.onSelect &&
    previousSelected === nextSelected
  )
}

export const CanvasNodeCard = memo(CanvasNodeCardComponent, areCanvasNodeCardPropsEqual)
