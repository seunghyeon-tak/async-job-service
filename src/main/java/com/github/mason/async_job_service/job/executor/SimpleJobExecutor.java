package com.github.mason.async_job_service.job.executor;

import com.github.mason.async_job_service.db.domain.Job;
import org.springframework.stereotype.Component;

@Component
public class SimpleJobExecutor implements JobExecutor {

    @Override
    public void execute(Job job) {
        System.out.println("Executing job id = " + job.getId());
        System.out.println("type = " + job.getType());
        System.out.println("payload = " + job.getPayload());
    }
}
