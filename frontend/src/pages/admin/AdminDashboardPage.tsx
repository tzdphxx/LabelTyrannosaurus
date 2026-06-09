import { Line, Column } from '@ant-design/plots'
import { PlusOutlined, ReloadOutlined, TrophyOutlined } from '@ant-design/icons'
import {
  Alert,
  Button,
  Card,
  Col,
  Drawer,
  Empty,
  Form,
  Input,
  Progress,
  Row,
  Segmented,
  Space,
  Statistic,
  Table,
  Tag,
  Typography,
  message,
} from 'antd'
import { useEffect, useMemo, useState } from 'react'
import { ContentShell } from '../../components/page/ContentShell'
import { PageHeader } from '../../components/page/PageHeader'
import { adminUserService } from '../../services'
import { useAdminDashboardStore } from '../../stores/adminDashboardStore'
import type { AdminCreateReviewerRequest, AdminDashboardRange, AdminTopLabeler, AdminUserResponse } from '../../types/admin'
import {
  formatDashboardDateTime,
  formatDashboardMoney,
  formatDashboardRate,
} from '../../utils/dashboard'
import styles from './AdminDashboardPage.module.css'

const adminRangeOptions: Array<{ label: string, value: AdminDashboardRange }> = [
  { label: '7 天', value: '7d' },
  { label: '14 天', value: '14d' },
  { label: '30 天', value: '30d' },
]

const taskStatusLabels: Record<string, string> = {
  DRAFT: '草稿',
  PUBLISHED: '已发布',
  PAUSED: '已暂停',
  ENDED: '已结束',
}

function toDashboardPercent(value?: number | null) {
  const normalized = value ?? 0
  const percentage = normalized > 1 ? normalized : normalized * 100

  return Math.max(0, Math.min(100, Number(percentage.toFixed(1))))
}

function getRankClass(index: number) {
  if (index === 0) {
    return `${styles.rankBadge} ${styles.rankGold}`
  }

  if (index === 1) {
    return `${styles.rankBadge} ${styles.rankSilver}`
  }

  if (index === 2) {
    return `${styles.rankBadge} ${styles.rankBronze}`
  }

  return styles.rankBadge
}

