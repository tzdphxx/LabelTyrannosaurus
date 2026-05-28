alter table dataset_items
    drop index uk_dataset_items_task_external;

alter table dataset_items
    add column active_external_id varchar(128)
        generated always as ((case when (`deleted` = 0) then `external_id` else NULL end)) stored
        after deleted;

alter table dataset_items
    add constraint uk_dataset_items_task_external
        unique (task_id, active_external_id);
