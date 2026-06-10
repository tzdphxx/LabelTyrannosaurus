import { describe, expect, it, vi } from 'vitest'
import type { DynamicFormSchema, DynamicSchemaNode } from '../../../types/dynamicForm'
import { schemaToFormilySchema } from './formilySchema'

type TestSchemaNode = Record<string, unknown>
type TestSchemaProperties = Record<string, TestSchemaNode>

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

describe('formilySchema', () => {
  it('maps dynamic field nodes to Formily schema nodes', () => {
    const result = schemaToFormilySchema(
      schema([
        node({
          id: 'answer-1',
          key: 'answer',
          type: 'textarea',
          title: 'Answer',
          defaultValue: 'initial',
          props: { placeholder: 'Input answer' },
          rules: [{ type: 'required', message: 'Required' }],
        }),
      ]),
    )
    const properties = result.properties as TestSchemaProperties

    expect(properties.answer).toMatchObject({
      type: 'string',
      title: 'Answer',
      default: 'initial',
      'x-decorator': 'FormItem',
      'x-component': 'Input.TextArea',
      'x-component-props': {
        componentId: 'answer-1',
        placeholder: 'Input answer',
        title: 'Answer',
      },
      'x-validator': [{ required: true, message: 'Required' }],
    })
  })

  it('builds visibility, required, disabled, and linked options reactions', () => {
    const result = schemaToFormilySchema(
      schema([
        node({
          id: 'field-1',
          key: 'status',
          type: 'select',
          title: 'Status',
          props: { options: [{ label: 'Default', value: 'default' }] },
          visibleWhen: { fieldKey: 'type', operator: 'equals', value: 'task' },
          linkage: {
            requiredWhen: { fieldKey: 'priority', operator: 'notEmpty' },
            disabledWhen: { fieldKey: 'locked', operator: 'equals', value: true },
            linkedOptions: [
              {
                when: { fieldKey: 'type', operator: 'equals', value: 'review' },
                options: [{ label: 'Review', value: 'review' }],
              },
            ],
          },
        }),
      ]),
    )

    const properties = result.properties as TestSchemaProperties
    const reactions = properties.status['x-reactions'] as unknown[] | undefined
    const visibleReaction = reactions?.[0] as { fulfill: { state: { visible: string } } } | undefined

    expect(reactions).toHaveLength(4)
    expect(reactions?.[0]).toMatchObject({ dependencies: ['type'] })
    expect(visibleReaction?.fulfill.state.visible).toBe('{{$deps[0] === "task"}}')
    expect(JSON.stringify(reactions)).toContain('required')
    expect(JSON.stringify(reactions)).toContain('disabled')
    expect(JSON.stringify(reactions)).toContain('componentProps')
    expect(JSON.stringify(reactions)).toContain('Review')
  })

  it('injects runtime props into llm prompt components', () => {
    const getCurrentValues = vi.fn()
    const onApplyLlmValues = vi.fn()
    const onRunLlmTrigger = vi.fn()
    const llmContext = { previewMode: true, taskId: 1 }
    const result = schemaToFormilySchema(
      schema([
        node({
          id: 'prompt-1',
          key: 'prompt',
          type: 'llmPrompt',
          title: 'Prompt',
          props: { targetFields: ['answer'] },
        }),
      ]),
      {
        answerFieldKeys: ['answer'],
        getCurrentValues,
        llmContext,
        onApplyLlmValues,
        onRunLlmTrigger,
      },
    )

    const properties = result.properties as TestSchemaProperties

    expect(properties.prompt).toMatchObject({
      type: 'void',
      'x-component': 'LlmPromptBlock',
      'x-component-props': {
        answerFieldKeys: ['answer'],
        componentId: 'prompt-1',
        llmContext,
        onApplyValues: onApplyLlmValues,
        onRunLlmTrigger,
      },
    })
  })
})
