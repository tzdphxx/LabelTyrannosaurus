import type { DynamicFormSchema, DynamicFormSubmitResult } from './dynamicForm'

export type AiReviewDecision = 'pass' | 'manual_review' | 'reject'
export type AiReviewStatus = 'pending' | 'completed' | 'failed'
export type ManualReviewStatus = 'none' | 'pending' | 'in_progress' | 'approved' | 'rejected'
export type SubmissionReviewStatus =
  | 'ai_pending'
  | 'ai_passed'
  | 'ai_rejected'
  | 'manual_pending'
  | 'manual_approved'
  | 'manual_rejected'
export type ReviewRiskLevel = 'low' | 'medium' | 'high'
export type ManualReviewDecision = 'approved' | 'rejected'

export interface ReviewerSubmissionListItem {
  submissionId: number
  taskId: number
  datasetItemId: number
  labelerId: number
  submissionStatus: string
  aiDecision: string
  aiReviewStatus: string
  conflictStatus: string | null
  reviewLevel: number
  assignedReviewerId: number | null
  createdAt: string
  updatedAt: string
}

export interface ReviewClaimResponse {
  taskId?: number
  reviewLevel?: number
  claimedSubmissionIds: number[]
  claimedCount: number
}

export interface ReviewerTaskSummary {
  taskId: number
  taskTitle: string
  pendingCount: number
  myPendingCount: number
  totalReviewedCount: number
  claimed: boolean
  claimedByMe: boolean
}

export interface ReviewerTaskStatusSummary {
  unclaimedCount: number
  claimedCount: number
  draftCount: number
  submittedCount: number
  returnedCount: number
  approvedCount: number
}

export interface ReviewerTaskItemRow {
  datasetItemId: number
  externalId?: string
  itemJson?: string
  metadataJson?: string
  itemStatus?: string
  assignmentId?: number
  assignmentStatus?: string
  labelerId?: number
  labelerName?: string
  latestSubmissionId?: number
  versionNo?: number
  submissionStatus?: string
  submittedAt?: string
  aiReviewStatus?: string
  aiDecision?: string
  averageScore?: string
  riskFlags?: string
  suggestion?: string
  reviewTaskStatus?: string
  reviewLevel?: number
  latestReviewAction?: string
  latestReviewAt?: string
  canOpenSubmissionDetail?: boolean
  canReview?: boolean
}

export interface ReviewerTaskItemPage {
  items: ReviewerTaskItemRow[]
  page: number
  pageSize: number
  total: number
}

export interface ReviewerTaskItemPageResponse {
  taskId: number
  taskTitle: string
  taskStatus?: string
  totalItemCount: number
  statusSummary?: ReviewerTaskStatusSummary
  page: ReviewerTaskItemPage
}

export interface ReviewerTaskItemQuery {
  itemStatus?: string
  submissionStatus?: string
  aiDecision?: string
  keyword?: string
  page: number
  size: number
}

export type ReviewerSubmissionScope = 'AVAILABLE' | 'CLAIMED'

export interface ReviewerSubmissionListQuery {
  scope: ReviewerSubmissionScope
  taskId?: string
  submissionStatus?: string
  aiDecision?: string
  aiReviewStatus?: string
  conflictStatus?: string
  reviewLevel?: string
  page?: number
  size?: number
}

export interface ReviewerSubmissionPageResponse {
  items: ReviewerSubmissionListItem[]
  page?: number
  pageSize?: number
  size?: number
  total?: number
}

export interface ReviewerClaimFilters {
  keyword: string
  taskId: string
  submissionStatus: string
  aiDecision: string
  aiReviewStatus: string
  conflictStatus: string
  reviewLevel: string
  assignedReviewerId: string
}

export interface ReviewerAiReviewStatusItem {
  submissionId: number
  taskId: number
  taskTitle: string
  submissionStatus: string
  aiReviewStatus: string
  aiDecision: string
  averageScore?: string
  submittedAt?: string
}

export interface SubmissionVersion {
  submissionId: number
  versionNo: number
  status: string
  answerHash?: string
  isGolden: boolean
  submittedAt?: string
  createdAt?: string
  aiDecision?: string
  aiFlowAction?: string
  latestReviewAction?: string
}

export interface ReviewActionResponse {
  submissionId: number
  action?: string
  newStatus?: string
  submissionStatus?: string
  reviewRecordId?: number
}

export interface BatchReviewResponse {
  totalCount?: number
  successCount: number
  failedCount?: number
  failCount?: number
  results?: Array<{
    submissionId: number
    success: boolean
    reason?: string
  }>
  failures?: Array<{
    submissionId?: number
    reviewId?: string
    reason: string
  }>
}

export type AiReviewQueueStatusFilter = 'all' | 'pending' | 'passed' | 'rejected' | 'manual' | 'failed'

export interface AiReviewResultResponse {
  aiReviewResultId?: number
  submissionId?: number
  taskId?: number
  taskTitle?: string
  submissionStatus?: string
  aiReviewStatus: string
  status?: string
  decision: string
  averageScore?: number | string
  submittedAt?: string
  dimensionScores?: Record<string, number | string>
  dimensions?: Array<{
    name: string
    score: number | string
    comment?: string
  }>
  answerJson?: string
  riskFlags?: string | string[]
  suggestion?: string
  errorCode?: string | null
  agentRunId?: number
  promptMode?: string
  degraded?: boolean
  limitations?: string | null
  promptSnapshot?: string
  rawPrompt?: string
  rawResponse?: string
  retryCount?: number
  createdAt?: string
}

