import { create } from 'zustand'
import { ownerDashboardService } from '../services'
import type { OwnerDashboardData } from '../types/task'

interface OwnerDashboardStore {
  data: OwnerDashboardData | null
  isLoading: boolean
  error: string | null
  loadDashboard: () => Promise<void>
}

export const useOwnerDashboardStore = create<OwnerDashboardStore>((set) => ({
  data: null,
  isLoading: false,
  error: null,
  loadDashboard: async () => {
    set({ isLoading: true, error: null })

    try {
      const data = await ownerDashboardService.getDashboardData()
      set({ data })
    } catch {
      set({ error: '工作台数据加载失败' })
    } finally {
      set({ isLoading: false })
    }
  },
}))
