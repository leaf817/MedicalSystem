-- ============================================================
-- 智能医疗服务管理系统 - 医生档案数据初始化脚本
-- 版本：v12
-- ============================================================

SET NAMES utf8mb4;
USE `medical_service`;

DELETE FROM `doctor`;

ALTER TABLE `doctor` AUTO_INCREMENT = 1;

INSERT INTO `doctor` (
    user_id,
    doctor_no,
    name,
    dept_id,
    title,
    phone,
    consultation_fee,
    status,
    created_time,
    updated_time
)
SELECT DISTINCT
    u.user_id,
    CONCAT('DOC', LPAD(u.user_id, 6, '0')) AS doctor_no,
    u.name,
    u.dept_id,
    -- 优先取专科角色名称，如果是 DOCTOR 则显示"通科医师"
    CASE
        WHEN MAX(CASE WHEN r.role_code != 'DOCTOR' THEN r.role_name END) IS NOT NULL
            THEN MAX(CASE WHEN r.role_code != 'DOCTOR' THEN r.role_name END)
        ELSE '通科医师'
        END AS title,
    u.mobile_phone,
    50.00,
    u.status,
    NOW(),
    NOW()
FROM sys_user u
         INNER JOIN sys_user_role ur ON u.user_id = ur.user_id
         INNER JOIN sys_role r ON ur.role_id = r.role_id
WHERE r.role_code IN ('ER_DOCTOR', 'PEDIATRICIAN', 'INTERNIST', 'SURGEON',
                      'GYNECOLOGIST', 'ORTHOPEDIST', 'DERMATOLOGIST',
                      'OPHTHALMOLOGIST', 'ENT_DOCTOR', 'CARDIOLOGIST',
                      'NEUROLOGIST', 'ONCOLOGIST', 'PSYCHIATRIST',
                      'TCM_DOCTOR', 'REHAB_DOCTOR', 'NUTRITIONIST',
                      'ANESTHESIOLOGIST', 'PATHOLOGIST', 'DOCTOR')
GROUP BY u.user_id, u.name, u.dept_id, u.mobile_phone, u.status