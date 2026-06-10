import {
  Alert,
  Button,
  Card,
  Col,
  DatePicker,
  Drawer,
  Input,
  InputNumber,
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
import { CheckOutlined, CloseOutlined, EyeOutlined, InboxOutlined, PlusOutlined, SaveOutlined, SendOutlined } from '@ant-design/icons'
import dayjs from 'dayjs'
import { useCallback, useEffect, useMemo, useRef, useState } from 'react'
import { useNavigate, useParams } from 'react-router'
import { ContentShell } from '../../components/page/ContentShell'
import { PageHeader } from '../../components/page/PageHeader'
import { ownerModelService, ownerTemplateService } from '../../services'
import { useOwnerDraftStore } from '../../stores/ownerDraftStore'
import { useOwnerTaskStore } from '../../stores/ownerTaskStore'
import type { AiFlowPolicy, AiReviewStrategy, DatasetItemResponse, OwnerLabelerOption, OwnerModelOptionResponse } from '../../types/task'
import type { TemplateSummary, TemplateVersionSnapshot } from '../../types/template'
import { distributionStrategyLabels, formatCount } from '../../utils/ownerTasks'
import styles from './OwnerTaskEditorPage.module.css'
import { AssigneePickerDrawer } from './task-editor/AssigneePickerDrawer'

const aiReviewStrategyOptions: Array<{ label: string, value: AiReviewStrategy }> = [
  { label: '单路 LLM（默认，兼容存量）', value: 'LIGHTWEIGHT' },
  { label: '多模型并行投票', value: 'PARALLEL_VOTE' },
  { label: '维度专项模型 + 维度内投票', value: 'DEEP_DIMENSION' },
  { label: '多 Agent 辩论', value: 'AGENT_DEBATE' },
]

const aiFlowPolicyOptions: Array<{ label: string, value: AiFlowPolicy }> = [
  { label: 'AI 只提建议，结果一律转人工', value: 'MANUAL_FIRST' },
  { label: 'AI 可直接过审，打回转人工', value: 'AI_PASS_ONLY' },
  { label: 'AI 可直接打回，通过转人工', value: 'AI_REJECT_ONLY' },
  { label: 'AI 可直接过审与打回', value: 'AI_PASS_AND_REJECT' },
  { label: '始终转人工', value: 'ALWAYS_MANUAL' },
]

type DatasetDraftRow = {
  rowType: 'draft'
  rowKey: 'draft'
  externalId: string
  itemJson: Record<string, unknown>
}

type DatasetPreviewRow = {
  rowType: 'preview'
  rowKey: string
  externalId: string
  itemJson: Record<string, unknown>
}

type DatasetTableRow = DatasetItemResponse | DatasetDraftRow | DatasetPreviewRow

const DATASET_EXTERNAL_ID_COLUMN_WIDTH = 190
const DATASET_FIELD_COLUMN_WIDTH = 220
const DATASET_STATUS_COLUMN_WIDTH = 110
const DATASET_UPDATED_AT_COLUMN_WIDTH = 170
const DATASET_STATS_COLUMN_WIDTH = 210
const DATASET_ACTION_COLUMN_WIDTH = 110

function isDatasetDraftRow(row: DatasetTableRow): row is DatasetDraftRow {
  return 'rowType' in row && row.rowType === 'draft'
}

function isDatasetPreviewRow(row: DatasetTableRow): row is DatasetPreviewRow {
  return 'rowType' in row && row.rowType === 'preview'
}

export function OwnerTaskEditorPage() {
  const navigate = useNavigate()
  const { taskId } = useParams()
  const [messageApi, contextHolder] = message.useMessage()
  const [templates, setTemplates] = useState<TemplateSummary[]>([])
  const [isLoadingTemplates, setIsLoadingTemplates] = useState(false)
  const [selectedTemplateId, setSelectedTemplateId] = useState<string | null>(null)
  const [templateVersionsByTemplateId, setTemplateVersionsByTemplateId] = useState<Record<string, TemplateVersionSnapshot[]>>({})
  const [isLoadingTemplateVersions, setIsLoadingTemplateVersions] = useState(false)
  const [modelOptions, setModelOptions] = useState<OwnerModelOptionResponse[]>([])
  const [isLoadingModels, setIsLoadingModels] = useState(false)
  const isLoadingTemplatesRef = useRef(false)
  const isLoadingModelsRef = useRef(false)
  const draft = useOwnerDraftStore((state) => state.draft)
  const importPreview = useOwnerDraftStore((state) => state.importPreview)
  const uploadedDatasetFile = useOwnerDraftStore((state) => state.uploadedDatasetFile)
  const hasUnsavedChanges = useOwnerDraftStore((state) => state.hasUnsavedChanges)
  const isSaving = useOwnerDraftStore((state) => state.isSaving)
  const isLoadingDraft = useOwnerDraftStore((state) => state.isLoading)
  const isUploadingDataset = useOwnerDraftStore((state) => state.isUploadingDataset)
  const draftError = useOwnerDraftStore((state) => state.error)
  const resetDraft = useOwnerDraftStore((state) => state.resetDraft)
  const loadFromTask = useOwnerDraftStore((state) => state.loadFromTask)
  const updateDraft = useOwnerDraftStore((state) => state.updateDraft)
  const uploadDatasetFile = useOwnerDraftStore((state) => state.uploadDatasetFile)
  const saveDraft = useOwnerDraftStore((state) => state.saveDraft)
  const publishDraft = useOwnerDraftStore((state) => state.publishDraft)
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
  const [previewDatasetPage, setPreviewDatasetPage] = useState(1)
  const [previewDatasetPageSize, setPreviewDatasetPageSize] = useState(10)
  const [isPreviewDrawerOpen, setIsPreviewDrawerOpen] = useState(false)
  const [isAssigneeDrawerOpen, setIsAssigneeDrawerOpen] = useState(false)
  const [selectedAssigneeName, setSelectedAssigneeName] = useState('')

  const loadTemplateOptions = useCallback(async (): Promise<TemplateSummary[]> => {
    if (isLoadingTemplatesRef.current) {
      return templates
    }

    isLoadingTemplatesRef.current = true
    setIsLoadingTemplates(true)

    try {
      const nextTemplates = await ownerTemplateService.listTemplates()

      setTemplates(nextTemplates)
      return nextTemplates
    } catch {
      messageApi.error('模板列表加载失败')
      return []
    } finally {
      isLoadingTemplatesRef.current = false
      setIsLoadingTemplates(false)
    }
  }, [messageApi, templates])

  const loadTemplateVersionOptions = useCallback(
    async (templateId: string): Promise<TemplateVersionSnapshot[]> => {
      const cachedVersions = templateVersionsByTemplateId[templateId]

      if (cachedVersions) {
        return cachedVersions
      }

      setIsLoadingTemplateVersions(true)

      try {
        const versions = await ownerTemplateService.listTemplateVersions(templateId)

        setTemplateVersionsByTemplateId((versionMap) => ({
          ...versionMap,
          [templateId]: versions,
        }))

        return versions
      } catch {
        messageApi.error('模板版本加载失败')
        return []
      } finally {
        setIsLoadingTemplateVersions(false)
      }
    },
    [messageApi, templateVersionsByTemplateId],
  )

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

    setSelectedTemplateId(null)
    void resetDraft()
  }, [loadFromTask, loadTaskDetail, resetDraft, taskId])

  useEffect(() => {
    if (!draft.publishedTemplateVersionId) {
      return
    }

    if (
      selectedTemplateId &&
      templateVersionsByTemplateId[selectedTemplateId]?.some((version) => version.versionId === draft.publishedTemplateVersionId)
    ) {
      return
    }

    let ignore = false

    async function resolveSelectedTemplate() {
      const loadedTemplates = templates.length ? templates : await loadTemplateOptions()
      const directTemplate = loadedTemplates.find((template) => template.currentVersionId === draft.publishedTemplateVersionId)

      if (directTemplate) {
        const versions = await loadTemplateVersionOptions(directTemplate.id)

        if (!ignore && versions.some((version) => version.versionId === draft.publishedTemplateVersionId)) {
          setSelectedTemplateId(directTemplate.id)
        }
        return
      }

      for (const template of loadedTemplates) {
        const versions = await loadTemplateVersionOptions(template.id)

        if (versions.some((version) => version.versionId === draft.publishedTemplateVersionId)) {
          if (!ignore) {
            setSelectedTemplateId(template.id)
          }
          return
        }
      }
    }

    void resolveSelectedTemplate()

    return () => {
      ignore = true
    }
  }, [
    draft.publishedTemplateVersionId,
    loadTemplateOptions,
    loadTemplateVersionOptions,
    selectedTemplateId,
    templateVersionsByTemplateId,
    templates,
  ])

  useEffect(() => {
    setPreviewDatasetPage(1)
  }, [importPreview?.id])

  useEffect(() => {
    if (!taskId && importPreview && draft.quota !== importPreview.validRows) {
      updateDraft({ quota: Math.max(importPreview.validRows, 1) })
    }
  }, [draft.quota, importPreview, taskId, updateDraft])

  const templateOptions = templates.map((template) => ({
    label: template.name,
    value: template.id,
  }))
  const selectedTemplateVersions = selectedTemplateId ? templateVersionsByTemplateId[selectedTemplateId] ?? [] : []
  const templateVersionOptions = selectedTemplateVersions.map((version) => ({
    label: `${version.version}${version.description ? ` · ${version.description}` : ''}`,
    value: version.versionId,
  }))
  const modelSelectOptions = modelOptions.map((option) => ({
    label: `${option.providerName} / ${option.defaultModel}`,
    value: String(option.id),
  }))
  const isReadonlyTask = Boolean(taskId && currentTaskDetail?.task.status !== 'draft')
  const isUnresolvedTemplateVersion = Boolean(draft.publishedTemplateVersionId && !selectedTemplateId && !isLoadingTemplates && !isLoadingTemplateVersions)
  const deadlineValue = draft.deadline && dayjs(draft.deadline).isValid() ? dayjs(draft.deadline) : null
  const ratingDimensions = draft.aiReview.aiScoringDimensions

  const selectTemplateForTask = (templateId: string | null) => {
    setSelectedTemplateId(templateId)
    updateDraft({ publishedTemplateVersionId: null })

    if (templateId) {
      void loadTemplateVersionOptions(templateId)
    }
  }

  const datasetPreviewRows = useMemo<DatasetPreviewRow[]>(() => (
    importPreview?.samples.map((sample) => ({
      rowType: 'preview',
      rowKey: sample.id,
      externalId: sample.id,
      itemJson: sample.values,
    })) ?? []
  ), [importPreview?.samples])
  const datasetFieldNames = useMemo(() => {
    const fieldNames = new Set<string>()
    const sourceRows = taskId ? currentDatasetItemsPage?.items ?? [] : datasetPreviewRows

    sourceRows.forEach((item) => {
      Object.keys(item.itemJson).forEach((key) => fieldNames.add(key))
    })

    const names = Array.from(fieldNames)

    return names.length ? names : ['text']
  }, [currentDatasetItemsPage?.items, datasetPreviewRows, taskId])
  const formatDatasetItemTime = (value: string) => value?.replace('T', ' ').slice(0, 19) || '-'
  const formatDatasetValue = (value: unknown) => {
    if (value === null || value === undefined || value === '') {
      return '-'
    }

    return typeof value === 'object' ? JSON.stringify(value) : String(value)
  }
  const getDatasetFieldTone = (index: number) => {
    const tone = ((index % 6) + 1) as 1 | 2 | 3 | 4 | 5 | 6
    return styles[`datasetFieldTone${tone}`]
  }
  const datasetTableRows = useMemo<DatasetTableRow[]>(() => {
    const items = taskId ? currentDatasetItemsPage?.items ?? [] : datasetPreviewRows

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
  }, [currentDatasetItemsPage?.items, datasetPreviewRows, draftDatasetExternalId, draftDatasetItem, taskId])
  const datasetColumns = useMemo(() => [
    {
      key: 'externalId',
      title: 'externalId',
      dataIndex: 'externalId',
      width: DATASET_EXTERNAL_ID_COLUMN_WIDTH,
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
          <Typography.Text strong>{row.externalId || (isDatasetPreviewRow(row) ? '-' : `#${row.itemId}`)}</Typography.Text>
        ),
    },
    ...datasetFieldNames.map((field, index) => ({
      key: `dataset-field-${field}`,
      title: <Tag className={getDatasetFieldTone(index)}>{field}</Tag>,
      dataIndex: ['itemJson', field],
      width: DATASET_FIELD_COLUMN_WIDTH,
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
      key: 'status',
      width: DATASET_STATUS_COLUMN_WIDTH,
      render: (_: unknown, row: DatasetTableRow) => {
        if (isDatasetDraftRow(row)) {
          return <Tag color="blue">新增</Tag>
        }

        if (isDatasetPreviewRow(row)) {
          return <Tag color="geekblue">预览</Tag>
        }

        return <Tag>{row.itemStatus || '-'}</Tag>
      },
    },
    {
      title: '更新时间',
      key: 'updatedAt',
      width: DATASET_UPDATED_AT_COLUMN_WIDTH,
      render: (_: unknown, row: DatasetTableRow) =>
        isDatasetDraftRow(row) || isDatasetPreviewRow(row) ? '-' : <Typography.Text type="secondary">{formatDatasetItemTime(row.updatedAt)}</Typography.Text>,
    },
    {
      title: '统计',
      key: 'stats',
      width: DATASET_STATS_COLUMN_WIDTH,
      render: (_: unknown, row: DatasetTableRow) =>
        isDatasetDraftRow(row) || isDatasetPreviewRow(row) ? (
          '-'
        ) : (
          <Space className={styles.datasetStats} size={8} wrap>
            <span>分发 {formatCount(row.assignedCount)}</span>
            <span>提交 {formatCount(row.submittedCount)}</span>
            <span>通过 {formatCount(row.approvedCount)}</span>
          </Space>
        ),
    },
    {
      title: '操作',
      key: 'actions',
      width: DATASET_ACTION_COLUMN_WIDTH,
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
    submitDatasetItemDraft,
  ])
  const datasetTableScrollX = useMemo(
    () =>
      DATASET_EXTERNAL_ID_COLUMN_WIDTH +
      datasetFieldNames.length * DATASET_FIELD_COLUMN_WIDTH +
      DATASET_STATUS_COLUMN_WIDTH +
      DATASET_UPDATED_AT_COLUMN_WIDTH +
      DATASET_STATS_COLUMN_WIDTH +
      DATASET_ACTION_COLUMN_WIDTH,
    [datasetFieldNames.length],
  )

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

  const previewButtonText = taskId ? '预览题目' : `预览题目${importPreview ? `（${formatCount(importPreview.validRows)}）` : ''}`

  return (
    <main className={styles.page}>
      {contextHolder}
      <ContentShell className={styles.headerShell}>
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

      <section className={styles.workspace}>
        <Card className={styles.formCard} loading={isLoadingDraft} title="基础信息">
          <div className={styles.formGrid}>
            <label className={styles.field}>
              <span>任务标题</span>
              <Input value={draft.title} onChange={(event) => updateDraft({ title: event.target.value })} />
            </label>
            <label className={styles.field}>
              <span>截止时间</span>
              <DatePicker
                format="YYYY-MM-DD HH:mm:ss"
                showTime
                value={deadlineValue}
                onChange={(value) => updateDraft({ deadline: value ? value.format('YYYY-MM-DDTHH:mm:ss') : '' })}
              />
            </label>
            <label className={`${styles.field} ${styles.fieldWide}`}>
              <span>任务描述</span>
              <Input.TextArea
                autoSize={{ minRows: 2, maxRows: 4 }}
                value={draft.description}
                onChange={(event) => updateDraft({ description: event.target.value })}
              />
            </label>
            <label className={`${styles.field} ${styles.fieldWide}`}>
              <span>标注说明</span>
              <Input.TextArea
                autoSize={{ minRows: 3, maxRows: 6 }}
                value={draft.instruction}
                onChange={(event) => updateDraft({ instruction: event.target.value })}
              />
            </label>
            <label className={styles.field}>
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
            <label className={styles.field}>
              <span>关联模板</span>
              <Select
                allowClear
                disabled={isReadonlyTask}
                loading={isLoadingTemplates}
                options={templateOptions}
                placeholder="选择模板"
                value={selectedTemplateId}
                onChange={(templateId) => selectTemplateForTask(templateId ?? null)}
                onOpenChange={(open) => {
                  if (open) {
                    void loadTemplateOptions()
                  }
                }}
              />
            </label>
            <label className={styles.field}>
              <span>模板版本</span>
              <Select
                allowClear
                disabled={isReadonlyTask || !selectedTemplateId}
                loading={isLoadingTemplateVersions}
                options={templateVersionOptions}
                placeholder={selectedTemplateId ? '选择模板版本' : '请先选择模板'}
                value={draft.publishedTemplateVersionId}
                onChange={(publishedTemplateVersionId) => updateDraft({ publishedTemplateVersionId: publishedTemplateVersionId ?? null })}
                onOpenChange={(open) => {
                  if (open && selectedTemplateId) {
                    void loadTemplateVersionOptions(selectedTemplateId)
                  }
                }}
              />
              {isUnresolvedTemplateVersion ? (
                <Typography.Text type="warning">当前模板版本不在可选模板列表中</Typography.Text>
              ) : null}
            </label>
            <label className={styles.field}>
              <span>审核级别数</span>
              <InputNumber min={1} precision={0} value={draft.reviewLevelCount} onChange={(reviewLevelCount) => updateDraft({ reviewLevelCount: reviewLevelCount ?? 1 })} />
            </label>
            <label className={styles.field}>
              <span>一致性次数</span>
              <InputNumber min={1} precision={0} value={draft.overlapCount} onChange={(overlapCount) => updateDraft({ overlapCount: overlapCount ?? 1 })} />
            </label>
            <label className={styles.field}>
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
            <label className={styles.field}>
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
            <label className={styles.field}>
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
            <label className={`${styles.field} ${styles.rewardVisibilityField}`}>
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
            <label className={styles.field}>
              <span>分发策略</span>
              <Select
                options={Object.entries(distributionStrategyLabels).map(([value, label]) => ({ value, label }))}
                value={draft.distributionStrategy}
                onChange={(distributionStrategy) => {
                  updateDraft({
                    distributionStrategy,
                    assignedLabelerId: distributionStrategy === '指派' ? draft.assignedLabelerId : null,
                  })
                }}
              />
            </label>
            {draft.distributionStrategy === '配额分发' ? (
              <label className={styles.field}>
                <span>每人最大领取数</span>
                <InputNumber
                  min={1}
                  precision={0}
                  value={draft.maxClaimsPerLabeler}
                  onChange={(maxClaimsPerLabeler) => updateDraft({ maxClaimsPerLabeler: maxClaimsPerLabeler ?? 10 })}
                />
              </label>
            ) : null}
            {draft.distributionStrategy === '指派' ? (
              <div className={`${styles.field} ${styles.assignmentField}`}>
                <span>指派标注员</span>
                <Space className={styles.assignmentControl} wrap>
                  <Button onClick={() => setIsAssigneeDrawerOpen(true)}>选择标注员</Button>
                  <Typography.Text type={draft.assignedLabelerId ? undefined : 'secondary'}>
                    {draft.assignedLabelerId ? selectedAssigneeName || `标注员 ID：${draft.assignedLabelerId}` : '请选择一位标注员'}
                  </Typography.Text>
                </Space>
              </div>
            ) : null}
            <label className={styles.field}>
              <span>AI 审核策略</span>
              <Select
                options={aiReviewStrategyOptions}
                value={draft.aiReview.aiReviewStrategy}
                onChange={(aiReviewStrategy) => updateDraft({ aiReview: { aiReviewStrategy } })}
              />
            </label>
            <label className={styles.field}>
              <span>AI 流转策略</span>
              <Select
                options={aiFlowPolicyOptions}
                value={draft.aiReview.aiFlowPolicy}
                onChange={(aiFlowPolicy) => updateDraft({ aiReview: { aiFlowPolicy } })}
              />
            </label>
            <label className={`${styles.field} ${styles.fieldWide}`}>
              <span>AI 审核 Prompt</span>
              <Input.TextArea
                autoSize={{ minRows: 2, maxRows: 4 }}
                value={draft.aiReview.aiPrompt}
                onChange={(event) => updateDraft({ aiReview: { aiPrompt: event.target.value } })}
              />
            </label>
            <label className={styles.field}>
              <span>AI 模型</span>
              <Select
                allowClear
                loading={isLoadingModels}
                options={modelSelectOptions}
                placeholder="选择大模型"
                value={draft.aiReview.aiModelName ?? undefined}
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
            <label className={styles.field}>
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
            <label className={styles.field}>
              <span>通过阈值</span>
              <InputNumber
                max={100}
                min={0}
                precision={2}
                value={draft.aiReview.aiPassThreshold}
                onChange={(aiPassThreshold) => updateDraft({ aiReview: { aiPassThreshold: aiPassThreshold ?? 0 } })}
              />
            </label>
            <label className={styles.field}>
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

        <Card
          className={styles.formCard}
          extra={
            <Button icon={<EyeOutlined />} onClick={() => setIsPreviewDrawerOpen(true)}>
              {previewButtonText}
            </Button>
          }
          title="数据集导入"
        >
          <Upload.Dragger
            className={styles.uploadDragger}
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

          <div className={styles.quotaSummary}>
            <Statistic title="题目数量" value={formatCount(importPreview?.validRows ?? draft.quota)} />
            <Typography.Text type="secondary">
              {importPreview ? '已根据上传文件自动计算' : '上传数据集后自动计算并写入任务配额'}
            </Typography.Text>
          </div>

          {!taskId && importPreview ? (
            <div className={styles.datasetSummary}>
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
            </div>
          ) : null}
        </Card>
      </section>

      <Drawer
        className={styles.previewDrawer}
        destroyOnHidden
        open={isPreviewDrawerOpen}
        placement="right"
        title={taskId ? '任务题目列表' : '题目预览'}
        width="min(760px, 100vw)"
        onClose={() => setIsPreviewDrawerOpen(false)}
      >
        <div className={styles.previewPanel}>
          <div className={styles.datasetTitlebar}>
            <Typography.Title level={5}>{taskId ? '任务题目列表' : '题目预览'}</Typography.Title>
            {taskId ? (
              <Button
                disabled={Boolean(draftDatasetItem) || isReadonlyTask}
                icon={<PlusOutlined />}
                size="small"
                type="primary"
                onClick={startDatasetItemDraft}
              >
                添加题目
              </Button>
            ) : null}
          </div>

          <Table<DatasetTableRow>
            className={styles.datasetTable}
            columns={datasetColumns}
            dataSource={datasetTableRows}
            loading={isDatasetItemsLoading}
            pagination={taskId ? {
              current: currentDatasetItemsPage?.page ?? 1,
              pageSize: currentDatasetItemsPage?.pageSize ?? 10,
              showSizeChanger: true,
              total: currentDatasetItemsPage?.total ?? 0,
              onChange: (page, pageSize) => {
                void loadTaskDatasetItems(taskId, { page, pageSize })
              },
            } : {
              current: previewDatasetPage,
              pageSize: previewDatasetPageSize,
              showSizeChanger: true,
              total: datasetPreviewRows.length,
              onChange: (page, pageSize) => {
                setPreviewDatasetPage(page)
                setPreviewDatasetPageSize(pageSize)
              },
            }}
            rowClassName={(row) => (isDatasetDraftRow(row) ? styles.datasetDraftRow : '')}
            rowKey={(row) => ('rowKey' in row ? row.rowKey : String(row.itemId))}
            scroll={{ x: datasetTableScrollX }}
            size="middle"
          />

          {!taskId && importPreview ? (
            <>
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
            </>
          ) : null}
        </div>
      </Drawer>

      <AssigneePickerDrawer
        open={isAssigneeDrawerOpen}
        selectedLabelerId={draft.assignedLabelerId}
        onClose={() => setIsAssigneeDrawerOpen(false)}
        onSelect={(labeler: OwnerLabelerOption) => {
          updateDraft({ assignedLabelerId: String(labeler.labelerId) })
          setSelectedAssigneeName(labeler.username)
          setIsAssigneeDrawerOpen(false)
        }}
      />
    </main>
  )
}
