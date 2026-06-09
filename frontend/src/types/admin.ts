import type { DashboardAttentionLevel } from './dashboard'

export type AdminDashboardRange = '7d' | '14d' | '30d'

export interface AdminDashboardKpis {
  activeTaskCount: number
  claimedCount: number
  submittedCount: number
  pendingReviewCount: number
  approvalRate: number
  rejectionRate: number
  rewardAmount: number
}

export interface AdminUserSummary {
  totalUserCount: number
  roleCounts: Record<string, number>
  disabledUserCount: number
  newUserCount: number
}

export interface AdminDashboardTrendPoint {
  date: string
  submittedCount: number
  approvedCount: number
  rejectedCount: number
  rewardAmount: number
}

export interface AdminTopLabeler {
  labelerId: number
  displayName: string
  submittedCount: number
  approvedCount: number
  rewardAmount: number
}

export interface AdminTopTask {
  taskId: number
  title: string
  submittedCount: number
  approvedCount: number
  rejectedCount: number
}

export interface AdminDashboardAlert {
  type: string
  level: DashboardAttentionLevel
  title: string
  description: string
  targetPath: string
}

export interface AdminDashboardOverview {
  range: AdminDashboardRange | string
  kpis: AdminDashboardKpis
  userSummary: AdminUserSummary
  trend: AdminDashboardTrendPoint[]
  taskStatusDistribution: Record<string, number>
  topLabelers: AdminTopLabeler[]
  topTasks: AdminTopTask[]
  alerts: AdminDashboardAlert[]
  generatedAt: string
}

export interface AdminCreateReviewerRequest {
  username: string
  email: string
  password: string
}

export interface AdminUserResponse {
  userId: number
  username: string
  email: string
  userType: string
  enabled: boolean
  loginEnabled: boolean
  tokenVersion: number
  role: 'REVIEWER' | string
}
