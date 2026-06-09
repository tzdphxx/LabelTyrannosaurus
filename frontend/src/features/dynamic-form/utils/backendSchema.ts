import type { DynamicFieldOption, DynamicFieldType, DynamicFormSchema, DynamicSchemaNode, DynamicValidationRule } from '../../../types/dynamicForm'

type BackendComponentType = DynamicFieldType | 'LlmTrigger' | 'ShowItem'

interface BackendSchemaComponent {
  id?: string
  key?: string
  type: BackendComponentType
  field?: string
  title?: string
  label?: string
  defaultValue?: unknown
  props?: Record<string, unknown>
  rules?: DynamicValidationRule[]
  visibleWhen?: DynamicSchemaNode['visibleWhen']
  linkage?: DynamicSchemaNode['linkage']
  required?: boolean
  enum?: Array<string | number | boolean>
  regex?: string
  providerId?: number | string
  modelName?: string
  promptTemplate?: string
  targetFields?: string[]
  children?: BackendSchemaComponent[]
  components?: BackendSchemaComponent[]
}

interface BackendTemplateSchema {
  id?: string
  version?: string
  title?: string
  components: BackendSchemaComponent[]
}

const layoutTypes = new Set<DynamicFieldType>(['group', 'tabs', 'tabPane'])
const choiceTypes = new Set<DynamicFieldType>(['radio', 'checkbox', 'select', 'tagSelect'])

function asStringArray(value: unknown) {
  return Array.isArray(value) ? value.map((item) => String(item)).filter(Boolean) : []
}

function isRecord(value: unknown): value is Record<string, unknown> {
  return Boolean(value) && typeof value === 'object' && !Array.isArray(value)
}

function isDynamicFormSchema(value: unknown): value is DynamicFormSchema {
  return (
    isRecord(value) &&
    typeof value.id === 'string' &&
    typeof value.version === 'string' &&
    typeof value.title === 'string' &&
    Array.isArray(value.nodes)
  )
}

function isBackendTemplateSchema(value: unknown): value is BackendTemplateSchema {
  return isRecord(value) && Array.isArray(value.components)
}

function getBackendType(type: DynamicFieldType): BackendComponentType {
  if (type === 'llmPrompt') {
    return 'LlmTrigger'
  }

  if (type === 'showItem') {
    return 'ShowItem'
  }

  return type
}

function getFrontendType(type: BackendComponentType): DynamicFieldType {
  if (type === 'LlmTrigger') {
    return 'llmPrompt'
  }

  if (type === 'ShowItem') {
    return 'showItem'
  }

  return type as DynamicFieldType
}

function getRequired(rules?: DynamicValidationRule[]) {
  return Boolean(rules?.some((rule) => rule.type === 'required'))
}

function isChoiceType(type: DynamicFieldType) {
  return choiceTypes.has(type)
}

function isEnumValue(value: unknown): value is string | number | boolean {
  return typeof value === 'string' || typeof value === 'number' || typeof value === 'boolean'
}

function getOptionValues(options: unknown): Array<string | number | boolean> {
  if (!Array.isArray(options)) {
    return []
  }

  return options
    .map((option) => (isRecord(option) ? option.value : undefined))
    .filter(isEnumValue)
}

function getEnumValues(node: DynamicSchemaNode) {
  if (isChoiceType(node.type) && node.props.options?.length) {
    return node.props.options.map((option) => option.value)
  }

  const enumRule = node.rules?.find((rule): rule is Extract<DynamicValidationRule, { type: 'enum' }> => rule.type === 'enum')

  if (enumRule) {
    return enumRule.values
  }

  return node.props.options?.map((option) => option.value)
}

