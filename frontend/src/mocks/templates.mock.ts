import type { TemplateSummary } from '../types/template'

export const mockTemplates: TemplateSummary[] = [
  {
    id: 'tpl-product-quality',
    name: '商品质检标注模板',
    version: 'v1.2',
    status: 'ready',
    fieldCount: 12,
    description: '用于商品图片、标题和描述的一致性检查。',
  },
  {
    id: 'tpl-support-intent',
    name: '客服意图分类模板',
    version: 'v1.0',
    status: 'ready',
    fieldCount: 8,
    description: '识别客服对话中的用户意图、情绪和风险等级。',
  },
  {
    id: 'tpl-risk-review',
    name: '内容风险审核模板',
    version: 'v0.9',
    status: 'draft',
    fieldCount: 10,
    description: '用于文本内容风险类别、证据片段和处置建议标注。',
  },
]
