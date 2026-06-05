import {
  Alert,
  Button,
  Card,
  Input,
  Modal,
  Progress,
  Select,
  Space,
  Table,
  Tag,
  Typography,
  message,
} from 'antd'
import { PlusOutlined, ReloadOutlined } from '@ant-design/icons'
import { useEffect } from 'react'
import { useNavigate } from 'react-router'
import { ContentShell } from '../../components/page/ContentShell'
import { PageHeader } from '../../components/page/PageHeader'
import { useOwnerTaskStore } from '../../stores/ownerTaskStore'
import type { OwnerTask, OwnerTaskStatus } from '../../types/task'
import {
  formatCount,
  getProgressPercent,
  ownerTaskStatusColors,
  ownerTaskStatusLabels,
} from '../../utils/ownerTasks'

const statusOptions = [
  { label: '全部状态', value: 'all' },
  { label: '草稿', value: 'draft' },
  { label: '已发布', value: 'published' },
  { label: '已暂停', value: 'paused' },
  { label: '已结束', value: 'ended' },
]

export function OwnerTasksPage() {
  const navigate = useNavigate()
  const [messageApi, contextHolder] = message.useMessage()
  const [modalApi, modalContextHolder] = Modal.useModal()
  const tasks = useOwnerTaskStore((state) => state.tasks)
  const filters = useOwnerTaskStore((state) => state.filters)
  const total = useOwnerTaskStore((state) => state.total)
  const error = useOwnerTaskStore((state) => state.error)
  const isListLoading = useOwnerTaskStore((state) => state.isListLoading)
  const isStatusSubmitting = useOwnerTaskStore((state) => state.isStatusSubmitting)
  const isDeleting = useOwnerTaskStore((state) => state.isDeleting)
  const setFilters = useOwnerTaskStore((state) => state.setFilters)
  const loadTasks = useOwnerTaskStore((state) => state.loadTasks)
  const publishTask = useOwnerTaskStore((state) => state.publishTask)
  const updateTaskStatus = useOwnerTaskStore((state) => state.updateTaskStatus)
  const deleteTask = useOwnerTaskStore((state) => state.deleteTask)

  useEffect(() => {
    void loadTasks()
  }, [loadTasks])

  const reloadWithFilter = (changes: Parameters<typeof setFilters>[0]) => {
    setFilters(changes)
    void loadTasks()
  }

  const confirmDeleteTask = (task: OwnerTask) => {
    modalApi.confirm({
      title: '删除草稿任务',
      content: `确认要删除「${task.title}」吗？删除后不可恢复。`,
      okText: '删除',
      okButtonProps: { danger: true },
      cancelText: '取消',
      onOk: async () => {
        const deleted = await deleteTask(task.id)

        if (deleted) {
          messageApi.success('草稿任务已删除')
        } else {
          messageApi.error('删除失败')
        }
      },
    })
  }

  const confirmStatusChange = (task: OwnerTask, status: OwnerTaskStatus, label: string) => {
    modalApi.confirm({
      title: `${label}任务`,
      content: `确认要${label}「${task.title}」吗？`,
      okText: '确认',
      cancelText: '取消',
      onOk: async () => {
        const updatedTask = await updateTaskStatus(task.id, status)

        if (updatedTask) {
          messageApi.success(`任务已${label}`)
        } else {
          messageApi.error('状态操作失败')
        }
      },
    })
  }

  const confirmPublishTask = (task: OwnerTask) => {
    modalApi.confirm({
      title: '发布任务',
      content: `确认要发布「${task.title}」吗？`,
      okText: '发布',
      cancelText: '取消',
      onOk: async () => {
        const publishedTask = await publishTask(task.id)

        if (publishedTask) {
          messageApi.success('任务已发布')
        } else {
          messageApi.error('发布失败')
        }
      },
    })
  }

  return (
    <main className="owner-page">
      {contextHolder}
      {modalContextHolder}
      <ContentShell>
        <PageHeader
          title="任务管理"
          description="查看 Owner 负责的任务、进度和当前状态。支持搜索、状态筛选、编辑入口和状态操作。"
          extra={
            <>
              <Button icon={<ReloadOutlined />} loading={isListLoading} onClick={() => void loadTasks()}>
                刷新
              </Button>
              <Button icon={<PlusOutlined />} type="primary" onClick={() => navigate('/app/owner/tasks/new')}>
                创建任务
              </Button>
            </>
          }
        />
      </ContentShell>

      {error ? <Alert message={error} showIcon type="error" /> : null}

      <Card className="owner-table-card">
        <div className="owner-toolbar">
          <Input.Search
            allowClear
            className="owner-toolbar__search"
            placeholder="搜索任务标题、描述或标签"
            value={filters.keyword}
            onChange={(event) => reloadWithFilter({ keyword: event.target.value })}
            onSearch={(keyword) => reloadWithFilter({ keyword })}
          />
          <Select
            className="owner-toolbar__select"
            options={statusOptions}
            value={filters.status}
            onChange={(status) => reloadWithFilter({ status })}
          />
        </div>

        <Table<OwnerTask>
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
              render: (status: OwnerTaskStatus) => <Tag color={ownerTaskStatusColors[status]}>{ownerTaskStatusLabels[status]}</Tag>,
            },
            {
              title: '模板',
              dataIndex: 'templateName',
              width: 180,
            },
            {
              title: '当前进度',
              width: 230,
              render: (_, task) => (
                <Space className="owner-table-progress" direction="vertical" size={4}>
                  <Progress percent={getProgressPercent(task.progress)} size="small" />
                  <Typography.Text type="secondary">
                    {formatCount(task.progress.completedItems)} / {formatCount(task.progress.totalItems)} 完成，待审核{' '}
                    {formatCount(task.progress.pendingReviewItems)}
                  </Typography.Text>
                </Space>
              ),
            },
            {
              title: '数据量',
              dataIndex: 'dataCount',
              width: 100,
              render: (value: number) => formatCount(value),
            },
            {
              title: '截止时间',
              dataIndex: 'deadline',
              width: 130,
            },
            {
              title: '操作',
              width: 260,
              render: (_, task) => (
                <Space wrap>
                  <Button size="small" type="link" onClick={() => navigate(`/app/owner/tasks/${task.id}/edit`)}>
                    编辑
                  </Button>
                  {task.status === 'draft' ? (
                    <Button loading={isStatusSubmitting} size="small" type="link" onClick={() => confirmPublishTask(task)}>
                      发布
                    </Button>
                  ) : null}
                  {task.status === 'draft' ? (
                    <Button danger loading={isDeleting} size="small" type="link" onClick={() => confirmDeleteTask(task)}>
                      删除
                    </Button>
                  ) : null}
                  {task.status === 'published' ? (
                    <Button
                      loading={isStatusSubmitting}
                      size="small"
                      type="link"
                      onClick={() => confirmStatusChange(task, 'paused', '暂停')}
                    >
                      暂停
                    </Button>
                  ) : null}
                  {task.status === 'paused' ? (
                    <Button
                      loading={isStatusSubmitting}
                      size="small"
                      type="link"
                      onClick={() => confirmStatusChange(task, 'published', '恢复')}
                    >
                      恢复
                    </Button>
                  ) : null}
                  {task.status !== 'ended' && task.status !== 'draft' ? (
                    <Button
                      danger
                      loading={isStatusSubmitting}
                      size="small"
                      type="link"
                      onClick={() => confirmStatusChange(task, 'ended', '结束')}
                    >
                      结束
                    </Button>
                  ) : null}
                </Space>
              ),
            },
          ]}
          dataSource={tasks}
          loading={isListLoading}
          pagination={{
            current: filters.page,
            pageSize: filters.pageSize,
            showSizeChanger: true,
            total,
            onChange: (page, pageSize) => reloadWithFilter({ page, pageSize }),
          }}
          rowKey="id"
        />
      </Card>
    </main>
  )
}
