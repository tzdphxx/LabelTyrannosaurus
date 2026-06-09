import { Alert, Input } from 'antd'
import { useCallback, useEffect, useMemo, useRef, useState } from 'react'
import type { DynamicFieldOption } from '../../../../types/dynamicForm'
import { getDuplicateOptionValues, optionsToText, textToOptions } from '../../utils/designerFields'

const COMMIT_DELAY_MS = 300

interface ChoiceOptionsEditorProps {
  nodeId: string
  options?: DynamicFieldOption[]
  onCommit: (options: DynamicFieldOption[]) => void
}

export function ChoiceOptionsEditor({ nodeId, options, onCommit }: ChoiceOptionsEditorProps) {
  const optionsText = optionsToText(options)
  const [draftText, setDraftText] = useState(optionsText)
  const activeNodeIdRef = useRef(nodeId)
  const commitTimerRef = useRef<number | null>(null)
  const lastCommittedTextRef = useRef(optionsText)
  const lastCommittedOptionsTextRef = useRef<string | null>(null)
  const duplicateValues = useMemo(() => getDuplicateOptionValues(draftText), [draftText])

  const clearCommitTimer = useCallback(() => {
    if (commitTimerRef.current !== null) {
      window.clearTimeout(commitTimerRef.current)
      commitTimerRef.current = null
    }
  }, [])

  const commitText = useCallback((nextText: string) => {
    clearCommitTimer()

    if (nextText === lastCommittedTextRef.current) {
      return
    }

    const nextOptions = textToOptions(nextText)

    lastCommittedTextRef.current = nextText
    lastCommittedOptionsTextRef.current = optionsToText(nextOptions)
    onCommit(nextOptions)
  }, [clearCommitTimer, onCommit])

  const scheduleCommit = useCallback((nextText: string) => {
    clearCommitTimer()
    commitTimerRef.current = window.setTimeout(() => {
      commitText(nextText)
    }, COMMIT_DELAY_MS)
  }, [clearCommitTimer, commitText])

  useEffect(() => {
    const nodeChanged = activeNodeIdRef.current !== nodeId
    activeNodeIdRef.current = nodeId

    if (nodeChanged) {
      clearCommitTimer()
      lastCommittedOptionsTextRef.current = null
      lastCommittedTextRef.current = optionsText
      setDraftText(optionsText)
      return
    }

    if (optionsText === lastCommittedOptionsTextRef.current) {
      lastCommittedOptionsTextRef.current = null
      return
    }

    lastCommittedTextRef.current = optionsText
    setDraftText(optionsText)
  }, [clearCommitTimer, nodeId, optionsText])

  useEffect(() => clearCommitTimer, [clearCommitTimer])

  return (
    <>
      <Input.TextArea
        autoSize={{ minRows: 4, maxRows: 8 }}
        value={draftText}
        onBlur={() => {
          setDraftText(optionsToText(textToOptions(draftText)))
          commitText(draftText)
        }}
        onChange={(event) => {
          const nextText = event.target.value
          setDraftText(nextText)
          scheduleCommit(nextText)
        }}
      />
      {duplicateValues.length ? (
        <Alert
          message={`选项值重复：${duplicateValues.join(', ')}。保存时会自动改为唯一值，避免单选联动选中。`}
          showIcon
          type="warning"
        />
      ) : null}
    </>
  )
}