export function AdminDashboardPage() {
  const [messageApi, contextHolder] = message.useMessage()
  const [form] = Form.useForm<AdminCreateReviewerRequest>()
  const data = useAdminDashboardStore((state) => state.data)
  const error = useAdminDashboardStore((state) => state.error)
  const isLoading = useAdminDashboardStore((state) => state.isLoading)
  const range = useAdminDashboardStore((state) => state.range)
  const setRange = useAdminDashboardStore((state) => state.setRange)
  const loadDashboard = useAdminDashboardStore((state) => state.loadDashboard)
  const [isCreateDrawerOpen, setIsCreateDrawerOpen] = useState(false)
  const [isCreatingReviewer, setIsCreatingReviewer] = useState(false)
  const [createdReviewer, setCreatedReviewer] = useState<AdminUserResponse | null>(null)

  useEffect(() => {
    const timer = window.setTimeout(() => {
      void loadDashboard()
    }, 0)

    return () => window.clearTimeout(timer)
  }, [loadDashboard])

  const trendData = useMemo(
    () =>
      (data?.trend ?? []).flatMap((point) => [
        { date: point.date, metric: '提交', value: point.submittedCount },
        { date: point.date, metric: '通过', value: point.approvedCount },
        { date: point.date, metric: '打回', value: point.rejectedCount },
      ]),
    [data?.trend],
  )
  const statusDistributionData = useMemo(
    () =>
      Object.entries(data?.taskStatusDistribution ?? {}).map(([status, value]) => ({
        status: taskStatusLabels[status] ?? status,
        value,
      })),
    [data?.taskStatusDistribution],
  )

  const openCreateReviewer = () => {
    form.resetFields()
    setIsCreateDrawerOpen(true)
  }

  const createReviewer = async () => {
    const values = await form.validateFields()
    setIsCreatingReviewer(true)

    try {
      const reviewer = await adminUserService.createReviewer({
        username: values.username.trim(),
        email: values.email.trim(),
        password: values.password,
      })
      setCreatedReviewer(reviewer)
      setIsCreateDrawerOpen(false)
      form.resetFields()
      messageApi.success('审核员账号已创建')
    } catch (createError) {
      messageApi.error(createError instanceof Error ? createError.message : '审核员账号创建失败')
    } finally {
      setIsCreatingReviewer(false)
    }
  }

  return (
    <main className={styles.page}>
      {contextHolder}
      <ContentShell className={styles.hero}>
        <PageHeader
          title="管理员数据看板"
          description={`平台任务、用户、审核积压和奖励总览。最后更新：${formatDashboardDateTime(data?.generatedAt)}`}
          extra={
            <>
              <Segmented
                options={adminRangeOptions}
                value={range}
                onChange={(value) => {
                  const nextRange = value as AdminDashboardRange
                  setRange(nextRange)
                  void loadDashboard(nextRange)
                }}
              />
              <Button icon={<ReloadOutlined />} loading={isLoading} onClick={() => void loadDashboard()}>
                刷新
              </Button>
              <Button icon={<PlusOutlined />} type="primary" onClick={openCreateReviewer}>
                创建审核员
              </Button>
            </>
          }
        />
      </ContentShell>

      {error ? <Alert message={error} showIcon type="error" /> : null}

      <Row gutter={[16, 16]}>
        <Col lg={6} md={12} xs={24}>
          <Card className={styles.statCard}>
            <Statistic loading={isLoading} title="活跃任务" value={data?.kpis.activeTaskCount ?? 0} />
          </Card>
        </Col>
        <Col lg={6} md={12} xs={24}>
          <Card className={`${styles.statCard} ${styles.statCardInfo}`}>
            <Statistic loading={isLoading} title="已领取" value={data?.kpis.claimedCount ?? 0} />
          </Card>
        </Col>
        <Col lg={6} md={12} xs={24}>
          <Card className={`${styles.statCard} ${styles.statCardSuccess}`}>
            <Statistic loading={isLoading} title="已提交" value={data?.kpis.submittedCount ?? 0} />
          </Card>
        </Col>
        <Col lg={6} md={12} xs={24}>
          <Card className={`${styles.statCard} ${styles.statCardWarning}`}>
            <Statistic loading={isLoading} title="待审核" value={data?.kpis.pendingReviewCount ?? 0} />
          </Card>
        </Col>
        <Col lg={6} md={12} xs={24}>
          <Card className={styles.statCard}>
            <Statistic loading={isLoading} title="平台用户" value={data?.userSummary.totalUserCount ?? 0} />
          </Card>
        </Col>
        <Col lg={6} md={12} xs={24}>
          <Card className={`${styles.statCard} ${styles.statCardSuccess}`}>
            <Statistic loading={isLoading} title="通过率" value={formatDashboardRate(data?.kpis.approvalRate)} />
          </Card>
        </Col>
        <Col lg={6} md={12} xs={24}>
          <Card className={`${styles.statCard} ${styles.statCardWarning}`}>
            <Statistic loading={isLoading} title="打回率" value={formatDashboardRate(data?.kpis.rejectionRate)} />
          </Card>
        </Col>
        <Col lg={6} md={12} xs={24}>
          <Card className={`${styles.statCard} ${styles.statCardAlert}`}>
            <Statistic loading={isLoading} prefix="¥" title="奖励金额" value={formatDashboardMoney(data?.kpis.rewardAmount)} />
          </Card>
        </Col>
      </Row>

      {createdReviewer ? (
        <Card className={styles.createdReviewerCard} title="最近创建的审核员">
          <Row gutter={[12, 12]}>
            <Col lg={6} md={12} xs={24}>
              <div className={styles.createdReviewerRow}>
                <span>用户 ID</span>
                <strong>{createdReviewer.userId}</strong>
              </div>
            </Col>
            <Col lg={6} md={12} xs={24}>
              <div className={styles.createdReviewerRow}>
                <span>用户名</span>
                <strong>{createdReviewer.username}</strong>
              </div>
            </Col>
            <Col lg={8} md={12} xs={24}>
              <div className={styles.createdReviewerRow}>
                <span>邮箱</span>
                <strong>{createdReviewer.email}</strong>
              </div>
            </Col>
            <Col lg={6} md={12} xs={24}>
              <Space wrap>
                <Tag color={createdReviewer.enabled ? 'green' : 'default'}>{createdReviewer.enabled ? '账号启用' : '账号停用'}</Tag>
                <Tag color={createdReviewer.loginEnabled ? 'green' : 'default'}>{createdReviewer.loginEnabled ? '允许登录' : '禁止登录'}</Tag>
                <Tag>{createdReviewer.role}</Tag>
              </Space>
            </Col>
          </Row>
        </Card>
      ) : null}

      <Row gutter={[16, 16]}>
        <Col lg={15} xs={24}>
          <Card className={styles.panelCard} title="平台提交趋势">
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
          <Card className={styles.panelCard} title="审核质量">
            <div className={styles.qualityGaugePanel}>
              <Progress
                format={() => formatDashboardRate(data?.kpis.approvalRate)}
                percent={toDashboardPercent(data?.kpis.approvalRate)}
                size={160}
                status="success"
                type="dashboard"
              />
              <Typography.Text strong>整体审核质量</Typography.Text>
              <Typography.Text className={styles.muted}>通过率用于衡量整体质量，打回率用于定位质量风险。</Typography.Text>
              <div className={styles.qualityGaugeSummary}>
                <div className={styles.qualityGaugeMetric}>
                  <span>审核通过</span>
                  <strong>{formatDashboardRate(data?.kpis.approvalRate)}</strong>
                </div>
                <div className={styles.qualityGaugeMetric}>
                  <span>审核打回</span>
                  <strong>{formatDashboardRate(data?.kpis.rejectionRate)}</strong>
                </div>
              </div>
            </div>
          </Card>
        </Col>
        <Col lg={16} xs={24}>
          <Card className={styles.tableCard} title="标注员排行">
            <div className={styles.rankingScroll}>
              <Table<AdminTopLabeler>
                className={styles.dataTable}
                columns={[
                  {
                    title: '排名',
                    width: 82,
                    render: (_, __, index) => (
                      <span className={getRankClass(index)}>
                        {index < 3 ? <TrophyOutlined /> : null}
                        {index + 1}
                      </span>
                    ),
                  },
                  {
                    title: '标注员',
                    dataIndex: 'displayName',
                    render: (_, labeler) => (
                      <Space direction="vertical" size={2}>
                        <Typography.Text strong>{labeler.displayName}</Typography.Text>
                        <Typography.Text type="secondary">ID {labeler.labelerId}</Typography.Text>
                      </Space>
                    ),
                  },
                  { title: '提交', dataIndex: 'submittedCount', width: 100 },
                  { title: '通过', dataIndex: 'approvedCount', width: 100 },
                  {
                    title: '奖励',
                    dataIndex: 'rewardAmount',
                    width: 120,
                    render: (value: number) => `¥${formatDashboardMoney(value)}`,
                  },
                ]}
                dataSource={data?.topLabelers ?? []}
                loading={isLoading}
                pagination={false}
                rowKey="labelerId"
                scroll={{ y: 320 }}
                size="small"
              />
            </div>
          </Card>
        </Col>
      </Row>

      <Drawer
        className={styles.reviewerDrawer}
        destroyOnHidden
        footer={
          <div className={styles.reviewerDrawerFooter}>
            <Typography.Text className={styles.muted}>账号创建后默认启用并允许登录。</Typography.Text>
            <Space>
              <Button onClick={() => setIsCreateDrawerOpen(false)}>取消</Button>
              <Button loading={isCreatingReviewer} type="primary" onClick={() => void createReviewer()}>
                创建审核员
              </Button>
            </Space>
          </div>
        }
        open={isCreateDrawerOpen}
        title="创建审核员账号"
        width="min(520px, 100vw)"
        onClose={() => setIsCreateDrawerOpen(false)}
      >
        <Form className={styles.reviewerForm} form={form} layout="vertical">
          <Form.Item label="用户名" name="username" rules={[{ required: true, message: '请填写用户名' }, { max: 64 }]}>
            <Input placeholder="reviewer01" />
          </Form.Item>
          <Form.Item
            label="邮箱"
            name="email"
            rules={[
              { required: true, message: '请填写邮箱' },
              { type: 'email', message: '请填写合法邮箱' },
              { max: 255 },
            ]}
          >
            <Input placeholder="reviewer01@example.com" />
          </Form.Item>
          <Form.Item
            label="初始密码"
            name="password"
            rules={[
              { required: true, message: '请填写初始密码' },
              { min: 8, message: '密码至少 8 位' },
              { max: 128, message: '密码不能超过 128 位' },
            ]}
          >
            <Input.Password autoComplete="new-password" placeholder="至少 8 位" />
          </Form.Item>
        </Form>
      </Drawer>
    </main>
  )
}
