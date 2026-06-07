import { AuditOutlined, FormOutlined, ProjectOutlined } from '@ant-design/icons'
import { Button, Card, Space, Typography } from 'antd'
import type { ReactNode } from 'react'
import { useNavigate } from 'react-router'
import { useAuthStore } from '../../stores/authStore'
import type { Role } from '../../types/auth'
import { getRoleHomePath, roleLabels } from '../../utils/roles'

const loginOptions: Array<{
  role: Role
  title: string
  description: string
  icon: ReactNode
}> = [
  {
    role: 'owner',
    title: '任务负责人',
    description: '创建任务、搭建模板、追踪质量与导出结果。',
    icon: <ProjectOutlined />,
  },
  {
    role: 'labeler',
    title: '标注员',
    description: '领取任务、在线作答、保存草稿并提交结果。',
    icon: <FormOutlined />,
  },
  {
    role: 'reviewer',
    title: '审核员',
    description: '查看待审队列、处理审核、回看原因与记录。',
    icon: <AuditOutlined />,
  },
]

export function LoginPage() {
  const navigate = useNavigate()
  const loginAsRole = useAuthStore((state) => state.loginAsRole)

  function handleLogin(role: Role) {
    loginAsRole(role)
    navigate(getRoleHomePath(role), { replace: true })
  }

  return (
    <main className="login-page">
      <section className="login-panel">
        <div className="login-panel__intro">
          <Typography.Text className="login-panel__eyebrow">LabelHub Foundation</Typography.Text>
          <Typography.Title className="login-panel__title">选择身份进入工作台</Typography.Title>
          <Typography.Paragraph className="login-panel__copy">
            P0 阶段先跑通登录、角色识别、角色路由和公共导航，为后续任务、标注、审核模块预留承载空间。
          </Typography.Paragraph>
        </div>

        <div className="login-options">
          {loginOptions.map((option) => (
            <Card className="login-card" key={option.role}>
              <Space className="login-card__content" direction="vertical" size={18}>
                <span className="login-card__icon">{option.icon}</span>
                <div>
                  <Typography.Title className="login-card__title" level={3}>
                    {option.title}
                  </Typography.Title>
                  <Typography.Paragraph className="login-card__copy">{option.description}</Typography.Paragraph>
                </div>
                <Button block onClick={() => handleLogin(option.role)} size="large" type="primary">
                  以{roleLabels[option.role]}身份进入
                </Button>
              </Space>
            </Card>
          ))}
        </div>
      </section>
    </main>
  )
}
