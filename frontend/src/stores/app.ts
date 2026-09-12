import { defineStore } from 'pinia'

interface AppState {
  sidebarCollapsed: boolean
  /** 语言标识，配合 vue-i18n / Element Plus */
  language: string
  /** 已打开的多页签 */
  tabs: { path: string; title: string }[]
}

const STORAGE_KEY = 'app'

function loadState(): Partial<AppState> {
  try {
    return JSON.parse(localStorage.getItem(STORAGE_KEY) || '{}')
  } catch {
    return {}
  }
}

export const useAppStore = defineStore('app', {
  state: (): AppState => {
    const saved = loadState()
    return {
      sidebarCollapsed: saved.sidebarCollapsed ?? false,
      language: saved.language ?? 'zh-CN',
      tabs: saved.tabs ?? []
    }
  },
  getters: {
    isCollapsed: (s) => s.sidebarCollapsed
  },
  actions: {
    toggleSidebar() {
      this.sidebarCollapsed = !this.sidebarCollapsed
      this.persist()
    },
    setLanguage(lang: string) {
      this.language = lang
      this.persist()
    },
    addTab(tab: { path: string; title: string }) {
      if (!this.tabs.some((t) => t.path === tab.path)) {
        this.tabs.push(tab)
      }
      this.persist()
    },
    removeTab(path: string): string | undefined {
      const idx = this.tabs.findIndex((t) => t.path === path)
      if (idx === -1) return undefined
      this.tabs.splice(idx, 1)
      this.persist()
      const next = this.tabs[idx] || this.tabs[idx - 1]
      return next?.path
    },
    persist() {
      localStorage.setItem(STORAGE_KEY, JSON.stringify({ ...this.$state }))
    }
  }
})
