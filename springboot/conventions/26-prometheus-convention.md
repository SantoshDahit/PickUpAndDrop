# 26. Prometheus 모니터링 컨벤션

## 개요

Spring Boot Actuator + Micrometer 기반으로 `/actuator/prometheus` 엔드포인트에서 메트릭을 노출하고, 외부 Prometheus 서버가 이를 스크랩한다. JVM·HTTP·HikariCP 등 표준 메트릭은 Spring Boot autoconfig 만으로 자동 수집된다.

> 서버 구축(Prometheus·Grafana 설치, 스크랩 설정, 대시보드)은 인프라 레이어에서 수행한다. 본 문서는 백엔드 애플리케이션 측 통합만 다룬다.

---

## 구성 요소

| 항목 | 위치 | 역할 |
|------|------|------|
| `spring-boot-starter-actuator` | `build.gradle` | actuator 엔드포인트 기반 |
| `io.micrometer:micrometer-registry-prometheus` | `build.gradle` | `/actuator/prometheus` 응답을 Prometheus exposition 포맷으로 렌더 |
| `actuator` 프로필 | `application.yml` 활성 그룹 | loc/dev/pro 전 환경에서 actuator 설정 로드 |
| `application-actuator.yml` | `src/main/resources/` | 노출 엔드포인트·태그·히스토그램 설정 |
| `application-common.yml` whitelist | `security.whitelist` | `/actuator/prometheus/**`, `/actuator/health/**` 만 인증 우회 |

---

## 의존성

```groovy
// build.gradle
implementation 'org.springframework.boot:spring-boot-starter-actuator'
implementation 'io.micrometer:micrometer-registry-prometheus'
```

두 의존성이 모두 있어야 `/actuator/prometheus` 가 정상 응답한다. starter-actuator 만 있으면 엔드포인트는 등록되지만 Prometheus 포맷터가 없어 500 으로 실패한다.

---

## 프로필 / 엔드포인트 설정

```yaml
# application-actuator.yml
spring:
  config:
    activate:
      on-profile: actuator

management:
  endpoints:
    web:
      base-path: /actuator
      exposure:
        include: health, prometheus, metrics, info

  endpoint:
    health:
      group:
        liveness:
          include: ping

  info:
    env:
      enabled: true
    build:
      enabled: true

  prometheus:
    metrics:
      export:
        enabled: true
        descriptions: false

  metrics:
    tags:
      application: ${spring.application.name}
      environment: ${spring.profiles.active}

    distribution:
      percentiles-histogram:
        http.server.requests: true
      percentiles:
        http.server.requests: [ 0.50, 0.95, 0.99 ]
```

| 항목 | 값 | 설명 |
|------|----|------|
| `exposure.include` | `health, prometheus, metrics, info` | 외부 노출 엔드포인트 화이트리스트 |
| `health.group.liveness` | `ping` | 배포 헬스체크용 가벼운 liveness 그룹 (`/actuator/health/liveness`) |
| `prometheus.metrics.export.enabled` | `true` | Prometheus exposition 활성화 |
| `prometheus.metrics.export.descriptions` | `false` | scrape 응답 크기 절감 (메트릭 설명 생략) |
| `metrics.tags.application` / `environment` | 자동 태그 | 멀티 서비스/환경 구분용 공용 태그 |
| `percentiles-histogram.http.server.requests` | `true` | HTTP 요청 지연 히스토그램 기록 |
| `percentiles.http.server.requests` | `[0.50, 0.95, 0.99]` | P50·P95·P99 직접 노출 |

actuator 프로필은 `application.yml` 활성 그룹에 이미 포함되어 있어 환경별 별도 활성화 작업이 필요 없다.

```yaml
spring:
  profiles:
    group:
      loc: 'common,loc,flyway,actuator'
      dev: 'common,dev,flyway,actuator'
      pro: 'common,pro,flyway,actuator'
```

---

## 보안 화이트리스트

actuator 엔드포인트 중 **Prometheus 스크랩과 헬스체크에 필요한 두 경로만** JWT 인증을 우회시킨다.

```yaml
# application-common.yml
security:
  whitelist:
    - /actuator/prometheus/**
    - /actuator/health/**
```

