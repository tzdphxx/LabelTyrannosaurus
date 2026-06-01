import { Card, Col, Row, Statistic } from 'antd'
import { RoleBadge } from '../../components/navigation/RoleBadge'
import { ContentShell } from '../../components/page/ContentShell'
import { PageHeader } from '../../components/page/PageHeader'
import type { Role } from '../../types/auth'
import { roleLabels } from '../../utils/roles'
import { useAuthStore } from '../../stores/authStore'

const roleHomeContent: Record<
  Role,
  {
    headline: string
    summary: string
    metrics: Array<{ label: string; value: string }>
    nextSteps: string[]
  }
> = {
  OWNER: {
    headline: '任务负责人工作台',
    summary: '后续承载任务创建、模板搭建、发布分发、质量追踪和导出审计。',
    metrics: [
      { label: '任务管理', value: '预留' },
      { label: '模板管理', value: '预留' },
      { label: '导出审计', value: '预留' },
    ],
    nextSteps: ['创建任务入口', '模板搭建器入口', '发布与导出状态入口'],
  },
  LABELER: {
    headline: '标注员工作台',
    summary: '后续承载任务领取、Schema 表单作答、草稿保存、提交和打回修改。',
    metrics: [
      { label: '任务广场', value: '预留' },
      { label: '标注工作台', value: '预留' },
      { label: '我的提交', value: '预留' },
    ],
    nextSteps: ['任务领取入口', '标注工作台入口', '提交历史入口'],
  },
  REVIEWER: {
    headline: '审核员工作台',
    summary: '后续承载 AI 人工复核队列、审核详情、AI 审核结果回看和审核历史。',
    metrics: [
      { label: '人工复核队列', value: 'P0' },
      { label: 'AI 审核结果', value: 'P0' },
      { label: '审核历史', value: '预留' },
    ],
    nextSteps: ['人工复核队列入口', 'AI 审核结果入口', '人工审核记录入口'],
  },
}

interface RoleHomePageProps {
  role: Role
}

export function RoleHomePage({ role }: RoleHomePageProps) {
  console.log('RoleHomePage', role)
  const currentUser = useAuthStore((state) => state.currentUser)
  const content = roleHomeContent[role]

  return (
    <main className="role-home">
      <ContentShell>
        <PageHeader
          description={content.summary}
          meta={<RoleBadge role={role} />}
          title={content.headline}
          extra={`当前用户：${currentUser?.name ?? '未登录'} / ${currentUser?.title ?? roleLabels[role]}`}
        />
      </ContentShell>

      <Row gutter={[16, 16]}>
        {content.metrics.map((metric) => (
          <Col key={metric.label} lg={8} md={8} sm={24} xs={24}>
            <Card className="metric-card">
              <Statistic title={metric.label} value={metric.value} />
            </Card>
          </Col>
        ))}
      </Row>

      <Card className="next-card" title="后续业务入口">
        <div className="next-card__grid">
          {content.nextSteps.map((item) => (
            <div className="next-card__item" key={item}>
              {item}
            </div>
          ))}
        </div>
      </Card>
    </main>
  )
}
