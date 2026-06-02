import { create } from 'zustand'
import { isRealServiceMode, ownerImportService, ownerTaskService } from '../services'
import type { FileUploadResponse, ImportPreview } from '../types/import'
import type { AiReviewConfigDraft, OwnerTask, PublishValidationResult, TaskDraftInput } from '../types/task'

type TaskDraftChanges = Partial<Omit<TaskDraftInput, 'aiReview'>> & {
  aiReview?: Partial<AiReviewConfigDraft>
}

interface OwnerDraftStore {
  draftId: string | null
  draft: TaskDraftInput
  importPreview: ImportPreview | null
  uploadedDatasetFile: FileUploadResponse | null
  currentStep: number
  hasUnsavedChanges: boolean
  isSaving: boolean
  isLoading: boolean
  isUploadingDataset: boolean
  validationResult: PublishValidationResult | null
  error: string | null
  resetDraft: () => Promise<void>
  loadFromTask: (taskId: string) => Promise<void>
  updateDraft: (changes: TaskDraftChanges) => void
  setStep: (step: number) => void
  loadImportPreview: () => Promise<void>
  uploadDatasetFile: (file: File) => Promise<FileUploadResponse | null>
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
  quota: 1,
  rewardRule: {
    unitPrice: 0.12,
    currency: 'CNY',
    description: '按有效标注条目结算',
  },
  distributionStrategy: '先到先得',
  publishedTemplateVersionId: null,
  aiReview: {
    prompt: '',
    model: '',
    rating: '',
  },
  reviewLevelCount: 1,
  datasetFileId: null,
}

function toDraftInput(task: OwnerTask): TaskDraftInput {
  return {
    title: task.title,
    description: task.description,
    instruction: task.instruction,
    tags: [...task.tags],
    deadline: task.deadline,
    quota: task.quota,
    rewardRule: { ...task.rewardRule },
    distributionStrategy: task.distributionStrategy,
    publishedTemplateVersionId: task.publishedTemplateVersionId,
    aiReview: { ...task.aiReview },
    reviewLevelCount: task.reviewLevelCount,
    datasetFileId: task.datasetFileId,
  }
}

export const useOwnerDraftStore = create<OwnerDraftStore>((set, get) => ({
  draftId: null,
  draft: emptyDraft,
  importPreview: null,
  uploadedDatasetFile: null,
  currentStep: 0,
  hasUnsavedChanges: false,
  isSaving: false,
  isLoading: false,
  isUploadingDataset: false,
  validationResult: null,
  error: null,
  resetDraft: async () => {
    set({
      draftId: null,
      draft: {
        ...emptyDraft,
        aiReview: { ...emptyDraft.aiReview },
        rewardRule: { ...emptyDraft.rewardRule },
        tags: [],
      },
      importPreview: null,
      uploadedDatasetFile: null,
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
        uploadedDatasetFile: null,
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
        aiReview: changes.aiReview ? { ...state.draft.aiReview, ...changes.aiReview } : state.draft.aiReview,
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
  uploadDatasetFile: async (file) => {
    set({ error: null, isUploadingDataset: true })

    try {
      const uploadedDatasetFile = await ownerImportService.uploadDatasetFile(file)
      const importPreview = isRealServiceMode() ? null : await ownerImportService.getDefaultImportPreview()

      set((state) => ({
        draft: {
          ...state.draft,
          datasetFileId: String(uploadedDatasetFile.fileId),
        },
        uploadedDatasetFile,
        importPreview,
        hasUnsavedChanges: true,
        validationResult: null,
      }))

      return uploadedDatasetFile
    } catch {
      set({ error: '数据集文件上传失败' })

      return null
    } finally {
      set({ isUploadingDataset: false })
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
    const draftValidation = ownerTaskService.validateDraftForPublish(get().draft, get().draftId ?? 'pending')

    if (!draftValidation.valid) {
      set({ validationResult: draftValidation })

      return draftValidation
    }

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

    const validationResult = ownerTaskService.validateDraftForPublish(get().draft, taskId)
    set({ validationResult })

    return validationResult
  },
  publishDraft: async () => {
    const validationResult = await get().validatePublish()
    const draftId = get().draftId

    if (!validationResult.valid || !draftId) {
      return null
    }

    const task = await ownerTaskService.publishTask(draftId)

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
