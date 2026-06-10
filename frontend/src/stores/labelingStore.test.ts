import { beforeEach, describe, expect, it, vi } from 'vitest'
import type { DynamicFormSchema } from '../types/dynamicForm'
import type { LabelingQuestion } from '../types/labeling'
import { labelingService } from '../services'
import { useLabelingStore } from './labelingStore'

vi.mock('../services', () => ({
  ApiError: class ApiError extends Error {},
  labelingService: {
    saveDraft: vi.fn(),
    submitQuestionDraft: vi.fn(),
  },
}))

function createSchema(): DynamicFormSchema {
  return {
    id: 'template-1',
    version: 'v1',
    title: 'Template',
    nodes: [
      {
        id: 'answer-node',
        key: 'answer',
        type: 'input',
        title: 'Answer',
        props: {},
      },
    ],
  }
}

function createSubmittedQuestion(): LabelingQuestion {
  return {
    id: 'question-1',
    taskId: 'task-1',
    assignmentId: 'assignment-1',
    submissionId: 'submission-1',
    templateVersionId: 'template-version-1',
    datasetItemId: 'dataset-item-1',
    title: '题目 1',
    description: '题目描述',
    source: {},
    schema: createSchema(),
    status: 'submitted',
  }
}

describe('labelingStore', () => {
  beforeEach(() => {
    vi.clearAllMocks()

    const question = createSubmittedQuestion()

    useLabelingStore.setState({
      questions: [question],
      currentQuestion: question,
      currentDraft: null,
      submitValidation: null,
      isDraftSaving: false,
      isSubmitting: false,
      error: null,
    })
  })

  it('does not save a draft for an already submitted question', async () => {
    const result = await useLabelingStore.getState().saveDraft({
      taskId: 'task-1',
      questionId: 'question-1',
      userId: 'labeler-1',
      values: {
        answer: 'changed',
      },
    })

    expect(result).toBeNull()
    expect(labelingService.saveDraft).not.toHaveBeenCalled()
    expect(useLabelingStore.getState().currentQuestion?.status).toBe('submitted')
    expect(useLabelingStore.getState().questions[0]?.status).toBe('submitted')
  })
})
