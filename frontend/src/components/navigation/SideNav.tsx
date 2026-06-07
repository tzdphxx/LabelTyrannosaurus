import { MenuFoldOutlined, MenuUnfoldOutlined } from '@ant-design/icons'
import { Button, Layout, Menu } from 'antd'
import { useNavigate } from 'react-router'
import { useRoleNavigation } from '../../hooks/useRoleNavigation'
import { useNavigationStore } from '../../stores/navigationStore'

const { Sider } = Layout

export function SideNav() {
  const navigate = useNavigate()
  const { activeKey, items } = useRoleNavigation()
  const collapsed = useNavigationStore((state) => state.collapsed)
  const setCollapsed = useNavigationStore((state) => state.setCollapsed)

  return (
    <Sider className="side-nav" width={232} collapsed={collapsed} trigger={null}>
      <div className="side-nav__tools">
        <Button
          aria-label={collapsed ? '展开侧边导航' : '收起侧边导航'}
          icon={collapsed ? <MenuUnfoldOutlined /> : <MenuFoldOutlined />}
          onClick={() => setCollapsed(!collapsed)}
          type="text"
        />
      </div>
      <Menu
        className="side-nav__menu"
        items={items.map((item) => ({
          key: item.key,
          icon: item.icon,
          label: item.label,
          onClick: () => navigate(item.path),
        }))}
        mode="inline"
        selectedKeys={activeKey ? [activeKey] : []}
      />
    </Sider>
  )
}

