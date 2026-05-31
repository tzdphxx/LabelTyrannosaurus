import {
  mockLabelerTasks,
  mockLabelingDrafts,
  mockLabelingQuestions,
  mockLabelingReviews,
  mockLabelingSubmissions,
} from '../../mocks'
import type {
  LabelerSubmissionStats,
  LabelerTaskListQuery,
  LabelerTaskSummary,
  LabelingDraft,
  LabelingQuestion,
  LabelingReviewSummary,
  LabelingSubmission,
  LabelingSubmitResult,
  LabelingSubmitValidationError,
  LabelingSubmitValidationResult,
} from '../../types/labeling'
import type { DynamicFormSubmitResult, DynamicSchemaNode } from '../../types/dynamicForm'

const tasks: LabelerTaskSummary[] = mockLabelerTasks.map(cloneTask)
const questions: LabelingQuestion[] = mockLabelingQuestions.map(cloneQuestion)
const draftStorageKey = 'labelhub-labeling-drafts'
const drafts: LabelingDraft[] = mergeDrafts(mockLabelingDrafts.map(cloneDraft), readStoredDrafts())
const submissions: LabelingSubmission[] = mockLabelingSubmissions.map(cloneSubmission)
const reviews: LabelingReviewSummary[] = mockLabelingReviews.map((review) => ({ ...review }))

function getNowLabel() {
  return new Intl.DateTimeFormat('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    hour12: false,
  })
    .format(new Date())
    .replace(/\//g, '-')
}

function cloneTask(task: LabelerTaskSummary): LabelerTaskSummary {
  return {
    ...task,
    tags: [...task.tags],
  }
}

function cloneQuestion(question: LabelingQuestion): LabelingQuestion {
  return {
    ...question,
    source: { ...question.source },
    previousValues: question.previousValues ? { ...question.previousValues } : undefined,
    schema: {
      ...question.schema,
      nodes: question.schema.nodes.map((node) => ({ ...node })),
    },
  }
}

function cloneDraft(draft: LabelingDraft): LabelingDraft {
  return {
    ...draft,
    values: { ...draft.values },
  }
}

function cloneAnswer(answer: DynamicFormSubmitResult): DynamicFormSubmitResult {
  return {
    ...answer,
    values: { ...answer.values },
  }
}

function cloneSubmission(submission: LabelingSubmission): LabelingSubmission {
  return {
    ...submission,
    answers: submission.answers.map(cloneAnswer),
  }
}

function readStoredDrafts(): LabelingDraft[] {
  if (typeof window === 'undefined') {
    return []
  }

  try {
    const rawValue = window.localStorage.getItem(draftStorageKey)
    const parsedValue = rawValue ? JSON.parse(rawValue) : []

    return Array.isArray(parsedValue) ? parsedValue.map(cloneDraft) : []
  } catch {
    return []
  }
}

function persistDrafts() {
  if (typeof window === 'undefined') {
    return
  }

  window.localStorage.setItem(draftStorageKey, JSON.stringify(drafts))
}

function mergeDrafts(baseDrafts: LabelingDraft[], storedDrafts: LabelingDraft[]) {
  const mergedDrafts = [...baseDrafts]

  storedDrafts.forEach((storedDraft) => {
    const index = mergedDrafts.findIndex(
      (draft) =>
        draft.taskId === storedDraft.taskId &&
        draft.questionId === storedDraft.questionId &&
        draft.userId === storedDraft.userId,
    )

    if (index >= 0) {
      mergedDrafts[index] = storedDraft
    } else {
      mergedDrafts.push(storedDraft)
    }
  })

  return mergedDrafts
}

