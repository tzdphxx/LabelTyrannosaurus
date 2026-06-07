import { create } from 'zustand'
import { ownerImportService, ownerTaskService } from '../services'
import type { ImportPreview } from '../types/import'
import type { OwnerTask, PublishValidationResult, TaskDraftInput } from '../types/task'

interface OwnerDraftStore {
  draftId: string | null
  draft: TaskDraftInput
  importPreview: ImportPreview | null
  currentStep: number
  hasUnsavedChanges: boolean
  isSaving: boolean
  isLoading: boolean
  validationResult: PublishValidationResult | null
  error: string | null
  resetDraft: () => Promise<void>
  loadFromTask: (taskId: string) => Promise<void>
  updateDraft: (changes: Partial<TaskDraftInput>) => void
  setStep: (step: number) => void
  loadImportPreview: () => Promise<void>
  saveDraft: () => Promise<OwnerTask | null>
  validatePublish: () => Promise<PublishValidationResult>
  publishDraft: () => Promise<OwnerTask | null>
}

const emptyDraft: TaskDraftInput = {
  title: '',
  description: '',
  instruction: '',
  tags: [],
  deadline: '',
  rewardRule: {
    unitPrice: 0.12,
    currency: 'CNY',
    description: '按有效标注条目结算',
  },
  distributionStrategy: 'balanced',
  templateId: null,
}

function toDraftInput(task: OwnerTask): TaskDraftInput {
  return {
    title: task.title,
    description: task.description,
    instruction: task.instruction,
    tags: [...task.tags],
    deadline: task.deadline,
    rewardRule: { ...task.rewardRule },
    distributionStrategy: task.distributionStrategy,
    templateId: task.templateId,
  }
}

export const useOwnerDraftStore = create<OwnerDraftStore>((set, get) => ({
  draftId: null,
  draft: emptyDraft,
  importPreview: null,
  currentStep: 0,
  hasUnsavedChanges: false,
  isSaving: false,
  isLoading: false,
  validationResult: null,
  error: null,
  resetDraft: async () => {
    set({
      draftId: null,
      draft: {
        ...emptyDraft,
        rewardRule: { ...emptyDraft.rewardRule },
        tags: [],
      },
      importPreview: null,
      currentStep: 0,
      hasUnsavedChanges: false,
      validationResult: null,
      error: null,
    })
    await get().loadImportPreview()
  },
  loadFromTask: async (taskId) => {
    set({ isLoading: true, error: null })

    try {
      const detail = await ownerTaskService.getTaskDetail(taskId)

      if (!detail) {
        set({ error: '任务不存在' })

        return
      }

      set({
        draftId: detail.task.id,
        draft: toDraftInput(detail.task),
        importPreview: detail.importPreview,
        currentStep: 0,
        hasUnsavedChanges: false,
        validationResult: null,
      })
    } catch {
      set({ error: '草稿加载失败' })
    } finally {
      set({ isLoading: false })
    }
  },
  updateDraft: (changes) => {
    set((state) => ({
      draft: {
        ...state.draft,
        ...changes,
        rewardRule: changes.rewardRule ? { ...changes.rewardRule } : state.draft.rewardRule,
        tags: changes.tags ? [...changes.tags] : state.draft.tags,
      },
      hasUnsavedChanges: true,
      validationResult: null,
    }))
  },
  setStep: (step) => set({ currentStep: step }),
  loadImportPreview: async () => {
    set({ isLoading: true, error: null })

    try {
      const importPreview = await ownerImportService.getDefaultImportPreview()
      set({ importPreview })
    } catch {
      set({ error: '导入预览加载失败' })
    } finally {
      set({ isLoading: false })
    }
  },
  saveDraft: async () => {
    set({ isSaving: true, error: null })

    try {
      const { draftId, draft } = get()
      const task = draftId ? await ownerTaskService.updateTask(draftId, draft) : await ownerTaskService.createTask(draft)

      if (task) {
        set({
          draftId: task.id,
          draft: toDraftInput(task),
          hasUnsavedChanges: false,
        })
      }

      return task
    } catch {
      set({ error: '草稿保存失败' })

      return null
    } finally {
      set({ isSaving: false })
    }
  },
  validatePublish: async () => {
    const task = get().draftId && !get().hasUnsavedChanges ? null : await get().saveDraft()
    const taskId = get().draftId ?? task?.id

    if (!taskId) {
      const result = {
        valid: false,
        errors: ['请先保存任务草稿'],
      }
      set({ validationResult: result })

      return result
    }

    const validationResult = await ownerTaskService.validatePublish(taskId)
    set({ validationResult })

    return validationResult
  },
  publishDraft: async () => {
    const validationResult = await get().validatePublish()

    if (!validationResult.valid || !get().draftId) {
      return null
    }

    const task = await ownerTaskService.updateTaskStatus(get().draftId, 'published')

    if (task) {
      set({
        draft: toDraftInput(task),
        hasUnsavedChanges: false,
        validationResult: null,
      })
    }

    return task
  },
}))
