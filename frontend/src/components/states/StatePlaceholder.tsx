import { Result } from 'antd'
import type { PageStateKind } from '../../types/page'

const statusMap: Record<PageStateKind, 'info' | 'warning' | 'error' | '403' | '404'> = {
  idle: 'info',
  loading: 'info',
  empty: '404',
  error: 'error',
  forbidden: '403',
}

const titleMap: Record<PageStateKind, string> = {
  idle: '等待内容',
  loading: '加载中',
  empty: '暂无内容',
  error: '页面异常',
  forbidden: '无权限访问',
}

interface StatePlaceholderProps {
  status: PageStateKind
  message?: string
}

export function StatePlaceholder({ status, message }: StatePlaceholderProps) {
  return (
    <Result
      status={statusMap[status]}
      title={titleMap[status]}
      subTitle={message ?? '当前阶段仅保留基础入口，业务功能将在后续阶段接入。'}
    />
  )
}

