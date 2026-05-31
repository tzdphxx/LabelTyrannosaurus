import { create } from 'zustand'
import type { PageState } from '../types/page'

interface PageUiStore {
  pageState: PageState
  setPageState: (pageState: PageState) => void
  resetPageState: () => void
}

const initialPageState: PageState = {
  status: 'idle',
}

export const usePageUiStore = create<PageUiStore>((set) => ({
  pageState: initialPageState,
  setPageState: (pageState) => set({ pageState }),
  resetPageState: () => set({ pageState: initialPageState }),
}))

