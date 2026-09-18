<template>
  <el-container class="default-layout">
    <el-aside :width="isCollapsed ? '64px' : '220px'" class="default-layout__aside">
      <AppLogo :collapsed="isCollapsed" />
      <Sidebar />
    </el-aside>
    <el-container>
      <el-header class="default-layout__header">
        <Navbar />
      </el-header>
      <TabsBar />
      <el-main class="default-layout__main">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useAppStore } from '@/stores/app'
import AppLogo from './components/AppLogo.vue'
import Sidebar from './components/Sidebar.vue'
import Navbar from './components/Navbar.vue'
import TabsBar from './components/TabsBar.vue'

const appStore = useAppStore()
const isCollapsed = computed(() => appStore.isCollapsed)
</script>

<style scoped lang="scss">
.default-layout {
  height: 100vh;

  &__aside {
    border-right: 1px solid var(--el-border-color-light);
    transition: width 0.2s;
    display: flex;
    flex-direction: column;
  }

  &__header {
    padding: 0;
  }

  &__main {
    padding: 0;
    background: var(--el-fill-color-lighter);
    overflow: auto;
  }
}
</style>
