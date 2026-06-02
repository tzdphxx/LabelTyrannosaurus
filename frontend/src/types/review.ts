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
  labelerId: number
  submissionStatus: string
  aiDecision: string
  aiReviewStatus: string
  conflictStatus: string
  reviewLevel: number
  assignedReviewerId: number
}

export interface SubmissionVersion {
  submissionId: number
  versionNo: number
  status: string
  answerHash: string
  isGolden: boolean
  submittedAt: string
  aiDecision: string
  aiFlowAction: string
  latestReviewAction: string
}

export interface ReviewActionResponse {
  submissionId: number
  action?: string
  newStatus?: string
  submissionStatus?: string
  reviewRecordId?: number
}

export interface BatchReviewResponse {
  successCount: number
  failedCount: number
  failures?: Array<{
    submissionId?: number
    reviewId?: string
    reason: string
  }>
}

export type AiReviewQueueStatusFilter = 'all' | 'pending' | 'passed' | 'rejected' | 'manual' | 'failed'

export interface AiReviewResultResponse {
  submissionId?: number
  taskId?: number
  aiReviewStatus: string
  decision: string
  averageScore?: number | string
  dimensionScores?: Record<string, number | string>
  riskFlags?: string[]
  suggestion?: string
  agentRunId?: number
  promptSnapshot?: string
  rawResponse?: string
  retryCount?: number
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
  rawSubmission?: ReviewerSubmissionListItem
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
