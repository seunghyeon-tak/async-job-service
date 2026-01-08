package com.github.mason.async_job_service.db.enums;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public enum JobStatus {
    PENDING("대기"),
    RUNNING("진행"),
    SUCCEEDED("완료"),
    FAILED("실패")
    ;

    private final String description;
}
