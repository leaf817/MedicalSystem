import { createRouter, createWebHistory } from 'vue-router'
import Placeholder from '@/views/Placeholder.vue'
import { ElMessage } from 'element-plus'
import { resolveDefaultHomePath } from '@/config/menu-config'
import { canAccessRoute, getRolesFromStorage } from '@/utils/route-permissions'

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/login/Login.vue'),
    meta: { title: '登录', guest: true }
  },
  {
    path: '/',
    component: () => import('@/layout/AdminLayout.vue'),
    redirect: () => ({ path: resolveDefaultHomePath() }),
    children: [
      {
        path: 'admin/dashboard',
        name: 'Dashboard',
        component: () => import('@/views/admin/Dashboard.vue'),
        meta: { title: '工作台' }
      },
      {
        path: 'admin/dept',
        component: () => import('@/views/admin/DeptList.vue'),
        meta: { title: '科室列表' }
      },
      {
        path: 'admin/dept/registration-board',
        component: () => import('@/views/admin/DeptRegistrationBoard.vue'),
        meta: { title: '科室挂号看板' }
      },
      {
        path: 'admin/doctor',
        component: () => import('@/views/admin/StaffList.vue'),
        meta: { title: '医生列表', staffRoleCode: 'DOCTOR' }
      },
      {
        path: 'admin/nurse',
        component: () => import('@/views/admin/StaffList.vue'),
        meta: { title: '护士列表', staffRoleCode: 'NURSE' }
      },
      {
        path: 'admin/patient',
        component: () => import('@/views/admin/StaffList.vue'),
        meta: { title: '患者列表', staffRoleCode: 'PATIENT' }
      },
      { path: 'admin/schedule', component: () => import('@/views/admin/ScheduleManagement.vue'), meta: { title: '排班管理' } },
      { path: 'admin/appointment', component: () => import('@/views/admin/AppointmentManagement.vue'), meta: { title: '预约管理' } },
      { path: 'admin/payment', component: () => import('@/views/admin/PaymentManagement.vue'), meta: { title: '收费查询' } },
      { path: 'admin/medicine', component: () => import('@/views/admin/MedicineList.vue'), meta: { title: '药品列表' } },
      {
        path: 'admin/medicine-stock-warning',
        name: 'MedicineStockWarning',
        component: () => import('@/views/admin/MedicineStockWarning.vue'),
        meta: { title: '库存预警', stockWarningOnly: true }
      },
      {
        path: 'admin/system/user',
        component: () => import('@/views/admin/UserManagement.vue'),
        meta: { title: '用户管理', requiresSuperAdmin: true }
      },
      {
        path: 'admin/system/role',
        component: () => import('@/views/admin/RoleManagement.vue'),
        meta: { title: '角色管理', requiresSuperAdmin: true }
      },
      {
        path: 'user/password',
        component: () => import('@/views/user/ChangePassword.vue'),
        meta: { title: '修改密码' }
      },
      { path: 'doctor/dashboard', component: () => import('@/views/doctor/DoctorDashboard.vue'), meta: { title: '医生工作台' } },
      {
        path: 'doctor/schedule',
        component: () => import('@/views/doctor/MySchedule.vue'),
        meta: { title: '我的排班' }
      },
      { path: 'doctor/queue', component: () => import('@/views/doctor/QueueList.vue'), meta: { title: '待诊队列' } },
      { path: 'doctor/medical-record', component: () => import('@/views/doctor/MedicalRecordList.vue'), meta: { title: '病历书写' } },
      { path: 'doctor/prescription', component: () => import('@/views/doctor/PrescriptionCreate.vue'), meta: { title: '处方开立' } },
      { path: 'patient/dashboard', component: () => import('@/views/patient/PatientDashboard.vue'), meta: { title: '患者首页' } },
      { path: 'patient/appointment', component: () => import('@/views/admin/AppointmentBooking.vue'), meta: { title: '我要预约' } },
      { path: 'patient/my-appointment', component: () => import('@/views/patient/MyAppointmentList.vue'), meta: { title: '我的预约' } },
      {
        path: 'patient/medical-record',
        component: () => import('@/views/patient/PatientMedicalRecordList.vue'),
        meta: { title: '我的病历' }
      },
      {
        path: 'patient/prescription',
        component: () => import('@/views/patient/PatientPrescriptionList.vue'),
        meta: { title: '我的处方' }
      },
      {
        path: 'patient/ai-consult',
        component: () => import('@/views/patient/AiConsultChat.vue'),
        meta: { title: '智能问诊' }
      },
      {
        path: 'reception/dashboard',
        component: () => import('@/views/reception/ReceptionDashboard.vue'),
        meta: { title: '挂号工作台' }
      },
      {
        path: 'reception/appointment',
        component: () => import('@/views/reception/ReceptionAppointment.vue'),
        meta: { title: '预约挂号' }
      },
      {
        path: 'reception/patient-register',
        component: () => import('@/views/reception/PatientRegister.vue'),
        meta: { title: '患者建档' }
      },
      {
        path: 'reception/payment',
        component: () => import('@/views/reception/Payment.vue'),
        meta: { title: '收费' }
      },
      {
        path: 'reception/refund',
        component: () => import('@/views/reception/Refund.vue'),
        meta: { title: '退费' }
      },
      { path: 'nurse/dashboard', component: () => import('@/views/nurse/NurseDashboard.vue'), meta: { title: '护士工作台' } },
      { path: 'nurse/prescription', component: () => import('@/views/nurse/NursePrescriptionPending.vue'), meta: { title: '待发药' } },
      { path: 'nurse/dispense', component: () => import('@/views/nurse/NurseDispenseList.vue'), meta: { title: '发药确认' } },
      { path: 'nurse/inventory', component: () => import('@/views/nurse/NurseInventory.vue'), meta: { title: '药品盘点' } }
    ]
  }
]

const router = createRouter({
    history: createWebHistory(),
    routes
})

router.beforeEach((to, from, next) => {
    document.title = (to.meta.title ? to.meta.title + ' - ' : '') + '智能医疗服务管理系统'
    if (to.meta.guest) {
        next()
        return
    }
    const userInfo = sessionStorage.getItem('userInfo')
    if (!userInfo && to.path !== '/login') {
        next({ path: '/login', query: { redirect: to.fullPath } })
        return
    }

    const roles = getRolesFromStorage()
    if (userInfo && !canAccessRoute(to, roles)) {
        ElMessage.warning('无权访问该页面')
        next({ path: resolveDefaultHomePath(), replace: true })
        return
    }

    next()
})

export default router