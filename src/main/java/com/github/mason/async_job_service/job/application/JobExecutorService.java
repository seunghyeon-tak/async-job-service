package com.github.mason.async_job_service.job.application;

import com.github.mason.async_job_service.db.domain.Job;
import com.github.mason.async_job_service.db.repository.JobRepository;
import com.github.mason.async_job_service.job.executor.JobExecutor;
import com.github.mason.async_job_service.job.worker.WorkerIdProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class JobExecutorService {
    private static final int MAX_RETRIES = 3;
    private static final int RETRY_DELAY_SECONDS = 10;
    private static final int LOCK_EXPIRES_MIN = 5;
    private static final int BATCH_SIZE = 10;

    private final JobRepository jobRepository;
    private final JobExecutor jobExecutor;
    private final WorkerIdProvider workerIdProvider;

    public void runBatch() {
        LocalDateTime now = LocalDateTime.now();
        String owner = workerIdProvider.getWorkerId();

        List<Job> claimed = claimBatch(now, owner);

        for (Job job : claimed) {
            executeOne(job.getId());
        }

    }

    @Transactional
    public List<Job> claimBatch(LocalDateTime now, String owner) {
        LocalDateTime expiresAt = now.plusMinutes(LOCK_EXPIRES_MIN);
        jobRepository.claimPendingJobs(now, owner, expiresAt, BATCH_SIZE);

        return jobRepository.findClaimedJobs(owner, now);
    }

    @Transactional
    public void executeOne(Long jobId) {
        Job job = jobRepository.findById(jobId).orElseThrow();

        try {
            // 성공했을때
            jobExecutor.execute(job);
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
    }

    // running 상태에서 머무는 job을 감지해서 자동 실행 가능 상태로 복구하는 로직
    public int recoverStaleRunningJobs(LocalDateTime now, int limit) {
        List<Job> staleJobs = jobRepository.findStaleJobs(
                now,
                PageRequest.of(0, limit)
        );

        for (Job job : staleJobs) {
            job.recoverToPending();
        }

        jobRepository.saveAll(staleJobs);

        return staleJobs.size();
    }
}
