-- medical-service/src/main/resources/db/v13_create_medical_record.sql
-- 病历表

CREATE TABLE IF NOT EXISTS `medical_record` (
                                                `record_id` bigint NOT NULL AUTO_INCREMENT COMMENT '病历ID',
                                                `record_no` varchar(50) NOT NULL COMMENT '病历号',
    `patient_id` bigint NOT NULL COMMENT '患者ID',
    `doctor_id` bigint NOT NULL COMMENT '医生ID',
    `appointment_id` bigint DEFAULT NULL COMMENT '关联预约ID',
    `visit_date` date NOT NULL COMMENT '就诊日期',
    `chief_complaint` varchar(500) DEFAULT NULL COMMENT '主诉',
    `present_illness` text COMMENT '现病史',
    `past_history` text COMMENT '既往史',
    `physical_exam` text COMMENT '体格检查',
    `diagnosis` text COMMENT '诊断',
    `treatment_plan` text COMMENT '治疗计划',
    `ai_suggestion` text COMMENT 'AI辅助建议',
    `status` tinyint DEFAULT '1' COMMENT '1=草稿 2=已归档',
    `created_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `created_by` varchar(50) DEFAULT NULL COMMENT '创建人',
    `updated_by` varchar(50) DEFAULT NULL COMMENT '更新人',
    PRIMARY KEY (`record_id`),
    UNIQUE KEY `uk_record_no` (`record_no`),
    KEY `idx_patient_id` (`patient_id`),
    KEY `idx_doctor_id` (`doctor_id`),
    KEY `idx_appointment_id` (`appointment_id`),
    KEY `idx_visit_date` (`visit_date`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='病历表';

-- 处方表
CREATE TABLE IF NOT EXISTS `prescription` (
                                              `prescription_id` bigint NOT NULL AUTO_INCREMENT COMMENT '处方ID',
                                              `prescription_no` varchar(50) NOT NULL COMMENT '处方号',
    `record_id` bigint NOT NULL COMMENT '病历ID',
    `patient_id` bigint NOT NULL COMMENT '患者ID',
    `doctor_id` bigint NOT NULL COMMENT '开方医生ID',
    `total_amount` decimal(10,2) DEFAULT '0.00' COMMENT '总金额',
    `status` tinyint DEFAULT '1' COMMENT '1=待发药 2=已发药 3=已取消',
    `remark` varchar(500) DEFAULT NULL COMMENT '备注',
    `created_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `created_by` varchar(50) DEFAULT NULL COMMENT '创建人',
    `updated_by` varchar(50) DEFAULT NULL COMMENT '更新人',
    PRIMARY KEY (`prescription_id`),
    UNIQUE KEY `uk_prescription_no` (`prescription_no`),
    KEY `idx_record_id` (`record_id`),
    KEY `idx_patient_id` (`patient_id`),
    KEY `idx_doctor_id` (`doctor_id`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='处方表';

-- 处方明细表
CREATE TABLE IF NOT EXISTS `prescription_detail` (
                                                     `detail_id` bigint NOT NULL AUTO_INCREMENT COMMENT '明细ID',
                                                     `prescription_id` bigint NOT NULL COMMENT '处方ID',
                                                     `medicine_id` bigint NOT NULL COMMENT '药品ID',
                                                     `quantity` int NOT NULL COMMENT '数量',
                                                     `dosage` varchar(100) DEFAULT NULL COMMENT '用法用量',
    `unit_price` decimal(10,2) DEFAULT NULL COMMENT '单价',
    `amount` decimal(10,2) DEFAULT NULL COMMENT '小计金额',
    `remark` varchar(200) DEFAULT NULL COMMENT '备注',
    `created_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`detail_id`),
    KEY `idx_prescription_id` (`prescription_id`),
    KEY `idx_medicine_id` (`medicine_id`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='处方明细表';

-- 收费记录表
CREATE TABLE IF NOT EXISTS `payment` (
                                         `payment_id` bigint NOT NULL AUTO_INCREMENT COMMENT '收费ID',
                                         `payment_no` varchar(50) NOT NULL COMMENT '流水号',
    `patient_id` bigint NOT NULL COMMENT '患者ID',
    `biz_type` varchar(20) DEFAULT NULL COMMENT '业务类型：APPOINTMENT/PRESCRIPTION/EXAM',
    `biz_id` bigint DEFAULT NULL COMMENT '业务ID',
    `amount` decimal(10,2) NOT NULL COMMENT '金额',
    `pay_method` varchar(20) DEFAULT NULL COMMENT '支付方式：CASH/WECHAT/ALIPAY/CARD',
    `pay_time` datetime DEFAULT NULL COMMENT '支付时间',
    `operator_id` bigint DEFAULT NULL COMMENT '操作员ID',
    `status` tinyint DEFAULT '1' COMMENT '1=已支付 2=已退款',
    `remark` varchar(500) DEFAULT NULL COMMENT '备注',
    `created_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `created_by` varchar(50) DEFAULT NULL COMMENT '创建人',
    `updated_by` varchar(50) DEFAULT NULL COMMENT '更新人',
    PRIMARY KEY (`payment_id`),
    UNIQUE KEY `uk_payment_no` (`payment_no`),
    KEY `idx_patient_id` (`patient_id`),
    KEY `idx_biz_type_biz_id` (`biz_type`, `biz_id`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='收费记录表';

-- 插入示例数据（可选）
INSERT INTO `medical_record` (`record_no`, `patient_id`, `doctor_id`, `visit_date`, `chief_complaint`, `diagnosis`, `status`) VALUES
                                                                                                                                  ('R202412010001', 1, 1, CURDATE(), '发热、咳嗽3天', '急性上呼吸道感染', 2),
                                                                                                                                  ('R202412010002', 2, 1, CURDATE(), '头痛、恶心2天', '高血压', 1)
    ON DUPLICATE KEY UPDATE record_no=record_no;