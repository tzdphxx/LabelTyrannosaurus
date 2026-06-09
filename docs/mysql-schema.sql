-- MySQL dump 10.13  Distrib 8.0.41, for Win64 (x86_64)
--
-- Host: 8.134.88.199    Database: label_db
-- ------------------------------------------------------
-- Server version	8.0.45

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!50503 SET NAMES utf8mb4 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Table structure for table `agent_runs`
--

DROP TABLE IF EXISTS `agent_runs`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `agent_runs` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `agent_type` varchar(32) NOT NULL,
  `submission_id` bigint DEFAULT NULL,
  `assignment_id` bigint DEFAULT NULL,
  `provider_id` bigint DEFAULT NULL,
  `model_name` varchar(128) DEFAULT NULL,
  `prompt_version` varchar(64) DEFAULT NULL,
  `input_snapshot` json DEFAULT NULL COMMENT 'Prompt input snapshot for traceability; do not reconstruct it from mutable business tables.',
  `output_snapshot` json DEFAULT NULL COMMENT 'Raw or normalized model output for this concrete agent execution.',
  `status` varchar(24) NOT NULL DEFAULT 'PENDING',
  `error_message` text,
  `trace_id` varchar(128) DEFAULT NULL,
  `latency_ms` bigint DEFAULT NULL,
  `queued_at` datetime(3) DEFAULT NULL,
  `started_at` datetime(3) DEFAULT NULL,
  `finished_at` datetime(3) DEFAULT NULL,
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`),
  KEY `idx_agent_runs_submission` (`submission_id`,`created_at`),
  KEY `idx_agent_runs_status` (`status`,`created_at`),
  KEY `fk_agent_runs_provider` (`provider_id`),
  KEY `idx_agent_runs_assignment` (`assignment_id`,`created_at`),
  CONSTRAINT `fk_agent_runs_assignment` FOREIGN KEY (`assignment_id`) REFERENCES `assignments` (`id`),
  CONSTRAINT `fk_agent_runs_provider` FOREIGN KEY (`provider_id`) REFERENCES `llm_providers` (`id`),
  CONSTRAINT `fk_agent_runs_submission` FOREIGN KEY (`submission_id`) REFERENCES `submissions` (`id`),
  CONSTRAINT `chk_agent_runs_status` CHECK ((`status` in (_utf8mb4'PENDING',_utf8mb4'RUNNING',_utf8mb4'SUCCESS',_utf8mb4'FAILED',_utf8mb4'RATE_LIMITED',_utf8mb4'MANUAL_REQUIRED'))),
  CONSTRAINT `chk_agent_runs_type` CHECK ((`agent_type` in (_utf8mb4'AI_REVIEW',_utf8mb4'LLM_TRIGGER',_utf8mb4'AI_REVIEW_CONFIG_TEST',_utf8mb4'PRE_ANNOTATION')))
) ENGINE=InnoDB AUTO_INCREMENT=739 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='Concrete AI/LLM execution attempts, including retries and failures.';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `ai_review_config_versions`
--

DROP TABLE IF EXISTS `ai_review_config_versions`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ai_review_config_versions` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `config_id` bigint NOT NULL,
  `task_id` bigint NOT NULL,
  `version_no` int NOT NULL,
  `prompt_version` varchar(64) NOT NULL,
  `provider_id` bigint DEFAULT NULL,
  `model_name` varchar(128) DEFAULT NULL,
  `snapshot_json` json NOT NULL,
  `created_by` bigint NOT NULL,
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_ai_review_config_versions_config_version` (`config_id`,`version_no`),
  KEY `idx_ai_review_config_versions_task_created` (`task_id`,`created_at`),
  KEY `idx_ai_review_config_versions_provider_model` (`provider_id`,`model_name`,`created_at`),
  KEY `fk_ai_review_config_versions_created_by` (`created_by`),
  CONSTRAINT `fk_ai_review_config_versions_config` FOREIGN KEY (`config_id`) REFERENCES `ai_review_configs` (`id`),
  CONSTRAINT `fk_ai_review_config_versions_created_by` FOREIGN KEY (`created_by`) REFERENCES `users` (`id`),
  CONSTRAINT `fk_ai_review_config_versions_provider` FOREIGN KEY (`provider_id`) REFERENCES `llm_providers` (`id`),
  CONSTRAINT `fk_ai_review_config_versions_task` FOREIGN KEY (`task_id`) REFERENCES `tasks` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=38 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='Immutable AI review prompt/config versions.';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `ai_review_configs`
--

DROP TABLE IF EXISTS `ai_review_configs`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ai_review_configs` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `task_id` bigint NOT NULL COMMENT '任务ID',
  `provider_id` bigint DEFAULT NULL COMMENT '服务提供商ID',
  `model_name` varchar(128) DEFAULT NULL COMMENT 'model名称',
  `enabled` tinyint(1) NOT NULL DEFAULT '1' COMMENT '启用状态',
  `prompt_template` mediumtext COMMENT '提示词模板',
  `scoring_dimensions_json` json DEFAULT NULL,
  `output_schema_json` json DEFAULT NULL COMMENT 'output结构JSON',
  `prompt_version` varchar(64) NOT NULL DEFAULT 'v1',
  `dimension_config_json` json DEFAULT NULL COMMENT '维度配置JSON',
  `pass_threshold` decimal(5,2) DEFAULT NULL COMMENT '通过阈值',
  `manual_review_threshold` decimal(5,2) DEFAULT NULL,
  `reject_threshold` decimal(5,2) DEFAULT NULL COMMENT '拒绝阈值',
  `manual_threshold` decimal(5,2) DEFAULT NULL COMMENT '人工阈值',
  `ai_reject_action` varchar(24) NOT NULL DEFAULT 'SUGGEST_ONLY' COMMENT 'AI拒绝动作',
  `max_retry` int NOT NULL DEFAULT '3' COMMENT '最大重试',
  `agent_mode` varchar(24) NOT NULL DEFAULT 'DIRECT' COMMENT '智能体模式',
  `enabled_tools_json` json DEFAULT NULL,
  `enabled_tools` json DEFAULT NULL COMMENT '启用工具列表',
  `max_iterations` int NOT NULL DEFAULT '10' COMMENT '最大迭代次数',
  `created_by` bigint NOT NULL COMMENT '创建人',
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  `updated_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
  `ai_flow_policy` varchar(30) NOT NULL DEFAULT 'MANUAL_FIRST' COMMENT 'MANUAL_FIRST, AI_PASS_ONLY, AI_REJECT_ONLY, AI_PASS_AND_REJECT, ALWAYS_MANUAL',
  `allow_ai_direct_approve` tinyint(1) NOT NULL DEFAULT '0',
  `allow_ai_direct_reject` tinyint(1) NOT NULL DEFAULT '0',
  `confidence_threshold` decimal(3,2) DEFAULT '0.85',
  `risk_flags_force_manual` json DEFAULT NULL COMMENT 'Risk flag values that force manual review',
  `multimodal_enabled` tinyint(1) NOT NULL DEFAULT '1',
  `degradation_penalty` decimal(3,2) DEFAULT '0.20',
  `vision_detail` varchar(20) DEFAULT 'auto',
  `max_images_per_request` int DEFAULT '5',
  `allow_ai_direct_approve_when_degraded` tinyint(1) NOT NULL DEFAULT '0',
  `review_strategy` varchar(32) NOT NULL DEFAULT 'LIGHTWEIGHT' COMMENT '审核策略: LIGHTWEIGHT | PARALLEL_VOTE | DEEP_DIMENSION | AGENT_DEBATE',
  `vote_models_json` json DEFAULT NULL COMMENT '投票模型列表, JSON array of {providerId, modelName}',
  `vote_min_agreement` int DEFAULT '2' COMMENT '最少一致票数, 默认2',
  `dimension_reviewers_json` json DEFAULT NULL COMMENT '深度模式 维度->模型列表映射, JSON object',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_ai_review_configs_task` (`task_id`),
  KEY `idx_ai_review_configs_task` (`task_id`),
  KEY `fk_ai_review_configs_provider` (`provider_id`),
  KEY `fk_ai_review_configs_created_by` (`created_by`),
  CONSTRAINT `fk_ai_review_configs_created_by` FOREIGN KEY (`created_by`) REFERENCES `users` (`id`),
  CONSTRAINT `fk_ai_review_configs_provider` FOREIGN KEY (`provider_id`) REFERENCES `llm_providers` (`id`),
  CONSTRAINT `fk_ai_review_configs_task` FOREIGN KEY (`task_id`) REFERENCES `tasks` (`id`),
  CONSTRAINT `chk_agent_mode` CHECK ((`agent_mode` in (_utf8mb4'DIRECT',_utf8mb4'SUPERVISOR'))),
  CONSTRAINT `chk_ai_review_configs_reject_action` CHECK ((`ai_reject_action` in (_utf8mb4'SUGGEST_ONLY',_utf8mb4'RETURN_TO_LABELER',_utf8mb4'MANUAL_REVIEW')))
) ENGINE=InnoDB AUTO_INCREMENT=75 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='AI审核配置表，存储AI审核配置相关业务数据。';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `ai_review_results`
--

