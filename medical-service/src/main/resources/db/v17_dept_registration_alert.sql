-- ============================================================
-- 科室挂号预警规则表及示例规则（合并 v17/v18/v19）
-- 说明：建表 + 示例规则，可重复执行
-- threshold_type: 1=今日挂号 2=较7日均值增幅(%) 3=明日预约 4=过去一周内预约合计(近7日含今日)
-- alert_level: 1=提示 2=警告 3=严重
-- ============================================================

SET NAMES utf8mb4;
USE `medical_service`;

CREATE TABLE IF NOT EXISTS `dept_registration_alert_rule` (
    `rule_id`       BIGINT       NOT NULL AUTO_INCREMENT COMMENT '规则ID',
    `dept_id`       BIGINT       NOT NULL COMMENT '科室ID',
    `rule_name`     VARCHAR(100)          DEFAULT NULL COMMENT '规则名称',
    `threshold_type` TINYINT     NOT NULL DEFAULT 1 COMMENT '1=今日挂号 2=较7日均值增幅 3=明日预约 4=过去一周内预约合计',
    `threshold_value` DECIMAL(10,2) NOT NULL COMMENT '阈值',
    `alert_level`   TINYINT      NOT NULL DEFAULT 2 COMMENT '1=提示 2=警告 3=严重',
    `enabled`       TINYINT      NOT NULL DEFAULT 1 COMMENT '1=启用 0=停用',
    `remark`        VARCHAR(500)          DEFAULT NULL COMMENT '备注',
    `created_time`  DATETIME              DEFAULT CURRENT_TIMESTAMP,
    `updated_time`  DATETIME              DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`rule_id`),
    KEY `idx_dept_enabled` (`dept_id`, `enabled`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='科室挂号预警规则';

-- 兼容旧版规则名/类型（已执行过旧脚本时自动迁移，无匹配行则跳过）
UPDATE dept_registration_alert_rule
SET threshold_type = 4,
    rule_name = '急诊科过去一周内预约预警',
    threshold_value = 80.00,
    remark = '急诊科过去一周内预约合计超过80例触发严重预警',
    updated_time = NOW()
WHERE rule_name IN ('急诊科未来2日预约合计', '急诊科一周内预约预警');

UPDATE dept_registration_alert_rule
SET threshold_type = 4,
    rule_name = '皮肤科过去一周内预约预警',
    threshold_value = 50.00,
    remark = '皮肤科过去一周内预约合计超过50例触发提示',
    updated_time = NOW()
WHERE rule_name IN ('皮肤科后日预约预警', '皮肤科一周内预约预警');

UPDATE dept_registration_alert_rule
SET threshold_type = 4,
    updated_time = NOW()
WHERE threshold_type = 5;

UPDATE dept_registration_alert_rule
SET threshold_value = 15.00,
    remark = '皮肤科明日预约超过15例触发警告',
    updated_time = NOW()
WHERE rule_name = '皮肤科明日预约预警';

-- 示例规则（按 rule_name 判重插入）
INSERT INTO dept_registration_alert_rule (dept_id, rule_name, threshold_type, threshold_value, alert_level, enabled, remark, created_time, updated_time)
SELECT d.dept_id, '儿科日挂号量预警', 1, 30.00, 2, 1, '儿科日挂号超过30例触发警告', NOW(), NOW()
FROM sys_dept d
WHERE d.code = 'PEDIATRICIAN'
  AND NOT EXISTS (SELECT 1 FROM dept_registration_alert_rule r WHERE r.dept_id = d.dept_id AND r.rule_name = '儿科日挂号量预警')
LIMIT 1;

INSERT INTO dept_registration_alert_rule (dept_id, rule_name, threshold_type, threshold_value, alert_level, enabled, remark, created_time, updated_time)
SELECT d.dept_id, '儿科就诊量异常增幅', 2, 50.00, 3, 1, '较近7日均值增幅超50%触发严重预警', NOW(), NOW()
FROM sys_dept d
WHERE d.code = 'PEDIATRICIAN'
  AND NOT EXISTS (SELECT 1 FROM dept_registration_alert_rule r WHERE r.dept_id = d.dept_id AND r.rule_name = '儿科就诊量异常增幅')
LIMIT 1;

INSERT INTO dept_registration_alert_rule (dept_id, rule_name, threshold_type, threshold_value, alert_level, enabled, remark, created_time, updated_time)
SELECT d.dept_id, '内科日挂号量预警', 1, 50.00, 2, 1, '内科/通科日挂号超过50例触发警告', NOW(), NOW()
FROM sys_dept d
WHERE d.code IN ('INTERNIST', 'DOCTOR')
  AND NOT EXISTS (SELECT 1 FROM dept_registration_alert_rule r WHERE r.dept_id = d.dept_id AND r.rule_name = '内科日挂号量预警')
LIMIT 1;

INSERT INTO dept_registration_alert_rule (dept_id, rule_name, threshold_type, threshold_value, alert_level, enabled, remark, created_time, updated_time)
SELECT d.dept_id, '内科就诊量异常增幅', 2, 40.00, 2, 1, '内科较近7日均值增幅超40%触发警告', NOW(), NOW()
FROM sys_dept d
WHERE d.code = 'INTERNIST'
  AND NOT EXISTS (SELECT 1 FROM dept_registration_alert_rule r WHERE r.dept_id = d.dept_id AND r.rule_name = '内科就诊量异常增幅')
LIMIT 1;

INSERT INTO dept_registration_alert_rule (dept_id, rule_name, threshold_type, threshold_value, alert_level, enabled, remark, created_time, updated_time)
SELECT d.dept_id, '内科明日预约预警', 3, 30.00, 2, 1, '内科明日预约超过30例触发警告', NOW(), NOW()
FROM sys_dept d
WHERE d.code = 'INTERNIST'
  AND NOT EXISTS (SELECT 1 FROM dept_registration_alert_rule r WHERE r.dept_id = d.dept_id AND r.rule_name = '内科明日预约预警')
LIMIT 1;

INSERT INTO dept_registration_alert_rule (dept_id, rule_name, threshold_type, threshold_value, alert_level, enabled, remark, created_time, updated_time)
SELECT d.dept_id, '内科过去一周内预约预警', 4, 100.00, 2, 1, '内科过去一周内预约合计超过100例触发警告', NOW(), NOW()
FROM sys_dept d
WHERE d.code = 'INTERNIST'
  AND NOT EXISTS (SELECT 1 FROM dept_registration_alert_rule r WHERE r.dept_id = d.dept_id AND r.rule_name = '内科过去一周内预约预警')
LIMIT 1;

INSERT INTO dept_registration_alert_rule (dept_id, rule_name, threshold_type, threshold_value, alert_level, enabled, remark, created_time, updated_time)
SELECT d.dept_id, '急诊科今日挂号预警', 1, 40.00, 2, 1, '急诊科今日挂号超过40例触发警告', NOW(), NOW()
FROM sys_dept d
WHERE d.code = 'ER_DOCTOR'
  AND NOT EXISTS (SELECT 1 FROM dept_registration_alert_rule r WHERE r.dept_id = d.dept_id AND r.rule_name = '急诊科今日挂号预警')
LIMIT 1;

INSERT INTO dept_registration_alert_rule (dept_id, rule_name, threshold_type, threshold_value, alert_level, enabled, remark, created_time, updated_time)
SELECT d.dept_id, '急诊科明日预约预警', 3, 35.00, 2, 1, '急诊科明日预约超过35例触发警告', NOW(), NOW()
FROM sys_dept d
WHERE d.code = 'ER_DOCTOR'
  AND NOT EXISTS (SELECT 1 FROM dept_registration_alert_rule r WHERE r.dept_id = d.dept_id AND r.rule_name = '急诊科明日预约预警')
LIMIT 1;

INSERT INTO dept_registration_alert_rule (dept_id, rule_name, threshold_type, threshold_value, alert_level, enabled, remark, created_time, updated_time)
SELECT d.dept_id, '急诊科过去一周内预约预警', 4, 80.00, 3, 1, '急诊科过去一周内预约合计超过80例触发严重预警', NOW(), NOW()
FROM sys_dept d
WHERE d.code = 'ER_DOCTOR'
  AND NOT EXISTS (SELECT 1 FROM dept_registration_alert_rule r WHERE r.dept_id = d.dept_id AND r.rule_name = '急诊科过去一周内预约预警')
LIMIT 1;

INSERT INTO dept_registration_alert_rule (dept_id, rule_name, threshold_type, threshold_value, alert_level, enabled, remark, created_time, updated_time)
SELECT d.dept_id, '外科今日挂号预警', 1, 25.00, 1, 1, '外科今日挂号超过25例触发提示', NOW(), NOW()
FROM sys_dept d
WHERE d.code = 'SURGEON'
  AND NOT EXISTS (SELECT 1 FROM dept_registration_alert_rule r WHERE r.dept_id = d.dept_id AND r.rule_name = '外科今日挂号预警')
LIMIT 1;

INSERT INTO dept_registration_alert_rule (dept_id, rule_name, threshold_type, threshold_value, alert_level, enabled, remark, created_time, updated_time)
SELECT d.dept_id, '外科明日预约预警', 3, 20.00, 2, 1, '外科明日预约超过20例触发警告', NOW(), NOW()
FROM sys_dept d
WHERE d.code = 'SURGEON'
  AND NOT EXISTS (SELECT 1 FROM dept_registration_alert_rule r WHERE r.dept_id = d.dept_id AND r.rule_name = '外科明日预约预警')
LIMIT 1;

INSERT INTO dept_registration_alert_rule (dept_id, rule_name, threshold_type, threshold_value, alert_level, enabled, remark, created_time, updated_time)
SELECT d.dept_id, '外科过去一周内预约预警', 4, 70.00, 2, 1, '外科过去一周内预约合计超过70例触发警告', NOW(), NOW()
FROM sys_dept d
WHERE d.code = 'SURGEON'
  AND NOT EXISTS (SELECT 1 FROM dept_registration_alert_rule r WHERE r.dept_id = d.dept_id AND r.rule_name = '外科过去一周内预约预警')
LIMIT 1;

INSERT INTO dept_registration_alert_rule (dept_id, rule_name, threshold_type, threshold_value, alert_level, enabled, remark, created_time, updated_time)
SELECT d.dept_id, '妇产科明日预约预警', 3, 20.00, 2, 1, '妇产科明日预约超过20例触发警告', NOW(), NOW()
FROM sys_dept d
WHERE d.code = 'GYNECOLOGIST'
  AND NOT EXISTS (SELECT 1 FROM dept_registration_alert_rule r WHERE r.dept_id = d.dept_id AND r.rule_name = '妇产科明日预约预警')
LIMIT 1;

INSERT INTO dept_registration_alert_rule (dept_id, rule_name, threshold_type, threshold_value, alert_level, enabled, remark, created_time, updated_time)
SELECT d.dept_id, '皮肤科过去一周内预约预警', 4, 50.00, 1, 1, '皮肤科过去一周内预约合计超过50例触发提示', NOW(), NOW()
FROM sys_dept d
WHERE d.code = 'DERMATOLOGIST'
  AND NOT EXISTS (SELECT 1 FROM dept_registration_alert_rule r WHERE r.dept_id = d.dept_id AND r.rule_name = '皮肤科过去一周内预约预警')
LIMIT 1;

INSERT INTO dept_registration_alert_rule (dept_id, rule_name, threshold_type, threshold_value, alert_level, enabled, remark, created_time, updated_time)
SELECT d.dept_id, '皮肤科明日预约预警', 3, 15.00, 2, 1, '皮肤科明日预约超过15例触发警告', NOW(), NOW()
FROM sys_dept d
WHERE d.code = 'DERMATOLOGIST'
  AND NOT EXISTS (SELECT 1 FROM dept_registration_alert_rule r WHERE r.dept_id = d.dept_id AND r.rule_name = '皮肤科明日预约预警')
LIMIT 1;
