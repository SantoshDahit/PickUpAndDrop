# 18. 배포 컨벤션

## 개요

본 프로젝트는 **GitHub Actions + Docker + Nginx 기반 Blue-Green 무중단 배포**를 사용합니다. 배포 중에도 서비스 다운타임 없이 트래픽을 처리할 수 있습니다.

---

## Dockerfile

```dockerfile
FROM eclipse-temurin:17-jdk

ARG JAR_FILE_PATH=build/libs/*.jar
COPY ${JAR_FILE_PATH} api.jar

ENTRYPOINT ["java", "-jar", "-Djava.net.preferIPv4Stack=true", "api.jar"]
```

- **베이스 이미지**: `eclipse-temurin:17-jdk`
- **IPv4 우선**: `-Djava.net.preferIPv4Stack=true` (DNS 해석 이슈 방지)

---

## 무중단 배포 (Blue-Green)

### 핵심 개념

Blue-Green 배포는 두 개의 동일한 환경(Blue, Green)을 번갈아 사용하여 다운타임 없이 배포하는 전략입니다.

```
┌─────────────────────────────────────────────────────┐
│                     Nginx                           │
│              (리버스 프록시)                          │
│                                                     │
│   트래픽 ──→ proxy_pass http://127.0.0.1:{PORT}     │
│                        │                            │
│              ┌─────────┴──────────┐                 │
│              ▼                    ▼                  │
│     ┌──────────────┐    ┌──────────────┐            │
│     │  BLUE 컨테이너 │    │ GREEN 컨테이너 │            │
│     │ (BLUE_PORT)   │    │ (GREEN_PORT)  │            │
│     └──────────────┘    └──────────────┘            │
│        활성(serving)       대기(idle)                 │
└─────────────────────────────────────────────────────┘
```

- **한 시점에 하나의 컨테이너만 트래픽을 수신**
- Nginx의 `proxy_pass` 포트를 변경하여 트래픽을 전환
- 새 컨테이너가 정상 기동된 후에만 전환하므로 다운타임 0

### 워크플로우 환경변수

```yaml
env:
  PROFILE: dev                                          # dev 또는 pro
  PROJECT_NAME: my-project                              # 프로젝트명
  APPLICATION_NAME: api                                 # 애플리케이션명
  DOCKER_HUB_USERNAME: myorg                            # Docker Hub 사용자
  CONTAINER_PORT: {port}                                # 컨테이너 내부 포트
  BLUE_PORT: {port}                                     # Blue 호스트 포트
  GREEN_PORT: {port+1}                                  # Green 호스트 포트 (Blue + 1 권장)
  DOMAIN: api.example.com                               # 서비스 도메인
  NGINX_CONF: /etc/nginx/sites-available/api.example.com  # Nginx 설정 파일 경로
```

### 컨테이너 이름 규칙

```
{PROJECT_NAME}-{APPLICATION_NAME}-{PROFILE}-BLUE
{PROJECT_NAME}-{APPLICATION_NAME}-{PROFILE}-GREEN
```

---

## 배포 흐름 (6단계)

### Phase 1: Setup

```
GitHub Actions (push to branch 또는 수동 트리거)
  ↓
JDK 17 설정 + Firebase 키 복원
  ↓
Gradle 빌드: ./gradlew clean build -x test
  ↓
Docker 이미지 빌드 + Docker Hub Push
  ↓
SSH 접속 → docker pull
```

### Phase 2: 활성 컬러 감지

현재 실행 중인 컨테이너 이름으로 활성 컬러를 판별합니다.

```bash
BLUE_CONTAINER="{PROJECT}-{APP}-{PROFILE}-BLUE"
GREEN_CONTAINER="{PROJECT}-{APP}-{PROFILE}-GREEN"

if docker ps --format '{{.Names}}' | grep -q "^${BLUE_CONTAINER}$"; then
    # BLUE 활성 → GREEN으로 배포
    NEW_COLOR="GREEN"
    NEW_PORT=$GREEN_PORT
elif docker ps --format '{{.Names}}' | grep -q "^${GREEN_CONTAINER}$"; then
    # GREEN 활성 → BLUE로 배포
    NEW_COLOR="BLUE"
    NEW_PORT=$BLUE_PORT
else
    # 첫 배포 → BLUE로 시작
    NEW_COLOR="BLUE"
    NEW_PORT=$BLUE_PORT
fi
```

### Phase 3: 새 컨테이너 시작

기존 컨테이너는 유지한 채 새 컬러의 컨테이너를 시작합니다.

```bash
# 동일 컬러의 잔여 컨테이너 정리 (안전장치)
docker stop "$NEW_CONTAINER" 2>/dev/null || true
docker rm "$NEW_CONTAINER" 2>/dev/null || true

# 새 컨테이너 시작 (기존 컨테이너는 계속 트래픽 처리 중)
docker run -d --name "$NEW_CONTAINER" \
    -p ${NEW_PORT}:${CONTAINER_PORT} \
    -e "SPRING_PROFILES_ACTIVE=${PROFILE}" \
    -e (환경변수들...) \
    --restart always \
    "$IMAGE_TAG"
```

> 이 시점에서 기존 컨테이너는 여전히 트래픽을 처리하고 있으므로 서비스 중단이 없습니다.

### Phase 4: Health Check

새 컨테이너가 완전히 기동될 때까지 대기합니다. Spring Boot Actuator의 liveness 엔드포인트를 사용합니다.

