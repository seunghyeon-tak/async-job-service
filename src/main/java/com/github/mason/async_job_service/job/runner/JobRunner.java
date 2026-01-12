package com.github.mason.async_job_service.job.runner;

import com.github.mason.async_job_service.job.application.JobExecutorService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class JobRunner {
    private final JobExecutorService jobExecutorService;

    @Scheduled(fixedDelay = 1000)
    public void run() {
        jobExecutorService.runBatch();
    }
}
