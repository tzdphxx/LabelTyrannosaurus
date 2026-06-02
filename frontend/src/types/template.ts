import type { DynamicFormSchema } from './dynamicForm'

export type TemplateStatus = 'draft' | 'ready' | 'archived'
export type OwnerTemplateVersionState = 'DRAFT' | 'PUBLISHED' | 'ARCHIVED'

export interface TemplateSummary {
  id: string
  currentVersionId: string
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

export interface OwnerTemplateCreateRequest {
  name: string
  schemaJson: DynamicFormSchema
  changeNote: string
}

export interface OwnerTemplateForkRequest {
  baseVersionId?: number
  schemaJson?: DynamicFormSchema
  changeNote?: string
}

export interface OwnerTemplateVersionResponse {
  versionId: number
  templateId: number
  taskId: number | null
  ownerId: number
  versionNo: number
  schemaJson: unknown
  publishedSnapshot: boolean
  state: OwnerTemplateVersionState
  changeNote: string
  createdBy: number
  createdAt: string
}

export interface OwnerTemplateResponse {
  templateId: number
  taskId: number | null
  ownerId: number
  name: string
  currentVersionNo: number
  currentVersion: OwnerTemplateVersionResponse
  createdBy: number
  createdAt: string
  updatedAt: string
}