DROP TABLE IF EXISTS `ai_review_results`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ai_review_results` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `submission_id` bigint NOT NULL,
  `effective_run_id` bigint DEFAULT NULL COMMENT 'The agent run whose output is currently effective for this AI review result.',
  `prompt_version_id` bigint DEFAULT NULL,
  `provider_id` bigint DEFAULT NULL,
  `model_name` varchar(128) DEFAULT NULL,
  `status` varchar(24) NOT NULL DEFAULT 'PENDING',
  `decision` varchar(24) DEFAULT NULL,
  `average_score` decimal(6,3) DEFAULT NULL,
  `dimension_scores` json DEFAULT NULL,
  `risk_flags` json DEFAULT NULL,
  `suggestion` text,
  `prompt_snapshot` mediumtext COMMENT 'Final prompt sent to the provider for audit display.',
  `raw_response` mediumtext COMMENT 'Original provider response retained for troubleshooting and review transparency.',
  `retry_count` int NOT NULL DEFAULT '0',
  `next_retry_at` datetime(3) DEFAULT NULL,
  `error_code` varchar(100) DEFAULT NULL,
  `error_message` varchar(500) DEFAULT NULL,
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `updated_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  `flow_action` varchar(30) DEFAULT NULL COMMENT 'AI_DIRECT_APPROVE, AI_DIRECT_REJECT, AI_ASSIGN_MANUAL_REVIEW',
  `confidence` decimal(5,2) DEFAULT NULL,
  `prompt_mode` varchar(40) DEFAULT NULL,
  `degraded` tinyint(1) NOT NULL DEFAULT '0',
  `limitations` json DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_ai_review_results_submission` (`submission_id`),
  UNIQUE KEY `uk_ai_review_results_effective_run` (`effective_run_id`),
  KEY `idx_ai_review_results_status` (`status`),
  KEY `fk_ai_review_results_provider` (`provider_id`),
  KEY `idx_ai_review_results_decision_status` (`decision`,`status`),
  KEY `idx_ai_review_results_retry` (`status`,`next_retry_at`),
  KEY `idx_ai_review_results_prompt_version` (`prompt_version_id`),
  CONSTRAINT `fk_ai_review_results_effective_run` FOREIGN KEY (`effective_run_id`) REFERENCES `agent_runs` (`id`),
  CONSTRAINT `fk_ai_review_results_prompt_version` FOREIGN KEY (`prompt_version_id`) REFERENCES `ai_review_config_versions` (`id`),
  CONSTRAINT `fk_ai_review_results_provider` FOREIGN KEY (`provider_id`) REFERENCES `llm_providers` (`id`),
  CONSTRAINT `fk_ai_review_results_submission` FOREIGN KEY (`submission_id`) REFERENCES `submissions` (`id`),
  CONSTRAINT `chk_ai_review_results_decision` CHECK (((`decision` is null) or (`decision` in (_utf8mb4'PASS',_utf8mb4'REJECT',_utf8mb4'MANUAL_REVIEW')))),
  CONSTRAINT `chk_ai_review_results_status` CHECK ((`status` in (_utf8mb4'PENDING',_utf8mb4'RUNNING',_utf8mb4'SUCCESS',_utf8mb4'FAILED',_utf8mb4'RATE_LIMITED',_utf8mb4'MANUAL_REQUIRED')))
) ENGINE=InnoDB AUTO_INCREMENT=45 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='Business-level AI review conclusion for a submission; agent_runs stores execution details.';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `assignment_dispatches`
--

DROP TABLE IF EXISTS `assignment_dispatches`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `assignment_dispatches` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `task_id` bigint NOT NULL,
  `dataset_item_id` bigint NOT NULL,
  `labeler_id` bigint NOT NULL,
  `status` varchar(20) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/CLAIMED/EXPIRED/REVOKED',
  `dispatched_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `claimed_at` datetime(3) DEFAULT NULL,
  `expires_at` datetime(3) DEFAULT NULL,
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `updated_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_dispatch_pending_item` (`task_id`,`dataset_item_id`,`status`),
  KEY `idx_dispatch_labeler_task` (`labeler_id`,`task_id`,`status`),
  CONSTRAINT `fk_dispatch_labeler` FOREIGN KEY (`labeler_id`) REFERENCES `users` (`id`),
  CONSTRAINT `fk_dispatch_task` FOREIGN KEY (`task_id`) REFERENCES `tasks` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=61 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='Owner 手动指派标注任务';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `assignment_draft_versions`
--

DROP TABLE IF EXISTS `assignment_draft_versions`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `assignment_draft_versions` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `assignment_id` bigint NOT NULL,
  `draft_version` int NOT NULL,
  `answer_json` json DEFAULT NULL,
  `created_by` bigint NOT NULL,
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_assignment_draft_versions_assignment_version` (`assignment_id`,`draft_version`),
  KEY `idx_assignment_draft_versions_assignment_created` (`assignment_id`,`created_at`),
  KEY `fk_assignment_draft_versions_created_by` (`created_by`),
  CONSTRAINT `fk_assignment_draft_versions_assignment` FOREIGN KEY (`assignment_id`) REFERENCES `assignments` (`id`),
  CONSTRAINT `fk_assignment_draft_versions_created_by` FOREIGN KEY (`created_by`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='Append-only assignment draft answer snapshots.';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `assignments`
--

DROP TABLE IF EXISTS `assignments`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `assignments` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `task_id` bigint NOT NULL,
  `dataset_item_id` bigint NOT NULL,
  `labeler_id` bigint NOT NULL,
  `template_version_id` bigint NOT NULL,
  `status` varchar(20) NOT NULL DEFAULT 'CLAIMED',
  `draft_answer_json` json DEFAULT NULL COMMENT 'Latest server-side draft answer; Redis may cache the same draft for faster autosave.',
  `draft_version` int NOT NULL DEFAULT '1',
  `claimed_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `submitted_at` datetime(3) DEFAULT NULL,
  `returned_at` datetime(3) DEFAULT NULL,
  `ai_returned_at` datetime(3) DEFAULT NULL,
  `approved_at` datetime(3) DEFAULT NULL,
  `cancelled_at` datetime(3) DEFAULT NULL,
  `updated_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  `active_dataset_item_id` bigint GENERATED ALWAYS AS ((case when (`status` <> _utf8mb4'CANCELLED') then `dataset_item_id` else NULL end)) STORED,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_assignments_item_labeler` (`dataset_item_id`,`labeler_id`),
  UNIQUE KEY `uk_assignments_active_item` (`active_dataset_item_id`),
  KEY `idx_assignments_labeler_status` (`labeler_id`,`status`),
  KEY `idx_assignments_task_status` (`task_id`,`status`),
  KEY `fk_assignments_template_version` (`template_version_id`),
  CONSTRAINT `fk_assignments_item` FOREIGN KEY (`dataset_item_id`) REFERENCES `dataset_items` (`id`),
  CONSTRAINT `fk_assignments_labeler` FOREIGN KEY (`labeler_id`) REFERENCES `users` (`id`),
  CONSTRAINT `fk_assignments_task` FOREIGN KEY (`task_id`) REFERENCES `tasks` (`id`),
  CONSTRAINT `fk_assignments_template_version` FOREIGN KEY (`template_version_id`) REFERENCES `template_versions` (`id`),
  CONSTRAINT `chk_assignments_status` CHECK ((`status` in (_utf8mb4'CLAIMED',_utf8mb4'DRAFTING',_utf8mb4'SUBMITTED',_utf8mb4'AI_RETURNED',_utf8mb4'RETURNED',_utf8mb4'APPROVED',_utf8mb4'CANCELLED')))
) ENGINE=InnoDB AUTO_INCREMENT=940267 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='Claim record and draft state for one labeler on one dataset item.';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `audit_logs`
--

