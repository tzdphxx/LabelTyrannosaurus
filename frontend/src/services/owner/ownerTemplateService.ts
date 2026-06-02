import { mockTemplates } from '../../mocks'
import type { DynamicFormSchema } from '../../types/dynamicForm'
import type {
  OwnerTemplateForkRequest,
  OwnerTemplateCreateRequest,
  OwnerTemplateResponse,
  OwnerTemplateVersionState,
  TemplateCreateInput,
  TemplateDetail,
  TemplateStatus,
  TemplateSummary,
} from '../../types/template'
import { fromBackendTemplateSchema, toBackendTemplateSchema } from '../../features/dynamic-form/utils/backendSchema'
import { getSchemaNodeKeys } from '../../features/dynamic-form/utils/schemaTree'
import { isRealServiceMode, request } from '../http'

const ownerTemplateStatusMap: Record<OwnerTemplateVersionState, TemplateStatus> = {
  DRAFT: 'draft',
  PUBLISHED: 'ready',
  ARCHIVED: 'archived',
}

function createEmptySchema(id: string, title: string): DynamicFormSchema {
  return {
    id,
    version: 'v0.1',
    title,
    nodes: [],
  }
}

function isDynamicFormSchema(value: unknown): value is DynamicFormSchema {
  return Boolean(
    value &&
      typeof value === 'object' &&
      'id' in value &&
      'version' in value &&
      'title' in value &&
      'nodes' in value &&
      Array.isArray((value as DynamicFormSchema).nodes),
  )
}

function normalizeSchema(schemaJson: unknown, templateId: string, name: string): DynamicFormSchema {
  if (isDynamicFormSchema(schemaJson)) {
    return fromBackendTemplateSchema(schemaJson)
  }

  return createEmptySchema(templateId, name)
}

function mapOwnerTemplate(response: OwnerTemplateResponse): TemplateDetail {
  const id = String(response.templateId)
  const schema = normalizeSchema(response.currentVersion.schemaJson, id, response.name)

  return {
    id,
    currentVersionId: String(response.currentVersion.versionId),
    name: response.name,
    version: `v${response.currentVersionNo}`,
    status: ownerTemplateStatusMap[response.currentVersion.state],
    fieldCount: getSchemaNodeKeys(schema).length,
    description: response.currentVersion.changeNote,
    schema,
    updatedAt: response.updatedAt,
  }
}

function toTemplateSummary(template: TemplateDetail): TemplateSummary {
  return {
    id: template.id,
    currentVersionId: template.currentVersionId,
    name: template.name,
    version: template.version,
    status: template.status,
    fieldCount: template.fieldCount,
    description: template.description,
  }
}

async function getRealOwnerTemplateDetail(templateId: string): Promise<TemplateDetail | null> {
  const templates = await request.get<OwnerTemplateResponse[]>('/v1/owner/templates')
  const template = templates.find((item) => String(item.templateId) === templateId)

  return template ? mapOwnerTemplate(template) : null
}

export const ownerTemplateService = {
  async listTemplates(): Promise<TemplateSummary[]> {
    if (isRealServiceMode()) {
      const templates = await request.get<OwnerTemplateResponse[]>('/v1/owner/templates')

      return templates.map((template) => toTemplateSummary(mapOwnerTemplate(template)))
    }

    return mockTemplates.map((template) => ({
      id: template.id,
      currentVersionId: template.currentVersionId,
      name: template.name,
      version: template.version,
      status: template.status,
      fieldCount: template.fieldCount,
      description: template.description,
    }))
  },

  async getTemplateDetail(templateId: string): Promise<TemplateDetail | null> {
    if (isRealServiceMode()) {
      return getRealOwnerTemplateDetail(templateId)
    }

    const template = mockTemplates.find((item) => item.id === templateId)

    return template ? structuredClone(template) : null
  },

  async getTemplateSchema(templateId: string): Promise<DynamicFormSchema | null> {
    if (isRealServiceMode()) {
      const template = await getRealOwnerTemplateDetail(templateId)

      return template?.schema ?? null
    }

    const template = mockTemplates.find((item) => item.id === templateId)

    return template ? structuredClone(template.schema) : null
  },

  async createTemplate(input: TemplateCreateInput): Promise<TemplateDetail> {
    const suffix = Math.random().toString(36).slice(2, 8)
    const templateId = `tpl-${suffix}`
    const schema = createEmptySchema(templateId, input.name)

    if (isRealServiceMode()) {
      const payload: OwnerTemplateCreateRequest = {
        name: input.name,
        schemaJson: toBackendTemplateSchema(schema),
        changeNote: input.description,
      }
      const template = await request.post<OwnerTemplateResponse, OwnerTemplateCreateRequest>('/v1/owner/templates', payload)

      return mapOwnerTemplate(template)
    }

    const template: TemplateDetail = {
      id: templateId,
      currentVersionId: `${templateId}-v1`,
      name: input.name,
      version: 'v0.1',
      status: 'draft',
      fieldCount: 0,
      description: input.description,
      schema,
      updatedAt: new Date().toLocaleString('zh-CN', { hour12: false }),
    }

    mockTemplates.unshift(template)

    return structuredClone(template)
  },

  async saveTemplateSchema(templateId: string, schema: DynamicFormSchema): Promise<DynamicFormSchema> {
    if (isRealServiceMode()) {
      const template = await getRealOwnerTemplateDetail(templateId)

      if (!template) {
        throw new Error('Template not found')
      }

      const payload: OwnerTemplateForkRequest = {
        baseVersionId: Number(template.currentVersionId),
        schemaJson: toBackendTemplateSchema(schema),
        changeNote: '更新模板 schema',
      }
      const nextTemplate = await request.post<OwnerTemplateResponse, OwnerTemplateForkRequest>(`/v1/templates/${templateId}/fork`, payload)

      return mapOwnerTemplate(nextTemplate).schema
    }

    const template = mockTemplates.find((item) => item.id === templateId)

    if (!template) {
      throw new Error('Template not found')
    }

    template.schema = structuredClone(schema)
    template.fieldCount = getSchemaNodeKeys(schema).length
    template.updatedAt = new Date().toLocaleString('zh-CN', { hour12: false })

    return structuredClone(template.schema)
  },
}
