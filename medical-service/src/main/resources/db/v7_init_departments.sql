-- ============================================================
-- 智能医疗服务管理系统 - 科室初始化（与角色代码 role_code 对齐，供按角色同步科室）
-- 版本：v8
-- 说明：sys_dept.code 与 sys_role.role_code 一致时，POST /api/admin/dept/sync-staff 可将医护归属对应科室。
-- 可重复执行：按 code 判重插入。
-- ============================================================

SET NAMES utf8mb4;
USE `medical_service`;

INSERT INTO sys_dept (parent_id, name, code, sort_order, status, created_time, updated_time)
SELECT 0, '通科门诊', 'DOCTOR', 5, 1, NOW(), NOW() FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_dept WHERE code = 'DOCTOR' LIMIT 1);

INSERT INTO sys_dept (parent_id, name, code, sort_order, status, created_time, updated_time)
SELECT 0, '急诊科', 'ER_DOCTOR', 10, 1, NOW(), NOW() FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_dept WHERE code = 'ER_DOCTOR' LIMIT 1);

INSERT INTO sys_dept (parent_id, name, code, sort_order, status, created_time, updated_time)
SELECT 0, '儿科', 'PEDIATRICIAN', 20, 1, NOW(), NOW() FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_dept WHERE code = 'PEDIATRICIAN' LIMIT 1);

INSERT INTO sys_dept (parent_id, name, code, sort_order, status, created_time, updated_time)
SELECT 0, '内科', 'INTERNIST', 30, 1, NOW(), NOW() FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_dept WHERE code = 'INTERNIST' LIMIT 1);

INSERT INTO sys_dept (parent_id, name, code, sort_order, status, created_time, updated_time)
SELECT 0, '外科', 'SURGEON', 40, 1, NOW(), NOW() FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_dept WHERE code = 'SURGEON' LIMIT 1);

INSERT INTO sys_dept (parent_id, name, code, sort_order, status, created_time, updated_time)
SELECT 0, '妇产科', 'GYNECOLOGIST', 50, 1, NOW(), NOW() FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_dept WHERE code = 'GYNECOLOGIST' LIMIT 1);

INSERT INTO sys_dept (parent_id, name, code, sort_order, status, created_time, updated_time)
SELECT 0, '骨科', 'ORTHOPEDIST', 60, 1, NOW(), NOW() FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_dept WHERE code = 'ORTHOPEDIST' LIMIT 1);

INSERT INTO sys_dept (parent_id, name, code, sort_order, status, created_time, updated_time)
SELECT 0, '皮肤科', 'DERMATOLOGIST', 70, 1, NOW(), NOW() FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_dept WHERE code = 'DERMATOLOGIST' LIMIT 1);

INSERT INTO sys_dept (parent_id, name, code, sort_order, status, created_time, updated_time)
SELECT 0, '眼科', 'OPHTHALMOLOGIST', 80, 1, NOW(), NOW() FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_dept WHERE code = 'OPHTHALMOLOGIST' LIMIT 1);

INSERT INTO sys_dept (parent_id, name, code, sort_order, status, created_time, updated_time)
SELECT 0, '耳鼻喉科', 'ENT_DOCTOR', 90, 1, NOW(), NOW() FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_dept WHERE code = 'ENT_DOCTOR' LIMIT 1);

INSERT INTO sys_dept (parent_id, name, code, sort_order, status, created_time, updated_time)
SELECT 0, '心内科', 'CARDIOLOGIST', 100, 1, NOW(), NOW() FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_dept WHERE code = 'CARDIOLOGIST' LIMIT 1);

INSERT INTO sys_dept (parent_id, name, code, sort_order, status, created_time, updated_time)
SELECT 0, '神经科', 'NEUROLOGIST', 110, 1, NOW(), NOW() FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_dept WHERE code = 'NEUROLOGIST' LIMIT 1);

INSERT INTO sys_dept (parent_id, name, code, sort_order, status, created_time, updated_time)
SELECT 0, '肿瘤科', 'ONCOLOGIST', 120, 1, NOW(), NOW() FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_dept WHERE code = 'ONCOLOGIST' LIMIT 1);

INSERT INTO sys_dept (parent_id, name, code, sort_order, status, created_time, updated_time)
SELECT 0, '精神科', 'PSYCHIATRIST', 130, 1, NOW(), NOW() FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_dept WHERE code = 'PSYCHIATRIST' LIMIT 1);

INSERT INTO sys_dept (parent_id, name, code, sort_order, status, created_time, updated_time)
SELECT 0, '中医科', 'TCM_DOCTOR', 140, 1, NOW(), NOW() FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_dept WHERE code = 'TCM_DOCTOR' LIMIT 1);

INSERT INTO sys_dept (parent_id, name, code, sort_order, status, created_time, updated_time)
SELECT 0, '康复科', 'REHAB_DOCTOR', 150, 1, NOW(), NOW() FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_dept WHERE code = 'REHAB_DOCTOR' LIMIT 1);

INSERT INTO sys_dept (parent_id, name, code, sort_order, status, created_time, updated_time)
SELECT 0, '营养科', 'NUTRITIONIST', 160, 1, NOW(), NOW() FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_dept WHERE code = 'NUTRITIONIST' LIMIT 1);

INSERT INTO sys_dept (parent_id, name, code, sort_order, status, created_time, updated_time)
SELECT 0, '麻醉科', 'ANESTHESIOLOGIST', 170, 1, NOW(), NOW() FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_dept WHERE code = 'ANESTHESIOLOGIST' LIMIT 1);

INSERT INTO sys_dept (parent_id, name, code, sort_order, status, created_time, updated_time)
SELECT 0, '病理科', 'PATHOLOGIST', 180, 1, NOW(), NOW() FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_dept WHERE code = 'PATHOLOGIST' LIMIT 1);

-- 护理单元（与护士类角色代码对齐）
INSERT INTO sys_dept (parent_id, name, code, sort_order, status, created_time, updated_time)
SELECT 0, '护理部', 'NURSE', 200, 1, NOW(), NOW() FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_dept WHERE code = 'NURSE' LIMIT 1);

INSERT INTO sys_dept (parent_id, name, code, sort_order, status, created_time, updated_time)
SELECT 0, '护理管理', 'NURSE_HEAD', 205, 1, NOW(), NOW() FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_dept WHERE code = 'NURSE_HEAD' LIMIT 1);

INSERT INTO sys_dept (parent_id, name, code, sort_order, status, created_time, updated_time)
SELECT 0, 'ICU 护理单元', 'ICU_NURSE', 210, 1, NOW(), NOW() FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_dept WHERE code = 'ICU_NURSE' LIMIT 1);

INSERT INTO sys_dept (parent_id, name, code, sort_order, status, created_time, updated_time)
SELECT 0, '手术室护理单元', 'OR_NURSE', 220, 1, NOW(), NOW() FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_dept WHERE code = 'OR_NURSE' LIMIT 1);

INSERT INTO sys_dept (parent_id, name, code, sort_order, status, created_time, updated_time)
SELECT 0, '急诊护理单元', 'EMERGENCY_NURSE', 230, 1, NOW(), NOW() FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_dept WHERE code = 'EMERGENCY_NURSE' LIMIT 1);
