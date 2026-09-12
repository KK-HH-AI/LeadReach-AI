import { permission } from './permission'
import type { App } from 'vue'

export default function initDirectives(app: App) {
  app.directive('permission', permission)
}
