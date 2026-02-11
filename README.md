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
│  ┌──────────────────────────────────────────────────┐     │
│  │    SettlementSearchController                     │     │
│  │    /api/settlements/search (Elasticsearch)        │     │
│  └──────────────────────────────────────────────────┘     │
└────────────────────┬────────────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────────────┐
│                    Service Layer                            │
│  RefundService + SettlementAdjustmentService                │
│  SettlementBatchService (일 단위 배치)                      │
│  SettlementIndexService (Elasticsearch 색인)                │
│  SettlementSearchService (복합 검색)                        │
└────────────────────┬────────────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────────────┐
│              Repository Layer                               │
│  Refund | SettlementAdjustment | Payment | Settlement       │
│  SettlementSearchRepository (Elasticsearch)                 │
└─────────┬───────────────────────────────────────┬───────────┘
          │                                       │
          ▼                                       ▼
┌──────────────────────┐            ┌──────────────────────┐
│ PostgreSQL Database  │            │   Elasticsearch      │
│  refunds             │            │  settlement_search   │
│  settlement_adj...   │            │  (검색 인덱스)       │
│  payments            │            └──────────────────────┘
│  settlements         │
└──────────────────────┘
```

## 📊 모니터링 & 검색

### Prometheus & Grafana (성능 모니터링)
- **Prometheus**: `/actuator/prometheus` 엔드포인트로 메트릭 수집
- **Grafana**: 실시간 대시보드 및 알림 설정
- **Slack 알림**: 에러율/응답시간 임계치 초과 시 자동 알림

### Elasticsearch (정산 검색)
- **settlement_search 인덱스**: 정산/주문/결제/환불 통합 데이터
- **Nori Analyzer**: 한글 형태소 분석 (결제 수단, 환불 사유 등)
- **복합 검색 API**: 기간/금액/상태별 필터링 + 집계

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

### 인프라 구성 전략

이 프로젝트는 **하이브리드 인프라 구성**을 사용합니다:

| 컴포넌트 | 환경 | 이유 |
|---------|------|------|
| **PostgreSQL** | 로컬 설치 | 프로덕션 환경과 동일한 설정, 성능 최적화 |
| **Elasticsearch** | Cloud (Elastic Cloud) | 확장성, 관리 용이성, 프로덕션 대비 |
| **Prometheus** | Docker | 개발 환경 전용 메트릭 수집 |
| **Grafana** | Docker | 개발 환경 전용 시각화 대시보드 |

### 1. 사전 요구사항
- Java 21
- Docker & Docker Compose
- Gradle
- **PostgreSQL 17** (로컬 설치)
- **Elastic Cloud 계정** (무료 트라이얼 가능)

### 2. PostgreSQL 로컬 설치 및 설정

#### macOS (Homebrew)
```bash
brew install postgresql@17
brew services start postgresql@17

# 데이터베이스 생성
createdb opslab
```

#### Windows
```bash
# PostgreSQL 공식 사이트에서 설치: https://www.postgresql.org/download/windows/
# 또는 Chocolatey 사용
choco install postgresql17

# 데이터베이스 생성
psql -U postgres -c "CREATE DATABASE opslab;"
```

#### Linux (Ubuntu/Debian)
```bash
sudo apt update
sudo apt install postgresql-17

# 데이터베이스 생성
sudo -u postgres createdb opslab
```

#### 사용자 및 권한 설정
```sql
-- PostgreSQL에 접속
psql -U postgres

-- 사용자 생성 및 권한 부여
CREATE DATABASE opslab;
CREATE USER inter WITH PASSWORD '1234';
GRANT ALL PRIVILEGES ON DATABASE opslab TO inter;

-- PostgreSQL 15+ 추가 권한
\c opslab
GRANT ALL ON SCHEMA public TO inter;
```

### 3. Elasticsearch Cloud 설정

#### 3-1. Elastic Cloud 계정 생성
1. [Elastic Cloud](https://cloud.elastic.co/) 방문
2. 무료 트라이얼 등록 (14일 무료)
3. **Deployment 생성**:
   - Region: 가장 가까운 리전 선택 (예: Tokyo)
   - Version: 8.x 최신 버전
   - Cloud Provider: AWS, GCP, Azure 중 선택

#### 3-2. Nori 플러그인 활성화
```bash
# Elastic Cloud Console에서:
# Deployments > [Your Deployment] > Manage > Extensions
# "analysis-nori" 플러그인 활성화
```

또는 Kibana Dev Tools에서 확인:
```
GET _cat/plugins
```

#### 3-3. 연결 정보 확인
```
Cloud ID: my-deployment:abcdef1234...
Elasticsearch Endpoint: https://my-deployment.es.us-east-1.aws.found.io:9243
Username: elastic
Password: [생성 시 제공된 비밀번호]
```

#### 3-4. application.yml 설정
```yaml
spring:
  elasticsearch:
    uris: https://my-deployment.es.us-east-1.aws.found.io:9243
    username: elastic
    password: your-password-here
