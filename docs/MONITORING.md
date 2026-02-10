# Lemuel 모니터링 가이드

## 📊 개요

Lemuel 시스템은 Spring Boot Actuator와 Micrometer를 사용하여 포괄적인 모니터링 기능을 제공합니다.
Prometheus, Grafana 등의 외부 모니터링 시스템과 연동 가능합니다.

## 🔍 Actuator 엔드포인트

### Health Check
```bash
GET http://localhost:8080/actuator/health
```

**응답 예시**:
```json
{
  "status": "UP",
  "components": {
    "db": {
      "status": "UP",
      "details": {
        "database": "PostgreSQL",
        "validationQuery": "isValid()"
      }
    },
    "settlementBatchHealthIndicator": {
      "status": "UP",
      "details": {
        "settlement_date": "2026-02-09",
        "settlement_pending_count": 0,
        "settlement_confirmed_count": 150,
        "adjustment_pending_count": 0
      }
    }
  }
}
```

### Metrics (Prometheus 형식)
```bash
GET http://localhost:8080/actuator/prometheus
```

## 📈 주요 메트릭

### 1. 정산 배치 메트릭

#### 정산 생성 건수
```
settlement_batch_created_total{batch="settlement_creation"} 150
```

#### 정산 확정 건수
```
settlement_batch_confirmed_total{batch="settlement_confirmation"} 150
```

#### 정산 조정 확정 건수
```
settlement_batch_adjustment_confirmed_total{batch="adjustment_confirmation"} 5
```

#### 배치 실행 시간
```
settlement_batch_creation_duration_seconds_sum 2.5
settlement_batch_creation_duration_seconds_count 1
settlement_batch_creation_duration_seconds_max 2.5
```

#### 배치 실패 건수
```
settlement_batch_failures_total{batch_name="settlement_creation"} 0
settlement_batch_failures_total{batch_name="settlement_confirmation"} 0
settlement_batch_failures_total{batch_name="adjustment_confirmation"} 0
```

### 2. 환불 메트릭

#### 환불 요청 건수
```
refund_requests_total 250
```

#### 환불 완료 건수
```
refund_completed_total 245
```

#### 환불 실패 건수 (이유별)
```
refund_failed_total{reason="exceeds_payment"} 3
refund_failed_total{reason="invalid_state"} 2
```

#### 멱등성 키 재사용 건수
```
refund_idempotency_key_reuse_total 15
```

#### 환불 금액 분포
```
refund_amount_sum 15000000.00
refund_amount_count 250
refund_amount_max 500000.00
```

#### 환불 처리 시간
```
refund_processing_duration_seconds_sum 125.0
refund_processing_duration_seconds_count 250
refund_processing_duration_seconds_max 1.2
```

## 🚨 배치 작업 Health Indicator

### 정상 상태 (UP)
```json
{
  "status": "UP",
  "details": {
    "settlement_date": "2026-02-09",
    "settlement_pending_count": 0,
    "settlement_confirmed_count": 150,
    "adjustment_pending_count": 0
  }
}
```

### 경고 상태 (WARNING)
50개 이상의 PENDING 조정이 있을 때:
```json
{
  "status": "WARNING",
  "details": {
    "reason": "Too many pending adjustments",
    "settlement_date": "2026-02-09",
    "settlement_pending_count": 10,
    "settlement_confirmed_count": 140,
    "adjustment_pending_count": 60
  }
}
```

### 비정상 상태 (DOWN)
100개 이상의 PENDING 정산이 있을 때:
```json
{
  "status": "DOWN",
  "details": {
    "reason": "Too many pending settlements",
    "settlement_date": "2026-02-09",
    "settlement_pending_count": 150,
    "settlement_confirmed_count": 0,
    "adjustment_pending_count": 0
  }
}
```

## 🔔 알림 규칙 (Prometheus AlertManager)

### 1. 배치 실패 알림
```yaml
alert: SettlementBatchFailure
expr: increase(settlement_batch_failures_total[5m]) > 0
for: 1m
labels:
  severity: critical
annotations:
  summary: "정산 배치 작업 실패"
  description: "{{ $labels.batch_name }} 배치가 실패했습니다."
```

### 2. PENDING 정산 누적 알림
```yaml
alert: TooManyPendingSettlements
expr: settlement_pending_count > 100
for: 1h
labels:
  severity: warning
annotations:
  summary: "PENDING 정산 과다"
  description: "PENDING 상태 정산이 {{ $value }}건 누적되었습니다."
```

### 3. 배치 실행 시간 초과 알림
```yaml
alert: SettlementBatchSlow
expr: settlement_batch_creation_duration_seconds_max > 300
for: 5m
labels:
  severity: warning
annotations:
  summary: "정산 배치 실행 시간 초과"
  description: "정산 생성 배치가 {{ $value }}초 소요되었습니다 (임계값: 300초)."
```

### 4. 환불 실패율 알림
```yaml
alert: HighRefundFailureRate
expr: (rate(refund_failed_total[1h]) / rate(refund_requests_total[1h])) > 0.05
for: 10m
labels:
  severity: warning
annotations:
  summary: "환불 실패율 높음"
  description: "환불 실패율이 {{ $value | humanizePercentage }}를 초과했습니다."
```

