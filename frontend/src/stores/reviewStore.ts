import { create } from 'zustand'
import { reviewService } from '../services'
import type {
  AiReviewLogQuery,
  AiReviewResult,
  AiReviewResultResponse,
  BatchManualReviewResult,
  ManualReviewActionPayload,
  ManualReviewRecord,
  ReviewDetail,
  ReviewQueueItem,
  ReviewQueueQuery,
  SubmissionVersion,
} from '../types/review'

interface ReviewStore {
  queue: ReviewQueueItem[]
  history: ReviewQueueItem[]
  filters: ReviewQueueQuery
  currentDetail: ReviewDetail | null
  currentAiResult: AiReviewResult | null
  manualReviewRecords: ManualReviewRecord[]
  submissionVersions: SubmissionVersion[]
  aiReviewLogs: AiReviewResultResponse[]
  currentAiReviewLog: AiReviewResultResponse | null
  aiReviewLogQuery: AiReviewLogQuery
  aiReviewLogTotal: number
  selectedReviewIds: string[]
  todayReviewedCount: number
  isQueueLoading: boolean
  isDetailLoading: boolean
  isVersionsLoading: boolean
  isAiResultLoading: boolean
  isHistoryLoading: boolean
  isActionSubmitting: boolean
  isBatchSubmitting: boolean
  isAiReviewLogsLoading: boolean
  isAiReviewRetrying: boolean
  error: string | null
  setFilters: (filters: Partial<ReviewQueueQuery>) => void
  setAiReviewLogQuery: (query: Partial<AiReviewLogQuery>) => void
  setCurrentAiReviewLog: (log: AiReviewResultResponse | null) => void
  setSelectedReviewIds: (reviewIds: string[]) => void
  loadQueue: () => Promise<void>
  loadHistory: () => Promise<void>
  loadAllAiReviewLogs: () => Promise<void>
  loadSubmissionAiReview: (submissionId: string) => Promise<AiReviewResultResponse | null>
  retrySubmissionAiReview: (submissionId: string) => Promise<AiReviewResultResponse | null>
  loadDetail: (reviewId: string) => Promise<ReviewDetail | null>
  loadAiResult: (submissionId: string) => Promise<AiReviewResult | null>
  loadManualReviewRecords: (submissionId: string) => Promise<void>
  loadSubmissionVersions: (submissionId: string) => Promise<void>
  submitManualReviewAction: (reviewId: string, payload: ManualReviewActionPayload) => Promise<ReviewDetail | null>
  submitBatchManualReviewAction: (reviewIds: string[], payload: ManualReviewActionPayload) => Promise<BatchManualReviewResult | null>
}

const initialFilters: ReviewQueueQuery = {
  keyword: '',
  riskLevel: 'all',
  manualStatus: 'all',
}

const initialAiReviewLogQuery: AiReviewLogQuery = {
  page: 1,
  pageSize: 20,
}

