import { create } from 'zustand'
import { reviewerDashboardService } from '../services'
import type { ReviewerDashboardOverview } from '../types/dashboard'

interface ReviewerDashboardStore {
  data: ReviewerDashboardOverview | null
  range: number
  isLoading: boolean
  error: string | null
  setRange: (range: number) => void
  loadDashboard: (range?: number) => Promise<void>
}

export const useReviewerDashboardStore = create<ReviewerDashboardStore>((set, get) => ({
  data: null,
  range: 7,
  isLoading: false,
  error: null,
  setRange: (range) => set({ range }),
  loadDashboard: async (range) => {
    const resolvedRange = range ?? get().range
    set({ isLoading: true, error: null })

    try {
      const data = await reviewerDashboardService.getOverview(resolvedRange)
      set({ data, range: resolvedRange })
    } catch {
      set({ error: '审核员看板数据加载失败' })
    } finally {
      set({ isLoading: false })
    }
  },
}))
