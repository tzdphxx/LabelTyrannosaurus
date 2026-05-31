import type { DynamicFormSchema } from '../types/dynamicForm'
import type { TemplateDetail } from '../types/template'

const productQualitySchema: DynamicFormSchema = {
  id: 'tpl-product-quality',
  version: 'v1.2',
  title: '商品质检标注模板',
  nodes: [
    {
      id: 'node-product-title',
      key: 'product_title',
      type: 'input',
      title: '商品标题',
      props: {
        placeholder: '请输入商品标题',
      },
      rules: [{ type: 'required' }],
    },
    {
      id: 'node-quality-group',
      key: 'quality_group',
      type: 'group',
      title: '质检结论',
      props: {},
      children: [
        {
          id: 'node-quality-result',
          key: 'quality_result',
          type: 'radio',
          title: '是否合格',
          props: {
            options: [
              { label: '合格', value: 'passed' },
              { label: '不合格', value: 'failed' },
            ],
          },
          rules: [{ type: 'required' }],
        },
        {
          id: 'node-quality-reason',
          key: 'quality_reason',
          type: 'textarea',
          title: '不合格原因',
          props: {
            placeholder: '当结论为不合格时填写原因',
          },
          visibleWhen: {
            fieldKey: 'quality_result',
            operator: 'equals',
            value: 'failed',
          },
        },
      ],
    },
  ],
}

const supportIntentSchema: DynamicFormSchema = {
  id: 'tpl-support-intent',
  version: 'v1.0',
  title: '客服意图分类模板',
  nodes: [
    {
      id: 'node-dialog-summary',
      key: 'dialog_summary',
      type: 'showItem',
      title: '对话摘要',
      props: {
        text: '请阅读客服对话，并判断用户意图、情绪和风险等级。',
      },
    },
    {
      id: 'node-intent',
      key: 'intent',
      type: 'select',
      title: '用户意图',
      props: {
        mode: 'tags',
        placeholder: '选择或输入意图标签',
        options: [
          { label: '咨询', value: 'consult' },
          { label: '投诉', value: 'complaint' },
          { label: '退款', value: 'refund' },
        ],
      },
      rules: [{ type: 'required' }],
    },
    {
      id: 'node-risk-tags',
      key: 'risk_tags',
      type: 'checkbox',
      title: '风险标签',
      props: {
        options: [
          { label: '情绪激烈', value: 'angry' },
          { label: '重复投诉', value: 'repeat' },
          { label: '高价值客户', value: 'vip' },
        ],
      },
    },
  ],
}

const riskReviewSchema: DynamicFormSchema = {
  id: 'tpl-risk-review',
  version: 'v0.9',
  title: '内容风险审核模板',
  nodes: [
    {
      id: 'node-risk-tabs',
      key: 'risk_tabs',
      type: 'tabs',
      title: '风险审核',
      props: {},
      children: [
        {
          id: 'node-risk-basic-tab',
          key: 'risk_basic_tab',
          type: 'tabPane',
          title: '基础判断',
          props: {},
          children: [
            {
              id: 'node-risk-level',
              key: 'risk_level',
              type: 'radio',
              title: '风险等级',
              props: {
                options: [
                  { label: '无风险', value: 'none' },
                  { label: '低风险', value: 'low' },
                  { label: '高风险', value: 'high' },
                ],
              },
              rules: [{ type: 'required' }],
            },
          ],
        },
        {
          id: 'node-risk-evidence-tab',
          key: 'risk_evidence_tab',
          type: 'tabPane',
          title: '证据说明',
          props: {},
          children: [
            {
              id: 'node-risk-evidence',
              key: 'risk_evidence',
              type: 'textarea',
              title: '证据片段',
              props: {
                placeholder: '填写命中的风险证据',
              },
            },
          ],
        },
      ],
    },
  ],
}

export const mockTemplates: TemplateDetail[] = [
  {
    id: 'tpl-product-quality',
    name: '商品质检标注模板',
    version: 'v1.2',
    status: 'ready',
    fieldCount: 12,
    description: '用于商品图片、标题和描述的一致性检查。',
    schema: productQualitySchema,
    updatedAt: '2026-05-30 18:10',
  },
  {
    id: 'tpl-support-intent',
    name: '客服意图分类模板',
    version: 'v1.0',
    status: 'ready',
    fieldCount: 8,
    description: '识别客服对话中的用户意图、情绪和风险等级。',
    schema: supportIntentSchema,
    updatedAt: '2026-05-30 18:18',
  },
  {
    id: 'tpl-risk-review',
    name: '内容风险审核模板',
    version: 'v0.9',
    status: 'draft',
    fieldCount: 10,
    description: '用于文本内容风险类别、证据片段和处置建议标注。',
    schema: riskReviewSchema,
    updatedAt: '2026-05-30 18:24',
  },
]
