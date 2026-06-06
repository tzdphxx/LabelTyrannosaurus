import { create } from 'zustand'
import { ownerImportService, ownerTaskService } from '../services'
import type { FileUploadResponse, ImportPreview } from '../types/import'
import type { AiReviewConfigDraft, OwnerTask, PublishValidationResult, TaskDraftInput } from '../types/task'

export const DEFAULT_AI_REVIEW_PROMPT = `你是电商商品标题审核员。请基于以下维度为提交内容打分(0-100):
[相关性]标注结果与原始数据是否对齐
[准确性]类目/关键词与商品事实是否一致
[格式合规]是否满足模板字符/正则规则
[安全性]是否包含敏感/违规词
请通过 function_call 返回 JSON:
{ "scores": {...}, "verdict": "pass|reject|manual", "reason": "..." }`

export const DEFAULT_AI_SCORING_DIMENSIONS = ['相关性', '准确性', '格式合规', '安全性']

type TaskDraftChanges = Partial<Omit<TaskDraftInput, 'aiReview'>> & {
  aiReview?: Partial<AiReviewConfigDraft>
}

interface OwnerDraftStore {
  draftId: string | null
  draft: TaskDraftInput
  importPreview: ImportPreview | null
  uploadedDatasetFile: FileUploadResponse | null
  hasUnsavedChanges: boolean
  isSaving: boolean
  isLoading: boolean
  isUploadingDataset: boolean
  validationResult: PublishValidationResult | null
  error: string | null
  resetDraft: () => Promise<void>
  loadFromTask: (taskId: string) => Promise<void>
  updateDraft: (changes: TaskDraftChanges) => void
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
    rewardMode: 'APPROVED_ITEM',
    rewardCurrency: 'POINT',
    rewardVisible: true,
    description: '按有效标注条目结算',
  },
  distributionStrategy: '先到先得',
  publishedTemplateVersionId: null,
  aiReview: {
    aiPrompt: DEFAULT_AI_REVIEW_PROMPT,
    aiModelName: '',
    aiProviderId: null,
    aiScoringDimensions: DEFAULT_AI_SCORING_DIMENSIONS,
    aiPassThreshold: 80,
    aiManualReviewThreshold: 60,
    aiReviewStrategy: 'LIGHTWEIGHT',
  },
  reviewLevelCount: 1,
  overlapCount: 1,
  maxClaimsPerLabeler: 10,
  assignedLabelerId: null,
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
    overlapCount: task.overlapCount,
    maxClaimsPerLabeler: task.maxClaimsPerLabeler,
    assignedLabelerId: task.assignedLabelerId ?? null,
    datasetFileId: task.datasetFileId,
  }
}

export const useOwnerDraftStore = create<OwnerDraftStore>((set, get) => ({
  draftId: null,
  draft: emptyDraft,
  importPreview: null,
  uploadedDatasetFile: null,
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
      hasUnsavedChanges: false,
      validationResult: null,
      error: null,
    })
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
      const importPreview = await ownerImportService.parseDatasetFile(file)

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
    const { draftId, hasUnsavedChanges } = get()
    const task = !draftId || hasUnsavedChanges ? await get().saveDraft() : null
    const taskId = get().draftId ?? task?.id

    if (!taskId) {
      return null
    }

    const publishedTask = await ownerTaskService.publishTask(taskId)

    if (publishedTask) {
      set({
        draft: toDraftInput(publishedTask),
        hasUnsavedChanges: false,
        validationResult: null,
      })
    }

    return publishedTask
  },
}))
