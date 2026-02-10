# Lemuel - OpsLab 주문·결제·정산 시스템

Spring Boot 기반의 JWT 인증 + 주문/결제/정산 통합 시스템입니다.

## 📋 프로젝트 개요

- **프로젝트명**: Lemuel (인증·주문·결제·정산 통합 시스템)
- **버전**: 0.0.1-SNAPSHOT
- **Java**: 21
- **Spring Boot**: 3.5.10
- **데이터베이스**: PostgreSQL 17

## 🔥 v0.2.0 부분환불 리팩토링 (2026-02-10)

### 주요 변경사항

**환불 모델 개선**:
- ❌ **이전**: 부분환불 시 음수 Payment 레코드 생성 (비표준, 조회/회계 복잡도 증가)
- ✅ **현재**: Refund 엔티티로 환불 이력 분리 관리 (실무 표준 패턴)

**새로운 기능**:
1. **멱등성 보장**: `Idempotency-Key` 헤더 기반 중복 환불 방지
2. **동시성 제어**: Payment row-level lock (PESSIMISTIC_WRITE)으로 환불 금액 초과 방지
3. **정산 조정**: CONFIRMED 정산 후 환불 시 `SettlementAdjustment` 생성 (회계 감사 추적)
4. **환불 누적 추적**: `Payment.refundedAmount`로 실시간 환불 누적 관리

### 도메인 모델 변경

```
Payment (원결제)
  - refundedAmount: 환불 누적 합계 (0 ~ amount)
  - status: REFUNDED (전액 환불 시)

Refund (환불 이력) - 신규 추가
  - payment_id, amount, status, idempotency_key
  - (payment_id, idempotency_key) UNIQUE 제약

SettlementAdjustment (정산 조정) - 신규 추가
  - settlement_id, refund_id, amount(음수)
  - CONFIRMED 정산에 대한 환불 처리용
```

### API 변경사항

**신규 API**:
```http
POST /refunds/{paymentId}
Idempotency-Key: {UUID}
Content-Type: application/json

{
  "amount": 5000.00,
  "reason": "고객 요청"
}
```

**기존 API 호환 유지** (Idempotency-Key 필수):
```http
POST /refunds/full/{paymentId}
Idempotency-Key: {UUID}

POST /refunds/partial/{paymentId}?refundAmount=5000.00
Idempotency-Key: {UUID}
```

### 정산 배치 추가

- **새벽 3시 10분**: 정산 조정 확정 배치 (`confirmDailySettlementAdjustments`)
  - PENDING -> CONFIRMED 상태 전환

### 마이그레이션 가이드

1. `V4__refunds_and_settlement_adjustments.sql` 자동 실행 (Flyway)
2. 기존 음수 Payment 레코드가 있다면 수동 마이그레이션 필요
3. 환불 API 호출 시 **`Idempotency-Key` 헤더 필수**

---

## 🏗️ 시스템 아키텍처

```
┌─────────────────────────────────────────────────────────────┐
│                         Client                              │
└────────────────────┬────────────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────────────┐
│                    Spring Security                          │
│                  (JWT Filter Chain)                         │
└────────────────────┬────────────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────────────┐
│                     Controllers                             │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐     │
│  │AuthController│  │OrderController│  │RefundControl │     │
│  │ /auth/login  │  │   /orders    │  │   /refunds   │     │
│  └──────────────┘  └──────────────┘  └──────────────┘     │
└────────────────────┬────────────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────────────┐
│                    Service Layer                            │
│  RefundService + SettlementAdjustmentService                │
│           SettlementBatchService (일 단위 배치)             │
└────────────────────┬────────────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────────────┐
│                   Repository Layer                          │
│  Refund | SettlementAdjustment | Payment | Settlement       │
└────────────────────┬────────────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────────────┐
│                  PostgreSQL Database                        │
│  refunds | settlement_adjustments | payments | settlements  │
└─────────────────────────────────────────────────────────────┘
```

## 📊 주문/결제/정산 상태 전이 다이어그램

### 주문(Order) 상태
- **CREATED**: 주문 생성됨(결제 전)
- **PAID**: 결제 완료로 주문 확정
- **CANCELED**: 결제 전 취소
- **REFUNDED**: 결제 후 환불 완료

### 결제(Payment) 상태
- **READY**: 결제 생성(요청 준비)
- **AUTHORIZED**: 승인됨(카드/간편결제 승인)
- **CAPTURED**: 매입/확정(실 결제 완료)
- **FAILED**: 실패
- **CANCELED**: 승인 취소
- **REFUNDED**: 전액 환불 완료

### 환불(Refund) 상태 - v0.2.0 신규
- **REQUESTED**: 환불 요청됨
- **APPROVED**: 환불 승인됨
- **COMPLETED**: 환불 완료
- **FAILED**: 환불 실패
- **CANCELED**: 환불 취소

