import { describe, expect, it, vi } from 'vitest'
import { canAcceptChild, createSchemaNodeFromMaterial, createTabPaneNode } from './materialRegistry'

describe('materialRegistry', () => {
  it('creates material nodes with default props', () => {
    vi.spyOn(Date, 'now').mockReturnValue(1000)
    vi.spyOn(Math, 'random').mockReturnValue(0.12345)

    const field = createSchemaNodeFromMaterial('input')

    expect(field.id).toMatch(/^node-1000-/)
    expect(field.key).toMatch(/^input_/)
    expect(field.type).toBe('input')
    expect(field.props).toHaveProperty('placeholder')
  })

  it('creates layout nodes with expected children', () => {
    const group = createSchemaNodeFromMaterial('group')
    const tabs = createSchemaNodeFromMaterial('tabs')
    const tabPane = createTabPaneNode('Custom Tab')

    expect(group.children).toEqual([])
    expect(tabs.children).toHaveLength(1)
    expect(tabs.children?.[0].type).toBe('tabPane')
    expect(tabPane.title).toBe('Custom Tab')
  })

  it('checks child acceptance rules', () => {
    expect(canAcceptChild(null, 'input')).toBe(true)
    expect(canAcceptChild(null, 'tabPane')).toBe(false)
    expect(canAcceptChild('input', 'textarea')).toBe(false)
    expect(canAcceptChild('group', 'input')).toBe(true)
    expect(canAcceptChild('tabs', 'tabPane')).toBe(true)
    expect(canAcceptChild('tabs', 'input')).toBe(false)
  })
})
