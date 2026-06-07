import {
  Alert,
  Button,
  Card,
  Descriptions,
  Input,
  List,
  Modal,
  Radio,
  Space,
  Tag,
  Timeline,
  Typography,
  message,
} from 'antd'
import { ArrowLeftOutlined, ReloadOutlined } from '@ant-design/icons'
import { useEffect, useMemo, useState } from 'react'
import { useNavigate, useParams } from 'react-router'
import { ContentShell } from '../../components/page/ContentShell'
import { PageHeader } from '../../components/page/PageHeader'
import { StatePlaceholder } from '../../components/states/StatePlaceholder'
import { DynamicFormRenderer } from '../../features/dynamic-form/components/DynamicFormRenderer'
import { useReviewStore } from '../../stores/reviewStore'
import type { AiReviewDecision, ManualReviewDecision, ReviewRiskLevel } from '../../types/review'

type ReviewActionMode = ManualReviewDecision | 'revision'

const riskLevelLabels: Record<ReviewRiskLevel, string> = {
  low: '低风险',
  medium: '中风险',
  high: '高风险',
}

const riskLevelColors: Record<ReviewRiskLevel, string> = {
  low: 'success',
  medium: 'warning',
  high: 'error',
}

const actionLabels: Record<ReviewActionMode, string> = {
  approved: '人工通过',
  rejected: '人工打回',
  revision: '修订建议',
}

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

