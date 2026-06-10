import { ReloadOutlined } from '@ant-design/icons'
import {
  Alert,
  Button,
  Card,
  Checkbox,
  Input,
  InputNumber,
  List,
  Space,
  Statistic,
  Table,
  Tabs,
  Tag,
  Typography,
} from 'antd'
import { useCallback, useEffect, useMemo, useState } from 'react'
import { ContentShell } from '../../components/page/ContentShell'
import { PageHeader } from '../../components/page/PageHeader'
import { adminReviewAssignmentService } from '../../services'
import type {
  AssignableReviewer,
  AssignableReviewerQuery,
  AssignableTask,
  AssignableTaskQuery,
  PageResponse,
  ReviewerProgress,
  ReviewerProgressQuery,
} from '../../types/adminReviewAssignment'
import styles from './AdminReviewAssignmentPage.module.css'

const statusLabels: Record<AssignableTask['status'], string> = {
  DRAFT: '草稿',
  PUBLISHED: '已发布',
  PAUSED: '已暂停',
  ENDED: '已结束',
}

const statusColors: Record<AssignableTask['status'], string> = {
  DRAFT: 'default',
  PUBLISHED: 'green',
  PAUSED: 'orange',
  ENDED: 'red',
}

const emptyPage = <T,>(): PageResponse<T> => ({
  items: [],
  page: 1,
  pageSize: 10,
  total: 0,
})

function formatDate(value: string | null) {
  return value ? value.replace('T', ' ').slice(0, 16) : '-'
}

function formatRate(value: number) {
  return `${Number(value ?? 0).toFixed(2)}%`
}

