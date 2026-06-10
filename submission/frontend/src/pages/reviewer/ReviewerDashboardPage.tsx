import { Alert, Button, Card, Col, Empty, List, Row, Segmented, Space, Statistic, Table, Tag, Typography } from 'antd'
import { Line } from '@ant-design/plots'
import { ReloadOutlined } from '@ant-design/icons'
import { useEffect, useMemo } from 'react'
import { useNavigate } from 'react-router'
import { ContentShell } from '../../components/page/ContentShell'
import { PageHeader } from '../../components/page/PageHeader'
import { useReviewerDashboardStore } from '../../stores/reviewerDashboardStore'
import type { ReviewerRecentReviewed } from '../../types/dashboard'
import {
  dashboardRangeOptions,
  formatDashboardDateTime,
  formatDashboardRate,
  getDashboardLevelColor,
} from '../../utils/dashboard'
import styles from './ReviewerPages.module.css'

export function ReviewerDashboardPage() {
  const navigate = useNavigate()
  const data = useReviewerDashboardStore((state) => state.data)
  const error = useReviewerDashboardStore((state) => state.error)
  const isLoading = useReviewerDashboardStore((state) => state.isLoading)
  const range = useReviewerDashboardStore((state) => state.range)
  const setRange = useReviewerDashboardStore((state) => state.setRange)
  const loadDashboard = useReviewerDashboardStore((state) => state.loadDashboard)

  useEffect(() => {
    void loadDashboard()
  }, [loadDashboard])

  const trendData = useMemo(
    () =>
      (data?.reviewTrend ?? []).flatMap((point) => [
        { date: point.date, metric: '审核', value: point.reviewedCount },
        { date: point.date, metric: '通过', value: point.approvedCount },
        { date: point.date, metric: '打回', value: point.rejectedCount },
      ]),
    [data?.reviewTrend],
  )
  const aiSummary = data?.aiReviewSummary

  return (
    <main className={`${styles.page} dashboard-page`}>
      <ContentShell className="dashboard-hero">
        <PageHeader
          title="审核员数据看板"
          description={`查看审核队列、AI 复核摘要和近期审核表现。最后更新：${formatDashboardDateTime(data?.generatedAt)}`}
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
          <Card className={styles.tableCard}>
            <Statistic loading={isLoading} title="待审核" value={data?.queueSummary.pendingCount ?? 0} />
          </Card>
        </Col>
        <Col lg={6} md={12} xs={24}>
          <Card className={styles.tableCard}>
            <Statistic loading={isLoading} title="逾期待审" value={data?.queueSummary.overduePendingCount ?? 0} />
          </Card>
        </Col>
        <Col lg={6} md={12} xs={24}>
          <Card className={styles.tableCard}>
            <Statistic loading={isLoading} title="需人工" value={data?.queueSummary.manualRequiredCount ?? 0} />
          </Card>
        </Col>
        <Col lg={6} md={12} xs={24}>
          <Card className={styles.tableCard}>
            <Statistic loading={isLoading} title="冲突处理" value={data?.queueSummary.conflictRequiredCount ?? 0} />
          </Card>
        </Col>
        <Col lg={6} md={12} xs={24}>
          <Card className={styles.tableCard}>
            <Statistic loading={isLoading} title="今日审核" value={data?.kpis.todayReviewedCount ?? 0} />
          </Card>
        </Col>
        <Col lg={6} md={12} xs={24}>
          <Card className={styles.tableCard}>
            <Statistic loading={isLoading} title="总通过" value={data?.kpis.totalApproved ?? 0} />
          </Card>
        </Col>
        <Col lg={6} md={12} xs={24}>
          <Card className={styles.tableCard}>
            <Statistic loading={isLoading} title="总打回" value={data?.kpis.totalRejected ?? 0} />
          </Card>
        </Col>
        <Col lg={6} md={12} xs={24}>
          <Card className={styles.tableCard}>
            <Statistic loading={isLoading} title="AI 关注" value={data?.kpis.aiAttentionCount ?? 0} />
          </Card>
        </Col>
      </Row>

      <Row gutter={[16, 16]}>
        <Col lg={15} xs={24}>
          <Card className={styles.tableCard} title="审核趋势">
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
          <Card className={styles.tableCard} title="AI 复核摘要">
            {aiSummary ? (
              <div className="dashboard-metric-stack">
                <div className="dashboard-metric-row">
                  <span>状态</span>
                  <strong>{aiSummary.status || '-'}</strong>
                </div>
                <div className="dashboard-metric-row">
                  <span>决策</span>
                  <strong>{aiSummary.decision || '-'}</strong>
                </div>
                <div className="dashboard-metric-row">
                  <span>均分</span>
                  <strong>{aiSummary.averageScore || '-'}</strong>
                </div>
                <div className="dashboard-metric-row">
                  <span>通过率</span>
                  <strong>{formatDashboardRate(data?.kpis.approvalRate)}</strong>
                </div>
                {aiSummary.riskFlags ? <Typography.Paragraph className="dashboard-note">{aiSummary.riskFlags}</Typography.Paragraph> : null}
                {aiSummary.suggestion ? <Typography.Paragraph className="dashboard-note">{aiSummary.suggestion}</Typography.Paragraph> : null}
                {aiSummary.degraded ? <Tag color="orange">降级</Tag> : null}
              </div>
            ) : (
              <Empty description={isLoading ? '正在加载 AI 摘要...' : '暂无 AI 摘要'} />
            )}
          </Card>
        </Col>
      </Row>

      <Row gutter={[16, 16]}>
        <Col lg={10} xs={24}>
          <Card className={styles.tableCard} title="关注项">
            <List
              dataSource={data?.attentionItems ?? []}
              locale={{ emptyText: isLoading ? '正在加载...' : '暂无关注项' }}
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
                        <Typography.Text strong>{item.taskTitle}</Typography.Text>
                      </Space>
                    }
                    description={item.description}
                  />
                </List.Item>
              )}
            />
          </Card>
        </Col>
        <Col lg={14} xs={24}>
          <Card className={styles.tableCard} title="最近审核">
            <Table<ReviewerRecentReviewed>
              columns={[
                {
                  title: '任务',
                  dataIndex: 'taskTitle',
                  render: (title: string, row) => (
                    <Space direction="vertical" size={2}>
                      <Typography.Text strong>{title}</Typography.Text>
                      <Typography.Text type="secondary">{row.labelerName}</Typography.Text>
                    </Space>
                  ),
                },
                {
                  title: '结果',
                  dataIndex: 'result',
                  width: 100,
                  render: (result: string) => <Tag color={result?.toLowerCase().includes('reject') ? 'orange' : 'green'}>{result || '-'}</Tag>,
                },
                {
                  title: '时间',
                  dataIndex: 'reviewedAt',
                  width: 150,
                  render: formatDashboardDateTime,
                },
              ]}
              dataSource={data?.recentReviewed ?? []}
              loading={isLoading}
              pagination={false}
              rowKey="reviewId"
              size="small"
            />
          </Card>
        </Col>
      </Row>
    </main>
  )
}
