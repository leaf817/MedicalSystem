import request from '@/utils/request'

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
