<template>
  <div ref="listRef" class="chat-message-list">
    <div
      v-for="msg in visibleMessages"
      :key="msg.messageId || msg._localId"
      class="message-row"
      :class="msg.role"
    >
      <div class="avatar">
        <i :class="msg.role === 'user' ? 'fa-solid fa-user' : 'fa-solid fa-robot'"></i>
      </div>
      <div class="bubble-wrap">
        <div class="bubble" :class="{ emergency: msg.urgencyHint === 'EMERGENCY' }">
          <p class="bubble-text">{{ msg.content }}</p>
        </div>
      </div>
    </div>

    <div v-if="loading" class="message-row assistant">
      <div class="avatar"><i class="fa-solid fa-robot"></i></div>
      <div class="bubble-wrap">
        <div class="bubble typing">
          <span class="dot"></span><span class="dot"></span><span class="dot"></span>
          <span class="typing-label">正在输入…</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, nextTick, ref, watch } from 'vue'

const props = defineProps({
  messages: {
    type: Array,
    default: () => []
  },
  loading: {
    type: Boolean,
    default: false
  }
})

const listRef = ref(null)

const visibleMessages = computed(() =>
  (props.messages || []).filter((m) => m.role === 'user' || m.role === 'assistant')
)

const scrollToBottom = () => {
  nextTick(() => {
    const el = listRef.value
    if (el) {
      el.scrollTop = el.scrollHeight
    }
  })
}

watch(
  () => [props.messages?.length, props.loading],
  () => scrollToBottom(),
  { deep: true }
)

defineExpose({ scrollToBottom })
</script>

<style scoped>
.chat-message-list {
  flex: 1;
  overflow-y: auto;
  padding: 16px;
  background: #f8fafc;
  border-radius: 12px;
  min-height: 320px;
  max-height: calc(100vh - 380px);
}

.message-row {
  display: flex;
  gap: 10px;
  margin-bottom: 16px;
}

.message-row.user {
  flex-direction: row-reverse;
}

.avatar {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  font-size: 16px;
}

.message-row.assistant .avatar {
  background: linear-gradient(135deg, #6366f1, #4f46e5);
  color: #fff;
}

.message-row.user .avatar {
  background: #e2e8f0;
  color: #475569;
}

.bubble-wrap {
  max-width: 75%;
}

.message-row.user .bubble-wrap {
  display: flex;
  justify-content: flex-end;
}

.bubble {
  padding: 10px 14px;
  border-radius: 12px;
  background: #fff;
  border: 1px solid #e2e8f0;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.04);
}

.message-row.user .bubble {
  background: #4f46e5;
  border-color: #4f46e5;
  color: #fff;
}

.bubble.emergency {
  border-color: #f87171;
  background: #fef2f2;
}

.message-row.user .bubble.emergency {
  background: #dc2626;
  border-color: #dc2626;
}

.bubble-text {
  margin: 0;
  font-size: 14px;
  line-height: 1.6;
  white-space: pre-wrap;
  word-break: break-word;
}

.bubble.typing {
  display: flex;
  align-items: center;
  gap: 6px;
}

.dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #94a3b8;
  animation: blink 1.2s infinite;
}

.dot:nth-child(2) {
  animation-delay: 0.2s;
}
.dot:nth-child(3) {
  animation-delay: 0.4s;
}

.typing-label {
  font-size: 13px;
  color: #64748b;
  margin-left: 4px;
}

@keyframes blink {
  0%,
  80%,
  100% {
    opacity: 0.3;
  }
  40% {
    opacity: 1;
  }
}
</style>
