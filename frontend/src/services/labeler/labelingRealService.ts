import type { DynamicFormSchema, DynamicFormSubmitResult } from '../../types/dynamicForm'
import type {
  LabelerSubmissionStats,
  LabelerTaskListQuery,
  LabelerTaskStatus,
  LabelerTaskSummary,
  LabelingDraft,
  LabelingQuestion,
  LabelingReviewSummary,
  LabelingSubmission,
  LabelingSubmitResult,
  LabelingSubmitValidationResult,
} from '../../types/labeling'
import { request } from '../http'
import { mockLabelingService } from './labelingService'
import { getNowLabel, validateQuestionDraft, validateTaskDrafts } from './labelingServiceHelpers'

interface MarketTaskResponse {
  taskId: number
  title: string
  description?: string
  tags?: string[]
  quota?: number
  remainingQuota?: number
  deadlineAt?: string
  reward?: string
  status?: string
  strategy?: string
  availableCount?: number
  currentUserClaimedCount?: number
  rewardSummary?: {
    amount?: number
    unit?: string
    text?: string
  }
}

interface AssignmentClaimResponse {
  assignmentId: number
  taskId?: number
  datasetItemId: number
  templateVersionId: number
  status?: string
  schemaJson?: string
  itemJson?: string
  draftAnswerJson?: string
  draftVersion?: number
}

interface AssignmentDetailResponse {
  assignmentId: number
  taskId: number
  datasetItemId: number
  itemList?: unknown[]
  itemJson?: string
  templateVersionId: number
  schemaJson?: string
  status?: string
  draftAnswerJson?: string
  draftVersion?: number
  updatedAt?: string
}

interface LabelerAssignmentListItem {
  assignmentId: number
  taskId: number
  taskTitle?: string
  datasetItemId: number
  status: string
  draftVersion?: number
  claimedAt?: string
  returnedAt?: string | null
  updatedAt?: string
}

interface AssignmentDraftResponse {
  assignmentId: number
  draftAnswerJson?: string
  draftVersion?: number
  status?: string
  updatedAt?: string
}

interface SubmissionSubmitResponse {
  submissionId: number
  assignmentId: number
  versionNo?: number
  status?: string
}

interface CachedAssignmentContext {
  assignmentId: string
  taskId: string
  datasetItemId?: string
  templateVersionId?: string
  task?: LabelerTaskSummary
  question?: LabelingQuestion
  draft?: LabelingDraft
  draftVersion?: number
  status?: string
  claimedAt?: string
  returnedAt?: string | null
  updatedAt?: string
}

const assignmentCacheKey = 'labelhub-real-labeler-assignments'
const assignmentCache = readAssignmentCache()

function readAssignmentCache(): Record<string, CachedAssignmentContext> {
  if (typeof window === 'undefined') {
    return {}
  }

  try {
    const rawValue = window.sessionStorage.getItem(assignmentCacheKey)
    const parsedValue = rawValue ? JSON.parse(rawValue) : {}

    return parsedValue && typeof parsedValue === 'object' && !Array.isArray(parsedValue)
      ? parsedValue as Record<string, CachedAssignmentContext>
      : {}
  } catch {
    return {}
  }
}

function persistAssignmentCache() {
  if (typeof window === 'undefined') {
    return
  }

  window.sessionStorage.setItem(assignmentCacheKey, JSON.stringify(assignmentCache))
}

function cacheAssignment(context: CachedAssignmentContext) {
  assignmentCache[context.assignmentId] = {
    ...assignmentCache[context.assignmentId],
    ...context,
  }
  persistAssignmentCache()

  return assignmentCache[context.assignmentId]
}

function findAssignmentByTaskId(taskId: string) {
  return Object.values(assignmentCache).find((context) => context.taskId === taskId)
}

function parseJsonValue(value?: string | null): unknown {
  if (!value) {
    return null
  }

  try {
    return JSON.parse(value)
  } catch {
    return null
  }
}

function toRecord(value: unknown): Record<string, unknown> {
  return value && typeof value === 'object' && !Array.isArray(value) ? value as Record<string, unknown> : {}
}

