-- ============================================================
-- 智能医疗服务管理系统 - 数据库建表脚本
-- 数据库：MySQL 8.x
-- 字符集：utf8mb4
-- ============================================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- 1. 系统管理
-- ----------------------------

CREATE DATABASE IF NOT EXISTS `medical_service` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE `medical_service`;

-- 部门/科室
DROP TABLE IF EXISTS `sys_dept`;
CREATE TABLE `sys_dept` (
  `dept_id` bigint NOT NULL AUTO_INCREMENT,
  `parent_id` bigint DEFAULT 0,
  `name` varchar(100) NOT NULL,
  `code` varchar(50) DEFAULT NULL,
  `manager` varchar(50) DEFAULT NULL,
  `phone` varchar(20) DEFAULT NULL,
  `address` varchar(200) DEFAULT NULL,
  `sort_order` int DEFAULT 0,
  `status` tinyint DEFAULT 1 COMMENT '1=启用 0=禁用',
  `remark` varchar(500) DEFAULT NULL,
  `created_time` datetime DEFAULT NULL,
  `updated_time` datetime DEFAULT NULL,
  `created_by` varchar(50) DEFAULT NULL,
  `updated_by` varchar(50) DEFAULT NULL,
  PRIMARY KEY (`dept_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 角色
DROP TABLE IF EXISTS `sys_role`;
CREATE TABLE `sys_role` (
  `role_id` bigint NOT NULL AUTO_INCREMENT,
  `role_code` varchar(50) NOT NULL,
  `role_name` varchar(50) DEFAULT NULL,
  `description` varchar(200) DEFAULT NULL,
  `status` tinyint DEFAULT 1,
  `created_time` datetime DEFAULT NULL,
  `updated_time` datetime DEFAULT NULL,
  `created_by` varchar(50) DEFAULT NULL,
  `updated_by` varchar(50) DEFAULT NULL,
  PRIMARY KEY (`role_id`),
  UNIQUE KEY `uk_role_code` (`role_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 用户
DROP TABLE IF EXISTS `sys_user`;
CREATE TABLE `sys_user` (
  `user_id` bigint NOT NULL AUTO_INCREMENT,
  `username` varchar(50) NOT NULL,
  `password` varchar(255) NOT NULL,
  `name` varchar(50) DEFAULT NULL,
  `email` varchar(100) DEFAULT NULL,
  `mobile_phone` varchar(20) DEFAULT NULL,
  `avatar_url` varchar(500) DEFAULT NULL,
  `dept_id` bigint DEFAULT NULL,
  `status` tinyint DEFAULT 1 COMMENT '1=启用 0=禁用',
  `last_login_ip` varchar(50) DEFAULT NULL,
  `last_login_time` datetime DEFAULT NULL,
  `remark` varchar(500) DEFAULT NULL,
  `created_time` datetime DEFAULT NULL,
  `updated_time` datetime DEFAULT NULL,
  `created_by` varchar(50) DEFAULT NULL,
  `updated_by` varchar(50) DEFAULT NULL,
  PRIMARY KEY (`user_id`),
  UNIQUE KEY `uk_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 用户-角色
DROP TABLE IF EXISTS `sys_user_role`;
CREATE TABLE `sys_user_role` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `role_id` bigint NOT NULL,
  `created_time` datetime DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_role` (`user_id`,`role_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ----------------------------
-- 2. 患者与医生
-- ----------------------------

-- 患者
DROP TABLE IF EXISTS `patient`;
CREATE TABLE `patient` (
  `patient_id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint DEFAULT NULL,
  `patient_no` varchar(50) NOT NULL,
  `name` varchar(50) NOT NULL,
  `id_card` varchar(18) DEFAULT NULL,
  `gender` char(1) DEFAULT NULL COMMENT 'M/F',
  `birth_date` date DEFAULT NULL,
  `phone` varchar(20) DEFAULT NULL,
  `email` varchar(100) DEFAULT NULL,
  `address` varchar(200) DEFAULT NULL,
  `avatar_url` varchar(500) DEFAULT NULL,
  `blood_type` varchar(10) DEFAULT NULL,
  `allergy_history` varchar(500) DEFAULT NULL,
  `chronic_diseases` varchar(500) DEFAULT NULL,
  `emergency_contact` varchar(50) DEFAULT NULL,
  `emergency_phone` varchar(20) DEFAULT NULL,
  `status` tinyint DEFAULT 1,
  `remark` varchar(500) DEFAULT NULL,
  `created_time` datetime DEFAULT NULL,
  `updated_time` datetime DEFAULT NULL,
  `created_by` varchar(50) DEFAULT NULL,
  `updated_by` varchar(50) DEFAULT NULL,
  PRIMARY KEY (`patient_id`),
  UNIQUE KEY `uk_patient_no` (`patient_no`),
  UNIQUE KEY `uk_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 医生
DROP TABLE IF EXISTS `doctor`;
CREATE TABLE `doctor` (
  `doctor_id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `doctor_no` varchar(50) NOT NULL,
  `name` varchar(50) NOT NULL,
  `dept_id` bigint NOT NULL,
  `title` varchar(50) DEFAULT NULL,
  `specialty` varchar(200) DEFAULT NULL,
  `introduction` text,
  `avatar_url` varchar(500) DEFAULT NULL,
  `phone` varchar(20) DEFAULT NULL,
  `consultation_fee` decimal(10,2) DEFAULT 0.00,
  `status` tinyint DEFAULT 1,
  `sort_order` int DEFAULT 0,
  `remark` varchar(500) DEFAULT NULL,
  `created_time` datetime DEFAULT NULL,
  `updated_time` datetime DEFAULT NULL,
  `created_by` varchar(50) DEFAULT NULL,
  `updated_by` varchar(50) DEFAULT NULL,
  PRIMARY KEY (`doctor_id`),
  UNIQUE KEY `uk_doctor_no` (`doctor_no`),
  UNIQUE KEY `uk_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ----------------------------
-- 3. 排班与预约
-- ----------------------------

-- 排班
DROP TABLE IF EXISTS `schedule`;
CREATE TABLE `schedule` (
  `schedule_id` bigint NOT NULL AUTO_INCREMENT,
  `doctor_id` bigint NOT NULL,
  `dept_id` bigint NOT NULL,
  `schedule_date` date NOT NULL,
  `time_slot` varchar(20) DEFAULT NULL,
  `total_slots` int DEFAULT 20,
  `booked_slots` int DEFAULT 0,
  `status` tinyint DEFAULT 1 COMMENT '1=可预约 0=停诊',
  `remark` varchar(500) DEFAULT NULL,
  `created_time` datetime DEFAULT NULL,
  `updated_time` datetime DEFAULT NULL,
  `created_by` varchar(50) DEFAULT NULL,
  `updated_by` varchar(50) DEFAULT NULL,
  PRIMARY KEY (`schedule_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 预约
DROP TABLE IF EXISTS `appointment`;
CREATE TABLE `appointment` (
  `appointment_id` bigint NOT NULL AUTO_INCREMENT,
  `appointment_no` varchar(50) NOT NULL,
  `patient_id` bigint NOT NULL,
  `doctor_id` bigint NOT NULL,
  `schedule_id` bigint NOT NULL,
  `appointment_date` date NOT NULL,
  `time_slot` varchar(20) DEFAULT NULL,
  `queue_no` int DEFAULT NULL,
  `status` tinyint DEFAULT 1 COMMENT '1=待就诊 2=已就诊 3=已取消 4=爽约',
  `fee_amount` decimal(10,2) DEFAULT NULL,
  `paid` tinyint DEFAULT 0 COMMENT '0=未支付 1=已支付',
  `paid_time` datetime DEFAULT NULL,
  `check_in_time` datetime DEFAULT NULL,
  `remark` varchar(500) DEFAULT NULL,
  `created_time` datetime DEFAULT NULL,
  `updated_time` datetime DEFAULT NULL,
  `created_by` varchar(50) DEFAULT NULL,
  `updated_by` varchar(50) DEFAULT NULL,
  PRIMARY KEY (`appointment_id`),
  UNIQUE KEY `uk_appointment_no` (`appointment_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ----------------------------
-- 4. 病历与处方
-- ----------------------------

-- 病历
DROP TABLE IF EXISTS `medical_record`;
CREATE TABLE `medical_record` (
  `record_id` bigint NOT NULL AUTO_INCREMENT,
  `record_no` varchar(50) NOT NULL,
  `patient_id` bigint NOT NULL,
  `doctor_id` bigint NOT NULL,
  `appointment_id` bigint DEFAULT NULL,
  `visit_date` date NOT NULL,
  `chief_complaint` varchar(500) DEFAULT NULL,
  `present_illness` text,
  `past_history` text,
  `physical_exam` text,
  `diagnosis` text,
  `treatment_plan` text,
  `ai_suggestion` text,
  `status` tinyint DEFAULT 1 COMMENT '1=草稿 2=已归档',
  `created_time` datetime DEFAULT NULL,
  `updated_time` datetime DEFAULT NULL,
  `created_by` varchar(50) DEFAULT NULL,
  `updated_by` varchar(50) DEFAULT NULL,
  PRIMARY KEY (`record_id`),
  UNIQUE KEY `uk_record_no` (`record_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 处方
DROP TABLE IF EXISTS `prescription`;
CREATE TABLE `prescription` (
  `prescription_id` bigint NOT NULL AUTO_INCREMENT,
  `prescription_no` varchar(50) NOT NULL,
  `record_id` bigint NOT NULL,
  `patient_id` bigint NOT NULL,
  `doctor_id` bigint NOT NULL,
  `total_amount` decimal(10,2) DEFAULT 0.00,
  `status` tinyint DEFAULT 1 COMMENT '1=待发药 2=已发药 3=已取消',
  `remark` varchar(500) DEFAULT NULL,
  `created_time` datetime DEFAULT NULL,
  `updated_time` datetime DEFAULT NULL,
  `created_by` varchar(50) DEFAULT NULL,
  `updated_by` varchar(50) DEFAULT NULL,
  PRIMARY KEY (`prescription_id`),
  UNIQUE KEY `uk_prescription_no` (`prescription_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 处方明细
DROP TABLE IF EXISTS `prescription_detail`;
CREATE TABLE `prescription_detail` (
  `detail_id` bigint NOT NULL AUTO_INCREMENT,
  `prescription_id` bigint NOT NULL,
  `medicine_id` bigint NOT NULL,
  `quantity` int NOT NULL,
  `dosage` varchar(100) DEFAULT NULL,
  `unit_price` decimal(10,2) DEFAULT NULL,
  `amount` decimal(10,2) DEFAULT NULL,
  `remark` varchar(200) DEFAULT NULL,
  `created_time` datetime DEFAULT NULL,
  `updated_time` datetime DEFAULT NULL,
  PRIMARY KEY (`detail_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ----------------------------
-- 5. 药品
-- ----------------------------

-- 药品分类
DROP TABLE IF EXISTS `medicine_category`;
CREATE TABLE `medicine_category` (
  `category_id` bigint NOT NULL AUTO_INCREMENT,
  `parent_id` bigint DEFAULT 0,
  `name` varchar(100) NOT NULL,
  `sort_order` int DEFAULT 0,
  `status` tinyint DEFAULT 1,
  `created_time` datetime DEFAULT NULL,
  `updated_time` datetime DEFAULT NULL,
  `created_by` varchar(50) DEFAULT NULL,
  `updated_by` varchar(50) DEFAULT NULL,
  PRIMARY KEY (`category_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 药品
DROP TABLE IF EXISTS `medicine`;
CREATE TABLE `medicine` (
  `medicine_id` bigint NOT NULL AUTO_INCREMENT,
  `medicine_code` varchar(50) DEFAULT NULL,
  `name` varchar(100) NOT NULL,
  `common_name` varchar(100) DEFAULT NULL,
  `category_id` bigint DEFAULT NULL,
  `spec` varchar(100) DEFAULT NULL,
  `unit` varchar(20) DEFAULT NULL,
  `manufacturer` varchar(200) DEFAULT NULL,
  `approval_no` varchar(100) DEFAULT NULL,
  `unit_price` decimal(10,2) DEFAULT 0.00,
  `cost_price` decimal(10,2) DEFAULT NULL,
  `stock_quantity` int DEFAULT 0,
  `min_stock` int DEFAULT 10,
  `status` tinyint DEFAULT 1,
  `remark` varchar(500) DEFAULT NULL,
  `created_time` datetime DEFAULT NULL,
  `updated_time` datetime DEFAULT NULL,
  `created_by` varchar(50) DEFAULT NULL,
  `updated_by` varchar(50) DEFAULT NULL,
  PRIMARY KEY (`medicine_id`),
  UNIQUE KEY `uk_medicine_code` (`medicine_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ----------------------------
-- 6. 收费
-- ----------------------------

DROP TABLE IF EXISTS `payment`;
CREATE TABLE `payment` (
  `payment_id` bigint NOT NULL AUTO_INCREMENT,
  `payment_no` varchar(50) NOT NULL,
  `patient_id` bigint NOT NULL,
  `biz_type` varchar(20) DEFAULT NULL COMMENT 'APPOINTMENT/PRESCRIPTION/EXAM',
  `biz_id` bigint DEFAULT NULL,
  `amount` decimal(10,2) NOT NULL,
  `pay_method` varchar(20) DEFAULT NULL,
  `pay_time` datetime DEFAULT NULL,
  `operator_id` bigint DEFAULT NULL,
  `status` tinyint DEFAULT 1 COMMENT '1=已支付 2=已退款',
  `remark` varchar(500) DEFAULT NULL,
  `created_time` datetime DEFAULT NULL,
  `updated_time` datetime DEFAULT NULL,
  `created_by` varchar(50) DEFAULT NULL,
  `updated_by` varchar(50) DEFAULT NULL,
  PRIMARY KEY (`payment_id`),
  UNIQUE KEY `uk_payment_no` (`payment_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ----------------------------
-- 初始数据
-- ----------------------------

INSERT INTO `sys_role` (`role_code`, `role_name`, `description`, `status`, `created_time`) VALUES
('SUPER_ADMIN', '超级管理员', '系统最高权限', 1, NOW()),
('ADMIN', '医院管理员', '日常运维管理', 1, NOW()),
('RECEPTIONIST', '挂号收费员', '前台挂号收费', 1, NOW()),
('DOCTOR', '医生', '门诊接诊', 1, NOW()),
('NURSE', '护士/药师', '发药、导诊', 1, NOW()),
('PATIENT', '患者', '患者自助服务', 1, NOW());

-- 默认管理员 admin / 123456 (BCrypt 加密，与 book-library 相同)
INSERT INTO `sys_user` (`username`, `password`, `name`, `status`, `created_time`) VALUES
('admin', '$2a$10$rQbeBO6qTw.ha5Dy6RgaduzMUl9k5bvln4l6mLXTLd5rIdTz27WbO', '系统管理员', 1, NOW());

INSERT INTO `sys_user_role` (`user_id`, `role_id`, `created_time`)
SELECT 1, role_id, NOW() FROM sys_role WHERE role_code = 'SUPER_ADMIN';

SET FOREIGN_KEY_CHECKS = 1;
