export type DashboardAttentionLevel = 'INFO' | 'WARNING' | string

export interface OwnerKpis {
  totalTaskCount: number
  runningTaskCount: number
  claimedItemCount: number
  submittedItemCount: number
  pendingReviewCount: number
  approvalRate: number
  rewardCost: number
}

export interface OwnerDeliveryTrendPoint {
  date: string
  claimedCount: number
  submittedCount: number
  approvedCount: number
}

export interface OwnerQualitySummary {
  approvedCount: number
  rejectedCount: number
  rejectionRate: number
}

export interface OwnerRewardSummary {
  totalRewardCost: number
  visibleTaskCount: number
}

export interface OwnerAttentionTask {
  taskId: number
  title: string
  type: string
  level: DashboardAttentionLevel
  description: string
  targetPath: string
}

export interface OwnerRecentTask {
  taskId: number
  title: string
  status: string
  progressRate: number
  pendingReviewCount: number
  updatedAt: string
}

export interface OwnerDashboardOverview {
  trendDays: number
  kpis: OwnerKpis
  taskStatusDistribution: Record<string, number>
  deliveryTrend: OwnerDeliveryTrendPoint[]
  qualitySummary: OwnerQualitySummary
  rewardSummary: OwnerRewardSummary
  attentionTasks: OwnerAttentionTask[]
  recentTasks: OwnerRecentTask[]
  generatedAt: string
}

export interface LabelerKpis {
  claimedCount: number
  submittedCount: number
  approvedCount: number
  rejectedCount: number
  approvalRate: number
  periodReward: number
  totalReward: number
  reworkCount: number
}

export interface LabelerContributionTrendPoint {
  date: string
  submittedCount: number
  approvedCount: number
  reward: number
}

export interface LabelerTaskContribution {
  taskId: number
  taskTitle: string
  submittedCount: number
  approvedCount: number
  totalReward: number
  targetPath: string
}

export interface LabelerTodoSummary {
  claimedNotSubmittedCount: number
  rejectedNeedFixCount: number
  continuableTaskCount: number
}

export interface LabelerDashboardAlert {
  type: string
  level: DashboardAttentionLevel
  title: string
  description: string
  targetPath: string
}

export interface LabelerDashboardOverview {
  range: string
  kpis: LabelerKpis
  contributionTrend: LabelerContributionTrendPoint[]
  taskContributions: LabelerTaskContribution[]
  todoSummary: LabelerTodoSummary
  alerts: LabelerDashboardAlert[]
  generatedAt: string
}

export interface ReviewerQueueSummary {
  pendingCount: number
  overduePendingCount: number
  manualRequiredCount: number
  conflictRequiredCount: number
}

export interface ReviewerKpis {
  todayReviewedCount: number
  totalApproved: number
  totalRejected: number
  approvalRate: number
  aiAttentionCount: number
}

export interface ReviewerTrendPoint {
  date: string
  reviewedCount: number
  approvedCount: number
  rejectedCount: number
}

export interface ReviewerAiReviewSummary {
  aiReviewResultId: number
  agentRunId: number
  status: string
  decision: string
  averageScore: string
  riskFlags: string
  suggestion: string
  errorCode: string
  promptMode: string
  degraded: boolean
  limitations: string
}

export interface ReviewerAttentionItem {
  reviewId: number
  submissionId: number
  taskId: number
  taskTitle: string
  type: string
  level: DashboardAttentionLevel
  description: string
  targetPath: string
}

export interface ReviewerRecentReviewed {
  reviewId: number
  submissionId: number
  taskTitle: string
  labelerName: string
  result: string
  reviewedAt: string
}

export interface ReviewerDashboardOverview {
  range: string
  queueSummary: ReviewerQueueSummary
  kpis: ReviewerKpis
  reviewTrend: ReviewerTrendPoint[]
  aiReviewSummary: ReviewerAiReviewSummary | null
  attentionItems: ReviewerAttentionItem[]
  recentReviewed: ReviewerRecentReviewed[]
  generatedAt: string
}
