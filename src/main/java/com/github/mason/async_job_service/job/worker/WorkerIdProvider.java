package com.github.mason.async_job_service.job.worker;

import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class WorkerIdProvider {
    private final String workerId = "worker-" + UUID.randomUUID();

    public String getWorkerId() {
        return workerId;
    }
}
