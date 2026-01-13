package com.github.mason.async_job_service.db.repository;

import com.github.mason.async_job_service.db.domain.Job;
import com.github.mason.async_job_service.db.enums.JobStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

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

    @Transactional
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = """
            update jobs
            set status = 'RUNNING',
                locked_at = :now,
                lock_owner = :owner,
                lock_expires_at = :expiresAt
            where id in (
                select id from (
                    select id
                    from jobs
                    where status = 'PENDING'
                    and next_run_at <= :now
                    and (lock_expires_at is null or lock_expires_at <= :now)
                    order by next_run_at asc, id asc 
                    limit :limit
                ) t
            )
            """, nativeQuery = true)
    int claimPendingJobs(
            @Param("now") LocalDateTime now,
            @Param("owner") String owner,
            @Param("expiresAt") LocalDateTime expiresAt,
            @Param("limit") int limit
    );

    @Query("""
            select j from Job j 
            where j.status = com.github.mason.async_job_service.db.enums.JobStatus.RUNNING
            and j.lockOwner = :owner
            and j.lockedAt = :now
            order by j.id asc
            """)
    List<Job> findClaimedJobs(@Param("owner") String owner, @Param("now") LocalDateTime now);
}
