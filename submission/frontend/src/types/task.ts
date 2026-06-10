import type { ImportPreview } from './import'
import type { LlmProviderResponse } from './llmProvider'
import type { TemplateSummary } from './template'

export type OwnerTaskStatus = 'draft' | 'published' | 'paused' | 'ended'
export type OwnerTaskApiStatus = 'DRAFT' | 'PUBLISHED' | 'PAUSED' | 'ENDED'

export type DistributionStrategy = '先到先得' | '配额分发' | '指派'
export type DistributionStrategyCode = 'FCFS' | 'QUOTA_GRAB' | 'ASSIGNED'
export type AiReviewStrategy = 'LIGHTWEIGHT' | 'PARALLEL_VOTE' | 'DEEP_DIMENSION' | 'AGENT_DEBATE'
export type AiFlowPolicy = 'MANUAL_FIRST' | 'AI_PASS_ONLY' | 'AI_REJECT_ONLY' | 'AI_PASS_AND_REJECT' | 'ALWAYS_MANUAL'
export type RewardMode = 'APPROVED_ITEM'
export type RewardCurrency = 'POINT'

export interface RewardRule {
  unitPrice: number
  currency: 'CNY'
  description: string
  rewardMode: RewardMode
  rewardCurrency: RewardCurrency
  rewardVisible: boolean
}

export interface AiReviewConfigDraft {
  aiPrompt: string
  aiModelName: string
  aiProviderId?: string | null
  aiScoringDimensions: string[]
  aiPassThreshold: number
  aiManualReviewThreshold: number
  aiReviewStrategy: AiReviewStrategy
  aiFlowPolicy: AiFlowPolicy
}

export type OwnerModelOptionResponse = LlmProviderResponse

export interface TaskProgress {
  totalItems: number
  distributedItems: number
  completedItems: number
  pendingReviewItems: number
  approvedItems: number
  rejectedItems: number
  abnormalItems: number
  passRate?: string
}

export interface OwnerTask {
  id: string
  title: string
  description: string
  instruction: string
  tags: string[]
  deadline: string
  quota: number
  claimedCount: number
  rewardRule: RewardRule
  distributionStrategy: DistributionStrategy
  publishedTemplateVersionId: string | null
  templateName: string
  status: OwnerTaskStatus
  dataCount: number
  updatedAt: string
  createdAt: string
  progress: TaskProgress
  aiReview: AiReviewConfigDraft
  reviewLevelCount: number
  overlapCount: number
  maxClaimsPerLabeler: number
  assignedLabelerId?: string | null
  datasetFileId?: string | null
  ownerId?: string
  publishedAt?: string | null
  endedAt?: string | null
  importPreviewId?: string
}

export interface TaskListQuery {
  keyword: string
  status: OwnerTaskStatus | 'all'
  page: number
  pageSize: number
}

export interface OwnerTaskPage {
  items: OwnerTask[]
  page: number
  pageSize: number
  total: number
}

export interface DatasetItemResponse {
  itemId: number
  taskId: number
  externalId: string
  itemJson: Record<string, unknown>
  metadataJson: Record<string, unknown>
  assignedCount: number
  submittedCount: number
  approvedCount: number
  itemStatus: 'UNCLAIMED' | 'CLAIMED' | 'DRAFT' | 'SUBMITTED' | 'RETURNED' | 'APPROVED' | ''
  labelerId: number | null
  createdAt: string
  updatedAt: string
}

export interface DatasetItemPageResponse {
  items: DatasetItemResponse[]
  page: number
  pageSize: number
  total: number
}

export interface DatasetItemPageQuery {
  page: number
  pageSize: number
  externalId?: string
}

export interface DatasetItemAppendInput {
  externalId: string
  itemJson: Record<string, unknown>
  metadataJson: Record<string, unknown>
}

export interface DatasetItemBatchAppendRequest {
  items: DatasetItemAppendInput[]
}

export interface DatasetItemAppendResult {
  itemId: number
  externalId: string
  success: boolean
  errorCode: number
  errorMessage: string
}

