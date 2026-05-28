<template>
  <div class="chat-input-bar">
    <div v-if="chips.length" class="quick-chips">
      <span class="chips-label">快捷描述：</span>
      <el-tag
        v-for="chip in chips"
        :key="chip"
        class="chip"
        effect="plain"
        :disabled="disabled"
        @click="appendChip(chip)"
      >
        {{ chip }}
      </el-tag>
    </div>
    <div class="input-row">
      <el-input
        v-model="inputText"
        type="textarea"
        :rows="2"
        :disabled="disabled"
        placeholder="请描述您的不适，例如：头痛三天，伴有低烧…"
        resize="none"
        @keydown.enter.exact.prevent="handleSend"
      />
      <el-button
        type="primary"
        class="send-btn"
        :disabled="disabled || !inputText.trim()"
        :loading="disabled"
        @click="handleSend"
      >
        <i class="fa-solid fa-paper-plane"></i> 发送
      </el-button>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'

const props = defineProps({
  disabled: {
    type: Boolean,
    default: false
  },
  chips: {
    type: Array,
    default: () => ['发热', '头痛', '咳嗽', '腹痛', '乏力', '恶心']
  }
})

const emit = defineEmits(['send'])

const inputText = ref('')

const appendChip = (chip) => {
  if (props.disabled) return
  inputText.value = inputText.value ? `${inputText.value}，${chip}` : chip
}

const handleSend = () => {
  const text = inputText.value.trim()
  if (!text || props.disabled) return
  emit('send', text)
  inputText.value = ''
}

defineExpose({
  setInput: (val) => {
    inputText.value = val || ''
  }
})
</script>

<style scoped>
.chat-input-bar {
  border-top: 1px solid #e2e8f0;
  padding-top: 12px;
  margin-top: 12px;
}

.quick-chips {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
  margin-bottom: 10px;
}

.chips-label {
  font-size: 13px;
  color: #64748b;
}

.chip {
  cursor: pointer;
}

.input-row {
  display: flex;
  gap: 12px;
  align-items: flex-end;
}

.input-row :deep(.el-textarea) {
  flex: 1;
}

.send-btn {
  height: 40px;
  flex-shrink: 0;
}
</style>
