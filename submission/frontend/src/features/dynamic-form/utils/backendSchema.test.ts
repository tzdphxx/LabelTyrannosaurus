import { describe, expect, it, vi } from 'vitest'
import type { DynamicFormSchema, DynamicSchemaNode } from '../../../types/dynamicForm'
import { fromBackendTemplateSchema, toBackendTemplateSchema } from './backendSchema'

function node(overrides: Partial<DynamicSchemaNode> & Pick<DynamicSchemaNode, 'id' | 'key' | 'type' | 'title'>): DynamicSchemaNode {
  return {
    props: {},
    ...overrides,
  }
}

describe('backendSchema', () => {
  it('maps frontend schema to backend component schema', () => {
    const schema: DynamicFormSchema = {
      id: 'schema-1',
      version: 'v1',
      title: 'Schema',
      nodes: [
        node({
          id: 'field-1',
          key: 'answer',
          type: 'radio',
          title: 'Answer',
          props: { options: [{ label: 'Yes', value: 'yes' }] },
          rules: [{ type: 'required' }],
        }),
        node({
          id: 'prompt-1',
          key: 'prompt',
          type: 'llmPrompt',
          title: 'Prompt',
          props: {
            modelName: 'gpt-4o',
            promptTemplate: 'Clean answer',
            providerId: 12,
            targetFields: ['answer'],
          },
        }),
      ],
    }

    expect(toBackendTemplateSchema(schema)).toMatchObject({
      id: 'schema-1',
      version: 'v1',
      title: 'Schema',
      components: [
        {
          field: 'answer',
          key: 'answer',
          required: true,
          enum: ['yes'],
          type: 'radio',
        },
        {
          field: 'prompt',
          modelName: 'gpt-4o',
          promptTemplate: 'Clean answer',
          providerId: 12,
          targetFields: ['answer'],
          type: 'LlmTrigger',
        },
      ],
    })
  })

  it('maps backend component schema to frontend schema', () => {
    const converted = fromBackendTemplateSchema({
      id: 'schema-2',
      version: 'v2',
      title: 'Backend Schema',
      components: [
        {
          id: 'field-1',
          field: 'status',
          title: 'Status',
          type: 'select',
          required: true,
          enum: ['open', 'closed'],
        },
        {
          id: 'display-1',
          key: 'display',
          label: 'Display',
          type: 'ShowItem',
          props: { text: 'Readonly' },
        },
      ],
    })

    expect(converted).toMatchObject({
      id: 'schema-2',
      version: 'v2',
      title: 'Backend Schema',
      nodes: [
        {
          id: 'field-1',
          key: 'status',
          type: 'select',
          props: {
            options: [
              { label: 'open', value: 'open' },
              { label: 'closed', value: 'closed' },
            ],
          },
          rules: [{ type: 'required' }, { type: 'enum', values: ['open', 'closed'] }],
        },
        {
          id: 'display-1',
          key: 'display',
          type: 'showItem',
        },
      ],
    })
  })

  it('normalizes frontend schema and handles invalid inputs', () => {
    vi.spyOn(Date, 'now').mockReturnValue(1000)

    const frontendSchema = fromBackendTemplateSchema({
      id: 'schema-3',
      version: 'v3',
      title: 'Frontend Schema',
      nodes: [
        node({
          id: 'field-1',
          key: 'choice',
          type: 'radio',
          title: 'Choice',
          props: { options: [{ label: 'A', value: 'a' }] },
          rules: [{ type: 'enum', values: ['stale'] }],
        }),
      ],
    })

    expect(frontendSchema.nodes[0].rules).toEqual([{ type: 'enum', values: ['a'] }])
    expect(fromBackendTemplateSchema(null)).toMatchObject({
      id: 'schema-1000',
      version: 'v0.1',
      nodes: [],
    })
  })
})
