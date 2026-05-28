-- 药品库存变动流水（阶段 C）
DROP TABLE IF EXISTS `medicine_stock_log`;
CREATE TABLE `medicine_stock_log` (
  `log_id` bigint NOT NULL AUTO_INCREMENT COMMENT '流水ID',
  `medicine_id` bigint NOT NULL COMMENT '药品ID',
  `change_qty` int NOT NULL COMMENT '变动数量（正增负减）',
  `before_qty` int NOT NULL COMMENT '变动前库存',
  `after_qty` int NOT NULL COMMENT '变动后库存',
  `biz_type` varchar(20) NOT NULL COMMENT 'DISPENSE/ADJUST/INBOUND',
  `biz_id` bigint DEFAULT NULL COMMENT '关联业务ID（如处方ID）',
  `operator_id` bigint DEFAULT NULL COMMENT '操作人用户ID',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注/差异说明',
  `created_time` datetime DEFAULT NULL COMMENT '创建时间',
  PRIMARY KEY (`log_id`),
  KEY `idx_medicine_id` (`medicine_id`),
  KEY `idx_biz` (`biz_type`, `biz_id`),
  KEY `idx_created_time` (`created_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='药品库存变动流水';
