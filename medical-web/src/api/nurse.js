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

// ==================== 库存盘点 ====================

export function getInventoryMedicinePage(params) {
  return request({
    url: '/nurse/inventory/medicine/page',
    method: 'get',
    params
  })
}

export function getInventoryMedicineCategories() {
  return request({
    url: '/nurse/inventory/medicine/categories',
    method: 'get'
  })
}

export function adjustInventoryStock(id, data) {
  return request({
    url: `/nurse/inventory/medicine/${id}/adjust`,
    method: 'put',
    data
  })
}

export function inboundInventoryStock(id, data) {
  return request({
    url: `/nurse/inventory/medicine/${id}/inbound`,
    method: 'post',
    data
  })
}

export function getInventoryStockLogPage(params) {
  return request({
    url: '/nurse/inventory/stock-log/page',
    method: 'get',
    params
  })
}