function matchesTaskQuery(task: LabelerTaskSummary, query: LabelerTaskListQuery) {
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

function getTaskIndex(taskId: string) {
  return tasks.findIndex((task) => task.id === taskId)
}

function isRequiredNode(node: DynamicSchemaNode) {
  return node.rules?.some((rule) => rule.type === 'required') ?? false
}

function isEmptyValue(value: unknown) {
  return value === undefined || value === null || value === '' || (Array.isArray(value) && value.length === 0)
}

function collectRequiredNodes(nodes: DynamicSchemaNode[]): DynamicSchemaNode[] {
  return nodes.flatMap((node) => {
    const currentNodes = isRequiredNode(node) ? [node] : []
    const childNodes = node.children ? collectRequiredNodes(node.children) : []

    return [...currentNodes, ...childNodes]
  })
}

function getTaskDrafts(taskId: string, userId: string) {
  return drafts.filter((draft) => draft.taskId === taskId && draft.userId === userId)
}

function validateTaskDrafts(taskId: string, userId: string): LabelingSubmitValidationResult {
  const taskQuestions = questions.filter((question) => question.taskId === taskId)
  const errors: LabelingSubmitValidationError[] = []

  taskQuestions.forEach((question) => {
    const draft = drafts.find((item) => item.taskId === taskId && item.questionId === question.id && item.userId === userId)

    if (!draft) {
      errors.push({
        questionId: question.id,
        questionTitle: question.title,
        message: '该题尚未保存草稿',
      })
      return
    }

    collectRequiredNodes(question.schema.nodes).forEach((node) => {
      if (isEmptyValue(draft.values[node.key])) {
        errors.push({
          questionId: question.id,
          questionTitle: question.title,
          fieldKey: node.key,
          fieldTitle: node.title,
          message: `${node.title}不能为空`,
        })
      }
    })
  })

  return {
    valid: errors.length === 0,
    errors,
  }
}

export const labelingService = {
  async listMarketTasks(query: LabelerTaskListQuery): Promise<LabelerTaskSummary[]> {
    return tasks.filter((task) => matchesTaskQuery(task, query)).map(cloneTask)
  },

  async listTags(): Promise<string[]> {
    return Array.from(new Set(tasks.flatMap((task) => task.tags))).sort((first, second) => first.localeCompare(second, 'zh-CN'))
  },

  async getTaskDetail(taskId: string): Promise<LabelerTaskSummary | null> {
    const task = tasks.find((item) => item.id === taskId)

    return task ? cloneTask(task) : null
  },

  async claimTask(taskId: string): Promise<LabelerTaskSummary | null> {
    const taskIndex = getTaskIndex(taskId)

    if (taskIndex < 0) {
      return null
    }

    const task = tasks[taskIndex]

    if (task.status === 'available') {
      tasks[taskIndex] = {
        ...task,
        status: 'claimed',
        claimedAt: getNowLabel(),
      }
    }

    return cloneTask(tasks[taskIndex])
  },

  async listQuestions(taskId: string): Promise<LabelingQuestion[]> {
    return questions.filter((question) => question.taskId === taskId).map(cloneQuestion)
  },

  async getQuestion(questionId: string): Promise<LabelingQuestion | null> {
    const question = questions.find((item) => item.id === questionId)

    return question ? cloneQuestion(question) : null
  },

  async getDraft(taskId: string, questionId: string, userId: string): Promise<LabelingDraft | null> {
    const draft = drafts.find((item) => item.taskId === taskId && item.questionId === questionId && item.userId === userId)

    return draft ? cloneDraft(draft) : null
  },

  async saveDraft(payload: Omit<LabelingDraft, 'id' | 'updatedAt'>): Promise<LabelingDraft> {
    const draftIndex = drafts.findIndex(
      (draft) => draft.taskId === payload.taskId && draft.questionId === payload.questionId && draft.userId === payload.userId,
    )
    const nextDraft: LabelingDraft = {
      ...payload,
      id: draftIndex >= 0 ? drafts[draftIndex].id : `draft-${Date.now()}`,
      values: { ...payload.values },
      updatedAt: getNowLabel(),
    }

    if (draftIndex >= 0) {
      drafts[draftIndex] = nextDraft
    } else {
      drafts.push(nextDraft)
    }

    persistDrafts()

    return cloneDraft(nextDraft)
  },

  async submitAnswers(taskId: string, userId: string, answers: DynamicFormSubmitResult[]): Promise<LabelingSubmission | null> {
    const taskIndex = getTaskIndex(taskId)

    if (taskIndex < 0) {
      return null
    }

    const task = tasks[taskIndex]
    const submission: LabelingSubmission = {
      id: `submission-${Date.now()}`,
      taskId,
      taskTitle: task.title,
      userId,
      status: 'submitted',
      submittedAt: getNowLabel(),
      answers: answers.map(cloneAnswer),
    }

    submissions.unshift(submission)
    tasks[taskIndex] = {
      ...task,
      status: 'submitted',
      completedQuestions: task.totalQuestions,
      submittedAt: submission.submittedAt,
    }

    return cloneSubmission(submission)
  },

  async validateSubmission(taskId: string, userId: string): Promise<LabelingSubmitValidationResult> {
    return validateTaskDrafts(taskId, userId)
  },

  async submitTaskDrafts(taskId: string, userId: string): Promise<LabelingSubmitResult> {
    const validation = validateTaskDrafts(taskId, userId)

    if (!validation.valid) {
      return {
        submission: null,
        validation,
      }
    }

    const taskIndex = getTaskIndex(taskId)

    if (taskIndex < 0) {
      return {
        submission: null,
        validation: {
          valid: false,
          errors: [
            {
              questionId: '',
              questionTitle: '',
              message: '任务不存在',
            },
          ],
        },
      }
    }

    const task = tasks[taskIndex]
    const taskDrafts = getTaskDrafts(taskId, userId)
    const answers = questions
      .filter((question) => question.taskId === taskId)
      .map((question) => {
        const draft = taskDrafts.find((item) => item.questionId === question.id)

        return {
          templateId: question.schema.id,
          schemaVersion: question.schema.version,
          values: { ...(draft?.values ?? {}) },
        }
      })
    const submission: LabelingSubmission = {
      id: `submission-${Date.now()}`,
      taskId,
      taskTitle: task.title,
      userId,
      status: 'submitted',
      submittedAt: getNowLabel(),
      answers,
    }

    submissions.unshift(submission)
    tasks[taskIndex] = {
      ...task,
      status: 'submitted',
      completedQuestions: task.totalQuestions,
      submittedAt: submission.submittedAt,
    }

    questions.forEach((question, index) => {
      if (question.taskId === taskId) {
        questions[index] = {
          ...question,
          status: 'submitted',
        }
      }
    })

    return {
      submission: cloneSubmission(submission),
      validation,
    }
  },

  async getReviewSummary(taskId: string): Promise<LabelingReviewSummary | null> {
    const review = reviews.find((item) => item.taskId === taskId)

    return review ? { ...review } : null
  },

  async getSubmissionStats(): Promise<LabelerSubmissionStats> {
    return {
      submitted: submissions.filter((submission) => submission.status === 'submitted').length,
      approved: submissions.filter((submission) => submission.status === 'approved').length,
      rejected: submissions.filter((submission) => submission.status === 'rejected').length,
      needsRevision: tasks.filter((task) => task.status === 'rejected').length,
      inProgress: tasks.filter((task) => task.status === 'claimed' || task.status === 'in_progress').length,
    }
  },

  async listSubmissions(): Promise<LabelingSubmission[]> {
    return submissions.map(cloneSubmission)
  },
}
