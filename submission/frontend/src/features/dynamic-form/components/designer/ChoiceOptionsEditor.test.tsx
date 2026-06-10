import { fireEvent, render, screen } from '@testing-library/react'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { ChoiceOptionsEditor } from './ChoiceOptionsEditor'

describe('ChoiceOptionsEditor', () => {
  beforeEach(() => {
    vi.useFakeTimers()
  })

  afterEach(() => {
    vi.runOnlyPendingTimers()
    vi.useRealTimers()
  })

  it('commits option changes after debounce', () => {
    const onCommit = vi.fn()
    render(<ChoiceOptionsEditor nodeId="field-1" options={[{ label: 'A', value: 'a' }]} onCommit={onCommit} />)

    fireEvent.change(screen.getByRole('textbox'), { target: { value: 'A=a\nB=b' } })
    vi.advanceTimersByTime(119)
    expect(onCommit).not.toHaveBeenCalled()

    vi.advanceTimersByTime(1)
    expect(onCommit).toHaveBeenCalledWith([
      { label: 'A', value: 'a' },
      { label: 'B', value: 'b' },
    ])
  })

  it('normalizes duplicate values on blur', () => {
    const onCommit = vi.fn()
    render(<ChoiceOptionsEditor nodeId="field-1" options={[]} onCommit={onCommit} />)
    const textarea = screen.getByRole('textbox') as HTMLTextAreaElement

    fireEvent.change(textarea, { target: { value: 'A=a\nB=a' } })
    fireEvent.blur(textarea)

    expect(textarea.value).toBe('A=a\nB=a_2')
    expect(onCommit).toHaveBeenLastCalledWith([
      { label: 'A', value: 'a' },
      { label: 'B', value: 'a_2' },
    ])
  })

  it('resets draft text when active node changes', () => {
    const onCommit = vi.fn()
    const { rerender } = render(<ChoiceOptionsEditor nodeId="field-1" options={[{ label: 'A', value: 'a' }]} onCommit={onCommit} />)
    const textarea = screen.getByRole('textbox') as HTMLTextAreaElement

    fireEvent.change(textarea, { target: { value: 'Unsaved=unsaved' } })
    rerender(<ChoiceOptionsEditor nodeId="field-2" options={[{ label: 'B', value: 'b' }]} onCommit={onCommit} />)

    expect(textarea.value).toBe('B=b')
  })
})
