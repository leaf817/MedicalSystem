import request from '@/utils/request'

// ==================== 工作台 ====================

export function getReceptionDashboardSummary() {
  return request({
    url: '/reception/dashboard/summary',
    method: 'get'
  })
}

// ==================== 患者 ====================

export function searchReceptionPatients(keyword) {
  return request({
    url: '/reception/patient/search',
    method: 'get',
    params: { keyword }
  })
}

export function registerReceptionPatient(data) {
  return request({
    url: '/reception/patient/register',
    method: 'post',
    data
  })
}

export function getReceptionPatient(patientId) {
  return request({
    url: `/reception/patient/${patientId}`,
    method: 'get'
  })
}

// ==================== 预约 ====================

export function getReceptionAppointmentPage(params) {
  return request({
    url: '/reception/appointment/page',
    method: 'get',
    params
  })
}

export function createReceptionAppointment(data) {
  return request({
    url: '/reception/appointment/create',
    method: 'post',
    data
  })
}

export function cancelReceptionAppointment(appointmentId) {
  return request({
    url: `/reception/appointment/${appointmentId}/cancel`,
    method: 'put'
  })
}

export function checkinReceptionAppointment(appointmentId) {
  return request({
    url: `/reception/appointment/${appointmentId}/checkin`,
    method: 'put'
  })
}

// ==================== 收费 ====================

export function chargePayment(data) {
  return request({
    url: '/reception/payment/charge',
    method: 'post',
    data
  })
}

export function refundPayment(data) {
  return request({
    url: '/reception/payment/refund',
    method: 'post',
    data
  })
}

export function getPaymentPage(params) {
  return request({
    url: '/reception/payment/page',
    method: 'get',
    params
  })
}

export function getPaymentDetail(paymentId) {
  return request({
    url: `/reception/payment/${paymentId}`,
    method: 'get'
  })
}

export function getUnpaidPrescriptions(keyword) {
  return request({
    url: '/reception/payment/unpaid-prescriptions',
    method: 'get',
    params: { keyword }
  })
}
