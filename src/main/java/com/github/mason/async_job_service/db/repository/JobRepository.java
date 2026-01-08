package com.github.mason.async_job_service.db.repository;

import com.github.mason.async_job_service.db.domain.Job;
import com.github.mason.async_job_service.db.enums.JobStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;

public interface JobRepository extends JpaRepository<Job, Long> {
    // 대기열 실행 후보 조회
    // select * from jobs where status = 'PENDING' and next_run_at <= now() order by next_run_at asc;
    @Query("""
        select j from Job j
        where j.status = :status
            and j.nextRunAt <= :now
        order by j.nextRunAt asc
    """)
    List<Job> findRunnableJobs(JobStatus status, LocalDateTime now);


}