```bash
HEALTH_URL="http://localhost:${NEW_PORT}/actuator/health/liveness"
MAX_RETRIES=30        # 최대 30회
RETRY_INTERVAL=10     # 10초 간격 → 최대 5분 대기

for i in $(seq 1 $MAX_RETRIES); do
    # 컨테이너가 예기치 않게 종료된 경우 즉시 실패
    if ! docker ps --format '{{.Names}}' | grep -q "^${NEW_CONTAINER}$"; then
        echo "❌ 컨테이너가 종료됨!"
        exit 1
    fi

    HTTP_STATUS=$(curl -s -o /dev/null -w "%{http_code}" "$HEALTH_URL")
    if [ "$HTTP_STATUS" = "200" ]; then
        echo "✅ Health Check 통과!"
        break
    fi

    if [ "$i" = "$MAX_RETRIES" ]; then
        # Health Check 실패 → 롤백 (새 컨테이너 제거, 기존 유지)
        docker stop "$NEW_CONTAINER" || true
        docker rm "$NEW_CONTAINER" || true
        exit 1
    fi

    sleep $RETRY_INTERVAL
done
```

**롤백 조건**:
- 컨테이너가 시작 후 크래시하여 종료된 경우
- 5분 내에 liveness 체크를 통과하지 못한 경우

롤백 시 새 컨테이너만 제거되므로 기존 컨테이너의 서비스는 영향받지 않습니다.

### Phase 5: Nginx 트래픽 전환

Health Check 통과 후, Nginx 설정을 변경하여 트래픽을 새 컨테이너로 전환합니다.

```bash
# server_name 블록 내의 proxy_pass 포트만 변경
sudo sed -i \
    '/server_name {DOMAIN}/,/^\}/{
        s/proxy_pass http:\/\/127\.0\.0\.1:[0-9]*\//proxy_pass http:\/\/127.0.0.1:'"$NEW_PORT"'\//
    }' "$NGINX_CONF"

# Nginx 문법 검사
if sudo nginx -t; then
    sudo nginx -s reload
else
    # 설정 오류 시 원복 + 새 컨테이너 제거
    sudo sed -i '...' "$NGINX_CONF"  # 이전 포트로 복원
    docker stop "$NEW_CONTAINER" || true
    docker rm "$NEW_CONTAINER" || true
    exit 1
fi
```

**안전장치**:
- `nginx -t`로 문법 검사를 먼저 수행
- 검사 실패 시 설정을 원복하고 새 컨테이너를 제거

### Phase 6: 이전 컨테이너 정리

```bash
# in-flight 요청 처리를 위해 10초 대기
sleep 10

# 이전 컬러 컨테이너 제거
docker stop "$OLD_CONTAINER" 2>/dev/null || true
docker rm "$OLD_CONTAINER" 2>/dev/null || true

# 레거시 컨테이너 정리 (Blue-Green 도입 전 단일 컨테이너)
docker stop "${PROJECT}-${APP}-${PROFILE}" 2>/dev/null || true
docker rm "${PROJECT}-${APP}-${PROFILE}" 2>/dev/null || true

# Dangling 이미지 정리
docker images -f "dangling=true" -q | xargs -r docker rmi -f || true
```

---

## 전체 타임라인

```
시간  ──────────────────────────────────────────────────→

BLUE   ████████████████████████████████░░░░░░░░░░░░░░░░░░
       (serving)                       (stopped)

GREEN  ░░░░░░░░░░░░░░████████████████████████████████████
                      (starting)       (serving)

Nginx  ── :BLUE_PORT ───────────┤ reload ├── :GREEN_PORT ─
                                      ↑
                                 트래픽 전환 (다운타임 0)
```

---

## Docker 이미지 태그 규칙

```
{DOCKER_HUB_USERNAME}/{PROJECT_NAME}-{APPLICATION_NAME}:{PROFILE}.{YYYY.MM.DD}
```

예시: `myorg/my-project-api:dev.2025.03.11`

### 구 이미지 자동 정리

배포 시 Docker Hub API로 현재 날짜 태그를 제외한 동일 프로필의 이전 태그를 자동 삭제합니다. 배포 서버에서도 이전 날짜의 로컬 이미지를 정리합니다.

---

## 동시 배포 방지

```yaml
concurrency:
  group: deploy-{profile}
  cancel-in-progress: true
```

동일 환경에 대한 배포가 동시에 실행되지 않도록 `concurrency` 그룹을 설정합니다. 진행 중인 배포가 있으면 취소하고 새 배포를 실행합니다.

---

## 환경변수 목록

### 공통 (dev + pro)

| 환경변수 | 설명 |
|----------|------|
| `SPRING_PROFILES_ACTIVE` | 활성 프로필 (dev/pro) |
| `AWS_ACCESS_KEY_ID` | AWS 인증 |
| `AWS_SECRET_ACCESS_KEY` | AWS 인증 |
| `JWT_TOKEN_SECRET` | JWT 서명 키 |

> 프로젝트별로 필요한 환경변수(OAuth, SMS, 외부 API 등)를 추가합니다.

### pro 전용

| 환경변수 | 설명 |
|----------|------|
| `AWS_DB_URL` | RDS 엔드포인트 |
| `AWS_DB_USERNAME` | RDS 사용자명 |
| `AWS_DB_PASSWORD` | RDS 비밀번호 |
