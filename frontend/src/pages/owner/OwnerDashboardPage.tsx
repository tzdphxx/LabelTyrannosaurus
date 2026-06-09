import { Alert, Button, Card, Col, Empty, List, Progress, Row, Segmented, Space, Statistic, Table, Tag, Typography } from 'antd'
import { Line, Column } from '@ant-design/plots'
import { PlusOutlined, ReloadOutlined } from '@ant-design/icons'
import { useEffect, useMemo } from 'react'
import { useNavigate } from 'react-router'
import { ContentShell } from '../../components/page/ContentShell'
import { PageHeader } from '../../components/page/PageHeader'
import { useOwnerDashboardStore } from '../../stores/ownerDashboardStore'
import type { OwnerRecentTask } from '../../types/dashboard'
import {
  dashboardRangeOptions,
  formatDashboardCount,
  formatDashboardDateTime,
  formatDashboardMoney,
  formatDashboardRate,
  getDashboardLevelColor,
} from '../../utils/dashboard'
import styles from './OwnerPages.module.css'

const ownerStatusLabels: Record<string, string> = {
  DRAFT: '草稿',
  PUBLISHED: '已发布',
  PAUSED: '已暂停',
  ENDED: '已结束',
  draft: '草稿',
  published: '已发布',
  paused: '已暂停',
  ended: '已结束',
}

