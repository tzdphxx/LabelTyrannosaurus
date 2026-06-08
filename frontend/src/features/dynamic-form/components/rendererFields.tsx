import { CheckCircleOutlined, ThunderboltOutlined } from '@ant-design/icons'
import { Alert, Button, Card, Input, Space, Upload, Typography } from 'antd'
import type { UploadFile } from 'antd/es/upload/interface'
import styles from './DynamicFormRenderer.module.css'
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
      className={styles.jsonEditor}
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
  onApplyValues,
  onRunLlmTrigger,
  prompt,
  promptTemplate,
  targetFields,
  text,
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
  const suggestionText = response?.displayText || (Object.keys(response?.suggestionJson ?? {}).length ? JSON.stringify(response?.suggestionJson, null, 2) : '')

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
      <Space className="dynamic-renderer__llm-head" direction="vertical" size={6}>
        <Typography.Text className="dynamic-renderer__llm-title" strong>AI 建议清洗</Typography.Text>
      </Space>

      <div className="dynamic-renderer__llm-cleaner">
        <Button disabled={!canRun} icon={<ThunderboltOutlined />} loading={isRunning} onClick={() => void runTrigger()} type="primary">
          清洗
        </Button>

        <div className="dynamic-renderer__llm-suggestion">
          {response?.status === 'SUCCESS' ? (
            <>
              <Typography.Text type="secondary">
                {response.latencyMs ? `${response.latencyMs}ms` : statusLabels[response.status]}
              </Typography.Text>
              {suggestionText ? <pre>{suggestionText}</pre> : <Typography.Text type="secondary">未返回可展示建议</Typography.Text>}
            </>
          ) : (
            <Typography.Text type="secondary">
              {text || displayPrompt || '点击清洗后在这里查看建议'}
            </Typography.Text>
          )}
        </div>

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
      </div>

      {!canRun ? (
        <Alert className="dynamic-renderer__llm-alert" message="需要真实任务上下文后才能调用 LLM" showIcon type="warning" />
      ) : null}

      {error ? <Alert className="dynamic-renderer__llm-alert" message={error} showIcon type="error" /> : null}
    </Card>
  )
}