### 정산(Settlement) 상태
- **PENDING**: 정산 대상 생성(아직 확정 전)
- **CONFIRMED**: 정산 금액 확정(회계 기준 확정)
- **CANCELED**: 정산 취소(환불/취소 반영) - *deprecated in v0.2.0*

### 정산 조정(SettlementAdjustment) 상태 - v0.2.0 신규
- **PENDING**: 조정 대기 중
- **CONFIRMED**: 조정 확정

### 환불 처리 흐름 (v0.2.0)

```
[Payment] CAPTURED (amount: 10000, refundedAmount: 0)
   |
   | (부분환불 3000원 요청 + Idempotency-Key)
   v
[Refund] REQUESTED -> COMPLETED (amount: 3000)
   |
   v
[Payment] CAPTURED (amount: 10000, refundedAmount: 3000)
   |
   | (부분환불 7000원 요청)
   v
[Refund] REQUESTED -> COMPLETED (amount: 7000)
   |
   v
[Payment] REFUNDED (amount: 10000, refundedAmount: 10000)
```

### 정산 확정 후 환불 시 조정 생성

```
[Settlement] CONFIRMED (amount: 10000)
   |
   | (환불 2000원 발생)
   v
[SettlementAdjustment] PENDING (amount: -2000, refund_id: ...)
   |
   | (새벽 3시 10분 배치)
   v
[SettlementAdjustment] CONFIRMED
```

## 📊 데이터베이스 스키마 (v0.2.0)

### payments 테이블 (변경)
```sql
CREATE TABLE payments (
    id BIGSERIAL PRIMARY KEY,
    order_id BIGINT NOT NULL,
    amount DECIMAL(10, 2) NOT NULL,
    refunded_amount DECIMAL(10, 2) NOT NULL DEFAULT 0,  -- 신규
    status VARCHAR(20) NOT NULL DEFAULT 'READY',
    payment_method VARCHAR(50),
    pg_transaction_id VARCHAR(100),
    captured_at TIMESTAMP,                              -- 신규
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_payments_refunded_amount
        CHECK (refunded_amount >= 0 AND refunded_amount <= amount)
);
```

### refunds 테이블 (신규)
```sql
CREATE TABLE refunds (
    id BIGSERIAL PRIMARY KEY,
    payment_id BIGINT NOT NULL,
    amount DECIMAL(10, 2) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'REQUESTED',
    reason TEXT,
    idempotency_key VARCHAR(255) NOT NULL,
    requested_at TIMESTAMP NOT NULL DEFAULT NOW(),
    completed_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_refund_payment FOREIGN KEY (payment_id) REFERENCES payments(id),
    CONSTRAINT chk_refunds_amount CHECK (amount > 0)
);

-- 멱등성 보장: 동일 payment + idempotency_key 중복 방지
CREATE UNIQUE INDEX idx_refunds_payment_idempotency
ON refunds(payment_id, idempotency_key);
```

### settlement_adjustments 테이블 (신규)
```sql
CREATE TABLE settlement_adjustments (
    id BIGSERIAL PRIMARY KEY,
    settlement_id BIGINT NOT NULL,
    refund_id BIGINT NOT NULL,
    amount DECIMAL(10, 2) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    adjustment_date DATE NOT NULL,
    confirmed_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_adjustment_settlement FOREIGN KEY (settlement_id) REFERENCES settlements(id),
    CONSTRAINT fk_adjustment_refund FOREIGN KEY (refund_id) REFERENCES refunds(id),
    CONSTRAINT chk_adjustments_amount CHECK (amount < 0)
);

-- 환불 1건당 조정 1건 보장
CREATE UNIQUE INDEX idx_adjustments_refund_id_unique
ON settlement_adjustments(refund_id);
```

## 🚀 시작하기

### 1. 사전 요구사항
- Java 21
- Docker & Docker Compose
- Gradle

### 2. PostgreSQL 실행
```bash
docker-compose up -d
```

### 3. 데이터베이스 생성
```bash
psql -U postgres -c "CREATE DATABASE opslab;"
```

또는 PostgreSQL에 접속해서:
```sql
CREATE DATABASE opslab;
CREATE USER inter WITH PASSWORD '1234';
GRANT ALL PRIVILEGES ON DATABASE opslab TO inter;
```

### 4. 애플리케이션 실행
```bash
./gradlew bootRun
```

## 📡 API 엔드포인트

### 환불 API (v0.2.0 업데이트)

#### 1. 환불 요청 (통합 API)
```http
POST /refunds/{paymentId}
Idempotency-Key: 550e8400-e29b-41d4-a716-446655440000
Content-Type: application/json

{
  "amount": 5000.00,
  "reason": "고객 요청"
}
```

