import type {
  AiReviewLogQuery,
  AiReviewResult,
  AiReviewResultPageResponse,
  AiReviewResultResponse,
  ManualReviewRecord,
  ReviewDetail,
  ReviewQueueItem,
  ReviewQueueQuery,
  ReviewerAiReviewStatusItem,
  ReviewerSubmissionDetail,
  ReviewerSubmissionListItem,
  ReviewerSubmissionPageResponse,
  SubmissionReviewStatus,
} from '../../types/review'

export type ReviewerSubmissionListResponse = ReviewerSubmissionListItem[] | ReviewerSubmissionPageResponse | null | undefined
export type ReviewerAiReviewStatusResponse = ReviewerAiReviewStatusItem[] | null | undefined

const AI_DECISION_LABELS: Record<string, string> = {
  PASS: 'AI 已建议通过',
  REJECT: 'AI 已建议打回',
  MANUAL_REVIEW: '转人工',
}

export function normalizeAiDecision(decision?: string): ReviewQueueItem['aiDecision'] {
  const normalizedDecision = decision?.toUpperCase()

  if (normalizedDecision === 'PASS') {
    return 'pass'
  }

  if (normalizedDecision === 'REJECT') {
    return 'reject'
  }

  return 'manual_review'
}

export function mapSubmissionStatusToReviewStatus(status?: string): SubmissionReviewStatus {
  if (status === 'APPROVED') {
    return 'manual_approved'
  }

  if (status === 'REJECTED') {
    return 'manual_rejected'
  }

  if (status === 'AI_REJECTED') {
    return 'ai_rejected'
  }

  return 'manual_pending'
}

export function mapSubmissionStatusToManualStatus(status?: string): ReviewQueueItem['manualReviewStatus'] {
  if (status === 'APPROVED') {
    return 'approved'
  }

  if (status === 'REJECTED') {
    return 'rejected'
  }

  return 'pending'
}

export function mapReviewerSubmissionToQueueItem(item: ReviewerSubmissionListItem): ReviewQueueItem {
  const aiLabel = AI_DECISION_LABELS[item.aiDecision] ?? item.aiDecision ?? '转人工'

  return {
    id: String(item.submissionId),
    submissionId: String(item.submissionId),
    taskId: String(item.taskId),
    taskTitle: `任务 ${item.taskId}`,
    labelerId: String(item.labelerId),
    labelerName: `标注员 ${item.labelerId}`,
    submittedAt: item.createdAt ?? '-',
    aiDecision: normalizeAiDecision(item.aiDecision),
    aiRiskLevel: item.conflictStatus && item.conflictStatus !== 'NONE' ? 'high' : 'medium',
    aiSummary: aiLabel,
    aiReasons: [
      `AI 结论：${item.aiDecision || '-'}`,
      `AI 审核状态：${item.aiReviewStatus || '-'}`,
      `冲突状态：${item.conflictStatus || '-'}`,
    ],
    manualReviewStatus: mapSubmissionStatusToManualStatus(item.submissionStatus),
    submissionReviewStatus: mapSubmissionStatusToReviewStatus(item.submissionStatus),
  }
}

export function matchesRealQueueQuery(item: ReviewQueueItem, query: ReviewQueueQuery) {
  const keyword = query.keyword.trim().toLowerCase()
  const matchesKeyword =
    keyword.length === 0 ||
    item.taskTitle.toLowerCase().includes(keyword) ||
    item.labelerName.toLowerCase().includes(keyword) ||
    item.aiSummary.toLowerCase().includes(keyword) ||
    item.aiReasons.some((reason) => reason.toLowerCase().includes(keyword))
  const matchesRiskLevel = query.riskLevel === 'all' || item.aiRiskLevel === query.riskLevel
  const matchesManualStatus = query.manualStatus === 'all' || item.manualReviewStatus === query.manualStatus

  return matchesKeyword && matchesRiskLevel && matchesManualStatus
}

export function getReviewerSubmissionItems(response: ReviewerSubmissionListResponse): ReviewerSubmissionListItem[] {
  if (Array.isArray(response)) {
    return response
  }

  return response?.items ?? []
}

export function parseRiskFlags(value?: string | string[] | null): string[] {
  if (!value) {
    return []
  }

  if (Array.isArray(value)) {
    return value
  }

  try {
    const parsed = JSON.parse(value)

    return Array.isArray(parsed) ? parsed.map(String) : []
  } catch {
    return [value]
  }
}

export function normalizeAiStatus(status?: string): AiReviewResult['status'] {
  const normalizedStatus = status?.toUpperCase()

  if (normalizedStatus === 'FAILED') {
    return 'failed'
  }

  if (normalizedStatus === 'PENDING' || normalizedStatus === 'RUNNING') {
    return 'pending'
  }

  return 'completed'
}

export function mapReviewActionToDecision(action?: string): ManualReviewRecord['decision'] {
  return action === 'REJECT' ? 'rejected' : 'approved'
}

