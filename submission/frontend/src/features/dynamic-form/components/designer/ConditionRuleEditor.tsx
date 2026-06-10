import { Button, Input, Select, Space, Switch, Typography } from 'antd'
import { PlusOutlined } from '@ant-design/icons'
import type { DynamicConditionRule, DynamicConditionLogic, DynamicVisibleRule } from '../../../../types/dynamicForm'
import { visibleOperatorOptions } from '../../utils/designerFields'

interface ConditionRuleEditorProps {
  title: string
  fieldKeys: string[]
  value?: DynamicVisibleRule
  onChange: (value: DynamicVisibleRule | undefined) => void
}

function normalizeConditions(value?: DynamicVisibleRule): DynamicConditionRule[] {
  if (!value) {
    return []
  }

  if (value.conditions?.length) {
    return value.conditions
  }

  if (value.fieldKey && value.operator) {
    return [{ fieldKey: value.fieldKey, operator: value.operator, value: value.value }]
  }

  return []
}

function createCondition(fieldKeys: string[]): DynamicConditionRule {
  return {
    fieldKey: fieldKeys[0] ?? '',
    operator: 'equals',
    value: '',
  }
}

export function ConditionRuleEditor({ title, fieldKeys, value, onChange }: ConditionRuleEditorProps) {
  const enabled = Boolean(value)
  const logic = value?.logic ?? 'and'
  const conditions = normalizeConditions(value)

  function updateConditions(nextConditions: DynamicConditionRule[], nextLogic: DynamicConditionLogic = logic) {
    onChange({
      logic: nextLogic,
      conditions: nextConditions,
    })
  }

  return (
    <Space direction="vertical" size={10}>
      <div className="designer-property__line">
        <Typography.Text>{title}</Typography.Text>
        <Switch checked={enabled} onChange={(checked) => onChange(checked ? { logic: 'and', conditions: [createCondition(fieldKeys)] } : undefined)} />
      </div>

      {enabled ? (
        <Space direction="vertical" size={10}>
          <Select
            options={[
              { label: '全部满足', value: 'and' },
              { label: '任一满足', value: 'or' },
            ]}
            value={logic}
            onChange={(nextLogic) => updateConditions(conditions, nextLogic)}
          />
          {conditions.map((condition, index) => (
            <Space.Compact block key={`${condition.fieldKey}-${index}`}>
              <Select
                options={fieldKeys.map((key) => ({ label: key, value: key }))}
                placeholder="依赖字段"
                value={condition.fieldKey || undefined}
                onChange={(fieldKey) => {
                  const nextConditions = [...conditions]
                  nextConditions[index] = { ...condition, fieldKey }
                  updateConditions(nextConditions)
                }}
              />
              <Select
                options={visibleOperatorOptions}
                value={condition.operator}
                onChange={(operator) => {
                  const nextConditions = [...conditions]
                  nextConditions[index] = { ...condition, operator }
                  updateConditions(nextConditions)
                }}
              />
              {condition.operator === 'empty' || condition.operator === 'notEmpty' ? null : (
                <Input
                  placeholder="比较值"
                  value={String(condition.value ?? '')}
                  onChange={(event) => {
                    const nextConditions = [...conditions]
                    nextConditions[index] = { ...condition, value: event.target.value }
                    updateConditions(nextConditions)
                  }}
                />
              )}
            </Space.Compact>
          ))}
          <Button
            icon={<PlusOutlined />}
            onClick={() => updateConditions([...conditions, createCondition(fieldKeys)])}
            type="dashed"
          >
            增加条件
          </Button>
        </Space>
      ) : null}
    </Space>
  )
}
