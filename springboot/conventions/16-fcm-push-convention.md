# 16. FCM 푸시 알림 컨벤션

## 개요

본 프로젝트는 **Firebase Cloud Messaging (FCM)**을 사용하여 푸시 알림을 전송합니다.
배치 전송(최대 500개 단위), 단건 전송, APNs 설정을 포함하며, 모든 전송 결과를 로그 엔티티에 기록합니다.

---

## 디렉토리 구조

```
config/
└── FirebaseConfig.java          # Firebase 초기화

service/
├── FcmService.java              # FCM 전송 핵심 로직
├── FcmTokenService.java         # FCM 토큰 CRUD
└── FcmSendLogService.java       # 전송 로그 CRUD

entity/
├── FcmToken.java                # FCM 토큰 엔티티
└── FcmSendLog.java              # 전송 로그 엔티티

enums/
├── PushEvent.java               # 푸시 이벤트 정의 (메시지 리스트)
├── FcmChannel.java              # 푸시 채널 (운동, 식단, 혈당 등)
└── FcmSendStatus.java           # 전송 상태 (SUCCESS, FAIL)
```

---

## Firebase 초기화

```java
// FirebaseConfig.java
@Configuration
public class FirebaseConfig {

    @PostConstruct
    public void init() throws IOException {
        InputStream serviceAccount = getClass().getClassLoader()
                .getResourceAsStream("key-store/{project-name}-firebase-service-key.json");

        FirebaseOptions options = FirebaseOptions.builder()
                .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                .build();

        if (FirebaseApp.getApps().isEmpty()) {
            FirebaseApp.initializeApp(options);
        }
    }
}
```

- **서비스 키 경로**: `src/main/resources/key-store/{project-name}-firebase-service-key.json`
- **초기화 시점**: `@PostConstruct` (애플리케이션 시작 시)
- **중복 방지**: `FirebaseApp.getApps().isEmpty()` 체크

---

## FcmService 핵심 패턴

### 배치 전송 (최대 500개)

```java
// FcmService.java
private static final int BATCH_SIZE = 500;

public void sendFcm(List<String> accountIdList, String title, String body, Map<String, String> data) {
    List<FcmToken> fcmTokens = fcmTokenService.getAllByAccountIdList(accountIdList);

    if (fcmTokens.isEmpty()) {
        log.info("📭 전송할 FCM 토큰이 없습니다.");
        return;
    }

    // 500개씩 배치 처리
    for (int i = 0; i < fcmTokens.size(); i += BATCH_SIZE) {
        List<FcmToken> batch = fcmTokens.subList(i, Math.min(i + BATCH_SIZE, fcmTokens.size()));
        sendBatchAndLogAll(batch, title, body, data);
    }
}
```

**배치 전송 흐름:**
1. `accountIdList`로 FCM 토큰 목록 조회
2. 500개 단위로 분할
3. `FirebaseMessaging.getInstance().sendEach(messages)` 호출
4. 각 응답(성공/실패)을 `FcmSendLog`에 기록

```java
BatchResponse response = FirebaseMessaging.getInstance().sendEach(messages);

log.info("📨 FCM 전송: 총={}, 성공={}, 실패={}",
        messages.size(), response.getSuccessCount(), response.getFailureCount());

// 모든 결과 로그 저장
List<SendResponse> responses = response.getResponses();
for (int i = 0; i < responses.size(); i++) {
    SendResponse sendResponse = responses.get(i);
    FcmToken fcmToken = fcmTokens.get(i);

    fcmSendLogSuccessList.add(new FcmSendLog(
            fcmToken.getAccount(),
            fcmToken.getToken(),
            body,
            sendResponse.isSuccessful() ? FcmSendStatus.SUCCESS : FcmSendStatus.FAIL,
            sendResponse.isSuccessful() ? null : sendResponse.getException().getMessage()
    ));
}
```

### 단건 전송

```java
public void sendAndLogFcm(Account account, String token, String title, String body, Map<String, String> data) {
    Message message = buildMessage(token, title, body, data);

    try {
        String messageId = FirebaseMessaging.getInstance().sendAsync(message).get();
        fcmSendLogService.create(account, token, body, FcmSendStatus.SUCCESS, null);
    } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        fcmSendLogService.create(account, token, body, FcmSendStatus.FAIL, "Interrupted: " + e.getMessage());
    } catch (ExecutionException e) {
        fcmSendLogService.create(account, token, body, FcmSendStatus.FAIL, "ExecutionException: " + error);
    }
}
```

### Message 빌더

```java
private Message buildMessage(String token, String title, String body, Map<String, String> data) {
    ApnsConfig apnsConfig = ApnsConfig.builder()
            .setAps(Aps.builder()
                    .setAlert(ApsAlert.builder()
                            .setTitle(title)
                            .setBody(body)
                            .build())
                    .setSound("default")
                    .build())
            .putHeader("apns-priority", "10")
            .build();

    return Message.builder()
            .setToken(token)
            .putAllData(data)
            .setNotification(Notification.builder()
                    .setTitle(title)
                    .setBody(body)
                    .build())
            .setApnsConfig(apnsConfig)
            .build();
}
```

