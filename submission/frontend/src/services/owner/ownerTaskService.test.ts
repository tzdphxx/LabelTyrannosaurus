import { beforeEach, describe, expect, it, vi } from 'vitest'

const httpMock = vi.hoisted(() => ({
  isRealServiceMode: vi.fn(() => true),
  request: {
    get: vi.fn(),
    post: vi.fn(),
  },
}))

vi.mock('../http', () => httpMock)

async function importService() {
  vi.resetModules()
  return import('./ownerTaskService')
}

describe('ownerTaskService dataset item endpoints', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    httpMock.isRealServiceMode.mockReturnValue(true)
  })

  it('lists dataset items through the backend task items endpoint', async () => {
    httpMock.request.get.mockResolvedValue({
      items: [],
      page: 1,
      pageSize: 10,
      total: 0,
    })

    const { ownerTaskService } = await importService()

    await ownerTaskService.listTaskDatasetItems('42', { page: 1, pageSize: 10 })

    expect(httpMock.request.get).toHaveBeenCalledWith('/v1/tasks/42/items', {
      params: {
        page: 1,
        pageSize: 10,
      },
    })
  })

  it('appends dataset items through the backend batch append json endpoint', async () => {
    httpMock.request.post.mockResolvedValue([])

    const { ownerTaskService } = await importService()

    await ownerTaskService.batchAppendDatasetItems('42', [
      {
        externalId: 'item-1',
        itemJson: { text: 'source' },
        metadataJson: {},
      },
    ])

    expect(httpMock.request.post).toHaveBeenCalledWith('/v1/tasks/42/items/batch-append-json', {
      items: [
        {
          externalId: 'item-1',
          itemJson: { text: 'source' },
          metadataJson: {},
        },
      ],
    })
  })
})
