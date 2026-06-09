import { Button, Card, Form, Input, List, Modal, Popover, Space, Table, Tag, Typography, message } from 'antd'
import { CopyOutlined, DownOutlined, EditOutlined, EyeOutlined, PlusOutlined } from '@ant-design/icons'
import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router'
import { ContentShell } from '../../../components/page/ContentShell'
import { PageHeader } from '../../../components/page/PageHeader'
import { ownerTemplateService } from '../../../services'
import type { TemplateDetail, TemplateSummary, TemplateVersionSnapshot } from '../../../types/template'
import styles from '../OwnerPages.module.css'

const templateStatusMeta = {
  draft: { label: '草稿', color: 'warning' },
  ready: { label: '可用', color: 'success' },
  archived: { label: '归档', color: 'default' },
} as const

export function OwnerTemplatesPage() {
  const navigate = useNavigate()
  const [messageApi, contextHolder] = message.useMessage()
  const [form] = Form.useForm<{ name: string; description: string }>()
  const [forkForm] = Form.useForm<{ changeNote: string }>()
  const [templates, setTemplates] = useState<TemplateSummary[]>([])
  const [isLoading, setIsLoading] = useState(true)
  const [isCreateOpen, setIsCreateOpen] = useState(false)
  const [forkTarget, setForkTarget] = useState<TemplateSummary | null>(null)
  const [openVersionTemplateId, setOpenVersionTemplateId] = useState<string | null>(null)
  const [versionCache, setVersionCache] = useState<Record<string, TemplateVersionSnapshot[]>>({})
  const [versionLoadingIds, setVersionLoadingIds] = useState<string[]>([])
  const [selectedVersions, setSelectedVersions] = useState<Record<string, TemplateVersionSnapshot>>({})

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

  async function openTemplateVersions(template: TemplateSummary) {
    setOpenVersionTemplateId(template.id)

    if (versionCache[template.id]) {
      return
    }

    setVersionLoadingIds((ids) => (ids.includes(template.id) ? ids : [...ids, template.id]))

    try {
      const versions = await ownerTemplateService.listTemplateVersions(template.id)

      setVersionCache((cache) => ({
        ...cache,
        [template.id]: versions,
      }))
    } catch {
      messageApi.error('模板版本加载失败')
    } finally {
      setVersionLoadingIds((ids) => ids.filter((id) => id !== template.id))
    }
  }

  function selectTemplateVersion(template: TemplateSummary, version: TemplateVersionSnapshot) {
    setSelectedVersions((versions) => ({
      ...versions,
      [template.id]: version,
    }))
    setTemplates((items) =>
      items.map((item) =>
        item.id === template.id
          ? {
            ...item,
            currentVersionId: version.versionId,
            version: version.version,
            status: version.status,
            fieldCount: version.fieldCount,
            description: version.description,
          }
          : item,
      ),
    )
    setOpenVersionTemplateId(null)
  }

  function toVersionTemplateDetail(template: TemplateSummary, version: TemplateVersionSnapshot): TemplateDetail {
    return {
      id: template.id,
      currentVersionId: version.versionId,
      name: template.name,
      version: version.version,
      status: version.status,
      fieldCount: version.fieldCount,
      description: version.description,
      schema: version.schema,
      updatedAt: version.createdAt,
    }
  }

  function openTemplateDesigner(template: TemplateSummary) {
    const selectedVersion = selectedVersions[template.id]

    navigate(`/app/owner/templates/${template.id}/designer`, {
      state: selectedVersion
        ? {
          templateVersion: toVersionTemplateDetail(template, selectedVersion),
        }
        : undefined,
    })
  }

  function renderVersionSelector(template: TemplateSummary) {
    const versions = versionCache[template.id] ?? []
    const isVersionLoading = versionLoadingIds.includes(template.id)

    return (
      <List
        className={styles.versionList}
        dataSource={versions}
        loading={isVersionLoading}
        locale={{ emptyText: '暂无版本' }}
        renderItem={(version) => {
          const isSelected = template.currentVersionId === version.versionId
          const meta = templateStatusMeta[version.status]

          return (
            <List.Item className={isSelected ? styles.versionListItemSelected : styles.versionListItem}>
              <button className={styles.versionOption} onClick={() => selectTemplateVersion(template, version)} type="button">
                <span className={styles.versionOptionMain}>
                  <Typography.Text strong>{version.version}</Typography.Text>
                  <Tag color={meta.color}>{meta.label}</Tag>
                </span>
                <Typography.Text className={styles.versionOptionNote} type="secondary">
                  {version.description || '无变更说明'}
                </Typography.Text>
                <Typography.Text className={styles.versionOptionTime} type="secondary">
                  {version.createdAt || '-'}
                </Typography.Text>
              </button>
            </List.Item>
          )
        }}
        size="small"
      />
    )
  }

  function openTemplateFork(template: TemplateSummary) {
    setForkTarget(template)
    forkForm.setFieldsValue({
      changeNote: `基于 ${template.version} Fork 新版本`,
    })
  }

  function openTemplateForkDesigner() {
    if (!forkTarget) {
      return
    }

    const values = forkForm.getFieldsValue()
    const changeNote = values.changeNote?.trim() || `基于 ${forkTarget.version} Fork 新版本`

    setForkTarget(null)
    forkForm.resetFields()
    navigate(`/app/owner/templates/${forkTarget.id}/designer`, {
      state: {
        forkTemplate: {
          changeNote,
        },
      },
    })
  }

  return (
    <main className={styles.page}>
      {contextHolder}
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
                  <Popover
                    content={renderVersionSelector(template)}
                    onOpenChange={(open) => {
                      if (open) {
                        void openTemplateVersions(template)
                        return
                      }

                      setOpenVersionTemplateId(null)
                    }}
                    open={openVersionTemplateId === template.id}
                    placement="bottomLeft"
                    trigger="click"
                  >
                    <Button className={styles.templateNameButton} type="link">
                      {template.name}
                      <DownOutlined />
                    </Button>
                  </Popover>
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
              width: 280,
              render: (_, template) => (
                <Space className={styles.templateActions}>
                  <Button
                    icon={<EditOutlined />}
                    onClick={() => openTemplateDesigner(template)}
                    type="primary"
                  >
                    设计
                  </Button>
                  <Button icon={<EyeOutlined />} onClick={() => openTemplateDesigner(template)}>
                    预览
                  </Button>
                  <Button icon={<CopyOutlined />} onClick={() => openTemplateFork(template)}>
                    Fork
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
        destroyOnHidden
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

      <Modal
        destroyOnHidden
        onCancel={() => {
          setForkTarget(null)
          forkForm.resetFields()
        }}
        onOk={openTemplateForkDesigner}
        open={Boolean(forkTarget)}
        title="Fork 模板版本"
      >
        <Form form={forkForm} layout="vertical" preserve={false}>
          <Form.Item label="目标模板">
            <Typography.Text strong>{forkTarget?.name}</Typography.Text>
            <Typography.Text type="secondary"> · 当前版本 {forkTarget?.version}</Typography.Text>
          </Form.Item>
          <Form.Item label="变更说明" name="changeNote">
            <Input.TextArea autoSize={{ minRows: 3, maxRows: 5 }} placeholder="说明这个新版本调整了什么" />
          </Form.Item>
        </Form>
      </Modal>
    </main>
  )
}
