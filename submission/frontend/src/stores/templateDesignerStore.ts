import { create } from 'zustand'
import { createSchemaNodeFromMaterial, createTabPaneNode } from '../features/dynamic-form/materialRegistry'
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
  isForkMode: boolean
  forkChangeNote: string
  error: string | null
  hasUnsavedChanges: boolean
  initializeDraftTemplate: (input: { description: string; name: string }) => void
  loadTemplate: (templateId: string, options?: { forkMode?: boolean; forkChangeNote?: string; templateVersion?: TemplateDetail }) => Promise<void>
  addNode: (type: DynamicFieldType, parentId?: string | null) => string | null
  addTabPane: (parentId: string, title?: string) => string | null
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
  isForkMode: false,
  forkChangeNote: '',
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
      isForkMode: false,
      forkChangeNote: '',
      hasUnsavedChanges: true,
      error: null,
    })
  },

  loadTemplate: async (templateId, options = {}) => {
    set({ isLoading: true, error: null })

    try {
      if (options.templateVersion) {
        const template = options.templateVersion

        set({
          template,
          schema: template.schema,
          selectedNodeId: template.schema.nodes[0]?.id ?? null,
          isDraftTemplate: false,
          isForkMode: false,
          forkChangeNote: '',
          hasUnsavedChanges: false,
        })
        return
      }

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
        isForkMode: Boolean(options.forkMode),
        forkChangeNote: options.forkChangeNote ?? '',
        hasUnsavedChanges: Boolean(options.forkMode),
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

  addTabPane: (parentId, title = 'Tab') => {
    const schema = get().schema

    if (!schema) {
      return null
    }

    const parent = findSchemaNode(schema, parentId)

    if (!parent || parent.type !== 'tabs') {
      return null
    }

    const node = createTabPaneNode(title)
    const nextSchema = updateSchemaNode(schema, parentId, (currentParent) => ({
      ...currentParent,
      children: [...(currentParent.children ?? []), node],
    }))

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
    const { forkChangeNote, isDraftTemplate, isForkMode, schema, template } = get()

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
          isForkMode: false,
          forkChangeNote: '',
          hasUnsavedChanges: false,
        })

        return true
      }

      const savedTemplate = await ownerTemplateService.forkTemplateVersion(template.id, {
        schema,
        changeNote: isForkMode ? forkChangeNote || '基于当前版本 Fork' : '更新模板 schema',
      })
      const selectedNodeId = get().selectedNodeId

      set({
        schema: savedTemplate.schema,
        template: savedTemplate,
        selectedNodeId:
          selectedNodeId && findSchemaNode(savedTemplate.schema, selectedNodeId)
            ? selectedNodeId
            : savedTemplate.schema.nodes[0]?.id ?? null,
        isDraftTemplate: false,
        isForkMode: false,
        forkChangeNote: '',
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