DROP TABLE IF EXISTS `audit_logs`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `audit_logs` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `biz_type` varchar(64) NOT NULL,
  `biz_id` bigint NOT NULL,
  `actor_type` varchar(32) NOT NULL,
  `actor_id` bigint DEFAULT NULL,
  `action` varchar(64) NOT NULL,
  `before_json` json DEFAULT NULL COMMENT 'Business object snapshot before the action when available.',
  `after_json` json DEFAULT NULL COMMENT 'Business object snapshot after the action when available.',
  `trace_id` varchar(128) DEFAULT NULL,
  `agent_run_id` bigint DEFAULT NULL,
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`),
  KEY `idx_audit_logs_biz` (`biz_type`,`biz_id`,`created_at`),
  KEY `idx_audit_logs_actor` (`actor_id`,`created_at`),
  KEY `idx_audit_logs_trace` (`trace_id`),
  KEY `idx_audit_logs_agent_run` (`agent_run_id`,`created_at`),
  CONSTRAINT `fk_audit_logs_actor` FOREIGN KEY (`actor_id`) REFERENCES `users` (`id`),
  CONSTRAINT `fk_audit_logs_agent_run` FOREIGN KEY (`agent_run_id`) REFERENCES `agent_runs` (`id`),
  CONSTRAINT `chk_audit_logs_actor_type` CHECK ((`actor_type` in (_utf8mb4'USER',_utf8mb4'SYSTEM_AGENT')))
) ENGINE=InnoDB AUTO_INCREMENT=751 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='Append-only audit trail for state transitions and critical business operations.';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `conflict_groups`
--

DROP TABLE IF EXISTS `conflict_groups`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `conflict_groups` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `task_id` bigint NOT NULL,
  `dataset_item_id` bigint NOT NULL,
  `status` varchar(24) NOT NULL DEFAULT 'OPEN',
  `consensus_score` decimal(6,3) DEFAULT NULL COMMENT 'Calculated agreement score across overlapping submissions.',
  `golden_submission_id` bigint DEFAULT NULL COMMENT 'Reviewer-selected golden submission for conflicted or overlapping labels.',
  `resolved_by` bigint DEFAULT NULL,
  `resolved_reason` text,
  `resolved_at` datetime(3) DEFAULT NULL,
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `updated_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_conflict_groups_task_item` (`task_id`,`dataset_item_id`),
  KEY `idx_conflict_groups_status` (`status`),
  KEY `fk_conflict_groups_item` (`dataset_item_id`),
  KEY `fk_conflict_groups_golden_submission` (`golden_submission_id`),
  KEY `fk_conflict_groups_resolved_by` (`resolved_by`),
  CONSTRAINT `fk_conflict_groups_golden_submission` FOREIGN KEY (`golden_submission_id`) REFERENCES `submissions` (`id`),
  CONSTRAINT `fk_conflict_groups_item` FOREIGN KEY (`dataset_item_id`) REFERENCES `dataset_items` (`id`),
  CONSTRAINT `fk_conflict_groups_resolved_by` FOREIGN KEY (`resolved_by`) REFERENCES `users` (`id`),
  CONSTRAINT `fk_conflict_groups_task` FOREIGN KEY (`task_id`) REFERENCES `tasks` (`id`),
  CONSTRAINT `chk_conflict_groups_status` CHECK ((`status` in (_utf8mb4'OPEN',_utf8mb4'RESOLVED')))
) ENGINE=InnoDB AUTO_INCREMENT=980002 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='Conflict and consensus resolution for multi-labeler overlap.';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `dataset_files`
--

DROP TABLE IF EXISTS `dataset_files`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `dataset_files` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `task_id` bigint NOT NULL COMMENT '任务ID',
  `file_id` bigint NOT NULL COMMENT '文件ID',
  `file_format` varchar(20) NOT NULL COMMENT '文件格式',
  `created_by` bigint NOT NULL COMMENT '创建人',
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_dataset_files_task` (`task_id`),
  KEY `fk_dataset_files_file` (`file_id`),
  KEY `fk_dataset_files_created_by` (`created_by`),
  CONSTRAINT `fk_dataset_files_created_by` FOREIGN KEY (`created_by`) REFERENCES `users` (`id`),
  CONSTRAINT `fk_dataset_files_file` FOREIGN KEY (`file_id`) REFERENCES `object_files` (`id`),
  CONSTRAINT `fk_dataset_files_task` FOREIGN KEY (`task_id`) REFERENCES `tasks` (`id`),
  CONSTRAINT `chk_dataset_files_format` CHECK ((`file_format` in (_utf8mb4'JSON',_utf8mb4'JSONL',_utf8mb4'EXCEL',_utf8mb4'CSV')))
) ENGINE=InnoDB AUTO_INCREMENT=900439 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='数据集文件表，存储数据集文件相关业务数据。';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `dataset_import_jobs`
--

DROP TABLE IF EXISTS `dataset_import_jobs`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `dataset_import_jobs` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `task_id` bigint NOT NULL COMMENT '任务ID',
  `dataset_file_id` bigint NOT NULL COMMENT '数据集文件ID',
  `status` varchar(20) NOT NULL DEFAULT 'PENDING' COMMENT 'status状态',
  `import_mode` varchar(20) NOT NULL DEFAULT 'APPEND' COMMENT '导入模式',
  `total_count` int NOT NULL DEFAULT '0' COMMENT '总计数量',
  `success_count` int NOT NULL DEFAULT '0' COMMENT '成功数量',
  `failed_count` int NOT NULL DEFAULT '0' COMMENT '失败数量',
  `error_report_file_id` bigint DEFAULT NULL COMMENT '错误report文件ID',
  `error_message` text COMMENT '错误信息',
  `started_at` datetime(3) DEFAULT NULL COMMENT '开始时间',
  `finished_at` datetime(3) DEFAULT NULL COMMENT '完成时间',
  `created_by` bigint NOT NULL COMMENT '创建人',
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_dataset_import_jobs_task` (`task_id`,`status`),
  KEY `fk_import_jobs_dataset_file` (`dataset_file_id`),
  KEY `fk_import_jobs_error_file` (`error_report_file_id`),
  KEY `fk_import_jobs_created_by` (`created_by`),
  CONSTRAINT `fk_import_jobs_created_by` FOREIGN KEY (`created_by`) REFERENCES `users` (`id`),
  CONSTRAINT `fk_import_jobs_dataset_file` FOREIGN KEY (`dataset_file_id`) REFERENCES `dataset_files` (`id`),
  CONSTRAINT `fk_import_jobs_error_file` FOREIGN KEY (`error_report_file_id`) REFERENCES `object_files` (`id`),
  CONSTRAINT `fk_import_jobs_task` FOREIGN KEY (`task_id`) REFERENCES `tasks` (`id`),
  CONSTRAINT `chk_import_jobs_mode` CHECK ((`import_mode` in (_utf8mb4'APPEND',_utf8mb4'OVERWRITE'))),
  CONSTRAINT `chk_import_jobs_status` CHECK ((`status` in (_utf8mb4'PENDING',_utf8mb4'RUNNING',_utf8mb4'SUCCESS',_utf8mb4'FAILED',_utf8mb4'PARTIAL_SUCCESS')))
) ENGINE=InnoDB AUTO_INCREMENT=900537 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='数据集导入作业表，存储数据集导入作业相关业务数据。';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `dataset_item_change_logs`
--

DROP TABLE IF EXISTS `dataset_item_change_logs`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `dataset_item_change_logs` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `task_id` bigint NOT NULL COMMENT '任务ID',
  `item_id` bigint DEFAULT NULL COMMENT '数据条目ID',
  `change_type` varchar(32) NOT NULL COMMENT '变更类型',
  `before_json` json DEFAULT NULL COMMENT '变更前JSON',
  `after_json` json DEFAULT NULL COMMENT '变更后JSON',
  `json_patch` json DEFAULT NULL COMMENT 'JSON补丁变更内容',
  `actor_id` bigint NOT NULL COMMENT '操作人ID',
  `failure_reason` text COMMENT '失败原因',
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_item_change_logs_task` (`task_id`,`created_at`),
  KEY `idx_item_change_logs_item` (`item_id`),
  KEY `fk_item_change_logs_actor` (`actor_id`),
  CONSTRAINT `fk_item_change_logs_actor` FOREIGN KEY (`actor_id`) REFERENCES `users` (`id`),
  CONSTRAINT `fk_item_change_logs_item` FOREIGN KEY (`item_id`) REFERENCES `dataset_items` (`id`),
  CONSTRAINT `fk_item_change_logs_task` FOREIGN KEY (`task_id`) REFERENCES `tasks` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=901486 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='数据集数据条目变更日志表，存储数据集数据条目变更日志相关业务数据。';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `dataset_item_media_contexts`
--

DROP TABLE IF EXISTS `dataset_item_media_contexts`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `dataset_item_media_contexts` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `dataset_item_id` bigint NOT NULL,
  `task_id` bigint NOT NULL,
  `media_type` varchar(24) NOT NULL,
  `processing_status` varchar(24) NOT NULL DEFAULT 'PENDING',
  `context_json` json DEFAULT NULL,
  `limitations_json` json DEFAULT NULL,
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `updated_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_media_context_item` (`dataset_item_id`),
  KEY `idx_media_context_task` (`task_id`,`processing_status`),
  CONSTRAINT `fk_media_context_item` FOREIGN KEY (`dataset_item_id`) REFERENCES `dataset_items` (`id`),
  CONSTRAINT `fk_media_context_task` FOREIGN KEY (`task_id`) REFERENCES `tasks` (`id`),
  CONSTRAINT `chk_media_context_status` CHECK ((`processing_status` in (_utf8mb4'PENDING',_utf8mb4'RUNNING',_utf8mb4'READY',_utf8mb4'PARTIAL',_utf8mb4'FAILED')))
) ENGINE=InnoDB AUTO_INCREMENT=775 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `dataset_items`
--

