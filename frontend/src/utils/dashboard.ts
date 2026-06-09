import type { DashboardAttentionLevel } from '../types/dashboard'

export const dashboardRangeOptions = [
  { label: '7 天', value: 7 },
  { label: '30 天', value: 30 },
]

export function formatDashboardCount(value?: number | null) {
  return Intl.NumberFormat('zh-CN').format(value ?? 0)
}

export function formatDashboardMoney(value?: number | null) {
  return Intl.NumberFormat('zh-CN', {
    maximumFractionDigits: 2,
    minimumFractionDigits: 0,
  }).format(value ?? 0)
}

export function formatDashboardRate(value?: number | null) {
  const normalized = value ?? 0
  const percentage = normalized > 1 ? normalized : normalized * 100

  return `${percentage.toFixed(1)}%`
}

export function formatDashboardDateTime(value?: string | null) {
  if (!value) {
    return '-'
  }

  return value.replace('T', ' ').slice(0, 16)
}

export function getDashboardLevelColor(level?: DashboardAttentionLevel) {
  return String(level).toUpperCase() === 'WARNING' ? 'orange' : 'blue'
}