**응답**:
```json
{
  "refundId": 1,
  "paymentId": 1,
  "refundAmount": 5000.00,
  "refundStatus": "COMPLETED",
  "reason": "고객 요청",
  "requestedAt": "2026-02-10T10:00:00",
  "completedAt": "2026-02-10T10:00:01",
  "paymentAmount": 10000.00,
  "refundedAmount": 5000.00,
  "refundableAmount": 5000.00,
  "paymentStatus": "CAPTURED"
}
```

#### 2. 전체 환불 (기존 API 호환)
```http
POST /refunds/full/{paymentId}
Idempotency-Key: 550e8400-e29b-41d4-a716-446655440001
```
- Payment: refundedAmount = amount, status = REFUNDED
- Refund 레코드 생성 (amount = 환불 가능 금액 전체)

#### 3. 부분 환불 (기존 API 호환)
```http
POST /refunds/partial/{paymentId}?refundAmount=5000.00
Idempotency-Key: 550e8400-e29b-41d4-a716-446655440002
```
- Payment: refundedAmount 누적, status = CAPTURED 유지
- Refund 레코드 생성 (amount = 요청 금액)

#### 4. 결제 실패 환불 (취소)
```http
POST /refunds/failed/{paymentId}
```
- Payment: AUTHORIZED/FAILED → CANCELED
- Idempotency-Key 불필요 (환불 아님)

### 오류 응답

**초과 환불 시도 (409 Conflict)**:
```json
{
  "timestamp": "2026-02-10T10:00:00",
  "status": 409,
  "error": "Conflict",
  "errorCode": "REFUND_EXCEEDS_PAYMENT",
  "message": "환불 가능 금액을 초과했습니다. 환불 가능: 3000.00, 요청: 5000.00"
}
```

**Idempotency-Key 누락 (400 Bad Request)**:
```json
{
  "timestamp": "2026-02-10T10:00:00",
  "status": 400,
  "error": "Bad Request",
  "errorCode": "MISSING_IDEMPOTENCY_KEY",
  "message": "Idempotency-Key 헤더는 필수입니다."
}
```

### 정산 배치 작업

- **매일 새벽 2시**: 전날 `CAPTURED` 결제 → `PENDING` 정산 생성
- **매일 새벽 3시**: `PENDING` 정산 → `CONFIRMED` 확정
- **매일 새벽 3시 10분**: `PENDING` 정산 조정 → `CONFIRMED` 확정 (v0.2.0)

## 🧪 테스트

```bash
./gradlew test
```

### 통합 테스트 시나리오

1. **부분환불 2회 누적**: refundedAmount 10000, status REFUNDED
2. **초과환불 시도**: RefundExceedsPaymentException (409)
3. **멱등성 키 재사용**: 동일 Refund 레코드 반환
4. **CONFIRMED 정산 후 환불**: SettlementAdjustment 생성
5. **PENDING 정산 후 환불**: Settlement 금액 직접 차감
6. **잘못된 상태 환불**: InvalidPaymentStateException (409)

## 📝 검증 체크리스트

### DB 제약
- ✅ `payments.refunded_amount` CHECK (0 ~ amount)
- ✅ `refunds(payment_id, idempotency_key)` UNIQUE
- ✅ `settlement_adjustments(refund_id)` UNIQUE
- ✅ `refunds.amount` CHECK (> 0)
- ✅ `settlement_adjustments.amount` CHECK (< 0)

### 멱등성
- ✅ 동일 `Idempotency-Key` 재요청 시 동일 Refund 반환
- ✅ 환불 금액 중복 반영 방지

### 동시성
- ✅ `PESSIMISTIC_WRITE` lock으로 동시 환불 요청 직렬화
- ✅ `refundedAmount` 초과 방지

### 배치 재실행
- ✅ Settlement 중복 생성 방지 (`findByPaymentId` 체크)
- ✅ Adjustment 중복 생성 방지 (`findByRefundId` 체크)

## 🐛 트러블슈팅

### SpringDoc OpenAPI ClassNotFoundException 오류
```bash
# build.gradle.kts에 kotlin-reflect 추가됨
implementation("org.jetbrains.kotlin:kotlin-reflect")
```

### Idempotency-Key 누락
```bash
# 환불 API 호출 시 반드시 헤더 포함
curl -X POST http://localhost:8080/refunds/1 \
  -H "Idempotency-Key: $(uuidgen)" \
  -H "Content-Type: application/json" \
  -d '{"amount": 5000.00, "reason": "고객 요청"}'
```

## 📄 라이선스

이 프로젝트는 내부 OpsLab 용도로 개발되었습니다.
