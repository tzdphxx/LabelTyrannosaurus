import type { LabelerTaskStatus } from '../types/labeling'

export const labelerTaskStatusLabels: Record<LabelerTaskStatus, string> = {
  available: '可领取',
  claimed: '已领取',
  in_progress: '进行中',
  submitted: '已提交',
  approved: '已通过',
  rejected: '待修改',
  ended: '已结束',
}

export const labelerTaskStatusColors: Record<LabelerTaskStatus, string> = {
  available: 'processing',
  claimed: 'warning',
  in_progress: 'blue',
  submitted: 'geekblue',
  approved: 'success',
  rejected: 'error',
  ended: 'default',
}

export function getTaskProgressLabel(completedQuestions: number, totalQuestions: number) {
  if (totalQuestions <= 0) {
    return '0 / 0'
  }

  return `${completedQuestions} / ${totalQuestions}`
}

export function getTaskProgressPercent(completedQuestions: number, totalQuestions: number) {
  if (totalQuestions <= 0) {
    return 0
  }

  return Math.round((completedQuestions / totalQuestions) * 100)
}
