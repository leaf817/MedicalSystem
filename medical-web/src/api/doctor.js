// medical-web/src/api/doctor.js
import request from '@/utils/request'

// 获取所有患者
export const getAllPatients = () => {
    return request({
        url: '/doctor/patient/all',
        method: 'get'
    })
}

// 搜索患者
export const searchPatients = (keyword) => {
    return request({
        url: '/doctor/patient/search',
        method: 'get',
        params: { keyword }
    })
}

// 获取患者历史病历
export const getPatientMedicalRecords = (patientId) => {
    return request({
        url: `/doctor/medical-record/patient/${patientId}`,
        method: 'get'
    })
}

// 获取所有病历（当前医生）
export const getAllMedicalRecords = () => {
    return request({
        url: '/doctor/medical-record/all',
        method: 'get'
    })
}

// 获取病历详情
export const getMedicalRecordDetail = (recordId) => {
    return request({
        url: `/doctor/medical-record/${recordId}`,
        method: 'get'
    })
}

// 保存病历（新增/编辑）
export const saveMedicalRecord = (data) => {
    return request({
        url: '/doctor/medical-record/save',
        method: 'post',
        data
    })
}

// 删除病历
export const deleteMedicalRecord = (recordId) => {
    return request({
        url: `/doctor/medical-record/${recordId}`,
        method: 'delete'
    })
}