export function ReviewerReviewDetailPage() {
  const navigate = useNavigate()
  const { reviewId } = useParams()
  const [messageApi, contextHolder] = message.useMessage()
  const [actionMode, setActionMode] = useState<ReviewActionMode>('approved')
  const [reason, setReason] = useState('')
  const [comment, setComment] = useState('')
  const queue = useReviewStore((state) => state.queue)
  const currentDetail = useReviewStore((state) => state.currentDetail)
  const error = useReviewStore((state) => state.error)
  const isDetailLoading = useReviewStore((state) => state.isDetailLoading)
  const isActionSubmitting = useReviewStore((state) => state.isActionSubmitting)
  const loadDetail = useReviewStore((state) => state.loadDetail)
  const submitManualReviewAction = useReviewStore((state) => state.submitManualReviewAction)

  useEffect(() => {
    if (reviewId) {
      void loadDetail(reviewId)
    }
  }, [loadDetail, reviewId])

  const isManualCompleted = useMemo(
    () => currentDetail?.manualReviewStatus === 'approved' || currentDetail?.manualReviewStatus === 'rejected',
    [currentDetail?.manualReviewStatus],
  )
  const nextPendingReview = useMemo(
    () =>
      queue.find(
        (item) =>
          item.id !== currentDetail?.id &&
          item.aiDecision === 'manual_review' &&
          (item.manualReviewStatus === 'pending' || item.manualReviewStatus === 'in_progress'),
      ),
    [currentDetail?.id, queue],
  )

  const submitAction = () => {
    if (!reviewId || !currentDetail) {
      return
    }

    if ((actionMode === 'rejected' || actionMode === 'revision') && !reason.trim() && !comment.trim()) {
      messageApi.error(`${actionLabels[actionMode]}必须填写原因或建议`)
      return
    }

    Modal.confirm({
      title: actionLabels[actionMode],
      content: `确认对「${currentDetail.taskTitle}」提交${actionLabels[actionMode]}结论吗？`,
      okText: '确认',
      cancelText: '取消',
      onOk: async () => {
        const decision: ManualReviewDecision = actionMode === 'approved' ? 'approved' : 'rejected'
        const updatedDetail = await submitManualReviewAction(reviewId, {
          reviewerId: 'user-reviewer',
          reviewerName: '审核员王敏',
          decision,
          reason: actionMode === 'revision' ? '修订建议' : reason.trim() || undefined,
          comment: actionMode === 'revision' ? reason.trim() || comment.trim() || undefined : comment.trim() || undefined,
        })

        if (updatedDetail) {
          messageApi.success('人工复核结论已提交')
          setReason('')
          setComment('')
        } else {
          messageApi.error('人工复核结论提交失败')
        }
      },
    })
  }

  if (!reviewId) {
    return <StatePlaceholder status="empty" message="缺少人工复核记录 ID。" />
  }

  if (!currentDetail && isDetailLoading) {
    return <StatePlaceholder status="loading" message="正在加载审核详情。" />
  }

  if (!currentDetail && !isDetailLoading) {
    return <StatePlaceholder status="empty" message="未找到对应的人工复核记录。" />
  }

  return (
    <main className="reviewer-page">
      {contextHolder}
      <ContentShell>
        <PageHeader
          title={currentDetail?.taskTitle ?? '人工复核详情'}
          description="查看提交快照、AI 审核结论和审计时间线，并对 AI 人工复核项给出最终人工结论。"
          extra={
            <>
              <Button icon={<ArrowLeftOutlined />} onClick={() => navigate('/app/reviewer/queue')}>
                返回队列
              </Button>
              <Button icon={<ReloadOutlined />} loading={isDetailLoading} onClick={() => void loadDetail(reviewId)}>
                刷新
              </Button>
            </>
          }
        />
      </ContentShell>

      {error ? <Alert message={error} showIcon type="error" /> : null}

      {currentDetail ? (
        <div className="reviewer-detail-grid">
          <Card title="AI 审核结果">
            <Space direction="vertical" size={12}>
              <Space wrap>
                <Tag color={aiDecisionColors[currentDetail.aiDecision]}>{aiDecisionLabels[currentDetail.aiDecision]}</Tag>
                <Tag color={riskLevelColors[currentDetail.aiRiskLevel]}>{riskLevelLabels[currentDetail.aiRiskLevel]}</Tag>
                {currentDetail.aiReview.status === 'failed' ? <Tag color="error">AI 异常降级</Tag> : null}
              </Space>
              <Typography.Paragraph>{currentDetail.aiReview.summary}</Typography.Paragraph>
              {currentDetail.aiReview.manualReviewReason ? (
                <Alert message={currentDetail.aiReview.manualReviewReason} showIcon type="warning" />
              ) : null}
              {currentDetail.aiReview.errorMessage ? (
                <Alert message={currentDetail.aiReview.errorMessage} showIcon type="error" />
              ) : null}
              <List
                dataSource={currentDetail.aiReview.reasons}
                header="命中原因"
                renderItem={(item) => <List.Item>{item}</List.Item>}
                size="small"
              />
            </Space>
          </Card>

          <Card title="人工复核操作">
            {currentDetail.aiDecision !== 'manual_review' ? (
              <Alert message="该提交已由 AI 自动处理，仅支持只读回看。" showIcon />
            ) : isManualCompleted ? (
              <Alert message={`该提交已${currentDetail.manualReviewStatus === 'approved' ? '人工通过' : '人工打回'}`} showIcon />
            ) : (
              <Space direction="vertical" size={12} className="reviewer-action-panel">
                <Radio.Group
                  optionType="button"
                  options={[
                    { label: '人工通过', value: 'approved' },
                    { label: '人工打回', value: 'rejected' },
                    { label: '修订建议', value: 'revision' },
                  ]}
                  value={actionMode}
                  onChange={(event) => setActionMode(event.target.value)}
                />
                <Input.TextArea
                  rows={3}
                  placeholder={actionMode === 'approved' ? '可选填写审核意见' : '填写打回原因或修订建议'}
                  value={actionMode === 'approved' ? comment : reason}
                  onChange={(event) => (actionMode === 'approved' ? setComment(event.target.value) : setReason(event.target.value))}
                />
                {actionMode !== 'approved' ? (
                  <Input.TextArea rows={3} placeholder="补充说明，可选" value={comment} onChange={(event) => setComment(event.target.value)} />
                ) : null}
                <Button loading={isActionSubmitting} type="primary" onClick={submitAction}>
                  提交{actionLabels[actionMode]}
                </Button>
              </Space>
            )}
            {isManualCompleted && nextPendingReview ? (
              <Button className="reviewer-next-button" type="link" onClick={() => navigate(`/app/reviewer/tasks/${nextPendingReview.id}`)}>
                处理下一条待复核
              </Button>
            ) : null}
          </Card>

          <Card title="提交快照">
            <Descriptions bordered column={1} size="small">
              <Descriptions.Item label="提交 ID">{currentDetail.submissionId}</Descriptions.Item>
              <Descriptions.Item label="标注员">{currentDetail.labelerName}</Descriptions.Item>
              <Descriptions.Item label="提交时间">{currentDetail.submittedAt}</Descriptions.Item>
              <Descriptions.Item label="当前状态">{currentDetail.submissionReviewStatus}</Descriptions.Item>
            </Descriptions>
            <Space direction="vertical" size={16} className="reviewer-answer-list">
              {currentDetail.answers.map((answer) => (
                <Card key={answer.questionId} size="small" title={answer.questionTitle}>
                  <Typography.Paragraph type="secondary">{answer.questionDescription}</Typography.Paragraph>
                  <Descriptions bordered column={1} size="small">
                    {Object.entries(answer.sourceSnapshot).map(([label, value]) => (
                      <Descriptions.Item key={label} label={label}>
                        {value}
                      </Descriptions.Item>
                    ))}
                  </Descriptions>
                  <div className="reviewer-readonly-form">
                    <DynamicFormRenderer
                      readOnly
                      initialValues={answer.answer.values}
                      schema={answer.schemaSnapshot}
                    />
                  </div>
                </Card>
              ))}
            </Space>
          </Card>

          <Card title="审核记录">
            <List
              dataSource={currentDetail.manualReviewRecords}
              locale={{ emptyText: '暂无人工审核记录' }}
              renderItem={(record) => (
                <List.Item>
                  <Space direction="vertical" size={4}>
                    <Typography.Text strong>{record.decision === 'approved' ? '人工通过' : '人工打回'}</Typography.Text>
                    <Typography.Text type="secondary">
                      {record.reviewerName} / {record.reviewedAt}
                    </Typography.Text>
                    {record.reason ? <Typography.Text>{record.reason}</Typography.Text> : null}
                    {record.comment ? <Typography.Text type="secondary">{record.comment}</Typography.Text> : null}
                  </Space>
                </List.Item>
              )}
              size="small"
            />
          </Card>

          <Card title="审计时间线">
            <Timeline
              items={currentDetail.auditTimeline.map((event) => ({
                children: (
                  <Space direction="vertical" size={2}>
                    <Typography.Text strong>{event.description}</Typography.Text>
                    <Typography.Text type="secondary">
                      {event.actorName} / {event.occurredAt}
                    </Typography.Text>
                  </Space>
                ),
              }))}
            />
          </Card>
        </div>
      ) : null}
    </main>
  )
}
