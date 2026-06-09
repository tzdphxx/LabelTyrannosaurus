import {
  Alert,
  Button,
  Card,
  Checkbox,
  Empty,
  Input,
  Modal,
  Progress,
  Segmented,
  Space,
  Statistic,
  Tag,
  Timeline,
  Tooltip,
  Typography,
  message,
} from 'antd'
import {
  ArrowLeftOutlined,
  CheckCircleOutlined,
  CloseCircleOutlined,
  EditOutlined,
  ExportOutlined,
  ReloadOutlined,
  UserSwitchOutlined,
} from '@ant-design/icons'
import { useEffect, useMemo, useState } from 'react'
import { useNavigate, useParams, useSearchParams } from 'react-router'
import { ContentShell } from '../../components/page/ContentShell'
import { PageHeader } from '../../components/page/PageHeader'
import { StatePlaceholder } from '../../components/states/StatePlaceholder'
import { useReviewStore } from '../../stores/reviewStore'
import type { ReviewerTaskItemRow, SubmissionVersion } from '../../types/review'
import styles from './ReviewerPages.module.css'

type QueueFilter = 'all' | 'pass' | 'reject' | 'manual'

const aiDecisionLabels: Record<string, string> = {
  PASS: 'AI 建议通过',
  REJECT: 'AI 建议打回',
  MANUAL_REVIEW: '转人工',
  pass: 'AI 建议通过',
  reject: 'AI 建议打回',
  manual_review: '转人工',
}

const aiDecisionColors: Record<string, string> = {
  PASS: 'success',
  REJECT: 'error',
  MANUAL_REVIEW: 'processing',
  pass: 'success',
  reject: 'error',
  manual_review: 'processing',
}

const reviewReasonPresets = ['事实不一致', '格式不合规', '缺少关键信息', '需补充证据']

function formatValue(value: unknown) {
  if (value === null || value === undefined || value === '') {
    return '-'
  }

  return String(value)
}

function formatJson(value: unknown) {
  if (!value) {
    return '-'
  }

  if (typeof value === 'string') {
    try {
      return JSON.stringify(JSON.parse(value), null, 2)
    } catch {
      return value
    }
  }

  return JSON.stringify(value, null, 2)
}

function toEditableJson(value: unknown) {
  if (!value) {
    return '{}'
  }

  if (typeof value === 'string') {
    try {
      return JSON.stringify(JSON.parse(value), null, 2)
    } catch {
      return value
    }
  }

  return JSON.stringify(value, null, 2)
}

function getItemTitle(item: ReviewerTaskItemRow) {
  return item.externalId?.trim() || `题目 ${item.datasetItemId}`
}

function canOpenItem(item: ReviewerTaskItemRow) {
  return Boolean(item.canOpenSubmissionDetail && item.latestSubmissionId)
}

function canReviewItem(item: ReviewerTaskItemRow) {
  return Boolean(item.canReview && item.latestSubmissionId)
}

function getReviewId(item: ReviewerTaskItemRow) {
  return item.latestSubmissionId ? String(item.latestSubmissionId) : ''
}

function getItemKey(item: ReviewerTaskItemRow) {
  return `${item.datasetItemId}-${item.latestSubmissionId ?? item.assignmentId ?? 'item'}`
}

function normalizeDecision(decision?: string) {
  const value = decision?.toLowerCase()

  if (value === 'pass') return 'pass'
  if (value === 'reject') return 'reject'
  if (value === 'manual_review') return 'manual'

  return 'manual'
}

function getDecisionLabel(decision?: string) {
  return aiDecisionLabels[formatValue(decision)] ?? formatValue(decision)
}

function getDecisionColor(decision?: string) {
  return aiDecisionColors[formatValue(decision)] ?? 'default'
}

function getDecisionClassName(decision?: string) {
  const normalizedDecision = normalizeDecision(decision)

  if (normalizedDecision === 'pass') {
    return styles.manualQueueItemPass
  }

  if (normalizedDecision === 'reject') {
    return styles.manualQueueItemReject
  }

  return styles.manualQueueItemManual
}

