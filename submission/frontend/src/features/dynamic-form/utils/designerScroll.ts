export function scrollNodeIntoCanvasView(scrollContainer: HTMLDivElement | null, nodeId: string) {
  window.requestAnimationFrame(() => {
    if (!scrollContainer) {
      return
    }

    const nodeElement = scrollContainer.querySelector<HTMLElement>(`[data-node-id="${nodeId}"]`)

    if (nodeElement) {
      const containerRect = scrollContainer.getBoundingClientRect()
      const nodeRect = nodeElement.getBoundingClientRect()
      const bottomDelta = nodeRect.bottom - containerRect.bottom + 16

      if (bottomDelta > 0) {
        scrollContainer.scrollBy({ top: bottomDelta, behavior: 'smooth' })
      }

      return
    }

    scrollContainer.scrollTo({ top: scrollContainer.scrollHeight, behavior: 'smooth' })
  })
}
