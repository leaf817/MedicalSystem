import request from '@/utils/request'

// ==================== 排班查询（预约用） ====================

export function getAvailableDates(userId) {
  return request({
    url: '/patient/schedule/available-dates',
    method: 'get',
    params: { userId }
  })
}

export function getScheduleSlots(userId, date) {
  return request({
    url: '/patient/schedule/slots',
    method: 'get',
    params: { userId, date }
  })
}

// ==================== 我的预约 ====================

export function createAppointment(data) {
  return request({
    url: '/patient/appointment/create',
    method: 'post',
    data
  })
}

export function cancelAppointment(appointmentId) {
  return request({
    url: `/patient/appointment/cancel/${appointmentId}`,
    method: 'put'
  })
}

export function getMyAppointments(params) {
  return request({
    url: '/patient/appointment/my',
    method: 'get',
    params
  })
}

export function getAppointmentDetail(appointmentId) {
  return request({
    url: `/patient/appointment/${appointmentId}`,
    method: 'get'
  })
}

export function payAppointment(appointmentId) {
  return request({
    url: `/patient/appointment/pay/${appointmentId}`,
    method: 'put'
  })
}

// ==================== 我的病历 ====================

export function getMyMedicalRecords() {
  return request({
    url: '/patient/medical-record/my',
    method: 'get'
  })
}

export function getMyMedicalRecordDetail(recordId) {
  return request({
    url: `/patient/medical-record/${recordId}`,
    method: 'get'
  })
}

// ==================== 我的处方 ====================

export function getMyPrescriptions() {
  return request({
    url: '/patient/prescription/my',
    method: 'get'
  })
}

export function getMyPrescriptionDetail(prescriptionId) {
  return request({
    url: `/patient/prescription/${prescriptionId}`,
    method: 'get'
  })
}

export function payMyPrescription(prescriptionId) {
  return request({
    url: `/patient/payment/prescription/${prescriptionId}`,
    method: 'put'
  })
}

// ==================== 患者首页 ====================

export function getPatientDashboardStats() {
  return request({
    url: '/patient/dashboard/stats',
    method: 'get'
  })
}
