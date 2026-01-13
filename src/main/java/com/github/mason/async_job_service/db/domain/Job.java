package com.github.mason.async_job_service.db.domain;

import com.github.mason.async_job_service.db.enums.JobStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

import static com.github.mason.async_job_service.db.enums.JobStatus.*;

@Entity
@Table(name = "jobs")
@Getter
@AllArgsConstructor
@NoArgsConstructor
@EntityListeners(AuditingEntityListener.class)
@Builder
public class Job {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String type;  // 어떤 일을 할건지?

    @Column(columnDefinition = "TEXT")
    private String payload;  // 뭘 가지고 실행할건지?

    @Enumerated(EnumType.STRING)
    private JobStatus status;  // 지금 상태

    private int retryCount;  // 실패횟수

    private LocalDateTime nextRunAt;  // 다음 실행 시각

    @Column(columnDefinition = "TEXT")
    private String lastError;  // 실패 이유

    private LocalDateTime lockedAt;  // 선점 시간

    @Column(length = 100)
    private String lockOwner;  // 선점한 워커 식별자

    private LocalDateTime lockExpiresAt;  // 락 만료

    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    public void markRunning(LocalDateTime now) {
        this.status = RUNNING;
    }

    public void markSuccess() {
        this.status = SUCCEEDED;
        this.lastError = null;
    }

    public void markFailure(String error, int maxRetries, LocalDateTime nextRunAt) {
        this.retryCount += 1;
        this.lastError = error;

        if (this.retryCount >= maxRetries) {
            this.status = FAILED;
        } else {
            this.status = PENDING;
            this.nextRunAt = nextRunAt;
        }
    }
}
