<template>
  <div class="ai-consult-page">
    <div class="page-header">
      <div class="header-left">
        <div class="header-icon-wrap">
          <i class="fa-solid fa-robot"></i>
        </div>
        <div class="header-text">
          <div class="title-row">
            <h2 class="page-title">智能问诊助手</h2>
            <span class="page-disclaimer">
              <i class="fa-solid fa-circle-info"></i>
              {{ displayDisclaimer }}
            </span>
          </div>
        </div>
      </div>
      <div class="header-actions">
        <el-button round @click="historyVisible = true">
          <i class="fa-solid fa-clock-rotate-left"></i> 历史会话
        </el-button>
        <el-button type="primary" round :disabled="pageLoading" @click="handleNewSession">
          <i class="fa-solid fa-plus"></i> 新建会话
        </el-button>
      </div>
    </div>

    <div class="content-card" v-loading="pageLoading">
      <div v-if="sessionId" class="card-fixed-top">
        <div class="session-meta">
          <el-tag size="small" type="info">会话 {{ sessionNo || sessionId }}</el-tag>
          <el-tag size="small" :type="statusTagType">{{ statusText }}</el-tag>
        </div>
      </div>

      <div class="chat-panel">
        <ChatMessageList
          ref="messageListRef"
          :messages="messages"
          :loading="sending"
          :user-avatar-url="userAvatarUrl"
          :user-avatar-initial="userAvatarInitial"
        />
      </div>

      <div class="card-fixed-bottom">
        <ChatInputBar
          v-if="isActive"
          :disabled="sending"
          @send="handleSend"
        />
        <div v-if="sessionId" class="footer-actions">
          <template v-if="isActive">
            <el-button type="warning" plain :loading="ending" @click="handleEndSession">
              结束问诊
            </el-button>
          </template>
          <template v-else>
            <el-button type="primary" plain @click="openSummary">
              查看摘要
            </el-button>
            <el-button
              v-if="lastSummary?.suggestedDeptId || lastSummary?.suggestedDeptName"
              type="primary"
              @click="goAppointment(lastSummary)"
            >
              去预约
            </el-button>
          </template>
        </div>
      </div>
    </div>

    <!-- 历史会话 -->
    <el-drawer v-model="historyVisible" title="历史问诊" size="400px" class="history-drawer">
      <div v-loading="historyLoading">
        <div
          v-for="item in historyList"
          :key="item.sessionId"
          class="history-item"
          @click="openHistorySession(item)"
        >
          <div class="history-title">{{ item.title || '问诊会话' }}</div>
          <div class="history-meta">
            <span>{{ formatTime(item.createdTime) }}</span>
            <el-tag size="small" :type="item.status === 1 ? 'success' : 'info'">
              {{ sessionStatusLabel(item.status) }}
            </el-tag>
          </div>
        </div>
        <el-empty v-if="!historyLoading && historyList.length === 0" description="暂无历史会话" />
      </div>
    </el-drawer>

    <ConsultSummaryDrawer
      v-model:visible="summaryVisible"
      :summary="summaryData"
      @go-appointment="goAppointment"
    />
  </div>
</template>

<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import {
  getUserInfoFromStorage,
  getUserAvatarInitial
} from '@/utils/user-session'
import { useRoute, useRouter, onBeforeRouteLeave } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import ChatMessageList from '@/components/ai/ChatMessageList.vue'
import ChatInputBar from '@/components/ai/ChatInputBar.vue'
import ConsultSummaryDrawer from '@/components/ai/ConsultSummaryDrawer.vue'
import {
  createAiSession,
  listAiSessions,
  getAiSessionDetail,
  sendAiMessage,
  endAiSession,
  getAiSessionSummary
} from '@/api/ai-consult'

const DEFAULT_DISCLAIMER =
  '本服务由人工智能提供健康咨询参考，不能替代执业医师面诊。如有胸痛、呼吸困难、大出血、意识不清等紧急情况，请立即拨打 120 或前往急诊科。'

const route = useRoute()
const router = useRouter()

const userInfo = computed(() => getUserInfoFromStorage())
const userAvatarUrl = computed(() => userInfo.value?.avatarUrl || '')
const userAvatarInitial = computed(() => getUserAvatarInitial(userInfo.value))

const pageLoading = ref(false)
const sending = ref(false)
const ending = ref(false)
const historyVisible = ref(false)
const historyLoading = ref(false)
const historyList = ref([])

