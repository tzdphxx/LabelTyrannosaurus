import type { DynamicFieldType, DynamicFormSchema, DynamicSchemaNode } from '../../../types/dynamicForm'

type BackendSchemaNode = Omit<DynamicSchemaNode, 'children' | 'type'> & {
  type: DynamicFieldType | 'LlmTrigger'
  label?: string
  providerId?: number | string
  modelName?: string
  promptTemplate?: string
  targetFields?: string[]
  children?: BackendSchemaNode[]
}

function asStringArray(value: unknown) {
  return Array.isArray(value) ? value.map((item) => String(item)).filter(Boolean) : []
}

function toBackendNode(node: DynamicSchemaNode): BackendSchemaNode {
  const children = node.children?.map(toBackendNode)

  if (node.type !== 'llmPrompt') {
    return {
      ...node,
      children,
    }
  }

  return {
    ...node,
    type: 'LlmTrigger',
    label: node.title,
    providerId: node.props.providerId,
    modelName: node.props.modelName ? String(node.props.modelName) : undefined,
    promptTemplate: node.props.promptTemplate ? String(node.props.promptTemplate) : String(node.props.prompt ?? ''),
    targetFields: asStringArray(node.props.targetFields),
    children,
  }
}

function fromBackendNode(node: BackendSchemaNode): DynamicSchemaNode {
  const children = node.children?.map(fromBackendNode)

  if (node.type !== 'LlmTrigger') {
    return {
      ...node,
      type: node.type,
      children,
    }
  }

  return {
    ...node,
    type: 'llmPrompt',
    title: node.title || node.label || 'LLM 交互',
    props: {
      ...node.props,
      providerId: node.providerId,
      modelName: node.modelName,
      promptTemplate: node.promptTemplate,
      targetFields: node.targetFields ?? [],
      prompt: node.promptTemplate ?? node.props.prompt,
    },
    children,
  }
}

export function toBackendTemplateSchema(schema: DynamicFormSchema): DynamicFormSchema {
  return {
    ...schema,
    nodes: schema.nodes.map(toBackendNode) as DynamicSchemaNode[],
  }
}

export function fromBackendTemplateSchema(schema: DynamicFormSchema): DynamicFormSchema {
  return {
    ...schema,
    nodes: schema.nodes.map((node) => fromBackendNode(node as BackendSchemaNode)),
  }
}
