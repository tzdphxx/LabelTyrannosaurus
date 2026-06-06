import { mockImportPreviews, mockTasks, mockTemplates } from '../../mocks'
import type { ImportPreview } from '../../types/import'
import type {
  CreateTaskRequest,
  CreateTaskResponse,
  DatasetItemAppendInput,
  DatasetItemAppendResult,
  DatasetItemBatchAppendRequest,
  DatasetItemPageQuery,
  DatasetItemPageResponse,
  DatasetItemResponse,
  DistributionStrategy,
  DistributionStrategyCode,
  OwnerTask,
  OwnerTaskApiStatus,
  OwnerTaskDetail,
  OwnerLabelerPageResponse,
  OwnerLabelerQuery,
  OwnerTaskPage,
  OwnerTaskPageResponse,
  OwnerTaskStatus,
  OwnerTaskSummaryResponse,
  PublishValidationResult,
  TaskDetailResponse,
  TaskDraftInput,
  TaskLifecycleResponse,
  TaskListQuery,
  TaskProgress,
  TaskStatisticsResponse,
  UpdateTaskRequest,
} from '../../types/task'
import type { TemplateSummary } from '../../types/template'
import { isRealServiceMode, request } from '../http'

const apiStatusToOwnerStatus: Record<OwnerTaskApiStatus, OwnerTaskStatus> = {
  DRAFT: 'draft',
  PUBLISHED: 'published',
  PAUSED: 'paused',
  ENDED: 'ended',
}

const ownerStatusToApiStatus: Record<OwnerTaskStatus, OwnerTaskApiStatus> = {
  draft: 'DRAFT',
  published: 'PUBLISHED',
  paused: 'PAUSED',
  ended: 'ENDED',
}

const distributionStrategyToApi: Record<DistributionStrategy, DistributionStrategyCode> = {
  先到先得: 'FCFS',
  配额分发: 'QUOTA_GRAB',
  指派: 'ASSIGNED',
}

const apiToDistributionStrategy: Partial<Record<DistributionStrategyCode, DistributionStrategy>> = {
  FCFS: '先到先得',
  QUOTA_GRAB: '配额分发',
  ASSIGNED: '指派',
}

const tasks: OwnerTask[] = mockTasks.map(cloneTask)

function cloneProgress(progress: TaskProgress): TaskProgress {
  return { ...progress }
}

function cloneTask(task: OwnerTask): OwnerTask {
  return {
    ...task,
    tags: [...task.tags],
    rewardRule: { ...task.rewardRule },
    progress: cloneProgress(task.progress),
    aiReview: { ...task.aiReview },
  }
}

function cloneImportPreview(preview: ImportPreview): ImportPreview {
  return {
    ...preview,
    mappings: preview.mappings.map((mapping) => ({ ...mapping })),
    samples: preview.samples.map((sample) => ({
      ...sample,
      values: { ...sample.values },
    })),
    issues: preview.issues.map((issue) => ({ ...issue })),
  }
}

function cloneTemplate(template: TemplateSummary): TemplateSummary {
  return { ...template }
}

