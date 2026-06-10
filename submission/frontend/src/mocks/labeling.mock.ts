import { mockTemplates } from './templates.mock'
import type {
  LabelerSubmissionStats,
  LabelerTaskSummary,
  LabelingDraft,
  LabelingQuestion,
  LabelingReviewSummary,
  LabelingSubmission,
} from '../types/labeling'

const productTemplate = mockTemplates.find((template) => template.id === 'tpl-product-quality')!
const supportTemplate = mockTemplates.find((template) => template.id === 'tpl-support-intent')!

export const mockLabelerTasks: LabelerTaskSummary[] = [
  {
    id: 'label-task-product-quality',
    title: '商品图片一致性标注',
    description: '判断商品主图、标题和描述是否匹配，输出质检结论。',
    instruction: '优先判断图片主体与标题是否一致，再补充不合格原因。',
    tags: ['商品', '图片', '质检'],
    status: 'available',
    templateId: productTemplate.id,
    templateName: productTemplate.name,
    deadline: '2026-06-10',
    rewardText: '0.18 元 / 条',
    totalQuestions: 42,
    completedQuestions: 0,
  },
  {
    id: 'label-task-support-intent',
    title: '客服对话意图识别',
    description: '阅读客服对话，标注用户意图、情绪和风险标签。',
    instruction: '每条对话只选择一个主意图，风险标签可多选。',
    tags: ['客服', '文本', '分类'],
    status: 'claimed',
    templateId: supportTemplate.id,
    templateName: supportTemplate.name,
    deadline: '2026-06-18',
    rewardText: '0.12 元 / 条',
    totalQuestions: 36,
    completedQuestions: 8,
    claimedAt: '2026-05-31 09:20',
  },
  {
    id: 'label-task-product-revision',
    title: '商品质检打回修改',
    description: '根据审核意见修正上一轮商品质检答案。',
    instruction: '重点补充不合格原因，避免只给出结论。',
    tags: ['商品', '返修'],
    status: 'rejected',
    templateId: productTemplate.id,
    templateName: productTemplate.name,
    deadline: '2026-06-08',
    rewardText: '0.18 元 / 条',
    totalQuestions: 18,
    completedQuestions: 18,
    claimedAt: '2026-05-29 15:10',
    submittedAt: '2026-05-30 18:42',
    reviewedAt: '2026-05-31 10:12',
    rejectReason: '部分不合格样本缺少原因说明。',
  },
  {
    id: 'label-task-support-submitted',
    title: '客服会话风险复核',
    description: '对高风险客服会话补充风险标签。',
    instruction: '只处理存在升级风险的会话。',
    tags: ['客服', '风险'],
    status: 'submitted',
    templateId: supportTemplate.id,
    templateName: supportTemplate.name,
    deadline: '2026-06-12',
    rewardText: '0.14 元 / 条',
    totalQuestions: 24,
    completedQuestions: 24,
    claimedAt: '2026-05-28 14:15',
    submittedAt: '2026-05-30 20:06',
  },
]

