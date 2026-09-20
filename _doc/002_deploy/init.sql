-- ============================================================
-- z-script 中间件数据库初始化脚本 (standalone schema bootstrap)
--
-- 适用对象: z-script 独立中间件仓 (Java 8 / Spring Boot 2.7 / MyBatis-Plus)
--          z-script-admin 控制台通过 z-boot-datasource-starter 的
--          ModuleDataSourceTemplate("script") 建连, 配置键为
--          z.base.db.script.{host,port,database,username,password},
--          默认 localhost:3306 / z_script / root / 空密码。
-- 内容:     z-script-core 16 个 @TableName 实体对应的全部建表语句 + 演示种子数据。
-- 幂等:     CREATE DATABASE/TABLE IF NOT EXISTS + 种子 INSERT ... WHERE NOT EXISTS,
--          可反复执行, 不会丢数据。
-- 用法:     mysql -h127.0.0.1 -uroot < _doc/002_deploy/init.sql
-- 校验:     mysql -h127.0.0.1 -uroot -e "SHOW TABLES FROM z_script;"
--
-- 说明: 历史版本 (FEATURE025 期间临时拼的 init.sql) 只有 z_script 与
--      z_script_run_log 两张表, 且 z_script_run_log 并非任何实体对应的表,
--      已废弃; 执行日志的正确落点是 z_script_execution_log。
--      凭证不入库: 第一把 API Key 由 POST /api/script/api-key 现场签发。
-- ============================================================

CREATE DATABASE IF NOT EXISTS z_script CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
USE z_script;