```

**보안을 위해 환경 변수 사용 권장**:
```yaml
spring:
  elasticsearch:
    uris: ${ELASTICSEARCH_URIS}
    username: ${ELASTICSEARCH_USERNAME}
    password: ${ELASTICSEARCH_PASSWORD}
```

```bash
# .env 파일 생성
export ELASTICSEARCH_URIS=https://your-deployment.es.region.cloud.es.io:9243
export ELASTICSEARCH_USERNAME=elastic
export ELASTICSEARCH_PASSWORD=your-password
```

### 4. Docker 인프라 실행 (Prometheus, Grafana)
```bash
docker-compose up -d
```

실행되는 서비스:
- **Prometheus**: `localhost:9090` (메트릭 수집)
- **Grafana**: `localhost:3000` (대시보드, admin/admin)

### 5. 애플리케이션 실행
```bash
# 환경 변수 설정 (선택사항)
export ELASTICSEARCH_URIS=https://your-deployment.es.region.cloud.es.io:9243
export ELASTICSEARCH_USERNAME=elastic
export ELASTICSEARCH_PASSWORD=your-password

# 애플리케이션 실행
./gradlew bootRun
```

애플리케이션이 시작되면:
- **API 서버**: `http://localhost:8080`
- **Swagger UI**: `http://localhost:8080/swagger-ui.html`
- **Actuator**: `http://localhost:8080/actuator`
- **Prometheus 메트릭**: `http://localhost:8080/actuator/prometheus`

### 6. Prometheus 설정 확인 (Docker)
Prometheus가 Spring Boot 애플리케이션에서 메트릭을 수집하는지 확인:

```bash
# Prometheus 웹 UI 접속
open http://localhost:9090

# Status > Targets 메뉴에서 'spring-boot' 타겟 상태 확인
# State: UP (초록색)이면 정상
```

### 7. Grafana 대시보드 설정 (Docker)
```bash
# Grafana 웹 UI 접속 (admin/admin)
open http://localhost:3000

# 1. Data Source 추가
#    - Configuration > Data Sources > Add data source
#    - Prometheus 선택
#    - URL: http://prometheus:9090
#    - Save & Test

# 2. 대시보드 Import
#    - Dashboards > Import
#    - Import via grafana.com: 4701 (JVM Micrometer)
#    - 또는 11378 (Spring Boot Statistics)
```

### 8. Elasticsearch Cloud 인덱스 생성 확인
애플리케이션 시작 시 자동으로 `settlement_search` 인덱스가 Elastic Cloud에 생성됩니다.

**Kibana Dev Tools에서 확인**:
```
# 인덱스 확인
GET _cat/indices?v

# settlement_search 인덱스 매핑 확인
GET settlement_search/_mapping
```

