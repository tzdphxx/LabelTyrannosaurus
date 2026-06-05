import {
  Alert,
  Button,
  Card,
  Col,
  DatePicker,
  Input,
  InputNumber,
  Progress,
  Row,
  Select,
  Space,
  Statistic,
  Switch,
  Table,
  Tag,
  Typography,
  Upload,
  message,
} from 'antd'
import { CheckOutlined, CloseOutlined, InboxOutlined, PlusOutlined, SaveOutlined, SendOutlined } from '@ant-design/icons'
import dayjs from 'dayjs'
import { useCallback, useEffect, useMemo, useRef, useState } from 'react'
import { useNavigate, useParams } from 'react-router'
import { ContentShell } from '../../components/page/ContentShell'
import { PageHeader } from '../../components/page/PageHeader'
import { ownerModelService, ownerTemplateService } from '../../services'
import { useOwnerDraftStore } from '../../stores/ownerDraftStore'
import { useOwnerTaskStore } from '../../stores/ownerTaskStore'
import type { DatasetSampleRow } from '../../types/import'
import type { DatasetItemResponse, OwnerModelOptionResponse } from '../../types/task'
import type { TemplateSummary } from '../../types/template'
import { distributionStrategyLabels, formatCount, getProgressPercent } from '../../utils/ownerTasks'

type DatasetDraftRow = {
  rowType: 'draft'
  rowKey: 'draft'
  externalId: string
  itemJson: Record<string, unknown>
}

type DatasetTableRow = DatasetItemResponse | DatasetDraftRow

function isDatasetDraftRow(row: DatasetTableRow): row is DatasetDraftRow {
  return 'rowType' in row
}

