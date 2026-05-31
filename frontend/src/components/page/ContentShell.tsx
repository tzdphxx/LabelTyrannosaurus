import type { ReactNode } from 'react'

interface ContentShellProps {
  children: ReactNode
  className?: string
}

export function ContentShell({ children, className }: ContentShellProps) {
  const classes = ['content-shell', className].filter(Boolean).join(' ')

  return <section className={classes}>{children}</section>
}

