import type { ImportPreview } from '../types/import'

export const mockImportPreviews: ImportPreview[] = [
  {
    id: 'import-product-quality',
    fileName: 'product-quality-sample.jsonl',
    fileType: 'jsonl',
    totalRows: 1280,
    validRows: 1264,
    invalidRows: 16,
    mappings: [
      { sourceField: 'image_url', targetField: '商品图片', required: true, matched: true },
      { sourceField: 'title', targetField: '商品标题', required: true, matched: true },
      { sourceField: 'description', targetField: '商品描述', required: false, matched: true },
      { sourceField: 'category', targetField: '类目', required: true, matched: true },
    ],
    samples: [
      {
        id: 'sample-1',
        values: {
          image_url: 'https://cdn.example.com/product-1001.jpg',
          title: '户外防水冲锋衣',
          description: '春秋款，支持轻度防雨。',
          category: '服饰',
        },
      },
      {
        id: 'sample-2',
        values: {
          image_url: 'https://cdn.example.com/product-1002.jpg',
          title: '无线降噪耳机',
          description: '蓝牙 5.3，主动降噪。',
          category: '数码',
        },
      },
    ],
    issues: [
      {
        id: 'issue-1',
        row: 38,
        field: 'image_url',
        level: 'blocking',
        message: '图片地址为空，无法进入标注。',
      },
      {
        id: 'issue-2',
        row: 74,
        field: 'category',
        level: 'warning',
        message: '类目不在模板推荐枚举中。',
      },
    ],
  },
  {
    id: 'import-support-intent',
    fileName: 'support-dialogue.xlsx',
    fileType: 'excel',
    totalRows: 860,
    validRows: 860,
    invalidRows: 0,
    mappings: [
      { sourceField: 'dialogue_id', targetField: '会话 ID', required: true, matched: true },
      { sourceField: 'messages', targetField: '对话内容', required: true, matched: true },
      { sourceField: 'channel', targetField: '渠道', required: false, matched: true },
    ],
    samples: [
      {
        id: 'sample-3',
        values: {
          dialogue_id: 'D-2026-001',
          messages: '用户咨询退款进度，客服已确认订单状态。',
          channel: '在线客服',
        },
      },
      {
        id: 'sample-4',
        values: {
          dialogue_id: 'D-2026-002',
          messages: '用户反馈物流停滞，需要升级处理。',
          channel: '电话',
        },
      },
    ],
    issues: [],
  },
]