**주요 설정:**
- **APNs sound**: `"default"`
- **APNs priority**: `"10"` (즉시 전송)
- **Notification**: Android/웹용 기본 알림
- **Data**: 커스텀 데이터 (화면 이동 등)

---

## FCM 로깅

### FcmSendLog 엔티티

모든 전송 결과(성공/실패)를 DB에 기록합니다.

```java
// FcmSendLog.java
@Entity
@Table(name = "fcm_send_log")
public class FcmSendLog extends BaseAuditEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @ManyToOne
    @JoinColumn(name = "account_id")
    private Account account;

    @Column(name = "token")
    private String token;

    @Column(name = "body")
    private String body;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private FcmSendStatus status;

    @Column(name = "error_message")
    private String errorMessage;
}
```

---

## FCM 토큰 관리

### FcmToken 엔티티

```java
// FcmToken.java
@Entity
@Table(name = "fcm_token")
public class FcmToken extends BaseAuditEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(name = "token")
    private String token;

    @Column(name = "device_code")
    private String deviceCode;

    @ManyToOne
    @JoinColumn(name = "account_id")
    private Account account;

    public void updateToken(String token) {
        this.token = token;
    }
}
```

- **device_code**: 디바이스 식별자 (같은 계정의 여러 기기 지원)
- **updateToken()**: 기존 토큰 갱신

---

## PushEvent enum

푸시 알림 이벤트를 정의하며, 각 이벤트에 복수의 메시지를 포함합니다.

```java
// PushEvent.java - 구조
@Getter
@RequiredArgsConstructor
public enum PushEvent {

    // 운동 이벤트
    EXERCISE_BREAKFAST_INSUFFICIENT("아침 운동", "운동량이 부족합니다", List.of("메시지1", "메시지2", ...)),
    EXERCISE_BREAKFAST_OPTIMAL("아침 운동", "적절한 운동량입니다", List.of(...)),
    ...

    // 식사 이벤트
    MEAL_BREAKFAST_EXCESSIVE("아침 식사", "탄수화물 과다", List.of(...)),
    ...

    // 혈당 이벤트
    GLUCOSE_BREAKFAST_BEFORE_SEVERE_HIGH("아침 식전 혈당", "매우 높음", List.of(...)),
    ...

    // Dexcom CGM 이벤트
    DEXCOM_NIGHT_LOW("야간 저혈당", "야간 저혈당 알림", List.of(...)),
    ...

    // 기타
    INQUIRY_ANSWER("1:1 문의", "답변이 등록되었습니다", List.of(...)),
    GLUCOSE_ALERT_URGENT_LOW("긴급 저혈당", "긴급 저혈당 알림", List.of(...)),

    private final String title;
    private final String description;
    private final List<String> messages;

    public String getRandomMessage() {
        return messages.get(new Random().nextInt(messages.size()));
    }
}
```

### 주요 카테고리

| 카테고리 | 이벤트 패턴 | 설명 |
|----------|-----------|------|
| 운동 | `EXERCISE_{시간대}_{수준}` | 아침/점심/저녁 × 부족/적정/주의/과다/미기록 |
| 식사 | `MEAL_{시간대}_{수준}` | 아침/점심/저녁 × 과다/적정/부족 |
| 혈당 | `GLUCOSE_{시간대}_{시점}_{수준}` | 식전/식후 × 매우높음/높음/목표/낮음 등 |
| Dexcom CGM | `DEXCOM_{시간대}_{패턴}` | 야간/오전/점심/저녁 × 저혈당/고혈당/변동성 |
| 주간 기록률 | `WEEKLY_RECORD_{대상}_{수준}` | 식사/운동/혈당 × 우수/보통/저조/미기록 |
| 실시간 알림 | `GLUCOSE_ALERT_{수준}` | 긴급 저/고혈당 알림 |

### 팩토리 메서드

| 메서드 | 설명 |
|--------|------|
| `fromExerciseTimingAndLevel()` | 운동 시간대 + 수준 → PushEvent |
| `fromMealTypeAndLevel()` | 식사 시간대 + 수준 → PushEvent |
| `fromGlucoseTimingAndLevel()` | 혈당 시간대 + 수준 → PushEvent |
| `fromGlucoseVariation()` | 혈당 변동 패턴 → PushEvent |

---

## FcmChannel enum

```java
// FcmChannel.java
@Getter
@RequiredArgsConstructor
public enum FcmChannel {
    EXERCISE("운동"),
    MEAL("식단"),
    SELF_GLUCOSE("자가혈당"),
    CGM("연속혈당");

    private final String description;
}
```

---

## FcmSendStatus enum

```java
// FcmSendStatus.java
public enum FcmSendStatus {
    SUCCESS,
    FAIL
}
```
