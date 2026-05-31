import { DeleteOutlined } from '@ant-design/icons'
import { Button, Divider, Empty, Input, InputNumber, Space, Switch, Tag } from 'antd'
import type { DynamicSchemaNode } from '../../../../types/dynamicForm'
import { optionsToText, textToOptions } from '../../utils/designerFields'
import { dynamicMaterialRegistry } from '../../materialRegistry'
import { ConditionRuleEditor } from './ConditionRuleEditor'
import { isChoiceNode, LinkageRuleEditor } from './LinkageRuleEditor'

interface PropertyPanelProps {
  fieldKeys: string[]
  node: DynamicSchemaNode | null
  onDelete: () => void
  onUpdate: (updates: Partial<DynamicSchemaNode>) => void
}

export function PropertyPanel({ fieldKeys, node, onDelete, onUpdate }: PropertyPanelProps) {
  const isChoice = isChoiceNode(node)
  const isField = node?.type !== 'group' && node?.type !== 'tabs' && node?.type !== 'tabPane' && node?.type !== 'showItem' && node?.type !== 'llmPrompt'
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

      {node.type === 'llmPrompt' ? (
        <>
          <label className="owner-field">
            <span>提示词</span>
            <Input.TextArea
              autoSize={{ minRows: 3, maxRows: 6 }}
              value={String(node.props.prompt ?? '')}
              onChange={(event) => onUpdate({ props: { prompt: event.target.value } })}
            />
          </label>
          <label className="owner-field">
            <span>占位回复</span>
            <Input.TextArea
              autoSize={{ minRows: 2, maxRows: 5 }}
              value={String(node.props.text ?? '')}
              onChange={(event) => onUpdate({ props: { text: event.target.value } })}
            />
          </label>
        </>
      ) : null}

      {node.type === 'fileUpload' ? (
        <>
          <label className="owner-field">
            <span>允许类型</span>
            <Input value={String(node.props.accept ?? '')} onChange={(event) => onUpdate({ props: { accept: event.target.value } })} />
          </label>
          <label className="owner-field">
            <span>最大文件数</span>
            <InputNumber min={1} max={20} value={Number(node.props.maxCount ?? 1)} onChange={(maxCount) => onUpdate({ props: { maxCount: maxCount ?? 1 } })} />
          </label>
        </>
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
            <span>必填校验</span>
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
          <ConditionRuleEditor fieldKeys={fieldKeys} title="条件显隐" value={node.visibleWhen} onChange={(visibleWhen) => onUpdate({ visibleWhen })} />
          <LinkageRuleEditor fieldKeys={fieldKeys} isChoice={isChoice} value={node.linkage} onChange={(linkage) => onUpdate({ linkage })} />
        </>
      ) : null}

      <Divider />
      <Button danger icon={<DeleteOutlined />} onClick={onDelete}>
        删除字段
      </Button>
    </Space>
  )
}
