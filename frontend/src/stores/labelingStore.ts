import { create } from 'zustand'
import { labelingService } from '../services'
import type {
  LabelerSubmissionStats,
  LabelerTaskListQuery,
  LabelerTaskSummary,
  LabelingDraft,
  LabelingQuestion,
  LabelingReviewSummary,
  LabelingSubmission,
  LabelingSubmitResult,
  LabelingSubmitValidationResult,
} from '../types/labeling'
import type { DynamicFormSubmitResult } from '../types/dynamicForm'

interface LabelingStore {
  marketTasks: LabelerTaskSummary[]
  marketTags: string[]
  filters: LabelerTaskListQuery
  currentTask: LabelerTaskSummary | null
  questions: LabelingQuestion[]
  currentQuestion: LabelingQuestion | null
  currentDraft: LabelingDraft | null
  reviewSummary: LabelingReviewSummary | null
  submitValidation: LabelingSubmitValidationResult | null
  submissionStats: LabelerSubmissionStats | null
  submissions: LabelingSubmission[]
  isMarketLoading: boolean
  isClaiming: boolean
  isWorkbenchLoading: boolean
  isDraftSaving: boolean
  isSubmitting: boolean
  isSubmissionsLoading: boolean
  error: string | null
  setFilters: (filters: Partial<LabelerTaskListQuery>) => void
  setCurrentQuestion: (questionId: string) => void
  loadMarket: () => Promise<void>
  claimTask: (taskId: string) => Promise<LabelerTaskSummary | null>
  loadWorkbench: (taskId: string) => Promise<void>
  loadDraft: (taskId: string, questionId: string, userId: string) => Promise<void>
  saveDraft: (payload: Omit<LabelingDraft, 'id' | 'updatedAt'>) => Promise<LabelingDraft | null>
  submitAnswers: (taskId: string, userId: string, answers: DynamicFormSubmitResult[]) => Promise<LabelingSubmission | null>
  submitTaskDrafts: (taskId: string, userId: string) => Promise<LabelingSubmitResult>
  loadSubmissions: () => Promise<void>
}

const initialFilters: LabelerTaskListQuery = {
  keyword: '',
  tag: 'all',
  status: 'all',
}

export const useLabelingStore = create<LabelingStore>((set, get) => ({
  marketTasks: [],
  marketTags: [],
  filters: initialFilters,
  currentTask: null,
  questions: [],
  currentQuestion: null,
  currentDraft: null,
  reviewSummary: null,
  submitValidation: null,
  submissionStats: null,
  submissions: [],
  isMarketLoading: false,
  isClaiming: false,
  isWorkbenchLoading: false,
  isDraftSaving: false,
  isSubmitting: false,
  isSubmissionsLoading: false,
  error: null,
  setFilters: (filters) => {
    set((state) => ({
      filters: {
        ...state.filters,
        ...filters,
      },
    }))
  },
  setCurrentQuestion: (questionId) => {
    set((state) => ({
      currentQuestion: state.questions.find((question) => question.id === questionId) ?? state.currentQuestion,
      currentDraft: null,
    }))
  },
  loadMarket: async () => {
    set({ isMarketLoading: true, error: null })

    try {
      const [marketTasks, marketTags] = await Promise.all([
        labelingService.listMarketTasks(get().filters),
        labelingService.listTags(),
      ])
      set({ marketTasks, marketTags })
    } catch {
      set({ error: '任务广场加载失败' })
    } finally {
      set({ isMarketLoading: false })
    }
  },
  claimTask: async (taskId) => {
    set({ isClaiming: true, error: null })

    try {
      const task = await labelingService.claimTask(taskId)
      await get().loadMarket()

      return task
    } catch {
      set({ error: '任务领取失败' })

      return null
    } finally {
      set({ isClaiming: false })
    }
  },
  loadWorkbench: async (taskId) => {
    set({ isWorkbenchLoading: true, error: null })

    try {
      const [currentTask, questions, reviewSummary] = await Promise.all([
        labelingService.getTaskDetail(taskId),
        labelingService.listQuestions(taskId),
        labelingService.getReviewSummary(taskId),
      ])
      set({
        currentTask,
        questions,
        currentQuestion: questions[0] ?? null,
        reviewSummary,
      })
    } catch {
      set({ error: '标注工作台加载失败' })
    } finally {
      set({ isWorkbenchLoading: false })
    }
  },
  loadDraft: async (taskId, questionId, userId) => {
    set({ error: null })

    try {
      const currentDraft = await labelingService.getDraft(taskId, questionId, userId)
      set({ currentDraft })
    } catch {
      set({ error: '草稿加载失败' })
    }
  },
  saveDraft: async (payload) => {
    set({ isDraftSaving: true, error: null })

    try {
      const currentDraft = await labelingService.saveDraft(payload)
      set((state) => ({
        currentDraft,
        submitValidation: null,
        questions: state.questions.map((question) =>
          question.id === currentDraft.questionId
            ? {
              ...question,
              status: 'draft',
            }
            : question,
        ),
        currentQuestion:
          state.currentQuestion?.id === currentDraft.questionId
            ? {
              ...state.currentQuestion,
              status: 'draft',
            }
            : state.currentQuestion,
      }))

      return currentDraft
    } catch {
      set({ error: '草稿保存失败' })

      return null
    } finally {
      set({ isDraftSaving: false })
    }
  },
  submitAnswers: async (taskId, userId, answers) => {
    set({ isSubmitting: true, error: null })

    try {
      const submission = await labelingService.submitAnswers(taskId, userId, answers)
      await Promise.all([get().loadMarket(), get().loadSubmissions()])

      return submission
    } catch {
      set({ error: '答案提交失败' })

      return null
    } finally {
      set({ isSubmitting: false })
    }
  },
  submitTaskDrafts: async (taskId, userId) => {
    set({ isSubmitting: true, error: null, submitValidation: null })

    try {
      const result = await labelingService.submitTaskDrafts(taskId, userId)
      set((state) => ({
        submitValidation: result.validation,
        questions: result.submission
          ? state.questions.map((question) => ({
              ...question,
              status: 'submitted',
            }))
          : state.questions,
        currentTask:
          result.submission && state.currentTask
            ? {
                ...state.currentTask,
                status: 'submitted',
                completedQuestions: state.currentTask.totalQuestions,
                submittedAt: result.submission.submittedAt,
              }
            : state.currentTask,
      }))

      if (result.submission) {
        await Promise.all([get().loadMarket(), get().loadSubmissions()])
      }

      return result
    } catch {
      const result: LabelingSubmitResult = {
        submission: null,
        validation: {
          valid: false,
          errors: [
            {
              questionId: '',
              questionTitle: '',
              message: '答案提交失败',
            },
          ],
        },
      }
      set({ error: '答案提交失败', submitValidation: result.validation })

      return result
    } finally {
      set({ isSubmitting: false })
    }
  },
  loadSubmissions: async () => {
    set({ isSubmissionsLoading: true, error: null })

    try {
      const [submissionStats, submissions] = await Promise.all([
        labelingService.getSubmissionStats(),
        labelingService.listSubmissions(),
      ])
      set({ submissionStats, submissions })
    } catch {
      set({ error: '我的数据加载失败' })
    } finally {
      set({ isSubmissionsLoading: false })
    }
  },
}))