- `/actuator/**` 전체를 통째로 화이트리스트에 넣지 않는다 — `info`, `metrics`, `env` 등이 비인증으로 노출되어 환경변수 / DB 비밀번호가 누설될 수 있다.
- 인터넷 노출 환경에서는 `/actuator/prometheus`, `/actuator/health` 도 인프라 레이어(ALB / Nginx)에서 Prometheus 서버 IP 만 허용하도록 별도 통제하는 것을 권장한다.
- 운영(pro) 환경에서 `info`, `metrics` 엔드포인트를 사용해야 할 일이 생기면 별도 인증 / 관리자 IP 화이트리스트를 거치도록 한다 — 위 화이트리스트에 추가하지 말 것.

---

## 표준 노출 메트릭

별도 코드 없이 자동 수집되는 주요 메트릭:

| 메트릭 | 의미 |
|--------|------|
| `http_server_requests_seconds` | HTTP 요청 지연·건수 (status, uri, method 태그) |
| `jvm_memory_used_bytes`, `jvm_gc_pause_seconds` | JVM 힙·GC |
| `hikaricp_connections_*` | HikariCP 커넥션 풀 |
| `process_cpu_usage`, `system_cpu_usage` | CPU 사용률 |
| `logback_events_total` | 로그 레벨별 카운트 |
| `tomcat_*` | Tomcat 스레드·세션 |

`http.server.requests` 는 위 설정에 따라 P50 / P95 / P99 퍼센타일과 전체 히스토그램(`_bucket`) 을 함께 노출하므로 외부 Prometheus 측에서 `histogram_quantile()` 으로 임의 퍼센타일을 다시 계산할 수 있다.

---

## 커스텀 메트릭 추가 (필요 시)

비즈니스 메트릭은 `MeterRegistry` 주입으로 추가한다. 메트릭 네이밍은 **`<domain>.<event>` 점-구분 소문자** 를 따른다 (`vote.created`, `reward.granted`).

```java
@Service
@RequiredArgsConstructor
public class VoteService {

    private final MeterRegistry meterRegistry;

    public void create(...) {
        // ... 비즈니스 로직 ...
        meterRegistry.counter("vote.created", "type", vote.getType().name()).increment();
    }
}
```

| 타입 | 사용처 |
|------|--------|
| `Counter` | 누적 발생 횟수 (vote.created, reward.granted) |
| `Timer` | 지연 + 횟수 (외부 API 호출 시간) |
| `Gauge` | 현재 값 스냅샷 (대기 큐 길이 등) |

- 카디널리티 폭증 방지 — 태그 값으로 `userId`, `voteId` 같은 무한 식별자를 쓰지 않는다.
- 메트릭 이름 변경은 외부 대시보드 / 알람 룰을 깨뜨리므로 추가는 자유, **삭제·변경은 운영팀과 협의** 후 수행한다.

---

## 로컬 검증

```bash
# 앱 부팅 후
curl -s http://localhost:8083/actuator/prometheus | head -20
curl -s http://localhost:8083/actuator/health/liveness
```

정상 응답 예시:

```
# HELP jvm_memory_used_bytes The amount of used memory
# TYPE jvm_memory_used_bytes gauge
jvm_memory_used_bytes{application="vote24-api-server",environment="loc",area="heap",...} 1.23e+08
http_server_requests_seconds_count{application="vote24-api-server",environment="loc",method="GET",status="200",uri="/v1/votes"} 7
```

500 응답이 나오면 `micrometer-registry-prometheus` 의존성이 누락되었는지 확인한다.

---

## 새 프로젝트 적용 시 체크리스트

- [ ] `spring-boot-starter-actuator` + `io.micrometer:micrometer-registry-prometheus` 의존성 추가
- [ ] `application-actuator.yml` 생성 (위 표준 설정 복사)
- [ ] `application.yml` 활성 프로필 그룹에 `actuator` 포함
- [ ] `application-common.yml` 의 `security.whitelist` 에 `/actuator/prometheus/**`, `/actuator/health/**` **만** 추가
- [ ] `/actuator/prometheus` 가 200 + Prometheus 포맷으로 응답하는지 로컬 확인
- [ ] 인프라 레이어에서 Prometheus 서버 IP 만 actuator 엔드포인트에 접근 가능하도록 통제
