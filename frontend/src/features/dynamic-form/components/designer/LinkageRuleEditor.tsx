import { Divider, Input, Space, Typography } from 'antd'
import type { DynamicLinkageRule, DynamicSchemaNode } from '../../../../types/dynamicForm'
import { optionsToText, textToOptions } from '../../utils/designerFields'
import { ConditionRuleEditor } from './ConditionRuleEditor'

interface LinkageRuleEditorProps {
  fieldKeys: string[]
  isChoice: boolean
  value?: DynamicLinkageRule
  onChange: (value: DynamicLinkageRule | undefined) => void
}

function cleanLinkage(value: DynamicLinkageRule): DynamicLinkageRule | undefined {
  if (value.requiredWhen || value.disabledWhen || value.linkedOptions?.length) {
    return value
  }

  return undefined
}

export function LinkageRuleEditor({ fieldKeys, isChoice, value, onChange }: LinkageRuleEditorProps) {
  const linkedCase = value?.linkedOptions?.[0]

  function update(nextValue: DynamicLinkageRule) {
    onChange(cleanLinkage(nextValue))
  }

  return (
    <Space direction="vertical" size={12}>
      <Divider />
      <Typography.Text strong>联动规则</Typography.Text>
      <ConditionRuleEditor
        fieldKeys={fieldKeys}
        title="条件必填"
        value={value?.requiredWhen}
        onChange={(requiredWhen) => update({ ...(value ?? {}), requiredWhen })}
      />
      <ConditionRuleEditor
        fieldKeys={fieldKeys}
        title="条件禁用"
        value={value?.disabledWhen}
        onChange={(disabledWhen) => update({ ...(value ?? {}), disabledWhen })}
      />
      {isChoice ? (
        <>
          <ConditionRuleEditor
            fieldKeys={fieldKeys}
            title="联动选项"
            value={linkedCase?.when}
            onChange={(when) =>
              update({
                ...(value ?? {}),
                linkedOptions: when ? [{ when: { fieldKey: when.conditions?.[0]?.fieldKey ?? '', operator: when.conditions?.[0]?.operator ?? 'equals', value: when.conditions?.[0]?.value }, options: linkedCase?.options ?? [] }] : undefined,
              })
            }
          />
          {linkedCase ? (
            <label className="owner-field">
              <span>命中后的选项</span>
              <Input.TextArea
                autoSize={{ minRows: 3, maxRows: 6 }}
                value={optionsToText(linkedCase.options)}
                onChange={(event) =>
                  update({
                    ...(value ?? {}),
                    linkedOptions: [
                      {
                        ...linkedCase,
                        options: textToOptions(event.target.value),
                      },
                    ],
                  })
                }
              />
            </label>
          ) : null}
        </>
      ) : null}
    </Space>
  )
}

export function isChoiceNode(node: DynamicSchemaNode | null) {
  return node?.type === 'radio' || node?.type === 'checkbox' || node?.type === 'select' || node?.type === 'tagSelect'
}