const sessionId = ref(null)
const sessionNo = ref('')
const sessionStatus = ref(1)
const disclaimer = ref(DEFAULT_DISCLAIMER)
const messages = ref([])
const lastSummary = ref(null)

const summaryVisible = ref(false)
const summaryData = ref(null)
const messageListRef = ref(null)

const displayDisclaimer = computed(() => {
  const t = disclaimer.value?.trim()
  return t || DEFAULT_DISCLAIMER
})

const isActive = computed(() => sessionStatus.value === 1)

const statusText = computed(() => sessionStatusLabel(sessionStatus.value))

const statusTagType = computed(() => {
  if (sessionStatus.value === 1) return 'success'
  if (sessionStatus.value === 2) return 'info'
  return 'warning'
})

function sessionStatusLabel(status) {
  if (status === 1) return '进行中'
  if (status === 2) return '已结束'
  if (status === 3) return '已转预约'
  return '未知'
}

function formatTime(t) {
  if (!t) return '—'
  return String(t).replace('T', ' ').slice(0, 16)
}

const applySessionDetail = (detail) => {
  sessionId.value = detail.sessionId
  sessionNo.value = detail.sessionNo
  sessionStatus.value = detail.status ?? 1
  messages.value = (detail.messages || []).map((m) => ({
    ...m,
    urgencyHint: m.role === 'assistant' && detail.urgencyLevel === 'EMERGENCY' ? 'EMERGENCY' : null
  }))
}

const loadSession = async (id) => {
  pageLoading.value = true
  try {
    const detail = await getAiSessionDetail(id)
    applySessionDetail(detail)
    if (detail.status !== 1) {
      try {
        lastSummary.value = await getAiSessionSummary(id)
      } catch {
        lastSummary.value = null
      }
    }
    router.replace({ path: '/patient/ai-consult', query: { sessionId: id } })
  } finally {
    pageLoading.value = false
  }
}

const startNewSession = async () => {
  pageLoading.value = true
  try {
    const res = await createAiSession()
    sessionId.value = res.sessionId
    sessionNo.value = res.sessionNo
    sessionStatus.value = 1
    disclaimer.value = res.disclaimer?.trim() || DEFAULT_DISCLAIMER
    lastSummary.value = null
    messages.value = [
      {
        messageId: `w-${Date.now()}`,
        role: 'assistant',
        content: res.welcomeMessage
      }
    ]
    router.replace({ path: '/patient/ai-consult', query: { sessionId: res.sessionId } })
  } finally {
    pageLoading.value = false
  }
}

const handleNewSession = async () => {
  if (isActive.value && messages.value.length > 2) {
    try {
      await ElMessageBox.confirm('当前会话尚未结束，是否新建会话？', '提示', {
        type: 'warning',
        confirmButtonText: '新建',
        cancelButtonText: '取消'
      })
    } catch {
      return
    }
  }
  await startNewSession()
}

const handleSend = async (content) => {
  if (!sessionId.value || !isActive.value) return
  sending.value = true
  const localUserId = `u-${Date.now()}`
  messages.value.push({
    messageId: localUserId,
    role: 'user',
    content
  })
  try {
    const res = await sendAiMessage(sessionId.value, content)
    messages.value.push({
      messageId: res.assistantMessageId,
      role: 'assistant',
      content: res.reply,
      urgencyHint: res.urgencyHint
    })
    if (res.userMessageId) {
      const u = messages.value.find((m) => m.messageId === localUserId)
      if (u) u.messageId = res.userMessageId
    }
  } catch (e) {
    messages.value = messages.value.filter((m) => m.messageId !== localUserId)
    console.error(e)
  } finally {
    sending.value = false
  }
}

const handleEndSession = async () => {
  if (!sessionId.value) return
  try {
    await ElMessageBox.confirm('确定结束本次问诊并生成摘要吗？', '结束问诊', {
      type: 'info',
      confirmButtonText: '结束',
      cancelButtonText: '继续问诊'
    })
  } catch {
    return
  }
  ending.value = true
  try {
    const summary = await endAiSession(sessionId.value)
    lastSummary.value = summary
    summaryData.value = summary
    sessionStatus.value = 2
    summaryVisible.value = true
    ElMessage.success('问诊已结束，已生成摘要')
  } finally {
    ending.value = false
  }
}

const openSummary = async () => {
  if (!sessionId.value) return
  try {
    summaryData.value = await getAiSessionSummary(sessionId.value)
    lastSummary.value = summaryData.value
    summaryVisible.value = true
  } catch (e) {
    console.error(e)
  }
}

