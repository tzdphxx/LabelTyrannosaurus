import { Button, Card, Form, Input, Modal, Space, Table, Tag, Typography } from 'antd'
import { EditOutlined, EyeOutlined, PlusOutlined } from '@ant-design/icons'
import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router'
import { ContentShell } from '../../../components/page/ContentShell'
import { PageHeader } from '../../../components/page/PageHeader'
import { ownerTemplateService } from '../../../services'
import type { TemplateSummary } from '../../../types/template'
import styles from '../OwnerPages.module.css'

const templateStatusMeta = {
  draft: { label: '草稿', color: 'warning' },
  ready: { label: '可用', color: 'success' },
  archived: { label: '归档', color: 'default' },
} as const

export function OwnerTemplatesPage() {
  const navigate = useNavigate()
  const [form] = Form.useForm<{ name: string; description: string }>()
  const [templates, setTemplates] = useState<TemplateSummary[]>([])
  const [isLoading, setIsLoading] = useState(true)
  const [isCreateOpen, setIsCreateOpen] = useState(false)

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

  function openTemplateDraft() {
    const values = form.getFieldsValue()
    const name = values.name?.trim() || '未命名模板'
    const description = values.description?.trim() ?? ''

    setIsCreateOpen(false)
    form.resetFields()
    navigate('/app/owner/templates/draft/designer', {
      state: {
        draftTemplate: {
          description,
          name,
        },
      },
    })
  }

  return (
    <main className={styles.page}>
      <ContentShell className={styles.hero}>
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

      <Card className={styles.tableCard}>
        <Table<TemplateSummary>
          className={styles.dataTable}
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
                <Space className={styles.templateActions}>
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
        destroyOnClose
        onCancel={() => setIsCreateOpen(false)}
        onOk={openTemplateDraft}
        open={isCreateOpen}
        title="新建模板"
      >
        <Form form={form} layout="vertical" preserve={false}>
          <Form.Item label="模板标题" name="name">
            <Input placeholder="例如：商品质检标注模板" />
          </Form.Item>
          <Form.Item label="模板描述" name="description">
            <Input.TextArea autoSize={{ minRows: 3, maxRows: 5 }} placeholder="例如：初始版本说明" />
          </Form.Item>
        </Form>
      </Modal>
    </main>
  )
}