export function OwnerDashboardPage() {
  const navigate = useNavigate()
  const data = useOwnerDashboardStore((state) => state.data)
  const error = useOwnerDashboardStore((state) => state.error)
  const isLoading = useOwnerDashboardStore((state) => state.isLoading)
  const trendDays = useOwnerDashboardStore((state) => state.trendDays)
  const setTrendDays = useOwnerDashboardStore((state) => state.setTrendDays)
  const loadDashboard = useOwnerDashboardStore((state) => state.loadDashboard)

  useEffect(() => {
    void loadDashboard()
  }, [loadDashboard])

  const deliveryTrendData = useMemo(
    () =>
      (data?.deliveryTrend ?? []).flatMap((point) => [
        { date: point.date, metric: '领取', value: point.claimedCount },
        { date: point.date, metric: '提交', value: point.submittedCount },
        { date: point.date, metric: '通过', value: point.approvedCount },
      ]),
    [data?.deliveryTrend],
  )
  const statusDistributionData = useMemo(
    () =>
      Object.entries(data?.taskStatusDistribution ?? {}).map(([status, value]) => ({
        status: ownerStatusLabels[status] ?? status,
        value,
      })),
    [data?.taskStatusDistribution],
  )

  return (
    <main className={`${styles.page} dashboard-page`}>
      <ContentShell className={styles.hero}>
        <PageHeader
          title="任务负责人数据看板"
          description={`汇总自有任务 KPI、交付趋势、质量风险和近期任务。最后更新：${formatDashboardDateTime(data?.generatedAt)}`}
          extra={
            <>
              <Segmented
                options={dashboardRangeOptions}
                value={trendDays}
                onChange={(value) => {
                  const nextValue = Number(value)
                  setTrendDays(nextValue)
                  void loadDashboard(nextValue)
                }}
              />
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
        <Col lg={6} md={12} xs={24}>
          <Card className={styles.statCard}>
            <Statistic loading={isLoading} title="总任务数" value={data?.kpis.totalTaskCount ?? 0} />
          </Card>
        </Col>
        <Col lg={6} md={12} xs={24}>
          <Card className={`${styles.statCard} ${styles.statCardInfo}`}>
            <Statistic loading={isLoading} title="运行任务" value={data?.kpis.runningTaskCount ?? 0} />
          </Card>
        </Col>
        <Col lg={6} md={12} xs={24}>
          <Card className={`${styles.statCard} ${styles.statCardSuccess}`}>
            <Statistic loading={isLoading} title="已提交" value={data?.kpis.submittedItemCount ?? 0} />
          </Card>
        </Col>
        <Col lg={6} md={12} xs={24}>
          <Card className={`${styles.statCard} ${styles.statCardWarning}`}>
            <Statistic loading={isLoading} title="待审核" value={data?.kpis.pendingReviewCount ?? 0} />
          </Card>
        </Col>
        <Col lg={6} md={12} xs={24}>
          <Card className={styles.statCard}>
            <Statistic loading={isLoading} title="已领取" value={data?.kpis.claimedItemCount ?? 0} />
          </Card>
        </Col>
        <Col lg={6} md={12} xs={24}>
          <Card className={`${styles.statCard} ${styles.statCardSuccess}`}>
            <Statistic loading={isLoading} title="通过率" value={formatDashboardRate(data?.kpis.approvalRate)} />
          </Card>
        </Col>
        <Col lg={6} md={12} xs={24}>
          <Card className={`${styles.statCard} ${styles.statCardAlert}`}>
            <Statistic loading={isLoading} prefix="¥" title="奖励成本" value={formatDashboardMoney(data?.kpis.rewardCost)} />
          </Card>
        </Col>
        <Col lg={6} md={12} xs={24}>
          <Card className={styles.statCard}>
            <Statistic loading={isLoading} title="可见任务" value={data?.rewardSummary.visibleTaskCount ?? 0} />
          </Card>
        </Col>
      </Row>

      <Row gutter={[16, 16]}>
        <Col lg={15} xs={24}>
          <Card className={styles.panelCard} title="交付趋势">
            {deliveryTrendData.length ? (
              <Line
                data={deliveryTrendData}
                height={300}
                xField="date"
                yField="value"
                colorField="metric"
                axis={{ y: { title: false }, x: { title: false } }}
                legend={{ color: { position: 'bottom' } }}
                style={{ lineWidth: 2 }}
              />
            ) : (
              <Empty description={isLoading ? '正在加载趋势...' : '暂无趋势数据'} />
            )}
          </Card>
        </Col>
        <Col lg={9} xs={24}>
          <Card className={styles.panelCard} title="任务状态分布">
            {statusDistributionData.length ? (
              <Column
                data={statusDistributionData}
                height={300}
                xField="status"
                yField="value"
                axis={{ y: { title: false }, x: { title: false } }}
                style={{ radiusTopLeft: 6, radiusTopRight: 6 }}
              />
            ) : (
              <Empty description={isLoading ? '正在加载分布...' : '暂无分布数据'} />
            )}
          </Card>
        </Col>
      </Row>

      <Row gutter={[16, 16]}>
        <Col lg={8} xs={24}>
          <Card className={styles.panelCard} title="质量与奖励">
            <div className="dashboard-metric-stack">
              <div className="dashboard-metric-row">
                <span>审核通过</span>
                <strong>{formatDashboardCount(data?.qualitySummary.approvedCount)}</strong>
              </div>
              <div className="dashboard-metric-row">
                <span>审核打回</span>
                <strong>{formatDashboardCount(data?.qualitySummary.rejectedCount)}</strong>
              </div>
              <div className="dashboard-metric-row">
                <span>打回率</span>
                <strong>{formatDashboardRate(data?.qualitySummary.rejectionRate)}</strong>
              </div>
              <div className="dashboard-metric-row">
                <span>总奖励成本</span>
                <strong>¥{formatDashboardMoney(data?.rewardSummary.totalRewardCost)}</strong>
              </div>
            </div>
          </Card>
        </Col>
        <Col lg={8} xs={24}>
          <Card className={styles.panelCard} title="待关注任务">
            <List
              dataSource={data?.attentionTasks ?? []}
              locale={{ emptyText: isLoading ? '正在加载...' : '暂无待关注任务' }}
              renderItem={(item) => (
                <List.Item
                  actions={[
                    item.targetPath ? (
                      <Button key="open" type="link" onClick={() => navigate(item.targetPath)}>
                        查看
                      </Button>
                    ) : null,
                  ].filter(Boolean)}
                >
                  <List.Item.Meta
                    title={
                      <Space>
                        <Tag color={getDashboardLevelColor(item.level)}>{item.level}</Tag>
                        <Typography.Text strong>{item.title}</Typography.Text>
                      </Space>
                    }
                    description={item.description}
                  />
                </List.Item>
              )}
            />
          </Card>
        </Col>
        <Col lg={8} xs={24}>
          <Card className={styles.tableCard} title="最近任务">
            <Table<OwnerRecentTask>
              className={styles.dataTable}
              columns={[
                {
                  title: '任务',
                  dataIndex: 'title',
                  render: (_, task) => (
                    <Space direction="vertical" size={2}>
                      <Typography.Text strong>{task.title}</Typography.Text>
                      <Typography.Text type="secondary">{formatDashboardDateTime(task.updatedAt)}</Typography.Text>
                    </Space>
                  ),
                },
                {
                  title: '进度',
                  width: 120,
                  render: (_, task) => <Progress percent={Math.round((task.progressRate ?? 0) * 100)} size="small" />,
                },
                {
                  title: '待审',
                  dataIndex: 'pendingReviewCount',
                  width: 76,
                },
              ]}
              dataSource={data?.recentTasks ?? []}
              loading={isLoading}
              pagination={false}
              rowKey="taskId"
              size="small"
            />
          </Card>
        </Col>
      </Row>
    </main>
  )
}
