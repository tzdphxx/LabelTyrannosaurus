import { mockTemplates } from '../../mocks'
import type { DynamicFormSchema } from '../../types/dynamicForm'
import type { TemplateCreateInput, TemplateDetail, TemplateSummary } from '../../types/template'
import { getSchemaNodeKeys } from '../../features/dynamic-form/utils/schemaTree'

export const ownerTemplateService = {
  async listTemplates(): Promise<TemplateSummary[]> {
    return mockTemplates.map((template) => ({
      id: template.id,
      name: template.name,
      version: template.version,
      status: template.status,
      fieldCount: template.fieldCount,
      description: template.description,
    }))
  },

  async getTemplateDetail(templateId: string): Promise<TemplateDetail | null> {
    const template = mockTemplates.find((item) => item.id === templateId)

    return template ? structuredClone(template) : null
  },

  async getTemplateSchema(templateId: string): Promise<DynamicFormSchema | null> {
    const template = mockTemplates.find((item) => item.id === templateId)

    return template ? structuredClone(template.schema) : null
  },

  async createTemplate(input: TemplateCreateInput): Promise<TemplateDetail> {
    const suffix = Math.random().toString(36).slice(2, 8)
    const templateId = `tpl-${suffix}`
    const schema: DynamicFormSchema = {
      id: templateId,
      version: 'v0.1',
      title: input.name,
      nodes: [],
    }
    const template: TemplateDetail = {
      id: templateId,
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
