import request from '@/utils/request'

// ==================== 患者检索 ====================

export const getAllPatients = () => {
  return request({
    url: '/doctor/patient/all',
    method: 'get'
  })
}

export const searchPatients = (keyword) => {
  return request({
    url: '/doctor/patient/search',
    method: 'get',
    params: { keyword }
  })
}

// ==================== 病历 ====================

export const getAllMedicalRecords = () => {
  return request({
    url: '/doctor/medical-record/all',
    method: 'get'
  })
}

export const getPatientMedicalRecords = (patientId) => {
  return request({
    url: `/doctor/medical-record/patient/${patientId}`,
    method: 'get'
  })
}

export const getMedicalRecordDetail = (recordId) => {
  return request({
    url: `/doctor/medical-record/${recordId}`,
    method: 'get'
  })
}

/** 与 getMedicalRecordDetail 相同，便于处方等页面语义化引入 */
export const getMedicalRecordById = getMedicalRecordDetail

export const saveMedicalRecord = (data) => {
  return request({
    url: '/doctor/medical-record/save',
    method: 'post',
    data
  })
}

export const deleteMedicalRecord = (recordId) => {
  return request({
    url: `/doctor/medical-record/${recordId}`,
    method: 'delete'
  })
}

// ==================== 待诊队列 ====================

export function getQueueList(params) {
  return request({
    url: '/doctor/queue/list',
    method: 'get',
    params
  })
}

export function startConsultation(appointmentId) {
  return request({
    url: `/doctor/queue/${appointmentId}/start`,
    method: 'put'
  })
}

export function callNext(appointmentId) {
  return request({
    url: `/doctor/queue/${appointmentId}/call`,
    method: 'put'
  })
}

export function getCurrentCalling(queryDate) {
  return request({
    url: '/doctor/queue/current-calling',
    method: 'get',
    params: { queryDate }
  })
}

export function getTodayStats(queryDate) {
  return request({
    url: '/doctor/queue/stats',
    method: 'get',
    params: { queryDate }
  })
}

export function getAvailableQueueDates() {
  return request({
    url: '/doctor/queue/available-dates',
    method: 'get'
  })
}

export function doctorCancelAppointment(appointmentId) {
  return request({
    url: `/doctor/queue/cancel/${appointmentId}`,
    method: 'put'
  })
}

// ==================== 处方 ====================

export function createPrescription(data) {
  return request({
    url: '/doctor/prescription',
    method: 'post',
    data
  })
}

export function updatePrescription(id, data) {
  return request({
    url: `/doctor/prescription/${id}`,
    method: 'put',
    data
  })
}

export function deletePrescription(id) {
  return request({
    url: `/doctor/prescription/${id}`,
    method: 'delete'
  })
}

export function getPrescriptionDetail(id) {
  return request({
    url: `/doctor/prescription/${id}`,
    method: 'get'
  })
}

export function getPrescriptionsByRecord(recordId) {
  return request({
    url: `/doctor/prescription/record/${recordId}`,
    method: 'get'
  })
}

export function getPrescriptionsByPatient(patientId) {
  return request({
    url: `/doctor/prescription/patient/${patientId}`,
    method: 'get'
  })
}

export function getMyPrescriptions(params) {
  return request({
    url: '/doctor/prescription/my',
    method: 'get',
    params
  })
}

// ==================== 工作台 ====================

export const getDoctorDashboardStats = () => {
  return request({
    url: '/doctor/dashboard/stats',
    method: 'get'
  })
}
