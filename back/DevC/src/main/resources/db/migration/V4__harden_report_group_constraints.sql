SET @unlinked_report_count := (
    SELECT COUNT(*)
    FROM reports
    WHERE report_group_id IS NULL
);

SET @unlinked_report_check_sql := IF(
    @unlinked_report_count = 0,
    'SELECT 1',
    'SIGNAL SQLSTATE ''45000'' SET MESSAGE_TEXT = ''reports.report_group_id contains NULL values'''
);

PREPARE unlinked_report_check_stmt FROM @unlinked_report_check_sql;
EXECUTE unlinked_report_check_stmt;
DEALLOCATE PREPARE unlinked_report_check_stmt;

SET @duplicate_report_group_count := (
    SELECT COUNT(*)
    FROM (
        SELECT reporter_user_id, report_group_id
        FROM reports
        GROUP BY reporter_user_id, report_group_id
        HAVING COUNT(*) > 1
    ) duplicated_reports
);

SET @duplicate_report_group_check_sql := IF(
    @duplicate_report_group_count = 0,
    'SELECT 1',
    'SIGNAL SQLSTATE ''45000'' SET MESSAGE_TEXT = ''duplicate reports exist for reporter_user_id and report_group_id'''
);

PREPARE duplicate_report_group_check_stmt FROM @duplicate_report_group_check_sql;
EXECUTE duplicate_report_group_check_stmt;
DEALLOCATE PREPARE duplicate_report_group_check_stmt;

SET @report_group_column_nullable := (
    SELECT IS_NULLABLE
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'reports'
      AND COLUMN_NAME = 'report_group_id'
);

SET @report_group_not_null_sql := IF(
    @report_group_column_nullable = 'YES',
    'ALTER TABLE reports MODIFY report_group_id BIGINT NOT NULL',
    'SELECT 1'
);

PREPARE report_group_not_null_stmt FROM @report_group_not_null_sql;
EXECUTE report_group_not_null_stmt;
DEALLOCATE PREPARE report_group_not_null_stmt;

SET @reporter_report_group_index_exists := (
    SELECT COUNT(*)
    FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'reports'
      AND INDEX_NAME = 'uk_reports_reporter_report_group'
);

SET @reporter_report_group_index_sql := IF(
    @reporter_report_group_index_exists = 0,
    'CREATE UNIQUE INDEX uk_reports_reporter_report_group ON reports (reporter_user_id, report_group_id)',
    'SELECT 1'
);

PREPARE reporter_report_group_index_stmt FROM @reporter_report_group_index_sql;
EXECUTE reporter_report_group_index_stmt;
DEALLOCATE PREPARE reporter_report_group_index_stmt;
