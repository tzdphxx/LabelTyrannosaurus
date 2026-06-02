import { Button, Card, Form, Input, Modal, Space, Table, Tag, Typography, message } from 'antd'
import { EditOutlined, EyeOutlined, PlusOutlined } from '@ant-design/icons'
import { useCallback, useEffect, useState } from 'react'
import { useNavigate } from 'react-router'
import { ContentShell } from '../../../components/page/ContentShell'
import { PageHeader } from '../../../components/page/PageHeader'
import { ApiError, ownerTemplateService } from '../../../services'
import type { TemplateSummary } from '../../../types/template'

const templateStatusMeta = {
  draft: { label: '草稿', color: 'warning' },
  ready: { label: '可用', color: 'success' },
  archived: { label: '归档', color: 'default' },
} as const

export function OwnerTemplatesPage() {
  const navigate = useNavigate()
  const [form] = Form.useForm<{ name: string; description: string }>()
  const [messageApi, contextHolder] = message.useMessage()
  const [templates, setTemplates] = useState<TemplateSummary[]>([])
  const [isLoading, setIsLoading] = useState(true)
  const [isCreateOpen, setIsCreateOpen] = useState(false)
  const [isCreating, setIsCreating] = useState(false)

  const loadTemplates = useCallback(() => {
    setIsLoading(true)
    void ownerTemplateService
      .listTemplates()
      .then(setTemplates)
      .finally(() => setIsLoading(false))
  }, [])

  useEffect(() => {
    let ignore = false

    void ownerTemplateService
      .listTemplates()
      .then((templates) => {
        if (!ignore) {
          setTemplates(templates)
        }
      })
      .finally(() => {
        if (!ignore) {
          setIsLoading(false)
        }
      })

    return () => {
      ignore = true
    }
  }, [])

  async function createTemplate() {
    const values = await form.validateFields()

    setIsCreating(true)

    try {
      const template = await ownerTemplateService.createTemplate(values)
      messageApi.success('模板已创建')
      setIsCreateOpen(false)
      form.resetFields()
      loadTemplates()
      navigate(`/app/owner/templates/${template.id}/designer`)
    } catch (error) {
      if (error instanceof ApiError && error.code === 409301) {
        messageApi.error('schema 校验失败')
      } else {
        messageApi.error('模板创建失败')
      }
    } finally {
      setIsCreating(false)
    }
  }

  return (
    <main className="owner-page">
      {contextHolder}
      <ContentShell>
        <PageHeader
          title="模板管理"
          description="管理 Owner 可用的动态表单模板，P0 支持进入 Designer、编辑 schema 并预览运行态表单。"
          extra={
            <Button icon={<PlusOutlined />} onClick={() => setIsCreateOpen(true)} type="primary">
              新建模板
            </Button>
          }
        />
      </ContentShell>

      <Card className="owner-table-card">
        <Table<TemplateSummary>
          columns={[
            {
              title: '模板',
              dataIndex: 'name',
              render: (_, template) => (
                <Space direction="vertical" size={2}>
                  <Typography.Text strong>{template.name}</Typography.Text>
                  <Typography.Text type="secondary">{template.description}</Typography.Text>
                </Space>
              ),
            },
            {
              title: '版本',
              dataIndex: 'version',
              width: 120,
            },
            {
              title: '字段数',
              dataIndex: 'fieldCount',
              width: 120,
            },
            {
              title: '状态',
              dataIndex: 'status',
              width: 120,
              render: (status: TemplateSummary['status']) => {
                const meta = templateStatusMeta[status]

                return <Tag color={meta.color}>{meta.label}</Tag>
              },
            },
            {
              title: '操作',
              width: 190,
              render: (_, template) => (
                <Space>
                  <Button
                    icon={<EditOutlined />}
                    onClick={() => navigate(`/app/owner/templates/${template.id}/designer`)}
                    type="primary"
                  >
                    设计
                  </Button>
                  <Button icon={<EyeOutlined />} onClick={() => navigate(`/app/owner/templates/${template.id}/designer`)}>
                    预览
                  </Button>
                </Space>
              ),
            },
          ]}
          dataSource={templates}
          loading={isLoading}
          pagination={false}
          rowKey="id"
        />
      </Card>

      <Modal
        confirmLoading={isCreating}
        destroyOnClose
        onCancel={() => setIsCreateOpen(false)}
        onOk={() => void createTemplate()}
        open={isCreateOpen}
        title="新建模板"
      >
        <Form form={form} layout="vertical" preserve={false}>
          <Form.Item
            label="模板名称"
            name="name"
            rules={[{ required: true, message: '请输入模板名称' }]}
          >
            <Input placeholder="例如：商品质检标注模板" />
          </Form.Item>
          <Form.Item
            label="变更说明"
            name="description"
            rules={[{ required: true, message: '请输入变更说明' }]}
          >
            <Input.TextArea autoSize={{ minRows: 3, maxRows: 5 }} placeholder="例如：初始版本" />
          </Form.Item>
        </Form>
      </Modal>
    </main>
  )
}
