import { fireEvent, render, screen, within } from '@testing-library/react'
import { MemoryRouter } from 'react-router'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { useLabelingStore } from '../../stores/labelingStore'
import type { LabelerAssignmentSummary } from '../../types/labeling'
import { LabelerSubmissionsPage } from './LabelerSubmissionsPage'

const loadAssignments = vi.fn()

function createAssignment(overrides: Partial<LabelerAssignmentSummary>): LabelerAssignmentSummary {
  return {
    id: 'task-1',
    assignmentId: 'assignment-1',
    taskId: '1',
    taskTitle: '任务 1',
    datasetItemId: 'item-1',
    status: 'CLAIMED',
    draftVersion: 0,
    claimedAt: '2026-06-10 10:00',
    updatedAt: '2026-06-10 10:00',
    myClaimedCount: 1,
    mySubmittedCount: 0,
    myApprovedCount: 0,
    ...overrides,
  }
}

function renderPage() {
  render(
    <MemoryRouter>
      <LabelerSubmissionsPage />
    </MemoryRouter>,
  )
}

describe('LabelerSubmissionsPage', () => {
  beforeEach(() => {
    vi.clearAllMocks()

    useLabelingStore.setState({
      assignments: [
        createAssignment({
          id: 'paused-task',
          taskId: '10',
          taskTitle: '暂停任务',
          status: 'PAUSED',
        }),
        createAssignment({
          id: 'ended-task',
          taskId: '11',
          taskTitle: '结束任务',
          status: 'ENDED',
        }),
      ],
      assignmentStats: {
        total: 2,
        claimed: 0,
        drafting: 0,
        submitted: 0,
        returned: 0,
        approved: 0,
        cancelled: 0,
      },
      error: null,
      isAssignmentsLoading: false,
      loadAssignments,
    })
  })

  it('shows paused and ended task statuses and disables workbench entry', () => {
    renderPage()

    const pausedRow = screen.getByText('暂停任务').closest('tr')
    const endedRow = screen.getByText('结束任务').closest('tr')

    expect(pausedRow).not.toBeNull()
    expect(endedRow).not.toBeNull()
    expect(within(pausedRow as HTMLElement).getByText('已暂停')).toBeInTheDocument()
    expect(within(endedRow as HTMLElement).getByText('已结束')).toBeInTheDocument()
    expect(within(pausedRow as HTMLElement).getByRole('button', { name: /进入工作台/ })).toBeDisabled()
    expect(within(endedRow as HTMLElement).getByRole('button', { name: /进入工作台/ })).toBeDisabled()
  })

  it('only offers claimed paused and ended status filters', () => {
    renderPage()

    fireEvent.mouseDown(screen.getByRole('combobox'))

    const options = Array.from(document.body.querySelectorAll('.ant-select-item-option-content'))
      .map((option) => option.textContent)

    expect(options).toEqual(['已领取', '已暂停', '已结束'])
  })
})