**또는 curl 사용** (Basic Auth):
```bash
# 인덱스 확인
curl -u elastic:your-password \
  https://your-deployment.es.region.cloud.es.io:9243/_cat/indices?v

# settlement_search 인덱스 매핑 확인
curl -u elastic:your-password \
  https://your-deployment.es.region.cloud.es.io:9243/settlement_search/_mapping?pretty
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

### 정산 검색 API (Elasticsearch 기반)

#### 복합 검색
```http
GET /api/settlements/search?startDate=2026-01-01T00:00:00&endDate=2026-02-11T23:59:59&isRefunded=false&page=0&size=20
```

**쿼리 파라미터**:
- `startDate`: 검색 시작 날짜 (ISO 8601 형식)
- `endDate`: 검색 종료 날짜
- `isRefunded`: 환불 여부 (true/false)
- `productName`: 결제 수단 검색 (Nori 형태소 분석 적용)
- `status`: 정산 상태 (PENDING/CONFIRMED)
- `page`: 페이지 번호 (0부터 시작)
- `size`: 페이지 크기 (기본 20)
- `sortBy`: 정렬 필드 (기본 orderCreatedAt)
- `sortDirection`: 정렬 방향 (ASC/DESC, 기본 DESC)

**응답 예시**:
```json
{
  "settlements": [
    {
      "settlementId": 1,
      "settlementStatus": "CONFIRMED",
      "settlementAmount": 10000.00,
      "orderId": 1,
      "paymentId": 1,
      "hasRefund": false,
      "orderCreatedAt": "2026-02-10T10:00:00"
    }
  ],
  "totalElements": 100,
  "totalPages": 5,
  "currentPage": 0,
  "pageSize": 20,
  "aggregations": {
    "totalAmount": 1000000.00,
    "averageAmount": 10000.00,
    "minAmount": 5000.00,
    "maxAmount": 50000.00,
    "countByStatus": {
      "CONFIRMED": 80,
      "PENDING": 20
    },
    "refundedCount": 10,
    "nonRefundedCount": 90,
    "countByDate": {}
  }
}
```

### 정산 배치 작업

- **매일 새벽 2시**: 전날 `CAPTURED` 결제 → `PENDING` 정산 생성
- **매일 새벽 3시**: `PENDING` 정산 → `CONFIRMED` 확정
- **매일 새벽 3시 10분**: `PENDING` 정산 조정 → `CONFIRMED` 확정 (v0.2.0)
- **정산 데이터 Elasticsearch 색인**: 정산 생성/수정 시 자동 색인 (Spring Event 기반)

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

### Elasticsearch Cloud 연결 실패
```
ElasticsearchStatusException: method [HEAD], host [https://...], URI [/]
```

**해결 방법**:
1. **연결 정보 확인**:
   - Elastic Cloud Console에서 Endpoint URL 복사
   - Username: `elastic`
   - Password: 배포 생성 시 제공된 비밀번호

2. **application.yml 설정 확인**:
   ```yaml
   spring:
     elasticsearch:
       uris: https://your-deployment.es.region.cloud.es.io:9243
       username: elastic
       password: your-password
   ```

3. **방화벽 확인**:
   - Elastic Cloud는 기본적으로 모든 IP 허용
   - 필요시 Security > Traffic Filters에서 IP 화이트리스트 설정

### Elasticsearch Nori 플러그인 에러
```
ElasticsearchException: Unknown tokenizer type [nori_tokenizer]
```

**해결 방법 (Elastic Cloud)**:
```bash
# Elastic Cloud Console에서:
# Deployments > [Your Deployment] > Manage > Extensions
# "analysis-nori" 플러그인 활성화 후 deployment 재시작
```

**또는 Docker 환경에서**:
```bash
# Elasticsearch 컨테이너에 접속
docker exec -it lemuel-elasticsearch-1 bash
bin/elasticsearch-plugin install analysis-nori
exit

# Elasticsearch 재시작
docker-compose restart elasticsearch

# 설치 확인
curl http://localhost:9200/_cat/plugins
```

### Prometheus 타겟이 DOWN 상태
Prometheus에서 Spring Boot 타겟이 `DOWN` 상태인 경우:

1. **Spring Boot 애플리케이션이 실행 중인지 확인**:
   ```bash
   curl http://localhost:8080/actuator/prometheus
   ```

2. **Prometheus 설정 확인** (`prometheus/prometheus.yml`):
   ```yaml
   scrape_configs:
     - job_name: 'spring-boot'
       metrics_path: '/actuator/prometheus'
       static_configs:
         - targets: ['host.docker.internal:8080']
   ```

3. **Docker 네트워크 확인**:
   - macOS/Windows: `host.docker.internal` 사용
   - Linux: `172.17.0.1` 또는 호스트 IP 사용

### Grafana에서 데이터가 보이지 않음
1. **Data Source 연결 확인**:
   - Configuration > Data Sources > Prometheus
   - URL: `http://prometheus:9090` (Docker 네트워크 내부 주소)
   - Test 버튼 클릭하여 연결 확인

2. **쿼리 테스트**:
   - Explore 메뉴에서 간단한 쿼리 실행
   - 예: `http_server_requests_seconds_count`

### Elasticsearch 인덱스가 생성되지 않음

**Elastic Cloud 환경**:
```bash
# Kibana Dev Tools에서 인덱스 확인
GET _cat/indices?v

# settlement_search 인덱스가 없으면 수동 생성
# Kibana Dev Tools에서 settlement-index-settings.json 내용 붙여넣기
PUT settlement_search
{
  "settings": { ... },
  "mappings": { ... }
}
```

**또는 curl 사용**:
```bash
# 인덱스 확인
curl -u elastic:your-password \
  https://your-deployment.es.region.cloud.es.io:9243/_cat/indices?v

# settlement_search 인덱스 수동 생성
curl -u elastic:your-password \
  -X PUT https://your-deployment.es.region.cloud.es.io:9243/settlement_search \
  -H "Content-Type: application/json" \
  -d @src/main/resources/elasticsearch/settlement-index-settings.json
```

**Docker 로컬 환경**:
```bash
# 인덱스 확인
curl http://localhost:9200/_cat/indices?v

# settlement_search 인덱스 수동 생성
curl -X PUT http://localhost:9200/settlement_search \
  -H "Content-Type: application/json" \
  -d @src/main/resources/elasticsearch/settlement-index-settings.json
```

### PostgreSQL 로컬 연결 실패
```
org.postgresql.util.PSQLException: Connection refused
```

**해결 방법**:
1. **PostgreSQL 서비스 확인**:
   ```bash
   # macOS
   brew services list

   # Windows
   Get-Service postgresql-x64-17

   # Linux
   sudo systemctl status postgresql
   ```

2. **포트 확인** (기본 5432):
   ```bash
   netstat -an | grep 5432
   ```

3. **pg_hba.conf 설정**:
   ```bash
   # 로컬 연결 허용 확인
   # /etc/postgresql/17/main/pg_hba.conf (Linux)
   # /usr/local/var/postgres/pg_hba.conf (macOS)

   local   all   all   trust
   host    all   all   127.0.0.1/32   md5
   ```

## 📄 라이선스

이 프로젝트는 내부 OpsLab 용도로 개발되었습니다.