-- ------------------------------------------------------------
-- 1. z_script - 脚本注册表 (entity: Script)
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `z_script`
(
    `id`            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键 ID',
    `script_code`   VARCHAR(128) NOT NULL COMMENT '脚本唯一编码',
    `script_name`   VARCHAR(128) NOT NULL COMMENT '脚本名称',
    `dsl_type`      VARCHAR(32)  DEFAULT 'EL' COMMENT 'DSL 类型: EL|GROOVY|LUA|SQL|MOCK|API_BRIDGE',
    `source_code`   LONGTEXT     NOT NULL COMMENT '脚本源码',
    `expose_as`     VARCHAR(32)  DEFAULT 'NONE' COMMENT '暴露协议: NONE|HTTP|MCP|BOTH',
    `http_path`     VARCHAR(256) DEFAULT NULL COMMENT 'HTTP 路径 (自动生成)',
    `mcp_tool_name` VARCHAR(128) DEFAULT NULL COMMENT 'MCP 工具名 (注册到 McpRegistry)',
    `input_schema`  TEXT         DEFAULT NULL COMMENT '输入 JSON Schema',
    `output_schema` TEXT         DEFAULT NULL COMMENT '输出 JSON Schema',
    `version`       VARCHAR(32)  DEFAULT '1.0.0' COMMENT '版本号',
    `description`   VARCHAR(512) DEFAULT NULL COMMENT '脚本描述',
    `status`        TINYINT      DEFAULT 1 COMMENT '0=草稿, 1=启用, 2=禁用',
    `creator_id`    VARCHAR(64)  DEFAULT NULL COMMENT '创建者 ID',
    `tenant_code`   VARCHAR(64)  DEFAULT 'default' COMMENT '租户编码',
    `deleted`       TINYINT      DEFAULT 0 COMMENT '逻辑删除: 0=正常, 1=已删',
    `create_time`   DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`   DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_favorite`   TINYINT      DEFAULT 0 COMMENT '是否收藏 (FEATURE051 增列, 原为 ALTER)',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_script_code` (`script_code`),
    KEY `idx_dsl_type` (`dsl_type`),
    KEY `idx_expose_as` (`expose_as`),
    KEY `idx_status` (`status`),
    KEY `idx_creator_id` (`creator_id`),
    KEY `idx_tenant_code` (`tenant_code`),
    KEY `idx_create_time` (`create_time`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '脚本注册表';

-- ------------------------------------------------------------
-- 2. z_script_version - 版本管理 + 灰度发布 (entity: ScriptVersionDO)
--    实体中 scriptCode/dslType/dslContent/outputMapping/canaryWeight/changeLog
--    均为 @TableField(exist = false) 的业务别名, 不落库。
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `z_script_version`
(
    `id`              BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键 ID',
    `script_id`       BIGINT      NOT NULL COMMENT '关联 z_script.id',
    `version_no`      INT         NOT NULL COMMENT '版本号 (1, 2, 3...)',
    `source_code`     LONGTEXT    NOT NULL COMMENT '该版本源代码',
    `changelog`       TEXT        DEFAULT NULL COMMENT '变更说明',
    `status`          VARCHAR(16) DEFAULT 'DRAFT' COMMENT 'DRAFT|PUBLISHED|DEPRECATED (兼容 GRAY|ACTIVE|ARCHIVED)',
    `gray_percentage` INT         DEFAULT 0 COMMENT '灰度比例 0-100',
    `gray_api_keys`   TEXT        DEFAULT NULL COMMENT '指定灰度的 API Key ID 列表 (JSON)',
    `published_by`    VARCHAR(64) DEFAULT NULL COMMENT '发布人',
    `published_at`    DATETIME    DEFAULT CURRENT_TIMESTAMP COMMENT '发布时间',
    `is_current`      TINYINT     DEFAULT 0 COMMENT '是否当前生效版本',
    `create_time`     DATETIME    DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`     DATETIME    DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_script_version` (`script_id`, `version_no`),
    KEY `idx_script_id` (`script_id`),
    KEY `idx_status` (`status`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = 'z-script 版本管理';

-- ------------------------------------------------------------
-- 3. z_script_tag - 标签 (entity: ScriptTagDO)
--    实体中 tagCategory/tagColor 为 @TableField(exist = false), 不落库。
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `z_script_tag`
(
    `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键 ID',
    `tag_name`    VARCHAR(64)  NOT NULL COMMENT '标签名',
    `color`       VARCHAR(16)  DEFAULT 'blue' COMMENT 'UI 显示颜色',
    `description` VARCHAR(256) DEFAULT NULL COMMENT '标签描述',
    `create_time` DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_tag_name` (`tag_name`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = 'z-script 标签';

-- ------------------------------------------------------------
-- 4. z_script_tag_rel - 标签关联 (entity: ScriptTagRelDO, 联合主键, 无自增 id)
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `z_script_tag_rel`
(
    `script_id`   BIGINT   NOT NULL COMMENT '关联 z_script.id (联合主键第一部分, @TableId INPUT)',
    `tag_id`      BIGINT   NOT NULL COMMENT '关联 z_script_tag.id',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`script_id`, `tag_id`),
    KEY `idx_tag_id` (`tag_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = 'z-script 标签关联';

-- ------------------------------------------------------------
-- 5. z_script_api_key - API Key 鉴权 (entity: ApiKeyDO)
--    max_retry/retry_backoff_ms/retry_on/circuit_breaker_threshold 由
--    RetryPolicyServiceImpl 读写, feature051 建表语句缺失, 此处补齐。
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `z_script_api_key`
(
    `id`                        BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键 ID',
    `api_key`                   VARCHAR(64)  NOT NULL COMMENT 'API Key 标识 (zsk_live_xxx)',
    `api_secret_hash`           VARCHAR(128) NOT NULL COMMENT 'Secret 的 SHA256 哈希 (不存明文)',
    `app_name`                  VARCHAR(128) NOT NULL COMMENT '调用方应用名',
    `owner_id`                  VARCHAR(64)  DEFAULT NULL COMMENT '创建人 ID',
    `scope`                     VARCHAR(32)  DEFAULT 'ALL' COMMENT '权限范围: ALL|READ_ONLY|SPECIFIC',
    `max_retry`                 INT          DEFAULT 0 COMMENT '最大重试次数 (0=不重试)',
    `retry_backoff_ms`          INT          DEFAULT 1000 COMMENT '重试基础退避毫秒 (指数退避)',
    `retry_on`                  VARCHAR(32)  DEFAULT 'HTTP_5XX' COMMENT '重试条件: ALL|TIMEOUT|HTTP_5XX|NONE',
    `circuit_breaker_threshold` INT          DEFAULT 0 COMMENT '熔断连续失败阈值 (0=不熔断)',
    `allowed_scripts`           TEXT         DEFAULT NULL COMMENT 'SPECIFIC 模式下的 script_code 列表 (JSON)',
    `ip_whitelist`              TEXT         DEFAULT NULL COMMENT 'IP 白名单 (JSON 数组, 支持 CIDR)',
    `status`                    TINYINT      DEFAULT 1 COMMENT '1=启用 0=禁用',
    `expire_at`                 DATETIME     DEFAULT NULL COMMENT '过期时间 (NULL=永不过期)',
    `last_used_at`              DATETIME     DEFAULT NULL COMMENT '最后一次调用时间',
    `total_calls`               BIGINT       DEFAULT 0 COMMENT '累计调用次数',
    `description`               VARCHAR(512) DEFAULT NULL COMMENT '备注',
    `create_time`               DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`               DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `tenant_code`               VARCHAR(64)  DEFAULT 'default' COMMENT '租户编码',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_api_key` (`api_key`),
    KEY `idx_app_name` (`app_name`),
    KEY `idx_status` (`status`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = 'z-script API Key';

-- ------------------------------------------------------------
-- 6. z_script_quota - 配额限流 (entity: QuotaDO)
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `z_script_quota`
(
    `id`                 BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键 ID',
    `api_key_id`         BIGINT      NOT NULL COMMENT '关联 z_script_api_key.id',
    `max_per_day`        BIGINT      DEFAULT 10000 COMMENT '每日最大调用次数',
    `max_per_minute`     BIGINT      DEFAULT 60 COMMENT '每分钟最大调用次数',
    `max_concurrent`     INT         DEFAULT 10 COMMENT '最大并发数',
    `used_today`         BIGINT      DEFAULT 0 COMMENT '今日已用',
    `used_today_date`    DATE        DEFAULT NULL COMMENT '今日日期 (跨天重置标记)',
    `used_minute`        BIGINT      DEFAULT 0 COMMENT '当前分钟窗口已用',
    `used_minute_window` VARCHAR(20) DEFAULT NULL COMMENT '当前分钟窗口 (yyyyMMddHHmm)',
    `alert_threshold`    INT         DEFAULT 80 COMMENT '告警阈值百分比',
    `alerted_at`         DATETIME    DEFAULT NULL COMMENT '上次告警时间 (防重复)',
    `create_time`        DATETIME    DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`        DATETIME    DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_api_key_id` (`api_key_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = 'z-script 配额';

-- ------------------------------------------------------------
-- 7. z_script_invoke_log - 调用日志 (entity: InvokeLogDO)
--    实体中 dslType/requestSize 为 @TableField(exist = false), 不落库。
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `z_script_invoke_log`
(
    `id`            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键 ID',
    `api_key_id`    BIGINT       DEFAULT NULL COMMENT '调用方 (NULL=未鉴权/匿名)',
    `app_name`      VARCHAR(128) DEFAULT NULL COMMENT '调用方应用名 (冗余)',
    `script_id`     BIGINT       DEFAULT NULL COMMENT '脚本 ID',
    `script_code`   VARCHAR(128) DEFAULT NULL COMMENT '脚本编码 (冗余方便查询)',
    `invoke_ip`     VARCHAR(64)  DEFAULT NULL COMMENT '调用方 IP',
    `invoke_method` VARCHAR(8)   DEFAULT 'POST' COMMENT 'HTTP 方法',
    `invoke_path`   VARCHAR(255) DEFAULT NULL COMMENT '调用路径',
    `invoke_params` TEXT         DEFAULT NULL COMMENT '入参 (脱敏后)',
    `invoke_status` TINYINT      DEFAULT 1 COMMENT '1=成功 0=失败',
    `http_status`   INT          DEFAULT NULL COMMENT 'HTTP 状态码',
    `duration_ms`   BIGINT       DEFAULT 0 COMMENT '耗时 (毫秒)',
    `error_message` TEXT         DEFAULT NULL COMMENT '错误信息',
    `invoked_at`    DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '调用时间',
    PRIMARY KEY (`id`),
    KEY `idx_api_key_invoked_at` (`api_key_id`, `invoked_at`),
    KEY `idx_script_code` (`script_code`),
    KEY `idx_invoked_at` (`invoked_at`),
    KEY `idx_invoke_status` (`invoke_status`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = 'z-script 调用日志';

-- ------------------------------------------------------------
-- 8. z_script_execution_log - 脚本执行日志 (entity: ScriptExecutionLog)
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `z_script_execution_log`
(
    `id`            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键 ID',
    `script_code`   VARCHAR(128) NOT NULL COMMENT '脚本编码',
    `execute_type`  VARCHAR(32)  DEFAULT 'AD_HOC' COMMENT '执行类型: AD_HOC|HTTP|MCP|TEST|API',
    `input_params`  TEXT         DEFAULT NULL COMMENT '输入参数 (JSON)',
    `output_result` TEXT         DEFAULT NULL COMMENT '输出结果 (JSON)',
    `success`       TINYINT      DEFAULT 1 COMMENT '1=成功 0=失败',
    `error_message` TEXT         DEFAULT NULL COMMENT '错误信息',
    `duration_ms`   BIGINT       DEFAULT 0 COMMENT '执行耗时 (毫秒)',
    `executor_id`   VARCHAR(64)  DEFAULT NULL COMMENT '执行人 ID',
    `app_name`      VARCHAR(128) DEFAULT NULL COMMENT '调用方应用名 (FEATURE051 增列, 原为 ALTER)',
    `create_time`   DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_script_code` (`script_code`),
    KEY `idx_execute_type` (`execute_type`),
    KEY `idx_create_time` (`create_time`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '脚本执行日志';

-- ------------------------------------------------------------
-- 9. z_mock_environment - Mock 环境 (真实/Mock 切换) (entity: MockEnvironment)
--    MockEngine.resolveEnv 依赖 env_code='default' 或 is_default=1 的行。
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `z_mock_environment`
(
    `id`            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键 ID',
    `env_code`      VARCHAR(64)  NOT NULL COMMENT '环境编码: default/dev/staging/prod',
    `env_name`      VARCHAR(128) NOT NULL COMMENT '环境名称',
    `env_type`      VARCHAR(32)  DEFAULT 'MOCK' COMMENT '环境类型: MOCK|REAL|MIXED',
    `base_url`      VARCHAR(512) DEFAULT NULL COMMENT '真实服务地址 (REAL/MIXED 时使用)',
    `mock_priority` TINYINT      DEFAULT 1 COMMENT '匹配优先级: 0=优先 Mock, 1=未匹配转发真实',
    `description`   VARCHAR(512) DEFAULT NULL COMMENT '环境描述',
    `is_default`    TINYINT      DEFAULT 0 COMMENT '是否默认环境',
    `tenant_code`   VARCHAR(64)  DEFAULT 'default' COMMENT '租户编码',
    `deleted`       TINYINT      DEFAULT 0 COMMENT '逻辑删除: 0=正常, 1=已删',
    `create_time`   DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`   DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_env_code` (`env_code`),
    KEY `idx_env_type` (`env_type`),
    KEY `idx_tenant_code` (`tenant_code`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = 'Mock 环境 - 控制真实/Mock 切换';

-- ------------------------------------------------------------
-- 10. z_mock_endpoint - Mock 端点 (核心匹配表) (entity: MockEndpoint)
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `z_mock_endpoint`
(
    `id`                BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键 ID',
    `mock_code`         VARCHAR(64)  NOT NULL COMMENT 'Mock 唯一编码',
    `mock_name`         VARCHAR(128) NOT NULL COMMENT 'Mock 名称',
    `env_code`          VARCHAR(64)  DEFAULT 'default' COMMENT '所属环境',
    `project_code`      VARCHAR(64)  DEFAULT 'default' COMMENT '项目编码 (用于分组)',
    `module_code`       VARCHAR(128) DEFAULT NULL COMMENT '模块/分组',
    `path`              VARCHAR(256) NOT NULL COMMENT '路径 (支持通配符: /api/users/*)',
    `method`            VARCHAR(16)  DEFAULT 'GET' COMMENT 'HTTP 方法',
    `match_url_pattern` VARCHAR(512) DEFAULT NULL COMMENT 'URL 匹配模式 (Ant 风格: /api/users/{id})',
    `match_headers`     TEXT         DEFAULT NULL COMMENT 'Header 匹配规则 (JSON)',
    `match_query`       TEXT         DEFAULT NULL COMMENT 'Query 匹配规则 (JSON)',
    `match_body`        TEXT         DEFAULT NULL COMMENT 'Body 匹配规则 (JSONPath/正则)',
    `match_body_type`   VARCHAR(16)  DEFAULT 'JSON' COMMENT 'Body 类型: JSON|XML|TEXT|REGEX',
    `response_template` TEXT         NOT NULL COMMENT '响应模板 (JSON with @expressions)',
    `response_headers`  TEXT         DEFAULT NULL COMMENT '响应头 (JSON)',
    `delay_ms`          INT          DEFAULT 0 COMMENT '模拟延迟 (毫秒)',
    `status_code`       INT          DEFAULT 200 COMMENT 'HTTP 状态码',
    `priority`          INT          DEFAULT 5 COMMENT '匹配优先级 (数字越小越高)',
    `scenario_code`     VARCHAR(64)  DEFAULT NULL COMMENT '关联场景编码 (状态机)',
    `required_state`    VARCHAR(64)  DEFAULT NULL COMMENT '匹配所需的场景状态 (空=不限制)',
    `new_state`         VARCHAR(64)  DEFAULT NULL COMMENT '匹配后切换到的新状态',
    `fault_type`        VARCHAR(32)  DEFAULT 'NONE' COMMENT '故障类型: NONE|TIMEOUT|EMPTY_RESPONSE|RANDOM_DROP|MALFORMED|CHUNKED|500_ERROR|RATE_LIMIT',
    `fault_config`      TEXT         DEFAULT NULL COMMENT '故障配置 (JSON)',
    `proxy_to`          VARCHAR(512) DEFAULT NULL COMMENT '代理目标 URL (代理模式时使用)',
    `hit_count`         BIGINT       DEFAULT 0 COMMENT '命中次数',
    `verify_count`      BIGINT       DEFAULT 0 COMMENT '验证次数',
    `last_hit_time`     DATETIME     DEFAULT NULL COMMENT '最后命中时间',
    `tags`              VARCHAR(512) DEFAULT NULL COMMENT '标签 (逗号分隔)',
    `description`       TEXT         DEFAULT NULL COMMENT '描述',
    `script_code`       VARCHAR(128) DEFAULT NULL COMMENT '关联的 z_script 编码',
    `source_recording`  VARCHAR(64)  DEFAULT NULL COMMENT '来源录制编码 (录制转 Mock)',
    `status`            TINYINT      DEFAULT 1 COMMENT '0=禁用, 1=启用',
    `tenant_code`       VARCHAR(64)  DEFAULT 'default' COMMENT '租户编码',
    `creator_id`        VARCHAR(64)  DEFAULT NULL COMMENT '创建者 ID',
    `deleted`           TINYINT      DEFAULT 0 COMMENT '逻辑删除: 0=正常, 1=已删',
    `create_time`       DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`       DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_mock_code` (`mock_code`),
    KEY `idx_env_code` (`env_code`),
    KEY `idx_project_code` (`project_code`),
    KEY `idx_path` (`path`),
    KEY `idx_method` (`method`),
    KEY `idx_scenario` (`scenario_code`),
    KEY `idx_source_recording` (`source_recording`),
    KEY `idx_status` (`status`),
    KEY `idx_priority` (`priority`),
    KEY `idx_tenant_code` (`tenant_code`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = 'Mock 端点 - 核心匹配表';

-- ------------------------------------------------------------
-- 11. z_mock_scenario - Mock 场景 (状态机定义) (entity: MockScenario)
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `z_mock_scenario`
(
    `id`            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键 ID',
    `scenario_code` VARCHAR(64)  NOT NULL COMMENT '场景编码 (全局唯一)',
    `scenario_name` VARCHAR(128) NOT NULL COMMENT '场景名称',
    `initial_state` VARCHAR(64)  DEFAULT 'started' COMMENT '初始状态名',
    `description`   TEXT         DEFAULT NULL COMMENT '场景描述',
    `tenant_code`   VARCHAR(64)  DEFAULT 'default' COMMENT '租户编码',
    `creator_id`    VARCHAR(64)  DEFAULT NULL COMMENT '创建人 ID',
    `deleted`       TINYINT      DEFAULT 0 COMMENT '逻辑删除: 0=正常, 1=已删',
    `create_time`   DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`   DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_scenario_code` (`scenario_code`),
    KEY `idx_tenant_code` (`tenant_code`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = 'Mock 场景 - 状态机定义';

-- ------------------------------------------------------------
-- 12. z_mock_scenario_state - 场景状态实例 (运行态) (entity: MockScenarioState)
--     ScenarioStateMachine 按 (scenario_code, instance_key) 读写, 唯一键必需。
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `z_mock_scenario_state`
(
    `id`             BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键 ID',
    `scenario_code`  VARCHAR(64)  NOT NULL COMMENT '场景编码',
    `instance_key`   VARCHAR(256) NOT NULL COMMENT '实例标识 (通常为 session/traceID)',
    `current_state`  VARCHAR(64)  NOT NULL COMMENT '当前状态',
    `transition_log` TEXT         DEFAULT NULL COMMENT '状态转换历史 (JSON)',
    `create_time`    DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`    DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_scenario_instance` (`scenario_code`, `instance_key`),
    KEY `idx_instance` (`instance_key`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = 'Mock 场景状态实例 - 状态机运行态';

-- ------------------------------------------------------------
-- 13. z_mock_test_case - Mock 测试用例库 (entity: MockTestCase)
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `z_mock_test_case`
(
    `id`                       BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键 ID',
    `case_code`                VARCHAR(64)  NOT NULL COMMENT '用例编码',
    `case_name`                VARCHAR(128) NOT NULL COMMENT '用例名称',
    `case_group`               VARCHAR(128) DEFAULT 'default' COMMENT '用例分组',
    `env_code`                 VARCHAR(64)  DEFAULT 'default' COMMENT '执行环境',
    `request_method`           VARCHAR(16)  DEFAULT 'GET' COMMENT '请求方法',
    `request_url`              VARCHAR(512) NOT NULL COMMENT '请求 URL',
    `request_headers`          TEXT         DEFAULT NULL COMMENT '请求头 (JSON)',
    `request_query`            TEXT         DEFAULT NULL COMMENT 'Query 参数 (JSON)',
    `request_body`             TEXT         DEFAULT NULL COMMENT '请求体',
    `expected_status`          INT          DEFAULT 200 COMMENT '期望的 HTTP 状态码',
    `expected_headers`         TEXT         DEFAULT NULL COMMENT '期望响应头 (JSON)',
    `expected_body`            TEXT         DEFAULT NULL COMMENT '期望响应体',
    `expected_body_match_type` VARCHAR(16)  DEFAULT 'EXACT' COMMENT '匹配类型: EXACT|PARTIAL|REGEX|JSONPATH|LOOSE',
    `assertions`               TEXT         DEFAULT NULL COMMENT '断言规则列表 (JSON 数组)',
    `extract_rules`            TEXT         DEFAULT NULL COMMENT '变量提取规则 (JSON, 用例间传参)',
    `depends_on`               VARCHAR(64)  DEFAULT NULL COMMENT '依赖的用例编码',
    `run_count`                BIGINT       DEFAULT 0 COMMENT '执行次数',
    `pass_count`               BIGINT       DEFAULT 0 COMMENT '通过次数',
    `fail_count`               BIGINT       DEFAULT 0 COMMENT '失败次数',
    `last_run_time`            DATETIME     DEFAULT NULL COMMENT '最后执行时间',
    `last_run_result`          VARCHAR(16)  DEFAULT NULL COMMENT '最后执行结果: PASS|FAIL|SKIP',
    `priority`                 VARCHAR(16)  DEFAULT 'P1' COMMENT '优先级: HIGH|MEDIUM|LOW / P0|P1|P2|P3',
    `tags`                     VARCHAR(512) DEFAULT NULL COMMENT '标签 (JSON 数组或逗号分隔)',
    `status`                   TINYINT      DEFAULT 1 COMMENT '0=禁用, 1=启用',
    `description`              TEXT         DEFAULT NULL COMMENT '描述',
    `owner_id`                 VARCHAR(64)  DEFAULT NULL COMMENT '负责人 ID',
    `tenant_code`              VARCHAR(64)  DEFAULT 'default' COMMENT '租户编码',
    `deleted`                  TINYINT      DEFAULT 0 COMMENT '逻辑删除: 0=正常, 1=已删',
    `create_time`              DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`              DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_case_code` (`case_code`),
    KEY `idx_case_group` (`case_group`),
    KEY `idx_env_code` (`env_code`),
    KEY `idx_priority` (`priority`),
    KEY `idx_tenant_code` (`tenant_code`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = 'Mock 测试用例库';

-- ------------------------------------------------------------
-- 14. z_mock_recording - 录制会话 (entity: MockRecording)
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `z_mock_recording`
(
    `id`             BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键 ID',
    `recording_code` VARCHAR(64)  NOT NULL COMMENT '录制编码',
    `recording_name` VARCHAR(128) NOT NULL COMMENT '录制名称',
    `target_url`     VARCHAR(512) NOT NULL COMMENT '被代理的真实服务地址',
    `record_status`  VARCHAR(16)  DEFAULT 'IDLE' COMMENT '状态: IDLE|RECORDING|STOPPED|EXPIRED',
    `total_requests` BIGINT       DEFAULT 0 COMMENT '录制请求数',
    `file_path`      VARCHAR(512) DEFAULT NULL COMMENT '录制文件路径 (用于播放)',
    `description`    TEXT         DEFAULT NULL COMMENT '描述',
    `start_time`     DATETIME     DEFAULT NULL COMMENT '开始录制时间',
    `end_time`       DATETIME     DEFAULT NULL COMMENT '结束录制时间',
    `tenant_code`    VARCHAR(64)  DEFAULT 'default' COMMENT '租户编码',
    `creator_id`     VARCHAR(64)  DEFAULT NULL COMMENT '创建人 ID',
    `deleted`        TINYINT      DEFAULT 0 COMMENT '逻辑删除: 0=正常, 1=已删',
    `create_time`    DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`    DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_recording_code` (`recording_code`),
    KEY `idx_record_status` (`record_status`),
    KEY `idx_tenant_code` (`tenant_code`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '录制会话';

-- ------------------------------------------------------------
-- 15. z_mock_recording_request - 录制的请求详情 (entity: MockRecordingRequest)
--     Mapper 里 ORDER BY sequence / WHERE recording_code, 两列均需索引。
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `z_mock_recording_request`
(
    `id`               BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键 ID',
    `recording_code`   VARCHAR(64)   NOT NULL COMMENT '录制编码',
    `sequence`         INT           NOT NULL COMMENT '请求序号',
    `request_method`   VARCHAR(16)   NOT NULL COMMENT '请求方法',
    `request_url`      VARCHAR(1024) NOT NULL COMMENT '请求 URL',
    `request_headers`  TEXT          DEFAULT NULL COMMENT '请求头 (JSON)',
    `request_body`     TEXT          DEFAULT NULL COMMENT '请求体',
    `response_status`  INT           DEFAULT NULL COMMENT '响应状态码',
    `response_headers` TEXT          DEFAULT NULL COMMENT '响应头 (JSON)',
    `response_body`    TEXT          DEFAULT NULL COMMENT '响应体',
    `response_time`    BIGINT        DEFAULT 0 COMMENT '响应时间 (毫秒)',
    `matched_endpoint` VARCHAR(64)   DEFAULT NULL COMMENT '匹配到的 Mock 端点 (回放时)',
    `create_time`      DATETIME      DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_recording_code` (`recording_code`),
    KEY `idx_sequence` (`sequence`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '录制的请求详情';

-- ------------------------------------------------------------
-- 16. z_mock_request_log - Mock 请求日志 (entity: MockRequestLog)
--     Mapper 里按 request_path / mock_code / matched 过滤并按 duration_ms 聚合。
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `z_mock_request_log`
(
    `id`              BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键 ID',
    `mock_code`       VARCHAR(64)  DEFAULT NULL COMMENT '匹配的 Mock 编码 (未匹配时为 NULL)',
    `env_code`        VARCHAR(64)  DEFAULT NULL COMMENT '环境编码',
    `request_path`    VARCHAR(512) DEFAULT NULL COMMENT '请求路径',
    `request_method`  VARCHAR(16)  DEFAULT NULL COMMENT '请求方法',
    `request_headers` TEXT         DEFAULT NULL COMMENT '请求头',
    `request_body`    TEXT         DEFAULT NULL COMMENT '请求体',
    `response_status` INT          DEFAULT NULL COMMENT '响应状态码',
    `response_body`   TEXT         DEFAULT NULL COMMENT '响应体',
    `matched`         TINYINT      DEFAULT 0 COMMENT '是否匹配到 Mock',
    `fallback_type`   VARCHAR(32)  DEFAULT NULL COMMENT '未匹配时的处理: PROXY|404|DEFAULT',
    `duration_ms`     BIGINT       DEFAULT 0 COMMENT '响应耗时 (毫秒)',
    `client_ip`       VARCHAR(64)  DEFAULT NULL COMMENT '客户端 IP',
    `user_agent`      VARCHAR(512) DEFAULT NULL COMMENT 'User-Agent',
    `scenario_state`  VARCHAR(64)  DEFAULT NULL COMMENT '触发时的场景状态',
    `create_time`     DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_mock_code` (`mock_code`),
    KEY `idx_env_code` (`env_code`),
    KEY `idx_path` (`request_path`),
    KEY `idx_matched` (`matched`),
    KEY `idx_create_time` (`create_time`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = 'Mock 请求日志';

-- ============================================================
-- 种子数据 (幂等: 唯一键已存在则跳过, 不覆盖用户改动)
-- 下列 INSERT 只补齐各表的 NOT NULL 列, 其余列走 DEFAULT。
-- ============================================================

-- 演示脚本: 控制台「脚本」页签默认能看到一条 hello_world，且点「执行」就能跑通。
-- dsl_type 必须是 ScriptEngine 认得的 EL|GROOVY|LUA|SQL|MOCK|API_BRIDGE；
-- 原先这里写的是 'python'，ScriptEngine 直接 "Unsupported DSL type" → 自检必挂。
-- EL 走 ElSandbox 的 SpEL，入参以 #name 形式引用。
-- http_path 只做展示（真正可调的路径由 Controller 按 scriptCode 推导：/api/script-run/{code}
-- 与 /run/{code}）。写死成 /demo/hello 会让控制台显示一个谁都调不通的路径，故与
-- ScriptController.create/publish 自动生成的值保持一致。
INSERT INTO `z_script`
(`script_code`, `script_name`, `dsl_type`, `source_code`, `expose_as`, `http_path`, `input_schema`,
 `description`, `status`, `creator_id`, `tenant_code`)
SELECT 'hello_world',
       'Hello World (Demo)',
       'EL',
       '\'hello, \' + #name + \'! @ z-script\'',
       'HTTP',
       '/run/hello_world',
       '{"type":"object","properties":{"name":{"type":"string","default":"z-script"}}}',
       'E2E demo script for z-script console',
       1,
       'e2e-tester',
       'default'
FROM DUAL
WHERE NOT EXISTS(SELECT 1 FROM `z_script` WHERE `script_code` = 'hello_world');

-- 默认 Mock 环境: MockEngine.resolveEnv 需要 env_code='default' 或 is_default=1
INSERT INTO `z_mock_environment`
(`env_code`, `env_name`, `env_type`, `base_url`, `mock_priority`, `description`, `is_default`, `tenant_code`)
SELECT 'default',
       '默认环境',
       'MOCK',
       NULL,
       1,
       '开箱即用的 Mock 环境 (未命中返回 404)',
       1,
       'default'
FROM DUAL
WHERE NOT EXISTS(SELECT 1 FROM `z_mock_environment` WHERE `env_code` = 'default');

-- 演示 Mock 端点: 控制台「Mock 端点」页签的展示样例
INSERT INTO `z_mock_endpoint`
(`mock_code`, `mock_name`, `env_code`, `project_code`, `path`, `method`, `response_template`, `response_headers`,
 `status_code`, `delay_ms`, `priority`, `fault_type`, `script_code`, `description`, `status`, `tenant_code`,
 `creator_id`)
SELECT 'demo_hello',
       'Hello Demo',
       'default',
       'default',
       '/api/mock/hello',
       'GET',
       '{"message":"hello from z-script mock","code":200}',
       '{"Content-Type":"application/json"}',
       200,
       0,
       5,
       'NONE',
       'hello_world',
       'E2E demo mock endpoint',
       1,
       'default',
       'e2e-tester'
FROM DUAL
WHERE NOT EXISTS(SELECT 1 FROM `z_mock_endpoint` WHERE `mock_code` = 'demo_hello');

-- ============================================================
-- 验证: 列出 z_script 库下全部表 (应与 16 个 @TableName 实体一一对应)
-- ============================================================
SELECT TABLE_NAME, TABLE_ROWS, TABLE_COMMENT
FROM information_schema.TABLES
WHERE TABLE_SCHEMA = DATABASE()
ORDER BY TABLE_NAME;