export function ReviewerReviewDetailPage() {
  const navigate = useNavigate()
  const { taskId } = useParams()
  const [searchParams, setSearchParams] = useSearchParams()
  const [messageApi, contextHolder] = message.useMessage()
  const [modalApi, modalContextHolder] = Modal.useModal()
  const [queueFilter, setQueueFilter] = useState<QueueFilter>('all')
  const [reviewComment, setReviewComment] = useState('')
  const [rejectOpen, setRejectOpen] = useState(false)
  const [rejectReason, setRejectReason] = useState('')
  const [revisionOpen, setRevisionOpen] = useState(false)
  const [revisedAnswerText, setRevisedAnswerText] = useState('')
  const [revisionError, setRevisionError] = useState('')
  const taskItemsPage = useReviewStore((state) => state.taskItemsPage)
  const currentDetail = useReviewStore((state) => state.currentDetail)
  const submissionVersions = useReviewStore((state) => state.submissionVersions)
  const selectedReviewIds = useReviewStore((state) => state.selectedReviewIds)
  const todayReviewedCount = useReviewStore((state) => state.todayReviewedCount)
  const error = useReviewStore((state) => state.error)
  const isTaskItemsLoading = useReviewStore((state) => state.isTaskItemsLoading)
  const isDetailLoading = useReviewStore((state) => state.isDetailLoading)
  const isVersionsLoading = useReviewStore((state) => state.isVersionsLoading)
  const isActionSubmitting = useReviewStore((state) => state.isActionSubmitting)
  const isBatchSubmitting = useReviewStore((state) => state.isBatchSubmitting)
  const loadReviewerTaskItems = useReviewStore((state) => state.loadReviewerTaskItems)
  const loadDetail = useReviewStore((state) => state.loadDetail)
  const setSelectedReviewIds = useReviewStore((state) => state.setSelectedReviewIds)
  const submitManualReviewAction = useReviewStore((state) => state.submitManualReviewAction)
  const submitBatchManualReviewAction = useReviewStore((state) => state.submitBatchManualReviewAction)

  const selectedSubmissionId = searchParams.get('submissionId')
  const taskItems = taskItemsPage?.page.items ?? []
  const reviewableItems = useMemo(() => taskItems.filter(canReviewItem), [taskItems])
  const reviewableIds = useMemo(() => reviewableItems.map(getReviewId).filter(Boolean), [reviewableItems])

  useEffect(() => {
    if (taskId) {
      void loadReviewerTaskItems(taskId, { page: 1, size: 100 })
    }
  }, [loadReviewerTaskItems, taskId])

  useEffect(() => {
    const nextSelectedReviewIds = selectedReviewIds.filter((reviewId) => reviewableIds.includes(reviewId))

    if (nextSelectedReviewIds.length !== selectedReviewIds.length) {
      setSelectedReviewIds(nextSelectedReviewIds)
    }
  }, [reviewableIds, selectedReviewIds, setSelectedReviewIds])

  const selectedItem = useMemo(() => {
    if (selectedSubmissionId) {
      const matched = taskItems.find((item) => String(item.latestSubmissionId) === selectedSubmissionId)

      if (matched) {
        return matched
      }
    }

    return taskItems.find(canOpenItem) ?? taskItems[0] ?? null
  }, [selectedSubmissionId, taskItems])

  useEffect(() => {
    if (!taskId || !selectedItem || !canOpenItem(selectedItem) || !selectedItem.latestSubmissionId) {
      return
    }

    const nextSubmissionId = String(selectedItem.latestSubmissionId)

    if (selectedSubmissionId !== nextSubmissionId) {
      setSearchParams({ submissionId: nextSubmissionId }, { replace: true })
      return
    }

    void loadDetail(nextSubmissionId)
  }, [loadDetail, selectedItem, selectedSubmissionId, setSearchParams, taskId])

  const displayedDetail =
    currentDetail && selectedSubmissionId && String(currentDetail.submissionId) === selectedSubmissionId ? currentDetail : null
  const rawSubmission = displayedDetail?.rawSubmission
  const aiResult = rawSubmission?.aiReviewResult
  const aiDecision = aiResult?.decision ?? rawSubmission?.aiDecision ?? selectedItem?.aiDecision ?? displayedDetail?.aiDecision
  const summary = taskItemsPage?.statusSummary
  const isActionDisabled =
    !displayedDetail ||
    !selectedItem?.canReview ||
    rawSubmission?.submissionStatus === 'APPROVED' ||
    rawSubmission?.submissionStatus === 'REJECTED'
  const passCount = taskItems.filter((item) => normalizeDecision(item.aiDecision) === 'pass').length
  const rejectCount = taskItems.filter((item) => normalizeDecision(item.aiDecision) === 'reject').length
  const manualCount = taskItems.filter((item) => normalizeDecision(item.aiDecision) === 'manual').length
  const approvedCount = summary?.approvedCount ?? 0
  const returnedCount = summary?.returnedCount ?? 0
  const passRate = approvedCount + returnedCount > 0 ? Math.round((approvedCount / (approvedCount + returnedCount)) * 100) : 0
  const filteredItems = taskItems.filter((item) => queueFilter === 'all' || normalizeDecision(item.aiDecision) === queueFilter)
  const currentVersion = submissionVersions[0] ?? null
  const previousVersion = submissionVersions.find((version) => version.versionNo !== rawSubmission?.versionNo) ?? null

  const openItem = (item: ReviewerTaskItemRow) => {
    if (!canOpenItem(item) || !item.latestSubmissionId) {
      return
    }

    setSearchParams({ submissionId: String(item.latestSubmissionId) })
  }

  const reloadDetail = async () => {
    if (!taskId) {
      return
    }

    await loadReviewerTaskItems(taskId, { page: 1, size: 100 })

    if (selectedSubmissionId) {
      void loadDetail(selectedSubmissionId)
    }
  }

  const toggleSelect = (reviewId: string, checked: boolean) => {
    setSelectedReviewIds(checked ? [...selectedReviewIds, reviewId] : selectedReviewIds.filter((item) => item !== reviewId))
  }

  const openRevisionModal = () => {
    if (!displayedDetail) {
      return
    }

    setRevisionError('')
    setRevisedAnswerText(toEditableJson(rawSubmission?.answerJson ?? displayedDetail.answers ?? {}))
    setRevisionOpen(true)
  }

  const submitRevisionApprove = async () => {
    if (!selectedSubmissionId) {
      return
    }

    let parsedAnswer: unknown

    try {
      parsedAnswer = JSON.parse(revisedAnswerText)
    } catch {
      setRevisionError('答案 JSON 格式不正确，请修正后再提交。')
      return
    }

    const updatedDetail = await submitManualReviewAction(selectedSubmissionId, {
      reviewerId: 'current-reviewer',
      reviewerName: '当前审核员',
      decision: 'approved',
      comment: reviewComment.trim() || undefined,
      revisedAnswerJson: JSON.stringify(parsedAnswer),
    })

    if (updatedDetail) {
      messageApi.success('修订答案已通过并提交')
      setRevisionOpen(false)
      setRevisedAnswerText('')
      setRevisionError('')
      setReviewComment('')
      void reloadDetail()
    } else {
      messageApi.error('修订提交失败')
    }
  }

  const submitApprove = () => {
    if (!selectedSubmissionId) {
      return
    }

    modalApi.confirm({
      title: '确认通过该提交',
      content: `提交 ${selectedSubmissionId} 将进入通过状态。`,
      okText: '通过·入库',
      cancelText: '取消',
      onOk: async () => {
        const updatedDetail = await submitManualReviewAction(selectedSubmissionId, {
          reviewerId: 'current-reviewer',
          reviewerName: '当前审核员',
          decision: 'approved',
          comment: reviewComment.trim() || undefined,
        })

        if (updatedDetail) {
          messageApi.success('审核通过已提交')
          setReviewComment('')
          void reloadDetail()
        } else {
          messageApi.error('审核通过提交失败')
        }
      },
    })
  }

  const submitReject = async () => {
    if (!selectedSubmissionId) {
      return
    }

    const reason = rejectReason.trim() || reviewComment.trim()
    const updatedDetail = await submitManualReviewAction(selectedSubmissionId, {
      reviewerId: 'current-reviewer',
      reviewerName: '当前审核员',
      decision: 'rejected',
      reason,
      comment: reviewComment.trim() || undefined,
    })

    if (updatedDetail) {
      messageApi.success('审核打回已提交')
      setRejectOpen(false)
      setRejectReason('')
      setReviewComment('')
      void reloadDetail()
    } else {
      messageApi.error('审核打回提交失败')
    }
  }

  const submitBatchAction = (decision: 'approved' | 'rejected') => {
    if (selectedReviewIds.length === 0) {
      messageApi.warning('请先选择需要处理的提交')
      return
    }

    modalApi.confirm({
      title: decision === 'approved' ? '批量通过' : '批量打回',
      content: `确认处理已选 ${selectedReviewIds.length} 条提交？`,
      okText: decision === 'approved' ? '批量通过' : '批量打回',
      cancelText: '取消',
      onOk: async () => {
        const result = await submitBatchManualReviewAction(selectedReviewIds, {
          reviewerId: 'current-reviewer',
          reviewerName: '当前审核员',
          decision,
          reason: decision === 'rejected' ? '批量打回' : undefined,
          comment: reviewComment.trim() || undefined,
        })

        if (result) {
          messageApi.success(`成功 ${result.success.length} 条，失败 ${result.failed.length} 条`)
          void reloadDetail()
        } else {
          messageApi.error('批量审核提交失败')
        }
      },
    })
  }

  if (!taskId) {
    return <StatePlaceholder status="empty" message="缺少任务 ID。" />
  }

  return (
    <main className={styles.page}>
      {contextHolder}
      {modalContextHolder}
      <ContentShell>
        <PageHeader
          title="人工审核"
          description={`审核与质检 / 人工审核 / ${taskItemsPage?.taskTitle?.trim() || `任务 ${taskId}`}`}
          extra={
            <>
              <Button icon={<ArrowLeftOutlined />} onClick={() => navigate('/app/reviewer/queue')}>
                返回队列
              </Button>
              <Button icon={<ReloadOutlined />} loading={isTaskItemsLoading || isDetailLoading || isVersionsLoading} onClick={reloadDetail}>
                刷新
              </Button>
              <Tooltip title="当前未发现审计日志导出接口">
                <Button disabled icon={<ExportOutlined />}>
                  导出审计日志
                </Button>
              </Tooltip>
            </>
          }
        />
      </ContentShell>

      {error ? <Alert message={error} showIcon type="error" /> : null}

      <div className={styles.manualReviewWorkbench}>
        <aside className={styles.manualQueuePanel}>
          <div className={styles.manualQueueHeader}>
            <Typography.Text strong>审核队列</Typography.Text>
            <Tag>{taskItems.length} 条</Tag>
          </div>
          <Segmented
            className={styles.manualQueueTabs}
            value={queueFilter}
            options={[
              { label: `全部 ${taskItems.length}`, value: 'all' },
              { label: `建议通过 ${passCount}`, value: 'pass' },
              { label: `建议打回 ${rejectCount}`, value: 'reject' },
              { label: `转人工 ${manualCount}`, value: 'manual' },
            ]}
            onChange={(value) => setQueueFilter(value as QueueFilter)}
          />
          <div className={styles.batchToolbar}>
            <Checkbox
              checked={reviewableIds.length > 0 && selectedReviewIds.length === reviewableIds.length}
              indeterminate={selectedReviewIds.length > 0 && selectedReviewIds.length < reviewableIds.length}
              onChange={(event) => setSelectedReviewIds(event.target.checked ? reviewableIds : [])}
            />
            <Typography.Text type="secondary">已选 {selectedReviewIds.length} 条</Typography.Text>
            <Button
              size="small"
              loading={isBatchSubmitting}
              disabled={selectedReviewIds.length === 0}
              onClick={() => submitBatchAction('approved')}
            >
              批量通过
            </Button>
            <Button
              danger
              size="small"
              loading={isBatchSubmitting}
              disabled={selectedReviewIds.length === 0}
              onClick={() => submitBatchAction('rejected')}
            >
              批量打回
            </Button>
            <Tooltip title="当前 reviewer 接口未提供指派能力">
              <Button disabled size="small" icon={<UserSwitchOutlined />}>
                指派给
              </Button>
            </Tooltip>
          </div>

          <div className={styles.manualQueueList}>
            {filteredItems.length > 0 ? (
              filteredItems.map((item) => {
                const active = selectedItem ? getItemKey(selectedItem) === getItemKey(item) : false
                const disabled = !canOpenItem(item)
                const reviewId = getReviewId(item)

                return (
                  <button
                    key={getItemKey(item)}
                    className={`${styles.manualQueueItem} ${getDecisionClassName(item.aiDecision)} ${active ? styles.manualQueueItemActive : ''} ${
                      disabled ? styles.manualQueueItemDisabled : ''
                    }`}
                    disabled={disabled}
                    type="button"
                    onClick={() => openItem(item)}
                  >
                    <span className={styles.manualQueueItemTop}>
                      <Checkbox
                        checked={selectedReviewIds.includes(reviewId)}
                        disabled={!canReviewItem(item)}
                        onClick={(event) => event.stopPropagation()}
                        onChange={(event) => toggleSelect(reviewId, event.target.checked)}
                      />
                      <span className={styles.manualQueueId}>#{formatValue(item.latestSubmissionId)}</span>
                      <Tag color={getDecisionColor(item.aiDecision)}>{getDecisionLabel(item.aiDecision)}</Tag>
                    </span>
                    <span className={styles.manualQueueTitle}>{getItemTitle(item)}</span>
                    <span className={styles.manualQueueMeta}>
                      {formatValue(item.labelerName ?? item.labelerId)} / v{formatValue(item.versionNo)} / {formatValue(item.submittedAt)}
                    </span>
                    <span className={styles.manualQueueScore}>
                      <span>AI 分数 {formatValue(item.averageScore)}</span>
                      <span>{formatValue(item.reviewTaskStatus)}</span>
                    </span>
                  </button>
                )
              })
            ) : (
              <Empty description={isTaskItemsLoading ? '正在加载审核队列...' : '当前筛选下暂无提交'} />
            )}
          </div>
        </aside>

        <section className={styles.manualDetailPanel}>
          <Card className={styles.manualHeroCard}>
            {displayedDetail ? (
              <div className={styles.manualDetailHero}>
                <div>
                  <Typography.Text className={styles.taskEyebrow}>SUBMISSION #{displayedDetail.submissionId}</Typography.Text>
                  <Typography.Title level={3}>{selectedItem ? getItemTitle(selectedItem) : `题目 ${rawSubmission?.datasetItemId}`}</Typography.Title>
                  <Space wrap>
                    <Tag>任务 {formatValue(rawSubmission?.taskId ?? displayedDetail.taskId)}</Tag>
                    <Tag>题目 {formatValue(rawSubmission?.datasetItemId ?? selectedItem?.datasetItemId)}</Tag>
                    <Tag>版本 v{formatValue(rawSubmission?.versionNo ?? selectedItem?.versionNo)}</Tag>
                    <Tag>{formatValue(rawSubmission?.submissionStatus ?? selectedItem?.submissionStatus)}</Tag>
                  </Space>
                </div>
                <div className={styles.manualHeroMeta}>
                  <span>标注员</span>
                  <strong>{formatValue(selectedItem?.labelerName ?? rawSubmission?.labelerId)}</strong>
                  <span>{formatValue(rawSubmission?.createdAt ?? selectedItem?.submittedAt)}</span>
                </div>
              </div>
            ) : (
              <Empty description={isDetailLoading ? '正在加载提交详情...' : '请选择可查看的提交'} />
            )}
          </Card>

          <div className={styles.submissionCompareGrid}>
            <Card title="上一次提交">
              {previousVersion ? (
                <Space direction="vertical" size={10} className={styles.panelStack}>
                  <Tag>v{previousVersion.versionNo}</Tag>
                  <Typography.Text type="secondary">
                    {formatValue(previousVersion.status)} / {formatValue(previousVersion.submittedAt ?? previousVersion.createdAt)}
                  </Typography.Text>
                  <pre className={styles.manualCodeBlock}>{formatJson(previousVersion)}</pre>
                </Space>
              ) : (
                <Empty description="暂无上一版提交" />
              )}
            </Card>
            <Card title="当前提交">
              {displayedDetail ? (
                <Space direction="vertical" size={10} className={styles.panelStack}>
                  <Tag color="processing">v{formatValue(rawSubmission?.versionNo ?? currentVersion?.versionNo)}</Tag>
                  <Typography.Text type="secondary">
                    {formatValue(rawSubmission?.submissionStatus)} / {formatValue(rawSubmission?.updatedAt ?? rawSubmission?.createdAt)}
                  </Typography.Text>
                  <pre className={styles.manualCodeBlock}>{formatJson(rawSubmission?.answerJson ?? displayedDetail.answers)}</pre>
                </Space>
              ) : (
                <Empty description="暂无当前提交" />
              )}
            </Card>
          </div>

          <Card className={styles.manualAiCard} title="AI 预审结果">
            {displayedDetail ? (
              <div className={styles.manualAiGrid}>
                <div>
                  <Space wrap>
                    <Tag color={getDecisionColor(aiDecision)}>{getDecisionLabel(aiDecision)}</Tag>
                    <Tag>{formatValue(aiResult?.status ?? rawSubmission?.aiReviewStatus ?? selectedItem?.aiReviewStatus)}</Tag>
                    <Tag>模型运行 {formatValue(rawSubmission?.agentRunSummary?.agentRunId)}</Tag>
                  </Space>
                  <Typography.Paragraph>{formatValue(aiResult?.suggestion ?? selectedItem?.suggestion)}</Typography.Paragraph>
                </div>
                <div className={styles.manualScoreBox}>
                  <span>AI 平均分</span>
                  <strong>{formatValue(aiResult?.averageScore ?? selectedItem?.averageScore)}</strong>
                  <Progress percent={Number(aiResult?.averageScore ?? selectedItem?.averageScore ?? 0)} showInfo={false} />
                </div>
                <div className={styles.manualRiskBox}>
                  <Typography.Text strong>风险标记</Typography.Text>
                  <Typography.Text type="secondary">{formatValue(aiResult?.riskFlags ?? selectedItem?.riskFlags)}</Typography.Text>
                </div>
              </div>
            ) : (
              <Empty description="暂无 AI 预审详情" />
            )}
          </Card>

          <Card title="人工审核意见">
            <Space direction="vertical" size={14} className={styles.actionPanel}>
              <Input.TextArea
                rows={4}
                placeholder="填写审核意见，打回时将作为原因补充"
                value={reviewComment}
                onChange={(event) => setReviewComment(event.target.value)}
              />
              <Space wrap>
                {reviewReasonPresets.map((reason) => (
                  <Tag.CheckableTag
                    key={reason}
                    checked={rejectReason === reason}
                    onChange={(checked) => setRejectReason(checked ? reason : '')}
                  >
                    {reason}
                  </Tag.CheckableTag>
                ))}
              </Space>
              <div className={styles.manualActionGrid}>
                <Button
                  danger
                  className={styles.manualActionButton}
                  disabled={isActionDisabled}
                  icon={<CloseCircleOutlined />}
                  loading={isActionSubmitting}
                  onClick={() => setRejectOpen(true)}
                >
                  打回
                </Button>
                <Tooltip title="修订答案后将直接通过并入库">
                  <Button
                    className={styles.manualActionButton}
                    disabled={isActionDisabled}
                    icon={<EditOutlined />}
                    loading={isActionSubmitting}
                    onClick={openRevisionModal}
                  >
                    直接修订
                  </Button>
                </Tooltip>
                <Button
                  className={styles.manualActionButton}
                  disabled={isActionDisabled}
                  icon={<CheckCircleOutlined />}
                  loading={isActionSubmitting}
                  type="primary"
                  onClick={submitApprove}
                >
                  通过·入库
                </Button>
              </div>
              {isActionDisabled && displayedDetail ? <Alert message="该题目当前不可审核或已完成终审。" showIcon /> : null}
            </Space>
          </Card>
        </section>

        <aside className={styles.manualInspectorPanel}>
          <Card title="今日质检">
            <div className={styles.manualStatGrid}>
              <Statistic title="今日已审" value={todayReviewedCount} />
              <Statistic title="通过率" value={passRate} suffix="%" />
              <Statistic title="待审" value={summary?.submittedCount ?? reviewableItems.length} />
              <Statistic title="SLA" value="-" />
            </div>
          </Card>
          <Card title="审计时间线">
            {displayedDetail ? (
              <Timeline
                items={[
                  ...(rawSubmission?.reviewRecords ?? []).map((record) => ({
                    children: (
                      <Space direction="vertical" size={2}>
                        <Typography.Text strong>{formatValue(record.action)}</Typography.Text>
                        <Typography.Text type="secondary">{formatValue(record.createdAt)}</Typography.Text>
                        <Typography.Text type="secondary">{formatValue(record.reason ?? record.reviewComment)}</Typography.Text>
                      </Space>
                    ),
                  })),
                  ...submissionVersions.map((version: SubmissionVersion) => ({
                    children: (
                      <Space direction="vertical" size={2}>
                        <Typography.Text strong>
                          提交版本 v{version.versionNo} / {formatValue(version.latestReviewAction)}
                        </Typography.Text>
                        <Typography.Text type="secondary">{formatValue(version.submittedAt ?? version.createdAt)}</Typography.Text>
                      </Space>
                    ),
                  })),
                ]}
              />
            ) : (
              <Empty description="暂无审计日志" />
            )}
          </Card>
        </aside>
      </div>

      <Modal
        confirmLoading={isActionSubmitting}
        okText="修订并通过"
        open={revisionOpen}
        title="直接修订并通过"
        width={720}
        onCancel={() => {
          setRevisionOpen(false)
          setRevisionError('')
        }}
        onOk={() => void submitRevisionApprove()}
      >
        <Space direction="vertical" size={12} className={styles.actionPanel}>
          <Typography.Text type="secondary">
            修订后的答案会通过 approve 接口提交到 revisedAnswerJson 字段，并直接进入通过状态。
          </Typography.Text>
          {revisionError ? <Alert message={revisionError} showIcon type="error" /> : null}
          <Input.TextArea
            rows={16}
            value={revisedAnswerText}
            onChange={(event) => {
              setRevisedAnswerText(event.target.value)
              setRevisionError('')
            }}
          />
        </Space>
      </Modal>

      <Modal
        confirmLoading={isActionSubmitting}
        okText="确认打回"
        open={rejectOpen}
        title="打回提交"
        onCancel={() => setRejectOpen(false)}
        onOk={() => void submitReject()}
      >
        <Space direction="vertical" size={12} className={styles.actionPanel}>
          <Typography.Text type="secondary">打回后，标注员可重新修改并提交。</Typography.Text>
          <Input.TextArea
            rows={4}
            placeholder="请输入打回原因"
            value={rejectReason}
            onChange={(event) => setRejectReason(event.target.value)}
          />
        </Space>
      </Modal>
    </main>
  )
}
