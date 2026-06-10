import { Breadcrumb } from 'antd'
import { useLocation, useNavigate } from 'react-router'
import { getRoleNavigation } from '../../app/navigation'
import { useAuthStore } from '../../stores/authStore'
import { roleHomePaths, roleLabels } from '../../utils/roles'

interface BreadcrumbEntry {
  label: string
  path?: string
}

const detailRoutes: Array<{ pattern: string; title: string; parentPath: string }> = [
  { pattern: '/app/owner/tasks/new', title: '创建任务', parentPath: '/app/owner/tasks' },
  { pattern: '/app/owner/tasks/:taskId/edit', title: '编辑任务', parentPath: '/app/owner/tasks' },
  { pattern: '/app/owner/templates/:templateId/designer', title: '模板 Designer', parentPath: '/app/owner/templates' },
  { pattern: '/app/labeler/workbench/:taskId', title: '标注工作台', parentPath: '/app/labeler/market' },
  { pattern: '/app/reviewer/tasks/:taskId', title: '审核详情', parentPath: '/app/reviewer/queue' },
]

function matchPathPattern(pattern: string, pathname: string) {
  const matcher = new RegExp(`^${pattern.replace(/:[^/]+/g, '[^/]+')}$`)

  return matcher.test(pathname)
}

function getDetailRoute(pathname: string) {
  return detailRoutes.find((route) => matchPathPattern(route.pattern, pathname)) ?? null
}

export function BreadcrumbNav() {
  const navigate = useNavigate()
  const location = useLocation()
  const role = useAuthStore((state) => state.currentRole)

  if (!role) {
    return null
  }

  const pathname = location.pathname
  const navigationItems = getRoleNavigation(role)
  const detailRoute = getDetailRoute(pathname)
  const activeItem = detailRoute
    ? navigationItems.find((item) => item.path === detailRoute.parentPath)
    : [...navigationItems].reverse().find((item) => pathname === item.path || pathname.startsWith(`${item.path}/`))
  const entries: BreadcrumbEntry[] = [
    {
      label: roleLabels[role],
      path: roleHomePaths[role],
    },
  ]

  if (activeItem) {
    entries.push({
      label: activeItem.label,
      path: activeItem.path,
    })
  }

  if (detailRoute) {
    entries.push({
      label: detailRoute.title,
    })
  }

  const dedupedEntries = entries.filter((entry, index, list) => index === 0 || entry.label !== list[index - 1].label)

  return (
    <div className="breadcrumb-nav">
      <Breadcrumb
        items={dedupedEntries.map((entry, index) => {
          const isCurrent = index === dedupedEntries.length - 1
          const canNavigate = Boolean(entry.path) && !isCurrent && entry.path !== pathname

          return {
            title: canNavigate ? (
              <button className="breadcrumb-nav__link" type="button" onClick={() => navigate(entry.path ?? roleHomePaths[role])}>
                {entry.label}
              </button>
            ) : (
              <span className={isCurrent ? 'breadcrumb-nav__current' : 'breadcrumb-nav__text'}>{entry.label}</span>
            ),
          }
        })}
      />
    </div>
  )
}
