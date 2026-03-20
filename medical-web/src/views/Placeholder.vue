<template>
  <div class="placeholder-page">
    <el-empty description="功能开发中，敬请期待">
      <el-button type="primary" @click="$router.push('/admin/dashboard')">返回工作台</el-button>
    </el-empty>
  </div>
</template>

<style scoped>
.placeholder-page {
  padding: 60px;
  text-align: center;
}
</style>
