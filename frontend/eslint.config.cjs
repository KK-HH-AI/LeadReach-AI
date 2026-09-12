const js = require('@eslint/js')
const pluginVue = require('eslint-plugin-vue')
const tseslint = require('@typescript-eslint/eslint-plugin')
const parserVue = require('vue-eslint-parser')
const tsParser = require('@typescript-eslint/parser')
const prettierRecommended = require('eslint-plugin-prettier/recommended')

module.exports = [
  js.configs.recommended,
  ...pluginVue.configs['flat/recommended'],
  prettierRecommended,
  {
    files: ['**/*.{ts,vue}'],
    languageOptions: {
      parser: parserVue,
      parserOptions: {
        parser: tsParser,
        ecmaVersion: 'latest',
        sourceType: 'module',
        extraFileExtensions: ['.vue']
      }
    },
    plugins: {
      '@typescript-eslint': tseslint
    },
    rules: {
      'vue/multi-word-component-names': 'off',
      'vue/no-v-html': 'off',
      'vue/attribute-hyphenation': 'off',
      '@typescript-eslint/no-explicit-any': 'off'
    }
  },
  {
    ignores: ['dist/**', 'node_modules/**', 'src/auto-imports.d.ts', 'src/components.d.ts']
  }
]