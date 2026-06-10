import { mockTemplates } from './templates.mock'
import type {
  AiReviewResult,
  ManualReviewRecord,
  ReviewAuditEvent,
  ReviewDetail,
  ReviewQueueItem,
} from '../types/review'

const productTemplate = mockTemplates.find((template) => template.id === 'tpl-product-quality')!
const supportTemplate = mockTemplates.find((template) => template.id === 'tpl-support-intent')!

export const mockAiReviewResults: AiReviewResult[] = [
  {
    id: 'ai-review-pass-001',
    submissionId: 'submission-ai-pass-001',
    status: 'completed',
    decision: 'pass',
    riskLevel: 'low',
    summary: '标注结果完整，关键字段与原始数据一致。',
    reasons: ['必填字段完整', '风险标签与客服语义一致', '未命中打回规则'],
    recommendedAction: '自动通过',
    reviewedAt: '2026-05-31 10:36',
    rawResponse: {
      score: 94,
      matchedRules: ['required_fields_complete', 'semantic_consistency_passed'],
    },
  },
  {
    id: 'ai-review-manual-001',
    submissionId: 'submission-support-001',
    status: 'completed',
    decision: 'manual_review',
    riskLevel: 'high',
    summary: '用户情绪和风险标签存在不确定性，需要人工复核。',
    reasons: ['用户表达包含投诉语气', '风险标签只选择 angry，可能遗漏 escalation', '意图与风险组合置信度偏低'],
    recommendedAction: '进入人工复核',
    manualReviewReason: '高风险会话标签存在歧义。',
    reviewedAt: '2026-05-31 10:40',
    rawResponse: {
      score: 61,
      matchedRules: ['risk_tag_uncertain', 'low_confidence_intent'],
    },
  },
  {
    id: 'ai-review-reject-001',
    submissionId: 'submission-ai-reject-001',
    status: 'completed',
    decision: 'reject',
    riskLevel: 'medium',
    summary: '不合格样本缺少原因说明，直接打回标注员修改。',
    reasons: ['quality_result 为 failed', 'quality_reason 为空', '命中必填解释规则'],
    recommendedAction: '自动打回',
    rejectReason: '不合格结论必须补充具体原因。',
    reviewedAt: '2026-05-31 10:44',
    rawResponse: {
      score: 42,
      matchedRules: ['failed_result_requires_reason'],
    },
  },
  {
    id: 'ai-review-failed-001',
    submissionId: 'submission-ai-failed-001',
    status: 'failed',
    decision: 'manual_review',
    riskLevel: 'medium',
    summary: 'AI 审核结果不可用，按兜底策略进入人工复核。',
    reasons: ['AI 审核超时'],
    recommendedAction: '进入人工复核',
    manualReviewReason: 'AI 审核超时，需人工确认。',
    errorMessage: 'AI review timeout',
  },
]

export const mockReviewQueueItems: ReviewQueueItem[] = [
  {
    id: 'review-submission-support-001',
    submissionId: 'submission-support-001',
    taskId: 'label-task-support-submitted',
    taskTitle: '客服会话风险复核',
    labelerId: 'user-labeler',
    labelerName: '标注员李雷',
    submittedAt: '2026-05-30 20:06',
    aiDecision: 'manual_review',
    aiRiskLevel: 'high',
    aiSummary: '用户情绪和风险标签存在不确定性，需要人工复核。',
    aiReasons: ['用户表达包含投诉语气', '风险标签只选择 angry，可能遗漏 escalation', '意图与风险组合置信度偏低'],
    manualReviewStatus: 'pending',
    submissionReviewStatus: 'manual_pending',
  },
  {
    id: 'review-submission-ai-failed-001',
    submissionId: 'submission-ai-failed-001',
    taskId: 'label-task-product-quality',
    taskTitle: '商品图片一致性标注',
    labelerId: 'user-labeler',
    labelerName: '标注员李雷',
    submittedAt: '2026-05-31 09:55',
    aiDecision: 'manual_review',
    aiRiskLevel: 'medium',
    aiSummary: 'AI 审核结果不可用，按兜底策略进入人工复核。',
    aiReasons: ['AI 审核超时'],
    manualReviewStatus: 'pending',
    submissionReviewStatus: 'manual_pending',
  },
]

export const mockManualReviewRecords: ManualReviewRecord[] = [
  {
    id: 'manual-review-product-revision-001',
    submissionId: 'submission-product-revision-001',
    reviewerId: 'user-reviewer',
    reviewerName: '审核员王敏',
    decision: 'rejected',
    reason: '不合格样本缺少原因说明。',
    comment: '请补充具体不一致点，例如图片主体、标题规格或描述承诺。',
    reviewedAt: '2026-05-31 10:12',
  },
]

export const mockReviewAuditEvents: ReviewAuditEvent[] = [
  {
    id: 'audit-submission-support-001-submit',
    submissionId: 'submission-support-001',
    actorType: 'labeler',
    actorName: '标注员李雷',
    action: 'submit',
    description: '提交客服会话风险复核答案。',
    occurredAt: '2026-05-30 20:06',
  },
  {
    id: 'audit-submission-support-001-ai',
    submissionId: 'submission-support-001',
    actorType: 'ai',
    actorName: 'AI 审核',
    action: 'manual_review',
    description: 'AI 判定风险标签存在歧义，进入人工复核。',
    occurredAt: '2026-05-31 10:40',
  },
  {
    id: 'audit-submission-ai-reject-001-ai',
    submissionId: 'submission-ai-reject-001',
    actorType: 'ai',
    actorName: 'AI 审核',
    action: 'reject',
    description: 'AI 判定不合格结论缺少原因，自动打回。',
    occurredAt: '2026-05-31 10:44',
  },
  {
    id: 'audit-submission-ai-failed-001-ai',
    submissionId: 'submission-ai-failed-001',
    actorType: 'system',
    actorName: '审核系统',
    action: 'fallback',
    description: 'AI 审核超时，按兜底策略进入人工复核。',
    occurredAt: '2026-05-31 10:02',
  },
]