DROP TABLE IF EXISTS `dataset_items`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `dataset_items` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `task_id` bigint NOT NULL COMMENT '任务ID',
  `external_id` varchar(128) NOT NULL COMMENT '外部ID',
  `item_json` json NOT NULL COMMENT '数据条目JSON',
  `metadata_json` json DEFAULT NULL COMMENT '元数据JSON',
  `assigned_count` int NOT NULL DEFAULT '0' COMMENT '分配数量',
  `submitted_count` int NOT NULL DEFAULT '0' COMMENT '提交数量',
  `approved_count` int NOT NULL DEFAULT '0' COMMENT '通过数量',
  `deleted` tinyint(1) NOT NULL DEFAULT '0' COMMENT '删除状态',
  `active_external_id` varchar(128) GENERATED ALWAYS AS ((case when (`deleted` = 0) then `external_id` else NULL end)) STORED COMMENT '有效外部ID',
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  `updated_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_dataset_items_task_external` (`task_id`,`active_external_id`),
  KEY `idx_dataset_items_claim` (`task_id`,`deleted`,`assigned_count`),
  CONSTRAINT `fk_dataset_items_task` FOREIGN KEY (`task_id`) REFERENCES `tasks` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=930876 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='数据集数据条目表，存储数据集数据条目相关业务数据。';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `export_jobs`
--

DROP TABLE IF EXISTS `export_jobs`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `export_jobs` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `task_id` bigint NOT NULL COMMENT '任务ID',
  `requested_by` bigint NOT NULL COMMENT '请求人',
  `export_format` varchar(20) NOT NULL COMMENT '导出格式',
  `status` varchar(20) NOT NULL DEFAULT 'PENDING' COMMENT 'status状态',
  `include_ai_review` tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否AI审核',
  `include_audit_trail` tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否审计轨迹',
  `include_review_comment` tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否审核备注',
  `include_labeler_info` tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否标注员info',
  `field_mapping_json` json DEFAULT NULL COMMENT '字段映射JSON',
  `result_file_id` bigint DEFAULT NULL COMMENT '结果文件ID',
  `download_url` varchar(1000) DEFAULT NULL COMMENT '下载地址',
  `error_message` text COMMENT '错误信息',
  `started_at` datetime(3) DEFAULT NULL,
  `finished_at` datetime(3) DEFAULT NULL,
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`),
  KEY `idx_export_jobs_task` (`task_id`,`created_at`),
  KEY `idx_export_jobs_status` (`status`),
  KEY `fk_export_jobs_result_file` (`result_file_id`),
  KEY `idx_export_jobs_requested_by` (`requested_by`,`created_at`),
  CONSTRAINT `fk_export_jobs_requested_by` FOREIGN KEY (`requested_by`) REFERENCES `users` (`id`),
  CONSTRAINT `fk_export_jobs_result_file` FOREIGN KEY (`result_file_id`) REFERENCES `object_files` (`id`),
  CONSTRAINT `fk_export_jobs_task` FOREIGN KEY (`task_id`) REFERENCES `tasks` (`id`),
  CONSTRAINT `chk_export_jobs_format` CHECK ((`export_format` in (_utf8mb4'JSON',_utf8mb4'JSONL',_utf8mb4'CSV',_utf8mb4'EXCEL'))),
  CONSTRAINT `chk_export_jobs_status` CHECK ((`status` in (_utf8mb4'PENDING',_utf8mb4'RUNNING',_utf8mb4'SUCCESS',_utf8mb4'FAILED')))
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='导出作业表，存储导出作业相关业务数据。';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `flyway_schema_history`
--

DROP TABLE IF EXISTS `flyway_schema_history`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `flyway_schema_history` (
  `installed_rank` int NOT NULL,
  `version` varchar(50) DEFAULT NULL,
  `description` varchar(200) NOT NULL,
  `type` varchar(20) NOT NULL,
  `script` varchar(1000) NOT NULL,
  `checksum` int DEFAULT NULL,
  `installed_by` varchar(100) NOT NULL,
  `installed_on` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `execution_time` int NOT NULL,
  `success` tinyint(1) NOT NULL,
  PRIMARY KEY (`installed_rank`),
  KEY `flyway_schema_history_s_idx` (`success`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `labeler_contribution_stats`
--

DROP TABLE IF EXISTS `labeler_contribution_stats`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `labeler_contribution_stats` (
  `labeler_id` bigint NOT NULL COMMENT '标注员ID',
  `claimed_count` int NOT NULL DEFAULT '0' COMMENT '领取数量',
  `submitted_count` int NOT NULL DEFAULT '0' COMMENT '提交数量',
  `pending_review_count` int NOT NULL DEFAULT '0' COMMENT '待处理审核数量',
  `approved_count` int NOT NULL DEFAULT '0' COMMENT '通过数量',
  `rejected_count` int NOT NULL DEFAULT '0' COMMENT '拒绝数量',
  `total_reward` decimal(14,2) NOT NULL DEFAULT '0.00' COMMENT '总计奖励',
  `today_submitted_count` int NOT NULL DEFAULT '0' COMMENT 'today提交数量',
  `last_submit_date` date DEFAULT NULL COMMENT '最近submit时间',
  `updated_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
  PRIMARY KEY (`labeler_id`),
  CONSTRAINT `fk_contribution_stats_labeler` FOREIGN KEY (`labeler_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='标注员贡献统计表，存储标注员贡献统计相关业务数据。';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `labeler_daily_stats`
--

DROP TABLE IF EXISTS `labeler_daily_stats`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `labeler_daily_stats` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `labeler_id` bigint NOT NULL COMMENT '标注员ID',
  `stat_date` date NOT NULL COMMENT 'stat时间',
  `submitted_count` int NOT NULL DEFAULT '0' COMMENT '提交数量',
  `approved_count` int NOT NULL DEFAULT '0' COMMENT '通过数量',
  `rejected_count` int NOT NULL DEFAULT '0' COMMENT '拒绝数量',
  `reward_amount` decimal(14,2) NOT NULL DEFAULT '0.00' COMMENT '奖励金额',
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  `updated_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_labeler_daily_stats_date` (`labeler_id`,`stat_date`),
  KEY `idx_labeler_daily_stats_date` (`stat_date`),
  CONSTRAINT `fk_labeler_daily_stats_labeler` FOREIGN KEY (`labeler_id`) REFERENCES `users` (`id`),
  CONSTRAINT `chk_labeler_daily_stats_counts` CHECK (((`submitted_count` >= 0) and (`approved_count` >= 0) and (`rejected_count` >= 0)))
) ENGINE=InnoDB AUTO_INCREMENT=9 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='标注员每日统计表，存储标注员每日统计相关业务数据。';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `labeler_task_stats`
--

DROP TABLE IF EXISTS `labeler_task_stats`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `labeler_task_stats` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `labeler_id` bigint NOT NULL,
  `task_id` bigint NOT NULL,
  `claimed_count` int NOT NULL DEFAULT '0',
  `submitted_count` int NOT NULL DEFAULT '0',
  `approved_count` int NOT NULL DEFAULT '0',
  `rejected_count` int NOT NULL DEFAULT '0',
  `total_reward` decimal(14,2) NOT NULL DEFAULT '0.00',
  `updated_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_labeler_task_stats` (`labeler_id`,`task_id`),
  KEY `idx_labeler_task_stats_task` (`task_id`),
  CONSTRAINT `fk_labeler_task_stats_labeler` FOREIGN KEY (`labeler_id`) REFERENCES `users` (`id`),
  CONSTRAINT `fk_labeler_task_stats_task` FOREIGN KEY (`task_id`) REFERENCES `tasks` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `llm_providers`
--

DROP TABLE IF EXISTS `llm_providers`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `llm_providers` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `provider_code` varchar(64) NOT NULL COMMENT '服务提供商编码',
  `provider_name` varchar(100) NOT NULL COMMENT '服务提供商名称',
  `base_url` varchar(500) NOT NULL COMMENT 'base地址',
  `encrypted_api_key` text COMMENT 'encryptedapi键',
  `default_model` varchar(128) NOT NULL COMMENT '默认模型',
  `custom_headers_json` json DEFAULT NULL COMMENT '自定义请求头JSON',
  `enabled` tinyint(1) NOT NULL DEFAULT '1' COMMENT '启用状态',
  `platform_rate_limit_per_minute` int DEFAULT NULL COMMENT '平台每分钟限流次数',
  `task_rate_limit_per_minute` int DEFAULT NULL COMMENT '任务ratelimitperminute',
  `user_rate_limit_per_minute` int DEFAULT NULL COMMENT '用户ratelimitperminute',
  `created_by` bigint DEFAULT NULL COMMENT '创建人',
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  `updated_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
  `support_vision` tinyint(1) NOT NULL DEFAULT '0',
  `support_multi_image` tinyint(1) NOT NULL DEFAULT '0',
  `max_image_count` int NOT NULL DEFAULT '10',
  `vision_model` varchar(100) DEFAULT NULL,
  `structured_output_mode` varchar(20) NOT NULL DEFAULT 'NONE' COMMENT 'NONE, JSON_OBJECT, JSON_SCHEMA',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_llm_providers_code` (`provider_code`),
  KEY `idx_llm_providers_enabled` (`enabled`),
  KEY `fk_llm_providers_created_by` (`created_by`),
  CONSTRAINT `fk_llm_providers_created_by` FOREIGN KEY (`created_by`) REFERENCES `users` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=20 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='大模型服务提供商表，存储大模型服务提供商相关业务数据。';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `llm_trigger_runs`
--

DROP TABLE IF EXISTS `llm_trigger_runs`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `llm_trigger_runs` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `task_id` bigint NOT NULL,
  `assignment_id` bigint DEFAULT NULL,
  `template_version_id` bigint NOT NULL,
  `dataset_item_id` bigint DEFAULT NULL,
  `component_id` varchar(128) DEFAULT NULL,
  `provider_id` bigint NOT NULL,
  `model_name` varchar(128) NOT NULL,
  `agent_run_id` bigint DEFAULT NULL,
  `status` varchar(30) NOT NULL,
  `target_fields_json` json DEFAULT NULL,
  `input_snapshot_json` json DEFAULT NULL,
  `result_json` json DEFAULT NULL,
  `content_text` longtext,
  `latency_ms` bigint DEFAULT NULL,
  `error_code` varchar(100) DEFAULT NULL,
  `error_message` varchar(500) DEFAULT NULL,
  `created_by` bigint NOT NULL,
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `updated_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`),
  KEY `idx_llm_trigger_runs_task` (`task_id`,`created_at`),
  KEY `idx_llm_trigger_runs_assignment` (`assignment_id`,`created_at`),
  KEY `idx_llm_trigger_runs_agent_run` (`agent_run_id`),
  KEY `fk_llm_trigger_runs_template_version` (`template_version_id`),
  KEY `fk_llm_trigger_runs_item` (`dataset_item_id`),
  KEY `fk_llm_trigger_runs_provider` (`provider_id`),
  KEY `fk_llm_trigger_runs_created_by` (`created_by`),
  CONSTRAINT `fk_llm_trigger_runs_agent_run` FOREIGN KEY (`agent_run_id`) REFERENCES `agent_runs` (`id`),
  CONSTRAINT `fk_llm_trigger_runs_assignment` FOREIGN KEY (`assignment_id`) REFERENCES `assignments` (`id`),
  CONSTRAINT `fk_llm_trigger_runs_created_by` FOREIGN KEY (`created_by`) REFERENCES `users` (`id`),
  CONSTRAINT `fk_llm_trigger_runs_item` FOREIGN KEY (`dataset_item_id`) REFERENCES `dataset_items` (`id`),
  CONSTRAINT `fk_llm_trigger_runs_provider` FOREIGN KEY (`provider_id`) REFERENCES `llm_providers` (`id`),
  CONSTRAINT `fk_llm_trigger_runs_task` FOREIGN KEY (`task_id`) REFERENCES `tasks` (`id`),
  CONSTRAINT `fk_llm_trigger_runs_template_version` FOREIGN KEY (`template_version_id`) REFERENCES `template_versions` (`id`),
  CONSTRAINT `chk_llm_trigger_runs_status` CHECK ((`status` in (_utf8mb4'PENDING',_utf8mb4'RUNNING',_utf8mb4'SUCCESS',_utf8mb4'FAILED',_utf8mb4'RATE_LIMITED',_utf8mb4'MANUAL_REQUIRED')))
) ENGINE=InnoDB AUTO_INCREMENT=6 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='Asynchronous LLM trigger execution records.';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `llm_usage_records`
--

DROP TABLE IF EXISTS `llm_usage_records`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `llm_usage_records` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `provider_id` bigint DEFAULT NULL,
  `model_name` varchar(128) DEFAULT NULL,
  `biz_type` varchar(64) NOT NULL,
  `status` varchar(32) NOT NULL,
  `latency_ms` bigint DEFAULT NULL,
  `prompt_tokens` bigint DEFAULT NULL,
  `completion_tokens` bigint DEFAULT NULL,
  `total_tokens` bigint DEFAULT NULL,
  `usage_available` tinyint(1) NOT NULL DEFAULT '0',
  `error_code` varchar(128) DEFAULT NULL,
  `trace_id` varchar(128) DEFAULT NULL,
  `agent_run_id` bigint DEFAULT NULL,
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`),
  KEY `idx_llm_usage_provider_model_created` (`provider_id`,`model_name`,`created_at`),
  KEY `idx_llm_usage_biz_created` (`biz_type`,`created_at`),
  KEY `idx_llm_usage_trace` (`trace_id`),
  KEY `fk_llm_usage_agent_run` (`agent_run_id`),
  CONSTRAINT `fk_llm_usage_agent_run` FOREIGN KEY (`agent_run_id`) REFERENCES `agent_runs` (`id`),
  CONSTRAINT `fk_llm_usage_provider` FOREIGN KEY (`provider_id`) REFERENCES `llm_providers` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=23 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='Persistent exact LLM usage records parsed from provider usage payloads.';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `media_assets`
