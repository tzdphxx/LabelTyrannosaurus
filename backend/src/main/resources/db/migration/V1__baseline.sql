create table users
(
    id            bigint auto_increment
        primary key,
    username      varchar(64)                              not null,
    email         varchar(255)                             not null,
    password_hash varchar(255)                             null,
    user_type     varchar(20) default 'USER'               not null,
    login_enabled tinyint(1)  default 1                    not null,
    enabled       tinyint(1)  default 1                    not null,
    token_version int         default 1                    not null,
    display_name  varchar(100)                             null,
    avatar_url    varchar(500)                             null,
    last_login_at datetime(3)                              null,
    created_at    datetime(3) default CURRENT_TIMESTAMP(3) not null,
    updated_at    datetime(3) default CURRENT_TIMESTAMP(3) not null on update CURRENT_TIMESTAMP(3),
    constraint uk_users_email
        unique (email),
    constraint uk_users_username
        unique (username),
    constraint chk_users_user_type
        check (`user_type` in (_utf8mb4\'USER\',_utf8mb4\'SYSTEM\'))
)
charset=utf8mb4;

create table labeler_contribution_stats
(
    labeler_id            bigint                                      not null
        primary key,
    claimed_count         int            default 0                    not null,
    submitted_count       int            default 0                    not null,
    pending_review_count  int            default 0                    not null,
    approved_count        int            default 0                    not null,
    rejected_count        int            default 0                    not null,
    total_reward          decimal(14, 2) default 0.00                 not null,
    today_submitted_count int            default 0                    not null,
    last_submit_date      date                                        null,
    updated_at            datetime(3)    default CURRENT_TIMESTAMP(3) not null on update CURRENT_TIMESTAMP(3),
    constraint fk_contribution_stats_labeler
        foreign key (labeler_id) references users (id)
)
    charset = utf8mb4;

create table labeler_daily_stats
(
    id              bigint auto_increment
        primary key,
    labeler_id      bigint                                      not null,
    stat_date       date                                        not null,
    submitted_count int            default 0                    not null,
    approved_count  int            default 0                    not null,
    rejected_count  int            default 0                    not null,
    reward_amount   decimal(14, 2) default 0.00                 not null,
    created_at      datetime(3)    default CURRENT_TIMESTAMP(3) not null,
    updated_at      datetime(3)    default CURRENT_TIMESTAMP(3) not null on update CURRENT_TIMESTAMP(3),
    constraint uk_labeler_daily_stats_date
        unique (labeler_id, stat_date),
    constraint fk_labeler_daily_stats_labeler
        foreign key (labeler_id) references users (id),
    constraint chk_labeler_daily_stats_counts
        check ((`submitted_count` >= 0) and (`approved_count` >= 0) and (`rejected_count` >= 0))
)
    comment 'Daily labeler contribution snapshot for trend charts.' charset = utf8mb4;

create index idx_labeler_daily_stats_date
    on labeler_daily_stats (stat_date);

create table llm_providers
(
    id                             bigint auto_increment
        primary key,
    provider_code                  varchar(64)                              not null,
    provider_name                  varchar(100)                             not null,
    base_url                       varchar(500)                             not null,
    encrypted_api_key              text                                     null,
    default_model                  varchar(128)                             not null,
    custom_headers_json            json                                     null,
    enabled                        tinyint(1)  default 1                    not null,
    platform_rate_limit_per_minute int                                      null,
    task_rate_limit_per_minute     int                                      null,
    user_rate_limit_per_minute     int                                      null,
    created_by                     bigint                                   null,
    created_at                     datetime(3) default CURRENT_TIMESTAMP(3) not null,
    updated_at                     datetime(3) default CURRENT_TIMESTAMP(3) not null on update CURRENT_TIMESTAMP(3),
    constraint uk_llm_providers_code
        unique (provider_code),
    constraint fk_llm_providers_created_by
        foreign key (created_by) references users (id)
)
    charset = utf8mb4;

create table ai_review_configs
(
    id                    bigint auto_increment
        primary key,
    task_id               bigint                                   not null,
    provider_id           bigint                                   null,
    model_name            varchar(128)                             null,
    enabled               tinyint(1)  default 1                    not null,
    prompt_template       mediumtext                               null,
    output_schema_json    json                                     null,
    dimension_config_json json                                     null,
    pass_threshold        decimal(5, 2)                            null,
    reject_threshold      decimal(5, 2)                            null,
    manual_threshold      decimal(5, 2)                            null,
    ai_reject_action      varchar(24) default 'SUGGEST_ONLY'       not null comment 'Whether AI reject only suggests, returns to labeler, or requires manual review.',
    max_retry             int         default 3                    not null,
    created_by            bigint                                   not null,
    created_at            datetime(3) default CURRENT_TIMESTAMP(3) not null,
    updated_at            datetime(3) default CURRENT_TIMESTAMP(3) not null on update CURRENT_TIMESTAMP(3),
    constraint fk_ai_review_configs_created_by
        foreign key (created_by) references users (id),
    constraint fk_ai_review_configs_provider
        foreign key (provider_id) references llm_providers (id),
    constraint chk_ai_review_configs_reject_action
        check (`ai_reject_action` in (_utf8mb4\'SUGGEST_ONLY\',_utf8mb4\'RETURN_TO_LABELER\',_utf8mb4\'MANUAL_REVIEW\'))
)
comment 'Per-task AI review prompt, thresholds, provider, and reject routing policy . ' charset=utf8mb4;

create index idx_ai_review_configs_task
    on ai_review_configs (task_id);

create index idx_llm_providers_enabled
    on llm_providers (enabled);

create table object_files
(
    id                bigint auto_increment
        primary key,
    owner_id          bigint                                   null,
    bucket_name       varchar(128)                             not null,
    object_key        varchar(512)                             not null,
    original_filename varchar(255)                             not null,
    content_type      varchar(128)                             null,
    file_size         bigint      default 0                    not null,
    checksum          varchar(128)                             null,
    storage_provider  varchar(32) default 'MINIO'              not null,
    created_at        datetime(3) default CURRENT_TIMESTAMP(3) not null,
    constraint uk_object_files_object
        unique (bucket_name, object_key),
    constraint fk_object_files_owner
        foreign key (owner_id) references users (id)
)
    charset = utf8mb4;

create index idx_object_files_owner
    on object_files (owner_id);

create table tasks
(
    id                            bigint auto_increment
        primary key,
    owner_id                      bigint                                   not null,
    title                         varchar(200)                             not null,
    description                   text                                     null,
    instruction_rich_text         mediumtext                               null,
    status                        varchar(20) default 'DRAFT'              not null,
    quota                         int                                      not null,
    claimed_count                 int         default 0                    not null,
    overlap_count                 int         default 1                    not null,
    deadline_at                   datetime(3)                              not null,
    published_template_version_id bigint                                   null,
    ai_review_config_id           bigint                                   null comment 'Current AI review config used when the task is published or submitted.',
    reward_visible                tinyint(1)  default 1                    not null,
    published_at                  datetime(3)                              null,
    ended_at                      datetime(3)                              null,
    created_at                    datetime(3) default CURRENT_TIMESTAMP(3) not null,
    updated_at                    datetime(3) default CURRENT_TIMESTAMP(3) not null on update CURRENT_TIMESTAMP(3),
    constraint fk_tasks_ai_review_config
        foreign key (ai_review_config_id) references ai_review_configs (id),
    constraint fk_tasks_owner
        foreign key (owner_id) references users (id),
    constraint chk_tasks_overlap
        check (`overlap_count` >= 1),
    constraint chk_tasks_quota
        check (`quota` > 0),
    constraint chk_tasks_status
        check (`status` in (_utf8mb4\'DRAFT\',_utf8mb4\'PUBLISHED\',_utf8mb4\'PAUSED\',_utf8mb4\'ENDED\'))
)
comment 'Task lifecycle and publish-time references.' charset=utf8mb4;

alter table ai_review_configs
    add constraint fk_ai_review_configs_task
        foreign key (task_id) references tasks (id);

create table dataset_files
(
    id          bigint auto_increment
        primary key,
    task_id     bigint                                   not null,
    file_id     bigint                                   not null,
    file_format varchar(20)                              not null,
    created_by  bigint                                   not null,
    created_at  datetime(3) default CURRENT_TIMESTAMP(3) not null,
    constraint fk_dataset_files_created_by
        foreign key (created_by) references users (id),
    constraint fk_dataset_files_file
        foreign key (file_id) references object_files (id),
    constraint fk_dataset_files_task
        foreign key (task_id) references tasks (id),
    constraint chk_dataset_files_format
        check (`file_format` in (_utf8mb4\'JSON\',_utf8mb4\'JSONL\',_utf8mb4\'EXCEL\',_utf8mb4\'CSV\'))
)
charset=utf8mb4;

create index idx_dataset_files_task
    on dataset_files (task_id);

create table dataset_import_jobs
(
    id                   bigint auto_increment
        primary key,
    task_id              bigint                                   not null,
    dataset_file_id      bigint                                   not null,
    status               varchar(20) default 'PENDING'            not null,
    import_mode          varchar(20) default 'APPEND'             not null,
    total_count          int         default 0                    not null,
    success_count        int         default 0                    not null,
    failed_count         int         default 0                    not null,
    error_report_file_id bigint                                   null,
    error_message        text                                     null,
    started_at           datetime(3)                              null,
    finished_at          datetime(3)                              null,
    created_by           bigint                                   not null,
    created_at           datetime(3) default CURRENT_TIMESTAMP(3) not null,
    constraint fk_import_jobs_created_by
        foreign key (created_by) references users (id),
    constraint fk_import_jobs_dataset_file
        foreign key (dataset_file_id) references dataset_files (id),
    constraint fk_import_jobs_error_file
        foreign key (error_report_file_id) references object_files (id),
    constraint fk_import_jobs_task
        foreign key (task_id) references tasks (id),
    constraint chk_import_jobs_mode
        check (`import_mode` in (_utf8mb4\'APPEND\',_utf8mb4\'OVERWRITE\')),
	constraint chk_import_jobs_status
		check (`status` in (_utf8mb4\'PENDING\',_utf8mb4\'RUNNING\',_utf8mb4\'SUCCESS\',_utf8mb4\'FAILED\',_utf8mb4\'PARTIAL_SUCCESS\'))
)
charset=utf8mb4;

create index idx_dataset_import_jobs_task
    on dataset_import_jobs (task_id, status);

create table dataset_items
(
    id              bigint auto_increment
        primary key,
    task_id         bigint                                   not null,
    external_id     varchar(128)                             not null,
    dataset_type    varchar(40)                              not null,
    item_json       json                                     not null comment 'Normalized dataset payload, independent from original JSON/JSONL/Excel file format.',
    metadata_json   json                                     null,
    assigned_count  int         default 0                    not null,
    submitted_count int         default 0                    not null,
    approved_count  int         default 0                    not null,
    deleted         tinyint(1)  default 0                    not null,
    created_at      datetime(3) default CURRENT_TIMESTAMP(3) not null,
    updated_at      datetime(3) default CURRENT_TIMESTAMP(3) not null on update CURRENT_TIMESTAMP(3),
    constraint uk_dataset_items_task_external
        unique (task_id, external_id),
    constraint fk_dataset_items_task
        foreign key (task_id) references tasks (id),
    constraint chk_dataset_items_type
        check (`dataset_type` in (_utf8mb4\'QA_QUALITY\',_utf8mb4\'PREFERENCE_COMPARE\'))
)
comment 'Dataset items available for labeler claim and review.' charset=utf8mb4;

create table dataset_item_change_logs
(
    id             bigint auto_increment
        primary key,
    task_id        bigint                                   not null,
    item_id        bigint                                   null,
    change_type    varchar(32)                              not null,
    before_json    json                                     null,
    after_json     json                                     null,
    json_patch     json                                     null,
    actor_id       bigint                                   not null,
    failure_reason text                                     null,
    created_at     datetime(3) default CURRENT_TIMESTAMP(3) not null,
    constraint fk_item_change_logs_actor
        foreign key (actor_id) references users (id),
    constraint fk_item_change_logs_item
        foreign key (item_id) references dataset_items (id),
    constraint fk_item_change_logs_task
        foreign key (task_id) references tasks (id)
)
    charset = utf8mb4;

create index idx_item_change_logs_item
    on dataset_item_change_logs (item_id);

create index idx_item_change_logs_task
    on dataset_item_change_logs (task_id, created_at);

create index idx_dataset_items_claim
    on dataset_items (task_id, deleted, assigned_count);

create index idx_dataset_items_type
    on dataset_items (task_id, dataset_type);

create table export_jobs
(
    id                     bigint auto_increment
        primary key,
    task_id                bigint                                   not null,
    requested_by           bigint                                   not null,
    export_format          varchar(20)                              not null,
    status                 varchar(20) default 'PENDING'            not null,
    include_ai_review      tinyint(1)  default 0                    not null,
    include_audit_trail    tinyint(1)  default 0                    not null,
    include_review_comment tinyint(1)  default 0                    not null,
    include_labeler_info   tinyint(1)  default 0                    not null,
    field_mapping_json     json                                     null comment 'Export field mapping array: source JSONPath, target name, formatter, and include flags.',
    result_file_id         bigint                                   null,
    download_url           varchar(1000)                            null,
    error_message          text                                     null,
    started_at             datetime(3)                              null,
    finished_at            datetime(3)                              null,
    created_at             datetime(3) default CURRENT_TIMESTAMP(3) not null,
    constraint fk_export_jobs_requested_by
        foreign key (requested_by) references users (id),
    constraint fk_export_jobs_result_file
        foreign key (result_file_id) references object_files (id),
    constraint fk_export_jobs_task
        foreign key (task_id) references tasks (id),
    constraint chk_export_jobs_format
        check (`export_format` in (_utf8mb4\'JSON\',_utf8mb4\'JSONL\',_utf8mb4\'CSV\',_utf8mb4\'EXCEL\')),
	constraint chk_export_jobs_status
		check (`status` in (_utf8mb4\'PENDING\',_utf8mb4\'RUNNING\',_utf8mb4\'SUCCESS\',_utf8mb4\'FAILED\'))
)
comment 'Async export configuration and output file metadata.' charset=utf8mb4;

create index idx_export_jobs_requested_by
    on export_jobs (requested_by, created_at);

create index idx_export_jobs_status
    on export_jobs (status);

create index idx_export_jobs_task
    on export_jobs (task_id, created_at);

create table labeler_task_stats
(
    id              bigint auto_increment
        primary key,
    labeler_id      bigint                                      not null,
    task_id         bigint                                      not null,
    claimed_count   int            default 0                    not null,
    submitted_count int            default 0                    not null,
    approved_count  int            default 0                    not null,
    rejected_count  int            default 0                    not null,
    total_reward    decimal(14, 2) default 0.00                 not null,
    updated_at      datetime(3)    default CURRENT_TIMESTAMP(3) not null on update CURRENT_TIMESTAMP(3),
    constraint uk_labeler_task_stats
        unique (labeler_id, task_id),
    constraint fk_labeler_task_stats_labeler
        foreign key (labeler_id) references users (id),
    constraint fk_labeler_task_stats_task
        foreign key (task_id) references tasks (id)
)
    charset = utf8mb4;

create index idx_labeler_task_stats_task
    on labeler_task_stats (task_id);

create table reward_rules
(
    id                bigint auto_increment
        primary key,
    task_id           bigint                                      not null,
    effective_version int                                         not null comment 'Reward rule version effective for newly approved data; historical ledgers are not rewritten.',
    reward_mode       varchar(32)    default 'APPROVED_ITEM'      not null,
    unit_reward       decimal(12, 2) default 0.00                 not null,
    reward_currency   varchar(32)    default 'POINT'              not null,
    reward_visible    tinyint(1)     default 1                    not null,
    effective_at      datetime(3)    default CURRENT_TIMESTAMP(3) not null,
    created_by        bigint                                      not null,
    created_at        datetime(3)    default CURRENT_TIMESTAMP(3) not null,
    constraint uk_reward_rules_task_version
        unique (task_id, effective_version),
    constraint fk_reward_rules_created_by
        foreign key (created_by) references users (id),
    constraint fk_reward_rules_task
        foreign key (task_id) references tasks (id),
    constraint chk_reward_rules_effective_version
        check (`effective_version` >= 1),
    constraint chk_reward_rules_mode
        check (`reward_mode` = _utf8mb4\'APPROVED_ITEM\'),
	constraint chk_reward_rules_unit_reward
		check (`unit_reward` >= 0)
)
comment ' Versioned virtual reward configuration for a task.' charset=utf8mb4;

create index idx_reward_rules_task
    on reward_rules (task_id);

create table task_stats
(
    task_id              bigint                                   not null
        primary key,
    item_count           int         default 0                    not null,
    assigned_count       int         default 0                    not null,
    submitted_count      int         default 0                    not null,
    pending_review_count int         default 0                    not null,
    approved_count       int         default 0                    not null,
    rejected_count       int         default 0                    not null,
    conflict_count       int         default 0                    not null,
    updated_at           datetime(3) default CURRENT_TIMESTAMP(3) not null on update CURRENT_TIMESTAMP(3),
    constraint fk_task_stats_task
        foreign key (task_id) references tasks (id)
)
    charset = utf8mb4;

create table task_tags
(
    id         bigint auto_increment
        primary key,
    task_id    bigint                                   not null,
    tag_name   varchar(64)                              not null,
    created_at datetime(3) default CURRENT_TIMESTAMP(3) not null,
    constraint uk_task_tags_task_tag
        unique (task_id, tag_name),
    constraint fk_task_tags_task
        foreign key (task_id) references tasks (id)
)
    charset = utf8mb4;

create index idx_task_tags_tag_task
    on task_tags (tag_name, task_id);

create fulltext index ft_tasks_search
    on tasks (title, description);

create index idx_tasks_ai_review_config
    on tasks (ai_review_config_id);

create index idx_tasks_owner_status
    on tasks (owner_id, status);

create index idx_tasks_status_deadline
    on tasks (status, deadline_at);

create index idx_tasks_template_version
    on tasks (published_template_version_id);

create table templates
(
    id                 bigint auto_increment
        primary key,
    task_id            bigint                                   not null,
    name               varchar(200)                             not null,
    current_version_no int         default 0                    not null,
    created_by         bigint                                   not null,
    created_at         datetime(3) default CURRENT_TIMESTAMP(3) not null,
    updated_at         datetime(3) default CURRENT_TIMESTAMP(3) not null on update CURRENT_TIMESTAMP(3),
    constraint fk_templates_created_by
        foreign key (created_by) references users (id),
    constraint fk_templates_task
        foreign key (task_id) references tasks (id)
)
    charset = utf8mb4;

create table template_versions
(
    id                 bigint auto_increment
        primary key,
    template_id        bigint                                   not null,
    task_id            bigint                                   not null,
    version_no         int                                      not null,
    schema_json        json                                     not null comment 'Frozen renderer schema: layout, components, ShowItem bindings, LlmTrigger config, and validation rules.',
    published_snapshot tinyint(1)  default 0                    not null,
    change_note        varchar(500)                             null,
    created_by         bigint                                   not null,
    created_at         datetime(3) default CURRENT_TIMESTAMP(3) not null,
    constraint uk_template_versions_template_version
        unique (template_id, version_no),
    constraint fk_template_versions_created_by
        foreign key (created_by) references users (id),
    constraint fk_template_versions_task
        foreign key (task_id) references tasks (id),
    constraint fk_template_versions_template
        foreign key (template_id) references templates (id)
)
    comment 'Immutable template schema versions used by owner preview, labeler rendering, and backend validation.'
    charset = utf8mb4;

create table assignments
(
    id                  bigint auto_increment
        primary key,
    task_id             bigint                                   not null,
    dataset_item_id     bigint                                   not null,
    labeler_id          bigint                                   not null,
    template_version_id bigint                                   not null,
    status              varchar(20) default 'CLAIMED'            not null,
    draft_answer_json   json                                     null comment 'Latest server-side draft answer; Redis may cache the same draft for faster autosave.',
    draft_version       int         default 1                    not null,
    claimed_at          datetime(3) default CURRENT_TIMESTAMP(3) not null,
    submitted_at        datetime(3)                              null,
    returned_at         datetime(3)                              null,
    ai_returned_at      datetime(3)                              null,
    approved_at         datetime(3)                              null,
    cancelled_at        datetime(3)                              null,
    updated_at          datetime(3) default CURRENT_TIMESTAMP(3) not null on update CURRENT_TIMESTAMP(3),
    constraint uk_assignments_item_labeler
        unique (dataset_item_id, labeler_id),
    constraint fk_assignments_item
        foreign key (dataset_item_id) references dataset_items (id),
    constraint fk_assignments_labeler
        foreign key (labeler_id) references users (id),
    constraint fk_assignments_task
        foreign key (task_id) references tasks (id),
    constraint fk_assignments_template_version
        foreign key (template_version_id) references template_versions (id),
    constraint chk_assignments_status
        check (`status` in (_utf8mb4\'CLAIMED\',_utf8mb4\'DRAFTING\',_utf8mb4\'SUBMITTED\',_utf8mb4\'AI_RETURNED\',_utf8mb4\'RETURNED\',_utf8mb4\'APPROVED\',_utf8mb4\'CANCELLED\'))
)
comment 'Claim record and draft state for one labeler on one dataset item.' charset=utf8mb4;

create index idx_assignments_labeler_status
    on assignments (labeler_id, status);

create index idx_assignments_task_status
    on assignments (task_id, status);

create table submissions
(
    id                     bigint auto_increment
        primary key,
    assignment_id          bigint                                   not null,
    task_id                bigint                                   not null,
    dataset_item_id        bigint                                   not null,
    labeler_id             bigint                                   not null,
    template_version_id    bigint                                   not null,
    version_no             int                                      not null,
    answer_json            json                                     not null,
    answer_hash            char(64)                                 not null,
    status                 varchar(20)                              not null,
    conflict_status        varchar(24) default 'NONE'               not null,
    current_review_level   int         default 1                    not null comment 'Current human review stage: 1 initial review, 2 second review, 3 final review.',
    review_flow_status     varchar(24) default 'UNASSIGNED'         not null comment 'Human review queue state, separate from submission business status.',
    assigned_reviewer_id   bigint                                   null comment 'Current reviewer owner for "assigned to me" filtering; history is stored in review_tasks/review_records.',
    review_version         int         default 1                    not null comment 'Optimistic lock version for reviewer state transitions.',
    is_golden              tinyint(1)  default 0                    not null,
    golden_dataset_item_id bigint as ((case when (`is_golden` = 1) then `dataset_item_id` else NULL end)) stored,
    submitted_at           datetime(3) default CURRENT_TIMESTAMP(3) not null,
    updated_at             datetime(3) default CURRENT_TIMESTAMP(3) not null on update CURRENT_TIMESTAMP(3),
    constraint uk_submissions_assignment_version
        unique (assignment_id, version_no),
    constraint uk_submissions_golden_item
        unique (golden_dataset_item_id),
    constraint fk_submissions_assigned_reviewer
        foreign key (assigned_reviewer_id) references users (id),
    constraint fk_submissions_assignment
        foreign key (assignment_id) references assignments (id),
    constraint fk_submissions_item
        foreign key (dataset_item_id) references dataset_items (id),
    constraint fk_submissions_labeler
        foreign key (labeler_id) references users (id),
    constraint fk_submissions_task
        foreign key (task_id) references tasks (id),
    constraint fk_submissions_template_version
        foreign key (template_version_id) references template_versions (id),
    constraint chk_submissions_conflict_status
        check (`conflict_status` in (_utf8mb4\'NONE\',_utf8mb4\'CONSENSUS_REACHED\',_utf8mb4\'CONFLICTED\',_utf8mb4\'RESOLVED\')),
	constraint chk_submissions_review_flow
		check (`review_flow_status` in (_utf8mb4\'UNASSIGNED\',_utf8mb4\'ASSIGNED\',_utf8mb4\'IN_REVIEW\',_utf8mb4\'LEVEL_APPROVED\',_utf8mb4\'FINAL_APPROVED\',_utf8mb4\'REJECTED\',_utf8mb4\'CANCELLED\')),
	constraint chk_submissions_review_level
		check (`current_review_level` between 1 and 3),
	constraint chk_submissions_review_version
		check (`review_version` >= 1),
	constraint chk_submissions_status
		check (`status` in (_utf8mb4\'SUBMITTED\',_utf8mb4\'AI_REVIEWING\',_utf8mb4\'AI_REJECTED\',_utf8mb4\'PENDING_FINAL\',_utf8mb4\'APPROVED\',_utf8mb4\'REJECTED\',_utf8mb4\'SUPERSEDED\'))
)
comment 'Immutable submission versions and current review routing state.' charset=utf8mb4;

create table agent_runs
(
    id              bigint auto_increment
        primary key,
    agent_type      varchar(32)                              not null,
    submission_id   bigint                                   null,
    provider_id     bigint                                   null,
    model_name      varchar(128)                             null,
    prompt_version  varchar(64)                              null,
    input_snapshot  json                                     null comment 'Prompt input snapshot for traceability; do not reconstruct it from mutable business tables.',
    output_snapshot json                                     null comment 'Raw or normalized model output for this concrete agent execution.',
    status          varchar(24) default 'PENDING'            not null,
    error_message   text                                     null,
    started_at      datetime(3)                              null,
    finished_at     datetime(3)                              null,
    created_at      datetime(3) default CURRENT_TIMESTAMP(3) not null,
    constraint fk_agent_runs_provider
        foreign key (provider_id) references llm_providers (id),
    constraint fk_agent_runs_submission
        foreign key (submission_id) references submissions (id),
    constraint chk_agent_runs_status
        check (`status` in (_utf8mb4\'PENDING\',_utf8mb4\'RUNNING\',_utf8mb4\'SUCCESS\',_utf8mb4\'FAILED\',_utf8mb4\'RATE_LIMITED\',_utf8mb4\'MANUAL_REQUIRED\')),
	constraint chk_agent_runs_type
		check (`agent_type` in (_utf8mb4\'AI_REVIEW\',_utf8mb4\'LLM_TRIGGER\'))
)
comment 'Concrete AI/LLM execution attempts, including retries and failures.' charset=utf8mb4;

create index idx_agent_runs_status
    on agent_runs (status, created_at);

create index idx_agent_runs_submission
    on agent_runs (submission_id, created_at);

create table ai_review_results
(
    id               bigint auto_increment
        primary key,
    submission_id    bigint                                   not null,
    effective_run_id bigint                                   null comment 'The agent run whose output is currently effective for this AI review result.',
    provider_id      bigint                                   null,
    model_name       varchar(128)                             null,
    status           varchar(24) default 'PENDING'            not null,
    decision         varchar(24)                              null,
    average_score    decimal(6, 3)                            null,
    dimension_scores json                                     null,
    risk_flags       json                                     null,
    suggestion       text                                     null,
    prompt_snapshot  mediumtext                               null comment 'Final prompt sent to the provider for audit display.',
    raw_response     mediumtext                               null comment 'Original provider response retained for troubleshooting and review transparency.',
    retry_count      int         default 0                    not null,
    created_at       datetime(3) default CURRENT_TIMESTAMP(3) not null,
    updated_at       datetime(3) default CURRENT_TIMESTAMP(3) not null on update CURRENT_TIMESTAMP(3),
    constraint uk_ai_review_results_effective_run
        unique (effective_run_id),
    constraint uk_ai_review_results_submission
        unique (submission_id),
    constraint fk_ai_review_results_effective_run
        foreign key (effective_run_id) references agent_runs (id),
    constraint fk_ai_review_results_provider
        foreign key (provider_id) references llm_providers (id),
    constraint fk_ai_review_results_submission
        foreign key (submission_id) references submissions (id),
    constraint chk_ai_review_results_decision
        check ((`decision` is null) or (`decision` in (_utf8mb4\'PASS\',_utf8mb4\'REJECT\',_utf8mb4\'MANUAL_REVIEW\'))),
	constraint chk_ai_review_results_status
		check (`status` in (_utf8mb4\'PENDING\',_utf8mb4\'RUNNING\',_utf8mb4\'SUCCESS\',_utf8mb4\'FAILED\',_utf8mb4\'RATE_LIMITED\',_utf8mb4\'MANUAL_REQUIRED\'))
)
comment 'Business-level AI review conclusion for a submission;
agent_runs stores execution details.' charset=utf8mb4;

create index idx_ai_review_results_decision_status
    on ai_review_results (decision, status);

create index idx_ai_review_results_status
    on ai_review_results (status);

create table audit_logs
(
    id           bigint auto_increment
        primary key,
    biz_type     varchar(64)                              not null,
    biz_id       bigint                                   not null,
    actor_type   varchar(32)                              not null,
    actor_id     bigint                                   null,
    action       varchar(64)                              not null,
    before_json  json                                     null comment 'Business object snapshot before the action when available.',
    after_json   json                                     null comment 'Business object snapshot after the action when available.',
    trace_id     varchar(128)                             null,
    agent_run_id bigint                                   null,
    created_at   datetime(3) default CURRENT_TIMESTAMP(3) not null,
    constraint fk_audit_logs_actor
        foreign key (actor_id) references users (id),
    constraint fk_audit_logs_agent_run
        foreign key (agent_run_id) references agent_runs (id),
    constraint chk_audit_logs_actor_type
        check (`actor_type` in (_utf8mb4\'USER\',_utf8mb4\'SYSTEM_AGENT\'))
)
comment 'Append-only audit trail for state transitions and critical business operations.' charset=utf8mb4;

create index idx_audit_logs_actor
    on audit_logs (actor_id, created_at);

create index idx_audit_logs_agent_run
    on audit_logs (agent_run_id, created_at);

create index idx_audit_logs_biz
    on audit_logs (biz_type, biz_id, created_at);

create index idx_audit_logs_trace
    on audit_logs (trace_id);

create table conflict_groups
(
    id                   bigint auto_increment
        primary key,
    task_id              bigint                                   not null,
    dataset_item_id      bigint                                   not null,
    status               varchar(24) default 'CONFLICTED'         not null,
    consensus_score      decimal(6, 3)                            null comment 'Calculated agreement score across overlapping submissions.',
    golden_submission_id bigint                                   null comment 'Reviewer-selected golden submission for conflicted or overlapping labels.',
    resolved_by          bigint                                   null,
    resolved_reason      text                                     null,
    resolved_at          datetime(3)                              null,
    created_at           datetime(3) default CURRENT_TIMESTAMP(3) not null,
    updated_at           datetime(3) default CURRENT_TIMESTAMP(3) not null on update CURRENT_TIMESTAMP(3),
    constraint uk_conflict_groups_task_item
        unique (task_id, dataset_item_id),
    constraint fk_conflict_groups_golden_submission
        foreign key (golden_submission_id) references submissions (id),
    constraint fk_conflict_groups_item
        foreign key (dataset_item_id) references dataset_items (id),
    constraint fk_conflict_groups_resolved_by
        foreign key (resolved_by) references users (id),
    constraint fk_conflict_groups_task
        foreign key (task_id) references tasks (id),
    constraint chk_conflict_groups_status
        check (`status` in (_utf8mb4\'NONE\',_utf8mb4\'CONSENSUS_REACHED\',_utf8mb4\'CONFLICTED\',_utf8mb4\'RESOLVED\'))
)
comment 'Conflict and consensus resolution for multi-labeler overlap.' charset=utf8mb4;

create index idx_conflict_groups_status
    on conflict_groups (status);

create table review_tasks
(
    id                   bigint auto_increment
        primary key,
    submission_id        bigint                                   not null,
    task_id              bigint                                   not null,
    review_level         int         default 1                    not null,
    assigned_reviewer_id bigint                                   not null,
    assigned_by          bigint                                   null,
    status               varchar(24) default 'PENDING'            not null,
    review_version       int         default 1                    not null comment 'Optimistic lock version for concurrent reviewer actions.',
    assigned_at          datetime(3) default CURRENT_TIMESTAMP(3) not null,
    started_at           datetime(3)                              null,
    completed_at         datetime(3)                              null,
    due_at               datetime(3)                              null,
    created_at           datetime(3) default CURRENT_TIMESTAMP(3) not null,
    updated_at           datetime(3) default CURRENT_TIMESTAMP(3) not null on update CURRENT_TIMESTAMP(3),
    constraint uk_review_tasks_submission_level
        unique (submission_id, review_level),
    constraint fk_review_tasks_assigned_by
        foreign key (assigned_by) references users (id),
    constraint fk_review_tasks_assigned_reviewer
        foreign key (assigned_reviewer_id) references users (id),
    constraint fk_review_tasks_submission
        foreign key (submission_id) references submissions (id),
    constraint fk_review_tasks_task
        foreign key (task_id) references tasks (id),
    constraint chk_review_tasks_level
        check (`review_level` between 1 and 3),
    constraint chk_review_tasks_status
        check (`status` in (_utf8mb4\'PENDING\',_utf8mb4\'IN_REVIEW\',_utf8mb4\'APPROVED\',_utf8mb4\'REJECTED\',_utf8mb4\'TRANSFERRED\',_utf8mb4\'CANCELLED\')),
	constraint chk_review_tasks_version
		check (`review_version` >= 1)
)
comment 'Reviewer work queue for batch assignment and level 1/2/3 review flow.' charset=utf8mb4;

create table review_records
(
    id                 bigint auto_increment
        primary key,
    submission_id      bigint                                   not null,
    review_task_id     bigint                                   null comment 'Review queue item this action belongs to; nullable for legacy/direct audit actions.',
    reviewer_id        bigint                                   not null,
    target_reviewer_id bigint                                   null comment 'Reviewer assigned by ASSIGN_REVIEWER action; reviewer_id is the actor who performed the assignment.',
    action             varchar(32)                              not null,
    review_level       int         default 1                    not null,
    comment            text                                     null,
    reason             text                                     null,
    before_status      varchar(20)                              null,
    after_status       varchar(20)                              null,
    created_at         datetime(3) default CURRENT_TIMESTAMP(3) not null,
    constraint fk_review_records_review_task
        foreign key (review_task_id) references review_tasks (id),
    constraint fk_review_records_reviewer
        foreign key (reviewer_id) references users (id),
    constraint fk_review_records_submission
        foreign key (submission_id) references submissions (id),
    constraint fk_review_records_target_reviewer
        foreign key (target_reviewer_id) references users (id),
    constraint chk_review_records_action
        check (`action` in (_utf8mb4\'APPROVE\',_utf8mb4\'REJECT\',_utf8mb4\'RESOLVE_CONFLICT\',_utf8mb4\'MARK_MANUAL_REQUIRED\',_utf8mb4\'ASSIGN_REVIEWER\'))
)
comment 'Append-only human review actions and assignment audit records.' charset=utf8mb4;

create index idx_review_records_reviewer
    on review_records (reviewer_id, created_at);

create index idx_review_records_submission
    on review_records (submission_id, created_at);

create index idx_review_records_target_reviewer
    on review_records (target_reviewer_id, created_at);

create index idx_review_tasks_reviewer_status
    on review_tasks (assigned_reviewer_id, status, due_at);

create index idx_review_tasks_task_level
    on review_tasks (task_id, review_level, status);

create table reward_ledger
(
    id                     bigint auto_increment
        primary key,
    task_id                bigint                                    not null,
    labeler_id             bigint                                    not null,
    submission_id          bigint                                    not null,
    assignment_id          bigint                                    not null,
    amount                 decimal(12, 2)                            not null,
    direction              varchar(16)                               not null,
    reason                 varchar(255)                              not null,
    source_event_id        varchar(128)                              not null,
    reward_type            varchar(32) default 'SUBMISSION_APPROVED' not null,
    positive_submission_id bigint as ((case
                                           when (`direction` = _utf8mb4'CREDIT') then `submission_id`
                                           else NULL end)) stored comment 'Idempotency key: one positive reward per submission.',
    positive_assignment_id bigint as ((case
                                           when (`direction` = _utf8mb4'CREDIT') then `assignment_id`
                                           else NULL end)) stored comment 'Idempotency key: one effective positive reward per assignment across versions.',
    created_at             datetime(3) default CURRENT_TIMESTAMP(3)  not null,
    constraint uk_reward_ledger_event
        unique (source_event_id),
    constraint uk_reward_ledger_positive_assignment
        unique (positive_assignment_id),
    constraint uk_reward_ledger_positive_submission
        unique (positive_submission_id),
    constraint fk_reward_ledger_assignment
        foreign key (assignment_id) references assignments (id),
    constraint fk_reward_ledger_labeler
        foreign key (labeler_id) references users (id),
    constraint fk_reward_ledger_submission
        foreign key (submission_id) references submissions (id),
    constraint fk_reward_ledger_task
        foreign key (task_id) references tasks (id),
    constraint chk_reward_ledger_amount
        check (`amount` > 0),
    constraint chk_reward_ledger_direction
        check (`direction` in (_utf8mb4\'CREDIT\',_utf8mb4\'DEBIT\')),
	constraint chk_reward_ledger_type
		check (`reward_type` in (_utf8mb4\'SUBMISSION_APPROVED\',_utf8mb4\'GOLDEN_SELECTED\',_utf8mb4\'REWARD_REVERSED\'))
)
comment 'Append-only virtual reward ledger;
reversals are negative rows, not updates.' charset=utf8mb4;

create index idx_reward_ledger_labeler
    on reward_ledger (labeler_id, created_at);

create index idx_reward_ledger_task
    on reward_ledger (task_id, created_at);

create index idx_submissions_assigned_reviewer
    on submissions (assigned_reviewer_id, review_flow_status, status);

create index idx_submissions_export
    on submissions (task_id, status, is_golden, submitted_at);

create index idx_submissions_labeler
    on submissions (labeler_id, status);

create index idx_submissions_labeler_time
    on submissions (labeler_id, submitted_at);

create index idx_submissions_review
    on submissions (status, conflict_status, submitted_at);

create index idx_submissions_task_item
    on submissions (task_id, dataset_item_id);

create index idx_submissions_task_review
    on submissions (task_id, status, conflict_status, submitted_at);

alter table tasks
    add constraint fk_tasks_published_template_version
        foreign key (published_template_version_id) references template_versions (id);

create index idx_template_versions_task
    on template_versions (task_id);

create index idx_templates_task
    on templates (task_id);

create table user_roles
(
    id         bigint auto_increment
        primary key,
    user_id    bigint                                   not null,
    role_code  varchar(32)                              not null,
    created_at datetime(3) default CURRENT_TIMESTAMP(3) not null,
    constraint uk_user_roles_user_role
        unique (user_id, role_code),
    constraint fk_user_roles_user
        foreign key (user_id) references users (id),
    constraint chk_user_roles_code
        check (`role_code` in (_utf8mb4\'ADMIN\',_utf8mb4\'OWNER\',_utf8mb4\'LABELER\',_utf8mb4\'REVIEWER\',_utf8mb4\'SYSTEM_AGENT\'))
)
charset=utf8mb4;

create index idx_user_roles_role
    on user_roles (role_code);

create index idx_users_enabled
    on users (enabled);
