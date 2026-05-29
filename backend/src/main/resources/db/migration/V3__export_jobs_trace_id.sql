alter table export_jobs
    add column trace_id varchar(128) null after error_message;
