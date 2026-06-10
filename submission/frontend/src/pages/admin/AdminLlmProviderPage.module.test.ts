/// <reference types="node" />
/// <reference types="vitest" />

import { readFileSync } from 'node:fs'
import { join } from 'node:path'
import { describe, expect, it } from 'vitest'

const css = readFileSync(join(process.cwd(), 'src/pages/admin/AdminLlmProviderPage.module.css'), 'utf8')

describe('AdminLlmProviderPage styles', () => {
  it('shares admin theme variables with portal-mounted provider drawer and test modal', () => {
    expect(css).toMatch(/\.page,\s*\.providerDrawer,\s*\.testModal\s*\{/)
    expect(css).toContain('--admin-primary: #0075de;')
    expect(css).toContain('--admin-primary-active: #005bab;')
  })

  it('keeps primary actions visible before hover', () => {
    expect(css).toMatch(
      /\.panel :global\(\.ant-btn-primary\),\s*\.providerDrawer :global\(\.ant-btn-primary\),\s*\.testModal :global\(\.ant-btn-primary\)\s*\{[^}]*background: var\(--admin-primary\);[^}]*color: #ffffff;/s,
    )
  })
})
