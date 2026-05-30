import { mockDashboardFocusTaskId } from '../../mocks'
import type { OwnerDashboardData } from '../../types/task'
import { ownerTaskService } from './ownerTaskService'

export const ownerDashboardService = {
  async getDashboardData(): Promise<OwnerDashboardData> {
    const tasks = await ownerTaskService.listTasks({ keyword: '', status: 'all' })
    const focusedTask = tasks.find((task) => task.id === mockDashboardFocusTaskId) ?? tasks[0] ?? null

    return {
      stats: {
        totalTasks: tasks.length,
        draftTasks: tasks.filter((task) => task.status === 'draft').length,
        publishedTasks: tasks.filter((task) => task.status === 'published').length,
        runningTasks: tasks.filter((task) => task.status === 'published' || task.status === 'paused').length,
        importIssueTasks: tasks.filter((task) => task.progress.abnormalItems > 0).length,
      },
      focusedTask,
      recentTasks: tasks.slice(0, 4),
    }
  },
}
