import { render, screen, waitFor } from '@testing-library/react'
import { MemoryRouter, Route, Routes } from 'react-router'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import type { ReviewerTaskItemRow } from '../../types/review'
import { useReviewStore } from '../../stores/reviewStore'
import { ReviewerReviewDetailPage } from './ReviewerReviewDetailPage'

function createTaskItem(overrides: Partial<ReviewerTaskItemRow>): ReviewerTaskItemRow {
  return {
    datasetItemId: 1,
    externalId: 'item-1',
    itemJson: '{}',
    metadataJson: '{}',
    itemStatus: 'SUBMITTED',
    assignmentId: 10,
    assignmentStatus: 'SUBMITTED',
    labelerId: 20,
    labelerName: 'labeler',
    latestSubmissionId: 100,
    versionNo: 1,
    submissionStatus: 'PENDING_FINAL',
    submittedAt: '2026-06-10 10:00',
    aiReviewStatus: 'SUCCESS',
    aiDecision: 'MANUAL_REVIEW',
    averageScore: '80',
    riskFlags: '[]',
    suggestion: 'manual',
    reviewTaskStatus: 'PENDING',
    reviewLevel: 1,
    latestReviewAction: undefined,
    latestReviewAt: undefined,
    canOpenSubmissionDetail: true,
    canReview: true,
    ...overrides,
  }
}

function renderPage() {
  render(
    <MemoryRouter initialEntries={['/app/reviewer/tasks/task-1']}>
      <Routes>
        <Route element={<ReviewerReviewDetailPage />} path="/app/reviewer/tasks/:taskId" />
      </Routes>
    </MemoryRouter>,
  )
}

describe('ReviewerReviewDetailPage', () => {
  const loadReviewerTaskItems = vi.fn()
  const loadDetail = vi.fn()
  const setSelectedReviewIds = vi.fn()

  beforeEach(() => {
    vi.clearAllMocks()

    const submittedItem = createTaskItem({
      datasetItemId: 1,
      externalId: 'submitted-item',
      latestSubmissionId: 1001,
      canOpenSubmissionDetail: true,
    })
    const unsubmittedItem = createTaskItem({
      datasetItemId: 2,
      externalId: 'unsubmitted-item',
      latestSubmissionId: undefined,
      submissionStatus: undefined,
      submittedAt: undefined,
      aiReviewStatus: undefined,
      aiDecision: undefined,
      canOpenSubmissionDetail: false,
      canReview: false,
    })

    useReviewStore.setState({
      taskItemsPage: {
        taskId: 1,
        taskTitle: '审核任务',
        taskStatus: 'PUBLISHED',
        totalItemCount: 2,
        statusSummary: {
          unclaimedCount: 0,
          claimedCount: 1,
          draftCount: 0,
          submittedCount: 1,
          returnedCount: 0,
          approvedCount: 0,
        },
        page: {
          items: [submittedItem, unsubmittedItem],
          page: 1,
          pageSize: 100,
          total: 2,
        },
      },
      currentDetail: null,
      submissionVersions: [],
      selectedReviewIds: [],
      todayReviewedCount: 0,
      error: null,
      isTaskItemsLoading: false,
      isDetailLoading: false,
      isVersionsLoading: false,
      isActionSubmitting: false,
      isBatchSubmitting: false,
      loadReviewerTaskItems,
      loadDetail,
      setSelectedReviewIds,
    })
  })

  it('requests and renders only submitted task items', async () => {
    renderPage()

    await waitFor(() => {
      expect(loadReviewerTaskItems).toHaveBeenCalledWith('task-1', {
        page: 1,
        size: 100,
        submittedOnly: true,
      })
    })

    expect(screen.getByText('submitted-item')).toBeInTheDocument()
    expect(screen.queryByText('unsubmitted-item')).not.toBeInTheDocument()
  })
})
