import { Alert, Button, Card, Descriptions, List, Modal, Space, Tag, Typography, message } from 'antd'
import { ArrowLeftOutlined, SaveOutlined, SendOutlined } from '@ant-design/icons'
import { useEffect, useState } from 'react'
import { useNavigate, useParams } from 'react-router'
import { ContentShell } from '../../components/page/ContentShell'
import { PageHeader } from '../../components/page/PageHeader'
import { StatePlaceholder } from '../../components/states/StatePlaceholder'
import { DynamicFormRenderer } from '../../features/dynamic-form/components/DynamicFormRenderer'
import { useAuthStore } from '../../stores/authStore'
import { useLabelingStore } from '../../stores/labelingStore'
import { labelerTaskStatusColors, labelerTaskStatusLabels } from '../../utils/labeling'

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
  const submitTaskDrafts = useLabelingStore((state) => state.submitTaskDrafts)
  const setCurrentQuestion = useLabelingStore((state) => state.setCurrentQuestion)
  const valuesKey = `${currentQuestion?.id ?? ''}:${currentDraft?.id ?? ''}:${currentDraft?.updatedAt ?? ''}`
  const baseValues = currentDraft?.values ?? currentQuestion?.previousValues ?? {}
  const [valueSnapshot, setValueSnapshot] = useState<{ key: string; values: Record<string, unknown>; dirty: boolean }>(() => ({
    key: valuesKey,
    values: baseValues,
    dirty: false,
  }))
  const latestValues = valueSnapshot.key === valuesKey ? valueSnapshot.values : baseValues
  const hasUnsavedChanges = valueSnapshot.key === valuesKey ? valueSnapshot.dirty : false

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
        values: latestValues,
      }).then((draft) => {
        if (draft) {
          setValueSnapshot((snapshot) => (snapshot.key === valuesKey ? { ...snapshot, dirty: false } : snapshot))
        }
      })
    }, 1200)

    return () => window.clearTimeout(timer)
  }, [currentQuestion, currentUser, hasUnsavedChanges, latestValues, saveDraft, taskId, valuesKey])

  const handleSaveDraft = async () => {
    if (!taskId || !currentQuestion || !currentUser) {
      messageApi.error('当前题目不可保存')
      return
    }

    const draft = await saveDraft({
      taskId,
      questionId: currentQuestion.id,
      userId: currentUser.id,
      values: latestValues,
    })

    if (draft) {
      setValueSnapshot((snapshot) => (snapshot.key === valuesKey ? { ...snapshot, dirty: false } : snapshot))
      messageApi.success('草稿已保存')
    } else {
      messageApi.error('草稿保存失败')
    }
  }

  const handleValuesChange = (values: Record<string, unknown>) => {
    setValueSnapshot({ key: valuesKey, values, dirty: true })
  }

  const submitCurrentTask = async () => {
    if (!taskId || !currentUser) {
      messageApi.error('当前任务不可提交')
      return
    }

    if (currentQuestion) {
      await saveDraft({
        taskId,
        questionId: currentQuestion.id,
        userId: currentUser.id,
        values: latestValues,
      })
      setValueSnapshot((snapshot) => (snapshot.key === valuesKey ? { ...snapshot, dirty: false } : snapshot))
    }

    const result = await submitTaskDrafts(taskId, currentUser.id)

    if (result.submission) {
      messageApi.success('任务已提交')
      return
    }

    const firstError = result.validation.errors[0]

    if (firstError?.questionId) {
      setCurrentQuestion(firstError.questionId)
    }

    messageApi.error(firstError?.message ?? '提交校验失败')
  }

  const confirmSubmitTask = () => {
    Modal.confirm({
      title: '提交标注任务',
      content: '提交后任务将进入待审核状态，确认提交当前任务的全部已保存答案吗？',
      okText: '提交',
      cancelText: '取消',
      onOk: () => submitCurrentTask(),
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
                onClick={() => setCurrentQuestion(question.id)}
              >
                <Space direction="vertical" size={4}>
                  <Typography.Text strong>
                    {index + 1}. {question.title}
                  </Typography.Text>
                  <Tag>{question.status === 'draft' ? '已保存草稿' : question.status === 'submitted' ? '已提交' : '未作答'}</Tag>
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
              <DynamicFormRenderer
                initialValues={latestValues}
                llmContext={{
                  taskId: currentTask?.id,
                  templateVersionId: currentQuestion.templateVersionId ?? currentTask?.templateVersionId ?? currentTask?.templateId,
                  datasetItemId: currentQuestion.datasetItemId ?? currentQuestion.id,
                  assignmentId: currentTask?.assignmentId,
                  previewMode: false,
                }}
                schema={currentQuestion.schema}
                submitText="校验当前题"
                onSubmit={(result) => setValueSnapshot({ key: valuesKey, values: result.values, dirty: true })}
                onValuesChange={handleValuesChange}
              />
            </Space>
          ) : (
            <StatePlaceholder status="empty" message="当前任务暂无题目。" />
          )}
        </Card>

        <Card className="labeler-workbench__side" loading={isWorkbenchLoading} title="保存与信息">
          <Space direction="vertical" size={16}>
            <Descriptions column={1} size="small">
              <Descriptions.Item label="任务模板">{currentTask?.templateName ?? '-'}</Descriptions.Item>
              <Descriptions.Item label="截止时间">{currentTask?.deadline ?? '-'}</Descriptions.Item>
              <Descriptions.Item label="题目数量">{questions.length}</Descriptions.Item>
              <Descriptions.Item label="回填来源">
                {currentDraft ? '当前草稿' : currentQuestion?.previousValues ? '上一轮答案' : '空白答案'}
              </Descriptions.Item>
              <Descriptions.Item label="保存状态">
                {isDraftSaving ? '保存中' : hasUnsavedChanges ? '有未保存修改' : '已保存'}
              </Descriptions.Item>
              <Descriptions.Item label="最近保存">{currentDraft?.updatedAt ?? '尚未保存'}</Descriptions.Item>
            </Descriptions>

            {currentQuestion?.previousValues ? (
              <Card className="labeler-history-card" size="small" title="上一轮答案">
                <pre className="labeler-history-card__content">
                  {JSON.stringify(currentQuestion.previousValues, null, 2)}
                </pre>
              </Card>
            ) : null}

            <Button
              block
              icon={<SaveOutlined />}
              loading={isDraftSaving}
              type="primary"
              onClick={() => void handleSaveDraft()}
            >
              保存当前题草稿
            </Button>

            <Button
              block
              icon={<SendOutlined />}
              loading={isSubmitting}
              onClick={confirmSubmitTask}
            >
              提交任务
            </Button>

            <Space className="labeler-workbench__pager">
              <Button
                disabled={!currentQuestion || questions.findIndex((question) => question.id === currentQuestion.id) <= 0}
                onClick={() => {
                  const currentIndex = questions.findIndex((question) => question.id === currentQuestion?.id)
                  const previous = questions[currentIndex - 1]

                  if (previous) {
                    setCurrentQuestion(previous.id)
                  }
                }}
              >
                上一题
              </Button>
              <Button
                disabled={!currentQuestion || questions.findIndex((question) => question.id === currentQuestion.id) >= questions.length - 1}
                onClick={() => {
                  const currentIndex = questions.findIndex((question) => question.id === currentQuestion?.id)
                  const next = questions[currentIndex + 1]

                  if (next) {
                    setCurrentQuestion(next.id)
                  }
                }}
              >
                下一题
              </Button>
            </Space>
          </Space>
        </Card>
      </div>
    </main>
  )
}
