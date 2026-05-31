import type { ISchema } from '@formily/react'
import type {
  DynamicConditionRule,
  DynamicFormSchema,
  DynamicLinkageRule,
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
    case 'richText':
      return 'RichTextEditor'
    case 'fileUpload':
      return 'FileUploadField'
    case 'jsonEditor':
      return 'JsonEditorField'
    case 'llmPrompt':
      return 'LlmPromptBlock'
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
  if (node.type === 'checkbox' || node.type === 'select' || node.type === 'fileUpload') {
    return 'array'
  }

  if (node.type === 'group' || node.type === 'tabs' || node.type === 'tabPane' || node.type === 'showItem' || node.type === 'llmPrompt') {
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

function normalizeConditions(rule?: DynamicVisibleRule): DynamicConditionRule[] {
  if (!rule) {
    return []
  }

  if (rule.conditions?.length) {
    return rule.conditions
  }

  if (rule.fieldKey && rule.operator) {
    return [
      {
        fieldKey: rule.fieldKey,
        operator: rule.operator,
        value: rule.value,
      },
    ]
  }

  return []
}

function getDependencies(rule?: DynamicVisibleRule) {
  return [...new Set(normalizeConditions(rule).map((condition) => condition.fieldKey).filter(Boolean))]
}

function toConditionExpression(condition: DynamicConditionRule, index: number) {
  if (condition.operator === 'empty') {
    return `($deps[${index}] === undefined || $deps[${index}] === null || $deps[${index}] === "" || (Array.isArray($deps[${index}]) && $deps[${index}].length === 0))`
  }

  if (condition.operator === 'notEmpty') {
    return `($deps[${index}] !== undefined && $deps[${index}] !== null && $deps[${index}] !== "" && (!Array.isArray($deps[${index}]) || $deps[${index}].length > 0))`
  }

  if (condition.operator === 'equals') {
    return `$deps[${index}] === ${quoteValue(condition.value)}`
  }

  if (condition.operator === 'notEquals') {
    return `$deps[${index}] !== ${quoteValue(condition.value)}`
  }

  return `(Array.isArray($deps[${index}]) ? $deps[${index}].includes(${quoteValue(condition.value)}) : String($deps[${index}] ?? "").includes(String(${quoteValue(
    condition.value,
  )})))`
}

function toRuleExpression(rule?: DynamicVisibleRule) {
  const conditions = normalizeConditions(rule)

  if (!conditions.length) {
    return '{{true}}'
  }

  const joiner = rule?.logic === 'or' ? ' || ' : ' && '
  return `{{${conditions.map((condition, index) => toConditionExpression(condition, index)).join(joiner)}}}`
}

function toStateReaction(rule: DynamicVisibleRule | undefined, stateKey: 'visible' | 'required' | 'disabled') {
  const dependencies = getDependencies(rule)

  if (!dependencies.length) {
    return null
  }

  return {
    dependencies,
    fulfill: {
      state: {
        [stateKey]: toRuleExpression(rule),
      },
    },
    otherwise: {
      state: {
        [stateKey]: false,
      },
    },
  }
}

function toLinkedOptionsReaction(linkage?: DynamicLinkageRule, fallbackOptions?: unknown) {
  const linkedCase = linkage?.linkedOptions?.[0]

  if (!linkedCase) {
    return null
  }

  return {
    dependencies: [linkedCase.when.fieldKey],
    fulfill: {
      state: {
        componentProps: {
          options: `{{${toConditionExpression(linkedCase.when, 0)} ? ${quoteValue(linkedCase.options)} : ${quoteValue(fallbackOptions ?? [])}}}`,
        },
      },
    },
  }
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

  const reactions = [
    toStateReaction(node.visibleWhen, 'visible'),
    toStateReaction(node.linkage?.requiredWhen, 'required'),
    toStateReaction(node.linkage?.disabledWhen, 'disabled'),
    toLinkedOptionsReaction(node.linkage, node.props.options),
  ].filter(Boolean)

  if (reactions.length) {
    schema['x-reactions'] = reactions
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
