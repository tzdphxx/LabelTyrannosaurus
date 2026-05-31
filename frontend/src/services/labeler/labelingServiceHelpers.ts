import type { DynamicFormSubmitResult, DynamicSchemaNode } from '../../types/dynamicForm'
import type {
  LabelerTaskListQuery,
  LabelerTaskSummary,
  LabelingDraft,
  LabelingQuestion,
  LabelingSubmission,
  LabelingSubmitValidationError,
  LabelingSubmitValidationResult,
} from '../../types/labeling'
import type { AiReviewProcessingResult } from '../../types/review'

export function getNowLabel() {
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

export function cloneTask(task: LabelerTaskSummary): LabelerTaskSummary {
  return {
    ...task,
    tags: [...task.tags],
  }
}

export function cloneQuestion(question: LabelingQuestion): LabelingQuestion {
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

export function cloneDraft(draft: LabelingDraft): LabelingDraft {
  return {
    ...draft,
    values: { ...draft.values },
  }
}

export function cloneAnswer(answer: DynamicFormSubmitResult): DynamicFormSubmitResult {
  return {
    ...answer,
    values: { ...answer.values },
  }
}

export function cloneSubmission(submission: LabelingSubmission): LabelingSubmission {
  return {
    ...submission,
    answers: submission.answers.map(cloneAnswer),
  }
}

export function readStoredDrafts(storageKey: string): LabelingDraft[] {
  if (typeof window === 'undefined') {
    return []
  }

  try {
    const rawValue = window.localStorage.getItem(storageKey)
    const parsedValue = rawValue ? JSON.parse(rawValue) : []

    return Array.isArray(parsedValue) ? parsedValue.map(cloneDraft) : []
  } catch {
    return []
  }
}

export function persistDrafts(storageKey: string, drafts: LabelingDraft[]) {
  if (typeof window === 'undefined') {
    return
  }

  window.localStorage.setItem(storageKey, JSON.stringify(drafts))
}

export function mergeDrafts(baseDrafts: LabelingDraft[], storedDrafts: LabelingDraft[]) {
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

export function matchesTaskQuery(task: LabelerTaskSummary, query: LabelerTaskListQuery) {
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

export function getTaskDrafts(drafts: LabelingDraft[], taskId: string, userId: string) {
  return drafts.filter((draft) => draft.taskId === taskId && draft.userId === userId)
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

export function validateTaskDrafts(
  questions: LabelingQuestion[],
  drafts: LabelingDraft[],
  taskId: string,
  userId: string,
): LabelingSubmitValidationResult {
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

export function applyAiReviewToSubmission(
  submission: LabelingSubmission,
  reviewResult: AiReviewProcessingResult,
): LabelingSubmission {
  const { aiReview, submissionReviewStatus } = reviewResult

  if (aiReview.decision === 'pass') {
    return {
      ...submission,
      status: 'approved',
      reviewedAt: aiReview.reviewedAt,
      reviewSource: 'ai',
      reviewStatus: submissionReviewStatus,
      aiDecision: aiReview.decision,
      aiReviewSummary: aiReview.summary,
      reviewComment: aiReview.summary,
    }
  }

  if (aiReview.decision === 'reject') {
    return {
      ...submission,
      status: 'rejected',
      reviewedAt: aiReview.reviewedAt,
      rejectReason: aiReview.rejectReason,
      reviewSource: 'ai',
      reviewStatus: submissionReviewStatus,
      aiDecision: aiReview.decision,
      aiReviewSummary: aiReview.summary,
      reviewComment: aiReview.recommendedAction,
    }
  }

  return {
    ...submission,
    status: 'submitted',
    reviewStatus: submissionReviewStatus,
    aiDecision: aiReview.decision,
    aiReviewSummary: aiReview.summary,
    reviewComment: aiReview.manualReviewReason,
  }
}

export function getTaskStatusFromSubmission(submission: LabelingSubmission): LabelerTaskSummary['status'] {
  if (submission.status === 'approved') {
    return 'approved'
  }

  if (submission.status === 'rejected') {
    return 'rejected'
  }

  return 'submitted'
}
