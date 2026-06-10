import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import { describe, expect, it, vi } from 'vitest'
import type { DynamicFormSchema } from '../../../types/dynamicForm'
import { DynamicFormRenderer } from './DynamicFormRenderer'

function createSchema(): DynamicFormSchema {
  return {
    id: 'template-1',
    version: 'v1',
    title: 'Template',
    nodes: [
      {
        id: 'answer-1',
        key: 'answer',
        type: 'input',
        title: 'Answer',
        props: {
          placeholder: 'Input answer',
        },
      },
    ],
  }
}

describe('DynamicFormRenderer', () => {
  it('renders schema fields and submits form values', async () => {
    const onSubmit = vi.fn()

    render(
      <DynamicFormRenderer
        initialValues={{ answer: 'ready' }}
        schema={createSchema()}
        submitText="Submit"
        onSubmit={onSubmit}
      />,
    )

    expect(screen.getByPlaceholderText('Input answer')).toBeInTheDocument()
    fireEvent.click(screen.getByRole('button', { name: 'Submit' }))

    await waitFor(() => {
      expect(onSubmit).toHaveBeenCalledWith({
        templateId: 'template-1',
        schemaVersion: 'v1',
        values: { answer: 'ready' },
      })
    })
  })

  it('hides submit action in read only mode', () => {
    render(<DynamicFormRenderer readOnly schema={createSchema()} submitText="Submit" />)

    expect(screen.queryByRole('button', { name: 'Submit' })).not.toBeInTheDocument()
  })
})
