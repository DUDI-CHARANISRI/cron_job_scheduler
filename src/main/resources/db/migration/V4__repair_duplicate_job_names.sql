DELETE FROM scheduled_jobs duplicate_job
WHERE EXISTS (
    SELECT 1
    FROM scheduled_jobs original_job
    WHERE LOWER(original_job.name) = LOWER(duplicate_job.name)
      AND original_job.id < duplicate_job.id
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_scheduled_jobs_name
  ON scheduled_jobs (name);
