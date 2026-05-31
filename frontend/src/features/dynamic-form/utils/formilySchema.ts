import type { ISchema } from '@formily/react'
import type {
  DynamicFormSchema,
  DynamicSchemaNode,
  DynamicValidationRule,
  DynamicVisibleRule,
} from '../../../types/dynamicForm'

function getComponentName(node: DynamicSchemaNode) {
  switch (node.type) {
    case 'input':
      return 'Input'
    case 'textarea':
      return 'Input.TextArea'
    case 'radio':
      return 'Radio.Group'
    case 'checkbox':
      return 'Checkbox.Group'
    case 'select':
      return 'Select'
    case 'showItem':
      return 'ShowItem'
    case 'group':
      return 'GroupSection'
    case 'tabs':
      return 'TabsSection'
    case 'tabPane':
      return 'TabPaneSection'
    default:
      return 'Input'
  }
}

function getSchemaType(node: DynamicSchemaNode) {
  if (node.type === 'checkbox' || node.type === 'select') {
    return 'array'
  }

  if (node.type === 'group' || node.type === 'tabs' || node.type === 'tabPane' || node.type === 'showItem') {
    return 'void'
  }

  return 'string'
}

function toValidator(rule: DynamicValidationRule, node: DynamicSchemaNode) {
  if (rule.type === 'required') {
    return {
      required: true,
      message: rule.message ?? `${node.title}不能为空`,
    }
  }

  if (rule.type === 'minLength') {
    return {
      min: rule.value,
      message: rule.message ?? `${node.title}长度不能小于 ${rule.value}`,
    }
  }

  if (rule.type === 'maxLength') {
    return {
      max: rule.value,
      message: rule.message ?? `${node.title}长度不能大于 ${rule.value}`,
    }
  }

  return {
    enum: rule.values,
    message: rule.message ?? `${node.title}不在允许范围内`,
  }
}

function quoteValue(value: unknown) {
  return JSON.stringify(value)
}

function toVisibleExpression(rule: DynamicVisibleRule) {
  if (rule.operator === 'empty') {
    return '{{$deps[0] === undefined || $deps[0] === null || $deps[0] === "" || (Array.isArray($deps[0]) && $deps[0].length === 0)}}'
  }

  if (rule.operator === 'notEmpty') {
    return '{{$deps[0] !== undefined && $deps[0] !== null && $deps[0] !== "" && (!Array.isArray($deps[0]) || $deps[0].length > 0)}}'
  }

  if (rule.operator === 'equals') {
    return `{{$deps[0] === ${quoteValue(rule.value)}}}`
  }

  if (rule.operator === 'notEquals') {
    return `{{$deps[0] !== ${quoteValue(rule.value)}}}`
  }

  return `{{Array.isArray($deps[0]) ? $deps[0].includes(${quoteValue(rule.value)}) : String($deps[0] ?? "").includes(String(${quoteValue(
    rule.value,
  )}))}}`
}

function toNodeSchema(node: DynamicSchemaNode): ISchema {
  const schema: ISchema = {
    type: getSchemaType(node),
    title: node.title,
    'x-decorator': node.type === 'showItem' || node.type === 'group' || node.type === 'tabs' || node.type === 'tabPane' ? undefined : 'FormItem',
    'x-component': getComponentName(node),
    'x-component-props': {
      ...node.props,
      title: node.title,
    },
  }

  if (node.defaultValue !== undefined) {
    schema.default = node.defaultValue
  }

  if (node.rules?.length) {
    schema['x-validator'] = node.rules.map((rule) => toValidator(rule, node))
  }

  if (node.visibleWhen) {
    schema['x-reactions'] = {
      dependencies: [node.visibleWhen.fieldKey],
      fulfill: {
        state: {
          visible: toVisibleExpression(node.visibleWhen),
        },
      },
      otherwise: {
        state: {
          visible: false,
        },
      },
    }
  }

  if (node.children?.length) {
    schema.properties = Object.fromEntries(node.children.map((child) => [child.key, toNodeSchema(child)]))
  }

  return schema
}

export function schemaToFormilySchema(schema: DynamicFormSchema): ISchema {
  return {
    type: 'object',
    properties: Object.fromEntries(schema.nodes.map((node) => [node.key, toNodeSchema(node)])),
  }
}
