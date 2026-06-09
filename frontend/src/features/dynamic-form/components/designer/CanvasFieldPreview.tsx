import { Alert, Card, Checkbox, Input, Radio, Select, Space, Tabs, Typography, Upload } from 'antd'
import type { ReactNode } from 'react'
import type { DynamicFieldOption, DynamicSchemaNode } from '../../../../types/dynamicForm'

interface CanvasFieldPreviewProps {
  children?: ReactNode
  node: DynamicSchemaNode
}

function normalizeOptions(options?: DynamicFieldOption[]) {
  return options?.length ? options : [{ label: '选项', value: 'option' }]
}

function FieldFrame({ children, title }: { children: ReactNode; title: string }) {
  return (
    <div className="designer-field-preview">
      <span className="designer-field-preview__label">{title}</span>
      {children}
    </div>
  )
}

export function CanvasFieldPreview({ children, node }: CanvasFieldPreviewProps) {
  const options = normalizeOptions(node.props.options)

  switch (node.type) {
    case 'input':
      return (
        <FieldFrame title={node.title}>
          <Input disabled placeholder={String(node.props.placeholder ?? '请输入')} />
        </FieldFrame>
      )
    case 'textarea':
      return (
        <FieldFrame title={node.title}>
          <Input.TextArea autoSize={{ minRows: 3, maxRows: 6 }} disabled placeholder={String(node.props.placeholder ?? '请输入详细内容')} />
        </FieldFrame>
      )
    case 'radio':
      return (
        <FieldFrame title={node.title}>
          <Radio.Group disabled>
            <Space direction="vertical" size={6}>
              {options.map((option, index) => (
                <Radio key={`${option.value}-${index}`} value={option.value}>
                  {option.label}
                </Radio>
              ))}
            </Space>
          </Radio.Group>
        </FieldFrame>
      )
    case 'checkbox':
      return (
        <FieldFrame title={node.title}>
          <Checkbox.Group disabled options={options} />
        </FieldFrame>
      )
    case 'select':
      return (
        <FieldFrame title={node.title}>
          <Select disabled mode={node.props.mode} options={options} placeholder={String(node.props.placeholder ?? '请选择')} />
        </FieldFrame>
      )
    case 'tagSelect':
      return (
        <FieldFrame title={node.title}>
          <Select disabled mode="tags" options={options} placeholder={String(node.props.placeholder ?? '请选择或输入标签')} />
        </FieldFrame>
      )
    case 'showItem':
      return <Alert className="designer-field-preview__show-item" message={String(node.props.text ?? node.title)} showIcon type="info" />
    case 'richText':
      return (
        <FieldFrame title={node.title}>
          <div className="designer-field-preview__rich-text">
            <div className="designer-field-preview__rich-toolbar">
              <span>B</span>
              <span>I</span>
              <span>U</span>
              <span>•</span>
            </div>
            <div className="designer-field-preview__rich-body">
              <strong>富文本内容</strong>
              <span>{String(node.props.placeholder ?? '请输入带格式文本')}</span>
            </div>
          </div>
        </FieldFrame>
      )
    case 'fileUpload':
      return (
        <FieldFrame title={node.title}>
          <Upload.Dragger accept={String(node.props.accept ?? '')} disabled maxCount={Number(node.props.maxCount ?? 1)}>
            <Typography.Text>拖拽或选择文件</Typography.Text>
            <Typography.Paragraph type="secondary">画布预览占位，不上传文件。</Typography.Paragraph>
          </Upload.Dragger>
        </FieldFrame>
      )
    case 'jsonEditor':
      return (
        <FieldFrame title={node.title}>
          <Input.TextArea autoSize={{ minRows: 6, maxRows: 12 }} disabled placeholder={String(node.props.placeholder ?? '{\n  "key": "value"\n}')} />
        </FieldFrame>
      )
    case 'llmPrompt':
      return (
        <Card className="designer-field-preview__llm" size="small" title={node.title}>
          <Typography.Paragraph>{String(node.props.prompt ?? '')}</Typography.Paragraph>
          <Alert message={String(node.props.text ?? 'AI 回复占位')} showIcon type="info" />
        </Card>
      )
    case 'group':
      return (
        <Card className="designer-field-preview__group" size="small" title={node.title}>
          {children}
        </Card>
      )
    case 'tabs':
      return (
        <Card className="designer-field-preview__group" size="small" title={node.title}>
          <Tabs
            items={[
              {
                key: 'content',
                label: '内容',
                children,
              },
            ]}
          />
        </Card>
      )
    case 'tabPane':
      return (
        <div className="designer-field-preview__tab-pane">
          <Typography.Text strong>{node.title}</Typography.Text>
          <div>{children}</div>
        </div>
      )
    default:
      return null
  }
}