export const mockLabelingQuestions: LabelingQuestion[] = [
  {
    id: 'question-product-001',
    taskId: 'label-task-product-quality',
    title: '样本 001：无线耳机主图',
    description: '判断商品图片与标题是否一致。',
    source: {
      商品标题: '主动降噪无线耳机 黑色',
      商品描述: '蓝牙 5.3，支持通话降噪，续航 32 小时。',
      图片摘要: '黑色入耳式耳机与充电盒。',
    },
    schema: productTemplate.schema,
    status: 'pending',
  },
  {
    id: 'question-product-002',
    taskId: 'label-task-product-quality',
    title: '样本 002：户外保温杯',
    description: '检查标题、描述和图片主体。',
    source: {
      商品标题: '大容量户外保温杯 1200ml',
      商品描述: '带手柄，适合露营和通勤。',
      图片摘要: '银色保温杯，杯盖带提手。',
    },
    schema: productTemplate.schema,
    status: 'pending',
  },
  {
    id: 'question-support-001',
    taskId: 'label-task-support-intent',
    title: '会话 001：退款咨询',
    description: '识别用户主要诉求。',
    source: {
      用户消息: '我昨天买的订单还没发货，现在想取消退款。',
      客服回复: '请提供订单号，我们为您核实处理。',
    },
    schema: supportTemplate.schema,
    status: 'draft',
  },
  {
    id: 'question-revision-001',
    taskId: 'label-task-product-revision',
    title: '返修样本 001：耳机颜色不一致',
    description: '根据审核意见补充不合格原因。',
    source: {
      商品标题: '主动降噪无线耳机 黑色',
      商品描述: '蓝牙 5.3，支持通话降噪，续航 32 小时。',
      图片摘要: '白色入耳式耳机与充电盒。',
    },
    schema: productTemplate.schema,
    previousValues: {
      product_title: '主动降噪无线耳机 黑色',
      quality_result: 'failed',
      quality_reason: '',
    },
    status: 'rejected',
  },
  {
    id: 'question-revision-002',
    taskId: 'label-task-product-revision',
    title: '返修样本 002：规格描述缺失',
    description: '补充标题与描述不一致的具体依据。',
    source: {
      商品标题: '不锈钢保温杯 1200ml',
      商品描述: '便携保温杯，适合通勤。',
      图片摘要: '杯身标签显示 800ml。',
    },
    schema: productTemplate.schema,
    previousValues: {
      product_title: '不锈钢保温杯 1200ml',
      quality_result: 'failed',
      quality_reason: '',
    },
    status: 'rejected',
  },
]

export const mockLabelingDrafts: LabelingDraft[] = [
  {
    id: 'draft-support-001',
    taskId: 'label-task-support-intent',
    questionId: 'question-support-001',
    userId: 'user-labeler',
    values: {
      intent: ['refund'],
      risk_tags: [],
    },
    updatedAt: '2026-05-31 10:28',
  },
]

export const mockLabelingSubmissions: LabelingSubmission[] = [
  {
    id: 'submission-product-revision-001',
    taskId: 'label-task-product-revision',
    taskTitle: '商品质检打回修改',
    userId: 'user-labeler',
    status: 'rejected',
    submittedAt: '2026-05-30 18:42',
    reviewedAt: '2026-05-31 10:12',
    rejectReason: '部分不合格样本缺少原因说明。',
    reviewComment: '请补充具体不一致点，例如图片主体、标题规格或描述承诺。',
    reviewSource: 'manual',
    reviewStatus: 'manual_rejected',
    answers: [
      {
        templateId: productTemplate.id,
        schemaVersion: productTemplate.schema.version,
        values: {
          product_title: '主动降噪无线耳机 黑色',
          quality_result: 'failed',
          quality_reason: '',
        },
      },
    ],
  },
  {
    id: 'submission-support-001',
    taskId: 'label-task-support-submitted',
    taskTitle: '客服会话风险复核',
    userId: 'user-labeler',
    status: 'submitted',
    submittedAt: '2026-05-30 20:06',
    reviewStatus: 'manual_pending',
    aiDecision: 'manual_review',
    aiReviewSummary: '用户情绪和风险标签存在不确定性，需要人工复核。',
    reviewComment: '高风险会话标签存在歧义。',
    answers: [
      {
        templateId: supportTemplate.id,
        schemaVersion: supportTemplate.schema.version,
        values: {
          intent: ['complaint'],
          risk_tags: ['angry'],
        },
      },
    ],
  },
]

export const mockLabelingReviews: LabelingReviewSummary[] = [
  {
    taskId: 'label-task-product-revision',
    reason: '部分不合格样本缺少原因说明。',
    comment: '请补充具体不一致点，例如图片主体、标题规格或描述承诺。',
    reviewedAt: '2026-05-31 10:12',
    reviewerName: '审核员王敏',
  },
]

export const mockLabelerSubmissionStats: LabelerSubmissionStats = {
  submitted: 2,
  approved: 0,
  rejected: 1,
  needsRevision: 1,
  inProgress: 1,
}
