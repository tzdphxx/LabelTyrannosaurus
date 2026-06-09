import { Alert, Button, Card, Col, Empty, List, Row, Segmented, Space, Statistic, Table, Tag, Typography } from 'antd'
import { Column, Line } from '@ant-design/plots'
import { ReloadOutlined } from '@ant-design/icons'
import { useEffect, useMemo } from 'react'
import { useNavigate } from 'react-router'
import { ContentShell } from '../../components/page/ContentShell'
import { PageHeader } from '../../components/page/PageHeader'
import { useLabelerDashboardStore } from '../../stores/labelerDashboardStore'
import type { LabelerTaskContribution } from '../../types/dashboard'
import {
  dashboardRangeOptions,
  formatDashboardCount,
  formatDashboardDateTime,
  formatDashboardMoney,
  formatDashboardRate,
  getDashboardLevelColor,
} from '../../utils/dashboard'

export function LabelerDashboardPage() {
  const navigate = useNavigate()
  const data = useLabelerDashboardStore((state) => state.data)
  const error = useLabelerDashboardStore((state) => state.error)
  const isLoading = useLabelerDashboardStore((state) => state.isLoading)
  const range = useLabelerDashboardStore((state) => state.range)
  const setRange = useLabelerDashboardStore((state) => state.setRange)
  const loadDashboard = useLabelerDashboardStore((state) => state.loadDashboard)

  useEffect(() => {
    void loadDashboard()
  }, [loadDashboard])

  const trendData = useMemo(
    () =>
      (data?.contributionTrend ?? []).flatMap((point) => [
        { date: point.date, metric: '提交', value: point.submittedCount },
        { date: point.date, metric: '通过', value: point.approvedCount },
        { date: point.date, metric: '奖励', value: point.reward },
      ]),
    [data?.contributionTrend],
  )
  const contributionData = useMemo(
    () =>
      (data?.taskContributions ?? []).map((task) => ({
        taskTitle: task.taskTitle,
        value: task.totalReward,
      })),
    [data?.taskContributions],
  )

  return (
    <main className="labeler-page dashboard-page">
      <ContentShell className="labeler-hero">
        <PageHeader
          title="标注员数据看板"
          description={`查看领取、提交、贡献和奖励数据。最后更新：${formatDashboardDateTime(data?.generatedAt)}`}
          extra={
            <>
              <Segmented
                options={dashboardRangeOptions}
                value={range}
                onChange={(value) => {
                  const nextValue = Number(value)
                  setRange(nextValue)
                  void loadDashboard(nextValue)
                }}
              />
              <Button icon={<ReloadOutlined />} loading={isLoading} onClick={() => void loadDashboard()}>
                刷新
              </Button>
            </>
          }
        />
      </ContentShell>

      {error ? <Alert message={error} showIcon type="error" /> : null}

      <Row gutter={[16, 16]}>
        <Col lg={6} md={12} xs={24}>
          <Card className="labeler-stat-card">
            <Statistic loading={isLoading} title="已领取" value={data?.kpis.claimedCount ?? 0} />
          </Card>
        </Col>
        <Col lg={6} md={12} xs={24}>
          <Card className="labeler-stat-card">
            <Statistic loading={isLoading} title="已提交" value={data?.kpis.submittedCount ?? 0} />
          </Card>
        </Col>
        <Col lg={6} md={12} xs={24}>
          <Card className="labeler-stat-card">
            <Statistic loading={isLoading} title="已通过" value={data?.kpis.approvedCount ?? 0} />
          </Card>
        </Col>
        <Col lg={6} md={12} xs={24}>
          <Card className="labeler-stat-card">
            <Statistic loading={isLoading} title="通过率" value={formatDashboardRate(data?.kpis.approvalRate)} />
          </Card>
        </Col>
        <Col lg={6} md={12} xs={24}>
          <Card className="labeler-stat-card">
            <Statistic loading={isLoading} title="已打回" value={data?.kpis.rejectedCount ?? 0} />
          </Card>
        </Col>
        <Col lg={6} md={12} xs={24}>
          <Card className="labeler-stat-card">
            <Statistic loading={isLoading} title="返工数" value={data?.kpis.reworkCount ?? 0} />
          </Card>
        </Col>
        <Col lg={6} md={12} xs={24}>
          <Card className="labeler-stat-card">
            <Statistic loading={isLoading} prefix="¥" title="本期奖励" value={formatDashboardMoney(data?.kpis.periodReward)} />
          </Card>
        </Col>
        <Col lg={6} md={12} xs={24}>
          <Card className="labeler-stat-card">
            <Statistic loading={isLoading} prefix="¥" title="累计奖励" value={formatDashboardMoney(data?.kpis.totalReward)} />
          </Card>
        </Col>
      </Row>

      <Row gutter={[16, 16]}>
        <Col lg={15} xs={24}>
          <Card className="labeler-table-card" title="贡献趋势">
            {trendData.length ? (
              <Line
                data={trendData}
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
          <Card className="labeler-table-card" title="任务奖励贡献">
            {contributionData.length ? (
              <Column
                data={contributionData}
                height={300}
                xField="taskTitle"
                yField="value"
                axis={{ y: { title: false }, x: { title: false, labelTransform: 'rotate(25)' } }}
                style={{ radiusTopLeft: 6, radiusTopRight: 6 }}
              />
            ) : (
              <Empty description={isLoading ? '正在加载贡献...' : '暂无贡献数据'} />
            )}
          </Card>
        </Col>
      </Row>

      <Row gutter={[16, 16]}>
        <Col lg={7} xs={24}>
          <Card className="labeler-table-card" title="待办摘要">
            <div className="dashboard-metric-stack">
              <div className="dashboard-metric-row">
                <span>已领取未提交</span>
                <strong>{formatDashboardCount(data?.todoSummary.claimedNotSubmittedCount)}</strong>
              </div>
              <div className="dashboard-metric-row">
                <span>打回待修正</span>
                <strong>{formatDashboardCount(data?.todoSummary.rejectedNeedFixCount)}</strong>
              </div>
              <div className="dashboard-metric-row">
                <span>可继续任务</span>
                <strong>{formatDashboardCount(data?.todoSummary.continuableTaskCount)}</strong>
              </div>
            </div>
          </Card>
        </Col>
        <Col lg={8} xs={24}>
          <Card className="labeler-table-card" title="提醒">
            <List
              dataSource={data?.alerts ?? []}
              locale={{ emptyText: isLoading ? '正在加载...' : '暂无提醒' }}
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
        <Col lg={9} xs={24}>
          <Card className="labeler-table-card" title="任务贡献">
            <Table<LabelerTaskContribution>
              columns={[
                {
                  title: '任务',
                  dataIndex: 'taskTitle',
                  render: (title: string) => <Typography.Text strong>{title}</Typography.Text>,
                },
                {
                  title: '提交',
                  dataIndex: 'submittedCount',
                  width: 72,
                },
                {
                  title: '通过',
                  dataIndex: 'approvedCount',
                  width: 72,
                },
                {
                  title: '奖励',
                  dataIndex: 'totalReward',
                  width: 92,
                  render: (value: number) => `¥${formatDashboardMoney(value)}`,
                },
                {
                  title: '',
                  width: 76,
                  render: (_, task) =>
                    task.targetPath ? (
                      <Button type="link" onClick={() => navigate(task.targetPath)}>
                        查看
                      </Button>
                    ) : null,
                },
              ]}
              dataSource={data?.taskContributions ?? []}
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
