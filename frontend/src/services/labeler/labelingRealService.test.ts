import { beforeEach, describe, expect, it, vi } from 'vitest'

const requestMock = vi.hoisted(() => ({
  get: vi.fn(),
  put: vi.fn(),
  post: vi.fn(),
}))

vi.mock('../http', () => ({
  ApiError: class ApiError extends Error {
    code?: number
    status?: number
    url?: string
    method?: string
    details?: unknown

    constructor(options: { code?: number; message?: string; status?: number; url?: string; method?: string; details?: unknown }) {
      super(options.message)
      this.code = options.code
      this.status = options.status
      this.url = options.url
      this.method = options.method
      this.details = options.details
    }
  },
  request: requestMock,
}))

function claimedTaskResponse(draftVersion: number, taskStatus = 'PUBLISHED') {
  return [
    {
      task: {
        taskId: 1,
        title: 'Task 1',
        status: taskStatus,
        quota: 1,
        claimedCount: 1,
        overlapCount: 1,
        strategy: 'FCFS',
      },
      taskId: 1,
      myClaimedCount: 1,
      items: [
        {
          claimId: 101,
          itemId: 201,
          externalId: 'item-201',
          claimStatus: 'DRAFTING',
          itemJson: JSON.stringify({ text: 'source' }),
          draftVersion,
          updatedAt: '2026-06-10T10:00:00',
        },
      ],
    },
  ]
}

function answerTemplateResponse() {
  return {
    taskId: 1,
    templateVersionId: 301,
    schemaJson: JSON.stringify({
      id: 'schema-1',
      version: 'v1',
      title: 'Schema',
      nodes: [
        {
          id: 'answer-node',
          key: 'answer',
          type: 'input',
          title: 'Answer',
          props: {},
        },
      ],
    }),
  }
}

function draftResponse(draftVersion: number, answer = { answer: 'saved' }) {
  return {
    assignmentId: 101,
    draftAnswerJson: JSON.stringify(answer),
    draftVersion,
    status: 'DRAFTING',
    updatedAt: '2026-06-10T10:00:01',
  }
}

async function importService() {
  vi.resetModules()
  return import('./labelingRealService')
}

describe('realLabelingService draft version handling', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('refreshes cached draftVersion from claimed item list before saving', async () => {
    const claimedVersions = [2, 3]
    requestMock.get.mockImplementation((url: string) => {
      if (url === '/v1/claims') {
        return Promise.resolve(claimedTaskResponse(claimedVersions.shift() ?? 3))
      }

      if (url === '/v1/labeler/tasks/1/answer-template') {
        return Promise.resolve(answerTemplateResponse())
      }

      throw new Error(`Unexpected GET ${url}`)
    })
    requestMock.put.mockResolvedValue(draftResponse(4))

    const { realLabelingService } = await importService()

    await realLabelingService.listQuestions('1')
    await realLabelingService.listQuestions('1')
    await realLabelingService.saveDraft({
      taskId: '1',
      questionId: '101',
      userId: 'labeler-1',
      values: { answer: 'changed' },
    })

    expect(requestMock.put).toHaveBeenCalledWith('/v1/claims/101/draft', {
      answerJson: JSON.stringify({ answer: 'changed' }),
      clientVersion: 3,
    })
  })

  it('serializes saves for the same assignment and uses the updated version', async () => {
    let resolveFirstSave: ((value: unknown) => void) | undefined
    requestMock.get.mockImplementation((url: string) => {
      if (url === '/v1/claims') {
        return Promise.resolve(claimedTaskResponse(2))
      }

      if (url === '/v1/labeler/tasks/1/answer-template') {
        return Promise.resolve(answerTemplateResponse())
      }

      throw new Error(`Unexpected GET ${url}`)
    })
    requestMock.put
      .mockImplementationOnce(() => new Promise((resolve) => {
        resolveFirstSave = resolve
      }))
      .mockResolvedValueOnce(draftResponse(4, { answer: 'second' }))

    const { realLabelingService } = await importService()

    await realLabelingService.listQuestions('1')

    const firstSave = realLabelingService.saveDraft({
      taskId: '1',
      questionId: '101',
      userId: 'labeler-1',
      values: { answer: 'first' },
    })
    const secondSave = realLabelingService.saveDraft({
      taskId: '1',
      questionId: '101',
      userId: 'labeler-1',
      values: { answer: 'second' },
    })

    await Promise.resolve()
    await Promise.resolve()

    expect(requestMock.put).toHaveBeenCalledTimes(1)
    expect(requestMock.put).toHaveBeenNthCalledWith(1, '/v1/claims/101/draft', {
      answerJson: JSON.stringify({ answer: 'first' }),
      clientVersion: 2,
    })

    resolveFirstSave?.(draftResponse(3, { answer: 'first' }))
    await firstSave
    await secondSave

    expect(requestMock.put).toHaveBeenCalledTimes(2)
    expect(requestMock.put).toHaveBeenNthCalledWith(2, '/v1/claims/101/draft', {
      answerJson: JSON.stringify({ answer: 'second' }),
      clientVersion: 3,
    })
  })

  it('refreshes draft state when a version conflict already saved the same values', async () => {
    requestMock.get.mockImplementation((url: string) => {
      if (url === '/v1/claims') {
        return Promise.resolve(claimedTaskResponse(2))
      }

      if (url === '/v1/labeler/tasks/1/answer-template') {
        return Promise.resolve(answerTemplateResponse())
      }

      if (url === '/v1/claims/101/draft') {
        return Promise.resolve(draftResponse(3, { answer: 'changed' }))
      }

      throw new Error(`Unexpected GET ${url}`)
    })
    const { realLabelingService } = await importService()
    const { ApiError } = await import('../http')
    requestMock.put.mockRejectedValueOnce(new ApiError({
      code: 409101,
      message: 'draft conflict',
      status: 409,
      url: '/v1/claims/101/draft',
      method: 'PUT',
    }))

    await realLabelingService.listQuestions('1')
    const draft = await realLabelingService.saveDraft({
      taskId: '1',
      questionId: '101',
      userId: 'labeler-1',
      values: { answer: 'changed' },
    })

    expect(requestMock.get).toHaveBeenCalledWith('/v1/claims/101/draft')
    expect(draft.values).toEqual({ answer: 'changed' })

    requestMock.put.mockResolvedValueOnce(draftResponse(4, { answer: 'next' }))
    await realLabelingService.saveDraft({
      taskId: '1',
      questionId: '101',
      userId: 'labeler-1',
      values: { answer: 'next' },
    })

    expect(requestMock.put).toHaveBeenLastCalledWith('/v1/claims/101/draft', {
      answerJson: JSON.stringify({ answer: 'next' }),
      clientVersion: 3,
    })
  })

  it('uses ended task lifecycle status for claimed task assignment summaries', async () => {
    requestMock.get.mockImplementation((url: string) => {
      if (url === '/v1/claims') {
        return Promise.resolve([
          {
            ...claimedTaskResponse(2, 'ENDED')[0],
            items: [],
          },
        ])
      }

      throw new Error(`Unexpected GET ${url}`)
    })

    const { realLabelingService } = await importService()
    const assignments = await realLabelingService.listAssignments()

    expect(assignments).toHaveLength(1)
    expect(assignments[0]?.status).toBe('ENDED')
  })
})