function formatValue(value: unknown) {
  if (value === null || value === undefined) {
    return ''
  }

  if (typeof value === 'string') {
    return value
  }

  if (typeof value === 'number' || typeof value === 'boolean') {
    return String(value)
  }

  return JSON.stringify(value)
}

function parseSchema(schemaJson?: string, templateVersionId?: number | string): DynamicFormSchema {
  const parsedSchema = toRecord(parseJsonValue(schemaJson))
  const parsedNodes = Array.isArray(parsedSchema.nodes) ? parsedSchema.nodes : []

  return {
    id: String(parsedSchema.id ?? templateVersionId ?? 'template'),
    version: String(parsedSchema.version ?? templateVersionId ?? '1'),
    title: String(parsedSchema.title ?? '标注模板'),
    nodes: parsedNodes as DynamicFormSchema['nodes'],
  }
}

function parseItemList(detail: AssignmentDetailResponse | AssignmentClaimResponse) {
  if ('itemList' in detail && Array.isArray(detail.itemList)) {
    return detail.itemList
  }

  const parsedItem = parseJsonValue(detail.itemJson)

  return Array.isArray(parsedItem) ? parsedItem : parsedItem ? [parsedItem] : []
}

function buildSourceRecord(itemList: unknown[]): Record<string, string> {
  if (itemList.length === 0) {
    return {}
  }

  return itemList.reduce<Record<string, string>>((source, item, index) => {
    if (item && typeof item === 'object' && !Array.isArray(item)) {
      Object.entries(item as Record<string, unknown>).forEach(([key, value]) => {
        const sourceKey = source[key] === undefined ? key : `${key} ${index + 1}`
        source[sourceKey] = formatValue(value)
      })

      return source
    }

    source[`材料 ${index + 1}`] = formatValue(item)

    return source
  }, {})
}

function formatDateTime(value?: string | null) {
  if (!value) {
    return '-'
  }

  return value.replace('T', ' ').slice(0, 16)
}

function buildRewardText(task: MarketTaskResponse) {
  if (task.reward) {
    return task.reward
  }

  if (task.rewardSummary?.text) {
    return task.rewardSummary.text
  }

  if (task.rewardSummary?.amount !== undefined) {
    return `${task.rewardSummary.amount}${task.rewardSummary.unit ?? ''}`
  }

  return '-'
}

function mapMarketStatus(task: MarketTaskResponse): LabelerTaskStatus {
  const remainingQuota = task.remainingQuota ?? task.availableCount ?? 0

  if (remainingQuota <= 0) {
    return 'ended'
  }

  return 'available'
}

function mapAssignmentTaskStatus(status?: string): LabelerTaskStatus {
  switch (status) {
    case 'CLAIMED':
      return 'claimed'
    case 'DRAFTING':
      return 'in_progress'
    case 'RETURNED':
      return 'rejected'
    case 'SUBMITTED':
      return 'submitted'
    case 'APPROVED':
      return 'approved'
    case 'CANCELLED':
      return 'ended'
    default:
      return 'claimed'
  }
}

function mapQuestionStatus(status?: string): LabelingQuestion['status'] {
  switch (status) {
    case 'DRAFTING':
      return 'draft'
    case 'RETURNED':
      return 'rejected'
    case 'SUBMITTED':
    case 'APPROVED':
      return 'submitted'
    case 'CLAIMED':
    default:
      return 'pending'
  }
}

function mapSubmissionStatus(status?: string): LabelingSubmission['status'] {
  if (status === 'APPROVED') {
    return 'approved'
  }

  if (status === 'RETURNED' || status === 'REJECTED') {
    return 'rejected'
  }

  return 'submitted'
}

