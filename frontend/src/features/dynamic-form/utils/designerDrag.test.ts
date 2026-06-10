import { describe, expect, it } from 'vitest'
import type { DynamicFormSchema } from '../../../types/dynamicForm'
import { createActiveDragState } from './designerDrag'

describe('designerDrag', () => {
  const schema: DynamicFormSchema = {
    id: 'schema-1',
    version: 'v1',
    title: 'Schema',
    nodes: [
      {
        id: 'field-1',
        key: 'answer',
        type: 'input',
        title: 'Answer',
        props: {},
      },
    ],
  }

  it('creates active state for palette materials', () => {
    expect(createActiveDragState(schema, { type: 'material', fieldType: 'textarea' })).toMatchObject({
      kind: 'material',
      type: 'textarea',
    })
  })

  it('creates active state for existing schema nodes', () => {
    expect(createActiveDragState(schema, { type: 'node', nodeId: 'field-1', parentId: null })).toEqual({
      kind: 'node',
      title: 'Answer',
      keyName: 'answer',
      type: 'input',
    })
  })

  it('returns null for missing drag data or missing nodes', () => {
    expect(createActiveDragState(schema, undefined)).toBeNull()
    expect(createActiveDragState(null, { type: 'node', nodeId: 'field-1', parentId: null })).toBeNull()
    expect(createActiveDragState(schema, { type: 'node', nodeId: 'missing', parentId: null })).toBeNull()
  })
})
