import {
  Alert,
  Button,
  Card,
  Descriptions,
  Empty,
  List,
  Pagination,
  Progress,
  Segmented,
  Space,
  Tag,
  Timeline,
  Typography,
  message,
} from 'antd'
import { ReloadOutlined, ThunderboltOutlined } from '@ant-design/icons'
import { useEffect, useMemo, useState } from 'react'
import { ContentShell } from '../../components/page/ContentShell'
import { PageHeader } from '../../components/page/PageHeader'
import { useReviewStore } from '../../stores/reviewStore'
import type { AiReviewLogQuery, AiReviewQueueStatusFilter, AiReviewResultResponse } from '../../types/review'

const statusOptions: Array<{ label: string; value: AiReviewQueueStatusFilter }> = [
  { label: '全部', value: 'all' },
  { label: '待审核', value: 'pending' },
  { label: '已通过', value: 'passed' },
  { label: '已打回', value: 'rejected' },
  { label: '转人工', value: 'manual' },
  { label: '失败', value: 'failed' },
]

const statusColors: Record<string, string> = {
  PENDING: 'default',
  RUNNING: 'processing',
  SUCCESS: 'success',
  FAILED: 'error',
  MANUAL_REQUIRED: 'warning',
}

const decisionColors: Record<string, string> = {
  PASS: 'success',
  REJECT: 'error',
  MANUAL_REVIEW: 'processing',
}

function formatValue(value: unknown) {
  if (value === undefined || value === null || value === '') {
    return '-'
  }

  return String(value)
}

function toPercent(value: unknown) {
  const numberValue = Number(value ?? 0)

  if (Number.isNaN(numberValue)) {
    return 0
  }

  return Math.max(0, Math.min(100, numberValue <= 1 ? numberValue * 100 : numberValue))
}

function buildQueryByStatus(status: AiReviewQueueStatusFilter): Partial<AiReviewLogQuery> {
  if (status === 'passed') {
    return { page: 1, decision: 'PASS', status: undefined }
  }

  if (status === 'rejected') {
    return { page: 1, decision: 'REJECT', status: undefined }
  }

  if (status === 'manual') {
    return { page: 1, decision: 'MANUAL_REVIEW', status: undefined }
  }

  if (status === 'failed') {
    return { page: 1, status: 'FAILED', decision: undefined }
  }

  if (status === 'pending') {
    return { page: 1, status: 'PENDING', decision: undefined }
  }

  return { page: 1, status: undefined, decision: undefined }
}

function getRecordKey(record: AiReviewResultResponse, index: number) {
  return String(record.submissionId ?? record.agentRunId ?? index)
}

function isRetryable(record: AiReviewResultResponse | null) {
  return Boolean(record?.submissionId && (record.aiReviewStatus === 'FAILED' || record.aiReviewStatus === 'MANUAL_REQUIRED'))
}