function buildTaskSummaryFromMarket(task: MarketTaskResponse): LabelerTaskSummary {
  const totalQuestions = task.quota ?? task.availableCount ?? 0
  const remainingQuota = task.remainingQuota ?? task.availableCount ?? 0

  return {
    id: String(task.taskId),
    title: task.title,
    description: task.description ?? '',
    instruction: task.description ?? '',
    tags: task.tags ?? [],
    status: mapMarketStatus(task),
    templateId: '',
    templateName: '-',
    deadline: formatDateTime(task.deadlineAt),
    rewardText: buildRewardText(task),
    totalQuestions,
    completedQuestions: Math.max(totalQuestions - remainingQuota, 0),
  }
}

function buildTaskSummaryFromAssignment(assignment: LabelerAssignmentListItem, existingTask?: LabelerTaskSummary): LabelerTaskSummary {
  const status = mapAssignmentTaskStatus(assignment.status)

  return {
    id: String(assignment.taskId),
    title: assignment.taskTitle ?? existingTask?.title ?? `任务 #${assignment.taskId}`,
    description: existingTask?.description ?? '',
    instruction: existingTask?.instruction ?? existingTask?.description ?? '',
    tags: existingTask?.tags ?? [],
    status,
    templateId: existingTask?.templateId ?? '',
    templateName: existingTask?.templateName ?? '-',
    deadline: existingTask?.deadline ?? '-',
    rewardText: existingTask?.rewardText ?? '-',
    totalQuestions: existingTask?.totalQuestions ?? 1,
    completedQuestions: status === 'submitted' || status === 'approved' ? 1 : existingTask?.completedQuestions ?? 0,
    claimedAt: formatDateTime(assignment.claimedAt),
    reviewedAt: assignment.returnedAt ? formatDateTime(assignment.returnedAt) : undefined,
  }
}

function buildQuestion(detail: AssignmentDetailResponse | AssignmentClaimResponse, taskId: string): LabelingQuestion {
  const assignmentId = String(detail.assignmentId)
  const datasetItemId = String(detail.datasetItemId)
  const templateVersionId = detail.templateVersionId
  const itemList = parseItemList(detail)

  return {
    id: assignmentId,
    taskId,
    title: `题目 #${datasetItemId}`,
    description: '',
    source: buildSourceRecord(itemList),
    schema: parseSchema(detail.schemaJson, templateVersionId),
    status: mapQuestionStatus(detail.status),
  }
}

function buildDraft(
  taskId: string,
  questionId: string,
  userId: string,
  values: Record<string, unknown>,
  updatedAt?: string,
): LabelingDraft {
  return {
    id: `draft-${questionId}`,
    taskId,
    questionId,
    userId,
    values,
    updatedAt: formatDateTime(updatedAt) === '-' ? getNowLabel() : formatDateTime(updatedAt),
  }
}

function buildSubmission(
  task: LabelerTaskSummary,
  response: SubmissionSubmitResponse,
  answer: DynamicFormSubmitResult,
  userId: string,
): LabelingSubmission {
  return {
    id: String(response.submissionId),
    taskId: task.id,
    taskTitle: task.title,
    userId,
    status: mapSubmissionStatus(response.status),
    submittedAt: getNowLabel(),
    answers: [
      {
        ...answer,
        values: { ...answer.values },
      },
    ],
  }
}

function matchesQuery(task: LabelerTaskSummary, query: LabelerTaskListQuery) {
  const keyword = query.keyword.trim().toLowerCase()
  const matchesKeyword =
    keyword.length === 0 ||
    task.title.toLowerCase().includes(keyword) ||
    task.description.toLowerCase().includes(keyword) ||
    task.tags.some((tag) => tag.toLowerCase().includes(keyword))
  const matchesTag = query.tag === 'all' || task.tags.includes(query.tag)
  const matchesStatus = query.status === 'all' || task.status === query.status

  return matchesKeyword && matchesTag && matchesStatus
}

function normalizeListResponse<T>(response: T[] | { list?: T[]; records?: T[]; content?: T[]; data?: T[] }) {
  if (Array.isArray(response)) {
    return response
  }

  return response.list ?? response.records ?? response.content ?? response.data ?? []
}

