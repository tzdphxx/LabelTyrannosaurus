import { create } from 'zustand'
import { ApiError, labelingService } from '../services'
import type {
  LabelerAssignmentListQuery,
  LabelerAssignmentStats,
  LabelerAssignmentSummary,
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
  assignmentStats: LabelerAssignmentStats | null
  assignments: LabelerAssignmentSummary[]
  submissionStats: LabelerSubmissionStats | null
  submissions: LabelingSubmission[]
  isMarketLoading: boolean
  isClaiming: boolean
  isWorkbenchLoading: boolean
  isDraftSaving: boolean
  isSubmitting: boolean
  isAssignmentsLoading: boolean
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
  submitQuestionDraft: (taskId: string, questionId: string, userId: string) => Promise<LabelingSubmitResult>
  submitTaskDrafts: (taskId: string, userId: string) => Promise<LabelingSubmitResult>
  loadAssignments: (query?: LabelerAssignmentListQuery) => Promise<void>
  loadSubmissions: () => Promise<void>
}

const initialFilters: LabelerTaskListQuery = {
  keyword: '',
  tag: 'all',
  status: 'all',
}

function getErrorMessage(error: unknown, fallback: string) {
  return error instanceof ApiError ? error.message : fallback
}

function getSettledErrorMessage(results: PromiseSettledResult<unknown>[], fallback: string) {
  const rejected = results.find((result) => result.status === 'rejected')

  return rejected?.status === 'rejected' ? getErrorMessage(rejected.reason, fallback) : null
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
  assignmentStats: null,
  assignments: [],
  submissionStats: null,
  submissions: [],
  isMarketLoading: false,
  isClaiming: false,
  isWorkbenchLoading: false,
  isDraftSaving: false,
  isSubmitting: false,
  isAssignmentsLoading: false,
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

    const marketTasksResult = await Promise.allSettled([
      labelingService.listMarketTasks(get().filters),
    ]).then(([result]) => result)

    set((state) => ({
      marketTasks: marketTasksResult.status === 'fulfilled' ? marketTasksResult.value : state.marketTasks,
      marketTags: marketTasksResult.status === 'fulfilled'
        ? Array.from(new Set(marketTasksResult.value.flatMap((task) => task.tags))).sort((first, second) =>
          first.localeCompare(second, 'zh-CN'),
        )
        : state.marketTags,
      error: marketTasksResult.status === 'rejected' ? getErrorMessage(marketTasksResult.reason, '任务广场加载失败') : null,
      isMarketLoading: false,
    }))
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

    const [taskResult, questionsResult, reviewSummaryResult] = await Promise.allSettled([
      labelingService.getTaskDetail(taskId),
      labelingService.listQuestions(taskId),
      labelingService.getReviewSummary(taskId),
    ])
    const error = getSettledErrorMessage([taskResult, questionsResult, reviewSummaryResult], '标注工作台加载失败')

    set((state) => {
      const nextQuestions = questionsResult.status === 'fulfilled'
        ? questionsResult.value
        : taskResult.status === 'fulfilled'
          ? []
          : state.questions

      return {
        currentTask: taskResult.status === 'fulfilled' ? taskResult.value : state.currentTask,
        questions: nextQuestions,
        currentQuestion:
          questionsResult.status === 'fulfilled'
            ? nextQuestions[0] ?? null
            : state.currentQuestion,
        reviewSummary: reviewSummaryResult.status === 'fulfilled' ? reviewSummaryResult.value : state.reviewSummary,
        error:
          taskResult.status === 'fulfilled' && questionsResult.status === 'fulfilled' && reviewSummaryResult.status === 'rejected'
            ? '审核信息加载失败，工作台数据已展示'
            : error,
        isWorkbenchLoading: false,
      }
    })
  },
  loadDraft: async (taskId, questionId, userId) => {
    set({ error: null })

    try {
      const currentDraft = await labelingService.getDraft(taskId, questionId, userId)
      set({ currentDraft })
    } catch (error) {
      set({ error: getErrorMessage(error, '草稿加载失败') })
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
    } catch (error) {
      set({ error: getErrorMessage(error, '草稿保存失败') })

      return null
    } finally {
      set({ isDraftSaving: false })
    }
  },
  submitAnswers: async (taskId, userId, answers) => {
    set({ isSubmitting: true, error: null })

    try {
      const submission = await labelingService.submitAnswers(taskId, userId, answers)
      await Promise.allSettled([get().loadMarket(), get().loadSubmissions()])

      return submission
    } catch (error) {
      set({ error: getErrorMessage(error, '答案提交失败') })

      return null
    } finally {
      set({ isSubmitting: false })
    }
  },
  submitQuestionDraft: async (taskId, questionId, userId) => {
    set({ isSubmitting: true, error: null, submitValidation: null })

    try {
      const result = await labelingService.submitQuestionDraft(taskId, questionId, userId)
      set((state) => {
        const submittedQuestion = result.submission
          ? state.questions.find((question) => question.id === questionId)
          : null
        const nextQuestions = result.submission
          ? state.questions.map((question) =>
              question.id === questionId
                ? {
                    ...question,
                    status: 'submitted' as const,
                  }
                : question,
            )
          : state.questions
        const completedQuestions = nextQuestions.filter((question) => question.status === 'submitted').length

        return {
          submitValidation: result.validation,
          questions: nextQuestions,
          currentQuestion: submittedQuestion
            ? {
                ...submittedQuestion,
                status: 'submitted',
              }
            : state.currentQuestion,
          currentTask:
            result.submission && state.currentTask
              ? {
                  ...state.currentTask,
                  status: completedQuestions > 0 ? 'in_progress' : state.currentTask.status,
                  completedQuestions,
                }
              : state.currentTask,
        }
      })

      if (result.submission) {
        await Promise.allSettled([get().loadMarket(), get().loadSubmissions()])
      }

      return result
    } catch (error) {
      const message = getErrorMessage(error, '当前题目提交失败')
      const result: LabelingSubmitResult = {
        submission: null,
        validation: {
          valid: false,
          errors: [
            {
              questionId,
              questionTitle: '',
              message,
            },
          ],
        },
      }
      set({ error: message, submitValidation: result.validation })

      return result
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
                status: result.submission.status,
                completedQuestions: state.currentTask.totalQuestions,
                submittedAt: result.submission.submittedAt,
                reviewedAt: result.submission.reviewedAt,
                rejectReason: result.submission.rejectReason,
                reviewSource: result.submission.reviewSource,
                reviewStatus: result.submission.reviewStatus,
                aiDecision: result.submission.aiDecision,
                aiReviewSummary: result.submission.aiReviewSummary,
              }
            : state.currentTask,
      }))

      if (result.submission) {
        await Promise.allSettled([get().loadMarket(), get().loadSubmissions()])
      }

      return result
    } catch (error) {
      const message = getErrorMessage(error, '答案提交失败')
      const result: LabelingSubmitResult = {
        submission: null,
        validation: {
          valid: false,
          errors: [
            {
              questionId: '',
              questionTitle: '',
              message,
            },
          ],
        },
      }
      set({ error: message, submitValidation: result.validation })

      return result
    } finally {
      set({ isSubmitting: false })
    }
  },
  loadSubmissions: async () => {
    set({ isSubmissionsLoading: true, error: null })

    const [submissionStatsResult, submissionsResult] = await Promise.allSettled([
      labelingService.getSubmissionStats(),
      labelingService.listSubmissions(),
    ])
    const error = getSettledErrorMessage([submissionStatsResult, submissionsResult], '我的数据加载失败')

    set((state) => ({
      submissionStats: submissionStatsResult.status === 'fulfilled' ? submissionStatsResult.value : state.submissionStats,
      submissions: submissionsResult.status === 'fulfilled' ? submissionsResult.value : state.submissions,
      error:
        submissionsResult.status === 'fulfilled' && submissionStatsResult.status === 'rejected'
          ? '统计数据加载失败，列表数据已展示'
          : error,
      isSubmissionsLoading: false,
    }))
  },
  loadAssignments: async (query = {}) => {
    set({ isAssignmentsLoading: true, error: null })

    const assignmentsResult = await Promise.allSettled([
      labelingService.listAssignments(query),
    ]).then(([result]) => result)

    set((state) => ({
      assignments: assignmentsResult.status === 'fulfilled' ? assignmentsResult.value : state.assignments,
      assignmentStats: assignmentsResult.status === 'fulfilled'
        ? {
          total: assignmentsResult.value.reduce((total, assignment) => total + (assignment.myClaimedCount ?? 0), 0),
          claimed: assignmentsResult.value.filter((assignment) => assignment.status === 'CLAIMED').length,
          drafting: assignmentsResult.value.filter((assignment) => assignment.status === 'DRAFTING').length,
          submitted: assignmentsResult.value.reduce((total, assignment) => total + (assignment.mySubmittedCount ?? 0), 0),
          returned: assignmentsResult.value.filter((assignment) => assignment.status === 'RETURNED' || assignment.status === 'AI_RETURNED').length,
          approved: assignmentsResult.value.reduce((total, assignment) => total + (assignment.myApprovedCount ?? 0), 0),
          cancelled: assignmentsResult.value.filter((assignment) => assignment.status === 'CANCELLED').length,
        }
        : state.assignmentStats,
      error: assignmentsResult.status === 'rejected' ? getErrorMessage(assignmentsResult.reason, '我的领取加载失败') : null,
      isAssignmentsLoading: false,
    }))
  },
}))
