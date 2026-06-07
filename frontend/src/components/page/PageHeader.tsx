import type { ReactNode } from 'react'
import { Space, Typography } from 'antd'

interface PageHeaderProps {
  title: string
  description?: string
  meta?: ReactNode
  extra?: ReactNode
}

export function PageHeader({ title, description, meta, extra }: PageHeaderProps) {
  return (
    <div className="page-header">
      <div className="page-header__main">
        {meta ? <div className="page-header__meta">{meta}</div> : null}
        <Typography.Title className="page-header__title">{title}</Typography.Title>
        {description ? <Typography.Paragraph className="page-header__description">{description}</Typography.Paragraph> : null}
      </div>
      {extra ? (
        <Space className="page-header__extra" size={12}>
          {extra}
        </Space>
      ) : null}
    </div>
  )
}