--

DROP TABLE IF EXISTS `media_assets`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `media_assets` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `dataset_item_id` bigint NOT NULL,
  `task_id` bigint NOT NULL,
  `source_file_id` bigint DEFAULT NULL,
  `media_type` varchar(24) NOT NULL,
  `content_type` varchar(128) DEFAULT NULL,
  `file_size` bigint DEFAULT NULL,
  `checksum` varchar(128) DEFAULT NULL,
  `status` varchar(24) NOT NULL DEFAULT 'PENDING',
  `metadata_json` json DEFAULT NULL,
  `limitations_json` json DEFAULT NULL,
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `updated_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`),
  KEY `idx_media_assets_item` (`dataset_item_id`,`status`),
  KEY `idx_media_assets_task` (`task_id`,`media_type`),
  KEY `fk_media_assets_file` (`source_file_id`),
  CONSTRAINT `fk_media_assets_file` FOREIGN KEY (`source_file_id`) REFERENCES `object_files` (`id`),
  CONSTRAINT `fk_media_assets_item` FOREIGN KEY (`dataset_item_id`) REFERENCES `dataset_items` (`id`),
  CONSTRAINT `fk_media_assets_task` FOREIGN KEY (`task_id`) REFERENCES `tasks` (`id`),
  CONSTRAINT `chk_media_assets_status` CHECK ((`status` in (_utf8mb4'PENDING',_utf8mb4'RUNNING',_utf8mb4'READY',_utf8mb4'PARTIAL',_utf8mb4'FAILED')))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `media_derivatives`
--

DROP TABLE IF EXISTS `media_derivatives`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `media_derivatives` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `asset_id` bigint NOT NULL,
  `derivative_type` varchar(32) NOT NULL,
  `sequence_no` int NOT NULL DEFAULT '0',
  `source_file_id` bigint DEFAULT NULL,
  `url` varchar(1000) DEFAULT NULL,
  `text_json` json DEFAULT NULL,
  `metadata_json` json DEFAULT NULL,
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`),
  KEY `idx_media_derivatives_asset` (`asset_id`,`derivative_type`,`sequence_no`),
  KEY `fk_media_derivatives_file` (`source_file_id`),
  CONSTRAINT `fk_media_derivatives_asset` FOREIGN KEY (`asset_id`) REFERENCES `media_assets` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_media_derivatives_file` FOREIGN KEY (`source_file_id`) REFERENCES `object_files` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `media_processing_jobs`
--

DROP TABLE IF EXISTS `media_processing_jobs`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `media_processing_jobs` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `dataset_item_id` bigint NOT NULL,
  `task_id` bigint NOT NULL,
  `status` varchar(24) NOT NULL DEFAULT 'PENDING',
  `total_assets` int NOT NULL DEFAULT '0',
  `processed_assets` int NOT NULL DEFAULT '0',
  `error_message` text,
  `created_by` bigint DEFAULT NULL,
  `started_at` datetime(3) DEFAULT NULL,
  `finished_at` datetime(3) DEFAULT NULL,
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `updated_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`),
  KEY `idx_media_jobs_item` (`dataset_item_id`,`created_at`),
  KEY `idx_media_jobs_task_status` (`task_id`,`status`),
  KEY `fk_media_jobs_created_by` (`created_by`),
  CONSTRAINT `fk_media_jobs_created_by` FOREIGN KEY (`created_by`) REFERENCES `users` (`id`),
  CONSTRAINT `fk_media_jobs_item` FOREIGN KEY (`dataset_item_id`) REFERENCES `dataset_items` (`id`),
  CONSTRAINT `fk_media_jobs_task` FOREIGN KEY (`task_id`) REFERENCES `tasks` (`id`),
  CONSTRAINT `chk_media_jobs_status` CHECK ((`status` in (_utf8mb4'PENDING',_utf8mb4'RUNNING',_utf8mb4'READY',_utf8mb4'PARTIAL',_utf8mb4'FAILED')))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `object_files`
--

