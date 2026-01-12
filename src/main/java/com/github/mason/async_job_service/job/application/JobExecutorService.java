package com.github.mason.async_job_service.job.application;

import com.github.mason.async_job_service.db.domain.Job;
import com.github.mason.async_job_service.db.repository.JobRepository;
import com.github.mason.async_job_service.job.executor.JobExecutor;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static com.github.mason.async_job_service.db.enums.JobStatus.PENDING;

@Service
@Transactional
@RequiredArgsConstructor
public class JobExecutorService {
    private static final int MAX_RETRIES = 3;
    private static final int RETRY_DELAY_SECONDS = 10;

    private final JobRepository jobRepository;
    private final JobExecutor jobExecutor;

    public void runBatch() {
        LocalDateTime now = LocalDateTime.now();
        List<Job> runnableJobs = jobRepository.findRunnableJobs(PENDING, now);

        if (runnableJobs.isEmpty()) return;

        Job job = runnableJobs.get(0);

        job.markRunning(now);
        jobRepository.save(job);

        try {
            jobExecutor.execute(job);
            // 성공했을때
            job.markSuccess();

        } catch (Exception e) {
            // 실패했을때
            job.markFailure(e.getMessage(),
                    MAX_RETRIES,
                    now.plusSeconds(RETRY_DELAY_SECONDS)
            );
        }

        jobRepository.save(job);
    }
}