export const useReviewStore = create<ReviewStore>((set, get) => ({
  queue: [],
  history: [],
  filters: initialFilters,
  currentDetail: null,
  currentAiResult: null,
  manualReviewRecords: [],
  submissionVersions: [],
  aiReviewLogs: [],
  currentAiReviewLog: null,
  aiReviewLogQuery: initialAiReviewLogQuery,
  aiReviewLogTotal: 0,
  selectedReviewIds: [],
  todayReviewedCount: 0,
  isQueueLoading: false,
  isDetailLoading: false,
  isVersionsLoading: false,
  isAiResultLoading: false,
  isHistoryLoading: false,
  isActionSubmitting: false,
  isBatchSubmitting: false,
  isAiReviewLogsLoading: false,
  isAiReviewRetrying: false,
  error: null,
  setFilters: (filters) => {
    set((state) => ({
      filters: {
        ...state.filters,
        ...filters,
      },
    }))
  },
  setAiReviewLogQuery: (query) => {
    set((state) => ({
      aiReviewLogQuery: {
        ...state.aiReviewLogQuery,
        ...query,
      },
    }))
  },
  setCurrentAiReviewLog: (log) => {
    set({ currentAiReviewLog: log })
  },
  setSelectedReviewIds: (reviewIds) => {
    set({ selectedReviewIds: reviewIds })
  },
  loadQueue: async () => {
    set({ isQueueLoading: true, error: null })

    try {
      const queue = await reviewService.listManualReviewQueue(get().filters)
      set({ queue })
    } catch {
      set({ error: '人工复核队列加载失败' })
    } finally {
      set({ isQueueLoading: false })
    }
  },
  loadHistory: async () => {
    set({ isHistoryLoading: true, error: null })

    try {
      const history = await reviewService.listReviewHistory()
      set({ history })
    } catch {
      set({ error: '审核历史加载失败' })
    } finally {
      set({ isHistoryLoading: false })
    }
  },
  loadAllAiReviewLogs: async () => {
    set({ isAiReviewLogsLoading: true, error: null })

    try {
      const result = await reviewService.listAllAiReviewLogs(get().aiReviewLogQuery)
      set((state) => ({
        aiReviewLogs: result.items,
        aiReviewLogTotal: result.total,
        currentAiReviewLog: state.currentAiReviewLog ?? result.items[0] ?? null,
      }))
    } catch {
      set({ error: 'AI 审核队列加载失败', aiReviewLogs: [], aiReviewLogTotal: 0, currentAiReviewLog: null })
    } finally {
      set({ isAiReviewLogsLoading: false })
    }
  },
  loadSubmissionAiReview: async (submissionId) => {
    set({ isAiReviewLogsLoading: true, error: null })

    try {
      const currentAiReviewLog = await reviewService.getSubmissionAiReview(submissionId)
      set({ currentAiReviewLog })

      return currentAiReviewLog
    } catch {
      set({ error: 'AI 审核详情加载失败' })

      return null
    } finally {
      set({ isAiReviewLogsLoading: false })
    }
  },
  retrySubmissionAiReview: async (submissionId) => {
    set({ isAiReviewRetrying: true, error: null })

    try {
      const currentAiReviewLog = await reviewService.retrySubmissionAiReview(submissionId)
      set((state) => ({
        currentAiReviewLog,
        aiReviewLogs: state.aiReviewLogs.map((log) =>
          String(log.submissionId) === submissionId ? currentAiReviewLog : log,
        ),
      }))

      return currentAiReviewLog
    } catch {
      set({ error: 'AI 审核重试失败' })

      return null
    } finally {
      set({ isAiReviewRetrying: false })
    }
  },
  loadDetail: async (reviewId) => {
    set({ isDetailLoading: true, error: null })

    try {
      const currentDetail = await reviewService.getReviewDetail(reviewId)
      set({ currentDetail })

      return currentDetail
    } catch {
      set({ error: '人工复核详情加载失败', currentDetail: null })

      return null
    } finally {
      set({ isDetailLoading: false })
    }
  },
  loadAiResult: async (submissionId) => {
    set({ isAiResultLoading: true, error: null })

    try {
      const currentAiResult = await reviewService.getAiReviewResult(submissionId)
      set({ currentAiResult })

      return currentAiResult
    } catch {
      set({ error: 'AI 审核结果加载失败', currentAiResult: null })

      return null
    } finally {
      set({ isAiResultLoading: false })
    }
  },
  loadManualReviewRecords: async (submissionId) => {
    set({ isHistoryLoading: true, error: null })

    try {
      const manualReviewRecords = await reviewService.listManualReviewRecords(submissionId)
      set({ manualReviewRecords })
    } catch {
      set({ error: '人工复核历史加载失败' })
    } finally {
      set({ isHistoryLoading: false })
    }
  },
  loadSubmissionVersions: async (submissionId) => {
    set({ isVersionsLoading: true, error: null })

    try {
      const submissionVersions = await reviewService.listSubmissionVersions(submissionId)
      set({ submissionVersions })
    } catch {
      set({ error: '历史提交记录加载失败', submissionVersions: [] })
    } finally {
      set({ isVersionsLoading: false })
    }
  },
  submitManualReviewAction: async (reviewId, payload) => {
    set({ isActionSubmitting: true, error: null })

    try {
      const currentDetail = await reviewService.submitManualReviewAction(reviewId, payload)

      if (currentDetail) {
        set((state) => ({
          currentDetail,
          queue: state.queue.map((item) =>
            item.id === currentDetail.id
              ? {
                  ...item,
                  manualReviewStatus: currentDetail.manualReviewStatus,
                  submissionReviewStatus: currentDetail.submissionReviewStatus,
                }
              : item,
          ),
          todayReviewedCount: state.todayReviewedCount + 1,
        }))
      }

      return currentDetail
    } catch {
      set({ error: '人工复核操作失败' })

      return null
    } finally {
      set({ isActionSubmitting: false })
    }
  },
  submitBatchManualReviewAction: async (reviewIds, payload) => {
    set({ isBatchSubmitting: true, error: null })

    try {
      const result = await reviewService.submitBatchManualReviewAction(reviewIds, payload)
      set((state) => ({
        selectedReviewIds: [],
        queue: state.queue.map((item) => {
          const updatedDetail = result.success.find((detail) => detail.id === item.id)

          return updatedDetail
            ? {
                ...item,
                manualReviewStatus: updatedDetail.manualReviewStatus,
                submissionReviewStatus: updatedDetail.submissionReviewStatus,
              }
            : item
        }),
      }))

      return result
    } catch {
      set({ error: '批量人工复核操作失败' })

      return null
    } finally {
      set({ isBatchSubmitting: false })
    }
  },
}))
