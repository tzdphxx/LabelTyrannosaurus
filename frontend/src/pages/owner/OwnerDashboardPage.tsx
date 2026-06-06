import { Alert, Button, Card, Col, Progress, Row, Space, Statistic, Table, Tag, Typography } from 'antd'
import { PlusOutlined, ReloadOutlined } from '@ant-design/icons'
import { useEffect } from 'react'
import { useNavigate } from 'react-router'
import { ContentShell } from '../../components/page/ContentShell'
import { PageHeader } from '../../components/page/PageHeader'
import { useOwnerDashboardStore } from '../../stores/ownerDashboardStore'
import type { OwnerTask } from '../../types/task'
import { formatCount, getProgressPercent, ownerTaskStatusColors, ownerTaskStatusLabels } from '../../utils/ownerTasks'
import styles from './OwnerPages.module.css'

export function OwnerDashboardPage() {
  const navigate = useNavigate()
  const data = useOwnerDashboardStore((state) => state.data)
  const error = useOwnerDashboardStore((state) => state.error)
  const isLoading = useOwnerDashboardStore((state) => state.isLoading)
  const loadDashboard = useOwnerDashboardStore((state) => state.loadDashboard)

  useEffect(() => {
    void loadDashboard()
  }, [loadDashboard])

  const focusedTask = data?.focusedTask ?? null
  const focusedPercent = focusedTask ? getProgressPercent(focusedTask.progress) : 0

  return (
    <main className={styles.page}>
      <ContentShell className={styles.hero}>
        <PageHeader
          title="任务负责人工作台"
          description="集中查看任务概况、当前进度和需要处理的导入异常，P0 阶段先用 Mock 数据跑通 Owner 闭环。"
          extra={
            <>
              <Button icon={<ReloadOutlined />} loading={isLoading} onClick={() => void loadDashboard()}>
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

      <Row gutter={[16, 16]}>
        <Col lg={5} md={12} xs={24}>
          <Card className={styles.statCard}>
            <Statistic loading={isLoading} title="总任务数" value={data?.stats.totalTasks ?? 0} />
          </Card>
        </Col>
        <Col lg={5} md={12} xs={24}>
          <Card className={`${styles.statCard} ${styles.statCardInfo}`}>
            <Statistic loading={isLoading} title="草稿任务" value={data?.stats.draftTasks ?? 0} />
          </Card>
        </Col>
        <Col lg={5} md={12} xs={24}>
          <Card className={`${styles.statCard} ${styles.statCardSuccess}`}>
            <Statistic loading={isLoading} title="已发布" value={data?.stats.publishedTasks ?? 0} />
          </Card>
        </Col>
        <Col lg={5} md={12} xs={24}>
          <Card className={`${styles.statCard} ${styles.statCardWarning}`}>
            <Statistic loading={isLoading} title="进行中" value={data?.stats.runningTasks ?? 0} />
          </Card>
        </Col>
        <Col lg={4} md={12} xs={24}>
          <Card className={`${styles.statCard} ${styles.statCardAlert}`}>
            <Statistic loading={isLoading} title="导入异常" value={data?.stats.importIssueTasks ?? 0} />
          </Card>
        </Col>
      </Row>

      <Row gutter={[16, 16]}>
        <Col lg={10} xs={24}>
          <Card
            className={styles.panelCard}
            title="当前重点任务进度"
            extra={<Button type="link" onClick={() => navigate('/app/owner/tasks')}>任务管理</Button>}
          >
            {focusedTask ? (
              <Space className={styles.progressCardContent} direction="vertical" size={16}>
                <div>
                  <Typography.Title level={4}>{focusedTask.title}</Typography.Title>
                  <Typography.Text type="secondary">{focusedTask.description}</Typography.Text>
                </div>
                <Progress percent={focusedPercent} status={focusedTask.progress.abnormalItems > 0 ? 'exception' : 'active'} />
                <div className={styles.progressGrid}>
                  <Statistic title="总量" value={formatCount(focusedTask.progress.totalItems)} />
                  <Statistic title="已完成" value={formatCount(focusedTask.progress.completedItems)} />
                  <Statistic title="待审核" value={formatCount(focusedTask.progress.pendingReviewItems)} />
                  <Statistic title="异常" value={formatCount(focusedTask.progress.abnormalItems)} />
                </div>
              </Space>
            ) : (
              <Typography.Text type="secondary">暂无重点任务</Typography.Text>
            )}
          </Card>
        </Col>
        <Col lg={14} xs={24}>
          <Card className={styles.tableCard} title="最近任务">
            <Table<OwnerTask>
              className={styles.dataTable}
              columns={[
                {
                  title: '任务',
                  dataIndex: 'title',
                  render: (_, task) => (
                    <Space direction="vertical" size={2}>
                      <Typography.Text strong>{task.title}</Typography.Text>
                      <Typography.Text type="secondary">{task.templateName}</Typography.Text>
                    </Space>
                  ),
                },
                {
                  title: '状态',
                  dataIndex: 'status',
                  width: 96,
                  render: (status: OwnerTask['status']) => <Tag color={ownerTaskStatusColors[status]}>{ownerTaskStatusLabels[status]}</Tag>,
                },
                {
                  title: '完成率',
                  width: 150,
                  render: (_, task) => <Progress percent={getProgressPercent(task.progress)} size="small" />,
                },
                {
                  title: '操作',
                  width: 100,
                  render: (_, task) => (
                    <Button type="link" onClick={() => navigate(`/app/owner/tasks/${task.id}/edit`)}>
                      查看
                    </Button>
                  ),
                },
              ]}
              dataSource={data?.recentTasks ?? []}
              loading={isLoading}
              pagination={false}
              rowKey="id"
              size="middle"
            />
          </Card>
        </Col>
      </Row>
    </main>
  )
}
