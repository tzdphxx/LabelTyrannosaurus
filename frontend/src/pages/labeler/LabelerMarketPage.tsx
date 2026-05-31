import { Alert, Button, Card, Input, Progress, Select, Space, Table, Tag, Typography, message } from 'antd'
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
  { label: '全部状态', value: 'all' },
  { label: '可领取', value: 'available' },
  { label: '已领取', value: 'claimed' },
  { label: '进行中', value: 'in_progress' },
  { label: '待修改', value: 'rejected' },
  { label: '已结束', value: 'ended' },
]

function getActionLabel(status: LabelerTaskStatus) {
  if (status === 'available') {
    return '领取任务'
  }

  if (status === 'claimed' || status === 'in_progress' || status === 'rejected') {
    return '进入工作台'
  }

  return '查看记录'
}

export function LabelerMarketPage() {
  const navigate = useNavigate()
  const [messageApi, contextHolder] = message.useMessage()
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

  const handleTaskAction = async (taskId: string, status: LabelerTaskStatus) => {
    if (status === 'submitted' || status === 'approved') {
      navigate('/app/labeler/submissions')
      return
    }

    if (status === 'ended') {
      messageApi.info('该任务已结束，暂不能进入标注。')
      return
    }

    if (status === 'available') {
      const task = await claimTask(taskId)

      if (!task) {
        messageApi.error('任务领取失败')
        return
      }

      messageApi.success('任务已领取')
    }

    navigate(`/app/labeler/workbench/${taskId}`)
  }

  const tagOptions = [
    { label: '全部标签', value: 'all' },
    ...marketTags.map((tag) => ({
      label: tag,
      value: tag,
    })),
  ]

  return (
    <main className="labeler-page">
      {contextHolder}
      <ContentShell className="labeler-hero">
        <PageHeader
          title="任务广场"
          description="筛选可参与的标注任务，领取后进入工作台继续作答。P1 覆盖搜索、标签筛选、状态筛选和领取流程。"
          extra={
            <Button icon={<ReloadOutlined />} loading={isMarketLoading} onClick={() => void loadMarket()}>
              刷新
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
            placeholder="搜索任务标题、描述或标签"
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
              title: '任务',
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
              title: '状态',
              dataIndex: 'status',
              width: 110,
              render: (status: LabelerTaskStatus) => <Tag color={labelerTaskStatusColors[status]}>{labelerTaskStatusLabels[status]}</Tag>,
            },
            {
              title: '模板',
              dataIndex: 'templateName',
              width: 180,
            },
            {
              title: '进度',
              width: 220,
              render: (_, task) => (
                <Space className="labeler-table-progress" direction="vertical" size={4}>
                  <Progress percent={getTaskProgressPercent(task.completedQuestions, task.totalQuestions)} size="small" />
                  <Typography.Text type="secondary">
                    {getTaskProgressLabel(task.completedQuestions, task.totalQuestions)} 题
                  </Typography.Text>
                </Space>
              ),
            },
            {
              title: '报酬',
              dataIndex: 'rewardText',
              width: 120,
              render: (rewardText: string) => <Typography.Text className="labeler-reward-text">{rewardText}</Typography.Text>,
            },
            {
              title: '截止时间',
              dataIndex: 'deadline',
              width: 130,
            },
            {
              title: '操作',
              width: 150,
              render: (_, task) => (
                <Button
                  disabled={task.status === 'ended'}
                  icon={<ArrowRightOutlined />}
                  loading={isClaiming}
                  size="small"
                  type={task.status === 'available' ? 'primary' : 'default'}
                  onClick={() => void handleTaskAction(task.id, task.status)}
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