const goAppointment = (summary) => {
  const deptId = summary?.suggestedDeptId
  summaryVisible.value = false
  router.push({
    path: '/patient/appointment',
    query: deptId ? { deptId: String(deptId) } : {}
  })
}

const loadHistory = async () => {
  historyLoading.value = true
  try {
    const res = await listAiSessions({ pageNum: 1, pageSize: 50 })
    historyList.value = res?.list || []
  } finally {
    historyLoading.value = false
  }
}

const openHistorySession = async (item) => {
  historyVisible.value = false
  await loadSession(item.sessionId)
}

watch(historyVisible, (v) => {
  if (v) loadHistory()
})

onMounted(async () => {
  const qid = route.query.sessionId
  if (qid) {
    await loadSession(Number(qid))
  } else {
    await startNewSession()
  }
})

onBeforeRouteLeave((to, from, next) => {
  if (!isActive.value || messages.value.length <= 2) {
    next()
    return
  }
  ElMessageBox.confirm('当前问诊尚未结束，确定离开吗？', '提示', {
    type: 'warning',
    confirmButtonText: '离开',
    cancelButtonText: '留下'
  })
    .then(() => next())
    .catch(() => next(false))
})
</script>

<style scoped>
.ai-consult-page {
  height: 100%;
  min-height: 0;
  box-sizing: border-box;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  padding: 0;
}

.page-header {
  flex-shrink: 0;
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 14px;
  flex-wrap: wrap;
  gap: 12px;
  padding: 14px 18px;
  background: rgba(255, 255, 255, 0.92);
  border-radius: 14px;
  border: 1px solid rgba(255, 255, 255, 0.8);
  box-shadow: 0 4px 20px rgba(15, 23, 42, 0.08);
  backdrop-filter: blur(8px);
}

.header-left {
  display: flex;
  align-items: center;
  gap: 14px;
  flex-wrap: wrap;
  flex: 1;
  min-width: 0;
}

.header-icon-wrap {
  width: 48px;
  height: 48px;
  border-radius: 14px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #6366f1, #4f46e5);
  color: #fff;
  font-size: 22px;
  box-shadow: 0 4px 12px rgba(79, 70, 229, 0.35);
  flex-shrink: 0;
}

.header-text {
  flex: 1;
  min-width: 0;
  overflow: hidden;
}

.title-row {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: nowrap;
  white-space: nowrap;
}

.page-title {
  margin: 0;
  font-size: 21px;
  font-weight: 700;
  color: #1e293b;
  letter-spacing: 0.02em;
  flex-shrink: 0;
}

.page-disclaimer {
  margin: 0;
  font-size: 13px;
  line-height: 1.45;
  color: #b45309;
  flex: 1;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
}

.page-disclaimer i {
  margin-right: 4px;
  color: #d97706;
}

.header-actions {
  display: flex;
  gap: 10px;
  flex-shrink: 0;
}

.header-actions .el-button--primary {
  background: linear-gradient(135deg, #6366f1, #4f46e5);
  border: none;
}

.content-card {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  background: rgba(255, 255, 255, 0.96);
  border-radius: 16px;
  padding: 16px 18px 14px;
  border: 1px solid rgba(255, 255, 255, 0.9);
  box-shadow: 0 8px 32px rgba(15, 23, 42, 0.1);
  backdrop-filter: blur(6px);
}

.card-fixed-top {
  flex-shrink: 0;
  margin-bottom: 8px;
}

.session-meta {
  display: flex;
  gap: 8px;
}

.chat-panel {
  flex: 1;
  min-height: 0;
  margin: 10px 0;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.card-fixed-bottom {
  flex-shrink: 0;
}

.footer-actions {
  margin-top: 12px;
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
}

.history-item {
  padding: 14px 16px;
  border: 1px solid #e2e8f0;
  border-radius: 12px;
  margin-bottom: 10px;
  cursor: pointer;
  transition: all 0.2s;
  background: #fff;
}

.history-item:hover {
  background: #eef2ff;
  border-color: #a5b4fc;
  transform: translateX(2px);
}

.history-title {
  font-weight: 500;
  color: #334155;
  margin-bottom: 6px;
}

.history-meta {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 12px;
  color: #94a3b8;
}
</style>

<style>
/* 智能问诊页：主内容区不整体滚动，高度交给页面内消息区 */
.main-content:has(.ai-consult-page) {
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

.main-content:has(.ai-consult-page) > .ai-consult-page {
  flex: 1;
  min-height: 0;
  width: 100%;
}
</style>
