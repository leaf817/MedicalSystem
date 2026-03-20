/**
 * 菜单配置 - 参考 book-library，按角色区分
 */

// 超级管理员/医院管理员菜单
export const adminMenuItems = [
  {
    index: '0',
    title: '主页',
    icon: 'fa-solid fa-house',
    url: '/admin/dashboard'
  },
  {
    index: '1',
    title: '科室管理',
    icon: 'fa-solid fa-building',
    children: [
      { index: '1-1', label: '科室列表', icon: 'fa-solid fa-list', url: '/admin/dept' }
    ]
  },
  {
    index: '2',
    title: '医生管理',
    icon: 'fa-solid fa-user-doctor',
    children: [
      { index: '2-1', label: '医生列表', icon: 'fa-solid fa-user', url: '/admin/doctor' }
    ]
  },
  {
    index: '3',
    title: '患者管理',
    icon: 'fa-solid fa-users',
    children: [
      { index: '3-1', label: '患者列表', icon: 'fa-solid fa-user', url: '/admin/patient' }
    ]
  },
  {
    index: '4',
    title: '排班预约',
    icon: 'fa-solid fa-calendar-days',
    children: [
      { index: '4-1', label: '排班管理', icon: 'fa-solid fa-calendar', url: '/admin/schedule' },
      { index: '4-2', label: '预约管理', icon: 'fa-solid fa-clock', url: '/admin/appointment' }
    ]
  },
  {
    index: '5',
    title: '药品管理',
    icon: 'fa-solid fa-pills',
    children: [
      { index: '5-1', label: '药品列表', icon: 'fa-solid fa-list', url: '/admin/medicine' }
    ]
  },
  {
    index: '6',
    title: '系统管理',
    icon: 'fa-solid fa-cog',
    children: [
      { index: '6-1', label: '用户管理', icon: 'fa-solid fa-users', url: '/admin/system/user' },
      { index: '6-2', label: '角色管理', icon: 'fa-solid fa-list', url: '/admin/system/role' }
    ]
  },
  {
    index: '7',
    title: '用户中心',
    icon: 'fa-solid fa-user-circle',
    children: [
      { index: '7-1', label: '修改密码', icon: 'fa-solid fa-key', url: '/user/password' }
    ]
  }
]

// 医生菜单
export const doctorMenuItems = [
  { index: '0', title: '工作台', icon: 'fa-solid fa-house', url: '/doctor/dashboard' },
  {
    index: '1',
    title: '门诊接诊',
    icon: 'fa-solid fa-stethoscope',
    children: [
      { index: '1-1', label: '我的排班', icon: 'fa-solid fa-calendar', url: '/doctor/schedule' },
      { index: '1-2', label: '待诊队列', icon: 'fa-solid fa-list', url: '/doctor/queue' }
    ]
  },
  {
    index: '2',
    title: '病历处方',
    icon: 'fa-solid fa-file-medical',
    children: [
      { index: '2-1', label: '病历书写', icon: 'fa-solid fa-pen', url: '/doctor/medical-record' },
      { index: '2-2', label: '处方开立', icon: 'fa-solid fa-prescription', url: '/doctor/prescription' }
    ]
  }
]

// 患者菜单
export const patientMenuItems = [
  { index: '0', title: '首页', icon: 'fa-solid fa-house', url: '/patient/dashboard' },
  {
    index: '1',
    title: '预约挂号',
    icon: 'fa-solid fa-calendar-check',
    children: [
      { index: '1-1', label: '我要预约', icon: 'fa-solid fa-calendar-plus', url: '/patient/appointment' },
      { index: '1-2', label: '我的预约', icon: 'fa-solid fa-clock', url: '/patient/my-appointment' }
    ]
  },
  {
    index: '2',
    title: '个人中心',
    icon: 'fa-solid fa-user-circle',
    children: [
      { index: '2-1', label: '我的病历', icon: 'fa-solid fa-file-medical', url: '/patient/medical-record' },
      { index: '2-2', label: '我的处方', icon: 'fa-solid fa-prescription', url: '/patient/prescription' }
    ]
  }
]

export function getMenuByRole(roles) {
  if (!roles || !roles.length) return adminMenuItems
  const role = roles[0]
  if (role === 'SUPER_ADMIN' || role === 'ADMIN') return adminMenuItems
  if (role === 'DOCTOR') return doctorMenuItems
  if (role === 'PATIENT') return patientMenuItems
  return adminMenuItems
}
