import { create } from 'zustand'
import { ownerDashboardService } from '../services'
import type { OwnerDashboardOverview } from '../types/dashboard'

interface OwnerDashboardStore {
  data: OwnerDashboardOverview | null
  trendDays: number
  isLoading: boolean
  error: string | null
  setTrendDays: (trendDays: number) => void
  loadDashboard: (trendDays?: number) => Promise<void>
}

export const useOwnerDashboardStore = create<OwnerDashboardStore>((set) => ({
  data: null,
  trendDays: 7,
  isLoading: false,
  error: null,
  setTrendDays: (trendDays) => set({ trendDays }),
  loadDashboard: async (trendDays) => {
    set({ isLoading: true, error: null })

    try {
      const resolvedTrendDays = trendDays ?? useOwnerDashboardStore.getState().trendDays
      const data = await ownerDashboardService.getOverview(resolvedTrendDays)
      set({ data, trendDays: resolvedTrendDays })
    } catch {
      set({ error: '工作台数据加载失败' })
    } finally {
      set({ isLoading: false })
    }
  },
}))
