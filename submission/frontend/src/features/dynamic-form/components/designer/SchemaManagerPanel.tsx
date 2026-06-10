import { Button, Input, Space, Tag, Typography, message } from 'antd'
import { useEffect, useMemo, useState } from 'react'
import type { DynamicFormSchema } from '../../../../types/dynamicForm'

interface SchemaManagerPanelProps {
  schema: DynamicFormSchema | null
  onImport: (schema: DynamicFormSchema) => void
}

export function SchemaManagerPanel({ schema, onImport }: SchemaManagerPanelProps) {
  const [messageApi, contextHolder] = message.useMessage()
  const schemaText = useMemo(() => JSON.stringify(schema, null, 2), [schema])
  const [draftText, setDraftText] = useState(schemaText)

  useEffect(() => {
    setDraftText(schemaText)
  }, [schemaText])

  function importSchema() {
    try {
      const nextSchema = JSON.parse(draftText) as DynamicFormSchema

      if (!nextSchema.id || !Array.isArray(nextSchema.nodes)) {
        messageApi.error('Schema 格式不正确')
        return
      }

      onImport(nextSchema)
      messageApi.success('Schema 已导入')
    } catch {
      messageApi.error('Schema JSON 解析失败')
    }
  }

  async function exportSchema() {
    try {
      await navigator.clipboard.writeText(schemaText)
      messageApi.success('Schema JSON 已复制')
    } catch {
      messageApi.error('复制失败，请手动选择 JSON 内容')
    }
  }

  return (
    <Space className="designer-schema-panel" direction="vertical" size={12}>
      {contextHolder}
      <Space>
        <Tag>{schema?.version ?? '-'}</Tag>
        <Typography.Text type="secondary">{schema?.nodes.length ?? 0} 个根节点</Typography.Text>
      </Space>
      <Input.TextArea
        autoSize={{ minRows: 16, maxRows: 26 }}
        className="designer-schema-panel__editor"
        value={draftText}
        onChange={(event) => setDraftText(event.target.value)}
      />
      <Space>
        <Button onClick={importSchema} type="primary">
          导入 JSON
        </Button>
        <Button onClick={() => void exportSchema()}>复制导出</Button>
      </Space>
    </Space>
  )
}
