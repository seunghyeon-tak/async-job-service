package com.github.mason.async_job_service.job.application;

import com.github.mason.async_job_service.db.domain.Job;
import com.github.mason.async_job_service.db.repository.JobRepository;
import com.github.mason.async_job_service.job.executor.JobExecutor;
import com.github.mason.async_job_service.job.worker.WorkerIdProvider;
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
    private static final int LOCK_EXPIRES_MIN = 5;

    private final JobRepository jobRepository;
    private final JobExecutor jobExecutor;
    private final WorkerIdProvider workerIdProvider;

    public void runBatch() {
        LocalDateTime now = LocalDateTime.now();
        List<Job> runnableJobs = jobRepository.findRunnableJobs(PENDING, now);

        if (runnableJobs.isEmpty()) return;

        Job job = runnableJobs.get(0);
        String owner = workerIdProvider.getWorkerId();

        job.markRunning(now, owner, now.plusMinutes(LOCK_EXPIRES_MIN));
        jobRepository.save(job);

        try {
            jobExecutor.execute(job);
            // 성공했을때
            job.markSuccess();

        } catch (Exception e) {
            /***
             * failedAt을 추가한 이유 execute()가 오래 걸렸다가 실패하면
             * runBatch 시작할때의 시간으로부터 되어버려서
             * 이미 시간이 지나버릴 수 있기때문에
             * ***/
            // 실패했을때
            LocalDateTime failedAt = LocalDateTime.now();
            job.markFailure(
                    e.getMessage(),
                    MAX_RETRIES,
                    failedAt.plusSeconds(RETRY_DELAY_SECONDS)
            );
        }

        jobRepository.save(job);
    }
}