## 📊 Grafana 대시보드

### 대시보드 구성

#### 1. 정산 배치 패널
- **정산 생성 건수** (Time Series)
  - Query: `rate(settlement_batch_created_total[5m])`

- **정산 확정 건수** (Time Series)
  - Query: `rate(settlement_batch_confirmed_total[5m])`

- **배치 실행 시간** (Gauge)
  - Query: `settlement_batch_creation_duration_seconds_max`

- **배치 실패 건수** (Counter)
  - Query: `settlement_batch_failures_total`

#### 2. 환불 현황 패널
- **환불 요청/완료** (Time Series)
  - Query: `rate(refund_requests_total[5m])`, `rate(refund_completed_total[5m])`

- **환불 실패율** (Gauge)
  - Query: `(rate(refund_failed_total[1h]) / rate(refund_requests_total[1h])) * 100`

- **환불 금액 분포** (Histogram)
  - Query: `histogram_quantile(0.99, rate(refund_amount_bucket[5m]))`

- **멱등성 키 재사용** (Counter)
  - Query: `refund_idempotency_key_reuse_total`

#### 3. 시스템 Health 패널
- **Health Status** (Stat)
  - Query: `up{job="lemuel"}`

- **PENDING 정산** (Time Series)
  - Query: Custom query to `/actuator/health`

## 🐳 Prometheus 설정

### prometheus.yml
```yaml
global:
  scrape_interval: 15s
  evaluation_interval: 15s

scrape_configs:
  - job_name: 'lemuel'
    metrics_path: '/actuator/prometheus'
    static_configs:
      - targets: ['localhost:8080']
        labels:
          application: 'lemuel'
          environment: 'production'
```

### Docker Compose 예시
```yaml
version: '3.8'

services:
  prometheus:
    image: prom/prometheus:latest
    ports:
      - "9090:9090"
    volumes:
      - ./prometheus.yml:/etc/prometheus/prometheus.yml
      - prometheus-data:/prometheus
    command:
      - '--config.file=/etc/prometheus/prometheus.yml'
      - '--storage.tsdb.path=/prometheus'

  grafana:
    image: grafana/grafana:latest
    ports:
      - "3000:3000"
    volumes:
      - grafana-data:/var/lib/grafana
    environment:
      - GF_SECURITY_ADMIN_PASSWORD=admin

volumes:
  prometheus-data:
  grafana-data:
```

## 📞 외부 시스템 연동

### 1. Slack 알림
```yaml
# AlertManager 설정
receivers:
  - name: 'slack'
    slack_configs:
      - api_url: 'https://hooks.slack.com/services/YOUR/WEBHOOK/URL'
        channel: '#lemuel-alerts'
        title: '{{ .GroupLabels.alertname }}'
        text: '{{ range .Alerts }}{{ .Annotations.description }}{{ end }}'
```

### 2. PagerDuty 연동
```yaml
receivers:
  - name: 'pagerduty'
    pagerduty_configs:
      - service_key: 'YOUR_SERVICE_KEY'
        severity: '{{ .CommonLabels.severity }}'
```

### 3. 이메일 알림
```yaml
receivers:
  - name: 'email'
    email_configs:
      - to: 'ops@example.com'
        from: 'lemuel-alerts@example.com'
        smarthost: 'smtp.gmail.com:587'
        auth_username: 'your-email@gmail.com'
        auth_password: 'your-app-password'
```

## 🔧 커스텀 메트릭 추가 방법

### 1. 새로운 메트릭 클래스 생성
```java
@Component
public class CustomMetrics {
    private final Counter customCounter;

    public CustomMetrics(MeterRegistry registry) {
        this.customCounter = Counter.builder("custom.metric")
                .description("Custom metric description")
                .tag("type", "custom")
                .register(registry);
    }

    public void incrementCustom() {
        customCounter.increment();
    }
}
```

### 2. 서비스에서 사용
```java
@Service
public class MyService {
    private final CustomMetrics metrics;

    public void doSomething() {
        // 비즈니스 로직
        metrics.incrementCustom();
    }
}
```

## 📝 모니터링 베스트 프랙티스

1. **배치 작업 모니터링**: 매일 새벽 배치 실행 결과를 확인하고, 실패 시 즉시 알림
2. **환불 이상 패턴 감지**: 환불 실패율, 초과환불 시도 등 비정상 패턴 모니터링
3. **성능 지표 추적**: 배치 실행 시간, 환불 처리 시간 등 성능 메트릭 추적
4. **용량 계획**: PENDING 정산/조정 누적 추세 모니터링으로 시스템 부하 예측
5. **SLO 정의**: 배치 성공률 99.9%, 환불 처리 시간 P99 < 2초 등 SLO 설정

## 🚀 빠른 시작

### 1. 메트릭 확인
```bash
curl http://localhost:8080/actuator/prometheus
```

### 2. Health 확인
```bash
curl http://localhost:8080/actuator/health
```

### 3. Prometheus 설정 후 Grafana 대시보드 임포트
- Grafana에서 "Import Dashboard" 선택
- JSON 파일 또는 Dashboard ID 입력
- Prometheus 데이터 소스 연결

---

**문의**: 모니터링 관련 문의는 DevOps 팀으로 연락하세요.
