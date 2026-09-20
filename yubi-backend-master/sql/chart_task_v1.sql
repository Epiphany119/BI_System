-- V1.0 智能分析任务模块
-- 执行前请确认已切换到 yubi 数据库。

CREATE TABLE IF NOT EXISTS chart_task (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  task_no VARCHAR(32) NOT NULL COMMENT '业务任务号',
  user_id BIGINT NOT NULL COMMENT '创建人',
  dataset_name VARCHAR(255) NOT NULL COMMENT '原始文件名',
  dataset_path VARCHAR(500) NOT NULL COMMENT '内部文件地址',
  dataset_size BIGINT NOT NULL COMMENT '文件字节数',
  goal VARCHAR(500) NOT NULL COMMENT '分析目标',
  chart_name VARCHAR(100) DEFAULT NULL COMMENT '图表名称',
  chart_type VARCHAR(30) DEFAULT NULL COMMENT '图表类型',
  mode VARCHAR(20) NOT NULL COMMENT 'SYNC/ASYNC',
  status VARCHAR(20) NOT NULL COMMENT 'CREATED/WAITING/RUNNING/SUCCEEDED/FAILED/CANCELED',
  execution_stage VARCHAR(30) DEFAULT NULL COMMENT 'FILE_PARSING/AI_CALLING/SAVING_RESULT',
  retry_count INT NOT NULL DEFAULT 0 COMMENT '异步重试次数',
  reconnect_count INT NOT NULL DEFAULT 0 COMMENT '同步重连次数',
  idempotency_key VARCHAR(64) NOT NULL COMMENT '幂等键',
  error_code VARCHAR(50) DEFAULT NULL COMMENT '错误码',
  error_message VARCHAR(1000) DEFAULT NULL COMMENT '脱敏错误信息',
  started_at DATETIME DEFAULT NULL,
  finished_at DATETIME DEFAULT NULL,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  is_delete TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  UNIQUE KEY uk_task_no (task_no),
  UNIQUE KEY uk_user_idempotency (user_id, idempotency_key),
  KEY idx_user_status (user_id, status),
  KEY idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='智能分析任务';

CREATE TABLE IF NOT EXISTS chart_result (
  id BIGINT NOT NULL AUTO_INCREMENT,
  task_id BIGINT NOT NULL,
  result_version INT NOT NULL DEFAULT 1,
  gen_chart LONGTEXT NOT NULL COMMENT 'ECharts JSON',
  gen_result TEXT NOT NULL COMMENT '分析结论',
  raw_response TEXT DEFAULT NULL COMMENT '脱敏后的模型原始响应',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_task_version (task_id, result_version),
  KEY idx_task_id (task_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='智能分析结果';

CREATE TABLE IF NOT EXISTS chart_task_event (
  id BIGINT NOT NULL AUTO_INCREMENT,
  task_id BIGINT NOT NULL,
  task_no VARCHAR(32) NOT NULL,
  event_type VARCHAR(40) NOT NULL,
  from_status VARCHAR(20) DEFAULT NULL,
  to_status VARCHAR(20) DEFAULT NULL,
  execution_mode VARCHAR(20) DEFAULT NULL,
  execution_stage VARCHAR(30) DEFAULT NULL,
  attempt INT NOT NULL DEFAULT 0,
  reconnect_count INT NOT NULL DEFAULT 0,
  error_code VARCHAR(50) DEFAULT NULL,
  error_message VARCHAR(1000) DEFAULT NULL,
  duration_ms BIGINT DEFAULT NULL,
  operator_type VARCHAR(20) NOT NULL,
  operator_id BIGINT DEFAULT NULL,
  trace_id VARCHAR(64) DEFAULT NULL,
  metadata JSON DEFAULT NULL,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_task_time (task_id, create_time),
  KEY idx_event_type_time (event_type, create_time),
  KEY idx_trace_id (trace_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='智能分析任务事件记录';
