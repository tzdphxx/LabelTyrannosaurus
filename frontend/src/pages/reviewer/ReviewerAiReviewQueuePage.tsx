import {
  Alert,
  Button,
  Card,
  Descriptions,
  Empty,
  List,
  Pagination,
  Progress,
  Select,
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
import styles from './ReviewerPages.module.css'

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

function formatJson(value: unknown) {
  if (value === undefined || value === null || value === '') {
    return '-'
  }

  if (typeof value === 'string') {
    try {
      return JSON.stringify(JSON.parse(value), null, 2)
    } catch {
      return value
    }
  }

  try {
    return JSON.stringify(value, null, 2)
  } catch {
    return String(value)
  }
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

function getRiskFlags(record: AiReviewResultResponse | null) {
  if (!record?.riskFlags) {
    return []
  }

  if (Array.isArray(record.riskFlags)) {
    return record.riskFlags
  }

  try {
    const parsed = JSON.parse(record.riskFlags)

    return Array.isArray(parsed) ? parsed.map(String) : [record.riskFlags]
  } catch {
    return [record.riskFlags]
  }
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
  const riskFlags = useMemo(() => getRiskFlags(currentAiReviewLog), [currentAiReviewLog])

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
    <main className={styles.page}>
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

      <div className={styles.aiShell}>
        <Card className={styles.aiQueue} title="题目队列">
          <Space direction="vertical" size={12} className={styles.panelStack}>
            <Select
              className={styles.aiQueueFilter}
              options={statusOptions}
              value={statusFilter}
              onChange={(value) => changeStatus(value as AiReviewQueueStatusFilter)}
            />
            <List
              className={styles.aiQueueList}
              dataSource={aiReviewLogs}
              loading={isAiReviewLogsLoading}
              locale={{ emptyText: '暂无 AI 审核记录' }}
              renderItem={(record, index) => {
                const recordKey = getRecordKey(record, index)
                const selectedKey = currentAiReviewLog ? getRecordKey(currentAiReviewLog, index) : ''
                const recordTitle = record.taskTitle ? `${record.taskTitle} / 提交 ${formatValue(record.submissionId)}` : `提交 ${formatValue(record.submissionId ?? record.agentRunId)}`
                const recordSummary = record.suggestion ?? (record.submittedAt ? `提交时间：${record.submittedAt}` : `提交状态：${formatValue(record.submissionStatus)}`)

                return (
                  <List.Item
                    className={`${styles.aiItem} ${recordKey === selectedKey ? styles.aiItemActive : ''}`}
                    onClick={() => selectRecord(record)}
                  >
                    <div className={styles.aiItemContent}>
                      <div className={styles.aiItemHeader}>
                        <Typography.Text className={styles.aiItemTitle} ellipsis={{ tooltip: recordTitle }} strong>
                          {recordTitle}
                        </Typography.Text>
                        <Tag color={statusColors[record.aiReviewStatus] ?? 'default'}>{formatValue(record.aiReviewStatus)}</Tag>
                      </div>
                      <div className={styles.aiItemMeta}>
                        <Tag color={decisionColors[record.decision] ?? 'default'}>{formatValue(record.decision)}</Tag>
                        <Typography.Text type="secondary">平均分 {formatValue(record.averageScore)}</Typography.Text>
                        <Typography.Text type="secondary">{formatValue(record.submissionStatus)}</Typography.Text>
                      </div>
                      <Typography.Text className={styles.aiItemFooter} ellipsis={{ tooltip: recordSummary }} type="secondary">
                        {recordSummary}
                      </Typography.Text>
                    </div>
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

        <section className={styles.aiDetail}>
          <div className={styles.aiDetailTopGrid}>
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
              <Space direction="vertical" size={16} className={styles.panelStack}>
                <Descriptions bordered column={3} size="small">
                  <Descriptions.Item label="任务标题">{formatValue(currentAiReviewLog.taskTitle)}</Descriptions.Item>
                  <Descriptions.Item label="提交 ID">{formatValue(currentAiReviewLog.submissionId)}</Descriptions.Item>
                  <Descriptions.Item label="任务 ID">{formatValue(currentAiReviewLog.taskId)}</Descriptions.Item>
                  <Descriptions.Item label="提交状态">{formatValue(currentAiReviewLog.submissionStatus)}</Descriptions.Item>
                  <Descriptions.Item label="提交时间">{formatValue(currentAiReviewLog.submittedAt)}</Descriptions.Item>
                  <Descriptions.Item label="AgentRun">{formatValue(currentAiReviewLog.agentRunId)}</Descriptions.Item>
                  <Descriptions.Item label="状态">{formatValue(currentAiReviewLog.aiReviewStatus)}</Descriptions.Item>
                  <Descriptions.Item label="结论">{formatValue(currentAiReviewLog.decision)}</Descriptions.Item>
                  <Descriptions.Item label="重试次数">{formatValue(currentAiReviewLog.retryCount)}</Descriptions.Item>
                </Descriptions>
                <Alert message={formatValue(currentAiReviewLog.suggestion)} showIcon type="info" />
                <div className={styles.aiRiskCompact}>
                  <Typography.Text className={styles.aiRiskTitle}>风险标记</Typography.Text>
                  {riskFlags.length ? (
                    <Space wrap size={[4, 4]}>
                      {riskFlags.map((flag) => (
                        <Tag key={flag} color="warning">
                          {flag}
                        </Tag>
                      ))}
                    </Space>
                  ) : (
                    <Typography.Text className={styles.aiRiskEmpty} type="secondary">
                      暂无风险标记
                    </Typography.Text>
                  )}
                </div>
              </Space>
            ) : (
              <Empty description="请选择一条 AI 审核记录" />
            )}
          </Card>

          <Card title="AI评分维度">
            {dimensionEntries.length > 0 ? (
              <Space direction="vertical" size={12} className={styles.panelStack}>
                <Progress percent={toPercent(currentAiReviewLog?.averageScore)} size="small" status="active" />
                {dimensionEntries.map(([name, score]) => (
                  <div key={name} className={styles.scoreRow}>
                    <Typography.Text>{name}</Typography.Text>
                    <Progress percent={toPercent(score)} size="small" />
                  </div>
                ))}
              </Space>
            ) : (
              <Empty description="暂无评分维度" />
            )}
          </Card>

          </div>

          <div className={styles.aiDetailMiddleGrid}>
            <Card title="标注内容">
              {currentAiReviewLog?.answerJson ? (
                <pre className={styles.codeBlock}>{formatJson(currentAiReviewLog.answerJson)}</pre>
              ) : (
                <Empty description="暂无标注内容" />
              )}
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

          <Card className={styles.aiPromptCard} title="Prompt 快照">
            <pre className={styles.codeBlock}>{formatValue(currentAiReviewLog?.promptSnapshot)}</pre>
          </Card>
        </section>
      </div>
    </main>
  )
}
