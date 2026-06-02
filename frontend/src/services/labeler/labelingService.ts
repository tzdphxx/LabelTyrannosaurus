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
  LabelingSubmitValidationResult,
} from '../../types/labeling'
import type { DynamicFormSubmitResult } from '../../types/dynamicForm'
import type { ReviewOutcomeSyncPayload } from '../../types/review'
import { reviewService } from '../review/reviewService'
import {
  applyAiReviewToSubmission,
  cloneDraft,
  cloneQuestion,
  cloneSubmission,
  cloneTask,
  getNowLabel,
  getTaskDrafts,
  getTaskStatusFromSubmission,
  matchesTaskQuery,
  mergeDrafts,
  persistDrafts,
  readStoredDrafts,
  validateQuestionDraft,
  validateTaskDrafts,
} from './labelingServiceHelpers'

const tasks: LabelerTaskSummary[] = mockLabelerTasks.map(cloneTask)
const questions: LabelingQuestion[] = mockLabelingQuestions.map(cloneQuestion)
const draftStorageKey = 'labelhub-labeling-drafts'
const drafts: LabelingDraft[] = mergeDrafts(mockLabelingDrafts.map(cloneDraft), readStoredDrafts(draftStorageKey))
const submissions: LabelingSubmission[] = mockLabelingSubmissions.map(cloneSubmission)
const reviews: LabelingReviewSummary[] = mockLabelingReviews.map((review) => ({ ...review }))

function getTaskIndex(taskId: string) {
  return tasks.findIndex((task) => task.id === taskId)
}

async function runAiReviewForSubmission(task: LabelerTaskSummary, submission: LabelingSubmission) {
  const reviewResult = await reviewService.processSubmissionWithAi(submission, {
    task,
    questions: questions.filter((question) => question.taskId === submission.taskId),
  })

  return applyAiReviewToSubmission(submission, reviewResult)
}

function updateTaskAfterSubmission(taskIndex: number, submission: LabelingSubmission) {
  const task = tasks[taskIndex]

  tasks[taskIndex] = {
    ...task,
    status: getTaskStatusFromSubmission(submission),
    completedQuestions: task.totalQuestions,
    submittedAt: submission.submittedAt,
    reviewedAt: submission.reviewedAt,
    rejectReason: submission.rejectReason,
    reviewSource: submission.reviewSource,
    reviewStatus: submission.reviewStatus,
    aiDecision: submission.aiDecision,
    aiReviewSummary: submission.aiReviewSummary,
  }
}

function createSubmission(task: LabelerTaskSummary, userId: string, answers: DynamicFormSubmitResult[]): LabelingSubmission {
  return {
    id: `submission-${Date.now()}`,
    taskId: task.id,
    taskTitle: task.title,
    userId,
    status: 'submitted',
    submittedAt: getNowLabel(),
    answers: answers.map((answer) => ({
      ...answer,
      values: { ...answer.values },
    })),
  }
}

async function submitTaskAnswers(taskId: string, userId: string, answers: DynamicFormSubmitResult[]) {
  const taskIndex = getTaskIndex(taskId)

  if (taskIndex < 0) {
    return null
  }

  const submission = createSubmission(tasks[taskIndex], userId, answers)
  const reviewedSubmission = await runAiReviewForSubmission(tasks[taskIndex], submission)

  submissions.unshift(reviewedSubmission)
  updateTaskAfterSubmission(taskIndex, reviewedSubmission)

  return cloneSubmission(reviewedSubmission)
}

function applyReviewOutcomeToLabelingState(payload: ReviewOutcomeSyncPayload) {
  const submissionIndex = submissions.findIndex((submission) => submission.id === payload.submissionId)

  if (submissionIndex < 0) {
    return
  }

  const submission = submissions[submissionIndex]
  const updatedSubmission: LabelingSubmission = {
    ...submission,
    status: payload.status,
    reviewedAt: payload.reviewedAt,
    rejectReason: payload.rejectReason,
    reviewComment: payload.reviewComment,
    reviewSource: payload.reviewSource,
    reviewStatus: payload.reviewStatus,
  }
  const taskIndex = getTaskIndex(submission.taskId)

  submissions[submissionIndex] = updatedSubmission

  if (taskIndex >= 0) {
    tasks[taskIndex] = {
      ...tasks[taskIndex],
      status: payload.status,
      reviewedAt: payload.reviewedAt,
      rejectReason: payload.rejectReason,
      reviewSource: payload.reviewSource,
      reviewStatus: payload.reviewStatus,
    }
  }
}

reviewService.registerReviewOutcomeSync(applyReviewOutcomeToLabelingState)

export const mockLabelingService = {
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

    persistDrafts(draftStorageKey, drafts)

    return cloneDraft(nextDraft)
  },

  async submitAnswers(taskId: string, userId: string, answers: DynamicFormSubmitResult[]): Promise<LabelingSubmission | null> {
    return submitTaskAnswers(taskId, userId, answers)
  },

  async validateSubmission(taskId: string, userId: string): Promise<LabelingSubmitValidationResult> {
    return validateTaskDrafts(questions, drafts, taskId, userId)
  },

  async submitTaskDrafts(taskId: string, userId: string): Promise<LabelingSubmitResult> {
    const validation = validateTaskDrafts(questions, drafts, taskId, userId)

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

    const taskDrafts = getTaskDrafts(drafts, taskId, userId)
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
    const reviewedSubmission = await submitTaskAnswers(taskId, userId, answers)

    questions.forEach((question, index) => {
      if (question.taskId === taskId) {
        questions[index] = {
          ...question,
          status: 'submitted',
        }
      }
    })

    return {
      submission: reviewedSubmission,
      validation,
    }
  },

  async submitQuestionDraft(taskId: string, questionId: string, userId: string): Promise<LabelingSubmitResult> {
    const validation = validateQuestionDraft(questions, drafts, taskId, questionId, userId)

    if (!validation.valid) {
      return {
        submission: null,
        validation,
      }
    }

    const taskIndex = getTaskIndex(taskId)
    const question = questions.find((item) => item.taskId === taskId && item.id === questionId)

    if (taskIndex < 0 || !question) {
      return {
        submission: null,
        validation: {
          valid: false,
          errors: [
            {
              questionId,
              questionTitle: question?.title ?? '',
              message: '当前题目不可提交',
            },
          ],
        },
      }
    }

    const draft = drafts.find((item) => item.taskId === taskId && item.questionId === questionId && item.userId === userId)
    const answer: DynamicFormSubmitResult = {
      templateId: question.schema.id,
      schemaVersion: question.schema.version,
      values: { ...(draft?.values ?? {}) },
    }
    const reviewedSubmission = await submitTaskAnswers(taskId, userId, [answer])
    const questionIndex = questions.findIndex((item) => item.id === questionId)

    if (questionIndex >= 0) {
      questions[questionIndex] = {
        ...questions[questionIndex],
        status: 'submitted',
      }
    }

    const taskQuestions = questions.filter((item) => item.taskId === taskId)
    const completedQuestions = taskQuestions.filter((item) => item.status === 'submitted').length

    tasks[taskIndex] = {
      ...tasks[taskIndex],
      status: completedQuestions > 0 ? 'in_progress' : tasks[taskIndex].status,
      completedQuestions,
    }

    return {
      submission: reviewedSubmission,
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
