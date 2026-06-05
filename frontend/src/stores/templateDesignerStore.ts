import { create } from 'zustand'
import { createSchemaNodeFromMaterial } from '../features/dynamic-form/materialRegistry'
import {
  deleteSchemaNode,
  findSchemaNode,
  insertSchemaNode,
  reorderSchemaNodes,
  updateSchemaNode,
  validateDynamicSchema,
} from '../features/dynamic-form/utils/schemaTree'
import { ownerTemplateService } from '../services'
import type { DynamicFieldType, DynamicFormSchema, DynamicSchemaNode } from '../types/dynamicForm'
import type { TemplateDetail } from '../types/template'

interface TemplateDesignerStore {
  template: TemplateDetail | null
  schema: DynamicFormSchema | null
  selectedNodeId: string | null
  isLoading: boolean
  isSaving: boolean
  isDraftTemplate: boolean
  error: string | null
  hasUnsavedChanges: boolean
  initializeDraftTemplate: (input: { description: string; name: string }) => void
  loadTemplate: (templateId: string) => Promise<void>
  addNode: (type: DynamicFieldType, parentId?: string | null) => string | null
  selectNode: (nodeId: string | null) => void
  updateSelectedNode: (updates: Partial<DynamicSchemaNode>) => void
  replaceSchema: (schema: DynamicFormSchema) => void
  deleteNode: (nodeId: string) => void
  deleteSelectedNode: () => void
  reorderNodes: (activeId: string, overId: string) => void
  saveSchema: () => Promise<boolean>
}

export const useTemplateDesignerStore = create<TemplateDesignerStore>((set, get) => ({
  template: null,
  schema: null,
  selectedNodeId: null,
  isLoading: false,
  isSaving: false,
  isDraftTemplate: false,
  error: null,
  hasUnsavedChanges: false,

  initializeDraftTemplate: (input) => {
    const id = `draft-${Date.now()}`
    const schema: DynamicFormSchema = {
      id,
      version: 'v0.1',
      title: input.name,
      nodes: [],
    }

    set({
      template: {
        id,
        currentVersionId: `${id}-v1`,
        name: input.name,
        version: 'v0.1',
        status: 'draft',
        fieldCount: 0,
        description: input.description,
        schema,
        updatedAt: new Date().toLocaleString('zh-CN', { hour12: false }),
      },
      schema,
      selectedNodeId: null,
      isDraftTemplate: true,
      hasUnsavedChanges: true,
      error: null,
    })
  },

  loadTemplate: async (templateId) => {
    set({ isLoading: true, error: null })

    try {
      const template = await ownerTemplateService.getTemplateDetail(templateId)

      if (!template) {
        set({ error: '模板不存在', schema: null, template: null })
        return
      }

      set({
        template,
        schema: template.schema,
        selectedNodeId: template.schema.nodes[0]?.id ?? null,
        isDraftTemplate: false,
        hasUnsavedChanges: false,
      })
    } catch {
      set({ error: '模板加载失败' })
    } finally {
      set({ isLoading: false })
    }
  },

  addNode: (type, parentId = null) => {
    const schema = get().schema

    if (!schema) {
      return null
    }

    const node = createSchemaNodeFromMaterial(type)
    const nextSchema = insertSchemaNode(schema, node, parentId)

    set({
      schema: nextSchema,
      selectedNodeId: node.id,
      hasUnsavedChanges: true,
    })

    return node.id
  },

  selectNode: (nodeId) => {
    set({ selectedNodeId: nodeId })
  },

  updateSelectedNode: (updates) => {
    const { schema, selectedNodeId } = get()

    if (!schema || !selectedNodeId) {
      return
    }

    set({
      schema: updateSchemaNode(schema, selectedNodeId, (node) => ({
        ...node,
        ...updates,
        props: updates.props ? { ...node.props, ...updates.props } : node.props,
        rules: updates.rules ?? node.rules,
        visibleWhen: Object.hasOwn(updates, 'visibleWhen') ? updates.visibleWhen : node.visibleWhen,
      })),
      hasUnsavedChanges: true,
    })
  },

  replaceSchema: (schema) => {
    set({
      schema,
      selectedNodeId: schema.nodes[0]?.id ?? null,
      hasUnsavedChanges: true,
      error: null,
    })
  },

  deleteNode: (nodeId) => {
    const { schema, selectedNodeId } = get()

    if (!schema) {
      return
    }

    const nextSchema = deleteSchemaNode(schema, nodeId)
    const nextSelectedNodeId = selectedNodeId && findSchemaNode(nextSchema, selectedNodeId) ? selectedNodeId : null

    set({
      schema: nextSchema,
      selectedNodeId: nextSelectedNodeId,
      hasUnsavedChanges: true,
    })
  },

  deleteSelectedNode: () => {
    const selectedNodeId = get().selectedNodeId

    if (selectedNodeId) {
      get().deleteNode(selectedNodeId)
    }
  },

  reorderNodes: (activeId, overId) => {
    const schema = get().schema

    if (!schema || activeId === overId) {
      return
    }

    set({
      schema: reorderSchemaNodes(schema, activeId, overId),
      selectedNodeId: activeId,
      hasUnsavedChanges: true,
    })
  },

  saveSchema: async () => {
    const { isDraftTemplate, schema, template } = get()

    if (!schema || !template) {
      return false
    }

    const validation = validateDynamicSchema(schema)

    if (!validation.valid) {
      set({ error: validation.errors.join('；') })
      return false
    }

    set({ isSaving: true, error: null })

    try {
      if (isDraftTemplate) {
        const createdTemplate = await ownerTemplateService.createTemplate({
          name: template.name,
          description: template.description,
          schema,
        })
        const selectedNodeId = get().selectedNodeId

        set({
          schema: createdTemplate.schema,
          template: createdTemplate,
          selectedNodeId:
            selectedNodeId && findSchemaNode(createdTemplate.schema, selectedNodeId)
              ? selectedNodeId
              : createdTemplate.schema.nodes[0]?.id ?? null,
          isDraftTemplate: false,
          hasUnsavedChanges: false,
        })

        return true
      }

      const savedSchema = await ownerTemplateService.saveTemplateSchema(template.id, schema)
      const selectedNodeId = get().selectedNodeId

      set({
        schema: savedSchema,
        template: {
          ...template,
          schema: savedSchema,
        },
        selectedNodeId: selectedNodeId && findSchemaNode(savedSchema, selectedNodeId) ? selectedNodeId : savedSchema.nodes[0]?.id ?? null,
        isDraftTemplate: false,
        hasUnsavedChanges: false,
      })

      return true
    } catch {
      set({ error: '模板保存失败' })
      return false
    } finally {
      set({ isSaving: false })
    }
  },
}))
