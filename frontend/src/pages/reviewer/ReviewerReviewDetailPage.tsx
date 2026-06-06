import {
  Alert,
  Button,
  Card,
  Descriptions,
  Empty,
  Input,
  List,
  Modal,
  Space,
  Statistic,
  Table,
  Tag,
  Timeline,
  Typography,
  message,
} from 'antd'
import { ArrowLeftOutlined, CheckOutlined, CloseOutlined, ReloadOutlined } from '@ant-design/icons'
import { useEffect, useMemo, useState } from 'react'
import { useNavigate, useParams, useSearchParams } from 'react-router'
import { ContentShell } from '../../components/page/ContentShell'
import { PageHeader } from '../../components/page/PageHeader'
import { StatePlaceholder } from '../../components/states/StatePlaceholder'
import { useReviewStore } from '../../stores/reviewStore'
import type { ReviewerTaskItemRow, SubmissionVersion } from '../../types/review'
import styles from './ReviewerPages.module.css'

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

function formatValue(value: unknown) {
  if (value === null || value === undefined || value === '') {
    return '-'
  }

  return String(value)
}

function getItemTitle(item: ReviewerTaskItemRow) {
  return item.externalId?.trim() || `题目 ${item.datasetItemId}`
}

function canOpenItem(item: ReviewerTaskItemRow) {
  return Boolean(item.canOpenSubmissionDetail && item.latestSubmissionId)
}

function getItemKey(item: ReviewerTaskItemRow) {
  return `${item.datasetItemId}-${item.latestSubmissionId ?? item.assignmentId ?? 'item'}`
}

