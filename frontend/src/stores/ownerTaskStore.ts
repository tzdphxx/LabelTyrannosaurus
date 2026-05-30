import { create } from 'zustand'
import { ownerTaskService } from '../services'
import type { OwnerTask, OwnerTaskDetail, OwnerTaskStatus, TaskListQuery, TaskProgress } from '../types/task'

interface OwnerTaskStore {
  tasks: OwnerTask[]
  currentTaskDetail: OwnerTaskDetail | null
  currentTaskProgress: TaskProgress | null
  filters: TaskListQuery
  isListLoading: boolean
  isDetailLoading: boolean
  isProgressLoading: boolean
  isStatusSubmitting: boolean
  error: string | null
  setFilters: (filters: Partial<TaskListQuery>) => void
  loadTasks: () => Promise<void>
  loadTaskDetail: (taskId: string) => Promise<void>
  updateTaskStatus: (taskId: string, status: OwnerTaskStatus) => Promise<OwnerTask | null>
}

const initialFilters: TaskListQuery = {
  keyword: '',
  status: 'all',
}

export const useOwnerTaskStore = create<OwnerTaskStore>((set, get) => ({
  tasks: [],
  currentTaskDetail: null,
  currentTaskProgress: null,
  filters: initialFilters,
  isListLoading: false,
  isDetailLoading: false,
  isProgressLoading: false,
  isStatusSubmitting: false,
  error: null,
  setFilters: (filters) => {
    set((state) => ({
      filters: {
        ...state.filters,
        ...filters,
      },
    }))
  },
  loadTasks: async () => {
    set({ isListLoading: true, error: null })

    try {
      const tasks = await ownerTaskService.listTasks(get().filters)
      set({ tasks })
    } catch {
      set({ error: '任务列表加载失败' })
    } finally {
      set({ isListLoading: false })
    }
  },
  loadTaskDetail: async (taskId) => {
    set({ isDetailLoading: true, isProgressLoading: true, error: null })

    try {
      const [detail, progress] = await Promise.all([
        ownerTaskService.getTaskDetail(taskId),
        ownerTaskService.getTaskProgress(taskId),
      ])
      set({
        currentTaskDetail: detail,
        currentTaskProgress: progress,
      })
    } catch {
      set({ error: '任务详情加载失败' })
    } finally {
      set({ isDetailLoading: false, isProgressLoading: false })
    }
  },
  updateTaskStatus: async (taskId, status) => {
    set({ isStatusSubmitting: true, error: null })

    try {
      const task = await ownerTaskService.updateTaskStatus(taskId, status)
      await get().loadTasks()

      if (task) {
        await get().loadTaskDetail(task.id)
      }

      return task
    } catch {
      set({ error: '任务状态更新失败' })

      return null
    } finally {
      set({ isStatusSubmitting: false })
    }
  },
}))
