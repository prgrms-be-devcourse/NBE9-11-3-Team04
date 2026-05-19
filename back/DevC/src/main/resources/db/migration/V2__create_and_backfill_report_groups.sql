CREATE TABLE IF NOT EXISTS report_groups (
    report_group_id BIGINT NOT NULL AUTO_INCREMENT,
    target_type VARCHAR(30) NOT NULL,
    target_id BIGINT NOT NULL,
    status VARCHAR(30) NOT NULL,
    report_count BIGINT NOT NULL DEFAULT 0,
    latest_reported_at DATETIME(6) NOT NULL,
    processed_by BIGINT NULL,
    processed_at DATETIME(6) NULL,
    admin_note TEXT NULL,
    sanction_type VARCHAR(30) NULL,
    suspension_days INT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    PRIMARY KEY (report_group_id),
    CONSTRAINT uk_report_groups_target UNIQUE (target_type, target_id),
    CONSTRAINT fk_report_groups_processed_by FOREIGN KEY (processed_by) REFERENCES users (user_id)
);

SET @report_group_status_index_exists := (
    SELECT COUNT(*)
    FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'report_groups'
      AND INDEX_NAME = 'idx_report_groups_status_latest'
);

SET @report_group_status_index_sql := IF(
    @report_group_status_index_exists = 0,
    'CREATE INDEX idx_report_groups_status_latest ON report_groups (status, latest_reported_at)',
    'SELECT 1'
);

PREPARE report_group_status_index_stmt FROM @report_group_status_index_sql;
EXECUTE report_group_status_index_stmt;
DEALLOCATE PREPARE report_group_status_index_stmt;

SET @report_group_target_index_exists := (
    SELECT COUNT(*)
    FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'report_groups'
      AND INDEX_NAME = 'idx_report_groups_target'
);

SET @report_group_target_index_sql := IF(
    @report_group_target_index_exists = 0,
    'CREATE INDEX idx_report_groups_target ON report_groups (target_type, target_id)',
    'SELECT 1'
);

PREPARE report_group_target_index_stmt FROM @report_group_target_index_sql;
EXECUTE report_group_target_index_stmt;
DEALLOCATE PREPARE report_group_target_index_stmt;

SET @report_group_column_exists := (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'reports'
      AND COLUMN_NAME = 'report_group_id'
);

SET @report_group_column_sql := IF(
    @report_group_column_exists = 0,
    'ALTER TABLE reports ADD COLUMN report_group_id BIGINT NULL',
    'SELECT 1'
);

PREPARE report_group_column_stmt FROM @report_group_column_sql;
EXECUTE report_group_column_stmt;
DEALLOCATE PREPARE report_group_column_stmt;

INSERT INTO report_groups (
    target_type,
    target_id,
    status,
    report_count,
    latest_reported_at,
    processed_by,
    processed_at,
    admin_note,
    sanction_type,
    suspension_days,
    version
)
SELECT
    r.target_type,
    r.target_id,
    CASE
        WHEN SUM(CASE WHEN r.status = 'PENDING' THEN 1 ELSE 0 END) > 0 THEN 'OPEN'
        WHEN SUM(CASE WHEN r.status = 'RESOLVED' THEN 1 ELSE 0 END) > 0 THEN 'APPROVED'
        ELSE 'REJECTED'
    END AS status,
    COUNT(*) AS report_count,
    COALESCE(MAX(r.created_at), CURRENT_TIMESTAMP(6)) AS latest_reported_at,
    MAX(r.processed_by) AS processed_by,
    MAX(r.processed_at) AS processed_at,
    NULL AS admin_note,
    NULL AS sanction_type,
    NULL AS suspension_days,
    0 AS version
FROM reports r
LEFT JOIN report_groups rg
    ON rg.target_type = r.target_type
   AND rg.target_id = r.target_id
WHERE rg.report_group_id IS NULL
GROUP BY r.target_type, r.target_id;

UPDATE reports r
JOIN report_groups rg
    ON rg.target_type = r.target_type
   AND rg.target_id = r.target_id
SET r.report_group_id = rg.report_group_id
WHERE r.report_group_id IS NULL;

SET @report_group_index_exists := (
    SELECT COUNT(*)
    FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'reports'
      AND INDEX_NAME = 'idx_reports_report_group'
);

SET @report_group_index_sql := IF(
    @report_group_index_exists = 0,
    'CREATE INDEX idx_reports_report_group ON reports (report_group_id)',
    'SELECT 1'
);

PREPARE report_group_index_stmt FROM @report_group_index_sql;
EXECUTE report_group_index_stmt;
DEALLOCATE PREPARE report_group_index_stmt;

SET @report_group_fk_exists := (
    SELECT COUNT(*)
    FROM information_schema.TABLE_CONSTRAINTS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'reports'
      AND CONSTRAINT_NAME = 'fk_reports_report_group'
);

SET @report_group_fk_sql := IF(
    @report_group_fk_exists = 0,
    'ALTER TABLE reports ADD CONSTRAINT fk_reports_report_group FOREIGN KEY (report_group_id) REFERENCES report_groups (report_group_id)',
    'SELECT 1'
);

PREPARE report_group_fk_stmt FROM @report_group_fk_sql;
EXECUTE report_group_fk_stmt;
DEALLOCATE PREPARE report_group_fk_stmt;
