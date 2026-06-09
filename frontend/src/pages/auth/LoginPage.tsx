import { LockOutlined, MailOutlined, UserOutlined } from '@ant-design/icons'
import { Button, Form, Input, Segmented, Select, Typography, message } from 'antd'
import { useState } from 'react'
import { useNavigate } from 'react-router'
import { ApiError } from '../../services'
import { useAuthStore } from '../../stores/authStore'
import type { Role } from '../../types/auth'
import { getRoleHomePath } from '../../utils/roles'

type AuthMode = 'login' | 'register'

interface AuthFormValues {
  account: string
  username: string
  email: string
  password: string
  role: Role
  confirmPassword?: string
}

const registerRoleOptions: Array<{ label: string; value: Role }> = [
  { label: '任务管理员', value: 'OWNER' },
  { label: '标注员', value: 'LABELER' },
]

export function LoginPage() {
  const [form] = Form.useForm<AuthFormValues>()
  const navigate = useNavigate()
  const loginWithPassword = useAuthStore((state) => state.loginWithPassword)
  const registerWithPassword = useAuthStore((state) => state.registerWithPassword)
  const [authMode, setAuthMode] = useState<AuthMode>('login')
  const [submitting, setSubmitting] = useState(false)

  function handleModeChange(value: AuthMode) {
    setAuthMode(value)
    form.resetFields()
  }

  async function handleSubmit(values: AuthFormValues) {
    setSubmitting(true)

    try {
      const role = isRegisterMode
        ? await registerWithPassword({
          username: values.username,
          email: values.email,
          password: values.password,
          role: values.role,
        })
        : await loginWithPassword({
          account: values.account,
          password: values.password,
        })

      navigate(getRoleHomePath(role), { replace: true })
    } catch (error) {
      message.error(error instanceof ApiError ? error.message : '认证失败，请检查输入后重试')
    } finally {
      setSubmitting(false)
    }
  }

  const isRegisterMode = authMode === 'register'

  return (
    <main className="login-page">
      <div className="login-sticker login-sticker--sky" aria-hidden="true" />
      <div className="login-sticker login-sticker--pink" aria-hidden="true" />
      <div className="login-sticker login-sticker--green" aria-hidden="true" />

      <section className="login-panel">
        <div className="login-panel__intro">
          <div className="login-panel__brandmark" aria-hidden="true">
            LH
          </div>
          <Typography.Text className="login-panel__eyebrow">LabelHub Workspace</Typography.Text>
          <Typography.Title className="login-panel__title">LabelHub</Typography.Title>
          <Typography.Paragraph className="login-panel__copy">
            用一个安静、清晰的工作台串起任务发布、数据标注、AI 预审和人工复核。
          </Typography.Paragraph>

          <div className="login-product-tags" aria-label="平台能力">
            <span>任务管理</span>
            <span>标注协作</span>
            <span>审核闭环</span>
          </div>

          <div className="login-flow" aria-hidden="true">
            <span className="login-flow__node login-flow__node--primary" />
            <span className="login-flow__line" />
            <span className="login-flow__node login-flow__node--review" />
            <span className="login-flow__line login-flow__line--short" />
            <span className="login-flow__node login-flow__node--done" />
          </div>

          <div className="login-mini-board" aria-hidden="true">
            <div className="login-mini-board__row">
              <span />
              <strong>Dataset review</strong>
              <em>82%</em>
            </div>
            <div className="login-mini-board__row">
              <span />
              <strong>Labeling queue</strong>
              <em>34</em>
            </div>
            <div className="login-mini-board__row">
              <span />
              <strong>AI precheck</strong>
              <em>Live</em>
            </div>
          </div>
        </div>

        <div className="auth-panel">
          <div className="auth-panel__accent" aria-hidden="true" />
          <div className="auth-panel__header">
            <Segmented<AuthMode>
              block
              className="auth-panel__switch"
              onChange={handleModeChange}
              options={[
                { label: '登录', value: 'login' },
                { label: '注册', value: 'register' },
              ]}
              value={authMode}
            />
            <div>
              <Typography.Title className="auth-panel__title" level={2}>
                {isRegisterMode ? '创建账号' : '账号登录'}
              </Typography.Title>
              <Typography.Paragraph className="auth-panel__copy">
                {isRegisterMode ? '使用邮箱、密码和身份创建一个工作账号。' : '继续进入你的标注协作空间。'}
              </Typography.Paragraph>
            </div>
          </div>

          <Form
            className="auth-form"
            form={form}
            layout="vertical"
            onFinish={handleSubmit}
            requiredMark={false}
          >
            {isRegisterMode ? (
              <Form.Item label="用户名" name="username" rules={[{ required: true, message: '请输入用户名' }]}>
                <Input autoComplete="username" prefix={<UserOutlined />} placeholder="请输入用户名" size="large" />
              </Form.Item>
            ) : (
              <Form.Item label="用户名或邮箱" name="account" rules={[{ required: true, message: '请输入用户名或邮箱' }]}>
                <Input autoComplete="username" prefix={<UserOutlined />} placeholder="请输入用户名或邮箱" size="large" />
              </Form.Item>
            )}

            {isRegisterMode ? (
              <Form.Item
                label="邮箱"
                name="email"
                rules={[
                  { required: true, message: '请输入邮箱' },
                  { type: 'email', message: '请输入有效的邮箱地址' },
                ]}
              >
                <Input autoComplete="email" prefix={<MailOutlined />} placeholder="name@example.com" size="large" />
              </Form.Item>
            ) : null}

            {isRegisterMode ? (
              <Form.Item label="身份" name="role" rules={[{ required: true, message: '请选择身份' }]}>
                <Select placeholder="请选择身份" size="large" options={registerRoleOptions} />
              </Form.Item>
            ) : null}

            <Form.Item label="密码" name="password" rules={[{ required: true, message: '请输入密码' }]}>
              <Input.Password
                autoComplete={isRegisterMode ? 'new-password' : 'current-password'}
                prefix={<LockOutlined />}
                placeholder="请输入密码"
                size="large"
              />
            </Form.Item>

            {isRegisterMode ? (
              <Form.Item
                dependencies={['password']}
                label="确认密码"
                name="confirmPassword"
                rules={[
                  { required: true, message: '请再次输入密码' },
                  ({ getFieldValue }) => ({
                    validator(_, value) {
                      if (!value || getFieldValue('password') === value) {
                        return Promise.resolve()
                      }

                      return Promise.reject(new Error('两次输入的密码不一致'))
                    },
                  }),
                ]}
              >
                <Input.Password
                  autoComplete="new-password"
                  prefix={<LockOutlined />}
                  placeholder="请再次输入密码"
                  size="large"
                />
              </Form.Item>
            ) : null}

            <Button block htmlType="submit" loading={submitting} size="large" type="primary">
              {isRegisterMode ? '注册账号' : '登录'}
            </Button>
          </Form>
        </div>
      </section>
    </main>
  )
}
