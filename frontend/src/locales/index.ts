import { createI18n } from 'vue-i18n'
import zhCN from './zh-CN'
import enUS from './en-US'

export type Language = 'zh-CN' | 'en-US'

const STORAGE_KEY = 'lang'

export function getSavedLanguage(): Language {
  const saved = localStorage.getItem(STORAGE_KEY)
  return saved === 'en-US' ? 'en-US' : 'zh-CN'
}

export function setupI18n() {
  const i18n = createI18n({
    legacy: false,
    globalInjection: true,
    locale: getSavedLanguage(),
    fallbackLocale: 'zh-CN',
    messages: {
      'zh-CN': zhCN,
      'en-US': enUS
    }
  })
  return i18n
}

export { STORAGE_KEY }
