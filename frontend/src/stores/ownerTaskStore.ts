import { create } from 'zustand'
import { ownerTaskService } from '../services'
import type {
  DatasetItemAppendInput,
  DatasetItemAppendResult,
  DatasetItemPageQuery,
  DatasetItemPageResponse,
  OwnerTask,
  OwnerTaskDetail,
  OwnerTaskStatus,
  TaskListQuery,
  TaskProgress,
} from '../types/task'

interface OwnerTaskStore {
  tasks: OwnerTask[]
  currentTaskDetail: OwnerTaskDetail | null
  currentTaskProgress: TaskProgress | null
  currentDatasetItemsPage: DatasetItemPageResponse | null
  filters: TaskListQuery
  total: number
  isListLoading: boolean
  isDetailLoading: boolean
  isProgressLoading: boolean
  isDatasetItemsLoading: boolean
  isAppendingDatasetItems: boolean
  isStatusSubmitting: boolean
  isDeleting: boolean
  error: string | null
  setFilters: (filters: Partial<TaskListQuery>) => void
  loadTasks: () => Promise<void>
  loadTaskDetail: (taskId: string) => Promise<void>
  loadTaskDatasetItems: (taskId: string, query: DatasetItemPageQuery) => Promise<void>
  appendTaskDatasetItems: (taskId: string, items: DatasetItemAppendInput[]) => Promise<DatasetItemAppendResult[] | null>
  publishTask: (taskId: string) => Promise<OwnerTask | null>
  updateTaskStatus: (taskId: string, status: OwnerTaskStatus) => Promise<OwnerTask | null>
  deleteTask: (taskId: string) => Promise<boolean>
}

const initialFilters: TaskListQuery = {
  keyword: '',
  page: 1,
  pageSize: 20,
  status: 'all',
}

export const useOwnerTaskStore = create<OwnerTaskStore>((set, get) => ({
  tasks: [],
  currentTaskDetail: null,
  currentTaskProgress: null,
  currentDatasetItemsPage: null,
  filters: initialFilters,
  total: 0,
  isListLoading: false,
  isDetailLoading: false,
  isProgressLoading: false,
  isDatasetItemsLoading: false,
  isAppendingDatasetItems: false,
  isStatusSubmitting: false,
  isDeleting: false,
  error: null,
  setFilters: (filters) => {
    set((state) => ({
      filters: {
        ...state.filters,
        ...filters,
        page: Object.hasOwn(filters, 'keyword') || Object.hasOwn(filters, 'status') ? 1 : (filters.page ?? state.filters.page),
      },
    }))
  },
  loadTasks: async () => {
    set({ isListLoading: true, error: null })

    try {
      const taskPage = await ownerTaskService.listTasks(get().filters)
      set({
        filters: {
          ...get().filters,
          page: taskPage.page,
          pageSize: taskPage.pageSize,
        },
        tasks: taskPage.items,
        total: taskPage.total,
      })
    } catch {
      set({ error: '任务列表加载失败' })
    } finally {
      set({ isListLoading: false })
    }
  },
  loadTaskDetail: async (taskId) => {
    set({ isDetailLoading: true, isProgressLoading: true, isDatasetItemsLoading: true, error: null })

    try {
      const [detail, progress, datasetItemsPage] = await Promise.all([
        ownerTaskService.getTaskDetail(taskId),
        ownerTaskService.getTaskProgress(taskId),
        ownerTaskService.listTaskDatasetItems(taskId, { page: 1, pageSize: 10 }),
      ])
      set({
        currentTaskDetail: detail,
        currentTaskProgress: progress,
        currentDatasetItemsPage: datasetItemsPage,
      })
    } catch {
      set({ error: '任务详情加载失败' })
    } finally {
      set({ isDetailLoading: false, isProgressLoading: false, isDatasetItemsLoading: false })
    }
  },
  loadTaskDatasetItems: async (taskId, query) => {
    set({ isDatasetItemsLoading: true, error: null })

    try {
      set({
        currentDatasetItemsPage: await ownerTaskService.listTaskDatasetItems(taskId, query),
      })
    } catch {
      set({ error: '任务题目加载失败' })
    } finally {
      set({ isDatasetItemsLoading: false })
    }
  },
  appendTaskDatasetItems: async (taskId, items) => {
    set({ isAppendingDatasetItems: true, error: null })

    try {
      const results = await ownerTaskService.batchAppendDatasetItems(taskId, items)
      const currentPage = get().currentDatasetItemsPage
      await get().loadTaskDatasetItems(taskId, {
        page: currentPage?.page ?? 1,
        pageSize: currentPage?.pageSize ?? 10,
      })

      return results
    } catch {
      set({ error: '任务题目添加失败' })

      return null
    } finally {
      set({ isAppendingDatasetItems: false })
    }
  },
  publishTask: async (taskId) => {
    set({ isStatusSubmitting: true, error: null })

    try {
      const task = await ownerTaskService.publishTask(taskId)
      await get().loadTasks()

      if (task) {
        await get().loadTaskDetail(task.id)
      }

      return task
    } catch {
      set({ error: '任务发布失败' })

      return null
    } finally {
      set({ isStatusSubmitting: false })
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
  deleteTask: async (taskId) => {
    set({ isDeleting: true, error: null })

    try {
      const deleted = await ownerTaskService.deleteTask(taskId)

      if (deleted) {
        await get().loadTasks()
      }

      return deleted
    } catch {
      set({ error: '任务删除失败' })

      return false
    } finally {
      set({ isDeleting: false })
    }
  },
}))
