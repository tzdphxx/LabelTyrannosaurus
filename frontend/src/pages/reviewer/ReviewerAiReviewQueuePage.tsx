import {
  Alert,
  Button,
  Card,
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
import styles from './ReviewerPages.module.css'

const statusOptions: Array<{ label: string; value: AiReviewQueueStatusFilter }> = [
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

const decisionLabels: Record<string, string> = {
  PASS: '建议通过',
  REJECT: '建议打回',
  MANUAL_REVIEW: '转人工复核',
}

function formatValue(value: unknown) {
  if (value === undefined || value === null || value === '') {
    return '-'
  }

  return String(value)
}

function formatCount(value: unknown) {
  const numberValue = Number(value ?? 0)

  if (Number.isNaN(numberValue)) {
    return '0'
  }

  return numberValue.toLocaleString('zh-CN')
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

function getScoreColor(score: number) {
  if (score >= 80) {
    return '#16a34a'
  }

  if (score >= 60) {
    return '#f97316'
  }

  return '#ef4444'
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

function getDimensionEntries(record: AiReviewResultResponse | null) {
  if (!record) {
    return []
  }

  if (record.dimensions?.length) {
    return record.dimensions.map((dimension) => [dimension.name, dimension.score] as const)
  }

  return Object.entries(record.dimensionScores ?? {})
}

export function ReviewerAiReviewQueuePage() {
  const [messageApi, contextHolder] = message.useMessage()
  const [statusFilter, setStatusFilter] = useState<AiReviewQueueStatusFilter>('pending')
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
    setAiReviewLogQuery(buildQueryByStatus('pending'))
    void loadAllAiReviewLogs()
  }, [loadAllAiReviewLogs, setAiReviewLogQuery])

  const dimensionEntries = useMemo(() => getDimensionEntries(currentAiReviewLog), [currentAiReviewLog])
  const riskFlags = useMemo(() => getRiskFlags(currentAiReviewLog), [currentAiReviewLog])
  const selectedAverageScore = toPercent(currentAiReviewLog?.averageScore)
  const selectedDecision = formatValue(currentAiReviewLog?.decision)
  const selectedDecisionLabel = decisionLabels[selectedDecision] ?? selectedDecision

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
          title="AI 自动预审队列"
          description="异步消费提交数据 -> 调用 LLM 结构化评分 -> 通过 / 打回 / 转人工复核"
          extra={
            <Space wrap>
              <Tag color="success">服务在线</Tag>
              <Tag>幂等键 idempotency_key</Tag>
              <Button icon={<ReloadOutlined />} loading={isAiReviewLogsLoading} onClick={() => void loadAllAiReviewLogs()}>
                刷新
              </Button>
            </Space>
          }
        />
      </ContentShell>

      {error ? <Alert message={error} showIcon type="error" /> : null}

      <div className={styles.aiShell}>
        <Card className={styles.aiQueue} title="AI 自动预审队列">
          <Space direction="vertical" size={12} className={styles.panelStack}>
            <Segmented
              className={styles.aiQueueTabs}
              options={statusOptions}
              value={statusFilter}
              onChange={(value) => changeStatus(value as AiReviewQueueStatusFilter)}
            />

            <div className={styles.aiQueueStats}>
              <span className={styles.aiPulse} />
              <div>
                <strong>{formatCount(aiReviewLogTotal)}</strong>
                <span>条记录</span>
              </div>
              <div>
                <strong>1.4s</strong>
                <span>平均耗时</span>
              </div>
              <div>
                <strong>1.2%</strong>
                <span>重试率</span>
              </div>
            </div>

            <List
              className={styles.aiQueueList}
              dataSource={aiReviewLogs}
              loading={isAiReviewLogsLoading}
              locale={{ emptyText: '暂无 AI 审核记录' }}
              renderItem={(record, index) => {
                const recordKey = getRecordKey(record, index)
                const selectedKey = currentAiReviewLog ? getRecordKey(currentAiReviewLog, index) : ''
                const recordTitle = record.taskTitle ? record.taskTitle : `提交 ${formatValue(record.submissionId ?? record.agentRunId)}`
                const averageScore = toPercent(record.averageScore)
                const decision = formatValue(record.decision)
                const decisionLabel = decisionLabels[decision] ?? decision

                return (
                  <List.Item
                    className={`${styles.aiItem} ${recordKey === selectedKey ? styles.aiItemActive : ''}`}
                    onClick={() => selectRecord(record)}
                  >
                    <div className={styles.aiItemContent}>
                      <div className={styles.aiItemHeader}>
                        <Typography.Text className={styles.aiItemId} type="secondary">
                          SUB-{formatValue(record.submissionId ?? record.agentRunId)}
                        </Typography.Text>
                        <Typography.Text className={styles.aiItemTime} type="secondary">
                          {formatValue(record.submittedAt ?? record.createdAt)}
                        </Typography.Text>
                      </div>
                      <Typography.Text className={styles.aiItemTitle} ellipsis={{ tooltip: recordTitle }} strong>
                        {recordTitle}
                      </Typography.Text>
                      <div className={styles.aiItemMeta}>
                        <Tag color={decisionColors[decision] ?? 'default'}>{decisionLabel}</Tag>
                        <Tag color={statusColors[record.aiReviewStatus] ?? 'default'}>{formatValue(record.aiReviewStatus)}</Tag>
                        <Typography.Text type="secondary">分数 {formatValue(averageScore)}</Typography.Text>
                      </div>
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
          <div className={styles.aiReviewHero}>
            <div>
              <Typography.Title level={4}>
                SUB-{formatValue(currentAiReviewLog?.submissionId ?? currentAiReviewLog?.agentRunId)} · {formatValue(currentAiReviewLog?.taskTitle)}
              </Typography.Title>
              <Typography.Text type="secondary">
                提交于 {formatValue(currentAiReviewLog?.submittedAt ?? currentAiReviewLog?.createdAt)} · AgentRun {formatValue(currentAiReviewLog?.agentRunId)}
              </Typography.Text>
            </div>
            <Space wrap>
              <Tag color={decisionColors[selectedDecision] ?? 'default'}>{selectedDecisionLabel}</Tag>
              <Tag color="purple">综合分 {formatValue(selectedAverageScore)}</Tag>
            </Space>
          </div>

          <div className={styles.aiReviewGrid}>
            <Card title="提交内容" extra={<Typography.Text type="secondary">JSON 字段视图</Typography.Text>}>
              {currentAiReviewLog?.answerJson ? (
                <pre className={styles.aiCodePanel}>{formatJson(currentAiReviewLog.answerJson)}</pre>
              ) : (
                <Empty description="暂无提交内容" />
              )}
            </Card>

            <Card title="维度评分" extra={<Typography.Text className={styles.aiScoreTotal}>共 {formatValue(selectedAverageScore)}</Typography.Text>}>
              {dimensionEntries.length > 0 ? (
                <Space direction="vertical" size={12} className={styles.panelStack}>
                  {dimensionEntries.map(([name, score]) => {
                    const percent = toPercent(score)

                    return (
                      <div key={name} className={styles.aiScoreRow}>
                        <Typography.Text>{name}</Typography.Text>
                        <Progress percent={percent} showInfo={false} size="small" strokeColor={getScoreColor(percent)} />
                        <Typography.Text style={{ color: getScoreColor(percent) }} strong>
                          {percent}
                        </Typography.Text>
                      </div>
                    )
                  })}
                </Space>
              ) : (
                <Empty description="暂无评分维度" />
              )}
            </Card>
          </div>

          <Card className={styles.aiCommentCard}>
            {currentAiReviewLog ? (
              <Space direction="vertical" size={10} className={styles.panelStack}>
                <div className={styles.aiCommentHeader}>
                  <Typography.Text strong>AI 评语</Typography.Text>
                  <Tag color={decisionColors[selectedDecision] ?? 'default'}>{selectedDecisionLabel}</Tag>
                  <Typography.Text type="secondary">阈值：综合 &lt; 70 时建议打回</Typography.Text>
                </div>
                <Typography.Paragraph>{formatValue(currentAiReviewLog.suggestion)}</Typography.Paragraph>
                {riskFlags.length ? (
                  <Space wrap size={[6, 6]}>
                    {riskFlags.map((flag) => (
                      <Tag key={flag} color="warning">
                        {flag}
                      </Tag>
                    ))}
                  </Space>
                ) : (
                  <Typography.Text type="secondary">暂无风险标记</Typography.Text>
                )}
              </Space>
            ) : (
              <Empty description="请选择一条 AI 审核记录" />
            )}
          </Card>

          <Card title="审核 Prompt 模板" extra={<Tag color="purple">规则：电商相关性 v2</Tag>}>
            <pre className={styles.aiPromptPanel}>{formatValue(currentAiReviewLog?.promptSnapshot ?? currentAiReviewLog?.rawPrompt)}</pre>
          </Card>

          <Card
            title="处理日志 / 审计"
            extra={
              isRetryable(currentAiReviewLog) ? (
                <Button icon={<ThunderboltOutlined />} loading={isAiReviewRetrying} size="small" type="primary" onClick={retryCurrent}>
                  失败重跑
                </Button>
              ) : null
            }
          >
            {currentAiReviewLog ? (
              <Timeline
                items={[
                  {
                    children: `queue 进入 AI 预审队列：${formatValue(currentAiReviewLog.aiReviewStatus)}`,
                  },
                  {
                    children: `llm 调用完成：${formatValue(currentAiReviewLog.agentRunId)}`,
                  },
                  {
                    children: `verdict 结构化输出：${formatValue(currentAiReviewLog.decision)} (${formatValue(selectedAverageScore)})`,
                  },
                  {
                    children: `retry 重试次数：${formatValue(currentAiReviewLog.retryCount)}`,
                  },
                ]}
              />
            ) : (
              <Empty description="暂无处理日志" />
            )}
          </Card>
        </section>
      </div>
    </main>
  )
}
