import { Alert, Button, Card, Input, InputNumber, Modal, Progress, Select, Space, Table, Tag, Typography, message } from 'antd'
import { ArrowRightOutlined, ReloadOutlined } from '@ant-design/icons'
import { useEffect } from 'react'
import { useNavigate } from 'react-router'
import { ContentShell } from '../../components/page/ContentShell'
import { PageHeader } from '../../components/page/PageHeader'
import { useLabelingStore } from '../../stores/labelingStore'
import type { LabelerTaskStatus, LabelerTaskSummary } from '../../types/labeling'
import {
  getTaskProgressLabel,
  getTaskProgressPercent,
  labelerTaskStatusColors,
  labelerTaskStatusLabels,
} from '../../utils/labeling'

const statusOptions = [
  { label: 'All statuses', value: 'all' },
  { label: 'Available', value: 'available' },
  { label: 'Claimed', value: 'claimed' },
  { label: 'In progress', value: 'in_progress' },
  { label: 'Paused', value: 'paused' },
  { label: 'Returned', value: 'rejected' },
  { label: 'Ended', value: 'ended' },
]

function getActionLabel(status: LabelerTaskStatus) {
  if (status === 'available') {
    return 'Claim task'
  }

  if (status === 'claimed' || status === 'in_progress' || status === 'rejected') {
    return 'Open workbench'
  }

  if (status === 'paused') {
    return 'Task paused'
  }

  return 'View records'
}

