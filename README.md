# Async Job Service

DB 기반 락과 상태 전이를 이용해 동시성 안전한 비동기 Job 처리를 구현한 프로젝트입니다.

여러 워커 환경에서도 중복 실행 없이 Job을 안전하게 처리하고,

실행 중 유실된 Job을 자동 복구할 수 있도록 설계했습니다.

---

## 1. 프로젝트 목표

- 다중 워커 환경에서 안전한 Job 실행
- Job 중복 실행 방지
- 실행 실패 및 워커 장애 상황에서도 Job 유실 방지
- 재시도 및 지연 실행 지원
- 테스트로 실행 흐름 검증

## 2. Job 상태 흐름
```
PENDING
↓ (선점)
RUNNING
↓ 성공
SUCCEEDED

RUNNING
↓ 실패 (재시도 가능)
PENDING (nextRunAt 설정)

RUNNING
↓ 재시도 초과
FAILED  
```

상태 설명

| 상태      | 설명           |
|---------|--------------|
| pending | 실행 대기 중인 Job |
| running| 워커에 의해 선점되어 실행 중|
| succeeded| 정상 처리 완료|
| failed | 재시도 초과로 실패 확정|

## 3. 핵심 설계 포인트

### 3.1 원자적 Job 선점 (DB 기반)

- `UPDATE ... WHERE status = PENDING` 쿼리로 선점과 상태 변경을 동시에 처리
- 여러 워커가 동시에 실행돼도 하나의 워커만 Job을 선점

```sql
UPDATE jobs
SET status = 'RUNNING',
locked_at = now,
lock_owner = workerId,
lock_expires_at = expiresAt
WHERE status = 'PENDING'
```

### 3.2 락 TTL 기반 유실 Job 복구

- 실행 중 워커가 장애로 종료되면 RUNNING 상태가 남을 수 있음
- lock_expires_at 기준으로 락이 만료된 RUNNING Job을 감지
- 자동으로 PENDING 상태로 복구하여 재실행 가능

```
RUNNING + lock_expires_at <= now()
→ PENDING 으로 복구
```

### 3.3 재시도 & 지연 실행

- Job 실패 시:
  - retryCount 증가
  - nextRunAt 설정
- 재시도 횟수 초과 시 FAILED 처리

### 3.4 트랜잭션 경계 설계

- Job 상태 변경은 트랜잭션 내에서 수행
- 실행 로직과 상태 전이를 명확히 분리
- Spring의 프록시 기반 트랜잭션 특성(self-invocation 문제)을 고려해 설계

### 4. 실행 흐름

1. 스케줄러가 주기적으로 runBatch() 호출
2. 락 만료된 RUNNING Job 복구
3. 실행 가능한 PENDING Job 선점
4. Job 실행
5. 성공/실패에 따라 상태 전이 및 락 해제

## 5. 테스트 전략

- Spring Boot 통합 테스트 기반
- 실제 DB를 사용해 상태 전이 검증
- 검증 항목:
  - PENDING → RUNNING → SUCCEEDED 흐름
  - 락 필드 정상 해제
  - JobExecutor 호출 여부 확인

## 6. 기술 스택

- Java 17
- Spring Boot 4.x
- Spring Data JPA
- MySQL 8
- JUnit 5
- Mockito