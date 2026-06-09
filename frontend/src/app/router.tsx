import { lazy, Suspense } from 'react'
import { BrowserRouter, Navigate, Route, Routes } from 'react-router'
import { StatePlaceholder } from '../components/states/StatePlaceholder'
import { useAuthStore } from '../stores/authStore'
import { AppLayout } from './layout/AppLayout'
import { PublicOnlyRoute } from './guards/PublicOnlyRoute'
import { RequireAuth } from './guards/RequireAuth'

const LoginPage = lazy(() => import('../pages/auth/LoginPage').then((module) => ({ default: module.LoginPage })))
const AdminDashboardPage = lazy(() =>
  import('../pages/admin/AdminDashboardPage').then((module) => ({ default: module.AdminDashboardPage })),
)
const AdminLlmProviderPage = lazy(() =>
  import('../pages/admin/AdminLlmProviderPage').then((module) => ({ default: module.AdminLlmProviderPage })),
)
const AdminReviewAssignmentPage = lazy(() =>
  import('../pages/admin/AdminReviewAssignmentPage').then((module) => ({ default: module.AdminReviewAssignmentPage })),
)
const LabelerDashboardPage = lazy(() =>
  import('../pages/labeler/LabelerDashboardPage').then((module) => ({ default: module.LabelerDashboardPage })),
)
const LabelerMarketPage = lazy(() =>
  import('../pages/labeler/LabelerMarketPage').then((module) => ({ default: module.LabelerMarketPage })),
)
const LabelerSubmissionsPage = lazy(() =>
  import('../pages/labeler/LabelerSubmissionsPage').then((module) => ({ default: module.LabelerSubmissionsPage })),
)
const LabelerWorkbenchPage = lazy(() =>
  import('../pages/labeler/LabelerWorkbenchPage').then((module) => ({ default: module.LabelerWorkbenchPage })),
)
const OwnerDashboardPage = lazy(() =>
  import('../pages/owner/OwnerDashboardPage').then((module) => ({ default: module.OwnerDashboardPage })),
)
const OwnerTaskEditorPage = lazy(() =>
  import('../pages/owner/OwnerTaskEditorPage').then((module) => ({ default: module.OwnerTaskEditorPage })),
)
const OwnerTasksPage = lazy(() =>
  import('../pages/owner/OwnerTasksPage').then((module) => ({ default: module.OwnerTasksPage })),
)
const OwnerTemplateDesignerPage = lazy(() =>
  import('../pages/owner/templates/OwnerTemplateDesignerPage').then((module) => ({
    default: module.OwnerTemplateDesignerPage,
  })),
)
const OwnerTemplatesPage = lazy(() =>
  import('../pages/owner/templates/OwnerTemplatesPage').then((module) => ({ default: module.OwnerTemplatesPage })),
)
const ReviewerDashboardPage = lazy(() =>
  import('../pages/reviewer/ReviewerDashboardPage').then((module) => ({ default: module.ReviewerDashboardPage })),
)
const ReviewerQueuePage = lazy(() =>
  import('../pages/reviewer/ReviewerQueuePage').then((module) => ({ default: module.ReviewerQueuePage })),
)
const ReviewerClaimPage = lazy(() =>
  import('../pages/reviewer/ReviewerClaimPage').then((module) => ({ default: module.ReviewerClaimPage })),
)
const ReviewerAiReviewQueuePage = lazy(() =>
  import('../pages/reviewer/ReviewerAiReviewQueuePage').then((module) => ({
    default: module.ReviewerAiReviewQueuePage,
  })),
)
const ReviewerReviewDetailPage = lazy(() =>
  import('../pages/reviewer/ReviewerReviewDetailPage').then((module) => ({ default: module.ReviewerReviewDetailPage })),
)

function EntryRedirect() {
  const isAuthenticated = useAuthStore((state) => state.isAuthenticated)
  const roleHomePath = useAuthStore((state) => state.getRoleHomePath())

  return <Navigate replace to={isAuthenticated ? roleHomePath : '/login'} />
}

function RouteFallback() {
  return <StatePlaceholder status="loading" message="Page loading..." />
}

export function AppRouter() {
  return (
    <BrowserRouter>
      <Suspense fallback={<RouteFallback />}>
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
          <Route path="admin">
            <Route index element={<AdminDashboardPage />} />
            <Route element={<AdminReviewAssignmentPage />} path="review-assignment" />
            <Route element={<AdminLlmProviderPage />} path="llm-providers" />
            <Route
              element={<StatePlaceholder status="empty" message="该入口已预留，后续阶段接入业务页面。" />}
              path="*"
            />
          </Route>
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
            <Route index element={<LabelerDashboardPage />} />
            <Route element={<LabelerMarketPage />} path="market" />
            <Route element={<LabelerWorkbenchPage />} path="workbench/:taskId" />
            <Route element={<LabelerSubmissionsPage />} path="submissions" />
            <Route
              element={<StatePlaceholder status="empty" message="该入口已预留，后续阶段接入业务页面。" />}
              path="*"
            />
          </Route>
          <Route path="reviewer">
            <Route index element={<ReviewerDashboardPage />} />
            <Route element={<ReviewerClaimPage />} path="claim" />
            <Route element={<ReviewerQueuePage />} path="queue" />
            <Route element={<ReviewerAiReviewQueuePage />} path="ai-reviews" />
            <Route element={<ReviewerReviewDetailPage />} path="tasks/:taskId" />
            <Route
              element={<StatePlaceholder status="empty" message="该入口已预留，后续阶段接入业务页面。" />}
              path="*"
            />
          </Route>
          <Route
            element={<StatePlaceholder status="empty" message="该入口已预留，后续阶段接入业务页面。" />}
            path="*"
          />
        </Route>
        <Route element={<Navigate replace to="/login" />} path="*" />
        </Routes>
      </Suspense>
    </BrowserRouter>
  )
}
