import { create } from 'zustand'

interface NavigationStore {
  collapsed: boolean
  activePath: string
  activeMenuKey: string
  setCollapsed: (collapsed: boolean) => void
  setActivePath: (path: string) => void
  setActiveMenuKey: (key: string) => void
}

export const useNavigationStore = create<NavigationStore>((set) => ({
  collapsed: false,
  activePath: '/',
  activeMenuKey: '',
  setCollapsed: (collapsed) => set({ collapsed }),
  setActivePath: (activePath) => set({ activePath }),
  setActiveMenuKey: (activeMenuKey) => set({ activeMenuKey }),
}))

