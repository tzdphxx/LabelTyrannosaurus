import { describe, expect, it } from 'vitest'
import type { AiReviewResultResponse } from '../../types/review'
import { formatAiReviewStatusLabel, matchesAiReviewLogQuery } from './reviewMappers'

function createAiReviewLog(overrides: Partial<AiReviewResultResponse>): AiReviewResultResponse {
  return {
    submissionId: 1,
    taskId: 1,
    taskTitle: 'Task',
    submissionStatus: 'PENDING_FINAL',
    aiReviewStatus: 'SUCCESS',
    decision: 'PASS',
    averageScore: 90,
    submittedAt: '2026-06-10 10:00',
    ...overrides,
  }
}

describe('reviewMappers', () => {
  it('matches manual queue filter by MANUAL_REQUIRED aiReviewStatus', () => {
    const item = createAiReviewLog({
      aiReviewStatus: 'MANUAL_REQUIRED',
      decision: '',
    })

    expect(matchesAiReviewLogQuery(item, { page: 1, pageSize: 20, decision: 'MANUAL_REVIEW' })).toBe(true)
  })

  it('formats manual AI review status values by their distinct semantics', () => {
    expect(formatAiReviewStatusLabel('MANUAL_REQUIRED')).toBe('人工复核')
    expect(formatAiReviewStatusLabel('MANUAL_REVIEW')).toBe('转人工')
  })
})
