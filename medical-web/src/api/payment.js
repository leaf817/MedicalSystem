import request from '@/utils/request'
import axios from 'axios'

/**
 * 管理端 — 收费流水（统一 PaymentService）
 */
export function getAdminPaymentPage(params) {
  return request({
    url: '/admin/payment/page',
    method: 'get',
    params
  })
}

export function getAdminPaymentDetail(paymentId) {
  return request({
    url: `/admin/payment/${paymentId}`,
    method: 'get'
  })
}

export function exportAdminPaymentCsv(params) {
  return axios({
    baseURL: '/api',
    url: '/admin/payment/export',
    method: 'get',
    params,
    responseType: 'blob',
    withCredentials: true,
    timeout: 60000
  })
}
