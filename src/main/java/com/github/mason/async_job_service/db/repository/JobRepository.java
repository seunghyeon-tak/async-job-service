package com.github.mason.async_job_service.db.repository;

import com.github.mason.async_job_service.db.domain.Job;
import com.github.mason.async_job_service.db.enums.JobStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface JobRepository extends JpaRepository<Job, Long> {

}
