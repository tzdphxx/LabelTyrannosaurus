import { create } from 'zustand'
import { reviewService } from '../services'
import type {
  AiReviewResult,
  BatchManualReviewResult,
  ManualReviewActionPayload,
  ManualReviewRecord,
  ReviewDetail,
  ReviewQueueItem,
  ReviewQueueQuery,
} from '../types/review'

interface ReviewStore {
  queue: ReviewQueueItem[]
  history: ReviewQueueItem[]
  filters: ReviewQueueQuery
  currentDetail: ReviewDetail | null
  currentAiResult: AiReviewResult | null
  manualReviewRecords: ManualReviewRecord[]
  selectedReviewIds: string[]
  isQueueLoading: boolean
  isDetailLoading: boolean
  isAiResultLoading: boolean
  isHistoryLoading: boolean
  isActionSubmitting: boolean
  isBatchSubmitting: boolean
  error: string | null
  setFilters: (filters: Partial<ReviewQueueQuery>) => void
  setSelectedReviewIds: (reviewIds: string[]) => void
  loadQueue: () => Promise<void>
  loadHistory: () => Promise<void>
  loadDetail: (reviewId: string) => Promise<ReviewDetail | null>
  loadAiResult: (submissionId: string) => Promise<AiReviewResult | null>
  loadManualReviewRecords: (submissionId: string) => Promise<void>
  submitManualReviewAction: (reviewId: string, payload: ManualReviewActionPayload) => Promise<ReviewDetail | null>
  submitBatchManualReviewAction: (reviewIds: string[], payload: ManualReviewActionPayload) => Promise<BatchManualReviewResult | null>
}

const initialFilters: ReviewQueueQuery = {
  keyword: '',
  riskLevel: 'all',
  manualStatus: 'all',
}

export const useReviewStore = create<ReviewStore>((set, get) => ({
  queue: [],
  history: [],
  filters: initialFilters,
  currentDetail: null,
  currentAiResult: null,
  manualReviewRecords: [],
  selectedReviewIds: [],
  isQueueLoading: false,
  isDetailLoading: false,
  isAiResultLoading: false,
  isHistoryLoading: false,
  isActionSubmitting: false,
  isBatchSubmitting: false,
  error: null,
  setFilters: (filters) => {
    set((state) => ({
      filters: {
        ...state.filters,
        ...filters,
      },
    }))
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