export interface SubmissionItemAiReviewHistory {
  aiReviewResultId?: number
  agentRunId?: number | null
  status?: string
  decision?: string | null
  reviewedAt?: string | null
}

export interface SubmissionItemReviewRoundHistory {
  reviewRecordId: number
  reviewLevel?: number | null
  reviewerId?: number | null
  reviewerName?: string | null
  action?: string | null
  reason?: string | null
  reviewComment?: string | null
  reviewedAt?: string | null
}

export interface SubmissionItemHistory {
  submissionId: number
  assignmentId?: number
  versionNo?: number
  status?: string
  submittedBy?: number
  submittedByName?: string | null
  submittedAt?: string | null
  aiReview?: SubmissionItemAiReviewHistory | null
  reviewRounds?: SubmissionItemReviewRoundHistory[]
}

export interface SubmissionItemHistoryResponse {
  taskId: number
  datasetItemId: number
  histories: SubmissionItemHistory[]
}

export interface ReviewerReviewRecord {
  reviewRecordId: number
  reviewerId: number
  action: string
  reviewLevel: number
  reason?: string
  reviewComment?: string
  createdAt: string
}

export interface ReviewerSubmissionDetail {
  submissionId: number
  taskId: number
  assignmentId?: number
  datasetItemId: number
  labelerId: number
  versionNo?: number
  submissionStatus: string
  aiDecision?: string
  aiReviewStatus?: string
  conflictStatus?: string | null
  reviewLevel?: number
  assignedReviewerId?: number | null
  createdAt?: string
  updatedAt?: string
  answerJson?: string
  itemJson?: string
  templateVersionId?: number
  schemaJson?: string
  aiReviewResult?: {
    aiReviewResultId?: number
    agentRunId?: number
    status?: string
    decision?: string
    averageScore?: string
    riskFlags?: string | string[]
    suggestion?: string | null
    errorCode?: string | null
    promptMode?: string
    degraded?: boolean
    limitations?: string | null
  }
  agentRunSummary?: {
    agentRunId?: number
    agentType?: string
    modelName?: string
    status?: string
    startedAt?: string
    finishedAt?: string
  }
  reviewRecords?: ReviewerReviewRecord[]
  versionHistory?: SubmissionVersion[]
  latestPreAnnotation?: Record<string, unknown> | null
}

export interface AiReviewResultPageResponse {
  items: AiReviewResultResponse[]
  page: number
  pageSize: number
  total: number
}

export interface AiReviewLogQuery {
  page: number
  pageSize: number
  status?: string
  decision?: string
  startTime?: string
  endTime?: string
}

export interface AiReviewResult {
  id: string
  submissionId: string
  status: AiReviewStatus
  decision: AiReviewDecision
  riskLevel: ReviewRiskLevel
  summary: string
  reasons: string[]
  recommendedAction: string
  reviewedAt?: string
  rejectReason?: string
  manualReviewReason?: string
  errorMessage?: string
  rawResponse?: Record<string, unknown>
}

export interface ManualReviewRecord {
  id: string
  submissionId: string
  reviewerId: string
  reviewerName: string
  decision: ManualReviewDecision
  reason?: string
  comment?: string
  reviewedAt: string
}

export interface ManualReviewActionPayload {
  reviewerId: string
  reviewerName: string
  decision: ManualReviewDecision
  reason?: string
  comment?: string
  revisedAnswerJson?: string
}

export interface ReviewOutcomeSyncPayload {
  submissionId: string
  status: 'approved' | 'rejected'
  reviewedAt: string
  reviewSource: 'manual'
  reviewStatus: SubmissionReviewStatus
  rejectReason?: string
  reviewComment?: string
}

export interface BatchManualReviewResult {
  success: ReviewDetail[]
  failed: Array<{
    reviewId: string
    reason: string
  }>
}

export interface ReviewAuditEvent {
  id: string
  submissionId: string
  actorType: 'system' | 'ai' | 'reviewer' | 'labeler'
  actorName: string
  action: string
  description: string
  occurredAt: string
}

export interface ReviewAnswerSnapshot {
  questionId: string
  questionTitle: string
  questionDescription: string
  sourceSnapshot: Record<string, string>
  schemaSnapshot: DynamicFormSchema
  answer: DynamicFormSubmitResult
}

export interface ReviewQueueItem {
  id: string
  submissionId: string
  taskId: string
  taskTitle: string
  labelerId: string
  labelerName: string
  submittedAt: string
  aiDecision: AiReviewDecision
  aiRiskLevel: ReviewRiskLevel
  aiSummary: string
  aiReasons: string[]
  manualReviewStatus: ManualReviewStatus
  submissionReviewStatus: SubmissionReviewStatus
}

export interface ReviewDetail extends ReviewQueueItem {
  rawSubmission?: ReviewerSubmissionDetail
  aiReview: AiReviewResult
  answers: ReviewAnswerSnapshot[]
  manualReviewRecords: ManualReviewRecord[]
  auditTimeline: ReviewAuditEvent[]
}

export interface ReviewQueueQuery {
  keyword: string
  riskLevel: ReviewRiskLevel | 'all'
  manualStatus: ManualReviewStatus | 'all'
}

export interface AiReviewProcessingResult {
  aiReview: AiReviewResult
  submissionReviewStatus: SubmissionReviewStatus
  queueItem: ReviewQueueItem | null
}