export function ReviewerReviewDetailPage() {
  const navigate = useNavigate()
  const { taskId } = useParams()
  const [searchParams, setSearchParams] = useSearchParams()
  const [messageApi, contextHolder] = message.useMessage()
  const [reviewComment, setReviewComment] = useState('')
  const [rejectReason, setRejectReason] = useState('')
  const [rejectOpen, setRejectOpen] = useState(false)
  const taskItemsPage = useReviewStore((state) => state.taskItemsPage)
  const currentDetail = useReviewStore((state) => state.currentDetail)
  const submissionVersions = useReviewStore((state) => state.submissionVersions)
  const todayReviewedCount = useReviewStore((state) => state.todayReviewedCount)
  const error = useReviewStore((state) => state.error)
  const isTaskItemsLoading = useReviewStore((state) => state.isTaskItemsLoading)
  const isDetailLoading = useReviewStore((state) => state.isDetailLoading)
  const isVersionsLoading = useReviewStore((state) => state.isVersionsLoading)
  const isActionSubmitting = useReviewStore((state) => state.isActionSubmitting)
  const loadReviewerTaskItems = useReviewStore((state) => state.loadReviewerTaskItems)
  const loadDetail = useReviewStore((state) => state.loadDetail)
  const submitManualReviewAction = useReviewStore((state) => state.submitManualReviewAction)

  const selectedSubmissionId = searchParams.get('submissionId')
  const taskItems = taskItemsPage?.page.items ?? []

  useEffect(() => {
    if (taskId) {
      void loadReviewerTaskItems(taskId, { page: 1, size: 100 })
    }
  }, [loadReviewerTaskItems, taskId])

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
  const aiDecision = rawSubmission?.aiReviewResult?.decision ?? rawSubmission?.aiDecision ?? selectedItem?.aiDecision ?? displayedDetail?.aiDecision
  const summary = taskItemsPage?.statusSummary
  const isActionDisabled =
    !displayedDetail ||
    !selectedItem?.canReview ||
    rawSubmission?.submissionStatus === 'APPROVED' ||
    rawSubmission?.submissionStatus === 'REJECTED'

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

  const submitApprove = () => {
    if (!selectedSubmissionId) {
      return
    }

    Modal.confirm({
      title: '确认通过',
      content: `确认通过提交 ${selectedSubmissionId} 吗？`,
      okText: '确认通过',
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

    if (!rejectReason.trim()) {
      messageApi.error('打回原因不能为空')
      return
    }

    const updatedDetail = await submitManualReviewAction(selectedSubmissionId, {
      reviewerId: 'current-reviewer',
      reviewerName: '当前审核员',
      decision: 'rejected',
      reason: rejectReason.trim(),
    })

    if (updatedDetail) {
      messageApi.success('审核打回已提交')
      setRejectReason('')
      setRejectOpen(false)
      void reloadDetail()
    } else {
      messageApi.error('审核打回提交失败')
    }
  }

  if (!taskId) {
    return <StatePlaceholder status="empty" message="缺少任务 ID。" />
  }

  return (
    <main className={styles.page}>
      {contextHolder}
      <ContentShell>
        <PageHeader
          title={taskItemsPage?.taskTitle?.trim() || `任务 ${taskId}`}
          description="左侧切换题目，中间查看提交详情，右侧跟踪任务进度。"
          extra={
            <>
              <Button icon={<ArrowLeftOutlined />} onClick={() => navigate('/app/reviewer/queue')}>
                返回队列
              </Button>
              <Button icon={<ReloadOutlined />} loading={isTaskItemsLoading || isDetailLoading || isVersionsLoading} onClick={reloadDetail}>
                刷新
              </Button>
            </>
          }
        />
      </ContentShell>

      {error ? <Alert message={error} showIcon type="error" /> : null}

      <div className={styles.workbench}>
        <aside className={styles.workbenchLeft}>
          <Card title="任务题目">
            {taskItems.length > 0 ? (
              <div className={styles.taskSidebarList}>
                {taskItems.map((item) => {
                  const active = selectedItem ? getItemKey(selectedItem) === getItemKey(item) : false
                  const disabled = !canOpenItem(item)

                  return (
                    <button
                      key={getItemKey(item)}
                      className={`${styles.taskSidebarItem} ${active ? styles.taskSidebarItemActive : ''} ${
                        disabled ? styles.taskSidebarItemDisabled : ''
                      }`}
                      disabled={disabled}
                      type="button"
                      onClick={() => openItem(item)}
                    >
                      <span className={styles.taskSidebarTitle}>{getItemTitle(item)}</span>
                      <span className={styles.taskSidebarMeta}>
                        提交 {formatValue(item.latestSubmissionId)} · 版本 {formatValue(item.versionNo)}
                      </span>
                      <span className={styles.taskSidebarStatus}>
                        <Tag color={item.canReview ? 'processing' : 'default'}>{formatValue(item.reviewTaskStatus)}</Tag>
                        <Tag color={item.aiDecision === 'REJECT' || item.aiDecision === 'reject' ? 'error' : 'success'}>
                          {formatValue(item.aiDecision)}
                        </Tag>
                      </span>
                    </button>
                  )
                })}
              </div>
            ) : (
              <Empty description={isTaskItemsLoading ? '正在加载题目...' : '当前任务暂无题目'} />
            )}
          </Card>
        </aside>

        <section className={styles.workbenchCenter}>
          <Card title="题目详细信息">
            {displayedDetail ? (
              <Descriptions bordered column={2} size="small">
                <Descriptions.Item label="提交 ID">{formatValue(rawSubmission?.submissionId ?? displayedDetail.submissionId)}</Descriptions.Item>
                <Descriptions.Item label="任务 ID">{formatValue(rawSubmission?.taskId ?? displayedDetail.taskId)}</Descriptions.Item>
                <Descriptions.Item label="题目 ID">{formatValue(rawSubmission?.datasetItemId ?? selectedItem?.datasetItemId)}</Descriptions.Item>
                <Descriptions.Item label="标注员 ID">{formatValue(rawSubmission?.labelerId ?? displayedDetail.labelerId)}</Descriptions.Item>
                <Descriptions.Item label="提交状态">{formatValue(rawSubmission?.submissionStatus ?? selectedItem?.submissionStatus)}</Descriptions.Item>
                <Descriptions.Item label="审核级别">{formatValue(rawSubmission?.reviewLevel ?? selectedItem?.reviewLevel)}</Descriptions.Item>
              </Descriptions>
            ) : (
              <Empty description={isDetailLoading ? '正在加载提交详情...' : '请选择可查看的题目'} />
            )}
          </Card>

          <Card title="历史提交记录">
            <Table<SubmissionVersion>
              columns={[
                { title: '版本', dataIndex: 'versionNo', width: 72 },
                { title: '状态', dataIndex: 'status', render: (value: string) => <Tag>{formatValue(value)}</Tag> },
                {
                  title: '黄金样本',
                  dataIndex: 'isGolden',
                  width: 96,
                  render: (value: boolean) => (value ? <Tag color="success">是</Tag> : <Tag>否</Tag>),
                },
                { title: '提交时间', dataIndex: 'submittedAt', width: 190, render: formatValue },
                { title: 'AI 结论', dataIndex: 'aiDecision', render: formatValue },
                { title: '流转动作', dataIndex: 'aiFlowAction', render: formatValue },
                { title: '最新审核动作', dataIndex: 'latestReviewAction', render: formatValue },
              ]}
              dataSource={displayedDetail ? submissionVersions : []}
              loading={isVersionsLoading}
              locale={{ emptyText: '暂无历史提交记录' }}
              pagination={false}
              rowKey={(record) => `${record.submissionId}-${record.versionNo}`}
              size="small"
            />
          </Card>

          <Card title="AI 预审结果">
            {displayedDetail ? (
              <Space direction="vertical" size={12} className={styles.panelStack}>
                <Space wrap>
                  <Tag color={aiDecisionColors[formatValue(aiDecision)] ?? 'default'}>
                    {aiDecisionLabels[formatValue(aiDecision)] ?? formatValue(aiDecision)}
                  </Tag>
                  <Tag>{formatValue(rawSubmission?.aiReviewStatus ?? selectedItem?.aiReviewStatus)}</Tag>
                  <Tag>均分 {formatValue(rawSubmission?.aiReviewResult?.averageScore ?? selectedItem?.averageScore)}</Tag>
                </Space>
                <Alert message={formatValue(rawSubmission?.aiReviewResult?.suggestion ?? selectedItem?.suggestion)} showIcon type="info" />
              </Space>
            ) : (
              <Empty description="暂无 AI 预审详情" />
            )}
          </Card>

          <Card title="人工审核员意见">
            <Space direction="vertical" size={12} className={styles.actionPanel}>
              <Input.TextArea
                rows={3}
                placeholder="通过时可填写审核评语"
                value={reviewComment}
                onChange={(event) => setReviewComment(event.target.value)}
              />
              <Space wrap>
                <Button
                  danger
                  disabled={isActionDisabled}
                  icon={<CloseOutlined />}
                  loading={isActionSubmitting}
                  onClick={() => setRejectOpen(true)}
                >
                  打回
                </Button>
                <Button
                  disabled={isActionDisabled}
                  icon={<CheckOutlined />}
                  loading={isActionSubmitting}
                  type="primary"
                  onClick={submitApprove}
                >
                  通过
                </Button>
              </Space>
              {isActionDisabled && displayedDetail ? <Alert message="该题目当前不可审核或已完成终审。" showIcon /> : null}
            </Space>
          </Card>
        </section>

        <aside className={styles.workbenchRight}>
          <Card title="任务进度">
            <div className={styles.statGrid}>
              <Statistic title="总题目" value={taskItemsPage?.totalItemCount ?? taskItemsPage?.page.total ?? 0} />
              <Statistic title="已提交" value={summary?.submittedCount ?? 0} />
              <Statistic title="已通过" value={summary?.approvedCount ?? 0} />
              <Statistic title="已打回" value={summary?.returnedCount ?? 0} />
              <Statistic title="已领取" value={summary?.claimedCount ?? 0} />
              <Statistic title="今日审核" value={todayReviewedCount} />
            </div>
          </Card>

          <Card title="当前题目审计日志">
            {displayedDetail && submissionVersions.length > 0 ? (
              <Timeline
                items={submissionVersions.map((version) => ({
                  children: (
                    <Space direction="vertical" size={2}>
                      <Typography.Text strong>
                        版本 {version.versionNo} / {formatValue(version.latestReviewAction)}
                      </Typography.Text>
                      <Typography.Text type="secondary">{formatValue(version.submittedAt ?? version.createdAt)}</Typography.Text>
                      <Typography.Text type="secondary">Hash: {formatValue(version.answerHash)}</Typography.Text>
                    </Space>
                  ),
                }))}
              />
            ) : (
              <List locale={{ emptyText: '暂无审计日志' }} />
            )}
          </Card>
        </aside>
      </div>

      <Modal
        confirmLoading={isActionSubmitting}
        okText="确认打回"
        open={rejectOpen}
        title="打回提交"
        onCancel={() => setRejectOpen(false)}
        onOk={() => void submitReject()}
      >
        <Space direction="vertical" size={12} className={styles.actionPanel}>
          <Typography.Text type="secondary">打回后标注员可重新修改并提交。</Typography.Text>
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
