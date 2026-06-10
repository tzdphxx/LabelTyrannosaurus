import { beforeEach, describe, expect, it, vi } from 'vitest'
import { ownerTaskService } from '../services'
import type { OwnerTask } from '../types/task'
import { useOwnerTaskStore } from './ownerTaskStore'

vi.mock('../services', () => ({
  ownerTaskService: {
    listTasks: vi.fn(),
    getTaskDetail: vi.fn(),
    getTaskProgress: vi.fn(),
    listTaskDatasetItems: vi.fn(),
    publishTask: vi.fn(),
    updateTaskStatus: vi.fn(),
  },
}))

function createTask(overrides: Partial<OwnerTask> = {}): OwnerTask {
  return {
    id: 'task-1',
    title: 'Task 1',
    description: 'Task description',
    instruction: '',
    tags: [],
    deadline: '2026-06-30',
    quota: 10,
    claimedCount: 0,
    rewardRule: {
      unitPrice: 1,
      currency: 'CNY',
      description: '',
      rewardMode: 'APPROVED_ITEM',
      rewardCurrency: 'POINT',
      rewardVisible: true,
    },
    distributionStrategy: '先到先得',
    publishedTemplateVersionId: null,
    templateName: '-',
    status: 'published',
    dataCount: 10,
    updatedAt: '2026-06-10 10:00',
    createdAt: '2026-06-01 10:00',
    progress: {
      totalItems: 10,
      distributedItems: 0,
      completedItems: 0,
      pendingReviewItems: 0,
      approvedItems: 0,
      rejectedItems: 0,
      abnormalItems: 0,
    },
    aiReview: {
      aiPrompt: '',
      aiModelName: '',
      aiProviderId: null,
      aiScoringDimensions: [],
      aiPassThreshold: 80,
      aiManualReviewThreshold: 60,
      aiReviewStrategy: 'LIGHTWEIGHT',
      aiFlowPolicy: 'MANUAL_FIRST',
    },
    reviewLevelCount: 1,
    overlapCount: 1,
    maxClaimsPerLabeler: 10,
    ...overrides,
  }
}

describe('ownerTaskStore', () => {
  beforeEach(() => {
    vi.clearAllMocks()

    useOwnerTaskStore.setState({
      tasks: [createTask()],
      currentTaskDetail: null,
      currentTaskProgress: null,
      currentDatasetItemsPage: null,
      filters: {
        keyword: '',
        page: 1,
        pageSize: 20,
        status: 'all',
      },
      total: 1,
      isListLoading: false,
      isDetailLoading: false,
      isProgressLoading: false,
      isDatasetItemsLoading: false,
      isAppendingDatasetItems: false,
      isStatusSubmitting: false,
      isDeleting: false,
      error: null,
    })
  })

  it('does not load task detail after a list page status update succeeds', async () => {
    const pausedTask = createTask({ status: 'paused' })

    vi.mocked(ownerTaskService.updateTaskStatus).mockResolvedValue(pausedTask)
    vi.mocked(ownerTaskService.listTasks).mockResolvedValue({
      items: [pausedTask],
      page: 1,
      pageSize: 20,
      total: 1,
    })
    vi.mocked(ownerTaskService.getTaskDetail).mockRejectedValue(new Error('detail failed'))

    const result = await useOwnerTaskStore.getState().updateTaskStatus('task-1', 'paused')

    expect(result).toEqual(pausedTask)
    expect(ownerTaskService.listTasks).toHaveBeenCalledOnce()
    expect(ownerTaskService.getTaskDetail).not.toHaveBeenCalled()
    expect(useOwnerTaskStore.getState().tasks).toEqual([pausedTask])
    expect(useOwnerTaskStore.getState().error).toBeNull()
  })
})
