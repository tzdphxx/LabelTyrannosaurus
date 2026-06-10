import { DeleteOutlined } from '@ant-design/icons'
import { Button, Divider, Empty, Input, InputNumber, Space, Switch, Tag } from 'antd'
import type { DynamicFieldOption, DynamicSchemaNode, DynamicValidationRule } from '../../../../types/dynamicForm'
import { dynamicMaterialRegistry } from '../../materialRegistry'
import { ChoiceOptionsEditor } from './ChoiceOptionsEditor'
import { ConditionRuleEditor } from './ConditionRuleEditor'
import { isChoiceNode, LinkageRuleEditor } from './LinkageRuleEditor'

function targetFieldsToText(value: unknown) {
  return Array.isArray(value) ? value.join('\n') : ''
}

function textToTargetFields(value: string) {
  return value
    .split(/\r?\n|,/)
    .map((item) => item.trim())
    .filter(Boolean)
}

interface PropertyPanelProps {
  fieldKeys: string[]
  node: DynamicSchemaNode | null
  onDelete: () => void
  onUpdate: (updates: Partial<DynamicSchemaNode>) => void
}

function syncChoiceEnumRule(node: DynamicSchemaNode, options: DynamicFieldOption[]) {
  const rules = node.rules ?? []
  const enumRuleIndex = rules.findIndex((rule) => rule.type === 'enum')
  const values = options.map((option) => option.value)
  const shouldKeepEnumRule = node.type === 'radio' || node.type === 'checkbox' || enumRuleIndex >= 0

  if (!shouldKeepEnumRule) {
    return rules
  }

  if (!values.length) {
    return rules.filter((rule) => rule.type !== 'enum')
  }

  if (enumRuleIndex < 0) {
    return [...rules, { type: 'enum', values } satisfies DynamicValidationRule]
  }

  return rules.map((rule) => (rule.type === 'enum' ? { ...rule, values } : rule))
}

function syncRequiredRule(rules: DynamicValidationRule[] | undefined, checked: boolean) {
  const currentRules = rules ?? []

  if (!checked) {
    return currentRules.filter((rule) => rule.type !== 'required')
  }

  if (currentRules.some((rule) => rule.type === 'required')) {
    return currentRules
  }

  return [{ type: 'required' } satisfies DynamicValidationRule, ...currentRules]
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
            <span>Provider ID</span>
            <Input
              value={String(node.props.providerId ?? '')}
              onChange={(event) => onUpdate({ props: { providerId: event.target.value } })}
            />
          </label>
          <label className="owner-field">
            <span>模型</span>
            <Input
              value={String(node.props.modelName ?? '')}
              onChange={(event) => onUpdate({ props: { modelName: event.target.value } })}
            />
          </label>
          <label className="owner-field">
            <span>Prompt 模板</span>
            <Input.TextArea
              autoSize={{ minRows: 4, maxRows: 8 }}
              value={String(node.props.promptTemplate ?? node.props.prompt ?? '')}
              onChange={(event) => onUpdate({ props: { promptTemplate: event.target.value, prompt: event.target.value } })}
            />
          </label>
          <label className="owner-field">
            <span>目标字段</span>
            <Input.TextArea
              autoSize={{ minRows: 3, maxRows: 6 }}
              placeholder="每行一个字段 key，例如 answer_text"
              value={targetFieldsToText(node.props.targetFields)}
              onChange={(event) => onUpdate({ props: { targetFields: textToTargetFields(event.target.value) } })}
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
          <ChoiceOptionsEditor
            nodeId={node.id}
            options={node.props.options}
            onCommit={(options) => onUpdate({ props: { options }, rules: syncChoiceEnumRule(node, options) })}
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
                  rules: syncRequiredRule(node.rules, checked),
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