export const mockReviewDetails: ReviewDetail[] = [
  {
    id: 'review-submission-ai-pass-001',
    submissionId: 'submission-ai-pass-001',
    taskId: 'label-task-support-intent',
    taskTitle: '客服对话意图识别',
    labelerId: 'user-labeler',
    labelerName: '标注员李雷',
    submittedAt: '2026-05-31 10:30',
    aiDecision: 'pass',
    aiRiskLevel: 'low',
    aiSummary: '标注结果完整，关键字段与原始数据一致。',
    aiReasons: ['必填字段完整', '风险标签与客服语义一致', '未命中打回规则'],
    manualReviewStatus: 'none',
    submissionReviewStatus: 'ai_passed',
    aiReview: mockAiReviewResults[0],
    answers: [
      {
        questionId: 'question-support-001',
        questionTitle: '会话 001：退款咨询',
        questionDescription: '识别用户主要诉求。',
        sourceSnapshot: {
          用户消息: '我昨天买的订单还没发货，现在想取消退款。',
          客服回复: '请提供订单号，我们为您核实处理。',
        },
        schemaSnapshot: supportTemplate.schema,
        answer: {
          templateId: supportTemplate.id,
          schemaVersion: supportTemplate.schema.version,
          values: {
            intent: ['refund'],
            risk_tags: [],
          },
        },
      },
    ],
    manualReviewRecords: [],
    auditTimeline: [
      {
        id: 'audit-submission-ai-pass-001-ai',
        submissionId: 'submission-ai-pass-001',
        actorType: 'ai',
        actorName: 'AI 审核',
        action: 'pass',
        description: 'AI 判定提交质量达标，自动通过。',
        occurredAt: '2026-05-31 10:36',
      },
    ],
  },
  {
    id: 'review-submission-ai-reject-001',
    submissionId: 'submission-ai-reject-001',
    taskId: 'label-task-product-quality',
    taskTitle: '商品图片一致性标注',
    labelerId: 'user-labeler',
    labelerName: '标注员李雷',
    submittedAt: '2026-05-31 10:38',
    aiDecision: 'reject',
    aiRiskLevel: 'medium',
    aiSummary: '不合格样本缺少原因说明，直接打回标注员修改。',
    aiReasons: ['quality_result 为 failed', 'quality_reason 为空', '命中必填解释规则'],
    manualReviewStatus: 'none',
    submissionReviewStatus: 'ai_rejected',
    aiReview: mockAiReviewResults[2],
    answers: [
      {
        questionId: 'question-product-001',
        questionTitle: '样本 001：无线耳机主图',
        questionDescription: '判断商品图片与标题是否一致。',
        sourceSnapshot: {
          商品标题: '主动降噪无线耳机 黑色',
          商品描述: '蓝牙 5.3，支持通话降噪，续航 32 小时。',
          图片摘要: '白色入耳式耳机与充电盒。',
        },
        schemaSnapshot: productTemplate.schema,
        answer: {
          templateId: productTemplate.id,
          schemaVersion: productTemplate.schema.version,
          values: {
            product_title: '主动降噪无线耳机 黑色',
            quality_result: 'failed',
            quality_reason: '',
          },
        },
      },
    ],
    manualReviewRecords: [],
    auditTimeline: mockReviewAuditEvents.filter((event) => event.submissionId === 'submission-ai-reject-001'),
  },
  {
    ...mockReviewQueueItems[0],
    aiReview: mockAiReviewResults[1],
    answers: [
      {
        questionId: 'question-support-001',
        questionTitle: '会话 001：退款咨询',
        questionDescription: '识别用户主要诉求。',
        sourceSnapshot: {
          用户消息: '我昨天买的订单还没发货，现在想取消退款。',
          客服回复: '请提供订单号，我们为您核实处理。',
        },
        schemaSnapshot: supportTemplate.schema,
        answer: {
          templateId: supportTemplate.id,
          schemaVersion: supportTemplate.schema.version,
          values: {
            intent: ['complaint'],
            risk_tags: ['angry'],
          },
        },
      },
    ],
    manualReviewRecords: [],
    auditTimeline: mockReviewAuditEvents.filter((event) => event.submissionId === 'submission-support-001'),
  },
  {
    ...mockReviewQueueItems[1],
    aiReview: mockAiReviewResults[3],
    answers: [
      {
        questionId: 'question-product-001',
        questionTitle: '样本 001：无线耳机主图',
        questionDescription: '判断商品图片与标题是否一致。',
        sourceSnapshot: {
          商品标题: '主动降噪无线耳机 黑色',
          商品描述: '蓝牙 5.3，支持通话降噪，续航 32 小时。',
          图片摘要: '黑色入耳式耳机与充电盒。',
        },
        schemaSnapshot: productTemplate.schema,
        answer: {
          templateId: productTemplate.id,
          schemaVersion: productTemplate.schema.version,
          values: {
            product_title: '主动降噪无线耳机 黑色',
            quality_result: 'passed',
            quality_reason: '',
          },
        },
      },
    ],
    manualReviewRecords: [],
    auditTimeline: mockReviewAuditEvents.filter((event) => event.submissionId === 'submission-ai-failed-001'),
  },
]
