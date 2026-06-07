import type { ActiveDragState } from '../components/designer'
import { dynamicMaterialRegistry } from '../materialRegistry'
import type { DynamicFieldType, DynamicFormSchema } from '../../../types/dynamicForm'
import { findSchemaNode } from './schemaTree'

export type DesignerDragData =
  | { type: 'material'; fieldType: DynamicFieldType }
  | { type: 'node'; nodeId: string; parentId: string | null }

export function createActiveDragState(schema: DynamicFormSchema | null, dragData: DesignerDragData | undefined): ActiveDragState | null {
  if (!dragData) {
    return null
  }

  if (dragData.type === 'material') {
    const definition = dynamicMaterialRegistry[dragData.fieldType]

    return {
      kind: 'material',
      type: dragData.fieldType,
      title: definition.title,
      description: definition.description,
    }
  }

  const node = schema ? findSchemaNode(schema, dragData.nodeId) : null

  return node
    ? {
        kind: 'node',
        title: node.title,
        keyName: node.key,
        type: node.type,
      }
    : null
}
