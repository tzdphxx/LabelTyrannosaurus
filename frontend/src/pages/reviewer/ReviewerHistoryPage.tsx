import { Button, Card, Input, Select, Space, Table, Tag, Typography } from 'antd'
import { EyeOutlined, ReloadOutlined } from '@ant-design/icons'
import { useEffect, useMemo, useState } from 'react'
import { useNavigate } from 'react-router'
import { ContentShell } from '../../components/page/ContentShell'
import { PageHeader } from '../../components/page/PageHeader'
import { useReviewStore } from '../../stores/reviewStore'
import type { AiReviewDecision, ManualReviewStatus, ReviewQueueItem, ReviewRiskLevel } from '../../types/review'

const aiDecisionLabels: Record<AiReviewDecision, string> = {
  pass: 'AI 通过',
  manual_review: 'AI 转人工',
  reject: 'AI 打回',
}

const aiDecisionColors: Record<AiReviewDecision, string> = {
  pass: 'success',
  manual_review: 'processing',
  reject: 'error',
}

const manualStatusLabels: Record<ManualReviewStatus, string> = {
  none: '无需人工',
  pending: '待复核',
  in_progress: '进行中',
  approved: '人工通过',
  rejected: '人工打回',
}

const riskLevelLabels: Record<ReviewRiskLevel, string> = {
  low: '低风险',
  medium: '中风险',
  high: '高风险',
}

const decisionOptions = [
  { label: '全部结论', value: 'all' },
  { label: 'AI 通过', value: 'pass' },
  { label: 'AI 转人工', value: 'manual_review' },
  { label: 'AI 打回', value: 'reject' },
]

export function ReviewerHistoryPage() {
  const navigate = useNavigate()
  const history = useReviewStore((state) => state.history)
  const isHistoryLoading = useReviewStore((state) => state.isHistoryLoading)
  const loadHistory = useReviewStore((state) => state.loadHistory)
  const [keyword, setKeyword] = useState('')
  const [decision, setDecision] = useState<AiReviewDecision | 'all'>('all')

  useEffect(() => {
    void loadHistory()
  }, [loadHistory])

  const filteredHistory = useMemo(() => {
    const normalizedKeyword = keyword.trim().toLowerCase()

    return history.filter((item) => {
      const matchesDecision = decision === 'all' || item.aiDecision === decision
      const matchesKeyword =
        normalizedKeyword.length === 0 ||
        item.taskTitle.toLowerCase().includes(normalizedKeyword) ||
        item.labelerName.toLowerCase().includes(normalizedKeyword) ||
        item.aiSummary.toLowerCase().includes(normalizedKeyword)

      return matchesDecision && matchesKeyword
    })
  }, [decision, history, keyword])

  return (
    <main className="reviewer-page">
      <ContentShell>
        <PageHeader
          title="审核历史"
          description="只读查看 AI 通过、AI 打回、AI 转人工和人工复核完成记录。"
          extra={
            <Button icon={<ReloadOutlined />} loading={isHistoryLoading} onClick={() => void loadHistory()}>
              刷新
            </Button>
          }
        />
      </ContentShell>

      <Card className="reviewer-table-card">
        <div className="owner-toolbar">
          <Input.Search
            allowClear
            className="owner-toolbar__search"
            placeholder="搜索任务、标注员或 AI 摘要"
            value={keyword}
            onChange={(event) => setKeyword(event.target.value)}
            onSearch={setKeyword}
          />
          <Select className="owner-toolbar__select" options={decisionOptions} value={decision} onChange={setDecision} />
        </div>

        <Table<ReviewQueueItem>
          columns={[
            {
              title: '提交',
              dataIndex: 'taskTitle',
              render: (_, item) => (
                <Space direction="vertical" size={4}>
                  <Typography.Text strong>{item.taskTitle}</Typography.Text>
                  <Typography.Text type="secondary">
                    标注员：{item.labelerName} / 提交时间：{item.submittedAt}
                  </Typography.Text>
                  <Typography.Text type="secondary">{item.aiSummary}</Typography.Text>
                </Space>
              ),
            },
            {
              title: 'AI 结论',
              dataIndex: 'aiDecision',
              width: 120,
              render: (value: AiReviewDecision) => <Tag color={aiDecisionColors[value]}>{aiDecisionLabels[value]}</Tag>,
            },
            {
              title: '风险',
              dataIndex: 'aiRiskLevel',
              width: 100,
              render: (value: ReviewRiskLevel) => riskLevelLabels[value],
            },
            {
              title: '人工状态',
              dataIndex: 'manualReviewStatus',
              width: 120,
              render: (value: ManualReviewStatus) => manualStatusLabels[value],
            },
            {
              title: '操作',
              width: 120,
              render: (_, item) => (
                <Button icon={<EyeOutlined />} size="small" type="link" onClick={() => navigate(`/app/reviewer/tasks/${item.id}`)}>
                  查看
                </Button>
              ),
            },
          ]}
          dataSource={filteredHistory}
          loading={isHistoryLoading}
          pagination={{ pageSize: 8 }}
          rowKey="id"
        />
      </Card>
    </main>
  )
}
