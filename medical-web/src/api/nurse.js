import request from '@/utils/request'

// ==================== 工作台 ====================

export function getNurseDashboardStats() {
  return request({
    url: '/nurse/dashboard/stats',
    method: 'get'
  })
}

// ==================== 发药 ====================

export function getPendingPrescriptions(keyword) {
  return request({
    url: '/nurse/prescription/pending',
    method: 'get',
    params: { keyword }
  })
}

export function getDispensedPrescriptions(keyword) {
  return request({
    url: '/nurse/prescription/dispensed',
    method: 'get',
    params: { keyword }
  })
}

export function dispensePrescription(id) {
  return request({
    url: `/nurse/prescription/${id}/dispense`,
    method: 'put'
  })
}

export function getNursePrescriptionDetail(id) {
  return request({
    url: `/nurse/prescription/${id}`,
    method: 'get'
  })
}
