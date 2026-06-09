import { Layout } from 'antd'
import { Outlet } from 'react-router'
import { BreadcrumbNav } from '../../components/navigation/BreadcrumbNav'
import { SideNav } from '../../components/navigation/SideNav'
import { TopNav } from '../../components/navigation/TopNav'

const { Content } = Layout

export function AppLayout() {
  return (
    <Layout className="app-shell">
      <TopNav />
      <BreadcrumbNav />
      <Layout className="app-shell__body">
        <SideNav />
        <Content className="app-shell__content">
          <Outlet />
        </Content>
      </Layout>
    </Layout>
  )
}