export function AdminReviewAssignmentPage() {
  const [taskQuery, setTaskQuery] = useState<AssignableTaskQuery>({
    page: 1,
    size: 10,
    keyword: '',
    taskId: null,
    reviewLevel: null,
    includeClaimed: false,
  })
  const [reviewerQuery, setReviewerQuery] = useState<AssignableReviewerQuery>({
    page: 1,
    size: 10,
    keyword: '',
    enabledOnly: true,
  })
  const [progressQuery, setProgressQuery] = useState<ReviewerProgressQuery>({
    keyword: '',
    enabledOnly: true,
  })
  const [tasksPage, setTasksPage] = useState<PageResponse<AssignableTask>>(emptyPage)
  const [reviewersPage, setReviewersPage] = useState<PageResponse<AssignableReviewer>>(emptyPage)
  const [reviewerProgress, setReviewerProgress] = useState<ReviewerProgress[]>([])
  const [tasksLoading, setTasksLoading] = useState(false)
  const [reviewersLoading, setReviewersLoading] = useState(false)
  const [progressLoading, setProgressLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)

  const loadTasks = useCallback(async () => {
    setTasksLoading(true)
    setError(null)

    try {
      setTasksPage(await adminReviewAssignmentService.listAssignableTasks(taskQuery))
    } catch (loadError) {
      setError(loadError instanceof Error ? loadError.message : '可分配任务加载失败')
    } finally {
      setTasksLoading(false)
    }
  }, [taskQuery])

  const loadReviewers = useCallback(async () => {
    setReviewersLoading(true)
    setError(null)

    try {
      setReviewersPage(await adminReviewAssignmentService.listAssignableReviewers(reviewerQuery))
    } catch (loadError) {
      setError(loadError instanceof Error ? loadError.message : '审核员列表加载失败')
    } finally {
      setReviewersLoading(false)
    }
  }, [reviewerQuery])

  const loadProgress = useCallback(async () => {
    setProgressLoading(true)
    setError(null)

    try {
      setReviewerProgress(await adminReviewAssignmentService.listReviewerProgress(progressQuery))
    } catch (loadError) {
      setError(loadError instanceof Error ? loadError.message : '审核员进度加载失败')
    } finally {
      setProgressLoading(false)
    }
  }, [progressQuery])

  useEffect(() => {
    void loadTasks()
  }, [loadTasks])

  useEffect(() => {
    void loadReviewers()
  }, [loadReviewers])

  useEffect(() => {
    void loadProgress()
  }, [loadProgress])

  const totals = useMemo(() => {
    const pendingTaskItems = tasksPage.items.reduce((sum, item) => sum + item.pendingCount, 0)
    const reviewerPending = reviewerProgress.reduce((sum, item) => sum + item.pendingCount, 0)
    const todayReviewed = reviewerProgress.reduce((sum, item) => sum + item.todayReviewedCount, 0)

    return { pendingTaskItems, reviewerPending, todayReviewed }
  }, [reviewerProgress, tasksPage.items])

  return (
    <main className={styles.page}>
      <ContentShell className={styles.hero}>
        <PageHeader
          title="审核分配"
          description="查看待终审任务池、可分配审核员和审核员工作状态。本页只展示查询结果，不执行任务分配。"
          extra={
            <Button
              icon={<ReloadOutlined />}
              loading={tasksLoading || reviewersLoading || progressLoading}
              onClick={() => {
                void loadTasks()
                void loadReviewers()
                void loadProgress()
              }}
            >
              刷新全部
            </Button>
          }
        />
      </ContentShell>

      {error ? <Alert message={error} showIcon type="error" /> : null}

      <div className={styles.summaryGrid}>
        <Card className={styles.summaryCard}>
          <Statistic title="当前任务待审量" value={totals.pendingTaskItems} />
        </Card>
        <Card className={styles.summaryCard}>
          <Statistic title="审核员待审负载" value={totals.reviewerPending} />
        </Card>
        <Card className={styles.summaryCard}>
          <Statistic title="今日已审" value={totals.todayReviewed} />
        </Card>
      </div>

      <div className={styles.workspace}>
        <Card className={styles.panel} title="可分配任务">
          <div className={styles.toolbar}>
            <Input.Search
              allowClear
              className={styles.search}
              placeholder="搜索任务标题或描述"
              value={taskQuery.keyword}
              onChange={(event) => setTaskQuery((query) => ({ ...query, keyword: event.target.value, page: 1 }))}
              onSearch={() => void loadTasks()}
            />
            <InputNumber
              className={styles.number}
              min={1}
              placeholder="任务 ID"
              precision={0}
              value={taskQuery.taskId}
              onChange={(taskId) => setTaskQuery((query) => ({ ...query, taskId, page: 1 }))}
            />
            <InputNumber
              className={styles.number}
              min={1}
              placeholder="审核级别"
              precision={0}
              value={taskQuery.reviewLevel}
              onChange={(reviewLevel) => setTaskQuery((query) => ({ ...query, reviewLevel, page: 1 }))}
            />
            <Checkbox
              checked={taskQuery.includeClaimed}
              onChange={(event) => setTaskQuery((query) => ({ ...query, includeClaimed: event.target.checked, page: 1 }))}
            >
              包含已认领
            </Checkbox>
          </div>

          <Table<AssignableTask>
            columns={[
              {
                title: '任务',
                dataIndex: 'title',
                render: (_, task) => (
                  <div className={styles.metaStack}>
                    <Typography.Text strong>{task.title}</Typography.Text>
                    <Typography.Text className={styles.muted}>ID {task.taskId}</Typography.Text>
                  </div>
                ),
              },
              {
                title: '状态',
                dataIndex: 'status',
                width: 96,
                render: (status: AssignableTask['status']) => <Tag color={statusColors[status]}>{statusLabels[status]}</Tag>,
              },
              {
                title: '级别',
                dataIndex: 'reviewLevel',
                width: 80,
                render: (value: number) => `L${value}`,
              },
              {
                title: '待审',
                dataIndex: 'pendingCount',
                width: 90,
              },
              {
                title: '认领状态',
                width: 150,
                render: (_, task) =>
                  task.claimed ? (
                    <Tag color="orange">{task.claimedReviewerName ?? `审核员 ${task.claimedReviewerId}`}</Tag>
                  ) : (
                    <Tag color={task.available ? 'green' : 'default'}>{task.available ? '可分配' : '不可分配'}</Tag>
                  ),
              },
              {
                title: '截止时间',
                dataIndex: 'deadlineAt',
                width: 150,
                render: formatDate,
              },
            ]}
            dataSource={tasksPage.items}
            loading={tasksLoading}
            pagination={{
              current: tasksPage.page,
              pageSize: tasksPage.pageSize,
              total: tasksPage.total,
              showSizeChanger: true,
              onChange: (page, size) => setTaskQuery((query) => ({ ...query, page, size })),
            }}
            rowKey={(task) => `${task.taskId}-${task.reviewLevel}`}
          />
        </Card>

        <Card className={styles.panel} title="审核员工作状态">
          <Tabs
            items={[
              {
                key: 'assignable',
                label: '可分配审核员',
                children: (
                  <>
                    <div className={styles.toolbar}>
                      <Input.Search
                        allowClear
                        className={styles.search}
                        placeholder="搜索用户名或邮箱"
                        value={reviewerQuery.keyword}
                        onChange={(event) => setReviewerQuery((query) => ({ ...query, keyword: event.target.value, page: 1 }))}
                        onSearch={() => void loadReviewers()}
                      />
                      <Checkbox
                        checked={reviewerQuery.enabledOnly}
                        onChange={(event) => setReviewerQuery((query) => ({ ...query, enabledOnly: event.target.checked, page: 1 }))}
                      >
                        仅启用
                      </Checkbox>
                    </div>
                    <Table<AssignableReviewer>
                      columns={[
                        {
                          title: '审核员',
                          dataIndex: 'username',
                          render: (_, reviewer) => (
                            <div className={styles.metaStack}>
                              <Typography.Text strong>{reviewer.username}</Typography.Text>
                              <Typography.Text className={styles.muted}>{reviewer.email}</Typography.Text>
                            </div>
                          ),
                        },
                        {
                          title: '负载',
                          width: 90,
                          render: (_, reviewer) => reviewer.pendingCount,
                        },
                        {
                          title: '今日',
                          dataIndex: 'todayReviewedCount',
                          width: 90,
                        },
                        {
                          title: '通过率',
                          dataIndex: 'approvalRate',
                          width: 100,
                          render: formatRate,
                        },
                      ]}
                      dataSource={reviewersPage.items}
                      loading={reviewersLoading}
                      pagination={{
                        current: reviewersPage.page,
                        pageSize: reviewersPage.pageSize,
                        total: reviewersPage.total,
                        showSizeChanger: true,
                        onChange: (page, size) => setReviewerQuery((query) => ({ ...query, page, size })),
                      }}
                      rowKey="reviewerId"
                      size="small"
                    />
                  </>
                ),
              },
              {
                key: 'progress',
                label: '进度',
                children: (
                  <>
                    <div className={styles.toolbar}>
                      <Input.Search
                        allowClear
                        className={styles.search}
                        placeholder="搜索用户名或邮箱"
                        value={progressQuery.keyword}
                        onChange={(event) => setProgressQuery((query) => ({ ...query, keyword: event.target.value }))}
                        onSearch={() => void loadProgress()}
                      />
                      <Checkbox
                        checked={progressQuery.enabledOnly}
                        onChange={(event) => setProgressQuery((query) => ({ ...query, enabledOnly: event.target.checked }))}
                      >
                        仅启用
                      </Checkbox>
                    </div>
                    <List
                      dataSource={reviewerProgress}
                      loading={progressLoading}
                      renderItem={(reviewer) => (
                        <List.Item>
                          <div className={styles.metaStack}>
                            <div className={styles.progressHeader}>
                              <Space direction="vertical" size={0}>
                                <Typography.Text strong>{reviewer.username}</Typography.Text>
                                <Typography.Text className={styles.muted}>{reviewer.email}</Typography.Text>
                              </Space>
                              <Tag color={reviewer.enabled && reviewer.loginEnabled ? 'green' : 'default'}>
                                {reviewer.enabled && reviewer.loginEnabled ? '可用' : '停用'}
                              </Tag>
                            </div>
                            <Space size="middle" wrap>
                              <Typography.Text>待审 {reviewer.pendingCount}</Typography.Text>
                              <Typography.Text>今日 {reviewer.todayReviewedCount}</Typography.Text>
                              <Typography.Text>历史 {reviewer.totalReviewedCount}</Typography.Text>
                              <Typography.Text>通过率 {formatRate(reviewer.approvalRate)}</Typography.Text>
                            </Space>
                            {reviewer.claimedTasks.length ? (
                              <div className={styles.claimedList}>
                                {reviewer.claimedTasks.map((task) => (
                                  <div className={styles.claimedItem} key={`${task.taskId}-${task.reviewLevel}`}>
                                    <Typography.Text>{task.title}</Typography.Text>
                                    <Typography.Text className={styles.muted}>
                                      L{task.reviewLevel} / 待审 {task.pendingCount}
                                    </Typography.Text>
                                  </div>
                                ))}
                              </div>
                            ) : null}
                          </div>
                        </List.Item>
                      )}
                    />
                  </>
                ),
              },
            ]}
          />
        </Card>
      </div>
    </main>
  )
}
