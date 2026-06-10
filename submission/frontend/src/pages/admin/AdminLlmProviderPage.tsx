import {
  ApiOutlined,
  CheckCircleOutlined,
  CloseCircleOutlined,
  CloudOutlined,
  DeleteOutlined,
  EditOutlined,
  KeyOutlined,
  PlusOutlined,
  ReloadOutlined,
  ThunderboltOutlined,
} from '@ant-design/icons'
import {
  Alert,
  Button,
  Card,
  Drawer,
  Empty,
  Form,
  Input,
  InputNumber,
  Modal,
  Select,
  Space,
  Statistic,
  Switch,
  Table,
  Tag,
  Tooltip,
  Typography,
  message,
} from 'antd'
import { useCallback, useEffect, useMemo, useState } from 'react'
import { ContentShell } from '../../components/page/ContentShell'
import { PageHeader } from '../../components/page/PageHeader'
import { adminLlmProviderService } from '../../services'
import type {
  CustomHeaders,
  LlmProviderResponse,
  LlmProviderTestRequest,
  LlmProviderTestResponse,
  LlmProviderUpsertRequest,
  StructuredOutputMode,
} from '../../types/llmProvider'
import styles from './AdminLlmProviderPage.module.css'

type HeaderRow = {
  key: string
  name: string
  value: string
}

type ProviderFormValues = Omit<LlmProviderUpsertRequest, 'customHeaders'> & {
  customHeaders: HeaderRow[]
}

type TestFormValues = Omit<LlmProviderTestRequest, 'customHeaders'> & {
  customHeaders: HeaderRow[]
}

const structuredOutputOptions: Array<{ label: string, value: StructuredOutputMode }> = [
  { label: 'NONE', value: 'NONE' },
  { label: 'JSON_OBJECT', value: 'JSON_OBJECT' },
  { label: 'JSON_SCHEMA', value: 'JSON_SCHEMA' },
]

const structuredOutputLabels: Record<StructuredOutputMode | 'NONE_VALUE', string> = {
  NONE: '不强制',
  NONE_VALUE: '不强制',
  JSON_OBJECT: 'JSON 对象',
  JSON_SCHEMA: 'JSON Schema',
}

function formatDate(value?: string | null) {
  return value ? value.replace('T', ' ').slice(0, 16) : '-'
}

function headersToRows(headers?: CustomHeaders): HeaderRow[] {
  return Object.entries(headers ?? {}).map(([name, value], index) => ({
    key: `${name}-${index}`,
    name,
    value,
  }))
}

function rowsToHeaders(rows?: HeaderRow[]): CustomHeaders {
  return Object.fromEntries((rows ?? []).filter((row) => row.name.trim() && row.value.trim()).map((row) => [row.name.trim(), row.value.trim()]))
}

function getProviderInitial(provider: LlmProviderResponse) {
  return (provider.providerName || provider.providerCode || 'L').trim().slice(0, 1).toUpperCase()
}

function formatLimit(value: number) {
  return value > 0 ? value : '不限'
}

function providerToForm(provider: LlmProviderResponse): ProviderFormValues {
  return {
    providerCode: provider.providerCode,
    providerName: provider.providerName,
    baseUrl: provider.baseUrl,
    apiKey: null,
    defaultModel: provider.defaultModel,
    customHeaders: headersToRows(provider.customHeaders),
    platformRateLimitPerMinute: provider.platformRateLimitPerMinute,
    taskRateLimitPerMinute: provider.taskRateLimitPerMinute,
    userRateLimitPerMinute: provider.userRateLimitPerMinute,
    supportVision: provider.supportVision,
    supportMultiImage: provider.supportMultiImage,
    maxImageCount: provider.maxImageCount,
    visionModel: provider.visionModel,
    structuredOutputMode: provider.structuredOutputMode,
  }
}

function toProviderPayload(values: ProviderFormValues, isEditing: boolean): LlmProviderUpsertRequest {
  const apiKey = values.apiKey?.trim()

  return {
    ...values,
    providerCode: values.providerCode.trim(),
    providerName: values.providerName.trim(),
    baseUrl: values.baseUrl.trim(),
    apiKey: apiKey || (isEditing ? null : ''),
    defaultModel: values.defaultModel.trim(),
    customHeaders: rowsToHeaders(values.customHeaders),
    visionModel: values.visionModel?.trim() || null,
    structuredOutputMode: values.structuredOutputMode ?? null,
  }
}