export function OwnerTaskEditorPage() {
  const navigate = useNavigate()
  const { taskId } = useParams()
  const [messageApi, contextHolder] = message.useMessage()
  const [templates, setTemplates] = useState<TemplateSummary[]>([])
  const [isLoadingTemplates, setIsLoadingTemplates] = useState(false)
  const [modelOptions, setModelOptions] = useState<OwnerModelOptionResponse[]>([])
  const [isLoadingModels, setIsLoadingModels] = useState(false)
  const isLoadingTemplatesRef = useRef(false)
  const isLoadingModelsRef = useRef(false)
  const draft = useOwnerDraftStore((state) => state.draft)
  const draftId = useOwnerDraftStore((state) => state.draftId)
  const importPreview = useOwnerDraftStore((state) => state.importPreview)
  const uploadedDatasetFile = useOwnerDraftStore((state) => state.uploadedDatasetFile)
  const hasUnsavedChanges = useOwnerDraftStore((state) => state.hasUnsavedChanges)
  const isSaving = useOwnerDraftStore((state) => state.isSaving)
  const isLoadingDraft = useOwnerDraftStore((state) => state.isLoading)
  const isUploadingDataset = useOwnerDraftStore((state) => state.isUploadingDataset)
  const validationResult = useOwnerDraftStore((state) => state.validationResult)
  const draftError = useOwnerDraftStore((state) => state.error)
  const resetDraft = useOwnerDraftStore((state) => state.resetDraft)
  const loadFromTask = useOwnerDraftStore((state) => state.loadFromTask)
  const updateDraft = useOwnerDraftStore((state) => state.updateDraft)
  const uploadDatasetFile = useOwnerDraftStore((state) => state.uploadDatasetFile)
  const saveDraft = useOwnerDraftStore((state) => state.saveDraft)
  const validatePublish = useOwnerDraftStore((state) => state.validatePublish)
  const publishDraft = useOwnerDraftStore((state) => state.publishDraft)
  const currentTaskProgress = useOwnerTaskStore((state) => state.currentTaskProgress)
  const currentTaskDetail = useOwnerTaskStore((state) => state.currentTaskDetail)
  const currentDatasetItemsPage = useOwnerTaskStore((state) => state.currentDatasetItemsPage)
  const isDatasetItemsLoading = useOwnerTaskStore((state) => state.isDatasetItemsLoading)
  const isAppendingDatasetItems = useOwnerTaskStore((state) => state.isAppendingDatasetItems)
  const loadTaskDetail = useOwnerTaskStore((state) => state.loadTaskDetail)
  const loadTaskDatasetItems = useOwnerTaskStore((state) => state.loadTaskDatasetItems)
  const appendTaskDatasetItems = useOwnerTaskStore((state) => state.appendTaskDatasetItems)
  const loadTasks = useOwnerTaskStore((state) => state.loadTasks)
  const [draftDatasetItem, setDraftDatasetItem] = useState<Record<string, string> | null>(null)
  const [draftDatasetExternalId, setDraftDatasetExternalId] = useState('')

  const loadTemplateOptions = useCallback(async () => {
    if (isLoadingTemplatesRef.current) {
      return
    }

    isLoadingTemplatesRef.current = true
    setIsLoadingTemplates(true)

    try {
      setTemplates(await ownerTemplateService.listTemplates())
    } catch {
      messageApi.error('模板列表加载失败')
    } finally {
      isLoadingTemplatesRef.current = false
      setIsLoadingTemplates(false)
    }
  }, [messageApi])

  const loadModelOptions = useCallback(async () => {
    if (isLoadingModelsRef.current) {
      return
    }

    isLoadingModelsRef.current = true
    setIsLoadingModels(true)

    try {
      setModelOptions(await ownerModelService.listModelOptions())
    } catch {
      messageApi.error('大模型列表加载失败')
    } finally {
      isLoadingModelsRef.current = false
      setIsLoadingModels(false)
    }
  }, [messageApi])

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
  }))
  const modelSelectOptions = modelOptions.map((option) => ({
    label: option.defaultModel,
    value: String(option.id),
  }))
  const isReadonlyTask = Boolean(taskId && currentTaskDetail?.task.status !== 'draft')
  const deadlineValue = draft.deadline && dayjs(draft.deadline).isValid() ? dayjs(draft.deadline) : null
  const ratingDimensions = draft.aiReview.aiScoringDimensions

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
  const datasetFieldNames = useMemo(() => {
    const fieldNames = new Set<string>()

    currentDatasetItemsPage?.items.forEach((item) => {
      Object.keys(item.itemJson).forEach((key) => fieldNames.add(key))
    })

    const names = Array.from(fieldNames)

    return names.length ? names : ['text']
  }, [currentDatasetItemsPage?.items])
  const formatDatasetItemTime = (value: string) => value?.replace('T', ' ').slice(0, 19) || '-'
  const formatDatasetValue = (value: unknown) => {
    if (value === null || value === undefined || value === '') {
      return '-'
    }

    return typeof value === 'object' ? JSON.stringify(value) : String(value)
  }
  const getDatasetFieldTone = (index: number) => `owner-dataset-field--tone-${(index % 6) + 1}`
  const datasetTableRows = useMemo<DatasetTableRow[]>(() => {
    const items = currentDatasetItemsPage?.items ?? []

    if (!draftDatasetItem) {
      return items
    }

    return [
      {
        rowType: 'draft',
        rowKey: 'draft',
        externalId: draftDatasetExternalId,
        itemJson: draftDatasetItem,
      },
      ...items,
    ]
  }, [currentDatasetItemsPage?.items, draftDatasetExternalId, draftDatasetItem])
  const datasetColumns = useMemo(() => [
    {
      title: 'externalId',
      dataIndex: 'externalId',
      width: 190,
      fixed: 'left' as const,
      render: (_: unknown, row: DatasetTableRow) =>
        isDatasetDraftRow(row) ? (
          <Input
            placeholder="留空自动生成"
            size="small"
            value={draftDatasetExternalId}
            onChange={(event) => setDraftDatasetExternalId(event.target.value)}
          />
        ) : (
          <Typography.Text strong>{row.externalId || `#${row.itemId}`}</Typography.Text>
        ),
    },
    ...datasetFieldNames.map((field, index) => ({
      title: <Tag className={getDatasetFieldTone(index)}>{field}</Tag>,
      dataIndex: ['itemJson', field],
      minWidth: 180,
      render: (_: unknown, row: DatasetTableRow) =>
        isDatasetDraftRow(row) ? (
          <Input
            placeholder={`填写 ${field}`}
            size="small"
            value={String(draftDatasetItem?.[field] ?? '')}
            onChange={(event) => updateDatasetItemDraft(field, event.target.value)}
          />
        ) : (
          <Typography.Text>{formatDatasetValue(row.itemJson[field])}</Typography.Text>
        ),
    })),
    {
      title: '状态',
      width: 110,
      render: (_: unknown, row: DatasetTableRow) => (isDatasetDraftRow(row) ? <Tag color="blue">新增</Tag> : <Tag>{row.itemStatus || '-'}</Tag>),
    },
    {
      title: '更新时间',
      width: 170,
      render: (_: unknown, row: DatasetTableRow) =>
        isDatasetDraftRow(row) ? '-' : <Typography.Text type="secondary">{formatDatasetItemTime(row.updatedAt)}</Typography.Text>,
    },
    {
      title: '统计',
      width: 210,
      render: (_: unknown, row: DatasetTableRow) =>
        isDatasetDraftRow(row) ? (
          '-'
        ) : (
          <Space className="owner-dataset-stats" size={8} wrap>
            <span>分发 {formatCount(row.assignedCount)}</span>
            <span>提交 {formatCount(row.submittedCount)}</span>
            <span>通过 {formatCount(row.approvedCount)}</span>
          </Space>
        ),
    },
    {
      title: '操作',
      width: 110,
      fixed: 'right' as const,
      render: (_: unknown, row: DatasetTableRow) =>
        isDatasetDraftRow(row) ? (
          <Space size={6}>
            <Button
              icon={<CheckOutlined />}
              loading={isAppendingDatasetItems}
              size="small"
              type="primary"
              onClick={() => void submitDatasetItemDraft()}
            />
            <Button
              disabled={isAppendingDatasetItems}
              icon={<CloseOutlined />}
              size="small"
              onClick={cancelDatasetItemDraft}
            />
          </Space>
        ) : null,
    },
  ], [
    datasetFieldNames,
    draftDatasetExternalId,
    draftDatasetItem,
    isAppendingDatasetItems,
  ])

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

  function startDatasetItemDraft() {
    setDraftDatasetExternalId('')
    setDraftDatasetItem(Object.fromEntries(datasetFieldNames.map((field) => [field, ''])))
  }

  function updateDatasetItemDraft(field: string, value: string) {
    setDraftDatasetItem((current) => (current ? { ...current, [field]: value } : current))
  }

  function cancelDatasetItemDraft() {
    setDraftDatasetExternalId('')
    setDraftDatasetItem(null)
  }

  async function submitDatasetItemDraft() {
    if (!taskId || !draftDatasetItem) {
      return
    }

    const externalId = draftDatasetExternalId.trim() || `manual-${Date.now()}`
    const results = await appendTaskDatasetItems(taskId, [
      {
        externalId,
        itemJson: Object.fromEntries(datasetFieldNames.map((field) => [field, draftDatasetItem[field] ?? ''])),
        metadataJson: {},
      },
    ])

    if (!results) {
      messageApi.error('题目添加失败')
      return
    }

    const failedResult = results.find((result) => !result.success)

    if (failedResult) {
      messageApi.error(failedResult.errorMessage || '题目添加失败')
      return
    }

    messageApi.success('题目已添加')
    cancelDatasetItemDraft()
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
                <DatePicker
                  format="YYYY-MM-DD HH:mm:ss"
                  showTime
                  value={deadlineValue}
                  onChange={(value) => updateDraft({ deadline: value ? value.format('YYYY-MM-DDTHH:mm:ss') : '' })}
                />
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
                <Select
                  mode="tags"
                  options={draft.tags.map((tag) => ({ label: tag, value: tag }))}
                  placeholder="输入标签后回车添加"
                  tokenSeparators={[',', '，', ' ']}
                  value={draft.tags}
                  onChange={(tags) => updateDraft({ tags })}
                />
              </label>
              <label className="owner-field">
                <span>关联模板</span>
                <Select
                  allowClear
                  loading={isLoadingTemplates}
                  options={templateOptions}
                  placeholder="选择模板当前版本"
                  value={draft.publishedTemplateVersionId}
                  onChange={(publishedTemplateVersionId) => updateDraft({ publishedTemplateVersionId: publishedTemplateVersionId ?? null })}
                  onOpenChange={(open) => {
                    if (open) {
                      void loadTemplateOptions()
                    }
                  }}
                />
              </label>
              <label className="owner-field">
                <span>审核级别数</span>
                <InputNumber min={1} precision={0} value={draft.reviewLevelCount} onChange={(reviewLevelCount) => updateDraft({ reviewLevelCount: reviewLevelCount ?? 1 })} />
              </label>
              <label className="owner-field">
                <span>一致性次数</span>
                <InputNumber min={1} precision={0} value={draft.overlapCount} onChange={(overlapCount) => updateDraft({ overlapCount: overlapCount ?? 1 })} />
              </label>
              <label className="owner-field">
                <span>每人最大领取数</span>
                <InputNumber
                  min={1}
                  precision={0}
                  value={draft.maxClaimsPerLabeler}
                  onChange={(maxClaimsPerLabeler) => updateDraft({ maxClaimsPerLabeler: maxClaimsPerLabeler ?? 10 })}
                />
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
                <span>奖励模式</span>
                <Select
                  options={[{ label: '按通过题目', value: 'APPROVED_ITEM' }]}
                  value={draft.rewardRule.rewardMode}
                  onChange={(rewardMode) =>
                    updateDraft({
                      rewardRule: {
                        ...draft.rewardRule,
                        rewardMode,
                      },
                    })
                  }
                />
              </label>
              <label className="owner-field">
                <span>奖励币种</span>
                <Select
                  options={[{ label: '积分', value: 'POINT' }]}
                  value={draft.rewardRule.rewardCurrency}
                  onChange={(rewardCurrency) =>
                    updateDraft({
                      rewardRule: {
                        ...draft.rewardRule,
                        rewardCurrency,
                      },
                    })
                  }
                />
              </label>
              <label className="owner-field">
                <span>展示奖励</span>
                <Switch
                  checked={draft.rewardRule.rewardVisible}
                  onChange={(rewardVisible) =>
                    updateDraft({
                      rewardRule: {
                        ...draft.rewardRule,
                        rewardVisible,
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
              <label className="owner-field">
                <span>AI 审核策略</span>
                <Select
                  options={[{ label: '轻量审核', value: 'LIGHTWEIGHT' }]}
                  value={draft.aiReview.aiReviewStrategy}
                  onChange={(aiReviewStrategy) => updateDraft({ aiReview: { aiReviewStrategy } })}
                />
              </label>
              <label className="owner-field owner-field--wide">
                <span>AI 审核 Prompt</span>
                <Input.TextArea
                  autoSize={{ minRows: 2, maxRows: 4 }}
                  value={draft.aiReview.aiPrompt}
                  onChange={(event) => updateDraft({ aiReview: { aiPrompt: event.target.value } })}
                />
              </label>
              <label className="owner-field">
                <span>AI 模型</span>
                <Select
                  allowClear
                  loading={isLoadingModels}
                  options={modelSelectOptions}
                  placeholder="选择大模型"
                  value={draft.aiReview.aiProviderId ?? undefined}
                  onChange={(aiProviderId) => {
                    const selectedModel = modelOptions.find((option) => String(option.id) === aiProviderId)
                    updateDraft({
                      aiReview: {
                        aiProviderId: aiProviderId ?? null,
                        aiModelName: selectedModel?.defaultModel ?? '',
                      },
                    })
                  }}
                  onOpenChange={(open) => {
                    if (open) {
                      void loadModelOptions()
                    }
                  }}
                />
              </label>
              <label className="owner-field">
                <span>评分维度</span>
                <Select
                  mode="tags"
                  options={ratingDimensions.map((dimension) => ({ label: dimension, value: dimension }))}
                  placeholder="输入维度后回车添加"
                  tokenSeparators={[',', '，', ' ']}
                  value={ratingDimensions}
                  onChange={(dimensions) => updateDraft({ aiReview: { aiScoringDimensions: dimensions } })}
                />
              </label>
              <label className="owner-field">
                <span>通过阈值</span>
                <InputNumber
                  max={100}
                  min={0}
                  precision={2}
                  value={draft.aiReview.aiPassThreshold}
                  onChange={(aiPassThreshold) => updateDraft({ aiReview: { aiPassThreshold: aiPassThreshold ?? 0 } })}
                />
              </label>
              <label className="owner-field">
                <span>人工复核阈值</span>
                <InputNumber
                  max={100}
                  min={0}
                  precision={2}
                  value={draft.aiReview.aiManualReviewThreshold}
                  onChange={(aiManualReviewThreshold) => updateDraft({ aiReview: { aiManualReviewThreshold: aiManualReviewThreshold ?? 0 } })}
                />
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

            {taskId ? (
              <div className="owner-import-preview">
                <div className="owner-dataset-titlebar">
                  <Typography.Title level={5}>任务题目列表</Typography.Title>
                  <Button
                    disabled={Boolean(draftDatasetItem) || isReadonlyTask}
                    icon={<PlusOutlined />}
                    size="small"
                    type="primary"
                    onClick={startDatasetItemDraft}
                  >
                    添加题目
                  </Button>
                </div>

                <Table<DatasetTableRow>
                  className="owner-dataset-table"
                  columns={datasetColumns}
                  dataSource={datasetTableRows}
                  loading={isDatasetItemsLoading}
                  pagination={{
                    current: currentDatasetItemsPage?.page ?? 1,
                    pageSize: currentDatasetItemsPage?.pageSize ?? 10,
                    showSizeChanger: true,
                    total: currentDatasetItemsPage?.total ?? 0,
                    onChange: (page, pageSize) => {
                      void loadTaskDatasetItems(taskId, { page, pageSize })
                    },
                  }}
                  rowClassName={(row) => (isDatasetDraftRow(row) ? 'owner-dataset-table__row--draft' : '')}
                  rowKey={(row) => (isDatasetDraftRow(row) ? row.rowKey : String(row.itemId))}
                  scroll={{ x: 'max-content' }}
                  size="middle"
                />
              </div>
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
