<template>
  <div ref="listRef" class="chat-message-list">
    <div v-if="visibleMessages.length === 0 && !loading" class="chat-empty">
      <i class="fa-solid fa-comments"></i>
      <p>向助手描述您的不适，开始预问诊</p>
    </div>

    <div
      v-for="msg in visibleMessages"
      :key="msg.messageId || msg._localId"
      class="message-row"
      :class="msg.role"
    >
      <el-avatar
        v-if="msg.role === 'user'"
        :size="40"
        :src="userAvatarUrl || undefined"
        class="msg-avatar avatar-login"
      >
        {{ userAvatarInitial }}
      </el-avatar>
      <el-avatar v-else :size="40" class="msg-avatar avatar-assistant">
        <i class="fa-solid fa-robot"></i>
      </el-avatar>

      <div class="bubble-wrap">
        <div class="bubble" :class="{ emergency: msg.urgencyHint === 'EMERGENCY' }">
          <p class="bubble-text">{{ msg.content }}</p>
        </div>
      </div>
    </div>

    <div v-if="loading" class="message-row assistant">
      <el-avatar :size="40" class="msg-avatar avatar-assistant">
        <i class="fa-solid fa-robot"></i>
      </el-avatar>
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
  },
  userAvatarUrl: {
    type: String,
    default: ''
  },
  userAvatarInitial: {
    type: String,
    default: '患'
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
  min-height: 0;
  height: 100%;
  overflow-y: auto;
  overflow-x: hidden;
  padding: 20px 16px;
  background: linear-gradient(180deg, #f1f5f9 0%, #f8fafc 48%, #eef2ff 100%);
  border-radius: 14px;
  border: 1px solid #e2e8f0;
  box-sizing: border-box;
}

.chat-empty {
  height: 100%;
  min-height: 160px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  color: #94a3b8;
  gap: 10px;
}

.chat-empty i {
  font-size: 40px;
  opacity: 0.5;
}

.chat-empty p {
  margin: 0;
  font-size: 14px;
}

.message-row {
  display: flex;
  gap: 12px;
  margin-bottom: 20px;
  align-items: flex-start;
}

.message-row.user {
  flex-direction: row-reverse;
}

.msg-avatar {
  flex-shrink: 0;
  box-shadow: 0 2px 8px rgba(15, 23, 42, 0.08);
}

/* 与 AppHeader 登录头像一致 */
.avatar-login {
  background: linear-gradient(135deg, #e8a54b, #d48232);
  color: #fff;
  font-weight: 600;
  font-size: 16px;
}

.avatar-assistant {
  background: linear-gradient(135deg, #6366f1, #4f46e5);
  color: #fff;
  font-size: 18px;
}

.bubble-wrap {
  max-width: min(88%, 720px);
  display: flex;
  flex-direction: column;
}

.message-row.user .bubble-wrap {
  align-items: flex-end;
}

.message-row.assistant .bubble-wrap {
  align-items: flex-start;
}

.bubble {
  padding: 12px 16px;
  border-radius: 16px;
  background: #fff;
  border: 1px solid #e2e8f0;
  box-shadow: 0 2px 8px rgba(15, 23, 42, 0.06);
}

.message-row.assistant .bubble {
  border-bottom-left-radius: 4px;
}

.message-row.user .bubble {
  background: linear-gradient(135deg, #6366f1, #4f46e5);
  border: none;
  color: #fff;
  border-bottom-right-radius: 4px;
  box-shadow: 0 4px 12px rgba(79, 70, 229, 0.25);
}

.bubble.emergency {
  border-color: #fca5a5;
  background: #fef2f2;
  color: #991b1b;
}

.message-row.user .bubble.emergency {
  background: linear-gradient(135deg, #ef4444, #dc2626);
  color: #fff;
}

.bubble-text {
  margin: 0;
  font-size: 14px;
  line-height: 1.65;
  white-space: pre-wrap;
  word-break: break-word;
}

.bubble.typing {
  display: flex;
  align-items: center;
  gap: 6px;
  min-width: 120px;
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
