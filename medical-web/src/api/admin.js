import request from '@/utils/request'

// ==================== 用户管理 ====================

export function getUserPage(params) {
  return request({
    url: '/admin/user/page',
    method: 'get',
    params
  })
}

export function getDoctorPage(params) {
  return request({
    url: '/admin/doctor/page',
    method: 'get',
    params
  })
}

export function createUser(data) {
  return request({
    url: '/admin/user',
    method: 'post',
    data
  })
}

export function updateUser(id, data) {
  return request({
    url: `/admin/user/${id}`,
    method: 'put',
    data
  })
}

export function updateUserStatus(id, status) {
  return request({
    url: `/admin/user/${id}/status`,
    method: 'put',
    params: { status }
  })
}

export function deleteUser(id) {
  return request({
    url: `/admin/user/${id}`,
    method: 'delete'
  })
}

// ==================== 角色管理 ====================

export function createRole(data) {
  return request({
    url: '/admin/role',
    method: 'post',
    data
  })
}

export function deleteRole(id) {
  return request({
    url: `/admin/role/${id}`,
    method: 'delete'
  })
}

export function updateRole(id, data) {
  return request({
    url: `/admin/role/${id}`,
    method: 'put',
    data
  })
}

export function updateRoleStatus(id, status) {
  return request({
    url: `/admin/role/${id}/status`,
    method: 'put',
    params: { status }
  })
}

export function getRolePage(params) {
  return request({
    url: '/admin/role/page',
    method: 'get',
    params
  })
}

export function getRoleList(params) {
  return request({
    url: '/admin/role/list',
    method: 'get',
    params
  })
}

// ==================== 药品管理 ====================

export function getMedicinePage(params) {
  return request({
    url: '/admin/medicine/page',
    method: 'get',
    params
  })
}

export function getMedicineStockWarning(params) {
  return request({
    url: '/admin/medicine/stock-warning',
    method: 'get',
    params
  })
}

export function getMedicineCategories() {
  return request({
    url: '/admin/medicine/categories',
    method: 'get'
  })
}

export function createMedicine(data) {
  return request({
    url: '/admin/medicine',
    method: 'post',
    data
  })
}

export function updateMedicine(id, data) {
  return request({
    url: `/admin/medicine/${id}`,
    method: 'put',
    data
  })
}

export function getMedicineDetail(id) {
  return request({
    url: `/admin/medicine/${id}`,
    method: 'get'
  })
}

// ==================== 科室管理 ====================

export function getDeptPage(params) {
  return request({
    url: '/admin/dept/page',
    method: 'get',
    params
  })
}

export function getDeptTree() {
  return request({
    url: '/admin/dept/tree',
    method: 'get'
  })
}

export function getDeptOptions() {
  return request({
    url: '/admin/dept/options',
    method: 'get'
  })
}

export function getDeptDetail(id) {
  return request({
    url: `/admin/dept/${id}`,
    method: 'get'
  })
}

export function createDept(data) {
  return request({
    url: '/admin/dept',
    method: 'post',
    data
  })
}

export function updateDept(id, data) {
  return request({
    url: `/admin/dept/${id}`,
    method: 'put',
    data
  })
}

export function deleteDept(id) {
  return request({
    url: `/admin/dept/${id}`,
    method: 'delete'
  })
}

// ==================== 排班管理 ====================

export function getScheduleList(params) {
  return request({
    url: '/admin/schedule/list',
    method: 'get',
    params
  })
}

export function createSchedule(data) {
  return request({
    url: '/admin/schedule',
    method: 'post',
    data
  })
}

export function createScheduleBatch(data) {
  return request({
    url: '/admin/schedule/batch',
    method: 'post',
    data
  })
}

export function updateSchedule(id, data) {
  return request({
    url: `/admin/schedule/${id}`,
    method: 'put',
    data
  })
}

export function deleteSchedule(id) {
  return request({
    url: `/admin/schedule/${id}`,
    method: 'delete'
  })
}

export function disableExpiredSchedules() {
  return request({
    url: '/admin/schedule/disable-expired',
    method: 'put'
  })
}

// ==================== 预约管理 ====================

export function getAdminAppointmentPage(params) {
  return request({
    url: '/admin/appointment/page',
    method: 'get',
    params
  })
}

export function getAdminAppointmentDetail(id) {
  return request({
    url: `/admin/appointment/${id}`,
    method: 'get'
  })
}

export function cancelAdminAppointment(id) {
  return request({
    url: `/admin/appointment/${id}/cancel`,
    method: 'put'
  })
}

export function checkInAppointment(appointmentId) {
  return request({
    url: `/admin/appointment/${appointmentId}/checkin`,
    method: 'put'
  })
}

export function expireOverdueAppointments() {
  return request({
    url: '/admin/appointment/expire-overdue',
    method: 'put'
  })
}

// ==================== 工作台 ====================

export function getAdminDashboardStats() {
  return request({
    url: '/admin/dashboard/stats',
    method: 'get'
  })
}
