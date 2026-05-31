import { BrowserRouter, Navigate, Route, Routes } from 'react-router'
import { StatePlaceholder } from '../components/states/StatePlaceholder'
import { LoginPage } from '../pages/auth/LoginPage'
import { LabelerMarketPage } from '../pages/labeler/LabelerMarketPage'
import { LabelerSubmissionsPage } from '../pages/labeler/LabelerSubmissionsPage'
import { LabelerWorkbenchPage } from '../pages/labeler/LabelerWorkbenchPage'
import { OwnerDashboardPage } from '../pages/owner/OwnerDashboardPage'
import { OwnerTaskEditorPage } from '../pages/owner/OwnerTaskEditorPage'
import { OwnerTasksPage } from '../pages/owner/OwnerTasksPage'
import { OwnerTemplateDesignerPage } from '../pages/owner/templates/OwnerTemplateDesignerPage'
import { OwnerTemplatesPage } from '../pages/owner/templates/OwnerTemplatesPage'
import { RoleHomePage } from '../pages/roles/RoleHomePage'
import { useAuthStore } from '../stores/authStore'
import { AppLayout } from './layout/AppLayout'
import { PublicOnlyRoute } from './guards/PublicOnlyRoute'
import { RequireAuth } from './guards/RequireAuth'

function EntryRedirect() {
  const isAuthenticated = useAuthStore((state) => state.isAuthenticated)
  const roleHomePath = useAuthStore((state) => state.getRoleHomePath())

  return <Navigate replace to={isAuthenticated ? roleHomePath : '/login'} />
}

export function AppRouter() {
  return (
    <BrowserRouter>
      <Routes>
        <Route index element={<EntryRedirect />} />
        <Route
          element={
            <PublicOnlyRoute>
              <LoginPage />
            </PublicOnlyRoute>
          }
          path="/login"
        />
        <Route
          element={
            <RequireAuth>
              <AppLayout />
            </RequireAuth>
          }
          path="/app"
        >
          <Route index element={<EntryRedirect />} />
          <Route path="owner">
            <Route index element={<OwnerDashboardPage />} />
            <Route element={<OwnerTasksPage />} path="tasks" />
            <Route element={<OwnerTaskEditorPage />} path="tasks/new" />
            <Route element={<OwnerTaskEditorPage />} path="tasks/:taskId/edit" />
            <Route element={<OwnerTemplatesPage />} path="templates" />
            <Route element={<OwnerTemplateDesignerPage />} path="templates/:templateId/designer" />
            <Route
              element={<StatePlaceholder status="empty" message="该入口已预留，后续阶段接入业务页面。" />}
              path="*"
            />
          </Route>
          <Route path="labeler">
            <Route index element={<RoleHomePage role="labeler" />} />
            <Route element={<LabelerMarketPage />} path="market" />
            <Route element={<LabelerWorkbenchPage />} path="workbench/:taskId" />
            <Route element={<LabelerSubmissionsPage />} path="submissions" />
            <Route
              element={<StatePlaceholder status="empty" message="该入口已预留，后续阶段接入业务页面。" />}
              path="*"
            />
          </Route>
          <Route element={<RoleHomePage role="reviewer" />} path="reviewer" />
          <Route
            element={<StatePlaceholder status="empty" message="该入口已预留，后续阶段接入业务页面。" />}
            path="*"
          />
        </Route>
        <Route element={<Navigate replace to="/login" />} path="*" />
      </Routes>
    </BrowserRouter>
  )
}