DROP TABLE IF EXISTS `object_files`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `object_files` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `owner_id` bigint DEFAULT NULL COMMENT '拥有者ID',
  `bucket_name` varchar(128) NOT NULL COMMENT '存储桶名称',
  `object_key` varchar(512) NOT NULL COMMENT '对象键',
  `original_filename` varchar(255) NOT NULL COMMENT '原始文件名',
  `content_type` varchar(128) DEFAULT NULL COMMENT '内容类型',
  `file_size` bigint NOT NULL DEFAULT '0' COMMENT '文件大小',
  `checksum` varchar(128) DEFAULT NULL COMMENT '校验值',
  `storage_provider` varchar(32) NOT NULL DEFAULT 'MINIO' COMMENT '存储服务提供商',
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_object_files_object` (`bucket_name`,`object_key`),
  KEY `idx_object_files_owner` (`owner_id`),
  CONSTRAINT `fk_object_files_owner` FOREIGN KEY (`owner_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=900443 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='对象文件表，存储对象文件相关业务数据。';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `pre_annotations`
--

DROP TABLE IF EXISTS `pre_annotations`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `pre_annotations` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `assignment_id` bigint NOT NULL,
  `task_id` bigint NOT NULL,
  `dataset_item_id` bigint NOT NULL,
  `labeler_id` bigint NOT NULL,
  `agent_run_id` bigint DEFAULT NULL,
  `status` varchar(30) NOT NULL,
  `suggested_answer_json` json DEFAULT NULL,
  `field_suggestions` json DEFAULT NULL,
  `risk_flags` json DEFAULT NULL,
  `overall_confidence` decimal(5,2) DEFAULT NULL,
  `limitations` json DEFAULT NULL,
  `prompt_mode` varchar(40) DEFAULT NULL,
  `degraded` tinyint(1) NOT NULL DEFAULT '0',
  `ignored_fields_json` json DEFAULT NULL,
  `media_understanding_json` json DEFAULT NULL,
  `raw_response` longtext,
  `error_code` varchar(100) DEFAULT NULL,
  `error_message` varchar(500) DEFAULT NULL,
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `updated_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`),
  KEY `idx_pre_annotations_assignment` (`assignment_id`,`created_at`),
  KEY `idx_pre_annotations_agent_run` (`agent_run_id`),
  KEY `fk_pre_annotations_task` (`task_id`),
  KEY `fk_pre_annotations_item` (`dataset_item_id`),
  KEY `fk_pre_annotations_labeler` (`labeler_id`),
  CONSTRAINT `fk_pre_annotations_agent_run` FOREIGN KEY (`agent_run_id`) REFERENCES `agent_runs` (`id`),
  CONSTRAINT `fk_pre_annotations_assignment` FOREIGN KEY (`assignment_id`) REFERENCES `assignments` (`id`),
  CONSTRAINT `fk_pre_annotations_item` FOREIGN KEY (`dataset_item_id`) REFERENCES `dataset_items` (`id`),
  CONSTRAINT `fk_pre_annotations_labeler` FOREIGN KEY (`labeler_id`) REFERENCES `users` (`id`),
  CONSTRAINT `fk_pre_annotations_task` FOREIGN KEY (`task_id`) REFERENCES `tasks` (`id`),
  CONSTRAINT `chk_pre_annotations_status` CHECK ((`status` in (_utf8mb4'PENDING',_utf8mb4'RUNNING',_utf8mb4'SUCCESS',_utf8mb4'FAILED',_utf8mb4'RATE_LIMITED',_utf8mb4'MANUAL_REQUIRED')))
) ENGINE=InnoDB AUTO_INCREMENT=14 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='Immutable AI pre-annotation suggestions for labeler assignments.';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `review_records`
--

DROP TABLE IF EXISTS `review_records`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `review_records` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `submission_id` bigint NOT NULL,
  `review_task_id` bigint DEFAULT NULL COMMENT 'Review queue item this action belongs to; nullable for legacy/direct audit actions.',
  `reviewer_id` bigint NOT NULL,
  `target_reviewer_id` bigint DEFAULT NULL COMMENT 'Reviewer assigned by ASSIGN_REVIEWER action; reviewer_id is the actor who performed the assignment.',
  `action` varchar(30) NOT NULL,
  `review_level` int NOT NULL DEFAULT '1',
  `comment` text,
  `reason` text,
  `before_status` varchar(20) DEFAULT NULL,
  `after_status` varchar(20) DEFAULT NULL,
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`),
  KEY `idx_review_records_submission` (`submission_id`,`created_at`),
  KEY `idx_review_records_reviewer` (`reviewer_id`,`created_at`),
  KEY `idx_review_records_target_reviewer` (`target_reviewer_id`,`created_at`),
  KEY `fk_review_records_review_task` (`review_task_id`),
  CONSTRAINT `fk_review_records_review_task` FOREIGN KEY (`review_task_id`) REFERENCES `review_tasks` (`id`),
  CONSTRAINT `fk_review_records_reviewer` FOREIGN KEY (`reviewer_id`) REFERENCES `users` (`id`),
  CONSTRAINT `fk_review_records_submission` FOREIGN KEY (`submission_id`) REFERENCES `submissions` (`id`),
  CONSTRAINT `fk_review_records_target_reviewer` FOREIGN KEY (`target_reviewer_id`) REFERENCES `users` (`id`),
  CONSTRAINT `chk_review_records_action` CHECK ((`action` in (_utf8mb4'APPROVE',_utf8mb4'REJECT',_utf8mb4'AI_DIRECT_REJECT',_utf8mb4'RESOLVE_CONFLICT',_utf8mb4'MARK_MANUAL_REQUIRED',_utf8mb4'ASSIGN_REVIEWER')))
) ENGINE=InnoDB AUTO_INCREMENT=7 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='Append-only human review actions and assignment audit records.';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `review_task_claims`
--

DROP TABLE IF EXISTS `review_task_claims`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `review_task_claims` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `task_id` bigint NOT NULL,
  `review_level` int NOT NULL DEFAULT '1',
  `reviewer_id` bigint NOT NULL,
  `claimed_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_review_task_claim` (`task_id`,`review_level`),
  KEY `idx_review_task_claims_reviewer` (`reviewer_id`)
) ENGINE=InnoDB AUTO_INCREMENT=8 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='Reviewer whole-task claim per (task, review_level); exclusive.';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `review_tasks`
--

DROP TABLE IF EXISTS `review_tasks`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `review_tasks` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `submission_id` bigint NOT NULL,
  `task_id` bigint NOT NULL,
  `review_level` int NOT NULL DEFAULT '1',
  `assigned_reviewer_id` bigint NOT NULL,
  `assigned_by` bigint DEFAULT NULL,
  `status` varchar(24) NOT NULL DEFAULT 'PENDING',
  `review_version` int NOT NULL DEFAULT '1' COMMENT 'Optimistic lock version for concurrent reviewer actions.',
  `assigned_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `started_at` datetime(3) DEFAULT NULL,
  `completed_at` datetime(3) DEFAULT NULL,
  `due_at` datetime(3) DEFAULT NULL,
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `updated_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_review_tasks_submission_level` (`submission_id`,`review_level`),
  KEY `idx_review_tasks_reviewer_status` (`assigned_reviewer_id`,`status`,`due_at`),
  KEY `idx_review_tasks_task_level` (`task_id`,`review_level`,`status`),
  KEY `fk_review_tasks_assigned_by` (`assigned_by`),
  CONSTRAINT `fk_review_tasks_assigned_by` FOREIGN KEY (`assigned_by`) REFERENCES `users` (`id`),
  CONSTRAINT `fk_review_tasks_assigned_reviewer` FOREIGN KEY (`assigned_reviewer_id`) REFERENCES `users` (`id`),
  CONSTRAINT `fk_review_tasks_submission` FOREIGN KEY (`submission_id`) REFERENCES `submissions` (`id`),
  CONSTRAINT `fk_review_tasks_task` FOREIGN KEY (`task_id`) REFERENCES `tasks` (`id`),
  CONSTRAINT `chk_review_tasks_level` CHECK ((`review_level` between 1 and 3)),
  CONSTRAINT `chk_review_tasks_status` CHECK ((`status` in (_utf8mb4'PENDING',_utf8mb4'IN_REVIEW',_utf8mb4'APPROVED',_utf8mb4'REJECTED',_utf8mb4'TRANSFERRED',_utf8mb4'CANCELLED'))),
  CONSTRAINT `chk_review_tasks_version` CHECK ((`review_version` >= 1))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='Reviewer work queue for batch assignment and level 1/2/3 review flow.';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `reward_ledger`
--

