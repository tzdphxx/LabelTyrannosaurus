import type { DynamicFormSchema } from './dynamicForm'

export type TemplateStatus = 'draft' | 'ready' | 'archived'

export interface TemplateSummary {
  id: string
  name: string
  version: string
  status: TemplateStatus
  fieldCount: number
  description: string
}

export interface TemplateDetail extends TemplateSummary {
  schema: DynamicFormSchema
  updatedAt: string
}

export interface TemplateCreateInput {
  name: string
  description: string
}
