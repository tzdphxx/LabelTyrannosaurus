import { mockImportPreviews, mockTasks, mockTemplates } from '../../mocks'
import type { ImportPreview } from '../../types/import'
import type {
  CreateTaskRequest,
  CreateTaskResponse,
  OwnerTask,
  OwnerTaskApiStatus,
  OwnerTaskDetail,
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
      prompt: '',
      model: '',
      rating: '',
    },
    reviewLevelCount: 1,
  }
}

function mapDetailResponse(response: TaskDetailResponse): OwnerTask {
  const rewardValue = Number(response.reward)

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
      unitPrice: Number.isFinite(rewardValue) ? rewardValue : 0,
      currency: 'CNY',
      description: response.reward,
    },
    distributionStrategy: response.strategy,
    publishedTemplateVersionId: response.publishedTemplateVersionId ? String(response.publishedTemplateVersionId) : null,
    templateName: response.publishedTemplateVersionId ? `版本 ${response.publishedTemplateVersionId}` : '未关联模板',
    status: apiStatusToOwnerStatus[response.status],
    dataCount: response.quota,
    updatedAt: formatTaskDate(response.updatedAt),
    createdAt: formatTaskDate(response.createdAt),
    progress: getEmptyProgress(response.quota, response.claimedCount),
    aiReview: {
      prompt: response.prompt ?? '',
      model: response.model ?? '',
      rating: response.rating ?? '',
    },
    reviewLevelCount: response.reviewLevelCount ?? 1,
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
    reward: String(payload.rewardRule.unitPrice),
    strategy: payload.distributionStrategy,
    prompt: payload.aiReview.prompt,
    model: payload.aiReview.model,
    rating: payload.aiReview.rating,
    reviewLevelCount: payload.reviewLevelCount,
  }
  const publishedTemplateVersionId = toNumberId(payload.publishedTemplateVersionId)
  const datasetFileId = toNumberId(payload.datasetFileId)

  if (publishedTemplateVersionId) {
    requestPayload.publishedTemplateVersionId = publishedTemplateVersionId
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