function toTestPayload(values: TestFormValues): LlmProviderTestRequest {
  return {
    apiKey: values.apiKey?.trim() || null,
    modelName: values.modelName?.trim() || null,
    customHeaders: rowsToHeaders(values.customHeaders),
  }
}

export function AdminLlmProviderPage() {
  const [messageApi, contextHolder] = message.useMessage()
  const [form] = Form.useForm<ProviderFormValues>()
  const [testForm] = Form.useForm<TestFormValues>()
  const [providers, setProviders] = useState<LlmProviderResponse[]>([])
  const [isLoading, setIsLoading] = useState(false)
  const [isSaving, setIsSaving] = useState(false)
  const [isTesting, setIsTesting] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [editingProvider, setEditingProvider] = useState<LlmProviderResponse | null>(null)
  const [testingProvider, setTestingProvider] = useState<LlmProviderResponse | null>(null)
  const [testResult, setTestResult] = useState<LlmProviderTestResponse | null>(null)
  const [isDrawerOpen, setIsDrawerOpen] = useState(false)

  const loadProviders = useCallback(async () => {
    setIsLoading(true)
    setError(null)

    try {
      setProviders(await adminLlmProviderService.listProviders())
    } catch (loadError) {
      setError(loadError instanceof Error ? loadError.message : 'LLM Provider 列表加载失败')
    } finally {
      setIsLoading(false)
    }
  }, [])

  useEffect(() => {
    const timer = window.setTimeout(() => {
      void loadProviders()
    }, 0)

    return () => window.clearTimeout(timer)
  }, [loadProviders])

  const totals = useMemo(() => ({
    all: providers.length,
    enabled: providers.filter((provider) => provider.enabled).length,
    configured: providers.filter((provider) => provider.apiKeyConfigured).length,
  }), [providers])

  const openCreateDrawer = () => {
    setEditingProvider(null)
    form.setFieldsValue({
      providerCode: '',
      providerName: '',
      baseUrl: '',
      apiKey: '',
      defaultModel: '',
      customHeaders: [],
      platformRateLimitPerMinute: 0,
      taskRateLimitPerMinute: 0,
      userRateLimitPerMinute: 0,
      supportVision: false,
      supportMultiImage: false,
      maxImageCount: 10,
      visionModel: null,
      structuredOutputMode: 'NONE',
    })
    setIsDrawerOpen(true)
  }

  const openEditDrawer = (provider: LlmProviderResponse) => {
    setEditingProvider(provider)
    form.setFieldsValue(providerToForm(provider))
    setIsDrawerOpen(true)
  }

  const saveProvider = async () => {
    const values = await form.validateFields()
    const isEditing = Boolean(editingProvider)
    const payload = toProviderPayload(values, isEditing)

    setIsSaving(true)

    try {
      if (editingProvider) {
        await adminLlmProviderService.updateProvider(editingProvider.id, payload)
        messageApi.success('Provider 已更新')
      } else {
        await adminLlmProviderService.createProvider(payload)
        messageApi.success('Provider 已创建')
      }

      setIsDrawerOpen(false)
      await loadProviders()
    } catch (saveError) {
      messageApi.error(saveError instanceof Error ? saveError.message : 'Provider 保存失败')
    } finally {
      setIsSaving(false)
    }
  }

  const toggleProvider = async (provider: LlmProviderResponse) => {
    try {
      if (provider.enabled) {
        await adminLlmProviderService.disableProvider(provider.id)
        messageApi.success('Provider 已停用')
      } else {
        await adminLlmProviderService.enableProvider(provider.id)
        messageApi.success('Provider 已启用')
      }

      await loadProviders()
    } catch (toggleError) {
      messageApi.error(toggleError instanceof Error ? toggleError.message : '状态更新失败')
    }
  }

  const openTestModal = (provider: LlmProviderResponse) => {
    setTestingProvider(provider)
    setTestResult(null)
    testForm.setFieldsValue({
      apiKey: null,
      modelName: provider.defaultModel,
      customHeaders: [],
    })
  }

  const testProvider = async () => {
    if (!testingProvider) {
      return
    }

    const values = await testForm.validateFields()
    setIsTesting(true)
    setTestResult(null)

    try {
      setTestResult(await adminLlmProviderService.testProvider(testingProvider.id, toTestPayload(values)))
    } catch (testError) {
      messageApi.error(testError instanceof Error ? testError.message : 'Provider 测试失败')
    } finally {
      setIsTesting(false)
    }
  }

  return (
    <main className={styles.page}>
      {contextHolder}
      <ContentShell className={styles.hero}>
        <PageHeader
          title="LLM Provider"
          description="维护全局可用的大模型供应商、模型、限流、视觉能力和结构化输出策略。"
          extra={
            <>
              <Button icon={<ReloadOutlined />} loading={isLoading} onClick={() => void loadProviders()}>
                刷新
              </Button>
              <Button icon={<PlusOutlined />} type="primary" onClick={openCreateDrawer}>
                新增 Provider
              </Button>
            </>
          }
        />
      </ContentShell>

      {error ? <Alert message={error} showIcon type="error" /> : null}

      <div className={styles.summaryGrid}>
        <Card className={styles.summaryCard}>
          <Statistic prefix={<CloudOutlined />} title="Provider 总数" value={totals.all} />
        </Card>
        <Card className={styles.summaryCard}>
          <Statistic prefix={<CheckCircleOutlined />} title="已启用" value={totals.enabled} />
        </Card>
        <Card className={styles.summaryCard}>
          <Statistic prefix={<KeyOutlined />} title="已配置 API Key" value={totals.configured} />
        </Card>
      </div>

      <Card
        className={styles.panel}
        extra={<Typography.Text className={styles.muted}>按更新时间倒序展示全部 Provider</Typography.Text>}
        title="Provider 列表"
      >
        <Table<LlmProviderResponse>
          columns={[
            {
              title: 'Provider',
              dataIndex: 'providerName',
              render: (_, provider) => (
                <div className={styles.providerIdentity}>
                  <div className={styles.providerMark}>{getProviderInitial(provider)}</div>
                  <div className={styles.metaStack}>
                    <Space size={6} wrap>
                      <Typography.Text strong>{provider.providerName}</Typography.Text>
                      <Tag className={provider.enabled ? styles.statusEnabled : styles.statusDisabled}>
                        {provider.enabled ? '启用' : '停用'}
                      </Tag>
                    </Space>
                    <Typography.Text className={styles.muted}>{provider.providerCode}</Typography.Text>
                  </div>
                </div>
              ),
            },
            {
              title: '模型',
              dataIndex: 'defaultModel',
              render: (_, provider) => (
                <div className={styles.metaStack}>
                  <Typography.Text className={styles.modelName}>{provider.defaultModel}</Typography.Text>
                  <Tooltip title={provider.baseUrl}>
                    <Typography.Text className={styles.baseUrl} ellipsis>{provider.baseUrl}</Typography.Text>
                  </Tooltip>
                </div>
              ),
            },
            {
              title: '能力',
              width: 220,
              render: (_, provider) => (
                <div className={styles.capabilityStack}>
                  <Tag className={styles.capabilityTag}>{provider.supportVision ? '视觉输入' : '文本输入'}</Tag>
                  {provider.supportMultiImage ? <Tag className={styles.capabilityTag}>多图</Tag> : null}
                  <Tag className={styles.capabilityTag}>
                    {structuredOutputLabels[provider.structuredOutputMode ?? 'NONE_VALUE']}
                  </Tag>
                </div>
              ),
            },
            {
              title: '限流',
              width: 210,
              render: (_, provider) => (
                <div className={styles.limitGrid}>
                  <span>平台 {formatLimit(provider.platformRateLimitPerMinute)}</span>
                  <span>任务 {formatLimit(provider.taskRateLimitPerMinute)}</span>
                  <span>用户 {formatLimit(provider.userRateLimitPerMinute)}</span>
                </div>
              ),
            },
            {
              title: 'API Key',
              dataIndex: 'apiKeyConfigured',
              width: 110,
              render: (configured: boolean) => (
                <Tag className={configured ? styles.keyConfigured : styles.keyMissing}>
                  {configured ? '已配置' : '未配置'}
                </Tag>
              ),
            },
            {
              title: '更新时间',
              dataIndex: 'updatedAt',
              width: 150,
              render: formatDate,
            },
            {
              title: '操作',
              width: 180,
              fixed: 'right',
              render: (_, provider) => (
                <div className={styles.actionBar}>
                  <Tooltip title="编辑 Provider">
                    <Button icon={<EditOutlined />} size="small" onClick={() => openEditDrawer(provider)} />
                  </Tooltip>
                  <Tooltip title="测试连通性">
                    <Button icon={<ThunderboltOutlined />} size="small" onClick={() => openTestModal(provider)} />
                  </Tooltip>
                  <Tooltip title={provider.enabled ? '停用后 Owner 不可选择' : '启用后 Owner 可选择'}>
                    <Button
                      icon={provider.enabled ? <CloseCircleOutlined /> : <CheckCircleOutlined />}
                      size="small"
                      type={provider.enabled ? 'default' : 'primary'}
                      onClick={() => void toggleProvider(provider)}
                    >
                      {provider.enabled ? '停用' : '启用'}
                    </Button>
                  </Tooltip>
                </div>
              ),
            },
          ]}
          dataSource={providers}
          loading={isLoading}
          locale={{
            emptyText: (
              <Empty
                description="还没有配置任何 LLM Provider"
                image={Empty.PRESENTED_IMAGE_SIMPLE}
              >
                <Button icon={<PlusOutlined />} type="primary" onClick={openCreateDrawer}>
                  新增 Provider
                </Button>
              </Empty>
            ),
          }}
          pagination={false}
          rowKey="id"
          scroll={{ x: 'max-content' }}
        />
      </Card>

      <Drawer
        className={styles.providerDrawer}
        destroyOnHidden
        footer={
          <div className={styles.drawerFooter}>
            <Typography.Text className={styles.muted}>
              {editingProvider ? 'API Key 留空则保留原密钥。' : '创建成功后服务端默认启用。'}
            </Typography.Text>
            <Space>
              <Button onClick={() => setIsDrawerOpen(false)}>取消</Button>
              <Button loading={isSaving} type="primary" onClick={() => void saveProvider()}>
                {editingProvider ? '保存修改' : '创建 Provider'}
              </Button>
            </Space>
          </div>
        }
        open={isDrawerOpen}
        title={
          <div className={styles.drawerTitle}>
            <Typography.Title level={4}>{editingProvider ? '编辑 Provider' : '新增 Provider'}</Typography.Title>
            <Typography.Text className={styles.muted}>
              {editingProvider ? 'API Key 留空会保留原密钥，只更新其他配置。' : '创建成功后服务端默认启用该 Provider。'}
            </Typography.Text>
          </div>
        }
        width="min(720px, 100vw)"
        onClose={() => setIsDrawerOpen(false)}
      >
        <Form className={styles.providerForm} form={form} layout="vertical">
          <section className={styles.formSection}>
            <div className={styles.sectionHead}>
              <div>
                <Typography.Text strong>基础配置</Typography.Text>
                <Typography.Text className={styles.muted}>填写供应商、模型和调用地址。</Typography.Text>
              </div>
            </div>
            <div className={styles.formGrid}>
              <Form.Item label="Provider 编码" name="providerCode" rules={[{ required: true, message: '请填写 Provider 编码' }, { max: 64 }]}>
                <Input placeholder="dashscope" />
              </Form.Item>
              <Form.Item label="展示名称" name="providerName" rules={[{ required: true, message: '请填写展示名称' }, { max: 100 }]}>
                <Input placeholder="DashScope Qwen Plus" />
              </Form.Item>
              <Form.Item className={styles.formWide} label="Base URL" name="baseUrl" rules={[{ required: true, message: '请填写 Base URL' }, { max: 500 }]}>
                <Input placeholder="https://dashscope.aliyuncs.com/compatible-mode/v1" />
              </Form.Item>
              <Form.Item label="默认模型" name="defaultModel" rules={[{ required: true, message: '请填写默认模型' }, { max: 128 }]}>
                <Input placeholder="qwen-plus" />
              </Form.Item>
              <Form.Item
                label={editingProvider ? 'API Key（留空不更新）' : 'API Key'}
                name="apiKey"
                rules={[{ required: !editingProvider, message: '请填写 API Key' }, { max: 4096 }]}
              >
                <Input.Password autoComplete="new-password" placeholder={editingProvider ? '留空保留原密钥' : 'sk-***'} />
              </Form.Item>
            </div>
          </section>

          <section className={styles.formSection}>
            <div className={styles.sectionHead}>
              <div>
                <Typography.Text strong>能力与策略</Typography.Text>
                <Typography.Text className={styles.muted}>这些字段可稍后补充，不影响创建主流程。</Typography.Text>
              </div>
            </div>
            <div className={styles.switchGrid}>
              <Form.Item label="支持视觉" name="supportVision" valuePropName="checked">
                <Switch />
              </Form.Item>
              <Form.Item label="支持多图" name="supportMultiImage" valuePropName="checked">
                <Switch />
              </Form.Item>
              <Form.Item label="最大图片数" name="maxImageCount">
                <InputNumber min={0} precision={0} />
              </Form.Item>
              <Form.Item label="视觉模型" name="visionModel" rules={[{ max: 100 }]}>
                <Input allowClear placeholder="可留空" />
              </Form.Item>
              <Form.Item label="结构化输出" name="structuredOutputMode">
                <Select allowClear options={structuredOutputOptions} />
              </Form.Item>
            </div>
          </section>

          <section className={styles.formSection}>
            <div className={styles.sectionHead}>
              <div>
                <Typography.Text strong>限流</Typography.Text>
                <Typography.Text className={styles.muted}>0 表示不限制，单位为每分钟请求数。</Typography.Text>
              </div>
            </div>
            <div className={styles.limitFormGrid}>
              <Form.Item label="平台限流" name="platformRateLimitPerMinute">
                <InputNumber min={0} precision={0} />
              </Form.Item>
              <Form.Item label="任务限流" name="taskRateLimitPerMinute">
                <InputNumber min={0} precision={0} />
              </Form.Item>
              <Form.Item label="用户限流" name="userRateLimitPerMinute">
                <InputNumber min={0} precision={0} />
              </Form.Item>
            </div>
          </section>

          <section className={styles.formSection}>
            <div className={styles.sectionHead}>
              <div>
                <Typography.Text strong>Custom Headers</Typography.Text>
                <Typography.Text className={styles.muted}>敏感 Header 会由后端返回为 ******，不要把它当成密钥明文。</Typography.Text>
              </div>
            </div>
            <Form.List name="customHeaders">
              {(fields, { add, remove }) => (
                <div className={styles.headerEditor}>
                  {fields.map((field) => (
                    <div className={styles.headerRow} key={field.key}>
                      <Form.Item name={[field.name, 'name']} rules={[{ max: 100 }]}>
                        <Input placeholder="Header name" />
                      </Form.Item>
                      <Form.Item name={[field.name, 'value']} rules={[{ max: 4096 }]}>
                        <Input placeholder="Header value" />
                      </Form.Item>
                      <Tooltip title="删除 Header">
                        <Button danger icon={<DeleteOutlined />} size="small" onClick={() => remove(field.name)} />
                      </Tooltip>
                    </div>
                  ))}
                  <Button icon={<PlusOutlined />} onClick={() => add({ key: `${Date.now()}`, name: '', value: '' })}>
                    添加 Header
                  </Button>
                </div>
              )}
            </Form.List>
          </section>
        </Form>
      </Drawer>

      <Modal
        className={styles.testModal}
        destroyOnHidden
        okButtonProps={{ icon: <ApiOutlined />, loading: isTesting }}
        okText="开始测试"
        open={Boolean(testingProvider)}
        title={`测试 Provider${testingProvider ? `：${testingProvider.providerName}` : ''}`}
        onCancel={() => setTestingProvider(null)}
        onOk={() => void testProvider()}
      >
        <Form className={styles.testForm} form={testForm} layout="vertical">
          <Form.Item label="临时 API Key" name="apiKey" rules={[{ max: 4096 }]}>
            <Input.Password autoComplete="new-password" placeholder="留空使用已保存密钥" />
          </Form.Item>
          <Form.Item label="临时模型名" name="modelName" rules={[{ max: 128 }]}>
            <Input />
          </Form.Item>
          <Form.List name="customHeaders">
            {(fields, { add, remove }) => (
              <div className={styles.headerEditor}>
                <Typography.Text strong>临时 Headers</Typography.Text>
                {fields.map((field) => (
                  <div className={styles.headerRow} key={field.key}>
                    <Form.Item name={[field.name, 'name']}>
                      <Input placeholder="Header name" />
                    </Form.Item>
                    <Form.Item name={[field.name, 'value']}>
                      <Input placeholder="Header value" />
                    </Form.Item>
                    <Button danger icon={<DeleteOutlined />} size="small" onClick={() => remove(field.name)} />
                  </div>
                ))}
                <Button icon={<PlusOutlined />} onClick={() => add({ key: `${Date.now()}`, name: '', value: '' })}>
                  添加 Header
                </Button>
              </div>
            )}
          </Form.List>
        </Form>
        {testResult ? (
          <div className={styles.testResult}>
            <Tag className={testResult.success ? styles.statusEnabled : styles.keyMissing}>
              {testResult.success ? '测试成功' : '测试失败'}
            </Tag>
            <Typography.Text strong>{testResult.message}</Typography.Text>
            <Typography.Text className={styles.muted}>耗时 {testResult.latencyMs} ms</Typography.Text>
          </div>
        ) : null}
      </Modal>
    </main>
  )
}