async function listAssignments(taskId?: string) {
  const response = await request.get<LabelerAssignmentListItem[] | { list?: LabelerAssignmentListItem[]; records?: LabelerAssignmentListItem[]; content?: LabelerAssignmentListItem[]; data?: LabelerAssignmentListItem[] }>(
    '/v1/labeler/assignments',
    {
      params: {
        taskId,
        page: 1,
        size: 100,
      },
    },
  )
  const assignments = normalizeListResponse(response)

  assignments.forEach((assignment) => {
    cacheAssignment({
      assignmentId: String(assignment.assignmentId),
      taskId: String(assignment.taskId),
      datasetItemId: String(assignment.datasetItemId),
      draftVersion: assignment.draftVersion,
      status: assignment.status,
      claimedAt: assignment.claimedAt,
      returnedAt: assignment.returnedAt,
      updatedAt: assignment.updatedAt,
    })
  })

  return assignments
}

async function resolveAssignmentForTask(taskId: string) {
  const cachedAssignment = findAssignmentByTaskId(taskId)

  if (cachedAssignment) {
    return cachedAssignment
  }

  const assignments = await listAssignments(taskId)
  const assignment = assignments.find((item) => String(item.taskId) === taskId)

  return assignment ? assignmentCache[String(assignment.assignmentId)] : null
}

async function loadAssignmentDetail(assignmentId: string, taskId?: string) {
  const detail = await request.get<AssignmentDetailResponse>(`/v1/assignments/${assignmentId}`)
  const resolvedTaskId = String(detail.taskId ?? taskId ?? assignmentCache[assignmentId]?.taskId)
  const question = buildQuestion(detail, resolvedTaskId)
  const draftValues = toRecord(parseJsonValue(detail.draftAnswerJson))
  const draft = buildDraft(resolvedTaskId, question.id, '', draftValues, detail.updatedAt)
  const context = cacheAssignment({
    assignmentId,
    taskId: resolvedTaskId,
    datasetItemId: String(detail.datasetItemId),
    templateVersionId: String(detail.templateVersionId),
    question,
    draft,
    draftVersion: detail.draftVersion,
    status: detail.status,
    updatedAt: detail.updatedAt,
  })

  return context
}

async function ensureAssignmentDetailForTask(taskId: string) {
  const assignment = await resolveAssignmentForTask(taskId)

  if (!assignment) {
    return null
  }

  if (assignment.question) {
    return assignment
  }

  return loadAssignmentDetail(assignment.assignmentId, taskId)
}

