-- V17: Promote task-bound templates to reusable owner template library.

ALTER TABLE templates ADD COLUMN owner_id BIGINT NULL COMMENT '模板所有者ID' AFTER task_id;

UPDATE templates t
JOIN tasks task ON task.id = t.task_id
SET t.owner_id = task.owner_id
WHERE t.owner_id IS NULL;

CREATE TEMPORARY TABLE flyway_v17_template_owner_precheck AS
SELECT COUNT(*) AS missing_owner_count
FROM templates
WHERE owner_id IS NULL;

ALTER TABLE flyway_v17_template_owner_precheck
    ADD CONSTRAINT chk_template_owner_backfill_source
        CHECK (missing_owner_count = 0);

DROP TEMPORARY TABLE flyway_v17_template_owner_precheck;

ALTER TABLE templates MODIFY COLUMN owner_id BIGINT NOT NULL COMMENT '模板所有者ID';

ALTER TABLE templates ADD CONSTRAINT fk_templates_owner FOREIGN KEY (owner_id) REFERENCES users(id);

ALTER TABLE templates ADD INDEX idx_templates_owner (owner_id, updated_at);

ALTER TABLE templates DROP FOREIGN KEY fk_templates_task;

ALTER TABLE templates MODIFY COLUMN task_id BIGINT NULL COMMENT '历史来源任务ID；owner 模板库中新建模板为空';

ALTER TABLE template_versions ADD COLUMN owner_id BIGINT NULL COMMENT '模板版本所有者ID' AFTER template_id;

UPDATE template_versions tv
JOIN templates t ON t.id = tv.template_id
SET tv.owner_id = t.owner_id
WHERE tv.owner_id IS NULL;

CREATE TEMPORARY TABLE flyway_v17_template_version_owner_precheck AS
SELECT COUNT(*) AS missing_owner_count
FROM template_versions
WHERE owner_id IS NULL;

ALTER TABLE flyway_v17_template_version_owner_precheck
    ADD CONSTRAINT chk_template_version_owner_backfill_source
        CHECK (missing_owner_count = 0);

DROP TEMPORARY TABLE flyway_v17_template_version_owner_precheck;

ALTER TABLE template_versions MODIFY COLUMN owner_id BIGINT NOT NULL COMMENT '模板版本所有者ID';

ALTER TABLE template_versions ADD CONSTRAINT fk_template_versions_owner FOREIGN KEY (owner_id) REFERENCES users(id);

ALTER TABLE template_versions ADD INDEX idx_template_versions_owner (owner_id, created_at);

ALTER TABLE template_versions DROP FOREIGN KEY fk_template_versions_task;

ALTER TABLE template_versions MODIFY COLUMN task_id BIGINT NULL COMMENT '历史来源任务ID；owner 模板库中新建版本为空';
