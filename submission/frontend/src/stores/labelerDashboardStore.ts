import { create } from 'zustand'
import { labelerDashboardService } from '../services'
import type { LabelerDashboardOverview } from '../types/dashboard'

interface LabelerDashboardStore {
  data: LabelerDashboardOverview | null
  range: number
  isLoading: boolean
  error: string | null
  setRange: (range: number) => void
  loadDashboard: (range?: number) => Promise<void>
}

export const useLabelerDashboardStore = create<LabelerDashboardStore>((set, get) => ({
  data: null,
  range: 7,
  isLoading: false,
  error: null,
  setRange: (range) => set({ range }),
  loadDashboard: async (range) => {
    const resolvedRange = range ?? get().range
    set({ isLoading: true, error: null })

    try {
      const data = await labelerDashboardService.getOverview(resolvedRange)
      set({ data, range: resolvedRange })
    } catch {
      set({ error: '标注员看板数据加载失败' })
    } finally {
      set({ isLoading: false })
    }
  },
}))
