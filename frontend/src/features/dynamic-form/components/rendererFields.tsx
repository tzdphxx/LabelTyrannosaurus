import { BulbOutlined, CheckCircleOutlined, ThunderboltOutlined } from '@ant-design/icons'
import { Alert, Button, Card, Input, Space, Tag, Upload, Typography } from 'antd'
import type { UploadFile } from 'antd/es/upload/interface'
import { useMemo, useState } from 'react'
import type { LlmGatewayStatus, LlmTriggerContext, LlmTriggerRunRequest, LlmTriggerRunResponse } from '../../../types/llm'

interface ValueFieldProps<T> {
  value?: T
  onChange?: (value: T) => void
}

const statusLabels: Partial<Record<LlmGatewayStatus, string>> = {
  SUCCESS: '调用成功',
  PROVIDER_UNAVAILABLE: 'Provider 不可用',
  PROVIDER_ERROR: 'Provider 异常',
  TIMEOUT: '调用超时',
  INVALID_JSON: '结构化输出无效',
  RATE_LIMITED: '触发限流',
}

function toNumber(value: number | string | null | undefined) {
  if (typeof value === 'number' && Number.isFinite(value)) {
    return value
  }

  if (typeof value === 'string' && value.trim()) {
    const parsed = Number(value)

    return Number.isFinite(parsed) ? parsed : null
  }

  return null
}

function normalizeTargetFields(value: unknown) {
  if (Array.isArray(value)) {
    return value.map((item) => String(item).trim()).filter(Boolean)
  }

  if (typeof value === 'string') {
    return value.split(/[\n,，]/).map((item) => item.trim()).filter(Boolean)
  }

  return []
}

function getRunnablePayload(options: {
  componentId?: string
  context?: LlmTriggerContext
  currentValues?: Record<string, unknown>
}) {
  const taskId = toNumber(options.context?.taskId)
  const templateVersionId = toNumber(options.context?.templateVersionId)
  const assignmentId = toNumber(options.context?.assignmentId)
  const datasetItemId = toNumber(options.context?.datasetItemId)

  if (!taskId || !templateVersionId || !options.componentId) {
    return null
  }

  if (!options.context?.previewMode && !assignmentId) {
    return null
  }

  const payload: LlmTriggerRunRequest = {
    taskId,
    templateVersionId,
    componentId: options.componentId,
    currentAnswerJson: options.currentValues ?? {},
    previewMode: Boolean(options.context?.previewMode),
  }

  if (datasetItemId) {
    payload.datasetItemId = datasetItemId
  }

  if (assignmentId) {
    payload.assignmentId = assignmentId
  }

  return payload
}

function buildApplyValues(response: LlmTriggerRunResponse, configuredTargetFields: string[]) {
  const targetFields = response.targetFields.length ? response.targetFields : configuredTargetFields
  const nextValues: Record<string, unknown> = {}

  targetFields.forEach((field) => {
    if (Object.hasOwn(response.suggestionJson, field)) {
      nextValues[field] = response.suggestionJson[field]
    }
  })

  return nextValues
}

export function RichTextEditor({ value, onChange, placeholder }: ValueFieldProps<string> & { placeholder?: string }) {
  return (
    <Input.TextArea
      autoSize={{ minRows: 5, maxRows: 10 }}
      placeholder={placeholder}
      value={value}
      onChange={(event) => onChange?.(event.target.value)}
    />
  )
}

export function JsonEditorField({ value, onChange, placeholder }: ValueFieldProps<string> & { placeholder?: string }) {
  return (
    <Input.TextArea
      autoSize={{ minRows: 6, maxRows: 12 }}
      className="dynamic-renderer__json-editor"
      placeholder={placeholder}
      value={value}
      onChange={(event) => onChange?.(event.target.value)}
    />
  )
}

export function FileUploadField({
  accept,
  maxCount,
  value,
  onChange,
}: ValueFieldProps<UploadFile[]> & {
  accept?: string
  maxCount?: number
}) {
  const fileList = Array.isArray(value) ? value : []

  return (
    <Upload.Dragger
      accept={accept}
      beforeUpload={(file) => {
        onChange?.([
          ...fileList,
          {
            uid: file.uid,
            name: file.name,
            status: 'done',
          },
        ])
        return false
      }}
      fileList={fileList}
      maxCount={maxCount}
      onRemove={(file) => {
        onChange?.(fileList.filter((item) => item.uid !== file.uid))
      }}
    >
      <Typography.Text>拖拽或选择文件</Typography.Text>
      <Typography.Paragraph type="secondary">当前组件只保留前端文件列表，不上传到后端。</Typography.Paragraph>
    </Upload.Dragger>
  )
}