DROP TABLE IF EXISTS `reward_ledger`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `reward_ledger` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `task_id` bigint NOT NULL,
  `labeler_id` bigint NOT NULL,
  `submission_id` bigint NOT NULL,
  `assignment_id` bigint NOT NULL,
  `amount` decimal(12,2) NOT NULL,
  `direction` varchar(16) NOT NULL,
  `reason` varchar(255) NOT NULL,
  `source_event_id` varchar(128) NOT NULL,
  `reward_type` varchar(32) NOT NULL DEFAULT 'SUBMISSION_APPROVED',
  `positive_submission_id` bigint GENERATED ALWAYS AS ((case when (`direction` = _utf8mb4'CREDIT') then `submission_id` else NULL end)) STORED COMMENT 'Idempotency key: one positive reward per submission.',
  `positive_assignment_id` bigint GENERATED ALWAYS AS ((case when (`direction` = _utf8mb4'CREDIT') then `assignment_id` else NULL end)) STORED COMMENT 'Idempotency key: one effective positive reward per assignment across versions.',
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_reward_ledger_event` (`source_event_id`),
  UNIQUE KEY `uk_reward_ledger_positive_submission` (`positive_submission_id`),
  UNIQUE KEY `uk_reward_ledger_positive_assignment` (`positive_assignment_id`),
  KEY `idx_reward_ledger_labeler` (`labeler_id`,`created_at`),
  KEY `idx_reward_ledger_task` (`task_id`,`created_at`),
  KEY `fk_reward_ledger_submission` (`submission_id`),
  KEY `fk_reward_ledger_assignment` (`assignment_id`),
  CONSTRAINT `fk_reward_ledger_assignment` FOREIGN KEY (`assignment_id`) REFERENCES `assignments` (`id`),
  CONSTRAINT `fk_reward_ledger_labeler` FOREIGN KEY (`labeler_id`) REFERENCES `users` (`id`),
  CONSTRAINT `fk_reward_ledger_submission` FOREIGN KEY (`submission_id`) REFERENCES `submissions` (`id`),
  CONSTRAINT `fk_reward_ledger_task` FOREIGN KEY (`task_id`) REFERENCES `tasks` (`id`),
  CONSTRAINT `chk_reward_ledger_amount` CHECK ((`amount` >= 0)),
  CONSTRAINT `chk_reward_ledger_direction` CHECK ((`direction` in (_utf8mb4'CREDIT',_utf8mb4'DEBIT'))),
  CONSTRAINT `chk_reward_ledger_type` CHECK ((`reward_type` in (_utf8mb4'SUBMISSION_APPROVED',_utf8mb4'GOLDEN_SELECTED',_utf8mb4'REWARD_REVERSED')))
) ENGINE=InnoDB AUTO_INCREMENT=970102 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='Append-only virtual reward ledger; reversals are negative rows, not updates.';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `reward_rules`
--

DROP TABLE IF EXISTS `reward_rules`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `reward_rules` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `task_id` bigint NOT NULL,
  `effective_version` int NOT NULL COMMENT 'Reward rule version effective for newly approved data; historical ledgers are not rewritten.',
  `reward_mode` varchar(32) NOT NULL DEFAULT 'APPROVED_ITEM',
  `unit_reward` decimal(12,2) NOT NULL DEFAULT '0.00',
  `reward_currency` varchar(32) NOT NULL DEFAULT 'POINT',
  `reward_visible` tinyint(1) NOT NULL DEFAULT '1',
  `effective_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `created_by` bigint NOT NULL,
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_reward_rules_task_version` (`task_id`,`effective_version`),
  KEY `idx_reward_rules_task` (`task_id`),
  KEY `fk_reward_rules_created_by` (`created_by`),
  CONSTRAINT `fk_reward_rules_created_by` FOREIGN KEY (`created_by`) REFERENCES `users` (`id`),
  CONSTRAINT `fk_reward_rules_task` FOREIGN KEY (`task_id`) REFERENCES `tasks` (`id`),
  CONSTRAINT `chk_reward_rules_effective_version` CHECK ((`effective_version` >= 1)),
  CONSTRAINT `chk_reward_rules_mode` CHECK ((`reward_mode` = _utf8mb4'APPROVED_ITEM')),
  CONSTRAINT `chk_reward_rules_unit_reward` CHECK ((`unit_reward` >= 0))
) ENGINE=InnoDB AUTO_INCREMENT=960094 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='Versioned virtual reward configuration for a task.';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `submissions`
--

DROP TABLE IF EXISTS `submissions`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `submissions` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `assignment_id` bigint NOT NULL,
  `task_id` bigint NOT NULL,
  `dataset_item_id` bigint NOT NULL,
  `labeler_id` bigint NOT NULL,
  `created_by` bigint DEFAULT NULL,
  `template_version_id` bigint NOT NULL,
  `version_no` int NOT NULL,
  `answer_json` json NOT NULL,
  `answer_hash` char(64) NOT NULL,
  `status` varchar(20) NOT NULL,
  `conflict_status` varchar(24) NOT NULL DEFAULT 'NONE',
  `current_review_level` int NOT NULL DEFAULT '1' COMMENT 'Current human review stage: 1 initial review, 2 second review, 3 final review.',
  `review_flow_status` varchar(24) NOT NULL DEFAULT 'UNASSIGNED' COMMENT 'Human review queue state, separate from submission business status.',
  `assigned_reviewer_id` bigint DEFAULT NULL COMMENT 'Current reviewer owner for "assigned to me" filtering; history is stored in review_tasks/review_records.',
  `review_version` int NOT NULL DEFAULT '1' COMMENT 'Optimistic lock version for reviewer state transitions.',
  `is_golden` tinyint(1) NOT NULL DEFAULT '0',
  `golden_dataset_item_id` bigint GENERATED ALWAYS AS ((case when (`is_golden` = 1) then `dataset_item_id` else NULL end)) STORED,
  `submitted_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `updated_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_submissions_assignment_version` (`assignment_id`,`version_no`),
  UNIQUE KEY `uk_submissions_golden_item` (`golden_dataset_item_id`),
  KEY `idx_submissions_review` (`status`,`conflict_status`,`submitted_at`),
  KEY `idx_submissions_assigned_reviewer` (`assigned_reviewer_id`,`review_flow_status`,`status`),
  KEY `idx_submissions_task_item` (`task_id`,`dataset_item_id`),
  KEY `idx_submissions_labeler` (`labeler_id`,`status`),
  KEY `fk_submissions_item` (`dataset_item_id`),
  KEY `fk_submissions_template_version` (`template_version_id`),
  KEY `idx_submissions_task_review` (`task_id`,`status`,`conflict_status`,`submitted_at`),
  KEY `idx_submissions_export` (`task_id`,`status`,`is_golden`,`submitted_at`),
  KEY `idx_submissions_labeler_time` (`labeler_id`,`submitted_at`),
  KEY `idx_submissions_review_claim` (`status`,`assigned_reviewer_id`,`submitted_at`),
  KEY `idx_submissions_task_review_claim` (`task_id`,`status`,`assigned_reviewer_id`,`submitted_at`),
  CONSTRAINT `fk_submissions_assigned_reviewer` FOREIGN KEY (`assigned_reviewer_id`) REFERENCES `users` (`id`),
  CONSTRAINT `fk_submissions_assignment` FOREIGN KEY (`assignment_id`) REFERENCES `assignments` (`id`),
  CONSTRAINT `fk_submissions_item` FOREIGN KEY (`dataset_item_id`) REFERENCES `dataset_items` (`id`),
  CONSTRAINT `fk_submissions_labeler` FOREIGN KEY (`labeler_id`) REFERENCES `users` (`id`),
  CONSTRAINT `fk_submissions_task` FOREIGN KEY (`task_id`) REFERENCES `tasks` (`id`),
  CONSTRAINT `fk_submissions_template_version` FOREIGN KEY (`template_version_id`) REFERENCES `template_versions` (`id`),
  CONSTRAINT `chk_submissions_conflict_status` CHECK ((`conflict_status` in (_utf8mb4'NONE',_utf8mb4'CONSENSUS_REACHED',_utf8mb4'CONFLICTED',_utf8mb4'RESOLVED'))),
  CONSTRAINT `chk_submissions_review_flow` CHECK ((`review_flow_status` in (_utf8mb4'UNASSIGNED',_utf8mb4'ASSIGNED',_utf8mb4'IN_REVIEW',_utf8mb4'LEVEL_APPROVED',_utf8mb4'FINAL_APPROVED',_utf8mb4'REJECTED',_utf8mb4'CANCELLED'))),
  CONSTRAINT `chk_submissions_review_level` CHECK ((`current_review_level` between 1 and 3)),
  CONSTRAINT `chk_submissions_review_version` CHECK ((`review_version` >= 1)),
  CONSTRAINT `chk_submissions_status` CHECK ((`status` in (_utf8mb4'SUBMITTED',_utf8mb4'AI_REVIEWING',_utf8mb4'PENDING_FINAL',_utf8mb4'APPROVED',_utf8mb4'REJECTED',_utf8mb4'SUPERSEDED')))
) ENGINE=InnoDB AUTO_INCREMENT=950148 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='Immutable submission versions and current review routing state.';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `task_reviewers`
--

DROP TABLE IF EXISTS `task_reviewers`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `task_reviewers` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `task_id` bigint NOT NULL,
  `reviewer_id` bigint NOT NULL,
  `assigned_by` bigint NOT NULL,
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_task_reviewer` (`task_id`,`reviewer_id`),
  KEY `idx_task_reviewers_reviewer` (`reviewer_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `task_stats`
--

