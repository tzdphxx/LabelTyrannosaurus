export type PageStateKind = 'idle' | 'loading' | 'empty' | 'error' | 'forbidden'

export interface PageState {
  status: PageStateKind
  message?: string
}

