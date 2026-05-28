-- Phase E: AI consult session and messages
DROP TABLE IF EXISTS `ai_consult_message`;
DROP TABLE IF EXISTS `ai_consult_session`;

CREATE TABLE `ai_consult_session` (
  `session_id` bigint NOT NULL AUTO_INCREMENT,
  `session_no` varchar(50) NOT NULL,
  `patient_id` bigint NOT NULL,
  `title` varchar(200) DEFAULT NULL,
  `status` tinyint NOT NULL DEFAULT 1,
  `chief_complaint` varchar(500) DEFAULT NULL,
  `suggested_dept_id` bigint DEFAULT NULL,
  `urgency_level` varchar(20) DEFAULT 'NORMAL',
  `summary` text,
  `message_count` int NOT NULL DEFAULT 0,
  `token_used` int NOT NULL DEFAULT 0,
  `deleted` tinyint NOT NULL DEFAULT 0,
  `created_time` datetime DEFAULT NULL,
  `updated_time` datetime DEFAULT NULL,
  `ended_time` datetime DEFAULT NULL,
  PRIMARY KEY (`session_id`),
  UNIQUE KEY `uk_session_no` (`session_no`),
  KEY `idx_session_patient` (`patient_id`, `created_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `ai_consult_message` (
  `message_id` bigint NOT NULL AUTO_INCREMENT,
  `session_id` bigint NOT NULL,
  `role` varchar(20) NOT NULL,
  `content` text NOT NULL,
  `content_type` varchar(20) NOT NULL DEFAULT 'TEXT',
  `model_name` varchar(50) DEFAULT NULL,
  `prompt_tokens` int DEFAULT NULL,
  `completion_tokens` int DEFAULT NULL,
  `created_time` datetime DEFAULT NULL,
  PRIMARY KEY (`message_id`),
  KEY `idx_message_session_time` (`session_id`, `created_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