export function ReviewerAiReviewQueuePage() {
  const [messageApi, contextHolder] = message.useMessage()
  const [statusFilter, setStatusFilter] = useState<AiReviewQueueStatusFilter>('all')
  const aiReviewLogs = useReviewStore((state) => state.aiReviewLogs)
  const currentAiReviewLog = useReviewStore((state) => state.currentAiReviewLog)
  const aiReviewLogQuery = useReviewStore((state) => state.aiReviewLogQuery)
  const aiReviewLogTotal = useReviewStore((state) => state.aiReviewLogTotal)
  const error = useReviewStore((state) => state.error)
  const isAiReviewLogsLoading = useReviewStore((state) => state.isAiReviewLogsLoading)
  const isAiReviewRetrying = useReviewStore((state) => state.isAiReviewRetrying)
  const setAiReviewLogQuery = useReviewStore((state) => state.setAiReviewLogQuery)
  const setCurrentAiReviewLog = useReviewStore((state) => state.setCurrentAiReviewLog)
  const loadAllAiReviewLogs = useReviewStore((state) => state.loadAllAiReviewLogs)
  const loadSubmissionAiReview = useReviewStore((state) => state.loadSubmissionAiReview)
  const retrySubmissionAiReview = useReviewStore((state) => state.retrySubmissionAiReview)

  useEffect(() => {
    void loadAllAiReviewLogs()
  }, [loadAllAiReviewLogs])

  const dimensionEntries = useMemo(
    () => Object.entries(currentAiReviewLog?.dimensionScores ?? {}),
    [currentAiReviewLog?.dimensionScores],
  )

  const selectRecord = (record: AiReviewResultResponse) => {
    setCurrentAiReviewLog(record)

    if (record.submissionId) {
      void loadSubmissionAiReview(String(record.submissionId))
    }
  }

  const changeStatus = (value: AiReviewQueueStatusFilter) => {
    setStatusFilter(value)
    setAiReviewLogQuery(buildQueryByStatus(value))
    void loadAllAiReviewLogs()
  }

  const changePage = (page: number, pageSize: number) => {
    setAiReviewLogQuery({ page, pageSize })
    void loadAllAiReviewLogs()
  }

  const retryCurrent = async () => {
    if (!currentAiReviewLog?.submissionId) {
      return
    }

    const result = await retrySubmissionAiReview(String(currentAiReviewLog.submissionId))

    if (result) {
      messageApi.success('AI 审核重试已触发')
    } else {
      messageApi.error('AI 审核重试失败')
    }
  }

  return (
    <main className="reviewer-page">
      {contextHolder}
      <ContentShell>
        <PageHeader
          title="AI审核队列"
          description="查看 AI 预审记录、评分维度、风险标记、Prompt 快照和原始响应。"
          extra={
            <Button icon={<ReloadOutlined />} loading={isAiReviewLogsLoading} onClick={() => void loadAllAiReviewLogs()}>
              刷新
            </Button>
          }
        />
      </ContentShell>

      {error ? <Alert message={error} showIcon type="error" /> : null}

      <div className="reviewer-ai-shell">
        <Card className="reviewer-ai-shell__queue" title="题目队列">
          <Space direction="vertical" size={12} className="reviewer-panel-stack">
            <Segmented
              block
              options={statusOptions}
              value={statusFilter}
              onChange={(value) => changeStatus(value as AiReviewQueueStatusFilter)}
            />
            <List
              dataSource={aiReviewLogs}
              loading={isAiReviewLogsLoading}
              locale={{ emptyText: '暂无 AI 审核记录' }}
              renderItem={(record, index) => {
                const recordKey = getRecordKey(record, index)
                const selectedKey = currentAiReviewLog ? getRecordKey(currentAiReviewLog, index) : ''

                return (
                  <List.Item
                    className={`reviewer-ai-item ${recordKey === selectedKey ? 'reviewer-ai-item--active' : ''}`}
                    onClick={() => selectRecord(record)}
                  >
                    <Space direction="vertical" size={6} className="reviewer-ai-item__content">
                      <Space wrap>
                        <Typography.Text strong>提交 {formatValue(record.submissionId ?? record.agentRunId)}</Typography.Text>
                        <Tag color={statusColors[record.aiReviewStatus] ?? 'default'}>{formatValue(record.aiReviewStatus)}</Tag>
                      </Space>
                      <Space wrap>
                        <Tag color={decisionColors[record.decision] ?? 'default'}>{formatValue(record.decision)}</Tag>
                        <Typography.Text type="secondary">平均分 {formatValue(record.averageScore)}</Typography.Text>
                      </Space>
                      <Typography.Text type="secondary" ellipsis>
                        {formatValue(record.suggestion)}
                      </Typography.Text>
                    </Space>
                  </List.Item>
                )
              }}
            />
            <Pagination
              current={aiReviewLogQuery.page}
              pageSize={aiReviewLogQuery.pageSize}
              showSizeChanger
              size="small"
              total={aiReviewLogTotal}
              onChange={changePage}
            />
          </Space>
        </Card>

        <section className="reviewer-ai-shell__detail">
          <Card
            title="AI 评语"
            extra={
              isRetryable(currentAiReviewLog) ? (
                <Button
                  icon={<ThunderboltOutlined />}
                  loading={isAiReviewRetrying}
                  size="small"
                  type="primary"
                  onClick={retryCurrent}
                >
                  重试
                </Button>
              ) : null
            }
          >
            {currentAiReviewLog ? (
              <Space direction="vertical" size={16} className="reviewer-panel-stack">
                <Descriptions bordered column={3} size="small">
                  <Descriptions.Item label="提交 ID">{formatValue(currentAiReviewLog.submissionId)}</Descriptions.Item>
                  <Descriptions.Item label="任务 ID">{formatValue(currentAiReviewLog.taskId)}</Descriptions.Item>
                  <Descriptions.Item label="AgentRun">{formatValue(currentAiReviewLog.agentRunId)}</Descriptions.Item>
                  <Descriptions.Item label="状态">{formatValue(currentAiReviewLog.aiReviewStatus)}</Descriptions.Item>
                  <Descriptions.Item label="结论">{formatValue(currentAiReviewLog.decision)}</Descriptions.Item>
                  <Descriptions.Item label="重试次数">{formatValue(currentAiReviewLog.retryCount)}</Descriptions.Item>
                </Descriptions>
                <Alert message={formatValue(currentAiReviewLog.suggestion)} showIcon type="info" />
              </Space>
            ) : (
              <Empty description="请选择一条 AI 审核记录" />
            )}
          </Card>

          <div className="reviewer-ai-detail-grid">
            <Card title="AI评分维度">
              {dimensionEntries.length > 0 ? (
                <Space direction="vertical" size={12} className="reviewer-panel-stack">
                  <Progress percent={toPercent(currentAiReviewLog?.averageScore)} size="small" status="active" />
                  {dimensionEntries.map(([name, score]) => (
                    <div key={name} className="reviewer-score-row">
                      <Typography.Text>{name}</Typography.Text>
                      <Progress percent={toPercent(score)} size="small" />
                    </div>
                  ))}
                </Space>
              ) : (
                <Empty description="暂无评分维度" />
              )}
            </Card>

            <Card title="风险标记">
              {currentAiReviewLog?.riskFlags?.length ? (
                <Space wrap>
                  {currentAiReviewLog.riskFlags.map((flag) => (
                    <Tag key={flag} color="warning">
                      {flag}
                    </Tag>
                  ))}
                </Space>
              ) : (
                <Empty description="暂无风险标记" />
              )}
            </Card>
          </div>

          <div className="reviewer-ai-detail-grid">
            <Card title="标注内容">
              <Empty description="暂无标注内容" />
            </Card>
            <Card title="审核模板">
              <Empty description="暂无审核模板" />
            </Card>
          </div>

          <Card title="处理日志">
            {currentAiReviewLog ? (
              <Timeline
                items={[
                  {
                    children: `AI 状态：${formatValue(currentAiReviewLog.aiReviewStatus)}`,
                  },
                  {
                    children: `AI 结论：${formatValue(currentAiReviewLog.decision)}`,
                  },
                  {
                    children: `重试次数：${formatValue(currentAiReviewLog.retryCount)}`,
                  },
                ]}
              />
            ) : (
              <Empty description="暂无处理日志" />
            )}
          </Card>

          <div className="reviewer-ai-detail-grid">
            <Card title="Prompt 快照">
              <pre className="reviewer-ai-code">{formatValue(currentAiReviewLog?.promptSnapshot)}</pre>
            </Card>
            <Card title="LLM 原始响应">
              <pre className="reviewer-ai-code">{formatValue(currentAiReviewLog?.rawResponse)}</pre>
            </Card>
          </div>
        </section>
      </div>
    </main>
  )
}
