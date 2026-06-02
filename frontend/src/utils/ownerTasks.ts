import type { OwnerTaskStatus, TaskProgress } from '../types/task'

export const ownerTaskStatusLabels: Record<OwnerTaskStatus, string> = {
  draft: '草稿',
  published: '已发布',
  paused: '已暂停',
  ended: '已结束',
}

export const ownerTaskStatusColors: Record<OwnerTaskStatus, string> = {
  draft: 'default',
  published: 'processing',
  paused: 'warning',
  ended: 'success',
}

export const distributionStrategyLabels = {
  先到先得: '先到先得',
  配额分发: '配额分发',
  指派: '指派',
}

export function getProgressPercent(progress: TaskProgress) {
  if (progress.totalItems <= 0) {
    return 0
  }

  return Math.round((progress.completedItems / progress.totalItems) * 100)
}

export function formatCount(value: number) {
  return new Intl.NumberFormat('zh-CN').format(value)
}
