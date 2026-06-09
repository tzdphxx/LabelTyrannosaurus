import type { LabelerTaskStatus, LabelingQuestion } from '../types/labeling'

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

export const labelingQuestionStatusLabels: Record<LabelingQuestion['status'], string> = {
  pending: '待标注',
  in_progress: '进行中',
  rejected: '已打回',
  submitted: '已提交',
  draft: '草稿',
}

export const labelingQuestionStatusColors: Record<LabelingQuestion['status'], string> = {
  pending: 'default',
  in_progress: 'processing',
  rejected: 'error',
  submitted: 'success',
  draft: 'warning',
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
