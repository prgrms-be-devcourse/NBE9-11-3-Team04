CREATE TABLE report_group_actions (
    report_group_action_id BIGINT NOT NULL AUTO_INCREMENT,
    report_group_id BIGINT NOT NULL,
    admin_user_id BIGINT NOT NULL,
    action_type VARCHAR(30) NOT NULL,
    before_status VARCHAR(30) NOT NULL,
    after_status VARCHAR(30) NOT NULL,
    note TEXT NULL,
    sanction_type VARCHAR(30) NULL,
    suspension_days INT NULL,
    created_at DATETIME(6) NOT NULL,

    PRIMARY KEY (report_group_action_id),

    CONSTRAINT fk_report_group_actions_group
        FOREIGN KEY (report_group_id)
        REFERENCES report_groups (report_group_id),

    CONSTRAINT fk_report_group_actions_admin
        FOREIGN KEY (admin_user_id)
        REFERENCES users (user_id)
);

CREATE INDEX idx_report_group_actions_group_created
    ON report_group_actions (report_group_id, created_at);

CREATE INDEX idx_report_group_actions_admin
    ON report_group_actions (admin_user_id);
