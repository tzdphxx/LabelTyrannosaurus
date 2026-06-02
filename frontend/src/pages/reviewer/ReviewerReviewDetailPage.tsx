import {
  Alert,
  Button,
  Card,
  Descriptions,
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
import { useNavigate, useParams } from 'react-router'
import { ContentShell } from '../../components/page/ContentShell'
import { PageHeader } from '../../components/page/PageHeader'
import { StatePlaceholder } from '../../components/states/StatePlaceholder'
import { useReviewStore } from '../../stores/reviewStore'
import type { SubmissionVersion } from '../../types/review'

const aiDecisionLabels: Record<string, string> = {
  PASS: 'AI已建议通过',
  REJECT: 'AI已建议打回',
  MANUAL_REVIEW: '转人工',
  pass: 'AI已建议通过',
  reject: 'AI已建议打回',
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

function isPendingStatus(status?: string) {
  return status === 'PENDING_FINAL' || status === 'manual_pending'
}

export function ReviewerReviewDetailPage() {
  const navigate = useNavigate()
  const { reviewId } = useParams()
  const [messageApi, contextHolder] = message.useMessage()
  const [reviewComment, setReviewComment] = useState('')
  const [rejectReason, setRejectReason] = useState('')
  const [rejectOpen, setRejectOpen] = useState(false)
  const queue = useReviewStore((state) => state.queue)
  const currentDetail = useReviewStore((state) => state.currentDetail)
  const submissionVersions = useReviewStore((state) => state.submissionVersions)
  const todayReviewedCount = useReviewStore((state) => state.todayReviewedCount)
  const error = useReviewStore((state) => state.error)
  const isDetailLoading = useReviewStore((state) => state.isDetailLoading)
  const isVersionsLoading = useReviewStore((state) => state.isVersionsLoading)
  const isQueueLoading = useReviewStore((state) => state.isQueueLoading)
  const isActionSubmitting = useReviewStore((state) => state.isActionSubmitting)
  const loadQueue = useReviewStore((state) => state.loadQueue)
  const loadDetail = useReviewStore((state) => state.loadDetail)
  const loadSubmissionVersions = useReviewStore((state) => state.loadSubmissionVersions)
  const submitManualReviewAction = useReviewStore((state) => state.submitManualReviewAction)

  useEffect(() => {
    if (reviewId) {
      void loadDetail(reviewId)
      void loadSubmissionVersions(reviewId)
    }
    void loadQueue()
  }, [loadDetail, loadQueue, loadSubmissionVersions, reviewId])

  const rawSubmission = currentDetail?.rawSubmission
  const aiDecision = rawSubmission?.aiDecision ?? currentDetail?.aiDecision
  const pendingCount = useMemo(
    () => queue.filter((item) => isPendingStatus(item.submissionReviewStatus) || item.manualReviewStatus === 'pending').length,
    [queue],
  )
  const passedCount = useMemo(
    () => queue.filter((item) => item.submissionReviewStatus === 'manual_approved' || item.manualReviewStatus === 'approved').length,
    [queue],
  )
  const rejectedCount = useMemo(
    () => queue.filter((item) => item.submissionReviewStatus === 'manual_rejected' || item.manualReviewStatus === 'rejected').length,
    [queue],
  )
  const isActionDisabled = rawSubmission?.submissionStatus === 'APPROVED' || rawSubmission?.submissionStatus === 'REJECTED'

  const reloadDetail = () => {
    if (!reviewId) {
      return
    }

    void loadDetail(reviewId)
    void loadSubmissionVersions(reviewId)
    void loadQueue()
  }

  const submitApprove = () => {
    if (!reviewId) {
      return
    }

    Modal.confirm({
      title: '确认通过',
      content: `确认通过提交 ${reviewId} 吗？`,
      okText: '确认通过',
      cancelText: '取消',
      onOk: async () => {
        const updatedDetail = await submitManualReviewAction(reviewId, {
          reviewerId: 'current-reviewer',
          reviewerName: '当前审核员',
          decision: 'approved',
          comment: reviewComment.trim() || undefined,
        })

        if (updatedDetail) {
          messageApi.success('审核通过已提交')
          setReviewComment('')
          reloadDetail()
        } else {
          messageApi.error('审核通过提交失败')
        }
      },
    })
  }

  const submitReject = async () => {
    if (!reviewId) {
      return
    }

    if (!rejectReason.trim()) {
      messageApi.error('打回原因不能为空')
      return
    }

    const updatedDetail = await submitManualReviewAction(reviewId, {
      reviewerId: 'current-reviewer',
      reviewerName: '当前审核员',
      decision: 'rejected',
      reason: rejectReason.trim(),
    })

    if (updatedDetail) {
      messageApi.success('审核打回已提交')
      setRejectReason('')
      setRejectOpen(false)
      reloadDetail()
    } else {
      messageApi.error('审核打回提交失败')
    }
  }

  if (!reviewId) {
    return <StatePlaceholder status="empty" message="缺少提交 ID。" />
  }

  if (!currentDetail && isDetailLoading) {
    return <StatePlaceholder status="loading" message="正在加载审核详情。" />
  }

  if (!currentDetail && !isDetailLoading) {
    return <StatePlaceholder status="empty" message="未找到对应的审核提交。" />
  }

  return (
    <main className="reviewer-page">
      {contextHolder}
      <ContentShell>
        <PageHeader
          title={`提交 ${currentDetail?.submissionId ?? reviewId}`}
          description="基于真实审核接口展示提交基础信息、AI 预审状态、历史版本和人工终审操作。"
          extra={
            <>
              <Button icon={<ArrowLeftOutlined />} onClick={() => navigate('/app/reviewer/queue')}>
                返回队列
              </Button>
              <Button icon={<ReloadOutlined />} loading={isDetailLoading || isVersionsLoading || isQueueLoading} onClick={reloadDetail}>
                刷新
              </Button>
            </>
          }
        />
      </ContentShell>

      {error ? <Alert message={error} showIcon type="error" /> : null}

      {currentDetail ? (
        <div className="reviewer-workbench">
          <aside className="reviewer-workbench__left">
            <Card title="题目状态">
              <Space direction="vertical" size={12} className="reviewer-panel-stack">
                <div className="reviewer-status-card reviewer-status-card--active">
                  <Typography.Text strong>提交 {currentDetail.submissionId}</Typography.Text>
                  <Typography.Text type="secondary">任务 {currentDetail.taskId}</Typography.Text>
                  <Tag color={aiDecisionColors[formatValue(aiDecision)] ?? 'default'}>
                    {aiDecisionLabels[formatValue(aiDecision)] ?? formatValue(aiDecision)}
                  </Tag>
                </div>
                <Descriptions column={1} size="small">
                  <Descriptions.Item label="标注员">{formatValue(rawSubmission?.labelerId ?? currentDetail.labelerId)}</Descriptions.Item>
                  <Descriptions.Item label="提交状态">{formatValue(rawSubmission?.submissionStatus)}</Descriptions.Item>
                  <Descriptions.Item label="审核级别">{formatValue(rawSubmission?.reviewLevel)}</Descriptions.Item>
                  <Descriptions.Item label="分配审核员">{formatValue(rawSubmission?.assignedReviewerId)}</Descriptions.Item>
                </Descriptions>
              </Space>
            </Card>
          </aside>

          <section className="reviewer-workbench__center">
            <Card title="题目详细信息">
              <Descriptions bordered column={2} size="small">
                <Descriptions.Item label="提交 ID">{formatValue(rawSubmission?.submissionId ?? currentDetail.submissionId)}</Descriptions.Item>
                <Descriptions.Item label="任务 ID">{formatValue(rawSubmission?.taskId ?? currentDetail.taskId)}</Descriptions.Item>
                <Descriptions.Item label="标注员 ID">{formatValue(rawSubmission?.labelerId ?? currentDetail.labelerId)}</Descriptions.Item>
                <Descriptions.Item label="提交状态">{formatValue(rawSubmission?.submissionStatus)}</Descriptions.Item>
                <Descriptions.Item label="冲突状态">{formatValue(rawSubmission?.conflictStatus)}</Descriptions.Item>
                <Descriptions.Item label="审核级别">{formatValue(rawSubmission?.reviewLevel)}</Descriptions.Item>
              </Descriptions>
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
                dataSource={submissionVersions}
                loading={isVersionsLoading}
                locale={{ emptyText: '暂无历史提交记录' }}
                pagination={false}
                rowKey={(record) => `${record.submissionId}-${record.versionNo}`}
                size="small"
              />
            </Card>

            <Card title="AI 预审结果">
              <Space direction="vertical" size={12} className="reviewer-panel-stack">
                <Space wrap>
                  <Tag color={aiDecisionColors[formatValue(aiDecision)] ?? 'default'}>
                    {aiDecisionLabels[formatValue(aiDecision)] ?? formatValue(aiDecision)}
                  </Tag>
                  <Tag>{formatValue(rawSubmission?.aiReviewStatus)}</Tag>
                  <Tag>{formatValue(rawSubmission?.conflictStatus)}</Tag>
                </Space>
                <Alert message="当前详情接口仅返回 AI 结论和审核状态，未返回 AI 评分、命中原因或模型解释。" showIcon type="info" />
              </Space>
            </Card>

            <Card title="人工审核员意见">
              <Space direction="vertical" size={12} className="reviewer-action-panel">
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
                {isActionDisabled ? <Alert message="该提交已完成审核，不能重复操作。" showIcon /> : null}
              </Space>
            </Card>
          </section>

          <aside className="reviewer-workbench__right">
            <Card title="今日工作状态">
              <div className="reviewer-stat-grid">
                <Statistic title="今日审核" value={todayReviewedCount} />
                <Statistic title="待审核" value={pendingCount} />
                <Statistic title="已通过" value={passedCount} />
                <Statistic title="已打回" value={rejectedCount} />
              </div>
            </Card>

            <Card title="当前题目审计日志">
              {submissionVersions.length > 0 ? (
                <Timeline
                  items={submissionVersions.map((version) => ({
                    children: (
                      <Space direction="vertical" size={2}>
                        <Typography.Text strong>
                          版本 {version.versionNo} / {formatValue(version.latestReviewAction)}
                        </Typography.Text>
                        <Typography.Text type="secondary">{formatValue(version.submittedAt)}</Typography.Text>
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
      ) : null}

      <Modal
        confirmLoading={isActionSubmitting}
        okText="确认打回"
        open={rejectOpen}
        title="打回提交"
        onCancel={() => setRejectOpen(false)}
        onOk={() => void submitReject()}
      >
        <Space direction="vertical" size={12} className="reviewer-action-panel">
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
