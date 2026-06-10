import { create } from 'zustand'
import { adminDashboardService } from '../services'
import type { AdminDashboardOverview, AdminDashboardRange } from '../types/admin'

interface AdminDashboardStore {
  data: AdminDashboardOverview | null
  range: AdminDashboardRange
  isLoading: boolean
  error: string | null
  setRange: (range: AdminDashboardRange) => void
  loadDashboard: (range?: AdminDashboardRange) => Promise<void>
}

export const useAdminDashboardStore = create<AdminDashboardStore>((set) => ({
  data: null,
  range: '7d',
  isLoading: false,
  error: null,
  setRange: (range) => set({ range }),
  loadDashboard: async (range) => {
    set({ isLoading: true, error: null })

    try {
      const resolvedRange = range ?? useAdminDashboardStore.getState().range
      const data = await adminDashboardService.getOverview(resolvedRange)
      set({ data, range: resolvedRange })
    } catch {
      set({ error: '管理员数据看板加载失败' })
    } finally {
      set({ isLoading: false })
    }
  },
}))
