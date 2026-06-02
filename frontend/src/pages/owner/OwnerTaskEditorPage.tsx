import {
  Alert,
  Button,
  Card,
  Col,
  Input,
  InputNumber,
  Progress,
  Row,
  Select,
  Space,
  Statistic,
  Steps,
  Table,
  Tag,
  Typography,
  Upload,
  message,
} from 'antd'
import { InboxOutlined, SaveOutlined, SendOutlined } from '@ant-design/icons'
import { useEffect, useMemo, useState } from 'react'
import { useNavigate, useParams } from 'react-router'
import { ContentShell } from '../../components/page/ContentShell'
import { PageHeader } from '../../components/page/PageHeader'
import { ownerTemplateService } from '../../services'
import { useOwnerDraftStore } from '../../stores/ownerDraftStore'
import { useOwnerTaskStore } from '../../stores/ownerTaskStore'
import type { DatasetSampleRow } from '../../types/import'
import type { TemplateSummary } from '../../types/template'
import { distributionStrategyLabels, formatCount, getProgressPercent } from '../../utils/ownerTasks'

export function OwnerTaskEditorPage() {
  const navigate = useNavigate()
  const { taskId } = useParams()
  const [messageApi, contextHolder] = message.useMessage()
  const [templates, setTemplates] = useState<TemplateSummary[]>([])
  const draft = useOwnerDraftStore((state) => state.draft)
  const draftId = useOwnerDraftStore((state) => state.draftId)
  const importPreview = useOwnerDraftStore((state) => state.importPreview)
  const uploadedDatasetFile = useOwnerDraftStore((state) => state.uploadedDatasetFile)
  const currentStep = useOwnerDraftStore((state) => state.currentStep)
  const hasUnsavedChanges = useOwnerDraftStore((state) => state.hasUnsavedChanges)
  const isSaving = useOwnerDraftStore((state) => state.isSaving)
  const isLoadingDraft = useOwnerDraftStore((state) => state.isLoading)
  const isUploadingDataset = useOwnerDraftStore((state) => state.isUploadingDataset)
  const validationResult = useOwnerDraftStore((state) => state.validationResult)
  const draftError = useOwnerDraftStore((state) => state.error)
  const resetDraft = useOwnerDraftStore((state) => state.resetDraft)
  const loadFromTask = useOwnerDraftStore((state) => state.loadFromTask)
  const updateDraft = useOwnerDraftStore((state) => state.updateDraft)
  const setStep = useOwnerDraftStore((state) => state.setStep)
  const uploadDatasetFile = useOwnerDraftStore((state) => state.uploadDatasetFile)
  const saveDraft = useOwnerDraftStore((state) => state.saveDraft)
  const validatePublish = useOwnerDraftStore((state) => state.validatePublish)
  const publishDraft = useOwnerDraftStore((state) => state.publishDraft)
  const currentTaskProgress = useOwnerTaskStore((state) => state.currentTaskProgress)
  const currentTaskDetail = useOwnerTaskStore((state) => state.currentTaskDetail)
  const loadTaskDetail = useOwnerTaskStore((state) => state.loadTaskDetail)
  const loadTasks = useOwnerTaskStore((state) => state.loadTasks)

  useEffect(() => {
    void ownerTemplateService.listTemplates().then(setTemplates)
  }, [])

  useEffect(() => {
    if (taskId) {
      void loadFromTask(taskId)
      void loadTaskDetail(taskId)
      return
    }

    void resetDraft()
  }, [loadFromTask, loadTaskDetail, resetDraft, taskId])

  const templateOptions = templates.map((template) => ({
    label: `${template.name} ${template.version}`,
    value: template.currentVersionId,
    disabled: template.status !== 'ready',
  }))
  const isReadonlyTask = Boolean(taskId && currentTaskDetail?.task.status !== 'draft')

  const sampleColumns = useMemo(() => {
    if (!importPreview) {
      return []
    }

    return importPreview.mappings.map((mapping) => ({
      title: mapping.sourceField,
      dataIndex: ['values', mapping.sourceField],
      key: mapping.sourceField,
      render: (value: string | number | boolean | null) => String(value ?? '-'),
    }))
  }, [importPreview])

  const formatFileSize = (fileSize: number) => {
    if (fileSize < 1024) {
      return `${fileSize} B`
    }

    if (fileSize < 1024 * 1024) {
      return `${(fileSize / 1024).toFixed(1)} KB`
    }

    return `${(fileSize / 1024 / 1024).toFixed(1)} MB`
  }

  const uploadDataset = async (file: File) => {
    const uploadedFile = await uploadDatasetFile(file)

    if (!uploadedFile) {
      messageApi.error('数据集文件上传失败')
      return
    }

    messageApi.success('数据集文件已上传')
  }

  const saveCurrentDraft = async () => {
    if (isReadonlyTask) {
      messageApi.warning('只有草稿任务可以编辑')
      return
    }

    const task = await saveDraft()

    if (!task) {
      messageApi.error('草稿保存失败')
      return
    }

    messageApi.success('草稿已保存')
    await loadTasks()

    if (!taskId) {
      navigate(`/app/owner/tasks/${task.id}/edit`, { replace: true })
    }
  }

  const validateCurrentDraft = async () => {
    const result = await validatePublish()

    if (result.valid) {
      messageApi.success('发布校验通过')
    } else {
      messageApi.warning('发布校验未通过')
    }
  }

  const publishCurrentDraft = async () => {
    const task = await publishDraft()

    if (!task) {
      messageApi.error('发布失败，请检查校验结果')
      return
    }

    messageApi.success('任务已发布')
    await loadTasks()
    navigate('/app/owner/tasks')
  }

  const completedPercent = currentTaskProgress ? getProgressPercent(currentTaskProgress) : 0

  return (
    <main className="owner-page">
      {contextHolder}
      <ContentShell>
        <PageHeader
          title={taskId ? '编辑任务' : '创建任务'}
          description="配置基础信息、关联模板、模拟导入数据并完成发布前校验。P0 导入不解析真实文件，使用 Mock 导入结果。"
          extra={
            <>
              {hasUnsavedChanges ? <Tag color="warning">有未保存变更</Tag> : <Tag color="success">已同步</Tag>}
              <Button disabled={isReadonlyTask} icon={<SaveOutlined />} loading={isSaving} onClick={() => void saveCurrentDraft()}>
                保存草稿
              </Button>
              <Button disabled={isReadonlyTask} icon={<SendOutlined />} type="primary" onClick={() => void publishCurrentDraft()}>
                发布任务
              </Button>
            </>
          }
        />
      </ContentShell>

      {draftError ? <Alert message={draftError} showIcon type="error" /> : null}
      {validationResult && !validationResult.valid ? (
        <Alert message="发布校验未通过" description={validationResult.errors.join('；')} showIcon type="warning" />
      ) : null}

      <Card className="owner-step-card">
        <Steps
          current={currentStep}
          items={[
            { title: '基础信息' },
            { title: '数据导入' },
            { title: '预览校验' },
          ]}
          onChange={setStep}
        />
      </Card>

      <Row gutter={[16, 16]}>
        <Col lg={15} xs={24}>
          <Card className="owner-form-card" loading={isLoadingDraft} title="基础信息">
            <div className="owner-form-grid">
              <label className="owner-field">
                <span>任务标题</span>
                <Input value={draft.title} onChange={(event) => updateDraft({ title: event.target.value })} />
              </label>
              <label className="owner-field">
                <span>截止时间</span>
                <Input placeholder="YYYY-MM-DDTHH:mm:ss" value={draft.deadline} onChange={(event) => updateDraft({ deadline: event.target.value })} />
              </label>
              <label className="owner-field">
                <span>任务配额</span>
                <InputNumber min={1} precision={0} value={draft.quota} onChange={(quota) => updateDraft({ quota: quota ?? 1 })} />
              </label>
              <label className="owner-field owner-field--wide">
                <span>任务描述</span>
                <Input.TextArea
                  autoSize={{ minRows: 2, maxRows: 4 }}
                  value={draft.description}
                  onChange={(event) => updateDraft({ description: event.target.value })}
                />
              </label>
              <label className="owner-field owner-field--wide">
                <span>标注说明</span>
                <Input.TextArea
                  autoSize={{ minRows: 3, maxRows: 6 }}
                  value={draft.instruction}
                  onChange={(event) => updateDraft({ instruction: event.target.value })}
                />
              </label>
              <label className="owner-field">
                <span>标签</span>
                <Select mode="tags" value={draft.tags} onChange={(tags) => updateDraft({ tags })} />
              </label>
              <label className="owner-field">
                <span>关联模板</span>
                <Select
                  allowClear
                  options={templateOptions}
                  placeholder="选择模板当前版本"
                  value={draft.publishedTemplateVersionId}
                  onChange={(publishedTemplateVersionId) => updateDraft({ publishedTemplateVersionId: publishedTemplateVersionId ?? null })}
                />
              </label>
              <label className="owner-field">
                <span>审核级别数</span>
                <InputNumber min={1} precision={0} value={draft.reviewLevelCount} onChange={(reviewLevelCount) => updateDraft({ reviewLevelCount: reviewLevelCount ?? 1 })} />
              </label>
              <label className="owner-field">
                <span>奖励单价</span>
                <InputNumber
                  min={0}
                  precision={2}
                  prefix="¥"
                  value={draft.rewardRule.unitPrice}
                  onChange={(unitPrice) =>
                    updateDraft({
                      rewardRule: {
                        ...draft.rewardRule,
                        unitPrice: unitPrice ?? 0,
                      },
                    })
                  }
                />
              </label>
              <label className="owner-field">
                <span>分发策略</span>
                <Select
                  options={Object.entries(distributionStrategyLabels).map(([value, label]) => ({ value, label }))}
                  value={draft.distributionStrategy}
                  onChange={(distributionStrategy) => updateDraft({ distributionStrategy })}
                />
              </label>
              <label className="owner-field owner-field--wide">
                <span>AI 审核 Prompt</span>
                <Input.TextArea
                  autoSize={{ minRows: 2, maxRows: 4 }}
                  value={draft.aiReview.prompt}
                  onChange={(event) => updateDraft({ aiReview: { prompt: event.target.value } })}
                />
              </label>
              <label className="owner-field">
                <span>AI 模型</span>
                <Input value={draft.aiReview.model} onChange={(event) => updateDraft({ aiReview: { model: event.target.value } })} />
              </label>
              <label className="owner-field">
                <span>评分维度</span>
                <Input value={draft.aiReview.rating} onChange={(event) => updateDraft({ aiReview: { rating: event.target.value } })} />
              </label>
            </div>
          </Card>

          <Card className="owner-form-card" title="数据集导入与预览">
            <Upload.Dragger
              accept=".json,.jsonl,.xlsx"
              beforeUpload={(file) => {
                void uploadDataset(file)
                return false
              }}
              disabled={isUploadingDataset}
              maxCount={1}
              showUploadList={false}
            >
              <p className="ant-upload-drag-icon">
                <InboxOutlined />
              </p>
              <p className="ant-upload-text">选择 JSON / JSONL / Excel 文件</p>
              <p className="ant-upload-hint">选择后会上传文件并记录数据集文件 ID。</p>
            </Upload.Dragger>

            {uploadedDatasetFile ? (
              <Alert
                message="数据集文件已上传"
                description={`文件：${uploadedDatasetFile.fileName}；大小：${formatFileSize(uploadedDatasetFile.fileSize)}；类型：${uploadedDatasetFile.contentType || '-'}；文件 ID：${uploadedDatasetFile.fileId}`}
                showIcon
                type="success"
              />
            ) : null}

            {importPreview ? (
              <div className="owner-import-preview">
                <Row gutter={[12, 12]}>
                  <Col md={6} xs={12}>
                    <Statistic title="总行数" value={formatCount(importPreview.totalRows)} />
                  </Col>
                  <Col md={6} xs={12}>
                    <Statistic title="有效行" value={formatCount(importPreview.validRows)} />
                  </Col>
                  <Col md={6} xs={12}>
                    <Statistic title="异常行" value={formatCount(importPreview.invalidRows)} />
                  </Col>
                  <Col md={6} xs={12}>
                    <Statistic title="文件类型" value={importPreview.fileType.toUpperCase()} />
                  </Col>
                </Row>

                <Typography.Title level={5}>字段映射</Typography.Title>
                <Table
                  columns={[
                    { title: '来源字段', dataIndex: 'sourceField' },
                    { title: '目标字段', dataIndex: 'targetField' },
                    {
                      title: '状态',
                      render: (_, mapping) => <Tag color={mapping.matched ? 'success' : 'error'}>{mapping.matched ? '已匹配' : '未匹配'}</Tag>,
                    },
                  ]}
                  dataSource={importPreview.mappings}
                  pagination={false}
                  rowKey="sourceField"
                  size="small"
                />

                <Typography.Title level={5}>样本预览</Typography.Title>
                <Table<DatasetSampleRow>
                  columns={sampleColumns}
                  dataSource={importPreview.samples}
                  pagination={false}
                  rowKey="id"
                  scroll={{ x: true }}
                  size="small"
                />

                <Typography.Title level={5}>导入异常</Typography.Title>
                <Table
                  columns={[
                    { title: '行号', dataIndex: 'row', width: 80 },
                    { title: '字段', dataIndex: 'field', width: 140 },
                    {
                      title: '等级',
                      dataIndex: 'level',
                      width: 100,
                      render: (level) => <Tag color={level === 'blocking' ? 'error' : 'warning'}>{level === 'blocking' ? '阻断' : '警告'}</Tag>,
                    },
                    { title: '原因', dataIndex: 'message' },
                  ]}
                  dataSource={importPreview.issues}
                  locale={{ emptyText: '暂无异常' }}
                  pagination={false}
                  rowKey="id"
                  size="small"
                />
              </div>
            ) : null}
          </Card>
        </Col>

        <Col lg={9} xs={24}>
          <Card className="owner-side-card" title="当前任务进度">
            {currentTaskProgress && draftId ? (
              <Space direction="vertical" size={16}>
                <Progress percent={completedPercent} />
                <div className="owner-progress-grid owner-progress-grid--compact">
                  <Statistic title="总量" value={formatCount(currentTaskProgress.totalItems)} />
                  <Statistic title="已分发" value={formatCount(currentTaskProgress.distributedItems)} />
                  <Statistic title="已完成" value={formatCount(currentTaskProgress.completedItems)} />
                  <Statistic title="待审核" value={formatCount(currentTaskProgress.pendingReviewItems)} />
                  <Statistic title="通过" value={formatCount(currentTaskProgress.approvedItems)} />
                  <Statistic title="驳回" value={formatCount(currentTaskProgress.rejectedItems)} />
                  <Statistic title="异常" value={formatCount(currentTaskProgress.abnormalItems)} />
                </div>
              </Space>
            ) : (
              <Typography.Text type="secondary">草稿未发布，暂无执行进度。</Typography.Text>
            )}
          </Card>

          <Card className="owner-side-card" title="发布校验">
            <Space direction="vertical" size={12}>
              <Typography.Text type="secondary">发布前会检查基础信息、关联模板、导入数据和阻断错误。</Typography.Text>
              <Button block onClick={() => void validateCurrentDraft()}>
                执行发布校验
              </Button>
              {validationResult?.valid ? <Alert message="校验通过，可以发布。" showIcon type="success" /> : null}
            </Space>
          </Card>
        </Col>
      </Row>
    </main>
  )
}
