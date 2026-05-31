export type DynamicFieldType =
  | 'input'
  | 'textarea'
  | 'radio'
  | 'checkbox'
  | 'select'
  | 'showItem'
  | 'richText'
  | 'fileUpload'
  | 'jsonEditor'
  | 'llmPrompt'
  | 'group'
  | 'tabs'
  | 'tabPane'

export type DynamicMaterialGroup = 'text' | 'choice' | 'display' | 'media' | 'smart' | 'layout'

export type DynamicVisibleOperator = 'equals' | 'notEquals' | 'contains' | 'empty' | 'notEmpty'
export type DynamicConditionLogic = 'and' | 'or'

export type DynamicValidationRule =
  | {
      type: 'required'
      message?: string
    }
  | {
      type: 'minLength'
      value: number
      message?: string
    }
  | {
      type: 'maxLength'
      value: number
      message?: string
    }
  | {
      type: 'enum'
      values: Array<string | number | boolean>
      message?: string
    }

export interface DynamicConditionRule {
  fieldKey: string
  operator: DynamicVisibleOperator
  value?: string | number | boolean | Array<string | number | boolean>
}

export interface DynamicVisibleRule extends Partial<DynamicConditionRule> {
  logic?: DynamicConditionLogic
  conditions?: DynamicConditionRule[]
}

export interface DynamicLinkedOptionsCase {
  when: DynamicConditionRule
  options: DynamicFieldOption[]
}

export interface DynamicLinkageRule {
  requiredWhen?: DynamicVisibleRule
  disabledWhen?: DynamicVisibleRule
  linkedOptions?: DynamicLinkedOptionsCase[]
}

export interface DynamicFieldOption {
  label: string
  value: string
}

export interface DynamicSchemaNode {
  id: string
  key: string
  type: DynamicFieldType
  title: string
  defaultValue?: unknown
  props: {
    placeholder?: string
    help?: string
    text?: string
    accept?: string
    maxCount?: number
    prompt?: string
    options?: DynamicFieldOption[]
    mode?: 'multiple' | 'tags'
  } & Record<string, unknown>
  rules?: DynamicValidationRule[]
  visibleWhen?: DynamicVisibleRule
  linkage?: DynamicLinkageRule
  children?: DynamicSchemaNode[]
}

export interface DynamicFormSchema {
  id: string
  version: string
  title: string
  nodes: DynamicSchemaNode[]
}

export interface DynamicFormSubmitResult {
  templateId: string
  schemaVersion: string
  values: Record<string, unknown>
}

export interface MaterialDefinition {
  type: DynamicFieldType
  title: string
  group: DynamicMaterialGroup
  description: string
  acceptsChildren: boolean
  allowedChildren?: DynamicFieldType[]
  defaultProps: DynamicSchemaNode['props']
  defaultRules?: DynamicValidationRule[]
}
