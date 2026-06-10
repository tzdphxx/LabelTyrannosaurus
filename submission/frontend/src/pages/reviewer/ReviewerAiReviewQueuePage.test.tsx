import { render, waitFor } from '@testing-library/react'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { useReviewStore } from '../../stores/reviewStore'
import type { AiReviewResultResponse } from '../../types/review'
import { ReviewerAiReviewQueuePage } from './ReviewerAiReviewQueuePage'

function createAiReviewLog(overrides: Partial<AiReviewResultResponse>): AiReviewResultResponse {
  return {
    submissionId: 1,
    taskId: 1,
    taskTitle: 'AI review task',
    aiReviewStatus: 'PENDING',
    decision: 'REJECT',
    averageScore: 45,
    submittedAt: '2026-06-10 10:00:00',
    ...overrides,
  }
}

describe('ReviewerAiReviewQueuePage', () => {
  const setAiReviewLogQuery = vi.fn()
  const setCurrentAiReviewLog = vi.fn()
  const loadAllAiReviewLogs = vi.fn()
  const loadSubmissionAiReview = vi.fn()
  const loadSubmissionItemHistory = vi.fn()
  const retrySubmissionAiReview = vi.fn()

  beforeEach(() => {
    vi.clearAllMocks()

    const selectedLog = createAiReviewLog({ submissionId: 1 })

    useReviewStore.setState({
      aiReviewLogs: [selectedLog],
      currentAiReviewLog: selectedLog,
      currentSubmissionItemHistory: null,
      aiReviewLogQuery: { page: 1, pageSize: 20 },
      aiReviewLogTotal: 1,
      error: null,
      isAiReviewLogsLoading: false,
      isSubmissionItemHistoryLoading: false,
      isAiReviewRetrying: false,
      setAiReviewLogQuery,
      setCurrentAiReviewLog,
      loadAllAiReviewLogs,
      loadSubmissionAiReview,
      loadSubmissionItemHistory,
      retrySubmissionAiReview,
    })
  })

  it('loads detail for the current first AI review log after the list selects it', async () => {
    render(<ReviewerAiReviewQueuePage />)

    await waitFor(() => {
      expect(loadSubmissionAiReview).toHaveBeenCalledWith('1')
    })
  })
})
