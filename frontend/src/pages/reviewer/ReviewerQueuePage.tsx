import { Alert, Button, Card, Input, Modal, Select, Space, Table, Tag, Typography, message } from 'antd'
import { EyeOutlined, ReloadOutlined } from '@ant-design/icons'
import { useEffect, useMemo, useState } from 'react'
import { useNavigate } from 'react-router'
import { ContentShell } from '../../components/page/ContentShell'
import { PageHeader } from '../../components/page/PageHeader'
import { useReviewStore } from '../../stores/reviewStore'
import type { ManualReviewStatus, ReviewQueueItem, ReviewRiskLevel } from '../../types/review'

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

const manualStatusLabels: Record<ManualReviewStatus, string> = {
  none: '无需人工',
  pending: '待复核',
  in_progress: '进行中',
  approved: '人工通过',
  rejected: '人工打回',
}

const manualStatusColors: Record<ManualReviewStatus, string> = {
  none: 'default',
  pending: 'processing',
  in_progress: 'warning',
  approved: 'success',
  rejected: 'error',
}

const riskOptions = [
  { label: '全部风险', value: 'all' },
  { label: '低风险', value: 'low' },
  { label: '中风险', value: 'medium' },
  { label: '高风险', value: 'high' },
]

const manualStatusOptions = [
  { label: '全部状态', value: 'all' },
  { label: '待复核', value: 'pending' },
  { label: '进行中', value: 'in_progress' },
  { label: '人工通过', value: 'approved' },
  { label: '人工打回', value: 'rejected' },
]

