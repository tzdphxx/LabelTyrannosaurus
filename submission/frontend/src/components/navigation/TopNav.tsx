import { LogoutOutlined, UserOutlined } from '@ant-design/icons'
import { Button, Layout, Space, Typography } from 'antd'
import { useNavigate } from 'react-router'
import { useAuthStore } from '../../stores/authStore'
import { RoleBadge } from './RoleBadge'

const { Header } = Layout

export function TopNav() {
  const navigate = useNavigate()
  const currentUser = useAuthStore((state) => state.currentUser)
  const currentRole = useAuthStore((state) => state.currentRole)
  const logout = useAuthStore((state) => state.logout)

  function handleLogout() {
    logout()
    navigate('/login', { replace: true })
  }

  return (
    <Header className="top-nav">
      <div className="top-nav__brand">
        <Typography.Text className="top-nav__mark">LH</Typography.Text>
        <div>
          <Typography.Title level={4} className="top-nav__title">
            LabelHub
          </Typography.Title>
          <Typography.Text className="top-nav__subtitle">数据标注工作台</Typography.Text>
        </div>
      </div>

      <Space size="middle" align="center">
        {currentRole ? <RoleBadge role={currentRole} /> : null}
        <Space size={8} className="top-nav__user">
          <UserOutlined />
          <Typography.Text>{currentUser?.name ?? '未登录'}</Typography.Text>
        </Space>
        <Button icon={<LogoutOutlined />} onClick={handleLogout}>
          退出
        </Button>
      </Space>
    </Header>
  )
}
