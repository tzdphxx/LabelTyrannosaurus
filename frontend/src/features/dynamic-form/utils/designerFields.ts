import type { DynamicFieldOption, DynamicVisibleOperator } from '../../../types/dynamicForm'

export const visibleOperatorOptions: Array<{ label: string; value: DynamicVisibleOperator }> = [
  { label: '等于', value: 'equals' },
  { label: '不等于', value: 'notEquals' },
  { label: '包含', value: 'contains' },
  { label: '为空', value: 'empty' },
  { label: '不为空', value: 'notEmpty' },
]

export function optionsToText(options?: DynamicFieldOption[]) {
  return options?.map((option) => `${option.label}=${option.value}`).join('\n') ?? ''
}

export function textToOptions(value: string): DynamicFieldOption[] {
  return value
    .split('\n')
    .map((line) => line.trim())
    .filter(Boolean)
    .map((line) => {
      const [label, optionValue] = line.split('=')

      return {
        label: label.trim(),
        value: (optionValue ?? label).trim(),
      }
    })
}
