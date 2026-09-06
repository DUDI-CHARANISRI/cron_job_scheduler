CREATE INDEX IF NOT EXISTS idx_job_audit_logs_job_name_executed_at
    ON job_audit_logs (job_name, executed_at DESC);