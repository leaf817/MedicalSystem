-- ============================================================
-- 智能医疗服务管理系统 - 为「专科医师」用户补授「医生」基础角色
-- 版本：v7
-- 说明：
--   医生列表（/admin/doctor）按 roleCode=DOCTOR 筛选；仅持有 ER_DOCTOR、NEUROLOGIST
--   等细分角色而未持有 DOCTOR 时不会出现在列表中。
--   本脚本为已拥有下列任一专科/医师类角色的用户，在尚未拥有 DOCTOR 时补插 sys_user_role。
-- 依赖：v3 角色表、既有用户与 sys_user_role 数据
-- 数据库：MySQL 8.x utf8mb4
-- ============================================================

SET NAMES utf8mb4;
USE `medical_service`;

INSERT IGNORE INTO sys_user_role (user_id, role_id, created_time)
SELECT DISTINCT ur.user_id, dr.role_id, NOW()
FROM sys_user_role ur
INNER JOIN sys_role sr ON ur.role_id = sr.role_id AND sr.status = 1
INNER JOIN sys_role dr ON dr.role_code = 'DOCTOR' AND dr.status = 1
WHERE sr.role_code IN (
  'ER_DOCTOR',
  'PEDIATRICIAN',
  'INTERNIST',
  'SURGEON',
  'GYNECOLOGIST',
  'ORTHOPEDIST',
  'DERMATOLOGIST',
  'OPHTHALMOLOGIST',
  'ENT_DOCTOR',
  'CARDIOLOGIST',
  'NEUROLOGIST',
  'ONCOLOGIST',
  'PSYCHIATRIST',
  'TCM_DOCTOR',
  'REHAB_DOCTOR',
  'NUTRITIONIST',
  'ANESTHESIOLOGIST',
  'PATHOLOGIST'
)
AND NOT EXISTS (
  SELECT 1
  FROM sys_user_role ur2
  INNER JOIN sys_role r2 ON ur2.role_id = r2.role_id AND r2.role_code = 'DOCTOR'
  WHERE ur2.user_id = ur.user_id
);
