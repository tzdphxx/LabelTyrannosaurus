import { Alert, Card, Input, Upload, Typography } from 'antd'
import type { UploadFile } from 'antd/es/upload/interface'
import styles from './DynamicFormRenderer.module.css'

interface ValueFieldProps<T> {
  value?: T
  onChange?: (value: T) => void
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
      <Typography.Paragraph type="secondary">P1 保留前端占位，不上传到后端。</Typography.Paragraph>
    </Upload.Dragger>
  )
}

export function LlmPromptBlock({ prompt, text, title }: { prompt?: string; text?: string; title?: string }) {
  return (
    <Card className={styles.llm} size="small" title={title ?? 'LLM 交互占位'}>
      <Typography.Paragraph>{prompt}</Typography.Paragraph>
      <Alert message={text ?? 'P1 阶段不调用真实模型。'} showIcon type="info" />
    </Card>
  )
}
