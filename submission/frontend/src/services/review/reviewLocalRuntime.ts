import {
  mockAiReviewResults,
  mockManualReviewRecords,
  mockReviewAuditEvents,
  mockReviewDetails,
  mockReviewQueueItems,
} from '../../mocks'
import type { LabelerTaskSummary, LabelingQuestion, LabelingSubmission } from '../../types/labeling'
import type {
  AiReviewProcessingResult,
  AiReviewResult,
  ManualReviewRecord,
  ReviewAnswerSnapshot,
  ReviewAuditEvent,
  ReviewDetail,
  ReviewQueueItem,
  SubmissionReviewStatus,
} from '../../types/review'
import { getNowLabel } from './reviewUtils'

const aiReviewResults: AiReviewResult[] = mockAiReviewResults.map(cloneAiReviewResult)
const manualReviewRecords: ManualReviewRecord[] = mockManualReviewRecords.map(cloneManualReviewRecord)
const reviewAuditEvents: ReviewAuditEvent[] = mockReviewAuditEvents.map(cloneAuditEvent)
const reviewQueueItems: ReviewQueueItem[] = mockReviewQueueItems.map(cloneQueueItem)
const reviewDetails: ReviewDetail[] = mockReviewDetails.map(cloneReviewDetail)

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

export function getAiReviewResult(submissionId: string): AiReviewResult | null {
  const result = aiReviewResults.find((item) => item.submissionId === submissionId)

  return result ? cloneAiReviewResult(result) : null
}

export function listManualReviewRecords(submissionId: string): ManualReviewRecord[] {
  return manualReviewRecords
    .filter((record) => record.submissionId === submissionId)
    .map(cloneManualReviewRecord)
}

export async function processSubmissionWithAi(
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
}
