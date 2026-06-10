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

function splitOptionLine(line: string) {
  const separatorIndex = line.indexOf('=')

  if (separatorIndex < 0) {
    return {
      label: line.trim(),
      value: line.trim(),
    }
  }

  const label = line.slice(0, separatorIndex).trim()
  const value = line.slice(separatorIndex + 1).trim()

  return {
    label,
    value: value || label,
  }
}

function toUniqueOptionValue(value: string, usedValues: Set<string>) {
  const baseValue = value.trim() || 'option'
  let nextValue = baseValue
  let suffix = 2

  while (usedValues.has(nextValue)) {
    nextValue = `${baseValue}_${suffix}`
    suffix += 1
  }

  usedValues.add(nextValue)

  return nextValue
}

export function getDuplicateOptionValues(value: string) {
  const counts = new Map<string, number>()

  value
    .split('\n')
    .map((line) => line.trim())
    .filter(Boolean)
    .forEach((line) => {
      const option = splitOptionLine(line)
      const optionValue = option.value.trim()

      if (!optionValue) {
        return
      }

      counts.set(optionValue, (counts.get(optionValue) ?? 0) + 1)
    })

  return Array.from(counts.entries())
    .filter(([, count]) => count > 1)
    .map(([optionValue]) => optionValue)
}

export function textToOptions(value: string): DynamicFieldOption[] {
  const usedValues = new Set<string>()

  return value
    .split('\n')
    .map((line) => line.trim())
    .filter(Boolean)
    .map((line) => {
      const option = splitOptionLine(line)

      return {
        label: option.label,
        value: toUniqueOptionValue(option.value, usedValues),
      }
    })
}
