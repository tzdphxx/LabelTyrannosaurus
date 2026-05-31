import type { DynamicFormSchema, DynamicFormSubmitResult } from './dynamicForm'

export type LabelerTaskStatus =
  | 'available'
  | 'claimed'
  | 'in_progress'
  | 'submitted'
  | 'approved'
  | 'rejected'
  | 'ended'

export type LabelerTaskQueryStatus = LabelerTaskStatus | 'all'

export interface LabelerTaskSummary {
  id: string
  title: string
  description: string
  instruction: string
  tags: string[]
  status: LabelerTaskStatus
  templateId: string
  templateName: string
  deadline: string
  rewardText: string
  totalQuestions: number
  completedQuestions: number
  claimedAt?: string
  submittedAt?: string
  reviewedAt?: string
  rejectReason?: string
}

export interface LabelerTaskListQuery {
  keyword: string
  tag: string
  status: LabelerTaskQueryStatus
}

export interface LabelingQuestion {
  id: string
  taskId: string
  title: string
  description: string
  source: Record<string, string>
  schema: DynamicFormSchema
  previousValues?: Record<string, unknown>
  status: 'pending' | 'draft' | 'submitted'
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
  answers: DynamicFormSubmitResult[]
}

export interface LabelingReviewSummary {
  taskId: string
  reason: string
  comment: string
  reviewedAt: string
  reviewerName: string
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
