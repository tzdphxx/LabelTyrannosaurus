import { mockImportPreviews, mockTasks, mockTemplates } from '../../mocks'
import type { ImportPreview } from '../../types/import'
import type {
  OwnerTask,
  OwnerTaskDetail,
  OwnerTaskStatus,
  PublishValidationResult,
  TaskDraftInput,
  TaskListQuery,
  TaskProgress,
} from '../../types/task'
import type { TemplateSummary } from '../../types/template'

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

function getTemplateName(templateId: string | null) {
  if (!templateId) {
    return '未关联模板'
  }

  return mockTemplates.find((template) => template.id === templateId)?.name ?? '未知模板'
}

function getImportPreview(importPreviewId?: string) {
  const preview = mockImportPreviews.find((item) => item.id === importPreviewId)

  return preview ? cloneImportPreview(preview) : null
}

function getTemplate(templateId: string | null) {
  const template = mockTemplates.find((item) => item.id === templateId)

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

function validateTaskForPublish(task: OwnerTask): PublishValidationResult {
  const errors: string[] = []
  const preview = mockImportPreviews.find((item) => item.id === task.importPreviewId)

  if (!task.title.trim()) {
    errors.push('任务标题不能为空')
  }

  if (!task.description.trim()) {
    errors.push('任务描述不能为空')
  }

  if (!task.templateId) {
    errors.push('发布前必须关联模板')
  }

  if (!preview || preview.validRows === 0) {
    errors.push('发布前必须存在可用导入数据')
  }

  if (preview?.issues.some((issue) => issue.level === 'blocking')) {
    errors.push('导入数据存在阻断错误')
  }

  if (task.status !== 'draft' && task.status !== 'paused') {
    errors.push('当前任务状态不允许发布')
  }

  return {
    valid: errors.length === 0,
    errors,
  }
}

export const ownerTaskService = {
  async listTasks(query: TaskListQuery): Promise<OwnerTask[]> {
    return tasks.filter((task) => matchesTaskQuery(task, query)).map(cloneTask)
  },

  async getTaskDetail(taskId: string): Promise<OwnerTaskDetail | null> {
    const task = tasks.find((item) => item.id === taskId)

    if (!task) {
      return null
    }

    return {
      task: cloneTask(task),
      template: getTemplate(task.templateId),
      importPreview: getImportPreview(task.importPreviewId),
    }
  },

  async createTask(payload: TaskDraftInput): Promise<OwnerTask> {
    const importPreview = mockImportPreviews.find((preview) => preview.issues.every((issue) => issue.level !== 'blocking')) ?? mockImportPreviews[0]
    const task: OwnerTask = {
      ...payload,
      id: `task-${Date.now()}`,
      status: 'draft',
      templateName: getTemplateName(payload.templateId),
      dataCount: importPreview.validRows,
      importPreviewId: importPreview.id,
      createdAt: getNowLabel(),
      updatedAt: getNowLabel(),
      progress: getInitialProgress(importPreview.id),
    }

    tasks.unshift(task)

    return cloneTask(task)
  },

  async updateTask(taskId: string, payload: TaskDraftInput): Promise<OwnerTask | null> {
    const task = tasks.find((item) => item.id === taskId)

    if (!task) {
      return null
    }

    const nextTask: OwnerTask = {
      ...task,
      ...payload,
      templateName: getTemplateName(payload.templateId),
      updatedAt: getNowLabel(),
    }
    const index = tasks.findIndex((item) => item.id === taskId)
    tasks[index] = nextTask

    return cloneTask(nextTask)
  },

  async updateTaskStatus(taskId: string | null, status: OwnerTaskStatus): Promise<OwnerTask | null> {
    const task = tasks.find((item) => item.id === taskId)

    if (!task) {
      return null
    }

    task.status = status
    task.updatedAt = getNowLabel()

    return cloneTask(task)
  },

  async validatePublish(taskId: string): Promise<PublishValidationResult> {
    const task = tasks.find((item) => item.id === taskId)

    if (!task) {
      return {
        valid: false,
        errors: ['任务不存在'],
      }
    }

    return validateTaskForPublish(task)
  },

  async getTaskProgress(taskId: string): Promise<TaskProgress | null> {
    const task = tasks.find((item) => item.id === taskId)

    return task ? cloneProgress(task.progress) : null
  },
}
