import type { DynamicFieldType } from '../../../../types/dynamicForm'

export type ActiveDragState =
  | {
      kind: 'material'
      type: DynamicFieldType
      title: string
      description: string
    }
  | {
      kind: 'node'
      title: string
      keyName: string
      type: DynamicFieldType
    }
