import { describe, expect, it } from 'vitest'
import { getDuplicateOptionValues, optionsToText, textToOptions } from './designerFields'

describe('designerFields', () => {
  it('converts options to editable text', () => {
    expect(
      optionsToText([
        { label: 'Approved', value: 'approved' },
        { label: 'Rejected', value: 'rejected' },
      ]),
    ).toBe('Approved=approved\nRejected=rejected')
  })

  it('parses text to unique option values', () => {
    expect(textToOptions('A=a\nB=a\nOnly Label\nEmpty Value=')).toEqual([
      { label: 'A', value: 'a' },
      { label: 'B', value: 'a_2' },
      { label: 'Only Label', value: 'Only Label' },
      { label: 'Empty Value', value: 'Empty Value' },
    ])
  })

  it('detects duplicate option values before normalization', () => {
    expect(getDuplicateOptionValues('A=a\nB=b\nC=a\nD=b')).toEqual(['a', 'b'])
    expect(getDuplicateOptionValues('A=a\nB=b')).toEqual([])
  })
})
