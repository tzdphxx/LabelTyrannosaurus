import { createForm } from '@formily/core'
import { createSchemaField, FormProvider } from '@formily/react'
import { Checkbox, FormItem, FormLayout, Input, Radio, Select } from '@formily/antd-v5'
import { Alert, Button, Card, Space, Tabs, Typography, message } from 'antd'
import type { ReactNode } from 'react'
import { useMemo, useState } from 'react'
import type { DynamicFormSchema, DynamicFormSubmitResult } from '../../../types/dynamicForm'
import { schemaToFormilySchema } from '../utils/formilySchema'
import { FileUploadField, JsonEditorField, LlmPromptBlock, RichTextEditor } from './rendererFields'

interface DynamicFormRendererProps {
  schema: DynamicFormSchema
  initialValues?: Record<string, unknown>
  readOnly?: boolean
  onSubmit?: (result: DynamicFormSubmitResult) => void
}

function ShowItem(props: { text?: string }) {
  return (
    <Alert
      className="dynamic-renderer__show-item"
      message={props.text ?? '展示信息'}
      showIcon
      type="info"
    />
  )
}

function GroupSection({ children, title }: { children?: ReactNode; title?: string }) {
  return (
    <Card className="dynamic-renderer__group" size="small" title={title}>
      {children}
    </Card>
  )
}

function TabsSection({ children, title }: { children?: ReactNode; title?: string }) {
  return (
    <Card className="dynamic-renderer__group" size="small" title={title}>
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
}

function TabPaneSection({ children, title }: { children?: ReactNode; title?: string }) {
  return (
    <div className="dynamic-renderer__tab-pane">
      <Typography.Text strong>{title}</Typography.Text>
      <div>{children}</div>
    </div>
  )
}

const SchemaField = createSchemaField({
  components: {
    Checkbox,
    FormItem,
    FileUploadField,
    GroupSection,
    Input,
    JsonEditorField,
    LlmPromptBlock,
    Radio,
    RichTextEditor,
    Select,
    ShowItem,
    TabPaneSection,
    TabsSection,
  },
})

export function DynamicFormRenderer({ schema, initialValues, readOnly = false, onSubmit }: DynamicFormRendererProps) {
  const [messageApi, contextHolder] = message.useMessage()
  const [submitting, setSubmitting] = useState(false)
  const form = useMemo(
    () =>
      createForm({
        initialValues,
        pattern: readOnly ? 'readPretty' : 'editable',
      }),
    [initialValues, readOnly, schema.id, schema.version],
  )
  const formilySchema = useMemo(() => schemaToFormilySchema(schema), [schema])

  async function submitForm() {
    setSubmitting(true)

    try {
      await form.validate()
      onSubmit?.({
        templateId: schema.id,
        schemaVersion: schema.version,
        values: { ...form.values },
      })
      messageApi.success('表单校验通过，已生成提交数据')
    } catch {
      messageApi.error('表单存在字段错误，请检查后再提交')
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <div className="dynamic-renderer">
      {contextHolder}
      <FormProvider form={form}>
        <FormLayout layout="vertical">
          <SchemaField schema={formilySchema} />
        </FormLayout>
      </FormProvider>
      {!readOnly ? (
        <Space className="dynamic-renderer__actions">
          <Button loading={submitting} onClick={() => void submitForm()} type="primary">
            提交预览
          </Button>
        </Space>
      ) : null}
    </div>
  )
}
