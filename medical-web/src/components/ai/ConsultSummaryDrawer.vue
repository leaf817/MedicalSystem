<template>
  <el-drawer
    :model-value="visible"
    title="问诊摘要"
    size="420px"
    destroy-on-close
    @update:model-value="$emit('update:visible', $event)"
  >
    <div v-if="summary" class="summary-body">
      <el-descriptions :column="1" border>
        <el-descriptions-item label="主诉">
          {{ summary.chiefComplaint || '—' }}
        </el-descriptions-item>
        <el-descriptions-item label="紧急程度">
          <el-tag :type="urgencyTagType" size="small">{{ urgencyLabel }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="建议科室">
          {{ summary.suggestedDeptName || '—' }}
        </el-descriptions-item>
        <el-descriptions-item label="就医建议">
          {{ summary.medicalAdvice || '—' }}
        </el-descriptions-item>
      </el-descriptions>

      <div v-if="summary.questionsForDoctor?.length" class="questions-block">
        <div class="block-title">建议告知医生的问题</div>
        <ul>
          <li v-for="(q, i) in summary.questionsForDoctor" :key="i">{{ q }}</li>
        </ul>
      </div>

      <div class="drawer-actions">
        <el-button type="primary" @click="$emit('go-appointment', summary)">
          <i class="fa-solid fa-calendar-plus"></i> 去预约
        </el-button>
        <el-button @click="$emit('update:visible', false)">关闭</el-button>
      </div>
    </div>
    <el-empty v-else description="暂无摘要" />
  </el-drawer>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  visible: Boolean,
  summary: {
    type: Object,
    default: null
  }
})

defineEmits(['update:visible', 'go-appointment'])

const urgencyLabel = computed(() => {
  const level = props.summary?.urgencyLevel
  if (level === 'EMERGENCY') return '紧急'
  if (level === 'URGENT') return '较急'
  return '一般'
})

const urgencyTagType = computed(() => {
  const level = props.summary?.urgencyLevel
  if (level === 'EMERGENCY') return 'danger'
  if (level === 'URGENT') return 'warning'
  return 'info'
})
</script>

<style scoped>
.summary-body {
  padding: 0 4px 24px;
}
.questions-block {
  margin-top: 20px;
}
.block-title {
  font-weight: 600;
  color: #334155;
  margin-bottom: 8px;
}
.questions-block ul {
  margin: 0;
  padding-left: 20px;
  color: #475569;
  font-size: 14px;
  line-height: 1.7;
}
.drawer-actions {
  margin-top: 24px;
  display: flex;
  gap: 12px;
}
</style>
