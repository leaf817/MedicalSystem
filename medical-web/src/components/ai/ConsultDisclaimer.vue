<template>
  <div class="consult-disclaimer">
    <el-alert type="warning" :closable="false" show-icon>
      <template #title>
        <span class="disclaimer-title">温馨提示</span>
      </template>
      <p class="disclaimer-text">{{ text }}</p>
    </el-alert>
  </div>
</template>

<script setup>
defineProps({
  text: {
    type: String,
    default:
      '本服务由人工智能提供健康咨询参考，不能替代执业医师面诊。如有胸痛、呼吸困难、大出血、意识不清等紧急情况，请立即拨打 120 或前往急诊科。'
  }
})
</script>

<style scoped>
.consult-disclaimer {
  margin-bottom: 12px;
}
.disclaimer-title {
  font-weight: 600;
}
.disclaimer-text {
  margin: 4px 0 0;
  font-size: 13px;
  line-height: 1.6;
  color: #92400e;
}
</style>
