import {
  mockAiReviewResults,
  mockReviewAuditEvents,
  mockManualReviewRecords,
  mockReviewDetails,
  mockReviewQueueItems,
} from '../../mocks'
import { request } from '../http'
import type { LabelerTaskSummary, LabelingQuestion, LabelingSubmission } from '../../types/labeling'
import type {
  AiReviewLogQuery,
  AiReviewResultPageResponse,
  AiReviewResultResponse,
  AiReviewResult,
  AiReviewProcessingResult,
  BatchManualReviewResult,
  BatchReviewResponse,
  ManualReviewActionPayload,
  ManualReviewRecord,
  ReviewAnswerSnapshot,
  ReviewAuditEvent,
  ReviewActionResponse,
  ReviewDetail,
  ReviewQueueItem,
  ReviewQueueQuery,
  ReviewerSubmissionListItem,
  ReviewOutcomeSyncPayload,
  SubmissionVersion,
  SubmissionReviewStatus,
} from '../../types/review'

type ReviewOutcomeSyncHandler = (payload: ReviewOutcomeSyncPayload) => void

const aiReviewResults: AiReviewResult[] = mockAiReviewResults.map(cloneAiReviewResult)
const manualReviewRecords: ManualReviewRecord[] = mockManualReviewRecords.map(cloneManualReviewRecord)
const reviewAuditEvents: ReviewAuditEvent[] = mockReviewAuditEvents.map(cloneAuditEvent)
const reviewQueueItems: ReviewQueueItem[] = mockReviewQueueItems.map(cloneQueueItem)
const reviewDetails: ReviewDetail[] = mockReviewDetails.map(cloneReviewDetail)
let reviewOutcomeSyncHandler: ReviewOutcomeSyncHandler | null = null

const AI_DECISION_LABELS: Record<string, string> = {
  PASS: 'AI已建议通过',
  REJECT: 'AI已建议打回',
  MANUAL_REVIEW: '转人工',
}

function normalizeAiDecision(decision?: string): ReviewQueueItem['aiDecision'] {
  if (decision === 'PASS') {
    return 'pass'
  }

  if (decision === 'REJECT') {
    return 'reject'
  }

  return 'manual_review'
}