export function LabelerMarketPage() {
  const navigate = useNavigate()
  const [messageApi, contextHolder] = message.useMessage()
  const [modal, modalContextHolder] = Modal.useModal()
  const marketTasks = useLabelingStore((state) => state.marketTasks)
  const marketTags = useLabelingStore((state) => state.marketTags)
  const filters = useLabelingStore((state) => state.filters)
  const error = useLabelingStore((state) => state.error)
  const isMarketLoading = useLabelingStore((state) => state.isMarketLoading)
  const isClaiming = useLabelingStore((state) => state.isClaiming)
  const setFilters = useLabelingStore((state) => state.setFilters)
  const loadMarket = useLabelingStore((state) => state.loadMarket)
  const claimTask = useLabelingStore((state) => state.claimTask)

  useEffect(() => {
    void loadMarket()
  }, [loadMarket])

  const reloadWithFilter = (changes: Parameters<typeof setFilters>[0]) => {
    setFilters(changes)
    void loadMarket()
  }

  const getMaxClaimQuantity = (task: LabelerTaskSummary) => {
    const availableLimit = Math.max(task.availableCount ?? Math.max(task.totalQuestions - task.completedQuestions, 1), 0)
    const quotaGrabLimit =
      task.strategy === 'QUOTA_GRAB' && typeof task.maxClaimsPerLabeler === 'number'
        ? Math.max(task.maxClaimsPerLabeler - (task.currentUserClaimedCount ?? 0), 0)
        : Number.POSITIVE_INFINITY

    return Math.max(Math.min(availableLimit, quotaGrabLimit), 0)
  }

  const openClaimQuantityModal = (task: LabelerTaskSummary) => {
    const maxQuantity = getMaxClaimQuantity(task)
    let quantity = maxQuantity > 0 ? 1 : 0

    if (maxQuantity <= 0) {
      messageApi.warning('No claimable items for this task')
      return
    }

    modal.confirm({
      title: 'Select claim quantity',
      content: (
        <Space direction="vertical" size={8}>
          <Typography.Text type="secondary">Max claimable items: {maxQuantity}</Typography.Text>
          <InputNumber
            autoFocus
            defaultValue={quantity}
            max={maxQuantity}
            min={1}
            precision={0}
            onChange={(value) => {
              quantity = Math.trunc(Number(value) || 1)
            }}
          />
        </Space>
      ),
      okText: 'Claim',
      cancelText: 'Cancel',
      onOk: async () => {
        const normalizedQuantity = Math.min(Math.max(quantity, 1), maxQuantity)
        const taskAfterClaim = await claimTask(task.id, { quantity: normalizedQuantity })

        if (!taskAfterClaim) {
          messageApi.error('Task claim failed')
          throw new Error('Claim task failed')
        }

        messageApi.success('Task claimed')
        navigate(`/app/labeler/workbench/${task.id}`)
      },
    })
  }

  const handleTaskAction = (task: LabelerTaskSummary) => {
    if (task.status === 'submitted' || task.status === 'approved') {
      navigate('/app/labeler/submissions')
      return
    }

    if (task.status === 'ended') {
      messageApi.info('This task has ended')
      return
    }

    if (task.status === 'paused') {
      messageApi.info('This task is paused')
      return
    }

    if (task.status === 'available') {
      openClaimQuantityModal(task)
      return
    }

    navigate(`/app/labeler/workbench/${task.id}`)
  }

  const tagOptions = [
    { label: 'All tags', value: 'all' },
    ...marketTags.map((tag) => ({
      label: tag,
      value: tag,
    })),
  ]

  return (
    <main className="labeler-page">
      {contextHolder}
      {modalContextHolder}
      <ContentShell className="labeler-hero">
        <PageHeader
          title="Task Market"
          description="Browse available labeling tasks, claim items, and continue work in the labeling workbench."
          extra={
            <Button icon={<ReloadOutlined />} loading={isMarketLoading} onClick={() => void loadMarket()}>
              Refresh
            </Button>
          }
        />
      </ContentShell>

      {error ? <Alert message={error} showIcon type="error" /> : null}

      <Card className="labeler-filter-card">
        <div className="labeler-toolbar">
          <Input.Search
            allowClear
            className="labeler-toolbar__search"
            placeholder="Search task title, description, or tag"
            value={filters.keyword}
            onChange={(event) => reloadWithFilter({ keyword: event.target.value })}
            onSearch={(keyword) => reloadWithFilter({ keyword })}
          />
          <Select
            className="labeler-toolbar__select"
            options={tagOptions}
            value={filters.tag}
            onChange={(tag) => reloadWithFilter({ tag })}
          />
          <Select
            className="labeler-toolbar__select"
            options={statusOptions}
            value={filters.status}
            onChange={(status) => reloadWithFilter({ status })}
          />
        </div>
      </Card>

      <Card className="labeler-table-card">
        <Table<LabelerTaskSummary>
          columns={[
            {
              title: 'Task',
              dataIndex: 'title',
              render: (_, task) => (
                <Space direction="vertical" size={4}>
                  <Typography.Text strong>{task.title}</Typography.Text>
                  <Typography.Text type="secondary">{task.description}</Typography.Text>
                  <Space size={4} wrap>
                    {task.tags.map((tag) => (
                      <Tag key={tag}>{tag}</Tag>
                    ))}
                  </Space>
                </Space>
              ),
            },
            {
              title: 'Status',
              dataIndex: 'status',
              width: 110,
              render: (status: LabelerTaskStatus) => <Tag color={labelerTaskStatusColors[status]}>{labelerTaskStatusLabels[status]}</Tag>,
            },
            {
              title: 'Progress',
              width: 220,
              render: (_, task) => (
                <Space className="labeler-table-progress" direction="vertical" size={4}>
                  <Progress percent={getTaskProgressPercent(task.completedQuestions, task.totalQuestions)} size="small" />
                  <Typography.Text type="secondary">
                    {getTaskProgressLabel(task.completedQuestions, task.totalQuestions)} items
                  </Typography.Text>
                </Space>
              ),
            },
            {
              title: 'Reward',
              dataIndex: 'rewardText',
              width: 120,
              render: (rewardText: string) => <Typography.Text className="labeler-reward-text">{rewardText}</Typography.Text>,
            },
            {
              title: 'Deadline',
              dataIndex: 'deadline',
              width: 130,
            },
            {
              title: 'Action',
              width: 150,
              render: (_, task) => (
                <Button
                  disabled={task.status === 'ended' || task.status === 'paused'}
                  icon={<ArrowRightOutlined />}
                  loading={isClaiming}
                  size="small"
                  type={task.status === 'available' ? 'primary' : 'default'}
                  onClick={() => handleTaskAction(task)}
                >
                  {getActionLabel(task.status)}
                </Button>
              ),
            },
          ]}
          dataSource={marketTasks}
          loading={isMarketLoading}
          pagination={false}
          rowKey="id"
        />
      </Card>
    </main>
  )
}
