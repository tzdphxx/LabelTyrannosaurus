import type { DynamicFormSchema, DynamicFormSubmitResult } from './dynamicForm'
import type { AiReviewDecision, SubmissionReviewStatus } from './review'

export type LabelerTaskStatus =
  | 'available'
  | 'claimed'
  | 'in_progress'
  | 'paused'
  | 'submitted'
  | 'approved'
  | 'rejected'
  | 'ended'

export type LabelerTaskQueryStatus = LabelerTaskStatus | 'all'

export type LabelerAssignmentStatus =
  | 'CLAIMED'
  | 'DRAFTING'
  | 'PAUSED'
  | 'ENDED'
  | 'SUBMITTED'
  | 'AI_RETURNED'
  | 'RETURNED'
  | 'APPROVED'
  | 'CANCELLED'

export type LabelerAssignmentQueryStatus = LabelerAssignmentStatus | 'all'

export interface LabelerTaskSummary {
  id: string
  title: string
  description: string
  instruction: string
  tags: string[]
  status: LabelerTaskStatus
  templateId: string
  templateVersionId?: number | string | null
  assignmentId?: number | string | null
  templateName: string
  deadline: string
  rewardText: string
  totalQuestions: number
  completedQuestions: number
  strategy?: string
  availableCount?: number
  currentUserClaimedCount?: number
  maxClaimsPerLabeler?: number
  claimedAt?: string
  submittedAt?: string
  reviewedAt?: string
  rejectReason?: string
  reviewSource?: 'ai' | 'manual'
  reviewStatus?: SubmissionReviewStatus
  aiDecision?: AiReviewDecision
  aiReviewSummary?: string
}

export interface LabelerTaskListQuery {
  keyword: string
  tag: string
  status: LabelerTaskQueryStatus
}

export interface LabelerClaimOptions {
  quantity: number
}

export interface LabelerAssignmentListQuery {
  taskId?: string
  status?: LabelerAssignmentQueryStatus
  page?: number
  size?: number
}

export interface LabelerAssignmentSummary {
  id: string
  assignmentId: string
  taskId: string
  taskTitle: string
  datasetItemId: string
  status: LabelerAssignmentStatus
  draftVersion: number
  claimedAt: string
  returnedAt?: string
  updatedAt: string
  myClaimedCount?: number
  mySubmittedCount?: number
  myApprovedCount?: number
}

export interface LabelingQuestion {
  id: string
  taskId: string
  assignmentId?: number | string | null
  submissionId?: number | string | null
  templateVersionId?: number | string | null
  datasetItemId?: number | string | null
  title: string
  description: string
  source: Record<string, string>
  schema: DynamicFormSchema
  previousValues?: Record<string, unknown>
  returnedReason?: string | null
  status: 'pending' | 'in_progress' | 'rejected' | 'submitted' | 'draft'
}

export interface LabelingDraft {
  id: string
  taskId: string
  questionId: string
  userId: string
  values: Record<string, unknown>
  updatedAt: string
}

export interface LabelingSubmission {
  id: string
  taskId: string
  taskTitle: string
  userId: string
  status: Extract<LabelerTaskStatus, 'submitted' | 'approved' | 'rejected'>
  submittedAt: string
  reviewedAt?: string
  rejectReason?: string
  reviewComment?: string
  reviewSource?: 'ai' | 'manual'
  reviewStatus?: SubmissionReviewStatus
  aiDecision?: AiReviewDecision
  aiReviewSummary?: string
  answers: DynamicFormSubmitResult[]
}

export interface LabelingReviewSummary {
  taskId: string
  reason: string
  comment: string
  reviewedAt: string
  reviewerName: string
}

export interface SubmissionAiReviewHistory {
  aiReviewResultId?: number
  agentRunId?: number | null
  status?: string
  decision?: string | null
  reviewedAt?: string | null
}

export interface SubmissionReviewRoundHistory {
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
  assignmentId: number
  versionNo: number
  status: string
  submittedBy: number
  submittedByName?: string | null
  submittedAt?: string | null
  aiReview?: SubmissionAiReviewHistory | null
  reviewRounds: SubmissionReviewRoundHistory[]
}

export interface SubmissionItemHistoryResponse {
  taskId: number
  datasetItemId: number
  histories: SubmissionItemHistory[]
}

export interface LabelingSubmitValidationError {
  questionId: string
  questionTitle: string
  fieldKey?: string
  fieldTitle?: string
  message: string
}

export interface LabelingSubmitValidationResult {
  valid: boolean
  errors: LabelingSubmitValidationError[]
}

export interface LabelingSubmitResult {
  submission: LabelingSubmission | null
  validation: LabelingSubmitValidationResult
}

export interface LabelerSubmissionStats {
  submitted: number
  approved: number
  rejected: number
  needsRevision: number
  inProgress: number
}

export interface LabelerAssignmentStats {
  total: number
  claimed: number
  drafting: number
  submitted: number
  returned: number
  approved: number
  cancelled: number
}
