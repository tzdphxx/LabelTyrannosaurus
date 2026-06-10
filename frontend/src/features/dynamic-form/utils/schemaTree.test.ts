import { describe, expect, it } from 'vitest'
import type { DynamicFormSchema, DynamicSchemaNode } from '../../../types/dynamicForm'
import {
  cloneSchema,
  deleteSchemaNode,
  findSchemaNode,
  getSchemaNodeKeys,
  insertSchemaNode,
  reorderSchemaNodes,
  updateSchemaNode,
  validateDynamicSchema,
  walkSchemaNodes,
} from './schemaTree'

function node(overrides: Partial<DynamicSchemaNode> & Pick<DynamicSchemaNode, 'id' | 'key' | 'type' | 'title'>): DynamicSchemaNode {
  return {
    props: {},
    ...overrides,
  }
}

function schema(nodes: DynamicSchemaNode[]): DynamicFormSchema {
  return {
    id: 'schema-1',
    version: 'v1',
    title: 'Schema',
    nodes,
  }
}

describe('schemaTree', () => {
  it('walks and finds nested nodes with parent context', () => {
    const formSchema = schema([
      node({
        id: 'group-1',
        key: 'group',
        type: 'group',
        title: 'Group',
        children: [node({ id: 'field-1', key: 'field', type: 'input', title: 'Field' })],
      }),
    ])
    const visited: Array<[string, string | null]> = []

    walkSchemaNodes(formSchema.nodes, (current, parent) => {
      visited.push([current.id, parent?.id ?? null])
    })

    expect(visited).toEqual([
      ['group-1', null],
      ['field-1', 'group-1'],
    ])
    expect(findSchemaNode(formSchema, 'field-1')?.key).toBe('field')
    expect(findSchemaNode(formSchema, 'missing')).toBeNull()
  })

  it('clones nested schema nodes without sharing mutable references', () => {
    const formSchema = schema([
      node({
        id: 'field-1',
        key: 'field',
        type: 'select',
        title: 'Field',
        props: { options: [{ label: 'A', value: 'a' }] },
        rules: [{ type: 'required' }],
      }),
    ])

    const cloned = cloneSchema(formSchema)

    expect(cloned).toEqual(formSchema)
    expect(cloned).not.toBe(formSchema)
    expect(cloned.nodes[0]).not.toBe(formSchema.nodes[0])
    expect(cloned.nodes[0].props).not.toBe(formSchema.nodes[0].props)
    expect(cloned.nodes[0].rules).not.toBe(formSchema.nodes[0].rules)
  })

  it('updates, inserts, deletes, and reorders nodes immutably', () => {
    const first = node({ id: 'field-1', key: 'first', type: 'input', title: 'First' })
    const second = node({ id: 'field-2', key: 'second', type: 'input', title: 'Second' })
    const group = node({ id: 'group-1', key: 'group', type: 'group', title: 'Group', children: [first] })
    const formSchema = schema([group, second])

    const updated = updateSchemaNode(formSchema, 'field-1', (current) => ({ ...current, title: 'Updated' }))
    expect(findSchemaNode(updated, 'field-1')?.title).toBe('Updated')
    expect(findSchemaNode(formSchema, 'field-1')?.title).toBe('First')
    expect(updateSchemaNode(formSchema, 'missing', (current) => current)).toBe(formSchema)

    const inserted = insertSchemaNode(formSchema, node({ id: 'field-3', key: 'third', type: 'textarea', title: 'Third' }), 'group-1')
    expect(findSchemaNode(inserted, 'group-1')?.children?.map((child) => child.id)).toEqual(['field-1', 'field-3'])

    const deleted = deleteSchemaNode(inserted, 'field-1')
    expect(findSchemaNode(deleted, 'field-1')).toBeNull()
    expect(findSchemaNode(deleted, 'group-1')?.children?.map((child) => child.id)).toEqual(['field-3'])

    const reordered = reorderSchemaNodes(schema([first, second]), 'field-2', 'field-1')
    expect(reordered.nodes.map((current) => current.id)).toEqual(['field-2', 'field-1'])
  })

  it('returns answer field keys and validates common schema errors', () => {
    const formSchema = schema([
      node({ id: 'field-1', key: 'answer', type: 'input', title: 'Answer' }),
      node({ id: 'display-1', key: 'display', type: 'showItem', title: 'Display' }),
      node({ id: 'prompt-1', key: 'prompt', type: 'llmPrompt', title: 'Prompt' }),
      node({ id: 'group-1', key: 'group', type: 'group', title: 'Group' }),
    ])

    expect(getSchemaNodeKeys(formSchema)).toEqual(['answer'])

    const invalid = schema([
      node({ id: 'field-1', key: 'dup', type: 'input', title: 'First' }),
      node({ id: 'field-2', key: 'dup', type: 'radio', title: 'Second', props: { options: [] } }),
      node({ id: 'tabs-1', key: 'tabs', type: 'tabs', title: 'Tabs', children: [] }),
    ])
    const result = validateDynamicSchema(invalid)

    expect(result.valid).toBe(false)
    expect(result.errors).toHaveLength(3)
  })
})
