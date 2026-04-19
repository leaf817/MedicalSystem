-- ============================================================
-- 为已拥有 PATIENT 角色但尚无 patient 档案的账号补插扩展行（与 P3 领域模型一致）
-- 可重复执行：按 user_id NOT EXISTS 判重
-- ============================================================

SET NAMES utf8mb4;
USE `medical_service`;

INSERT INTO patient (
    user_id, patient_no, name, phone, email, status, created_time, updated_time
)
SELECT
    u.user_id,
    CONCAT('P', u.user_id),
    COALESCE(NULLIF(TRIM(u.name), ''), u.username),
    u.mobile_phone,
    u.email,
    COALESCE(u.status, 1),
    NOW(),
    NOW()
FROM sys_user u
         INNER JOIN sys_user_role ur ON u.user_id = ur.user_id
         INNER JOIN sys_role r ON r.role_id = ur.role_id AND r.role_code = 'PATIENT' AND r.status = 1
WHERE NOT EXISTS (SELECT 1 FROM patient p WHERE p.user_id = u.user_id);