export function ReviewerQueuePage() {
  const navigate = useNavigate()
  const [messageApi, contextHolder] = message.useMessage()
  const [batchRejectOpen, setBatchRejectOpen] = useState(false)
  const [batchRejectReason, setBatchRejectReason] = useState('')
  const queue = useReviewStore((state) => state.queue)
  const filters = useReviewStore((state) => state.filters)
  const error = useReviewStore((state) => state.error)
  const isQueueLoading = useReviewStore((state) => state.isQueueLoading)
  const selectedReviewIds = useReviewStore((state) => state.selectedReviewIds)
  const setFilters = useReviewStore((state) => state.setFilters)
  const setSelectedReviewIds = useReviewStore((state) => state.setSelectedReviewIds)
  const loadQueue = useReviewStore((state) => state.loadQueue)
  const isBatchSubmitting = useReviewStore((state) => state.isBatchSubmitting)
  const submitBatchManualReviewAction = useReviewStore((state) => state.submitBatchManualReviewAction)

  useEffect(() => {
    void loadQueue()
  }, [loadQueue])

  const reloadWithFilter = (changes: Parameters<typeof setFilters>[0]) => {
    setFilters(changes)
    void loadQueue()
  }

  const selectedActionableIds = useMemo(
    () =>
      queue
        .filter((item) => selectedReviewIds.includes(item.id))
        .filter((item) => item.aiDecision === 'manual_review' && ['pending', 'in_progress'].includes(item.manualReviewStatus))
        .map((item) => item.id),
    [queue, selectedReviewIds],
  )

  const showBatchResult = (successCount: number, failedCount: number) => {
    if (failedCount > 0) {
      messageApi.warning(`批量处理完成：成功 ${successCount} 条，失败 ${failedCount} 条`)
    } else {
      messageApi.success(`批量处理成功：${successCount} 条`)
    }
  }

  const submitBatchPass = () => {
    if (selectedActionableIds.length === 0) {
      messageApi.error('请选择待复核记录')
      return
    }

    Modal.confirm({
      title: '批量人工通过',
      content: `确认通过 ${selectedActionableIds.length} 条人工复核记录吗？`,
      okText: '确认',
      cancelText: '取消',
      onOk: async () => {
        const result = await submitBatchManualReviewAction(selectedActionableIds, {
          reviewerId: 'user-reviewer',
          reviewerName: '审核员王敏',
          decision: 'approved',
          comment: '批量人工通过',
        })

        if (result) {
          showBatchResult(result.success.length, result.failed.length)
        }
      },
    })
  }

  const submitBatchReject = async () => {
    if (!batchRejectReason.trim()) {
      messageApi.error('批量打回必须填写原因')
      return
    }

    const result = await submitBatchManualReviewAction(selectedActionableIds, {
      reviewerId: 'user-reviewer',
      reviewerName: '审核员王敏',
      decision: 'rejected',
      reason: batchRejectReason.trim(),
      comment: '批量人工打回',
    })

    if (result) {
      showBatchResult(result.success.length, result.failed.length)
      setBatchRejectReason('')
      setBatchRejectOpen(false)
    }
  }

  return (
    <main className="reviewer-page">
      {contextHolder}
      <ContentShell>
        <PageHeader
          title="人工复核队列"
          description="仅展示 AI 判定为人工复核或 AI 异常降级的提交。AI 通过和 AI 打回不会进入该队列。"
          extra={
            <>
              <Button disabled={selectedActionableIds.length === 0} loading={isBatchSubmitting} onClick={submitBatchPass}>
                批量通过
              </Button>
              <Button
                danger
                disabled={selectedActionableIds.length === 0}
                loading={isBatchSubmitting}
                onClick={() => setBatchRejectOpen(true)}
              >
                批量打回
              </Button>
              <Button icon={<ReloadOutlined />} loading={isQueueLoading} onClick={() => void loadQueue()}>
                刷新
              </Button>
            </>
          }
        />
      </ContentShell>

      {error ? <Alert message={error} showIcon type="error" /> : null}

      <Card className="reviewer-table-card">
        <div className="owner-toolbar">
          <Input.Search
            allowClear
            className="owner-toolbar__search"
            placeholder="搜索任务、标注员或 AI 原因"
            value={filters.keyword}
            onChange={(event) => reloadWithFilter({ keyword: event.target.value })}
            onSearch={(keyword) => reloadWithFilter({ keyword })}
          />
          <Select
            className="owner-toolbar__select"
            options={riskOptions}
            value={filters.riskLevel}
            onChange={(riskLevel) => reloadWithFilter({ riskLevel })}
          />
          <Select
            className="owner-toolbar__select"
            options={manualStatusOptions}
            value={filters.manualStatus}
            onChange={(manualStatus) => reloadWithFilter({ manualStatus })}
          />
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
              render: () => <Tag color="processing">人工复核</Tag>,
            },
            {
              title: '风险',
              dataIndex: 'aiRiskLevel',
              width: 110,
              render: (value: ReviewRiskLevel) => <Tag color={riskLevelColors[value]}>{riskLevelLabels[value]}</Tag>,
            },
            {
              title: '人工状态',
              dataIndex: 'manualReviewStatus',
              width: 120,
              render: (value: ManualReviewStatus) => <Tag color={manualStatusColors[value]}>{manualStatusLabels[value]}</Tag>,
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
          dataSource={queue}
          loading={isQueueLoading}
          pagination={{ pageSize: 8 }}
          rowKey="id"
          rowSelection={{
            selectedRowKeys: selectedReviewIds,
            onChange: (keys) => setSelectedReviewIds(keys.map(String)),
            getCheckboxProps: (item) => ({
              disabled: item.aiDecision !== 'manual_review' || !['pending', 'in_progress'].includes(item.manualReviewStatus),
            }),
          }}
        />
      </Card>

      <Modal
        confirmLoading={isBatchSubmitting}
        okText="确认打回"
        open={batchRejectOpen}
        title="批量人工打回"
        onCancel={() => setBatchRejectOpen(false)}
        onOk={() => void submitBatchReject()}
      >
        <Space direction="vertical" size={12} className="reviewer-action-panel">
          <Typography.Text type="secondary">将打回 {selectedActionableIds.length} 条人工复核记录。</Typography.Text>
          <Input.TextArea
            rows={4}
            placeholder="填写统一打回原因"
            value={batchRejectReason}
            onChange={(event) => setBatchRejectReason(event.target.value)}
          />
        </Space>
      </Modal>
    </main>
  )
}