export function mapReviewerSubmissionDetailToDetail(item: ReviewerSubmissionDetail): ReviewDetail {
  const aiReviewResult = item.aiReviewResult
  const aiDecision = aiReviewResult?.decision ?? item.aiDecision
  const normalizedAiDecision = normalizeAiDecision(aiDecision)
  const riskFlags = parseRiskFlags(aiReviewResult?.riskFlags)
  const aiLabel = AI_DECISION_LABELS[aiDecision?.toUpperCase() ?? ''] ?? aiDecision ?? 'AI 审核'
  const submittedAt =
    item.createdAt ??
    item.versionHistory?.find((version) => version.submissionId === item.submissionId)?.createdAt ??
    '-'
  const queueItem: ReviewQueueItem = {
    id: String(item.submissionId),
    submissionId: String(item.submissionId),
    taskId: String(item.taskId),
    taskTitle: `任务 ${item.taskId}`,
    labelerId: String(item.labelerId),
    labelerName: `标注员 ${item.labelerId}`,
    submittedAt,
    aiDecision: normalizedAiDecision,
    aiRiskLevel: riskFlags.length > 0 ? 'high' : 'medium',
    aiSummary: aiReviewResult?.suggestion ?? aiLabel,
    aiReasons: [
      `AI 状态：${aiReviewResult?.status ?? item.aiReviewStatus ?? '-'}`,
      `AI 结论：${aiDecision ?? '-'}`,
      `平均分：${aiReviewResult?.averageScore ?? '-'}`,
    ],
    manualReviewStatus: mapSubmissionStatusToManualStatus(item.submissionStatus),
    submissionReviewStatus: mapSubmissionStatusToReviewStatus(item.submissionStatus),
  }

  return {
    ...queueItem,
    rawSubmission: item,
    aiReview: {
      id: String(aiReviewResult?.aiReviewResultId ?? `ai-review-${item.submissionId}`),
      submissionId: String(item.submissionId),
      status: normalizeAiStatus(aiReviewResult?.status ?? item.aiReviewStatus),
      decision: normalizedAiDecision,
      riskLevel: queueItem.aiRiskLevel,
      summary: queueItem.aiSummary,
      reasons: queueItem.aiReasons,
      recommendedAction: normalizedAiDecision === 'reject' ? '建议打回' : normalizedAiDecision === 'pass' ? '建议通过' : '转人工审核',
      rawResponse: {
        promptMode: aiReviewResult?.promptMode,
        degraded: aiReviewResult?.degraded,
        limitations: aiReviewResult?.limitations,
        riskFlags,
      },
    },
    answers: [],
    manualReviewRecords: (item.reviewRecords ?? []).map((record) => ({
      id: String(record.reviewRecordId),
      submissionId: String(item.submissionId),
      reviewerId: String(record.reviewerId),
      reviewerName: `审核员 ${record.reviewerId}`,
      decision: mapReviewActionToDecision(record.action),
      reason: record.reason,
      comment: record.reviewComment,
      reviewedAt: record.createdAt,
    })),
    auditTimeline: (item.reviewRecords ?? []).map((record) => ({
      id: String(record.reviewRecordId),
      submissionId: String(item.submissionId),
      actorType: 'reviewer',
      actorName: `审核员 ${record.reviewerId}`,
      action: record.action,
      description: record.reason ?? record.reviewComment ?? record.action,
      occurredAt: record.createdAt,
    })),
  }
}

export function mapReviewerAiReviewStatusToResult(item: ReviewerAiReviewStatusItem): AiReviewResultResponse {
  return {
    submissionId: item.submissionId,
    taskId: item.taskId,
    taskTitle: item.taskTitle,
    submissionStatus: item.submissionStatus,
    aiReviewStatus: item.aiReviewStatus,
    decision: item.aiDecision,
    averageScore: item.averageScore,
    submittedAt: item.submittedAt,
  }
}

export function normalizeAiReviewResultResponse(item: AiReviewResultResponse): AiReviewResultResponse {
  const dimensionScores = item.dimensionScores ?? Object.fromEntries((item.dimensions ?? []).map((dimension) => [dimension.name, dimension.score]))

  return {
    ...item,
    aiReviewStatus: item.aiReviewStatus ?? item.status ?? 'PENDING',
    status: item.status ?? item.aiReviewStatus,
    dimensionScores,
    riskFlags: Array.isArray(item.riskFlags) ? item.riskFlags : parseRiskFlags(item.riskFlags),
    promptSnapshot: item.promptSnapshot ?? item.rawPrompt,
  }
}

export function matchesAiReviewLogQuery(item: AiReviewResultResponse, query: AiReviewLogQuery) {
  const matchesStatus = !query.status || item.aiReviewStatus === query.status
  const matchesDecision = !query.decision || item.decision === query.decision

  return matchesStatus && matchesDecision
}

export function paginateAiReviewLogs(items: AiReviewResultResponse[], query: AiReviewLogQuery): AiReviewResultPageResponse {
  const page = Math.max(1, query.page || 1)
  const pageSize = Math.max(1, query.pageSize || 20)
  const start = (page - 1) * pageSize

  return {
    items: items.slice(start, start + pageSize),
    page,
    pageSize,
    total: items.length,
  }
}
