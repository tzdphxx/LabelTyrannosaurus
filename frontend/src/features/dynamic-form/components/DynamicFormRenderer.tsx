import { createForm, onFormValuesChange } from '@formily/core'
import { createSchemaField, FormProvider, RecursionField, useFieldSchema } from '@formily/react'
import { Checkbox, FormItem, FormLayout, Input, Radio, Select } from '@formily/antd-v5'
import { Alert, Button, Card, Space, Tabs, Typography, message, type TabsProps } from 'antd'
import type { ReactNode } from 'react'
import { useCallback, useEffect, useMemo, useState } from 'react'
import { llmService } from '../../../services/llm'
import type { DynamicFormSchema, DynamicFormSubmitResult } from '../../../types/dynamicForm'
import type { LlmTriggerContext } from '../../../types/llm'
import { schemaToFormilySchema } from '../utils/formilySchema'
import { getSchemaNodeKeys } from '../utils/schemaTree'
import { FileUploadField, JsonEditorField, LlmPromptBlock, RichTextEditor } from './rendererFields'
import styles from './DynamicFormRenderer.module.css'

interface DynamicFormRendererProps {
  schema: DynamicFormSchema
  initialValues?: Record<string, unknown>
  readOnly?: boolean
  submitText?: string
  llmContext?: LlmTriggerContext
  onValuesChange?: (values: Record<string, unknown>) => void
  onSubmit?: (result: DynamicFormSubmitResult) => void
}

function ShowItem(props: { text?: string }) {
  return <Alert className={styles.showItem} message={props.text ?? '展示信息'} showIcon type="info" />
}

function GroupSection({ children, title }: { children?: ReactNode; title?: string }) {
  return (
    <Card className={styles.group} size="small" title={title}>
      {children}
    </Card>
  )
}

function TabsSection({ title }: { children?: ReactNode; title?: string }) {
  const fieldSchema = useFieldSchema()
  const paneItems = useMemo(
    () =>
      fieldSchema.reduceProperties<NonNullable<TabsProps['items']>, NonNullable<TabsProps['items']>>((items, paneSchema, paneKey, index) => {
        const paneTitle = typeof paneSchema.title === 'string' && paneSchema.title.trim() ? paneSchema.title : `Tab ${index + 1}`
        const itemKey = String(paneKey)

        return [
          ...items,
          {
            key: itemKey,
            label: paneTitle,
            children: <RecursionField schema={paneSchema} name={paneKey} />,
          },
        ]
      }, []),
    [fieldSchema],
  )
  const firstPaneKey = paneItems[0]?.key
  const [activeKey, setActiveKey] = useState<string | undefined>(firstPaneKey)

  useEffect(() => {
    if (!paneItems.length) {
      setActiveKey(undefined)
      return
    }

    if (!activeKey || !paneItems.some((item) => item.key === activeKey)) {
      setActiveKey(firstPaneKey)
    }
  }, [activeKey, firstPaneKey, paneItems])

  if (paneItems.length > 0) {
    return (
      <Card className={styles.group} size="small" title={title}>
        <Tabs activeKey={activeKey} destroyInactiveTabPane={false} items={paneItems} onChange={setActiveKey} />
      </Card>
    )
  }

  return (
    <Card className={styles.group} size="small" title={title}>
      <Typography.Text type="secondary">暂无 TabPane</Typography.Text>
    </Card>
  )
}

function TabPaneSection({ children, title }: { children?: ReactNode; title?: string }) {
  return (
    <div className={styles.tabPane} data-tab-title={title}>
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

export function DynamicFormRenderer({
  schema,
  initialValues,
  readOnly = false,
  submitText = '提交预览',
  llmContext,
  onSubmit,
  onValuesChange,
}: DynamicFormRendererProps) {
  const [messageApi, contextHolder] = message.useMessage()
  const [submitting, setSubmitting] = useState(false)
  const answerFieldKeys = useMemo(() => getSchemaNodeKeys(schema), [schema])

  const form = useMemo(
    () =>
      createForm({
        initialValues,
        pattern: readOnly ? 'readPretty' : 'editable',
        effects() {
          onFormValuesChange((formInstance) => {
            const values = { ...formInstance.values }
            onValuesChange?.(values)
          })
        },
      }),
    [initialValues, onValuesChange, readOnly],
  )

  const applyLlmValues = useCallback(
    (values: Record<string, unknown>) => {
      const nextValues = {
        ...form.values,
        ...values,
      }

      form.setValues(nextValues)
      onValuesChange?.(nextValues)
    },
    [form, onValuesChange],
  )

  const formilySchema = useMemo(
    () =>
      schemaToFormilySchema(schema, {
        answerFieldKeys,
        getCurrentValues: () => ({ ...form.values }),
        llmContext,
        onApplyLlmValues: applyLlmValues,
        onRunLlmTrigger: llmService.runTrigger,
      }),
    [answerFieldKeys, applyLlmValues, form, llmContext, schema],
  )

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
    <div className={styles.renderer}>
      {contextHolder}
      <FormProvider form={form}>
        <FormLayout layout="vertical">
          <SchemaField schema={formilySchema} />
        </FormLayout>
      </FormProvider>
      {!readOnly ? (
        <Space className={styles.actions}>
          <Button loading={submitting} onClick={() => void submitForm()} type="primary">
            {submitText}
          </Button>
        </Space>
      ) : null}
    </div>
  )
}
