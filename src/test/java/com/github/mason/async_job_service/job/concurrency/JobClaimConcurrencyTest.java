package com.github.mason.async_job_service.job.concurrency;

import com.github.mason.async_job_service.db.domain.Job;
import com.github.mason.async_job_service.db.repository.JobRepository;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static com.github.mason.async_job_service.db.enums.JobStatus.PENDING;
import static com.github.mason.async_job_service.db.enums.JobStatus.RUNNING;
import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
public class JobClaimConcurrencyTest {

    @Autowired
    JobRepository jobRepository;

    @Test
    void 동시에_claim을_시도하면_하나만_선점된다() throws Exception {
        // 준비
        /*
         * db에 pending job 1개 저장
         * now, expiresAt 같은 기준 시간은 1번만 생성해서 공유
         * owner 2개 준비 (worker-1, worker-2)
         * */
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expiresAt = now.plusMinutes(5);

        Job job = Job.builder()
                .type("TEST")
                .payload("{}")
                .status(PENDING)
                .retryCount(0)
                .nextRunAt(now.minusSeconds(1))
                .build();

        jobRepository.save(job);

        String owner1 = "worker-1";
        String owner2 = "worker-2";

        ExecutorService pool = Executors.newFixedThreadPool(2);
        CountDownLatch startGate = new CountDownLatch(1);
        CountDownLatch doneGate = new CountDownLatch(2);

        AtomicInteger r1 = new AtomicInteger(-1);
        AtomicInteger r2 = new AtomicInteger(-1);

        // 동시 실행
        /*
         * 스레드 2개를 동시에 시작
         * 둘 다 claimpendingjobs() 호출
         * 반환값(int)을 각각 저장
         * */
        pool.submit(() -> {
            await(startGate);
            try {
                r1.set(jobRepository.claimPendingJobs(now, owner1, expiresAt, 1));
            } finally {
                doneGate.countDown();
            }
        });

        pool.submit(() -> {
            await(startGate);
            try {
                r2.set(jobRepository.claimPendingJobs(now, owner2, expiresAt, 1));
            } finally {
                doneGate.countDown();
            }
        });

        startGate.countDown();
        doneGate.await();
        pool.shutdown();

        // 검증
        /*
         * 두 반환값의 합이 정확히 1
         * DB에서 해당 job을 다시 조회
         *   - status가 running인지
         *   - lockOwner가 owner1또는 owner2 중 하나인지 확이
         * */
//        int sum = r1.get() + r2.get();
//        assertThat(sum).isEqualTo(1);

        Job updated = jobRepository.findById(job.getId()).orElseThrow();
        assertThat(updated.getStatus()).isEqualTo(RUNNING);
        assertThat(updated.getLockOwner()).isIn(owner1, owner2);
        assertThat(updated.getLockedAt()).isEqualTo(now);

        int claimedByOwner1 = jobRepository.findClaimedJobs(owner1, now).size();
        int claimedByOwner2 = jobRepository.findClaimedJobs(owner2, now).size();
        assertThat(claimedByOwner1 + claimedByOwner2).isEqualTo(1);
    }

    private void await(CountDownLatch latch) {
        try {
            latch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        }
    }
}