DROP TABLE IF EXISTS `task_stats`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `task_stats` (
  `task_id` bigint NOT NULL,
  `item_count` int NOT NULL DEFAULT '0',
  `assigned_count` int NOT NULL DEFAULT '0',
  `submitted_count` int NOT NULL DEFAULT '0',
  `pending_review_count` int NOT NULL DEFAULT '0',
  `approved_count` int NOT NULL DEFAULT '0',
  `rejected_count` int NOT NULL DEFAULT '0',
  `conflict_count` int NOT NULL DEFAULT '0',
  `updated_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`task_id`),
  CONSTRAINT `fk_task_stats_task` FOREIGN KEY (`task_id`) REFERENCES `tasks` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `task_tags`
--

DROP TABLE IF EXISTS `task_tags`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `task_tags` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `task_id` bigint NOT NULL,
  `tag_name` varchar(64) NOT NULL,
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_task_tags_task_tag` (`task_id`,`tag_name`),
  KEY `idx_task_tags_tag_task` (`tag_name`,`task_id`),
  CONSTRAINT `fk_task_tags_task` FOREIGN KEY (`task_id`) REFERENCES `tasks` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=227 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `task_versions`
--

DROP TABLE IF EXISTS `task_versions`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `task_versions` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `task_id` bigint NOT NULL,
  `version_no` int NOT NULL,
  `snapshot_json` json NOT NULL,
  `created_by` bigint NOT NULL,
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_task_versions_task_version` (`task_id`,`version_no`),
  KEY `idx_task_versions_task_created` (`task_id`,`created_at`),
  KEY `fk_task_versions_created_by` (`created_by`),
  CONSTRAINT `fk_task_versions_created_by` FOREIGN KEY (`created_by`) REFERENCES `users` (`id`),
  CONSTRAINT `fk_task_versions_task` FOREIGN KEY (`task_id`) REFERENCES `tasks` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=42 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='Append-only task draft snapshots for rollback.';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `tasks`
--

DROP TABLE IF EXISTS `tasks`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `tasks` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `owner_id` bigint NOT NULL COMMENT '拥有者ID',
  `title` varchar(200) NOT NULL COMMENT '标题',
  `description` text COMMENT '描述',
  `instruction_rich_text` mediumtext COMMENT '说明富文本文本',
  `status` varchar(20) NOT NULL DEFAULT 'DRAFT' COMMENT 'status状态',
  `quota` int NOT NULL COMMENT '配额',
  `claimed_count` int NOT NULL DEFAULT '0' COMMENT '领取数量',
  `overlap_count` int NOT NULL DEFAULT '1' COMMENT '重叠数量',
  `deadline_at` datetime(3) NOT NULL COMMENT '截止时间',
  `published_template_version_id` bigint DEFAULT NULL COMMENT '发布模板版本ID',
  `ai_review_config_id` bigint DEFAULT NULL COMMENT 'AI审核配置ID',
  `review_level_count` int DEFAULT '1',
  `reward_visible` tinyint(1) NOT NULL DEFAULT '1' COMMENT '是否奖励可见',
  `published_at` datetime(3) DEFAULT NULL COMMENT '发布时间',
  `ended_at` datetime(3) DEFAULT NULL COMMENT '结束时间',
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  `updated_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
  `strategy` varchar(20) NOT NULL DEFAULT 'FCFS' COMMENT '领取策略: FCFS/QUOTA_GRAB/ASSIGNED',
  `max_claims_per_labeler` int DEFAULT NULL COMMENT '单人并发未完成上限(仅 QUOTA_GRAB 有效)',
  `assigned_labeler_id` bigint DEFAULT NULL COMMENT '指派策略下的被指派人id',
  PRIMARY KEY (`id`),
  KEY `idx_tasks_owner_status` (`owner_id`,`status`),
  KEY `idx_tasks_status_deadline` (`status`,`deadline_at`),
  KEY `idx_tasks_template_version` (`published_template_version_id`),
  KEY `idx_tasks_ai_review_config` (`ai_review_config_id`),
  KEY `idx_tasks_assigned_labeler` (`assigned_labeler_id`),
  FULLTEXT KEY `ft_tasks_search` (`title`,`description`),
  CONSTRAINT `fk_tasks_ai_review_config` FOREIGN KEY (`ai_review_config_id`) REFERENCES `ai_review_configs` (`id`),
  CONSTRAINT `fk_tasks_assigned_labeler` FOREIGN KEY (`assigned_labeler_id`) REFERENCES `users` (`id`),
  CONSTRAINT `fk_tasks_owner` FOREIGN KEY (`owner_id`) REFERENCES `users` (`id`),
  CONSTRAINT `fk_tasks_published_template_version` FOREIGN KEY (`published_template_version_id`) REFERENCES `template_versions` (`id`),
  CONSTRAINT `chk_tasks_overlap` CHECK ((`overlap_count` >= 1)),
  CONSTRAINT `chk_tasks_quota` CHECK ((`quota` >= 0)),
  CONSTRAINT `chk_tasks_status` CHECK ((`status` in (_utf8mb4'DRAFT',_utf8mb4'PUBLISHED',_utf8mb4'PAUSED',_utf8mb4'ENDED')))
) ENGINE=InnoDB AUTO_INCREMENT=910306 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='标注任务表，存储任务生命周期、配额和配置。';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `template_versions`
--

DROP TABLE IF EXISTS `template_versions`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `template_versions` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `template_id` bigint NOT NULL,
  `task_id` bigint DEFAULT NULL,
  `owner_id` bigint NOT NULL,
  `version_no` int NOT NULL,
  `schema_json` json NOT NULL COMMENT 'Frozen renderer schema: layout, components, ShowItem bindings, LlmTrigger config, and validation rules.',
  `published_snapshot` tinyint(1) NOT NULL DEFAULT '0',
  `change_note` varchar(500) DEFAULT NULL,
  `created_by` bigint NOT NULL,
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_template_versions_template_version` (`template_id`,`version_no`),
  KEY `idx_template_versions_task` (`task_id`),
  KEY `fk_template_versions_created_by` (`created_by`),
  KEY `idx_template_versions_owner` (`owner_id`),
  CONSTRAINT `fk_template_versions_created_by` FOREIGN KEY (`created_by`) REFERENCES `users` (`id`),
  CONSTRAINT `fk_template_versions_owner` FOREIGN KEY (`owner_id`) REFERENCES `users` (`id`),
  CONSTRAINT `fk_template_versions_template` FOREIGN KEY (`template_id`) REFERENCES `templates` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=920083 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='Immutable template schema versions used by owner preview, labeler rendering, and backend validation.';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `templates`
--

DROP TABLE IF EXISTS `templates`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `templates` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `task_id` bigint DEFAULT NULL,
  `owner_id` bigint NOT NULL,
  `name` varchar(200) NOT NULL,
  `current_version_no` int NOT NULL DEFAULT '0',
  `created_by` bigint NOT NULL,
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `updated_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`),
  KEY `idx_templates_task` (`task_id`),
  KEY `fk_templates_created_by` (`created_by`),
  KEY `idx_templates_owner` (`owner_id`),
  CONSTRAINT `fk_templates_created_by` FOREIGN KEY (`created_by`) REFERENCES `users` (`id`),
  CONSTRAINT `fk_templates_owner` FOREIGN KEY (`owner_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=920060 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `user_roles`
--

DROP TABLE IF EXISTS `user_roles`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `user_roles` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `role_code` varchar(32) NOT NULL,
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_roles_user_role` (`user_id`,`role_code`),
  KEY `idx_user_roles_role` (`role_code`),
  CONSTRAINT `fk_user_roles_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`),
  CONSTRAINT `chk_user_roles_code` CHECK ((`role_code` in (_utf8mb4'ADMIN',_utf8mb4'OWNER',_utf8mb4'LABELER',_utf8mb4'REVIEWER',_utf8mb4'SYSTEM_AGENT')))
) ENGINE=InnoDB AUTO_INCREMENT=296 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `users`
--

DROP TABLE IF EXISTS `users`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `users` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `username` varchar(64) NOT NULL COMMENT 'username：标识',
  `email` varchar(255) NOT NULL COMMENT 'email：标识',
  `password_hash` varchar(255) DEFAULT NULL COMMENT '密码哈希',
  `user_type` varchar(20) NOT NULL DEFAULT 'USER' COMMENT '用户类型',
  `login_enabled` tinyint(1) NOT NULL DEFAULT '1' COMMENT '是否登录启用状态',
  `enabled` tinyint(1) NOT NULL DEFAULT '1' COMMENT '启用状态',
  `token_version` int NOT NULL DEFAULT '1' COMMENT '令牌版本',
  `display_name` varchar(100) DEFAULT NULL COMMENT '展示名称',
  `avatar_url` varchar(500) DEFAULT NULL COMMENT '头像地址',
  `last_login_at` datetime(3) DEFAULT NULL COMMENT '最近登录时间',
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  `updated_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_users_username` (`username`),
  UNIQUE KEY `uk_users_email` (`email`),
  KEY `idx_users_enabled` (`enabled`),
  CONSTRAINT `chk_users_user_type` CHECK ((`user_type` in (_utf8mb4'USER',_utf8mb4'SYSTEM')))
) ENGINE=InnoDB AUTO_INCREMENT=900211 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户账号表，存储登录身份和基础资料。';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2026-06-09 18:08:50