export function LlmPromptBlock({
  componentId,
  getCurrentValues,
  llmContext,
  modelName,
  onApplyValues,
  onRunLlmTrigger,
  prompt,
  promptTemplate,
  targetFields,
  text,
  title,
}: {
  componentId?: string
  getCurrentValues?: () => Record<string, unknown>
  llmContext?: LlmTriggerContext
  modelName?: string
  onApplyValues?: (values: Record<string, unknown>) => void
  onRunLlmTrigger?: (payload: LlmTriggerRunRequest) => Promise<LlmTriggerRunResponse>
  prompt?: string
  promptTemplate?: string
  targetFields?: string[] | string
  text?: string
  title?: string
}) {
  const [isRunning, setIsRunning] = useState(false)
  const [response, setResponse] = useState<LlmTriggerRunResponse | null>(null)
  const [error, setError] = useState<string | null>(null)
  const configuredTargetFields = useMemo(() => normalizeTargetFields(targetFields), [targetFields])
  const payload = getRunnablePayload({ componentId, context: llmContext })
  const canRun = Boolean(payload && onRunLlmTrigger)
  const applyValues = response ? buildApplyValues(response, configuredTargetFields) : {}
  const canApply = response?.status === 'SUCCESS' && Object.keys(applyValues).length > 0
  const displayPrompt = promptTemplate || prompt

  async function runTrigger() {
    if (!payload || !onRunLlmTrigger) {
      setError('缺少真实任务上下文，无法调用 LLM。')
      return
    }

    setIsRunning(true)
    setError(null)

    try {
      const nextResponse = await onRunLlmTrigger({
        ...payload,
        currentAnswerJson: getCurrentValues?.() ?? {},
      })
      setResponse(nextResponse)

      if (nextResponse.status !== 'SUCCESS') {
        setError(nextResponse.errorMessage || statusLabels[nextResponse.status] || 'LLM 调用失败')
      }
    } catch (runError) {
      setError(runError instanceof Error ? runError.message : 'LLM 调用失败')
    } finally {
      setIsRunning(false)
    }
  }

  return (
    <Card className="dynamic-renderer__llm" size="small">
      <Space className="dynamic-renderer__llm-head" direction="vertical" size={8}>
        <div className="dynamic-renderer__llm-title">
          <BulbOutlined />
          <Typography.Text strong>{title ?? 'LLM 字段辅助'}</Typography.Text>
        </div>
        <Space size={6} wrap>
          {modelName ? <Tag>{modelName}</Tag> : <Tag color="default">未配置模型</Tag>}
          {configuredTargetFields.length ? <Tag color="processing">{configuredTargetFields.join(', ')}</Tag> : <Tag>仅展示建议</Tag>}
        </Space>
      </Space>

      {displayPrompt ? (
        <Typography.Paragraph className="dynamic-renderer__llm-prompt">{displayPrompt}</Typography.Paragraph>
      ) : null}

      {!canRun ? (
        <Alert message="需要真实任务上下文后才能调用 LLM" showIcon type="warning" />
      ) : null}

      {error ? <Alert message={error} showIcon type="error" /> : null}

      {response?.status === 'SUCCESS' ? (
        <div className="dynamic-renderer__llm-result">
          <Typography.Text type="secondary">
            {response.latencyMs ? `${response.latencyMs}ms` : statusLabels[response.status]}
          </Typography.Text>
          {response.displayText ? <Typography.Paragraph>{response.displayText}</Typography.Paragraph> : null}
          {Object.keys(response.suggestionJson).length ? (
            <pre>{JSON.stringify(response.suggestionJson, null, 2)}</pre>
          ) : null}
        </div>
      ) : null}

      <Space className="dynamic-renderer__llm-actions">
        <Button disabled={!canRun} icon={<ThunderboltOutlined />} loading={isRunning} onClick={() => void runTrigger()} type="primary">
          运行
        </Button>
        <Button
          disabled={!canApply}
          icon={<CheckCircleOutlined />}
          onClick={() => {
            if (canApply) {
              onApplyValues?.(applyValues)
            }
          }}
        >
          采纳
        </Button>
      </Space>

      {!response && text ? <Typography.Text type="secondary">{text}</Typography.Text> : null}
    </Card>
  )
}
