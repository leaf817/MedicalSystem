<template>
  <div class="consult-disclaimer">
    <el-alert
      type="warning"
      :closable="false"
      show-icon
      title="温馨提示"
      :description="displayText"
    />
  </div>
</template>

<script setup>
import { computed } from 'vue'

const DEFAULT_TEXT =
  '本服务由人工智能提供健康咨询参考，不能替代执业医师面诊。如有胸痛、呼吸困难、大出血、意识不清等紧急情况，请立即拨打 120 或前往急诊科。'

const props = defineProps({
  text: {
    type: String,
    default: ''
  }
})

const displayText = computed(() => {
  const t = props.text?.trim()
  return t || DEFAULT_TEXT
})
</script>

<style scoped>
.consult-disclaimer :deep(.el-alert) {
  border-radius: 10px;
  border: 1px solid #fde68a;
  background: linear-gradient(135deg, #fffbeb 0%, #fef3c7 100%);
}

.consult-disclaimer :deep(.el-alert__title) {
  font-weight: 600;
  color: #b45309;
}

.consult-disclaimer :deep(.el-alert__description) {
  margin: 4px 0 0;
  font-size: 13px;
  line-height: 1.6;
  color: #92400e;
}
</style>
