package com.github.mason.async_job_service.job.application;

import com.github.mason.async_job_service.db.domain.Job;
import com.github.mason.async_job_service.db.enums.JobStatus;
import com.github.mason.async_job_service.db.repository.JobRepository;
import com.github.mason.async_job_service.job.executor.JobExecutor;
import com.github.mason.async_job_service.job.worker.WorkerIdProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@SpringBootTest
@ActiveProfiles("test")
public class JobExecutorServiceIntegrationTest {
    @Autowired
    JobRepository jobRepository;

    @Autowired
    JobExecutorService jobExecutorService;

    @MockitoBean
    private JobExecutor jobExecutor;

    @MockitoBean
    private WorkerIdProvider workerIdProvider;

    @BeforeEach
    void setUp() {
        jobRepository.deleteAll();
        when(workerIdProvider.getWorkerId()).thenReturn("worker-test");

        doNothing().when(jobExecutor).execute(any(Job.class));
    }

    @Test
    void runBatch는_PENDING을_선점해_실행후_SUCCEEDED로_만든다() {
        when(workerIdProvider.getWorkerId()).thenReturn("worker-test");
        doNothing().when(jobExecutor).execute(any(Job.class));

        Job pending = jobRepository.save(Job.builder()
                .type("TEST")
                .payload("{}")
                .status(JobStatus.PENDING)
                .nextRunAt(null)
                .retryCount(0)
                .build());

        Long pendingId = pending.getId();

        jobExecutorService.runBatch();

        Job after = jobRepository.findById(pendingId).orElseThrow();

        System.out.println("DEBUG lockOwner=" + after.getLockOwner()
                + ", status=" + after.getStatus()
                + ", lockExpiresAt=" + after.getLockExpiresAt());

        assertThat(after.getStatus()).isEqualTo(JobStatus.SUCCEEDED);
        assertThat(after.getLockedAt()).isNull();
        assertThat(after.getLockOwner()).isNull();
        assertThat(after.getLockExpiresAt()).isNull();
        assertThat(after.getLastError()).isNull();

        verify(jobExecutor, atLeastOnce()).execute(any(Job.class));
    }
}
