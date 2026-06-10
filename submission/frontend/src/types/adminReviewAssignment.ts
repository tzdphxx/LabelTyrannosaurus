export interface PageResponse<T> {
  items: T[]
  page: number
  pageSize: number
  total: number
}

export interface AssignableTaskQuery {
  taskId?: number | null
  keyword?: string
  reviewLevel?: number | null
  includeClaimed?: boolean
  page: number
  size: number
}

export interface AssignableTask {
  taskId: number
  title: string
  status: 'DRAFT' | 'PUBLISHED' | 'PAUSED' | 'ENDED'
  deadlineAt: string | null
  reviewLevel: number
  pendingCount: number
  claimed: boolean
  claimedReviewerId: number | null
  claimedReviewerName: string | null
  available: boolean
}

export interface AssignableReviewerQuery {
  keyword?: string
  enabledOnly?: boolean
  page: number
  size: number
}

export interface AssignableReviewer {
  reviewerId: number
  username: string
  email: string
  enabled: boolean
  loginEnabled: boolean
  pendingCount: number
  todayReviewedCount: number
  totalApprovedCount: number
  totalRejectedCount: number
  approvalRate: number
}

export interface ReviewerProgressQuery {
  keyword?: string
  enabledOnly?: boolean
}

export interface ClaimedReviewTask {
  taskId: number
  title: string
  reviewLevel: number
  pendingCount: number
  claimedAt: string
}

export interface ReviewerProgress {
  reviewerId: number
  username: string
  email: string
  enabled: boolean
  loginEnabled: boolean
  pendingCount: number
  todayReviewedCount: number
  totalReviewedCount: number
  approvalRate: number
  claimedTaskCount: number
  claimedTasks: ClaimedReviewTask[]
}
