import { DeleteOutlined } from '@ant-design/icons'
import { Button, Divider, Empty, Input, Select, Space, Switch, Tag, Typography } from 'antd'
import type { DynamicSchemaNode } from '../../../../types/dynamicForm'
import { optionsToText, textToOptions, visibleOperatorOptions } from '../../utils/designerFields'
import { dynamicMaterialRegistry } from '../../materialRegistry'

interface PropertyPanelProps {
  fieldKeys: string[]
  node: DynamicSchemaNode | null
  onDelete: () => void
  onUpdate: (updates: Partial<DynamicSchemaNode>) => void
}

export function PropertyPanel({ fieldKeys, node, onDelete, onUpdate }: PropertyPanelProps) {
  const isChoice = node?.type === 'radio' || node?.type === 'checkbox' || node?.type === 'select'
  const isField = node?.type !== 'group' && node?.type !== 'tabs' && node?.type !== 'tabPane' && node?.type !== 'showItem'
  const required = Boolean(node?.rules?.some((rule) => rule.type === 'required'))

  if (!node) {
    return <Empty description="选择画布字段后编辑属性" image={Empty.PRESENTED_IMAGE_SIMPLE} />
  }

  return (
    <Space className="designer-property" direction="vertical" size={14}>
      <Tag>{dynamicMaterialRegistry[node.type].title}</Tag>
      <label className="owner-field">
        <span>标题</span>
        <Input value={node.title} onChange={(event) => onUpdate({ title: event.target.value })} />
      </label>
      <label className="owner-field">
        <span>字段 key</span>
        <Input value={node.key} onChange={(event) => onUpdate({ key: event.target.value })} />
      </label>

      {'placeholder' in node.props ? (
        <label className="owner-field">
          <span>占位提示</span>
          <Input
            value={String(node.props.placeholder ?? '')}
            onChange={(event) => onUpdate({ props: { placeholder: event.target.value } })}
          />
        </label>
      ) : null}

      {node.type === 'showItem' ? (
        <label className="owner-field">
          <span>展示内容</span>
          <Input.TextArea
            autoSize={{ minRows: 3, maxRows: 6 }}
            value={String(node.props.text ?? '')}
            onChange={(event) => onUpdate({ props: { text: event.target.value } })}
          />
        </label>
      ) : null}

      {isChoice ? (
        <label className="owner-field">
          <span>选项</span>
          <Input.TextArea
            autoSize={{ minRows: 4, maxRows: 8 }}
            value={optionsToText(node.props.options)}
            onChange={(event) => onUpdate({ props: { options: textToOptions(event.target.value) } })}
          />
        </label>
      ) : null}

      {isField ? (
        <>
          <Divider />
          <div className="designer-property__line">
            <Typography.Text>必填校验</Typography.Text>
            <Switch
              checked={required}
              onChange={(checked) =>
                onUpdate({
                  rules: checked ? [{ type: 'required' }] : node.rules?.filter((rule) => rule.type !== 'required') ?? [],
                })
              }
            />
          </div>
        </>
      ) : null}

      {isField ? (
        <>
          <Divider />
          <div className="designer-property__line">
            <Typography.Text>条件显隐</Typography.Text>
            <Switch
              checked={Boolean(node.visibleWhen)}
              onChange={(checked) =>
                onUpdate({
                  visibleWhen: checked
                    ? {
                      fieldKey: fieldKeys[0] ?? '',
                      operator: 'equals',
                      value: '',
                    }
                    : undefined,
                })
              }
            />
          </div>
          {node.visibleWhen ? (
            <Space direction="vertical" size={10}>
              <Select
                options={fieldKeys.map((key) => ({ label: key, value: key }))}
                placeholder="依赖字段"
                value={node.visibleWhen.fieldKey || undefined}
                onChange={(fieldKey) =>
                  onUpdate({
                    visibleWhen: {
                      ...node.visibleWhen,
                      fieldKey,
                    },
                  })
                }
              />
              <Select
                options={visibleOperatorOptions}
                value={node.visibleWhen.operator}
                onChange={(operator) =>
                  onUpdate({
                    visibleWhen: {
                      ...node.visibleWhen,
                      operator,
                    },
                  })
                }
              />
              {node.visibleWhen.operator === 'empty' || node.visibleWhen.operator === 'notEmpty' ? null : (
                <Input
                  placeholder="比较值"
                  value={String(node.visibleWhen.value ?? '')}
                  onChange={(event) =>
                    onUpdate({
                      visibleWhen: {
                        ...node.visibleWhen,
                        value: event.target.value,
                      },
                    })
                  }
                />
              )}
            </Space>
          ) : null}
        </>
      ) : null}

      <Divider />
      <Button danger icon={<DeleteOutlined />} onClick={onDelete}>
        删除字段
      </Button>
    </Space>
  )
}