export const realLabelingService = {
  async listMarketTasks(query: LabelerTaskListQuery): Promise<LabelerTaskSummary[]> {
    const params = {
      keyword: query.keyword || undefined,
      tag: query.tag === 'all' ? undefined : query.tag,
      status: query.status === 'available' || query.status === 'all' ? 'PUBLISHED' : undefined,
    }
    const marketResponse = await request.get<MarketTaskResponse[]>('/v1/market/tasks', { params })
    const assignments = await listAssignments().catch(() => [])
    const taskMap = new Map<string, LabelerTaskSummary>()

    marketResponse.map(buildTaskSummaryFromMarket).forEach((task) => {
      taskMap.set(task.id, task)
    })

    assignments.forEach((assignment) => {
      const taskId = String(assignment.taskId)
      const task = buildTaskSummaryFromAssignment(assignment, taskMap.get(taskId))
      taskMap.set(taskId, task)
      cacheAssignment({
        ...assignmentCache[String(assignment.assignmentId)],
        assignmentId: String(assignment.assignmentId),
        taskId,
        task,
      })
    })

    return Array.from(taskMap.values()).filter((task) => matchesQuery(task, query))
  },

  async listTags(): Promise<string[]> {
    const marketTasks = await request.get<MarketTaskResponse[]>('/v1/market/tasks', {
      params: {
        status: 'PUBLISHED',
      },
    })

    return Array.from(new Set(marketTasks.flatMap((task) => task.tags ?? []))).sort((first, second) =>
      first.localeCompare(second, 'zh-CN'),
    )
  },

  async getTaskDetail(taskId: string): Promise<LabelerTaskSummary | null> {
    const cachedAssignment = findAssignmentByTaskId(taskId)

    if (cachedAssignment?.task) {
      return {
        ...cachedAssignment.task,
        tags: [...cachedAssignment.task.tags],
      }
    }

    const tasks = await this.listMarketTasks({
      keyword: '',
      tag: 'all',
      status: 'all',
    })

    return tasks.find((task) => task.id === taskId) ?? null
  },

  async claimTask(taskId: string): Promise<LabelerTaskSummary | null> {
    const claimResponse = await request.post<AssignmentClaimResponse>(`/v1/tasks/${taskId}/assignments/claim`)
    const assignmentId = String(claimResponse.assignmentId)
    const question = claimResponse.schemaJson ? buildQuestion(claimResponse, taskId) : undefined
    const task = await this.getTaskDetail(taskId)
    const nextTask: LabelerTaskSummary = task
      ? {
          ...task,
          status: 'claimed',
          claimedAt: getNowLabel(),
        }
      : {
          id: taskId,
          title: `任务 #${taskId}`,
          description: '',
          instruction: '',
          tags: [],
          status: 'claimed',
          templateId: '',
          templateName: '-',
          deadline: '-',
          rewardText: '-',
          totalQuestions: 1,
          completedQuestions: 0,
          claimedAt: getNowLabel(),
        }

    cacheAssignment({
      assignmentId,
      taskId,
      datasetItemId: String(claimResponse.datasetItemId),
      templateVersionId: String(claimResponse.templateVersionId),
      task: nextTask,
      question,
      draft: question
        ? buildDraft(taskId, question.id, '', toRecord(parseJsonValue(claimResponse.draftAnswerJson)))
        : undefined,
      draftVersion: claimResponse.draftVersion,
      status: claimResponse.status ?? 'CLAIMED',
      claimedAt: getNowLabel(),
    })

    return nextTask
  },

  async listQuestions(taskId: string): Promise<LabelingQuestion[]> {
    const assignment = await ensureAssignmentDetailForTask(taskId)

    return assignment?.question ? [{ ...assignment.question, source: { ...assignment.question.source } }] : []
  },

  async getQuestion(questionId: string): Promise<LabelingQuestion | null> {
    const assignment = assignmentCache[questionId] ?? await loadAssignmentDetail(questionId)

    return assignment.question ? { ...assignment.question, source: { ...assignment.question.source } } : null
  },

  async getDraft(taskId: string, questionId: string, userId: string): Promise<LabelingDraft | null> {
    const assignment = assignmentCache[questionId] ?? await ensureAssignmentDetailForTask(taskId)

    if (!assignment) {
      return null
    }

    const draftResponse = await request.get<AssignmentDraftResponse>(`/v1/assignments/${assignment.assignmentId}/draft`).catch(() => null)

    if (!draftResponse) {
      return assignment.draft
        ? {
            ...assignment.draft,
            userId,
          }
        : null
    }
    const draft = buildDraft(taskId, assignment.assignmentId, userId, toRecord(parseJsonValue(draftResponse.draftAnswerJson)), draftResponse.updatedAt)

    cacheAssignment({
      ...assignment,
      draft,
      draftVersion: draftResponse.draftVersion,
      status: draftResponse.status ?? assignment.status,
      updatedAt: draftResponse.updatedAt,
    })

    return draft
  },

  async saveDraft(payload: Omit<LabelingDraft, 'id' | 'updatedAt'>): Promise<LabelingDraft> {
    const assignment = assignmentCache[payload.questionId] ?? await ensureAssignmentDetailForTask(payload.taskId)
    const assignmentId = assignment?.assignmentId ?? payload.questionId
    const draftResponse = await request.put<AssignmentDraftResponse, { answerJson: string; clientVersion: number }>(
      `/v1/assignments/${assignmentId}/draft`,
      {
        answerJson: JSON.stringify(payload.values),
        clientVersion: assignment?.draftVersion ?? 0,
      },
    )
    const draft = buildDraft(payload.taskId, assignmentId, payload.userId, payload.values, draftResponse.updatedAt)

    cacheAssignment({
      ...(assignment ?? {
        assignmentId,
        taskId: payload.taskId,
      }),
      draft,
      draftVersion: draftResponse.draftVersion,
      status: draftResponse.status ?? 'DRAFTING',
      updatedAt: draftResponse.updatedAt,
    })

    return draft
  },

  async submitAnswers(taskId: string, userId: string, answers: DynamicFormSubmitResult[]): Promise<LabelingSubmission | null> {
    const assignment = await ensureAssignmentDetailForTask(taskId)

    if (!assignment || !answers[0]) {
      return null
    }

    await this.saveDraft({
      taskId,
      questionId: assignment.assignmentId,
      userId,
      values: answers[0].values,
    })

    const result = await this.submitQuestionDraft(taskId, assignment.assignmentId, userId)

    return result.submission
  },

  async validateSubmission(taskId: string, userId: string): Promise<LabelingSubmitValidationResult> {
    const assignment = await ensureAssignmentDetailForTask(taskId)
    const questions = assignment?.question ? [assignment.question] : []
    const drafts = assignment?.draft ? [{ ...assignment.draft, userId }] : []

    return validateTaskDrafts(questions, drafts, taskId, userId)
  },

  async submitTaskDrafts(taskId: string, userId: string): Promise<LabelingSubmitResult> {
    const assignment = await ensureAssignmentDetailForTask(taskId)

    if (!assignment) {
      return {
        submission: null,
        validation: {
          valid: false,
          errors: [
            {
              questionId: '',
              questionTitle: '',
              message: '当前任务没有可提交的题目',
            },
          ],
        },
      }
    }

    return this.submitQuestionDraft(taskId, assignment.assignmentId, userId)
  },

  async submitQuestionDraft(taskId: string, questionId: string, userId: string): Promise<LabelingSubmitResult> {
    const assignment = assignmentCache[questionId] ?? await ensureAssignmentDetailForTask(taskId)
    const question = assignment?.question
    const draft = assignment?.draft ? { ...assignment.draft, userId } : null
    const validation = validateQuestionDraft(question ? [question] : [], draft ? [draft] : [], taskId, questionId, userId)

    if (!assignment || !question || !draft || !validation.valid) {
      return {
        submission: null,
        validation,
      }
    }

    const answer: DynamicFormSubmitResult = {
      templateId: question.schema.id,
      schemaVersion: question.schema.version,
      values: { ...draft.values },
    }
    const submitResponse = await request.post<SubmissionSubmitResponse, { answerJson: string; clientVersion: number }>(
      `/v1/assignments/${assignment.assignmentId}/submit`,
      {
        answerJson: JSON.stringify(draft.values),
        clientVersion: assignment.draftVersion ?? 0,
      },
    )
    const task = assignment.task ?? await this.getTaskDetail(taskId) ?? {
      id: taskId,
      title: `任务 #${taskId}`,
      description: '',
      instruction: '',
      tags: [],
      status: 'submitted' as const,
      templateId: '',
      templateName: '-',
      deadline: '-',
      rewardText: '-',
      totalQuestions: 1,
      completedQuestions: 1,
    }
    const submission = buildSubmission(task, submitResponse, answer, userId)

    cacheAssignment({
      ...assignment,
      question: {
        ...question,
        status: 'submitted',
      },
      task: {
        ...task,
        status: submission.status,
        completedQuestions: Math.max(task.completedQuestions, 1),
        submittedAt: submission.submittedAt,
      },
      status: submitResponse.status ?? 'SUBMITTED',
    })

    return {
      submission,
      validation,
    }
  },

  async getReviewSummary(taskId: string): Promise<LabelingReviewSummary | null> {
    void taskId

    return null
  },

  async getSubmissionStats(): Promise<LabelerSubmissionStats> {
    return mockLabelingService.getSubmissionStats()
  },

  async listSubmissions(): Promise<LabelingSubmission[]> {
    return mockLabelingService.listSubmissions()
  },
}
