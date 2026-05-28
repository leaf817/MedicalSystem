<template>
  <div class="chat-input-bar">
    <div v-if="chips.length" class="quick-chips">
      <span class="chips-label"><i class="fa-solid fa-bolt"></i> 快捷描述</span>
      <el-tag
        v-for="chip in chips"
        :key="chip"
        class="chip"
        round
        effect="light"
        :disabled="disabled"
        @click="appendChip(chip)"
      >
        {{ chip }}
      </el-tag>
    </div>
    <div class="input-shell">
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
        round
        :disabled="disabled || !inputText.trim()"
        :loading="disabled"
        @click="handleSend"
      >
        <i class="fa-solid fa-paper-plane"></i>
        <span>发送</span>
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
  padding-top: 4px;
}

.quick-chips {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
  margin-bottom: 12px;
}

.chips-label {
  font-size: 13px;
  color: #64748b;
  font-weight: 500;
}

.chips-label i {
  color: #6366f1;
  margin-right: 4px;
}

.chip {
  cursor: pointer;
  border-color: #e0e7ff !important;
  color: #4f46e5 !important;
  background: #eef2ff !important;
}

.chip:hover:not(.is-disabled) {
  background: #e0e7ff !important;
}

.input-shell {
  display: flex;
  gap: 12px;
  align-items: flex-end;
  padding: 12px 14px;
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 14px;
  transition: border-color 0.2s, box-shadow 0.2s;
}

.input-shell:focus-within {
  border-color: #a5b4fc;
  box-shadow: 0 0 0 3px rgba(99, 102, 241, 0.12);
}

.input-shell :deep(.el-textarea__inner) {
  background: transparent;
  box-shadow: none;
  padding: 4px 0;
}

.send-btn {
  height: 42px;
  padding: 0 20px;
  flex-shrink: 0;
  background: linear-gradient(135deg, #6366f1, #4f46e5);
  border: none;
}

.send-btn i {
  margin-right: 6px;
}
</style>
