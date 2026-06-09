import { mockTemplates } from '../../mocks'
import type { DynamicFormSchema } from '../../types/dynamicForm'
import type {
  OwnerTemplateForkRequest,
  OwnerTemplateCreateRequest,
  OwnerTemplateResponse,
  OwnerTemplateVersionResponse,
  OwnerTemplateVersionState,
  TemplateCreateInput,
  TemplateDetail,
  TemplateForkInput,
  TemplateStatus,
  TemplateSummary,
  TemplateVersionSnapshot,
} from '../../types/template'
import { fromBackendTemplateSchema, toBackendTemplateSchema } from '../../features/dynamic-form/utils/backendSchema'
import { getSchemaNodeKeys } from '../../features/dynamic-form/utils/schemaTree'
import { isRealServiceMode, request } from '../http'

function mapOwnerTemplateStatus(state: OwnerTemplateVersionState): TemplateStatus {
  if (state === 'PUBLISHED_SNAPSHOT' || state === 'PUBLISHED') {
    return 'ready'
  }

  if (state === 'ARCHIVED') {
    return 'archived'
  }

  return 'draft'
}

function createEmptySchema(id: string, title: string): DynamicFormSchema {
  return {
    id,
    version: 'v0.1',
    title,
    nodes: [],
  }
}

function normalizeSchema(schemaJson: unknown, templateId: string, name: string): DynamicFormSchema {
  const schema = fromBackendTemplateSchema(schemaJson)

  return {
    ...schema,
    id: schema.id || templateId,
    title: schema.title || name,
  }
}

function mapOwnerTemplate(response: OwnerTemplateResponse): TemplateDetail {
  const id = String(response.templateId)
  const schema = normalizeSchema(response.currentVersion.schemaJson, id, response.name)

  return {
    id,
    currentVersionId: String(response.currentVersion.versionId),
    name: response.name,
    version: `v${response.currentVersionNo}`,
    status: mapOwnerTemplateStatus(response.currentVersion.state),
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

function mapOwnerTemplateVersion(version: OwnerTemplateVersionResponse): TemplateVersionSnapshot {
  const templateId = String(version.templateId)
  const schema = normalizeSchema(version.schemaJson, templateId, `v${version.versionNo}`)

  return {
    templateId,
    versionId: String(version.versionId),
    version: `v${version.versionNo}`,
    status: mapOwnerTemplateStatus(version.state || (version.publishedSnapshot ? 'PUBLISHED_SNAPSHOT' : 'DRAFT')),
    fieldCount: getSchemaNodeKeys(schema).length,
    description: version.changeNote,
    schema,
    createdAt: version.createdAt,
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

  async listTemplateVersions(templateId: string): Promise<TemplateVersionSnapshot[]> {
    if (isRealServiceMode()) {
      const versions = await request.get<OwnerTemplateVersionResponse[]>(`/v1/templates/${templateId}/versions`)

      return versions.map(mapOwnerTemplateVersion)
    }

    const template = mockTemplates.find((item) => item.id === templateId)

    if (!template) {
      throw new Error('Template not found')
    }

    return [
      {
        templateId: template.id,
        versionId: template.currentVersionId,
        version: template.version,
        status: template.status,
        fieldCount: template.fieldCount,
        description: template.description,
        schema: structuredClone(template.schema),
        createdAt: template.updatedAt,
      },
    ]
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
    const schema = input.schema ?? createEmptySchema(templateId, input.name)

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

  async forkTemplateVersion(templateId: string, input: TemplateForkInput): Promise<TemplateDetail> {
    if (isRealServiceMode()) {
      const template = await getRealOwnerTemplateDetail(templateId)

      if (!template) {
        throw new Error('Template not found')
      }

      const payload: OwnerTemplateForkRequest = {
        baseVersionId: Number(template.currentVersionId),
        schemaJson: toBackendTemplateSchema(input.schema),
        changeNote: input.changeNote,
      }
      const nextTemplate = await request.post<OwnerTemplateResponse, OwnerTemplateForkRequest>(`/v1/templates/${templateId}/fork`, payload)

      return mapOwnerTemplate(nextTemplate)
    }

    const template = mockTemplates.find((item) => item.id === templateId)

    if (!template) {
      throw new Error('Template not found')
    }

    const match = template.version.match(/^v(\d+)(?:\.(\d+))?$/)
    const majorVersion = match ? Number(match[1]) : 0

    template.schema = structuredClone(input.schema)
    template.currentVersionId = `${template.id}-v${majorVersion + 1}`
    template.version = `v${majorVersion + 1}`
    template.fieldCount = getSchemaNodeKeys(input.schema).length
    template.description = input.changeNote
    template.updatedAt = new Date().toLocaleString('zh-CN', { hour12: false })

    return structuredClone(template)
  },

  async saveTemplateSchema(templateId: string, schema: DynamicFormSchema): Promise<DynamicFormSchema> {
    const template = await this.forkTemplateVersion(templateId, {
      schema,
      changeNote: '更新模板 schema',
    })

    return template.schema
  },
}