export type TaskDraft = Omit<OwnerTask, 'claimedCount' | 'createdAt' | 'dataCount' | 'progress' | 'status' | 'templateName' | 'updatedAt'> & {
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

export interface OwnerTaskResponseRewardRule {
  rewardMode?: RewardMode
  unitReward?: number
  rewardCurrency?: RewardCurrency
  rewardVisible?: boolean
}

export interface OwnerTaskSummaryResponse {
  taskId: number
  title: string
  status: OwnerTaskApiStatus
  quota: number
  claimedCount: number
  createdAt: string
  tags?: string[]
  tag?: string[]
  description?: string
  deadlineAt: string
  rewardRule?: OwnerTaskResponseRewardRule | null
}

export interface OwnerTaskPageResponse {
  items: OwnerTaskSummaryResponse[]
  page: number
  pageSize: number
  total: number
}

export interface TaskDetailResponse {
  taskId: number
  ownerId: number
  title: string
  description?: string
  instructionRichText?: string
  status: OwnerTaskApiStatus
  tags?: string[]
  quota: number
  claimedCount: number
  deadlineAt: string
  publishedTemplateVersionId?: number | null
  aiPrompt?: string
  aiModelName?: string
  aiProviderId?: number | null
  aiScoringDimensions?: string[]
  aiPassThreshold?: number
  aiManualReviewThreshold?: number
  aiReview?: {
    modelName?: string
    promptTemplate?: string
    scoringDimensions?: string[]
    passThreshold?: number
    manualReviewThreshold?: number
    reviewStrategy?: AiReviewStrategy
    flowPolicy?: AiFlowPolicy
  } | null
  reviewLevelCount?: number
  overlapCount?: number
  maxClaimsPerLabeler?: number
  assignedLabelerId?: number | null
  aiFlowPolicy?: AiFlowPolicy
  aiReviewStrategy?: AiReviewStrategy
  rewardRule?: {
    rewardMode?: RewardMode
    unitReward?: number
    rewardCurrency?: RewardCurrency
    rewardVisible?: boolean
  } | null
  publishedAt?: string | null
  endedAt?: string | null
  createdAt: string
  updatedAt: string
  reward: string
  strategy: DistributionStrategyCode
}

export interface CreateTaskRequest {
  title: string
  description?: string
  instructionRichText?: string
  tags?: string[]
  quota: number
  deadlineAt: string
  overlapCount: number
  publishedTemplateVersionId?: number
  aiReviewConfigId?: number
  aiPrompt?: string
  aiModelName?: string
  aiProviderId?: number
  aiScoringDimensions?: string[]
  aiPassThreshold?: number
  aiManualReviewThreshold?: number
  aiReviewStrategy?: AiReviewStrategy
  aiFlowPolicy?: AiFlowPolicy
  reviewLevelCount?: number
  maxClaimsPerLabeler: number
  assignedLabelerId?: number
  datasetFileId?: number
  rewardRule: {
    rewardMode: RewardMode
    unitReward: number
    rewardCurrency: RewardCurrency
    rewardVisible: boolean
  }
  strategy: DistributionStrategyCode
}

export type UpdateTaskRequest = Omit<CreateTaskRequest, 'datasetFileId'>

export type TaskExportFormat = 'JSON' | 'JSONL' | 'CSV' | 'XLSX'

export interface TaskDirectExportRequest {
  format: TaskExportFormat
}

export interface TaskDirectExportResponse {
  fileId: number
  filename: string
  contentType: string
  fileSize: number
  checksum: string
  downloadUrl: string
  exportedCount: number
}

export interface CreateTaskResponse {
  taskId: number
  status: OwnerTaskApiStatus
  datasetImportJob: unknown | null
}

export interface TaskLifecycleResponse {
  taskId: number
  status: OwnerTaskApiStatus
}

export interface TaskStatisticsResponse {
  taskId: number
  totalItems: number
  claimedCount: number
  submittedCount: number
  approvedCount: number
  rejectedCount: number
  pendingReviewCount: number
  passRate: string
}

export interface OwnerLabelerQuery {
  keyword?: string
  page: number
  size: number
}

export interface OwnerLabelerOption {
  labelerId: number
  username: string
  email: string
  displayName: string
  avatarUrl: string
  enabled: boolean
  loginEnabled: boolean
  claimedCount: number
  submittedCount: number
  pendingReviewCount: number
  approvedCount: number
  rejectedCount: number
  totalReward: number
  todaySubmittedCount: number
  lastSubmitDate: string
  statsUpdatedAt: string
  approvalRate: number
}

export interface OwnerLabelerPageResponse {
  items: OwnerLabelerOption[]
  page: number
  pageSize: number
  total: number
}
