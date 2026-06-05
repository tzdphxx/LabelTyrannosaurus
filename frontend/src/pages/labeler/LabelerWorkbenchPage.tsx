import { Alert, Button, Card, Descriptions, List, Modal, Space, Tag, Timeline, Typography, message } from 'antd'
import { ArrowLeftOutlined, SaveOutlined, SendOutlined } from '@ant-design/icons'
import { useEffect, useMemo, useState } from 'react'
import { useNavigate, useParams } from 'react-router'
import { ContentShell } from '../../components/page/ContentShell'
import { PageHeader } from '../../components/page/PageHeader'
import { StatePlaceholder } from '../../components/states/StatePlaceholder'
import { DynamicFormRenderer } from '../../features/dynamic-form/components/DynamicFormRenderer'
import { useAuthStore } from '../../stores/authStore'
import { useLabelingStore } from '../../stores/labelingStore'
import {
  labelerTaskStatusColors,
  labelerTaskStatusLabels,
  labelingQuestionStatusColors,
  labelingQuestionStatusLabels,
} from '../../utils/labeling'

export function LabelerWorkbenchPage() {
  const { taskId } = useParams()
  const navigate = useNavigate()
  const [messageApi, contextHolder] = message.useMessage()
  const currentUser = useAuthStore((state) => state.currentUser)
  const currentTask = useLabelingStore((state) => state.currentTask)
  const questions = useLabelingStore((state) => state.questions)
  const currentQuestion = useLabelingStore((state) => state.currentQuestion)
  const currentDraft = useLabelingStore((state) => state.currentDraft)
  const reviewSummary = useLabelingStore((state) => state.reviewSummary)
  const submitValidation = useLabelingStore((state) => state.submitValidation)
  const isWorkbenchLoading = useLabelingStore((state) => state.isWorkbenchLoading)
  const isDraftSaving = useLabelingStore((state) => state.isDraftSaving)
  const isSubmitting = useLabelingStore((state) => state.isSubmitting)
  const error = useLabelingStore((state) => state.error)
  const loadWorkbench = useLabelingStore((state) => state.loadWorkbench)
  const loadDraft = useLabelingStore((state) => state.loadDraft)
  const saveDraft = useLabelingStore((state) => state.saveDraft)
  const submitQuestionDraft = useLabelingStore((state) => state.submitQuestionDraft)
  const setCurrentQuestion = useLabelingStore((state) => state.setCurrentQuestion)
  const [latestValues, setLatestValues] = useState<Record<string, unknown>>({})
  const [hasUnsavedChanges, setHasUnsavedChanges] = useState(false)
  const [editingQuestionId, setEditingQuestionId] = useState<string | null>(null)
  const formInitialValues = useMemo(
    () => currentDraft?.values ?? currentQuestion?.previousValues ?? {},
    [currentDraft?.values, currentQuestion?.previousValues],
  )
  const effectiveValues =
    hasUnsavedChanges && editingQuestionId === currentQuestion?.id
      ? latestValues
      : formInitialValues
  const currentQuestionIndex = currentQuestion
    ? questions.findIndex((question) => question.id === currentQuestion.id)
    : -1
  const currentQuestionStatus = currentQuestion
    ? hasUnsavedChanges
      ? 'in_progress'
      : currentQuestion.status
    : 'pending'
  const isCurrentSchemaEmpty = currentQuestion ? currentQuestion.schema.nodes.length === 0 : false
  const canGoPrevious = currentQuestionIndex > 0
  const canGoNext = currentQuestionIndex >= 0 && currentQuestionIndex < questions.length - 1
  const answerSource = currentDraft ? '当前草稿' : currentQuestion?.previousValues ? '上一轮答案' : '空白答案'
  const flowItems = currentQuestion
    ? [
      {
        color: 'blue',
        children: `题目状态：${labelingQuestionStatusLabels[currentQuestionStatus]}`,
      },
      ...(currentQuestion.previousValues
        ? [
          {
            color: 'red',
            children: reviewSummary
              ? `已打回：${reviewSummary.reason}`
              : '已打回：存在上一轮待修正答案',
          },
        ]
        : []),
      ...(currentDraft
        ? [
          {
            color: 'gold',
            children: `草稿保存：${currentDraft.updatedAt}`,
          },
        ]
        : []),
      ...(currentQuestion.status === 'submitted'
        ? [
          {
            color: 'green',
            children: '当前题已提交',
          },
        ]
        : []),
    ]
    : []

  useEffect(() => {
    if (taskId) {
      void loadWorkbench(taskId)
    }
  }, [loadWorkbench, taskId])

  useEffect(() => {
    if (taskId && currentQuestion && currentUser) {
      void loadDraft(taskId, currentQuestion.id, currentUser.id)
    }
  }, [currentQuestion, currentUser, loadDraft, taskId])

  useEffect(() => {
    if (!taskId || !currentQuestion || !currentUser || !hasUnsavedChanges) {
      return
    }

    const timer = window.setTimeout(() => {
      void saveDraft({
        taskId,
        questionId: currentQuestion.id,
        userId: currentUser.id,
        values: effectiveValues,
      }).then((draft) => {
        if (draft) {
          setHasUnsavedChanges(false)
        }
      })
    }, 1200)

    return () => window.clearTimeout(timer)
  }, [currentQuestion, currentUser, effectiveValues, hasUnsavedChanges, saveDraft, taskId])

  const handleSaveDraft = async () => {
    if (!taskId || !currentQuestion || !currentUser) {
      messageApi.error('当前题目不可保存')
      return
    }

    const draft = await saveDraft({
      taskId,
      questionId: currentQuestion.id,
      userId: currentUser.id,
      values: effectiveValues,
    })

    if (draft) {
      setHasUnsavedChanges(false)
      messageApi.success('草稿已保存')
    } else {
      messageApi.error('草稿保存失败')
    }
  }

  const handleValuesChange = (values: Record<string, unknown>) => {
    setLatestValues(values)
    setEditingQuestionId(currentQuestion?.id ?? null)
    setHasUnsavedChanges(true)
  }

  const selectQuestion = (questionId: string) => {
    setHasUnsavedChanges(false)
    setEditingQuestionId(null)
    setCurrentQuestion(questionId)
  }

  const submitCurrentQuestion = async () => {
    if (!taskId || !currentQuestion || !currentUser) {
      messageApi.error('当前题目不可提交')
      return
    }

    await saveDraft({
      taskId,
      questionId: currentQuestion.id,
      userId: currentUser.id,
      values: effectiveValues,
    })
    setHasUnsavedChanges(false)

    const result = await submitQuestionDraft(taskId, currentQuestion.id, currentUser.id)

    if (result.submission) {
      messageApi.success('当前题目已提交')
      return
    }

    const firstError = result.validation.errors[0]

    if (firstError?.questionId) {
      selectQuestion(firstError.questionId)
    }

    messageApi.error(firstError?.message ?? '提交校验失败')
  }

  const confirmSubmitQuestion = () => {
    Modal.confirm({
      title: '提交当前题目',
      content: '提交后仅当前题进入已提交状态，其他题目不会被提交。确认提交当前题吗？',
      okText: '提交',
      cancelText: '取消',
      onOk: () => submitCurrentQuestion(),
    })
  }

  if (!taskId) {
    return <StatePlaceholder status="empty" message="缺少任务 ID，无法打开标注工作台。" />
  }

  if (!isWorkbenchLoading && !currentTask) {
    return <StatePlaceholder status="empty" message="未找到对应的标注任务。" />
  }

  return (
    <main className="labeler-page">
      {contextHolder}
      <ContentShell className="labeler-hero">
        <PageHeader
          title={currentTask?.title ?? '标注工作台'}
          description={currentTask?.instruction ?? '按题目完成 schema 表单作答，并保存当前草稿。'}
          meta={
            currentTask ? (
              <Tag color={labelerTaskStatusColors[currentTask.status]}>{labelerTaskStatusLabels[currentTask.status]}</Tag>
            ) : null
          }
          extra={
            <Button icon={<ArrowLeftOutlined />} onClick={() => navigate('/app/labeler/market')}>
              返回任务广场
            </Button>
          }
        />
      </ContentShell>

      {error ? <Alert message={error} showIcon type="error" /> : null}
      {reviewSummary ? (
        <Alert
          message={`打回原因：${reviewSummary.reason}`}
          description={`上一轮审核意见：${reviewSummary.comment}`}
          showIcon
          type="warning"
        />
      ) : null}
      {submitValidation && !submitValidation.valid ? (
        <Alert
          message="提交校验未通过"
          description={submitValidation.errors.map((item) => `${item.questionTitle || '任务'}：${item.message}`).join('；')}
          showIcon
          type="error"
        />
      ) : null}

      <div className="labeler-workbench">
        <Card className="labeler-workbench__nav" loading={isWorkbenchLoading} title="题目导航">
          <List
            dataSource={questions}
            renderItem={(question, index) => (
              <List.Item
                className={question.id === currentQuestion?.id ? 'labeler-question-item labeler-question-item--active' : 'labeler-question-item'}
                onClick={() => selectQuestion(question.id)}
              >
                <Space direction="vertical" size={4}>
                  <Typography.Text strong>
                    {index + 1}. {question.title}
                  </Typography.Text>
                  <Tag color={labelingQuestionStatusColors[question.id === currentQuestion?.id ? currentQuestionStatus : question.status]}>
                    {labelingQuestionStatusLabels[question.id === currentQuestion?.id ? currentQuestionStatus : question.status]}
                  </Tag>
                </Space>
              </List.Item>
            )}
          />
        </Card>

        <Card className="labeler-workbench__form" loading={isWorkbenchLoading} title={currentQuestion?.title ?? '当前题目'}>
          {currentQuestion ? (
            <Space className="labeler-workbench__form-content" direction="vertical" size={16}>
              <Descriptions bordered column={1} size="small" title="题目材料">
                {Object.entries(currentQuestion.source).map(([label, value]) => (
                  <Descriptions.Item key={label} label={label}>
                    {value}
                  </Descriptions.Item>
                ))}
              </Descriptions>
              {isCurrentSchemaEmpty ? (
                <Alert
                  message="模板加载失败或暂无可渲染字段"
                  description="已展示题目材料，请稍后刷新重试。"
                  showIcon
                  type="warning"
                />
              ) : (
                <DynamicFormRenderer
                  initialValues={formInitialValues}
                  schema={currentQuestion.schema}
                  submitText="校验当前题"
                  onSubmit={(result) => setLatestValues(result.values)}
                  onValuesChange={handleValuesChange}
                />
              )}
              <div className="labeler-workbench__actions">
                <Space className="labeler-workbench__pager">
                  <Button
                    disabled={!canGoPrevious}
                    onClick={() => {
                      const previous = questions[currentQuestionIndex - 1]

                      if (previous) {
                        selectQuestion(previous.id)
                      }
                    }}
                  >
                    上一题
                  </Button>
                  <Button
                    disabled={!canGoNext}
                    onClick={() => {
                      const next = questions[currentQuestionIndex + 1]

                      if (next) {
                        selectQuestion(next.id)
                      }
                    }}
                  >
                    下一题
                  </Button>
                </Space>
                <Space className="labeler-workbench__submit-actions">
                  <Button
                    disabled={isCurrentSchemaEmpty}
                    icon={<SaveOutlined />}
                    loading={isDraftSaving}
                    onClick={() => void handleSaveDraft()}
                  >
                    保存草稿
                  </Button>
                  <Button
                    disabled={isCurrentSchemaEmpty}
                    icon={<SendOutlined />}
                    loading={isSubmitting}
                    type="primary"
                    onClick={confirmSubmitQuestion}
                  >
                    提交当前题
                  </Button>
                </Space>
              </div>
            </Space>
          ) : (
            <StatePlaceholder status="empty" message="当前任务暂无题目。" />
          )}
        </Card>

        <Card className="labeler-workbench__side" loading={isWorkbenchLoading} title="题目状态与流程">
          <Space direction="vertical" size={16}>
            <Descriptions column={1} size="small">
              <Descriptions.Item label="当前题目">{currentQuestion?.title ?? '-'}</Descriptions.Item>
              <Descriptions.Item label="题目状态">
                <Tag color={labelingQuestionStatusColors[currentQuestionStatus]}>
                  {labelingQuestionStatusLabels[currentQuestionStatus]}
                </Tag>
              </Descriptions.Item>
              <Descriptions.Item label="回填来源">{answerSource}</Descriptions.Item>
              <Descriptions.Item label="保存状态">
                {isDraftSaving ? '保存中' : hasUnsavedChanges ? '有未保存修改' : '已保存'}
              </Descriptions.Item>
              <Descriptions.Item label="最近保存">{currentDraft?.updatedAt ?? '尚未保存'}</Descriptions.Item>
            </Descriptions>

            <Timeline className="labeler-question-flow" items={flowItems} />

            {currentQuestion?.previousValues ? (
              <Card className="labeler-history-card" size="small" title="上一轮答案">
                <pre className="labeler-history-card__content">
                  {JSON.stringify(currentQuestion.previousValues, null, 2)}
                </pre>
              </Card>
            ) : null}
          </Space>
        </Card>
      </div>
    </main>
  )
}
