import request from '@/utils/request'

/** AI 问诊接口超时（大模型响应较慢） */
const AI_TIMEOUT = 60000

export function createAiSession() {
  return request({
    url: '/patient/ai-consult/session',
    method: 'post',
    timeout: AI_TIMEOUT
  })
}

export function listAiSessions(params) {
  return request({
    url: '/patient/ai-consult/session/list',
    method: 'get',
    params
  })
}

export function getAiSessionDetail(sessionId) {
  return request({
    url: `/patient/ai-consult/session/${sessionId}`,
    method: 'get'
  })
}

export function sendAiMessage(sessionId, content) {
  return request({
    url: `/patient/ai-consult/session/${sessionId}/message`,
    method: 'post',
    data: { content },
    timeout: AI_TIMEOUT
  })
}

export function endAiSession(sessionId) {
  return request({
    url: `/patient/ai-consult/session/${sessionId}/end`,
    method: 'put',
    timeout: AI_TIMEOUT
  })
}

export function getAiSessionSummary(sessionId) {
  return request({
    url: `/patient/ai-consult/session/${sessionId}/summary`,
    method: 'get'
  })
}

export function deleteAiSession(sessionId) {
  return request({
    url: `/patient/ai-consult/session/${sessionId}`,
    method: 'delete'
  })
}

export function suggestAiDept(keyword) {
  return request({
    url: '/patient/ai-consult/dept/suggest',
    method: 'get',
    params: { keyword }
  })
}
