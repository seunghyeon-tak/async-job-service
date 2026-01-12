package com.github.mason.async_job_service.job.executor;

import com.github.mason.async_job_service.db.domain.Job;

public interface JobExecutor {
    void execute(Job job);
}
