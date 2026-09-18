<template>
  <div class="error-page">
    <el-result
      icon="error"
      title="500"
      sub-title="服务异常，请稍后重试（traceId 请反馈给技术支持）"
    >
      <template #extra>
        <el-button type="primary" @click="$router.push('/')">返回首页</el-button>
      </template>
    </el-result>
  </div>
</template>

<script setup lang="ts"></script>

<style scoped>
.error-page {
  height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
}
</style>
