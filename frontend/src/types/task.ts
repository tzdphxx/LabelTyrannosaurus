import type { ImportPreview } from './import'
import type { TemplateSummary } from './template'

export type OwnerTaskStatus = 'draft' | 'published' | 'paused' | 'ended'

export type DistributionStrategy = 'manual' | 'auto' | 'balanced'

export interface RewardRule {
  unitPrice: number
  currency: 'CNY'
  description: string
}

export interface TaskProgress {
  totalItems: number
  distributedItems: number
  completedItems: number
  pendingReviewItems: number
  approvedItems: number
  rejectedItems: number
  abnormalItems: number
}

export interface OwnerTask {
  id: string
  title: string
  description: string
  instruction: string
  tags: string[]
  deadline: string
  rewardRule: RewardRule
  distributionStrategy: DistributionStrategy
  templateId: string | null
  templateName: string
  status: OwnerTaskStatus
  dataCount: number
  updatedAt: string
  createdAt: string
  progress: TaskProgress
  importPreviewId?: string
}

export interface TaskListQuery {
  keyword: string
  status: OwnerTaskStatus | 'all'
}

export type TaskDraft = Omit<OwnerTask, 'createdAt' | 'dataCount' | 'progress' | 'status' | 'templateName' | 'updatedAt'> & {
  status?: OwnerTaskStatus
}

export type TaskDraftInput = Omit<TaskDraft, 'id' | 'importPreviewId'>

export interface PublishValidationResult {
  valid: boolean
  errors: string[]
}

export interface OwnerTaskDetail {
  task: OwnerTask
  template: TemplateSummary | null
  importPreview: ImportPreview | null
}

export interface OwnerDashboardStats {
  totalTasks: number
  draftTasks: number
  publishedTasks: number
  runningTasks: number
  importIssueTasks: number
}

export interface OwnerDashboardData {
  stats: OwnerDashboardStats
  focusedTask: OwnerTask | null
  recentTasks: OwnerTask[]
}