function mapSubmissionStatusToReviewStatus(status?: string): SubmissionReviewStatus {
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

function mapSubmissionStatusToManualStatus(status?: string): ReviewQueueItem['manualReviewStatus'] {
  if (status === 'APPROVED') {
    return 'approved'
  }

  if (status === 'REJECTED') {
    return 'rejected'
  }

  return 'pending'
}

function mapReviewerSubmissionToQueueItem(item: ReviewerSubmissionListItem): ReviewQueueItem {
  const aiLabel = AI_DECISION_LABELS[item.aiDecision] ?? item.aiDecision ?? '转人工'

  return {
    id: String(item.submissionId),
    submissionId: String(item.submissionId),
    taskId: String(item.taskId),
    taskTitle: `任务 ${item.taskId}`,
    labelerId: String(item.labelerId),
    labelerName: `标注员 ${item.labelerId}`,
    submittedAt: '-',
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

function mapReviewerSubmissionToDetail(item: ReviewerSubmissionListItem): ReviewDetail {
  const queueItem = mapReviewerSubmissionToQueueItem(item)
  const aiLabel = AI_DECISION_LABELS[item.aiDecision] ?? item.aiDecision ?? '转人工'

  return {
    ...queueItem,
    rawSubmission: item,
    aiReview: {
      id: `ai-review-${item.submissionId}`,
      submissionId: String(item.submissionId),
      status: item.aiReviewStatus === 'FAILED' ? 'failed' : 'completed',
      decision: queueItem.aiDecision,
      riskLevel: queueItem.aiRiskLevel,
      summary: aiLabel,
      reasons: queueItem.aiReasons,
      recommendedAction: item.aiDecision === 'REJECT' ? '建议打回' : item.aiDecision === 'PASS' ? '建议通过' : '转人工审核',
    },
    answers: [],
    manualReviewRecords: [],
    auditTimeline: [],
  }
}

function matchesRealQueueQuery(item: ReviewQueueItem, query: ReviewQueueQuery) {
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

function getNowLabel() {
  return new Intl.DateTimeFormat('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    hour12: false,
  })
    .format(new Date())
    .replace(/\//g, '-')
}

function cloneAiReviewResult(result: AiReviewResult): AiReviewResult {
  return {
    ...result,
    reasons: [...result.reasons],
    rawResponse: result.rawResponse ? { ...result.rawResponse } : undefined,
  }
}

function cloneAnswerSnapshot(answer: ReviewAnswerSnapshot): ReviewAnswerSnapshot {
  return {
    ...answer,
    sourceSnapshot: { ...answer.sourceSnapshot },
    schemaSnapshot: {
      ...answer.schemaSnapshot,
      nodes: answer.schemaSnapshot.nodes.map((node) => ({ ...node })),
    },
    answer: {
      ...answer.answer,
      values: { ...answer.answer.values },
    },
  }
}

function cloneAuditEvent(event: ReviewAuditEvent): ReviewAuditEvent {
  return { ...event }
}

function cloneManualReviewRecord(record: ManualReviewRecord): ManualReviewRecord {
  return { ...record }
}

function cloneQueueItem(item: ReviewQueueItem): ReviewQueueItem {
  return {
    ...item,
    aiReasons: [...item.aiReasons],
  }
}

function cloneReviewDetail(detail: ReviewDetail): ReviewDetail {
  return {
    ...cloneQueueItem(detail),
    aiReview: cloneAiReviewResult(detail.aiReview),
    answers: detail.answers.map(cloneAnswerSnapshot),
    manualReviewRecords: detail.manualReviewRecords.map(cloneManualReviewRecord),
    auditTimeline: detail.auditTimeline.map(cloneAuditEvent),
  }
}

function getSubmissionReviewStatus(decision: AiReviewResult['decision']): SubmissionReviewStatus {
  if (decision === 'pass') {
    return 'ai_passed'
  }

  if (decision === 'reject') {
    return 'ai_rejected'
  }

  return 'manual_pending'
}

function decideAiReview(submission: LabelingSubmission): Pick<
  AiReviewResult,
  'decision' | 'riskLevel' | 'summary' | 'reasons' | 'recommendedAction' | 'rejectReason' | 'manualReviewReason'
> {
  const hasMissingFailedReason = submission.answers.some(
    (answer) => answer.values.quality_result === 'failed' && !String(answer.values.quality_reason ?? '').trim(),
  )

  if (hasMissingFailedReason) {
    return {
      decision: 'reject',
      riskLevel: 'medium',
      summary: '不合格样本缺少原因说明，AI 直接打回。',
      reasons: ['quality_result 为 failed', 'quality_reason 为空', '命中不合格原因必填规则'],
      recommendedAction: '自动打回',
      rejectReason: '不合格结论必须补充具体原因。',
    }
  }

  const hasRiskUncertainty = submission.answers.some((answer) => {
    const riskTags = answer.values.risk_tags

    return Array.isArray(riskTags) && riskTags.includes('angry')
  })

  if (hasRiskUncertainty) {
    return {
      decision: 'manual_review',
      riskLevel: 'high',
      summary: '用户情绪和风险标签存在不确定性，需要人工复核。',
      reasons: ['用户表达包含投诉语气', '风险标签可能遗漏升级风险', '意图与风险组合置信度偏低'],
      recommendedAction: '进入人工复核',
      manualReviewReason: '高风险会话标签存在歧义。',
    }
  }

  return {
    decision: 'pass',
    riskLevel: 'low',
    summary: '标注结果完整，关键字段与原始数据一致。',
    reasons: ['必填字段完整', '答案结构符合 schema', '未命中打回或复核规则'],
    recommendedAction: '自动通过',
  }
}

function buildAnswerSnapshots(submission: LabelingSubmission, questions: LabelingQuestion[]): ReviewAnswerSnapshot[] {
  return submission.answers.map((answer, index) => {
    const question = questions[index] ?? questions[0]

    return {
      questionId: question?.id ?? `question-${index + 1}`,
      questionTitle: question?.title ?? `题目 ${index + 1}`,
      questionDescription: question?.description ?? '',
      sourceSnapshot: { ...(question?.source ?? {}) },
      schemaSnapshot: question?.schema ?? {
        id: answer.templateId,
        version: answer.schemaVersion,
        title: '提交快照',
        nodes: [],
      },
      answer: {
        ...answer,
        values: { ...answer.values },
      },
    }
  })
}

function upsertDetail(detail: ReviewDetail) {
  const index = reviewDetails.findIndex((item) => item.id === detail.id || item.submissionId === detail.submissionId)

  if (index >= 0) {
    reviewDetails[index] = detail
  } else {
    reviewDetails.unshift(detail)
  }
}

export const reviewService = {
  registerReviewOutcomeSync(handler: ReviewOutcomeSyncHandler) {
    reviewOutcomeSyncHandler = handler
  },

  async listManualReviewQueue(query: ReviewQueueQuery): Promise<ReviewQueueItem[]> {
    const items = await request.get<ReviewerSubmissionListItem[]>('/v1/reviewer/submissions', {
      params: {
        submissionStatus: 'PENDING_FINAL',
        page: 1,
        size: 20,
      },
    })

    return items.map(mapReviewerSubmissionToQueueItem).filter((item) => matchesRealQueueQuery(item, query))
  },

  async listReviewHistory(): Promise<ReviewQueueItem[]> {
    const items = await request.get<ReviewerSubmissionListItem[]>('/v1/reviewer/submissions', {
      params: {
        page: 1,
        size: 100,
      },
    })

    return items.map(mapReviewerSubmissionToQueueItem)
  },

  async getReviewDetail(reviewId: string): Promise<ReviewDetail | null> {
    const detail = await request.get<ReviewerSubmissionListItem>(`/v1/reviewer/submissions/${reviewId}`)

    return detail ? mapReviewerSubmissionToDetail(detail) : null
  },

  async getAiReviewResult(submissionId: string): Promise<AiReviewResult | null> {
    const result = aiReviewResults.find((item) => item.submissionId === submissionId)

    return result ? cloneAiReviewResult(result) : null
  },

  async listAllAiReviewLogs(query: AiReviewLogQuery): Promise<AiReviewResultPageResponse> {
    return request.get<AiReviewResultPageResponse>('/v1/tasks/ai-review-logs', {
      params: query,
    })
  },

  async listTaskAiReviewLogs(taskId: string, query: AiReviewLogQuery): Promise<AiReviewResultPageResponse> {
    return request.get<AiReviewResultPageResponse>(`/v1/tasks/${taskId}/ai-review-logs`, {
      params: query,
    })
  },

  async getSubmissionAiReview(submissionId: string): Promise<AiReviewResultResponse> {
    return request.get<AiReviewResultResponse>(`/v1/submissions/${submissionId}/ai-review`)
  },

  async retrySubmissionAiReview(submissionId: string): Promise<AiReviewResultResponse> {
    return request.post<AiReviewResultResponse>(`/v1/submissions/${submissionId}/ai-review/retry`)
  },

  async listManualReviewRecords(submissionId: string): Promise<ManualReviewRecord[]> {
    return manualReviewRecords
      .filter((record) => record.submissionId === submissionId)
      .map(cloneManualReviewRecord)
  },

  async listSubmissionVersions(submissionId: string): Promise<SubmissionVersion[]> {
    return request.get<SubmissionVersion[]>(`/v1/submissions/${submissionId}/versions`)
  },

  async submitManualReviewAction(reviewId: string, payload: ManualReviewActionPayload): Promise<ReviewDetail | null> {
    const response =
      payload.decision === 'approved'
        ? await request.post<ReviewActionResponse, { reviewComment?: string; reviewLevel: number }>(
            `/v1/reviewer/submissions/${reviewId}/approve`,
            {
              reviewComment: payload.comment,
              reviewLevel: 1,
            },
          )
        : await request.post<ReviewActionResponse, { reason: string; reviewLevel: number }>(
            `/v1/reviewer/submissions/${reviewId}/reject`,
            {
              reason: payload.reason ?? payload.comment ?? '',
              reviewLevel: 1,
            },
          )

    const current = await this.getReviewDetail(String(response.submissionId))

    reviewOutcomeSyncHandler?.({
      submissionId: String(response.submissionId),
      status: payload.decision === 'approved' ? 'approved' : 'rejected',
      reviewedAt: getNowLabel(),
      reviewSource: 'manual',
      reviewStatus: payload.decision === 'approved' ? 'manual_approved' : 'manual_rejected',
      rejectReason: payload.decision === 'rejected' ? payload.reason ?? payload.comment : undefined,
      reviewComment: payload.comment ?? payload.reason,
    })

    return current
  },

  async submitBatchManualReviewAction(reviewIds: string[], payload: ManualReviewActionPayload): Promise<BatchManualReviewResult> {
    const response =
      payload.decision === 'approved'
        ? await request.post<BatchReviewResponse, { submissionIds: number[] }>(
            '/v1/reviewer/submissions/batch/approve',
            {
              submissionIds: reviewIds.map(Number),
            },
          )
        : await request.post<BatchReviewResponse, { submissionIds: number[]; reason: string }>(
            '/v1/reviewer/submissions/batch/reject',
            {
              submissionIds: reviewIds.map(Number),
              reason: payload.reason ?? payload.comment ?? '',
            },
          )
    const failedIds = new Set((response.failures ?? []).map((item) => String(item.submissionId ?? item.reviewId)))
    const successfulIds = reviewIds.filter((reviewId) => !failedIds.has(reviewId)).slice(0, response.successCount)
    const success = (
      await Promise.all(successfulIds.map((reviewId) => this.getReviewDetail(reviewId).catch(() => null)))
    ).filter((detail): detail is ReviewDetail => Boolean(detail))

    return {
      success,
      failed: (response.failures ?? []).map((failure) => ({
        reviewId: String(failure.submissionId ?? failure.reviewId ?? ''),
        reason: failure.reason,
      })),
    }
  },

  async processSubmissionWithAi(
    submission: LabelingSubmission,
    context: {
      task: LabelerTaskSummary
      questions: LabelingQuestion[]
    },
  ): Promise<AiReviewProcessingResult> {
    const now = getNowLabel()
    const decision = decideAiReview(submission)
    const aiReview: AiReviewResult = {
      id: `ai-review-${submission.id}`,
      submissionId: submission.id,
      status: 'completed',
      reviewedAt: now,
      rawResponse: {
        source: 'mock',
      },
      ...decision,
    }
    const submissionReviewStatus = getSubmissionReviewStatus(aiReview.decision)
    const auditEvent: ReviewAuditEvent = {
      id: `audit-${submission.id}-ai`,
      submissionId: submission.id,
      actorType: 'ai',
      actorName: 'AI 审核',
      action: aiReview.decision,
      description: aiReview.summary,
      occurredAt: now,
    }
    const queueItem: ReviewQueueItem | null =
      aiReview.decision === 'manual_review'
        ? {
            id: `review-${submission.id}`,
            submissionId: submission.id,
            taskId: submission.taskId,
            taskTitle: submission.taskTitle,
            labelerId: submission.userId,
            labelerName: '标注员李雷',
            submittedAt: submission.submittedAt,
            aiDecision: aiReview.decision,
            aiRiskLevel: aiReview.riskLevel,
            aiSummary: aiReview.summary,
            aiReasons: [...aiReview.reasons],
            manualReviewStatus: 'pending',
            submissionReviewStatus,
          }
        : null
    const detailBase: Omit<ReviewDetail, keyof ReviewQueueItem> = {
      aiReview,
      answers: buildAnswerSnapshots(submission, context.questions),
      manualReviewRecords: [],
      auditTimeline: [auditEvent],
    }

    aiReviewResults.unshift(aiReview)
    reviewAuditEvents.unshift(auditEvent)

    if (queueItem) {
      reviewQueueItems.unshift(queueItem)
      upsertDetail({
        ...queueItem,
        ...detailBase,
      })
    } else {
      upsertDetail({
        id: `review-${submission.id}`,
        submissionId: submission.id,
        taskId: submission.taskId,
        taskTitle: context.task.title,
        labelerId: submission.userId,
        labelerName: '标注员李雷',
        submittedAt: submission.submittedAt,
        aiDecision: aiReview.decision,
        aiRiskLevel: aiReview.riskLevel,
        aiSummary: aiReview.summary,
        aiReasons: [...aiReview.reasons],
        manualReviewStatus: 'none',
        submissionReviewStatus,
        ...detailBase,
      })
    }

    return {
      aiReview: cloneAiReviewResult(aiReview),
      submissionReviewStatus,
      queueItem: queueItem ? cloneQueueItem(queueItem) : null,
    }
  },
}
