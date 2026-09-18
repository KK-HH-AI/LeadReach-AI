import { createApp } from 'vue'
import { createPinia } from 'pinia'
import ElementPlus from 'element-plus'
import zhCn from 'element-plus/es/locale/lang/zh-cn'
import 'element-plus/dist/index.css'
import * as ElementPlusIconsVue from '@element-plus/icons-vue'

import App from './App.vue'
import router from './router'
import { setupI18n } from './locales'
import initDirectives from './directives'
import { setupMock } from './mock'
import './styles/index.scss'

const app = createApp(App)

app.use(createPinia())
app.use(router)

const i18n = setupI18n()
app.use(i18n)

// Element Plus 图标全局注册
for (const [name, comp] of Object.entries(ElementPlusIconsVue)) {
  app.component(name, comp)
}

app.use(ElementPlus, { locale: zhCn })

initDirectives(app)

// 骨架阶段：VITE_MOCK=true 时启用本地 mock
if (import.meta.env.VITE_MOCK === 'true') {
  setupMock()
}

app.mount('#app')

export default app
