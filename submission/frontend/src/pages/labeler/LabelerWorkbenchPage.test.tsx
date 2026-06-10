import { fireEvent, render, screen } from '@testing-library/react'
import { MemoryRouter, Route, Routes } from 'react-router'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import type { DynamicFormSchema } from '../../types/dynamicForm'
import type { LabelingQuestion } from '../../types/labeling'
import { useAuthStore } from '../../stores/authStore'
import { useLabelingStore } from '../../stores/labelingStore'
import { LabelerWorkbenchPage } from './LabelerWorkbenchPage'

vi.mock('../../features/dynamic-form/components/DynamicFormRenderer', () => ({
  DynamicFormRenderer: ({ readOnly }: { readOnly?: boolean }) => (
    <div data-read-only={readOnly ? 'true' : 'false'} data-testid="dynamic-form">
      {readOnly ? null : <button type="button">校验当前题</button>}
    </div>
  ),
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

function createQuestion(overrides: Partial<LabelingQuestion> = {}): LabelingQuestion {
  return {
    id: 'question-1',
    taskId: 'task-1',
    assignmentId: 'assignment-1',
    submissionId: 'submission-1',
    templateVersionId: 'template-version-1',
    datasetItemId: 'dataset-item-1',
    title: '题目 1',
    description: '题目描述',
    source: {
      text: '待标注文本',
    },
    schema: createSchema(),
    previousValues: {
      answer: '已提交答案',
    },
    status: 'submitted',
    ...overrides,
  }
}

function renderWorkbench() {
  render(
    <MemoryRouter initialEntries={['/app/labeler/workbench/task-1']}>
      <Routes>
        <Route element={<LabelerWorkbenchPage />} path="/app/labeler/workbench/:taskId" />
      </Routes>
    </MemoryRouter>,
  )
}

describe('LabelerWorkbenchPage', () => {
  const loadWorkbench = vi.fn()
  const loadDraft = vi.fn()
  const loadQuestionHistory = vi.fn()
  const saveDraft = vi.fn()
  const submitQuestionDraft = vi.fn()
  const setCurrentQuestion = vi.fn()

  beforeEach(() => {
    vi.clearAllMocks()

    const question = createQuestion()

    useAuthStore.setState({
      currentUser: {
        id: 'labeler-1',
        name: 'Labeler',
        role: 'LABELER',
        title: '标注员',
      },
      currentRole: 'LABELER',
      isAuthenticated: true,
      loginError: null,
      accessToken: 'access-token',
      refreshToken: 'refresh-token',
      tokenVersion: 1,
    })

    useLabelingStore.setState({
      currentTask: {
        id: 'task-1',
        title: '任务 1',
        description: '任务描述',
        instruction: '任务说明',
        tags: [],
        status: 'in_progress',
        templateId: 'template-1',
        templateName: '模板 1',
        deadline: '2026-06-30',
        rewardText: '10',
        totalQuestions: 1,
        completedQuestions: 1,
      },
      questions: [question],
      currentQuestion: question,
      currentDraft: {
        id: 'draft-1',
        taskId: 'task-1',
        questionId: 'question-1',
        userId: 'labeler-1',
        values: {
          answer: '已提交答案',
        },
        updatedAt: '2026-06-10 10:00',
      },
      reviewSummary: null,
      currentQuestionHistory: {
        taskId: 1,
        datasetItemId: 1,
        histories: [],
      },
      submitValidation: null,
      isWorkbenchLoading: false,
      isDraftSaving: false,
      isSubmitting: false,
      isQuestionHistoryLoading: false,
      error: null,
      loadWorkbench,
      loadDraft,
      loadQuestionHistory,
      saveDraft,
      submitQuestionDraft,
      setCurrentQuestion,
    })
  })

  it('locks save and submit actions for submitted questions', () => {
    renderWorkbench()

    expect(screen.getByRole('button', { name: /保存草稿/ })).toBeDisabled()
    expect(screen.getByRole('button', { name: /提交当前题/ })).toBeDisabled()
    expect(screen.getByTestId('dynamic-form')).toHaveAttribute('data-read-only', 'true')
    expect(screen.queryByRole('button', { name: /校验当前题/ })).not.toBeInTheDocument()

    fireEvent.keyDown(window, { key: 's', ctrlKey: true })
    fireEvent.keyDown(window, { key: 'Enter', ctrlKey: true })

    expect(saveDraft).not.toHaveBeenCalled()
    expect(submitQuestionDraft).not.toHaveBeenCalled()
  })
})
