/// <reference types="node" />
/// <reference types="vitest" />

import { readFileSync } from 'node:fs'
import { join } from 'node:path'
import { describe, expect, it } from 'vitest'

const reviewerStyles = readFileSync(join(process.cwd(), 'src/pages/reviewer/ReviewerPages.module.css'), 'utf8')
const queuePage = readFileSync(join(process.cwd(), 'src/pages/reviewer/ReviewerAiReviewQueuePage.tsx'), 'utf8')

function ruleFor(selector: string) {
  const escapedSelector = selector.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')
  const match = reviewerStyles.match(new RegExp(`${escapedSelector}\\s*\\{([^}]*)\\}`))

  return match?.[1] ?? ''
}

describe('ReviewerAiReviewQueuePage readable detail content styles', () => {
  it('does not cap prompt and code panels inside the scrollable detail column', () => {
    expect(ruleFor('.aiCodePanel')).not.toContain('max-height')
    expect(ruleFor('.aiPromptPanel')).not.toContain('max-height')
  })

  it('applies a shared wrapping style to long AI review detail text', () => {
    expect(reviewerStyles).toMatch(/\.aiReadableText\s*\{[^}]*overflow-wrap: anywhere;/s)
    expect(queuePage).toContain('className={styles.aiReadableText}')
  })

  it('prevents AI detail cards from shrinking and clipping their content', () => {
    expect(reviewerStyles).toMatch(/\.aiDetail :global\(\.ant-card\)\s*\{[^}]*flex: 0 0 auto;/s)
  })
})