function getNowLabel() {
  return new Intl.DateTimeFormat('zh-CN', {
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    hour12: false,
  })
    .format(new Date())
    .replace(/\//g, '-')
}

function getTemplateName(publishedTemplateVersionId: string | null) {
  if (!publishedTemplateVersionId) {
    return '未关联模板'
  }

  return mockTemplates.find((template) => template.currentVersionId === publishedTemplateVersionId || template.id === publishedTemplateVersionId)?.name ?? '未知模板'
}

function getImportPreview(importPreviewId?: string) {
  const preview = mockImportPreviews.find((item) => item.id === importPreviewId)

  return preview ? cloneImportPreview(preview) : null
}

function toDatasetItem(task: OwnerTask, sample: ImportPreview['samples'][number], index: number): DatasetItemResponse {
  return {
    itemId: index + 1,
    taskId: Number(task.id.replace(/\D/g, '')) || index + 1,
    externalId: sample.id,
    itemJson: sample.values,
    metadataJson: {},
    assignedCount: 0,
    submittedCount: 0,
    approvedCount: 0,
    itemStatus: 'UNCLAIMED',
    labelerId: null,
    createdAt: task.createdAt,
    updatedAt: task.updatedAt,
  }
}

function getMockDatasetItems(taskId: string, query: DatasetItemPageQuery): DatasetItemPageResponse {
  const task = tasks.find((item) => item.id === taskId)
  const preview = task ? getImportPreview(task.importPreviewId) : null
  const allItems = task && preview ? preview.samples.map((sample, index) => toDatasetItem(task, sample, index)) : []
  const filteredItems = query.externalId ? allItems.filter((item) => item.externalId.includes(query.externalId ?? '')) : allItems
  const start = (query.page - 1) * query.pageSize

  return {
    items: filteredItems.slice(start, start + query.pageSize),
    page: query.page,
    pageSize: query.pageSize,
    total: filteredItems.length,
  }
}

function toMockDatasetValues(itemJson: DatasetItemAppendInput['itemJson']): Record<string, string | number | boolean | null> {
  return Object.fromEntries(
    Object.entries(itemJson).map(([key, value]) => [
      key,
      typeof value === 'string' || typeof value === 'number' || typeof value === 'boolean' || value === null
        ? value
        : JSON.stringify(value),
    ]),
  )
}

function appendMockDatasetItems(taskId: string, items: DatasetItemAppendInput[]): DatasetItemAppendResult[] {
  const task = tasks.find((item) => item.id === taskId)
  const preview = task ? mockImportPreviews.find((item) => item.id === task.importPreviewId) : null

  if (!task || !preview) {
    return items.map((item) => ({
      itemId: 0,
      externalId: item.externalId,
      success: false,
      errorCode: 404,
      errorMessage: '任务数据集不存在',
    }))
  }

  const startIndex = preview.samples.length

  items.forEach((item, index) => {
    preview.samples.push({
      id: item.externalId || `manual-${Date.now()}-${index + 1}`,
      values: toMockDatasetValues(item.itemJson),
    })
  })
  preview.totalRows += items.length
  preview.validRows += items.length
  task.dataCount = preview.validRows
  task.progress.totalItems = preview.validRows

  return items.map((item, index) => ({
    itemId: startIndex + index + 1,
    externalId: item.externalId,
    success: true,
    errorCode: 0,
    errorMessage: '',
  }))
}

function getTemplate(publishedTemplateVersionId: string | null) {
  const template = mockTemplates.find((item) => item.currentVersionId === publishedTemplateVersionId || item.id === publishedTemplateVersionId)

  return template ? cloneTemplate(template) : null
}

function getInitialProgress(importPreviewId?: string): TaskProgress {
  const preview = mockImportPreviews.find((item) => item.id === importPreviewId)
  const totalItems = preview?.validRows ?? 0

  return {
    totalItems,
    distributedItems: 0,
    completedItems: 0,
    pendingReviewItems: 0,
    approvedItems: 0,
    rejectedItems: 0,
    abnormalItems: preview?.invalidRows ?? 0,
  }
}

function matchesTaskQuery(task: OwnerTask, query: TaskListQuery) {
  const keyword = query.keyword.trim().toLowerCase()
  const matchesKeyword =
    keyword.length === 0 ||
    task.title.toLowerCase().includes(keyword) ||
    task.description.toLowerCase().includes(keyword) ||
    task.tags.some((tag) => tag.toLowerCase().includes(keyword))
  const matchesStatus = query.status === 'all' || task.status === query.status

  return matchesKeyword && matchesStatus
}

function toNumberId(value?: string | null) {
  if (!value || !/^\d+$/.test(value)) {
    return undefined
  }

  return Number(value)
}

function formatTaskDate(value?: string | null) {
  if (!value) {
    return ''
  }

  return value.replace('T', ' ').slice(0, 16)
}

function buildTaskProgressFromStatistics(statistics: TaskStatisticsResponse): TaskProgress {
  return {
    totalItems: statistics.totalItems,
    distributedItems: statistics.claimedCount,
    completedItems: statistics.submittedCount,
    pendingReviewItems: statistics.pendingReviewCount,
    approvedItems: statistics.approvedCount,
    rejectedItems: statistics.rejectedCount,
    abnormalItems: 0,
    passRate: statistics.passRate,
  }
}

function getEmptyProgress(totalItems = 0, claimedCount = 0): TaskProgress {
  return {
    totalItems,
    distributedItems: claimedCount,
    completedItems: 0,
    pendingReviewItems: 0,
    approvedItems: 0,
    rejectedItems: 0,
    abnormalItems: 0,
  }
}

function mapSummaryResponse(response: OwnerTaskSummaryResponse): OwnerTask {
  return {
    id: String(response.taskId),
    title: response.title,
    description: response.description ?? '',
    instruction: '',
    tags: response.tags ?? response.tag ?? [],
    deadline: '',
    quota: response.quota,
    claimedCount: response.claimedCount,
    rewardRule: {
      unitPrice: 0,
      currency: 'CNY',
      rewardMode: 'APPROVED_ITEM',
      rewardCurrency: 'POINT',
      rewardVisible: true,
      description: '',
    },
    distributionStrategy: '先到先得',
    publishedTemplateVersionId: null,
    templateName: '-',
    status: apiStatusToOwnerStatus[response.status],
    dataCount: response.quota,
    updatedAt: '',
    createdAt: formatTaskDate(response.createdAt),
    progress: getEmptyProgress(response.quota, response.claimedCount),
    aiReview: {
      aiPrompt: '',
      aiModelName: '',
      aiProviderId: null,
      aiScoringDimensions: [],
      aiPassThreshold: 80,
      aiManualReviewThreshold: 60,
      aiReviewStrategy: 'LIGHTWEIGHT',
    },
    reviewLevelCount: 1,
    overlapCount: 1,
    maxClaimsPerLabeler: 10,
  }
}

function mapDetailResponse(response: TaskDetailResponse): OwnerTask {
  const rewardValue = Number(response.reward)
  const aiReviewConfig = response.aiReview

  return {
    id: String(response.taskId),
    ownerId: String(response.ownerId),
    title: response.title,
    description: response.description ?? '',
    instruction: response.instructionRichText ?? '',
    tags: response.tags ?? [],
    deadline: formatTaskDate(response.deadlineAt),
    quota: response.quota,
    claimedCount: response.claimedCount,
    rewardRule: {
      unitPrice: response.rewardRule?.unitReward ?? (Number.isFinite(rewardValue) ? rewardValue : 0),
      currency: 'CNY',
      rewardMode: response.rewardRule?.rewardMode ?? 'APPROVED_ITEM',
      rewardCurrency: response.rewardRule?.rewardCurrency ?? 'POINT',
      rewardVisible: response.rewardRule?.rewardVisible ?? true,
      description: response.reward,
    },
    distributionStrategy: apiToDistributionStrategy[response.strategy as DistributionStrategyCode] ?? (response.strategy as DistributionStrategy),
    publishedTemplateVersionId: response.publishedTemplateVersionId ? String(response.publishedTemplateVersionId) : null,
    templateName: response.publishedTemplateVersionId ? `版本 ${response.publishedTemplateVersionId}` : '未关联模板',
    status: apiStatusToOwnerStatus[response.status],
    dataCount: response.quota,
    updatedAt: formatTaskDate(response.updatedAt),
    createdAt: formatTaskDate(response.createdAt),
    progress: getEmptyProgress(response.quota, response.claimedCount),
    aiReview: {
      aiPrompt: aiReviewConfig?.promptTemplate ?? response.aiPrompt ?? '',
      aiModelName: aiReviewConfig?.modelName ?? response.aiModelName ?? '',
      aiProviderId: response.aiProviderId ? String(response.aiProviderId) : null,
      aiScoringDimensions: aiReviewConfig?.scoringDimensions ?? response.aiScoringDimensions ?? [],
      aiPassThreshold: aiReviewConfig?.passThreshold ?? response.aiPassThreshold ?? 80,
      aiManualReviewThreshold: aiReviewConfig?.manualReviewThreshold ?? response.aiManualReviewThreshold ?? 60,
      aiReviewStrategy: response.aiReviewStrategy ?? 'LIGHTWEIGHT',
    },
    reviewLevelCount: response.reviewLevelCount ?? 1,
    overlapCount: response.overlapCount ?? 1,
    maxClaimsPerLabeler: response.maxClaimsPerLabeler ?? 10,
    assignedLabelerId: response.assignedLabelerId ? String(response.assignedLabelerId) : null,
    publishedAt: response.publishedAt ?? null,
    endedAt: response.endedAt ?? null,
  }
}

function buildTaskRequest(payload: TaskDraftInput, includeDatasetFileId: boolean): CreateTaskRequest | UpdateTaskRequest {
  const requestPayload: CreateTaskRequest = {
    title: payload.title,
    description: payload.description,
    instructionRichText: payload.instruction,
    tags: payload.tags,
    quota: payload.quota,
    deadlineAt: payload.deadline,
    overlapCount: payload.overlapCount,
    rewardRule: {
      rewardMode: payload.rewardRule.rewardMode,
      unitReward: payload.rewardRule.unitPrice,
      rewardCurrency: payload.rewardRule.rewardCurrency,
      rewardVisible: payload.rewardRule.rewardVisible,
    },
    strategy: distributionStrategyToApi[payload.distributionStrategy],
    aiPrompt: payload.aiReview.aiPrompt,
    aiModelName: payload.aiReview.aiModelName,
    aiScoringDimensions: payload.aiReview.aiScoringDimensions,
    aiPassThreshold: payload.aiReview.aiPassThreshold,
    aiManualReviewThreshold: payload.aiReview.aiManualReviewThreshold,
    aiReviewStrategy: payload.aiReview.aiReviewStrategy,
    reviewLevelCount: payload.reviewLevelCount,
    maxClaimsPerLabeler: payload.maxClaimsPerLabeler,
  }
  const publishedTemplateVersionId = toNumberId(payload.publishedTemplateVersionId)
  const datasetFileId = toNumberId(payload.datasetFileId)
  const aiProviderId = toNumberId(payload.aiReview.aiProviderId)

  if (publishedTemplateVersionId) {
    requestPayload.publishedTemplateVersionId = publishedTemplateVersionId
  }

  if (aiProviderId) {
    requestPayload.aiProviderId = aiProviderId
  }

  if (requestPayload.strategy === 'ASSIGNED') {
    const assignedLabelerId = toNumberId(payload.assignedLabelerId)

    if (assignedLabelerId) {
      requestPayload.assignedLabelerId = assignedLabelerId
    }
  }

  if (includeDatasetFileId && datasetFileId) {
    requestPayload.datasetFileId = datasetFileId
  }

  if (includeDatasetFileId) {
    return requestPayload
  }

  const updatePayload = { ...requestPayload }
  delete updatePayload.datasetFileId

  return updatePayload
}

function buildTaskFromDraft(taskId: string, status: OwnerTaskStatus, payload: TaskDraftInput): OwnerTask {
  return {
    ...payload,
    id: taskId,
    status,
    templateName: getTemplateName(payload.publishedTemplateVersionId),
    dataCount: payload.quota,
    claimedCount: 0,
    createdAt: getNowLabel(),
    updatedAt: getNowLabel(),
    progress: getEmptyProgress(payload.quota),
  }
}

function validateTaskForPublish(task: OwnerTask | TaskDraftInput, hasTaskId: boolean): PublishValidationResult {
  const errors: string[] = []

  if (!hasTaskId) {
    errors.push('请先保存任务草稿')
  }

  if (!task.title.trim()) {
    errors.push('任务标题不能为空')
  }

  if (task.title.length > 200) {
    errors.push('任务标题不能超过 200 个字符')
  }

  if (task.tags.some((tag) => tag.length > 64)) {
    errors.push('单个标签不能超过 64 个字符')
  }

  if (task.quota < 1) {
    errors.push('任务配额必须大于等于 1')
  }

  if (!task.deadline) {
    errors.push('截止时间不能为空')
  } else if (Number.isNaN(Date.parse(task.deadline)) || new Date(task.deadline).getTime() <= Date.now()) {
    errors.push('截止时间必须为未来时间')
  }

  if (!task.publishedTemplateVersionId) {
    errors.push('发布前必须关联模板版本')
  }

  if (!task.datasetFileId) {
    errors.push('发布前必须上传数据集文件')
  }

  if (task.reviewLevelCount < 1) {
    errors.push('审核级别数必须大于等于 1')
  }

  if (task.rewardRule.unitPrice <= 0) {
    errors.push('奖励金额必须大于 0')
  }

  if (!task.distributionStrategy) {
    errors.push('请选择分发策略')
  }

  if (!task.aiReview.aiProviderId) {
    errors.push('请选择 AI 模型')
  }

  if (!task.aiReview.aiPrompt.trim()) {
    errors.push('AI 审核 Prompt 不能为空')
  }

  if (task.aiReview.aiPrompt.length > 10000) {
    errors.push('AI 审核 Prompt 不能超过 10000 个字符')
  }

  if (task.aiReview.aiModelName.length > 128) {
    errors.push('AI 模型名不能超过 128 个字符')
  }

  if (!task.aiReview.aiScoringDimensions.length) {
    errors.push('评分维度不能为空')
  }

  if (task.aiReview.aiScoringDimensions.some((dimension) => dimension.length > 64)) {
    errors.push('单个评分维度不能超过 64 个字符')
  }

  if (task.aiReview.aiPassThreshold < 0 || task.aiReview.aiPassThreshold > 100) {
    errors.push('通过阈值必须在 0 到 100 之间')
  }

  if (task.aiReview.aiManualReviewThreshold < 0 || task.aiReview.aiManualReviewThreshold > 100) {
    errors.push('人工复核阈值必须在 0 到 100 之间')
  }

  if ('status' in task && task.status && task.status !== 'draft') {
    errors.push('只有草稿任务可以发布')
  }

  return {
    valid: errors.length === 0,
    errors,
  }
}

async function refreshTaskAfterLifecycle(response: TaskLifecycleResponse) {
  const detail = await ownerTaskService.getTaskDetail(String(response.taskId))

  return detail?.task ?? null
}

export const ownerTaskService = {
  async listAssignableLabelers(query: OwnerLabelerQuery): Promise<OwnerLabelerPageResponse> {
    const params: Record<string, string | number> = {
      page: query.page,
      size: query.size,
    }

    if (query.keyword?.trim()) {
      params.keyword = query.keyword.trim()
    }

    return request.get<OwnerLabelerPageResponse>('/v1/owner/labelers/assignable', { params })
  },

  validateDraftForPublish(payload: TaskDraftInput, taskId: string | null): PublishValidationResult {
    return validateTaskForPublish(payload, Boolean(taskId))
  },

  async listTasks(query: TaskListQuery): Promise<OwnerTaskPage> {
    if (isRealServiceMode()) {
      const params: Record<string, string | number> = {
        page: query.page,
        size: query.pageSize,
      }

      if (query.keyword.trim()) {
        params.keyword = query.keyword.trim()
      }

      if (query.status !== 'all') {
        params.status = ownerStatusToApiStatus[query.status]
      }

      const page = await request.get<OwnerTaskPageResponse>('/v1/owner/tasks', { params })

      return {
        items: page.items.map(mapSummaryResponse),
        page: page.page,
        pageSize: page.pageSize,
        total: page.total,
      }
    }

    const filteredTasks = tasks.filter((task) => matchesTaskQuery(task, query))
    const start = (query.page - 1) * query.pageSize
    const items = filteredTasks.slice(start, start + query.pageSize).map(cloneTask)

    return {
      items,
      page: query.page,
      pageSize: query.pageSize,
      total: filteredTasks.length,
    }
  },

  async getTaskDetail(taskId: string): Promise<OwnerTaskDetail | null> {
    if (isRealServiceMode()) {
      const task = mapDetailResponse(await request.get<TaskDetailResponse>(`/v1/tasks/${taskId}`))

      return {
        task,
        template: null,
        importPreview: null,
      }
    }

    const task = tasks.find((item) => item.id === taskId)

    if (!task) {
      return null
    }

    return {
      task: cloneTask(task),
      template: getTemplate(task.publishedTemplateVersionId),
      importPreview: getImportPreview(task.importPreviewId),
    }
  },

  async listTaskDatasetItems(taskId: string, query: DatasetItemPageQuery): Promise<DatasetItemPageResponse> {
    if (isRealServiceMode()) {
      const params: Record<string, string | number> = {
        page: query.page,
        pageSize: query.pageSize,
      }

      if (query.externalId?.trim()) {
        params.externalId = query.externalId.trim()
      }

      return request.get<DatasetItemPageResponse>(`/v1/tasks/${taskId}/dataset/items`, { params })
    }

    return getMockDatasetItems(taskId, query)
  },

  async batchAppendDatasetItems(taskId: string, items: DatasetItemAppendInput[]): Promise<DatasetItemAppendResult[]> {
    if (isRealServiceMode()) {
      return request.post<DatasetItemAppendResult[], DatasetItemBatchAppendRequest>(
        `/v1/tasks/${taskId}/dataset/items/batch-append-json`,
        { items },
      )
    }

    return appendMockDatasetItems(taskId, items)
  },

  async createTask(payload: TaskDraftInput): Promise<OwnerTask> {
    if (isRealServiceMode()) {
      const response = await request.post<CreateTaskResponse, CreateTaskRequest>('/v1/tasks', buildTaskRequest(payload, true) as CreateTaskRequest)

      return buildTaskFromDraft(String(response.taskId), apiStatusToOwnerStatus[response.status], payload)
    }

    const importPreview = mockImportPreviews.find((preview) => preview.issues.every((issue) => issue.level !== 'blocking')) ?? mockImportPreviews[0]
    const task: OwnerTask = {
      ...buildTaskFromDraft(`task-${Date.now()}`, 'draft', payload),
      importPreviewId: importPreview.id,
      dataCount: importPreview.validRows,
      progress: getInitialProgress(importPreview.id),
    }

    tasks.unshift(task)

    return cloneTask(task)
  },

  async updateTask(taskId: string, payload: TaskDraftInput): Promise<OwnerTask | null> {
    if (isRealServiceMode()) {
      await request.put<TaskLifecycleResponse, UpdateTaskRequest>(`/v1/tasks/${taskId}`, buildTaskRequest(payload, false) as UpdateTaskRequest)

      const detail = await this.getTaskDetail(taskId)
      return detail?.task ?? buildTaskFromDraft(taskId, 'draft', payload)
    }

    const task = tasks.find((item) => item.id === taskId)

    if (!task) {
      return null
    }

    const nextTask: OwnerTask = {
      ...task,
      ...payload,
      templateName: getTemplateName(payload.publishedTemplateVersionId),
      dataCount: payload.quota,
      updatedAt: getNowLabel(),
    }
    const index = tasks.findIndex((item) => item.id === taskId)
    tasks[index] = nextTask

    return cloneTask(nextTask)
  },

  async publishTask(taskId: string): Promise<OwnerTask | null> {
    if (isRealServiceMode()) {
      return refreshTaskAfterLifecycle(await request.post<TaskLifecycleResponse>(`/v1/tasks/${taskId}/publish`))
    }

    return this.updateTaskStatus(taskId, 'published')
  },

  async updateTaskStatus(taskId: string | null, status: OwnerTaskStatus): Promise<OwnerTask | null> {
    if (!taskId) {
      return null
    }

    if (isRealServiceMode()) {
      const endpointMap: Partial<Record<OwnerTaskStatus, string>> = {
        paused: 'pause',
        published: 'resume',
        ended: 'end',
      }
      const endpoint = endpointMap[status]

      if (!endpoint) {
        return null
      }

      return refreshTaskAfterLifecycle(await request.post<TaskLifecycleResponse>(`/v1/tasks/${taskId}/${endpoint}`))
    }

    const task = tasks.find((item) => item.id === taskId)

    if (!task) {
      return null
    }

    task.status = status
    task.updatedAt = getNowLabel()

    return cloneTask(task)
  },

  async deleteTask(taskId: string): Promise<boolean> {
    if (isRealServiceMode()) {
      await request.delete<void>(`/v1/tasks/${taskId}`)
      return true
    }

    const index = tasks.findIndex((item) => item.id === taskId)

    if (index < 0 || tasks[index].status !== 'draft') {
      return false
    }

    tasks.splice(index, 1)
    return true
  },

  async validatePublish(taskId: string): Promise<PublishValidationResult> {
    const task = tasks.find((item) => item.id === taskId)

    if (!task) {
      return {
        valid: false,
        errors: ['任务不存在'],
      }
    }

    return validateTaskForPublish(task, true)
  },

  async getTaskProgress(taskId: string): Promise<TaskProgress | null> {
    if (isRealServiceMode()) {
      return buildTaskProgressFromStatistics(await request.get<TaskStatisticsResponse>(`/v1/tasks/${taskId}/statistics`))
    }

    const task = tasks.find((item) => item.id === taskId)

    return task ? cloneProgress(task.progress) : null
  },
}