function toBackendNode(node: DynamicSchemaNode): BackendSchemaComponent {
  const children = node.children?.map(toBackendNode)
  const backendType = getBackendType(node.type)
  const props = { ...node.props }
  const enumValues = getEnumValues(node)
  const component: BackendSchemaComponent = {
    id: node.id,
    key: node.key,
    type: backendType,
    title: node.title,
    label: node.title,
    defaultValue: node.defaultValue,
    props,
    rules: node.rules,
    visibleWhen: node.visibleWhen,
    linkage: node.linkage,
    required: getRequired(node.rules),
  }

  if (!layoutTypes.has(node.type)) {
    component.field = node.key
  }

  if (enumValues?.length) {
    component.enum = enumValues
  }

  if (node.type === 'llmPrompt') {
    component.providerId = node.props.providerId
    component.modelName = node.props.modelName ? String(node.props.modelName) : undefined
    component.promptTemplate = node.props.promptTemplate ? String(node.props.promptTemplate) : String(node.props.prompt ?? '')
    component.targetFields = asStringArray(node.props.targetFields)
  }

  if (children?.length || layoutTypes.has(node.type)) {
    component.children = children ?? []
  }

  return component
}

function toOptions(values: unknown): DynamicFieldOption[] | undefined {
  if (!Array.isArray(values)) {
    return undefined
  }

  return values.map((value) => ({
    label: String(value),
    value: String(value),
  }))
}

function fromBackendNode(component: BackendSchemaComponent): DynamicSchemaNode {
  const type = getFrontendType(component.type)
  const children = (component.children ?? component.components)?.map(fromBackendNode)
  const props = isRecord(component.props) ? { ...component.props } : {}
  const field = component.field ?? component.key ?? component.id ?? `field-${Math.random().toString(36).slice(2, 8)}`
  const rules: DynamicValidationRule[] = Array.isArray(component.rules) ? [...component.rules] : []

  if (component.required && !rules.some((rule) => rule.type === 'required')) {
    rules.push({ type: 'required' })
  }

  if (component.enum?.length && !rules.some((rule) => rule.type === 'enum')) {
    rules.push({ type: 'enum', values: component.enum })
    props.options = props.options ?? toOptions(component.enum)
  }

  if (isChoiceType(type)) {
    const optionValues = getOptionValues(props.options)
    const enumRuleIndex = rules.findIndex((rule) => rule.type === 'enum')
    const enumRule = rules[enumRuleIndex]

    if (optionValues.length && enumRule?.type === 'enum') {
      rules[enumRuleIndex] = { ...enumRule, values: optionValues }
    }
  }

  if (type === 'llmPrompt') {
    props.providerId = component.providerId
    props.modelName = component.modelName
    props.promptTemplate = component.promptTemplate
    props.prompt = component.promptTemplate ?? props.prompt
    props.targetFields = component.targetFields ?? []
  }

  return {
    id: component.id ?? field,
    key: component.key ?? field,
    type,
    title: component.title ?? component.label ?? field,
    defaultValue: component.defaultValue,
    props,
    rules: rules.length ? rules : undefined,
    visibleWhen: component.visibleWhen,
    linkage: component.linkage,
    children,
  }
}

export function toBackendTemplateSchema(schema: DynamicFormSchema): BackendTemplateSchema {
  return {
    id: schema.id,
    version: schema.version,
    title: schema.title,
    components: schema.nodes.map(toBackendNode),
  }
}

export function fromBackendTemplateSchema(schema: unknown): DynamicFormSchema {
  if (isDynamicFormSchema(schema)) {
    return {
      ...schema,
      nodes: schema.nodes.map((node) => fromBackendNode(toBackendNode(node))),
    }
  }

  if (isBackendTemplateSchema(schema)) {
    return {
      id: schema.id ?? `schema-${Date.now()}`,
      version: schema.version ?? 'v0.1',
      title: schema.title ?? '模板',
      nodes: schema.components.map(fromBackendNode),
    }
  }

  return {
    id: `schema-${Date.now()}`,
    version: 'v0.1',
    title: '模板',
    nodes: [],
  }
}
